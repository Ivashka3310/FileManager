package com.example.server.data

import com.example.server.domain.FileService
import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem
import com.example.shared.requests.FileListRequest
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
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
        val target = resolveForCreate(relativePath)

        try {
            if (isDirectory) Files.createDirectory(target) else Files.createFile(target)
            true
        } catch (_: FileAlreadyExistsException) {
            false
        } catch (e: NoSuchFileException) {
            throw e
        } catch (_: IOException) {
            false
        }
    }

    override suspend fun copyFile(
        sourcePath: String,
        destinationPath: String,
        overwrite: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val source = resolveExisting(sourcePath)
        val target = resolveForCreate(destinationPath)

        try {

            if (source.isDirectory() && target.startsWith(source)) {
                throw IllegalArgumentException("Cannot copy a directory into itself")
            }

            if (source.isDirectory()) {
                val success = source.toFile().copyRecursively(target.toFile(), overwrite = overwrite)
                if (!success) throw IOException("Failed to copy directory")
            } else {
                val options = if (overwrite) arrayOf(StandardCopyOption.REPLACE_EXISTING) else emptyArray()
                Files.copy(source, target, *options)
            }
            true
        } catch (e: FileAlreadyExistsException) {
            false
        } catch (_: SecurityException) {
            throw SecurityException("Action denied")
        } catch (e: IOException) {
            if (e is NoSuchFileException) throw e
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

    override suspend fun deleteFile(relativePath: String, recursive: Boolean): Boolean = withContext(Dispatchers.IO) {
        val resolved = resolveExisting(relativePath)

        try {
            if (resolved.isDirectory()) {
                if (!recursive) {
                    Files.delete(resolved)
                } else {
                    Files.walkFileTree(resolved, object : SimpleFileVisitor<Path>() {
                        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                            Files.delete(file)
                            return FileVisitResult.CONTINUE
                        }

                        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                            if (exc != null) throw exc
                            Files.delete(dir)
                            return FileVisitResult.CONTINUE
                        }
                    })
                }
            } else {
                Files.deleteIfExists(resolved)
            }
            true
        } catch (_: IOException) {
            false
        }
    }

    @Suppress("CheckResult")
    override suspend fun getJsonContent(relativePath: String): String = withContext(Dispatchers.IO) {
        val resolved = resolveExisting(relativePath)

        if (!Files.isRegularFile(resolved)) {
            throw IllegalArgumentException("Not a file: $relativePath")
        }

        val size = Files.size(resolved)
        if (size > 5 * 1024 * 1024) {
            throw IllegalArgumentException("File too large to display")
        }

        val content = Files.readString(resolved)

        try {
            JsonParser.parseString(content)
        } catch (e: JsonSyntaxException) {
            throw IllegalArgumentException("Invalid JSON format", e)
        }

        content
    }

    override suspend fun getXmlContent(relativePath: String): String = withContext(Dispatchers.IO) {
        val resolved = resolveExisting(relativePath)

        if (!Files.isRegularFile(resolved)) {
            throw IllegalArgumentException("Not a file: $relativePath")
        }

        val size = Files.size(resolved)
        if (size > 5 * 1024 * 1024) {
            throw IllegalArgumentException("File too large to display")
        }

        val content = Files.readString(resolved)

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            factory.isXIncludeAware = false
            factory.isExpandEntityReferences = false

            val builder = factory.newDocumentBuilder()
            builder.parse(InputSource(StringReader(content)))
        } catch (e: SAXException) {
            throw IllegalArgumentException("Invalid XML format", e)
        }

        content
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
        require(!path.isAbsolute) { "Relative path expected, got absolute: $relativePath" }

        val candidate = canonicalRoot.resolve(path).normalize()
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