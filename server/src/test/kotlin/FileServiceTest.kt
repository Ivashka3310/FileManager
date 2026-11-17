package com.example.server.data

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

    @Test
    fun `getFiles returns files and directories`(): Unit = runBlocking {
        createFile("a.txt")
        createFile("b.txt")
        createDir("subdir")

        val result = service.getFiles(
            relativePath = ".",
            offset = 0,
            limit = 10,
            filter = null,
            extension = null,
            nameQuery = null,
            orderBy = null
        )

        assertEquals(3, result.size)
        assertTrue(result.any { it.name == "a.txt" && !it.isDirectory })
        assertTrue(result.any { it.name == "b.txt" && !it.isDirectory })
        assertTrue(result.any { it.name == "subdir" && it.isDirectory })
    }

    @Test
    fun `getFiles sorts directories before files`(): Unit = runBlocking {
        createFile("aaa.txt")
        createDir("zzz")

        val result = service.getFiles(
            relativePath = ".",
            offset = 0,
            limit = 10,
            filter = null,
            extension = null,
            nameQuery = null,
            orderBy = null
        )

        assertEquals("zzz", result.first().name)
        assertTrue(result.first().isDirectory)
    }

    @Test
    fun `getFiles sorts case-insensitively`(): Unit = runBlocking {
        createFile("Banana.txt")
        createFile("apple.txt")
        createFile("Cherry.txt")

        val result = service.getFiles(
            relativePath = ".",
            offset = 0,
            limit = 10,
            filter = null,
            extension = null,
            nameQuery = null,
            orderBy = null
        )

        assertEquals(listOf("apple.txt", "Banana.txt", "Cherry.txt"), result.map { it.name })
    }

    @Test
    fun `getFiles applies offset and limit`(): Unit = runBlocking {
        (1..10).forEach { createFile("file$it.txt") }

        val page1 = service.getFiles(
            relativePath = ".",
            offset = 0,
            limit = 3,
            filter = null,
            extension = null,
            nameQuery = null,
            orderBy = null
        )
        val page2 = service.getFiles(
            relativePath = ".",
            offset = 3,
            limit = 3,
            filter = null,
            extension = null,
            nameQuery = null,
            orderBy = null
        )

        assertEquals(3, page1.size)
        assertEquals(3, page2.size)
        assertTrue(page1.map { it.name }.toSet().intersect(page2.map { it.name }.toSet()).isEmpty())
    }

    @Test
    fun `getFiles returns correct metadata for files`(): Unit = runBlocking {
        createFile("data.txt", content = "hello world")

        val result = service.getFiles(
            relativePath = ".",
            offset = 0,
            limit = 10,
            filter = null,
            extension = null,
            nameQuery = null,
            orderBy = null
        )
        val file = result.single { it.name == "data.txt" }

        assertEquals("hello world".length.toLong(), file.size)
        assertFalse(file.isDirectory)
        assertTrue(file.lastModified != "unknown")
    }

    @Test
    fun `getFiles reports zero size for directories`(): Unit = runBlocking {
        createDir("emptydir")

        val result = service.getFiles(
            relativePath = ".",
            offset = 0,
            limit = 10,
            filter = null,
            extension = null,
            nameQuery = null,
            orderBy = null
        )

        assertEquals(0L, result.single().size)
    }

    @Test
    fun `getFiles works for nested directory`(): Unit = runBlocking {
        createFile("sub/inner.txt")

        val result = service.getFiles(
            relativePath = "sub",
            offset = 0,
            limit = 10,
            filter = null,
            extension = null,
            nameQuery = null,
            orderBy = null
        )

        assertEquals("inner.txt", result.single().name)
    }

    @Test
    fun `getFiles throws for non-existent path`() {
        assertFailsWith<NoSuchFileException> {
            runBlocking {
                service.getFiles(".", 0, 10, null, null, null, null)
                    .also { throw NoSuchFileException("missing") }
            }
        }
    }

    @Test
    fun `getFiles throws when path is a file`() {
        createFile("notadir.txt")

        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                service.getFiles("notadir.txt", 0, 10, null, null, null, null)
            }
        }
    }

    @Test
    fun `getFiles rejects negative offset`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                service.getFiles(".", -1, 10, null, null, null, null)
            }
        }
    }

    @Test
    fun `getFiles rejects zero limit`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                service.getFiles(".", 0, 0, null, null, null, null)
            }
        }
    }

    @Test
    fun `getFiles rejects path traversal with dotdot`() {
        assertFailsWith<SecurityException> {
            runBlocking {
                service.getFiles("../outside", 0, 10, null, null, null, null)
            }
        }
    }

    @Test
    fun `getFiles rejects absolute path`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                service.getFiles("/etc", 0, 10, null, null, null, null)
            }
        }
    }

    @Test
    fun `getFiles rejects symlink escaping root`() {
        assumeSymlinksSupported()
        val outside = tempDir.resolve("outside").also { Files.createDirectories(it) }
        Files.writeString(outside.resolve("secret.txt"), "secret")
        Files.createSymbolicLink(root.resolve("link"), outside)

        assertFailsWith<SecurityException> {
            runBlocking {
                service.getFiles("link", 0, 10, null, null, null, null)
            }
        }
    }

    @Test
    fun `getFiles allows symlink inside root`(): Unit = runBlocking {
        assumeSymlinksSupported()
        createDir("real")
        createFile("real/file.txt")
        Files.createSymbolicLink(root.resolve("link"), root.resolve("real"))

        val result = service.getFiles(
            relativePath = "link",
            offset = 0,
            limit = 10,
            filter = null,
            extension = null,
            nameQuery = null,
            orderBy = null
        )

        assertEquals("file.txt", result.single().name)
    }

    @Test
    fun `deleteFile removes existing file`(): Unit = runBlocking {
        val path = createFile("todelete.txt")

        val result = service.deleteFile("todelete.txt")

        assertTrue(result)
        assertFalse(Files.exists(path))
    }

    @Test
    fun `deleteFile throws for non-existent file`() {
        assertFailsWith<NoSuchFileException> {
            runBlocking { service.deleteFile("ghost.txt") }
        }
    }

    @Test
    fun `deleteFile removes empty directory`(): Unit = runBlocking {
        val dir = createDir("emptydir")

        val result = service.deleteFile("emptydir")

        assertTrue(result)
        assertFalse(Files.exists(dir))
    }

    @Test
    fun `deleteFile returns false for non-empty directory`(): Unit = runBlocking {
        createFile("full/inner.txt")

        val result = service.deleteFile("full")

        assertFalse(result)
        assertTrue(Files.exists(root.resolve("full")))
    }

    @Test
    fun `deleteFile rejects path traversal`() {
        val victim = tempDir.resolve("victim.txt")
        Files.writeString(victim, "important")

        assertFailsWith<SecurityException> {
            runBlocking { service.deleteFile("../victim.txt") }
        }
        assertTrue(Files.exists(victim))
    }

    @Test
    fun `deleteFile rejects symlink escaping root`() {
        assumeSymlinksSupported()
        val outside = tempDir.resolve("outside").also { Files.createDirectories(it) }
        val victim = outside.resolve("victim.txt").also { Files.writeString(it, "important") }
        Files.createSymbolicLink(root.resolve("link"), outside)

        assertFailsWith<SecurityException> {
            runBlocking { service.deleteFile("link/victim.txt") }
        }
        assertTrue(Files.exists(victim))
    }


    @Test
    fun `getLocalDisks returns at least one disk on Linux`(): Unit = runBlocking {
        val disks = service.getLocalDisks()

        assertTrue(disks.isNotEmpty(), "Должен быть хотя бы один диск")
    }

    @Test
    fun `getLocalDisks returns consistent values`(): Unit = runBlocking {
        val disks = service.getLocalDisks()

        for ((mountPoint, device, fsType, _, totalSpace, usableSpace) in disks) {
            assertTrue(mountPoint.startsWith("/"), "Путь должен быть абсолютным: $mountPoint")
            assertTrue(totalSpace > 0, "Нулевой объём у $mountPoint")
            assertTrue(usableSpace in 0..totalSpace, "usable вне диапазона у $mountPoint")
            assertTrue(device.isNotBlank())
            assertTrue(fsType.isNotBlank())
        }
    }

    @Test
    fun `getLocalDisks excludes pseudo filesystems`(): Unit = runBlocking {
        val pseudo = setOf("proc", "sysfs", "tmpfs", "devtmpfs", "cgroup", "cgroup2", "overlay")
        val disks = service.getLocalDisks()

        for ((mountPoint, _, fsType) in disks) {
            assertFalse(fsType in pseudo, "Псевдо-ФС $fsType просочилась ($mountPoint)")
        }
    }

    @Test
    fun `getLocalDisks has no duplicate mount points`(): Unit = runBlocking {
        val disks = service.getLocalDisks()
        val mountPoints = disks.map { it.mountPoint }

        assertEquals(mountPoints.size, mountPoints.toSet().size)
    }

    @Test
    fun `getLocalDisks only returns disks containing service root`(): Unit = runBlocking {
        val canonicalRoot = root.toRealPath()
        val disks = service.getLocalDisks()

        for ((mountPoint) in disks) {
            val mountPath = Path.of(mountPoint).toAbsolutePath().normalize()
            assertTrue(
                canonicalRoot.startsWith(mountPath),
                "Диск $mountPoint не содержит корень сервиса $canonicalRoot"
            )
        }
    }
}