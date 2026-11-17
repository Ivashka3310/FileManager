package com.example.server.data

import com.example.server.domain.FileService
import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
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

    override suspend fun getFiles(
        relativePath: String,
        offset: Int,
        limit: Int,
        filter: String?,
        extension: String?,
        nameQuery: String?,
        orderBy: String?
    ): List<FileItem> {
        require(offset >= 0) { "offset must be >= 0, was $offset" }
        require(limit > 0) { "limit must be > 0, was $limit" }

        return withContext(Dispatchers.IO) {
            val resolved = resolveExisting(relativePath)
            if (!Files.isDirectory(resolved)) {
                throw IllegalArgumentException("Not a directory: $relativePath")
            }

            val filterPredicate = PathFilterParser.parse(filter, hideDotFilesByDefault = true)

            Files.list(resolved).use { stream ->
                stream
                    .filter(filterPredicate)
                    .sorted(
                        compareByDescending<Path> { it.isDirectory() }
                            .thenBy { it.fileName.toString().lowercase(Locale.ROOT) }
                    )
                    .skip(offset.toLong())
                    .limit(limit.toLong())
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

    override suspend fun deleteFile(relativePath: String): Boolean = withContext(Dispatchers.IO) {
        val resolved = resolveExisting(relativePath)
        try {
            Files.deleteIfExists(resolved)
        } catch (_: IOException) {
            false
        }
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