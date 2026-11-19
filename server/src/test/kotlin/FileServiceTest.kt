package com.example.server.data

import com.example.shared.requests.FileListRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FileServiceImplTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var root: Path
    private lateinit var service: FileServiceImpl

    @BeforeEach
    fun setUp() {
        root = tempDir.resolve("root").also { Files.createDirectories(it) }
        service = FileServiceImpl(root)
    }

    private fun createFile(relative: String, content: String = "data"): Path {
        val path = root.resolve(relative)
        path.parent?.let { Files.createDirectories(it) }
        Files.writeString(path, content)
        return path
    }

    private fun createDir(relative: String): Path {
        val path = root.resolve(relative)
        Files.createDirectories(path)
        return path
    }

    private fun assumeSymlinksSupported() {
        assumeTrue(
            !System.getProperty("os.name").lowercase().contains("windows"),
            "Symlinks require Linux/macOS or Windows Developer Mode"
        )
    }

    private fun listRequest(
        path: String = ".",
        offset: Int = 0,
        limit: Int = 50,
        filter: String? = null
    ) = FileListRequest(
        path = path,
        offset = offset,
        limit = limit,
        filter = filter,
        extension = null,
        nameQuery = null,
        orderBy = null
    )

    // ---------------------------------------------------------------- getFiles

    @Test
    fun `getFiles returns files and directories`(): Unit = runBlocking {
        createFile("a.txt")
        createFile("b.txt")
        createDir("subdir")

        val result = service.getFiles(listRequest(limit = 10))

        assertEquals(3, result.size)
        assertTrue(result.any { it.name == "a.txt" && !it.isDirectory })
        assertTrue(result.any { it.name == "b.txt" && !it.isDirectory })
        assertTrue(result.any { it.name == "subdir" && it.isDirectory })
    }

    @Test
    fun `getFiles sorts directories before files`(): Unit = runBlocking {
        createFile("aaa.txt")
        createDir("zzz")

        val result = service.getFiles(listRequest(limit = 10))

        assertEquals("zzz", result.first().name)
        assertTrue(result.first().isDirectory)
    }

    @Test
    fun `getFiles sorts case-insensitively`(): Unit = runBlocking {
        createFile("Banana.txt")
        createFile("apple.txt")
        createFile("Cherry.txt")

        val result = service.getFiles(listRequest(limit = 10))

        assertEquals(listOf("apple.txt", "Banana.txt", "Cherry.txt"), result.map { it.name })
    }

    @Test
    fun `getFiles applies offset and limit`(): Unit = runBlocking {
        (1..10).forEach { createFile("file$it.txt") }

        val page1 = service.getFiles(listRequest(offset = 0, limit = 3))
        val page2 = service.getFiles(listRequest(offset = 3, limit = 3))

        assertEquals(3, page1.size)
        assertEquals(3, page2.size)
        assertTrue(page1.map { it.name }.toSet().intersect(page2.map { it.name }.toSet()).isEmpty())
    }

    @Test
    fun `getFiles returns correct metadata for files`(): Unit = runBlocking {
        createFile("data.txt", content = "hello world")

        val result = service.getFiles(listRequest(limit = 10))
        val file = result.single { it.name == "data.txt" }

        assertEquals("hello world".length.toLong(), file.size)
        assertFalse(file.isDirectory)
        assertTrue(file.lastModified != "unknown")
    }

    @Test
    fun `getFiles reports zero size for directories`(): Unit = runBlocking {
        createDir("emptydir")

        val result = service.getFiles(listRequest(limit = 10))

        assertEquals(0L, result.single().size)
    }

    @Test
    fun `getFiles works for nested directory`(): Unit = runBlocking {
        createFile("sub/inner.txt")

        val result = service.getFiles(listRequest(path = "sub", limit = 10))

        assertEquals("inner.txt", result.single().name)
    }

    @Test
    fun `getFiles throws for non-existent path`(): Unit {
        assertFailsWith<NoSuchFileException> {
            runBlocking { service.getFiles(listRequest(path = "missing")) }
        }
    }

    @Test
    fun `getFiles throws when path is a file`(): Unit {
        createFile("notadir.txt")

        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.getFiles(listRequest(path = "notadir.txt")) }
        }
    }

    @Test
    fun `getFiles rejects negative offset`(): Unit {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.getFiles(listRequest(offset = -1)) }
        }
    }

    @Test
    fun `getFiles rejects zero limit`(): Unit {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.getFiles(listRequest(limit = 0)) }
        }
    }

    @Test
    fun `getFiles rejects path traversal with dotdot`(): Unit {
        assertFailsWith<SecurityException> {
            runBlocking { service.getFiles(listRequest(path = "../outside")) }
        }
    }

    @Test
    fun `getFiles rejects absolute path`(): Unit {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.getFiles(listRequest(path = "/etc")) }
        }
    }

    @Test
    fun `getFiles rejects symlink escaping root`(): Unit {
        assumeSymlinksSupported()
        val outside = tempDir.resolve("outside").also { Files.createDirectories(it) }
        Files.writeString(outside.resolve("secret.txt"), "secret")
        Files.createSymbolicLink(root.resolve("link"), outside)

        assertFailsWith<SecurityException> {
            runBlocking { service.getFiles(listRequest(path = "link")) }
        }
    }

    @Test
    fun `getFiles allows symlink inside root`(): Unit = runBlocking {
        assumeSymlinksSupported()
        createDir("real")
        createFile("real/file.txt")
        Files.createSymbolicLink(root.resolve("link"), root.resolve("real"))

        val result = service.getFiles(listRequest(path = "link", limit = 10))

        assertEquals("file.txt", result.single().name)
    }

    // ---------------------------------------------------------------- createFile

    @Test
    fun `createFile creates empty file`(): Unit = runBlocking {
        val created = service.createFile("new.txt", isDirectory = false)

        assertTrue(created)
        val path = root.resolve("new.txt")
        assertTrue(Files.exists(path))
        assertTrue(Files.isRegularFile(path))
        assertEquals(0L, Files.size(path))
    }

    @Test
    fun `createFile creates directory`(): Unit = runBlocking {
        val created = service.createFile("newdir", isDirectory = true)

        assertTrue(created)
        val path = root.resolve("newdir")
        assertTrue(Files.exists(path))
        assertTrue(Files.isDirectory(path))
    }

    @Test
    fun `createFile creates file in nested directory`(): Unit = runBlocking {
        createDir("sub")

        val created = service.createFile("sub/file.txt", isDirectory = false)

        assertTrue(created)
        assertTrue(Files.exists(root.resolve("sub/file.txt")))
    }

    @Test
    fun `createFile returns false when file already exists`(): Unit = runBlocking {
        createFile("existing.txt")

        val created = service.createFile("existing.txt", isDirectory = false)

        assertFalse(created)
    }

    @Test
    fun `createFile throws when parent directory does not exist`(): Unit {
        assertFailsWith<NoSuchFileException> {
            runBlocking { service.createFile("nonexistent/file.txt", isDirectory = false) }
        }
    }

    @Test
    fun `createFile rejects path traversal`(): Unit {
        assertFailsWith<SecurityException> {
            runBlocking { service.createFile("../outside.txt", isDirectory = false) }
        }
    }

    @Test
    fun `createFile rejects absolute path`(): Unit {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.createFile("/etc/passwd", isDirectory = false) }
        }
    }

    // ---------------------------------------------------------------- copyFile

    @Test
    fun `copyFile copies file`(): Unit = runBlocking {
        createFile("source.txt", "hello")

        val copied = service.copyFile("source.txt", "target.txt", overwrite = false)

        assertTrue(copied)
        assertEquals("hello", Files.readString(root.resolve("target.txt")))
        assertTrue(Files.exists(root.resolve("source.txt")))
    }

    @Test
    fun `copyFile fails when target exists and overwrite is false`(): Unit = runBlocking {
        createFile("source.txt", "a")
        createFile("target.txt", "b")

        val copied = service.copyFile("source.txt", "target.txt", overwrite = false)

        assertFalse(copied)
        assertEquals("b", Files.readString(root.resolve("target.txt")))
    }

    @Test
    fun `copyFile overwrites when requested`(): Unit = runBlocking {
        createFile("source.txt", "new")
        createFile("target.txt", "old")

        val copied = service.copyFile("source.txt", "target.txt", overwrite = true)

        assertTrue(copied)
        assertEquals("new", Files.readString(root.resolve("target.txt")))
    }

    @Test
    fun `copyFile copies directory recursively`(): Unit = runBlocking {
        createFile("src/inner/a.txt", "a")
        createFile("src/inner/b.txt", "b")

        val copied = service.copyFile("src", "dst", overwrite = false)

        assertTrue(copied)
        assertTrue(Files.exists(root.resolve("dst/inner/a.txt")))
        assertTrue(Files.exists(root.resolve("dst/inner/b.txt")))
    }

    @Test
    fun `copyFile rejects directory into itself`(): Unit {
        createDir("dir")

        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.copyFile("dir", "dir/sub", overwrite = false) }
        }
    }

    @Test
    fun `copyFile rejects source outside root`(): Unit {
        assertFailsWith<SecurityException> {
            runBlocking { service.copyFile("../victim.txt", "target.txt", overwrite = false) }
        }
    }

    @Test
    fun `copyFile throws when source does not exist`(): Unit {
        assertFailsWith<NoSuchFileException> {
            runBlocking { service.copyFile("ghost.txt", "target.txt", overwrite = false) }
        }
    }

    // ---------------------------------------------------------------- renameFile

    @Test
    fun `renameFile renames file`(): Unit = runBlocking {
        createFile("old.txt", "content")

        val renamed = service.renameFile("old.txt", "new.txt")

        assertTrue(renamed)
        assertFalse(Files.exists(root.resolve("old.txt")))
        assertEquals("content", Files.readString(root.resolve("new.txt")))
    }

    @Test
    fun `renameFile renames directory`(): Unit = runBlocking {
        createFile("olddir/inner.txt", "x")

        val renamed = service.renameFile("olddir", "newdir")

        assertTrue(renamed)
        assertFalse(Files.exists(root.resolve("olddir")))
        assertTrue(Files.exists(root.resolve("newdir/inner.txt")))
    }

    @Test
    fun `renameFile rejects moving directory into itself`(): Unit {
        createDir("dir")

        assertFailsWith<IllegalArgumentException> {
            runBlocking { service.renameFile("dir", "dir/sub") }
        }
    }

    // ---------------------------------------------------------------- deleteFile

    @Test
    fun `deleteFile removes existing file`(): Unit = runBlocking {
        val path = createFile("todelete.txt")

        val result = service.deleteFile("todelete.txt", recursive = false)

        assertTrue(result)
        assertFalse(Files.exists(path))
    }

    @Test
    fun `deleteFile throws for non-existent file`(): Unit {
        assertFailsWith<NoSuchFileException> {
            runBlocking { service.deleteFile("ghost.txt", recursive = false) }
        }
    }

    @Test
    fun `deleteFile removes empty directory`(): Unit = runBlocking {
        val dir = createDir("emptydir")

        val result = service.deleteFile("emptydir", recursive = false)

        assertTrue(result)
        assertFalse(Files.exists(dir))
    }

    @Test
    fun `deleteFile fails on non-empty directory without recursive`(): Unit = runBlocking {
        createFile("full/inner.txt")

        val result = service.deleteFile("full", recursive = false)

        assertFalse(result)
        assertTrue(Files.exists(root.resolve("full")))
    }

    @Test
    fun `deleteFile removes non-empty directory recursively`(): Unit = runBlocking {
        createFile("full/inner/a.txt")
        createDir("full/inner/sub")

        val result = service.deleteFile("full", recursive = true)

        assertTrue(result)
        assertFalse(Files.exists(root.resolve("full")))
    }

    @Test
    fun `deleteFile rejects path traversal`(): Unit {
        val victim = tempDir.resolve("victim.txt")
        Files.writeString(victim, "important")

        assertFailsWith<SecurityException> {
            runBlocking { service.deleteFile("../victim.txt", recursive = false) }
        }
        assertTrue(Files.exists(victim))
    }

    @Test
    fun `deleteFile rejects symlink escaping root`(): Unit {
        assumeSymlinksSupported()
        val outside = tempDir.resolve("outside").also { Files.createDirectories(it) }
        val victim = outside.resolve("victim.txt").also { Files.writeString(it, "important") }
        Files.createSymbolicLink(root.resolve("link"), outside)

        assertFailsWith<SecurityException> {
            runBlocking { service.deleteFile("link/victim.txt", recursive = false) }
        }
        assertTrue(Files.exists(victim))
    }

    // ---------------------------------------------------------------- getLocalDisks

    @Test
    fun `getLocalDisks returns at least one disk on Linux`(): Unit = runBlocking {
        val disks = service.getLocalDisks()

        assertTrue(disks.isNotEmpty(), "Должен быть хотя бы один диск")
    }

    @Test
    fun `getLocalDisks returns consistent values`(): Unit = runBlocking {
        val disks = service.getLocalDisks()

        for (disk in disks) {
            assertTrue(disk.mountPoint.startsWith("/"), "Путь должен быть абсолютным: ${disk.mountPoint}")
            assertTrue(disk.totalSpace > 0, "Нулевой объём у ${disk.mountPoint}")
            assertTrue(disk.usableSpace in 0..disk.totalSpace, "usable вне диапазона у ${disk.mountPoint}")
            assertTrue(disk.device.isNotBlank())
            assertTrue(disk.fsType.isNotBlank())
        }
    }

    @Test
    fun `getLocalDisks excludes pseudo filesystems`(): Unit = runBlocking {
        val pseudo = setOf("proc", "sysfs", "tmpfs", "devtmpfs", "cgroup", "cgroup2", "overlay")
        val disks = service.getLocalDisks()

        for (disk in disks) {
            assertFalse(disk.fsType in pseudo, "Псевдо-ФС ${disk.fsType} просочилась (${disk.mountPoint})")
        }
    }

    @Test
    fun `getLocalDisks has no duplicate mount points`(): Unit = runBlocking {
        val disks = service.getLocalDisks()
        val mountPoints = disks.map { it.mountPoint }

        assertEquals(mountPoints.size, mountPoints.toSet().size)
    }
}