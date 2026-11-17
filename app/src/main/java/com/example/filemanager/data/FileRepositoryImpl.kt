package com.example.filemanager.data

import io.ktor.http.ContentType
import com.example.filemanager.domain.FileRepository
import com.example.filemanager.domain.SessionStorage
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
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.io.File
import java.net.URI

class FileRepositoryImpl(
    private val client: HttpClient,
    private val sessionStorage: SessionStorage
) : FileRepository {

    override suspend fun connectViaQrData(qrData: String): Result<Unit> = runCatching {
        val uri = URI(qrData)
        val host = uri.host ?: error("Invalid host in QR: $qrData")
        val secret = uri.query
            ?.split('&')
            ?.mapNotNull { it.split('=', limit = 2).takeIf { p -> p.size == 2 } }
            ?.firstOrNull { it[0] == "secret" }
            ?.get(1)
            ?: error("Secret not found in QR: $qrData")

        val port = if (uri.port == -1) 80 else uri.port
        val url = "http://$host:$port"
        sessionStorage.saveUrl(url)

        val response = client.post("$url/login") {
            setBody(FormDataContent(Parameters.build { append("secret", secret) }))
        }

        if (response.status != HttpStatusCode.OK) {
            error("Auth failed with status: ${response.status}")
        }

        val body: Map<String, String> = response.body()
        val token = body["token"] ?: error("Token not found in response")
        sessionStorage.saveToken(token)
    }

    override suspend fun getLocalDisks(): List<DiskInfo> {
        val (url, token) = getSession()

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
        path: String,
        offset: Int,
        limit: Int,
        filter: String?
    ): List<FileItem> {
        val (url, token) = getSession()

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

    override suspend fun createFile(path: String, isDirectory: Boolean): Boolean {
        val (url, token) = getSession()
        val response = client.post("$url/files/create") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CreateFileRequest(path, isDirectory))
        }
        return handleActionResponse(response)
    }

    override suspend fun copyFile(sourcePath: String, destinationPath: String, overwrite: Boolean): Boolean {
        val (url, token) = getSession()
        val response = client.post("$url/files/copy") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(CopyFileRequest(sourcePath, destinationPath, overwrite))
        }
        return handleActionResponse(response)
    }

    override suspend fun renameFile(oldPath: String, newPath: String): Boolean {
        val (url, token) = getSession()
        val response = client.put("$url/files/rename") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(RenameFileRequest(oldPath, newPath))
        }
        return handleActionResponse(response)
    }

    override suspend fun deleteFile(path: String, recursive: Boolean): Boolean {
        val (url, token) = getSession()
        val response = client.delete("$url/files/delete") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("path", path)
            parameter("recursive", recursive.toString())
        }
        // Для удаления сервер возвращает 204 No Content
        return response.status == HttpStatusCode.NoContent
    }

    // Вспомогательные методы для избежания дублирования
    private suspend fun getSession(): Pair<String, String> {
        val url = sessionStorage.url.first()
        val token = sessionStorage.accessToken.first()
        if (url.isNullOrEmpty() || token.isNullOrEmpty()) error("Session expired")
        return url to token
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