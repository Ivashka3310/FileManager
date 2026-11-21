package com.example.filemanager.data.remote

import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem
import com.example.shared.requests.CopyFileRequest
import com.example.shared.requests.CreateFileRequest
import com.example.shared.requests.RenameFileRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import java.io.File

class FileRemoteDataSourceImpl(
    private val client: HttpClient
) : FileRemoteDataSource {
    override suspend fun login(url: String, secret: String): String {
        val response = client.post("$url/login") {
            setBody(FormDataContent(Parameters.build { append("secret", secret) }))
        }

        if (response.status != HttpStatusCode.OK) {
            error("Auth failed with status: ${response.status}")
        }

        val body: Map<String, String> = response.body()
        val token = body["token"] ?: error("Token not found in response")
        return token
    }

    override suspend fun getLocalDisks(url: String, token: String): List<DiskInfo> {
        val response = client.get("$url/disks") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Unauthorized -> throw Exception("Session expired")
            else -> throw Exception("Server error: ${response.status}")
        }
    }

    override suspend fun getFiles(
        url: String,
        token: String,
        path: String,
        offset: Int,
        limit: Int,
        filter: String?
    ): List<FileItem> {
        val response = client.get("$url/files/list") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("path", path)
            parameter("offset", offset)
            parameter("limit", limit)
            filter?.let { parameter("filter", it) }
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Forbidden -> throw SecurityException(response.errorMessage("Access denied"))
            HttpStatusCode.NotFound -> throw NoSuchFileException(File(path))
            HttpStatusCode.BadRequest -> throw IllegalArgumentException(response.errorMessage("Bad request"))
            else -> error("Server error: ${response.status}")
        }
    }

    override suspend fun createFile(url: String, token: String, path: String, isDirectory: Boolean): Boolean {
        val response = client.post("$url/files/create") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateFileRequest(path, isDirectory))
        }
        return handleActionResponse(response)
    }

    override suspend fun copyFile(
        url: String,
        token: String,
        sourcePath: String,
        destinationPath: String,
        overwrite: Boolean
    ): Boolean {
        val response = client.post("$url/files/copy") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CopyFileRequest(sourcePath, destinationPath, overwrite))
        }
        return handleActionResponse(response)
    }

    override suspend fun renameFile(
        url: String,
        token: String,
        oldPath: String,
        newPath: String
    ): Boolean {
        val response = client.put("$url/files/rename") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(RenameFileRequest(oldPath, newPath))
        }
        return handleActionResponse(response)
    }

    override suspend fun deleteFile(
        url: String,
        token: String,
        path: String,
        recursive: Boolean
    ): Boolean {
        val response = client.delete("$url/files/delete") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("path", path)
            parameter("recursive", recursive.toString())
        }
        return response.status == HttpStatusCode.NoContent
    }

    private suspend fun handleActionResponse(response: HttpResponse): Boolean {
        return when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.Created -> true
            HttpStatusCode.Forbidden -> throw SecurityException(response.errorMessage("Access denied"))
            HttpStatusCode.NotFound -> throw NoSuchFileException(File("unknown"))
            HttpStatusCode.Conflict -> throw IllegalStateException(response.errorMessage("File already exists"))
            HttpStatusCode.BadRequest -> throw IllegalArgumentException(response.errorMessage("Bad request"))
            else -> error("Action failed: ${response.status}")
        }
    }

    private suspend fun HttpResponse.errorMessage(fallback: String): String =
        runCatching { body<Map<String, String>>()["message"] }.getOrNull() ?: fallback
}