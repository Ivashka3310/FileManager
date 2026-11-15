package com.example.filemanager

import android.annotation.SuppressLint
import com.example.shared.models.FileItem
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    private val gson = Gson()

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun json_parsing_isCorrect() {
        val src = """
        {
            "name" : "Test file name",
            "path" : "/test/path",
            "size" : 10,
            "isDirectory" : false,
            "createdAt": "2023-10-27T10:00:00Z",
            "lastModified": "2023-10-28T12:00:00Z"
        }
        """.trimIndent()

        val fileItem = gson.fromJson(src, FileItem::class.java)

        assertEquals("Test file name", fileItem.name)
        assertEquals("/test/path", fileItem.path)
        assertEquals(10L, fileItem.size)
        assertEquals(false, fileItem.isDirectory)
        assertEquals("2023-10-27T10:00:00Z", fileItem.createdAt)
        assertEquals("2023-10-28T12:00:00Z", fileItem.lastModified)
    }

    @Test
    fun json_parsing_isIncorrect() {
        val src = """
        {
            "name" : "Test file name",
            "path" : "/test/path",
            "size" : 10,
            "isDirectory" : false,
            "createdAt": "2023-10-27T10:00:00Z",
            "lastModified": "2023-10-28T12:00:00Z"
        }
        """.trimIndent()

        val fileItem = gson.fromJson(src, FileItem::class.java)

        assertNotEquals("Test name", fileItem.name)
        assertNotEquals("/test/Path", fileItem.path)
        assertEquals(10, fileItem.size)
        assertNotEquals(true, fileItem.isDirectory)
        assertEquals("2023-10-27T10:00:00Z", fileItem.createdAt)
        assertEquals("2023-10-28T12:00:00Z", fileItem.lastModified)
    }

    @SuppressLint("CheckResult")
    @Test
    fun src_isInvalid() {
        val listOfSource = listOf(
            """
            {
                "name" : "Test file name",
                "path" : "/test/path",
                "isDirectory" : false,
                "createdAt": "2023-10-27T10:00:00Z",
                "lastModified": "2023-10-28T12:00:00Z"
            }
            """.trimIndent(),
            """
            {
                "name" : "Test file name",
                "path" : "/test/path",
                "size" : 10,
                "createdAt": "2023-10-27T10:00:00Z",
                "lastModified": "2023-10-28T12:00:00Z"
            }
            """.trimIndent(),
            """
            {
                "name" : "Test file name",
                "path" : "/test/path",
                "isDirectory" : false
            }
            """.trimIndent(),
            """
            {
                "name" : "Test file name"
            }
            """.trimIndent(),
            """
            {
                "path" : "/test/path"
            }
            """.trimIndent(),
            """
            {
                "size" : 10
            }
            """.trimIndent(),
            """
            {}
            """.trimIndent(),
            """
            {
                "name" : "Test file name",
                "path" : "/test/path",
                "size" : "10",
                "isDirectory" : false,
                "createdAt": "2023-10-27T10:00:00Z",
                "lastModified": "2023-10-28T12:00:00Z",
                "excess field" : "string value"
            }
            """.trimIndent(),
            """
            {
                "name" : null,
                "path" : "/test/path",
                "size" : 10,
                "isDirectory" : false,
                "createdAt": "2023-10-27T10:00:00Z",
                "lastModified": "2023-10-28T12:00:00Z"
            }
            """.trimIndent(),
            """
            {
                "name" : "Test file name",
                "path" : "/test/path",
                "size" : null,
                "isDirectory" : false,
                "createdAt": "2023-10-27T10:00:00Z",
                "lastModified": "2023-10-28T12:00:00Z"
            }
            """.trimIndent()
        )

        for (src in listOfSource) {
            try {
                gson.fromJson(src, FileItem::class.java)
                assertTrue("Information unparsing: ", true)
            } catch (_: Exception) {
                fail("Program crashed!")
            }
        }
    }


    @Test
    fun list_json_parsing_isCorrect() {
        val src = """
        [
            {
                "name" : "Test file name",
                "path" : "/test/path",
                "size" : 10,
                "isDirectory" : false,
                "createdAt": "2023-10-27T10:00:00Z",
                "lastModified": "2023-10-28T12:00:00Z"
            },
            {
                "name" : "Test file name",
                "path" : "/test/path",
                "size" : 10,
                "isDirectory" : true,
                "createdAt": "2023-10-27T10:00:00Z",
                "lastModified": "2023-10-28T12:00:00Z"
            }
        ]
        """.trimIndent()

        val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, FileItem::class.java).type
        val fileItemList = gson.fromJson<List<FileItem>>(src, type)

        assertEquals("Test file name", fileItemList[0].name)
        assertEquals("/test/path", fileItemList[0].path)
        assertEquals(10L, fileItemList[0].size)
        assertEquals(false, fileItemList[0].isDirectory)
        assertEquals("2023-10-27T10:00:00Z", fileItemList[0].createdAt)
        assertEquals("2023-10-28T12:00:00Z", fileItemList[0].lastModified)

        assertEquals("Test file name", fileItemList[1].name)
        assertEquals("/test/path", fileItemList[1].path)
        assertEquals(10L, fileItemList[1].size)
        assertEquals(true, fileItemList[1].isDirectory)
        assertEquals("2023-10-27T10:00:00Z", fileItemList[1].createdAt)
        assertEquals("2023-10-28T12:00:00Z", fileItemList[1].lastModified)
    }

    @SuppressLint("CheckResult")
    @Test
    fun empty_list_parsing_isCorrect() {
        val src = "[]"

        try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, FileItem::class.java).type
            gson.fromJson<List<FileItem>>(src, type)
            assertTrue("Empty list successfully parsed", true)
        } catch (e: Exception) {
            fail(e.message)
        }
    }
}