package com.example.server.data

import com.example.server.domain.FileService
import com.example.shared.requests.FileListRequest
import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale
import kotlin.io.path.fileSize
import kotlin.io.path.getAttribute
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile

class FileServiceImpl(rootPath: Path) : FileService {

    private val canonicalRoot: Path = rootPath.toAbsolutePath().normalize().toRealPath()

    override suspend fun getLocalDisks(): List<DiskInfo> = withContext(Dispatchers.IO) {
        val mountsFile = File("/proc/mounts")
        if (!mountsFile.exists()) return@withContext getRootFallback()

        val pseudoFs = setOf(
            "proc", "sysfs", "tmpfs", "devtmpfs", "devpts", "cgroup",
            "cgroup2", "securityfs", "pstore", "debugfs", "tracefs",
            "configfs", "fusectl", "mqueue", "hugetlbfs", "autofs",
            "binfmt_misc", "rpc_pipefs", "nsfs", "overlay", "squashfs",
            "bpf", "efivarfs", "ramfs", "fuse.gvfsd-fuse", "fuse.portal"
        )

        val seen = mutableSetOf<String>()
        val result = mutableListOf<DiskInfo>()

        for (line in mountsFile.readLines()) {
            val parts = line.split("\\s+".toRegex())
            if (parts.size < 4) continue

            val device = parts[0]
            val mountPoint = parts[1].replace("\\040", " ")
            val fsType = parts[2]
            val options = parts[3].split(",")

            if (fsType in pseudoFs) continue
            if (!seen.add(mountPoint)) continue

            val mountPath = Path.of(mountPoint)

            val store = try {
                Files.getFileStore(mountPath)
            } catch (_: Exception) {
                continue
            }

            val total = runCatching { store.totalSpace }.getOrDefault(0L)
            if (total == 0L) continue

            result += DiskInfo(
                mountPoint = mountPoint,
                device = device,
                fsType = fsType,
                fsName = store.name(),
                totalSpace = total,
                usableSpace = runCatching { store.usableSpace }.getOrDefault(0L),
                unallocatedSpace = runCatching { store.unallocatedSpace }.getOrDefault(0L),
                isReadOnly = runCatching { store.isReadOnly }.getOrDefault(false),
                options = options
            )
        }

        result.sortedBy { if (it.mountPoint == "/") "" else it.mountPoint }
    }

    private fun getRootFallback(): List<DiskInfo> = try {
        val rootMount = Path.of("/")
        if (!canonicalRoot.startsWith(rootMount)) {
            emptyList()
        } else {
            val store = Files.getFileStore(rootMount)
            listOf(
                DiskInfo(
                    mountPoint = "/",
                    device = "unknown",
                    fsType = "unknown",
                    fsName = store.name(),
                    totalSpace = store.totalSpace,
                    usableSpace = store.usableSpace,
                    unallocatedSpace = store.unallocatedSpace,
                    isReadOnly = store.isReadOnly,
                    options = emptyList()
                )
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    override suspend fun getFiles(fileListRequest: FileListRequest): List<FileItem> {
        require(fileListRequest.offset >= 0) { "offset must be >= 0, was ${fileListRequest.offset}" }
        require(fileListRequest.limit > 0) { "limit must be > 0, was ${fileListRequest.limit}" }

        return withContext(Dispatchers.IO) {
            val resolved = resolveExisting(fileListRequest.path)
            if (!Files.isDirectory(resolved)) {
                throw IllegalArgumentException("Not a directory: ${fileListRequest.path}")
            }

            val filterPredicate = PathFilterParser.parse(fileListRequest.filter, hideDotFilesByDefault = true)

            Files.list(resolved).use { stream ->
                stream
                    .filter(filterPredicate)
                    .sorted(
                        compareByDescending<Path> { it.isDirectory() }
                            .thenBy { it.fileName.toString().lowercase(Locale.ROOT) }
                    )
                    .skip(fileListRequest.offset.toLong())
                    .limit(fileListRequest.limit.toLong())
                    .map { path ->
                        FileItem(
                            name = path.fileName.toString(),
                            path = path.toString(),
                            size = if (path.isRegularFile()) path.fileSize() else 0L,
                            isDirectory = path.isDirectory(),
                            createdAt = path.getAttribute("creationTime")?.toString() ?: "unknown",
                            lastModified = path.getLastModifiedTime().toString()
                        )
                    }
                    .toList()
            }
        }
    }

    override suspend fun createFile(
        relativePath: String,
        isDirectory: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val target = resolveForCreate(relativePath)

            if (isDirectory) {
                Files.createDirectory(target)
            } else {
                Files.createFile(target)
            }
            true
        } catch (_: IOException) {
            false
        }
    }

    override suspend fun copyFile(
        sourcePath: String,
        destinationPath: String,
        overwrite: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val source = resolveExisting(sourcePath)
            val target = resolveExisting(destinationPath)

            if (source.isDirectory() && target.startsWith(source)) {
                throw IllegalArgumentException("Cannot copy a directory into itself")
            }

            if (source.isDirectory()) {
                source.toFile().copyRecursively(target.toFile(), overwrite = overwrite)
            } else {
                val options = if (overwrite) {
                    arrayOf(StandardCopyOption.REPLACE_EXISTING)
                } else {
                    emptyArray()
                }
                Files.copy(source, target, *options)
            }
            true
        } catch (_: SecurityException) {
            throw SecurityException("Action denied")
        } catch (_: IOException) {
            false
        }
    }

    override suspend fun renameFile(
        oldPath: String,
        newPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val source = resolveExisting(oldPath)
            val target = resolveForCreate(newPath)

            if (source.isDirectory() && target.startsWith(source)) {
                throw IllegalArgumentException("Cannot move a directory into itself")
            }

            try {
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: AtomicMoveNotSupportedException) {
                // Если диски разные, делаем обычный move (с копированием)
                Files.move(source, target)
            }
            true
        } catch (_: SecurityException) {
            throw SecurityException("Action denied")
        } catch (_: IOException) {
            false
        }
    }

    override suspend fun deleteFile(
        relativePath: String,
        recursive: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val resolved = resolveExisting(relativePath)

            if (resolved.isDirectory() && !recursive) {
                Files.delete(resolved)
            } else if (resolved.isDirectory() && recursive) {
                resolved.toFile().deleteRecursively()
            } else {
                Files.deleteIfExists(resolved)
            }
            true
        } catch (_: SecurityException) {
            throw SecurityException("Action denied")
        } catch (_: IOException) {
            false
        }
    }

    private fun resolveForCreate(relativePath: String): Path {
        val rel = Path.of(relativePath)
        require(!rel.isAbsolute) { "Relative path expected, got absolute: $relativePath" }
        require(rel.fileName != null) { "Invalid path: $relativePath" }

        val parent = resolveExisting(rel.parent?.toString() ?: ".")
        if (!Files.isDirectory(parent)) {
            throw IllegalArgumentException("Parent is not a directory: $relativePath")
        }

        return parent.resolve(rel.fileName)
    }
    private fun resolveExisting(relativePath: String): Path {
        val path = Path.of(relativePath)

        val candidate = if (path.isAbsolute) {
            path.normalize()
        } else {
            canonicalRoot.resolve(path).normalize()
        }

        if (!candidate.startsWith(canonicalRoot)) {
            throw SecurityException("Access denied: path escapes root: $relativePath")
        }

        val real = candidate.toRealPath()
        if (!real.startsWith(canonicalRoot)) {
            throw SecurityException("Access denied: symlink escapes root: $relativePath")
        }
        return real
    }
}