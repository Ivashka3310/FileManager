package com.example.filemanager.data

import com.example.filemanager.domain.FileRepository
import com.example.filemanager.domain.SessionStorage
import com.example.shared.models.DiskInfo
import com.example.shared.models.FileItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
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

    override suspend fun getFiles(
        path: String,
        offset: Int,
        limit: Int,
        filter: String?
    ): List<FileItem> {
        val currentUrl = sessionStorage.url.first()
        val currentToken = sessionStorage.accessToken.first()

        if (currentUrl.isNullOrEmpty() || currentToken.isNullOrEmpty()) {
            error("Session expired")
        }

        val response = client.get("$currentUrl/files/list") {
            header(HttpHeaders.Authorization, "Bearer $currentToken")
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

    override suspend fun removeFile(relativePath: String): Flow<String> = flow {
        val currentUrl = sessionStorage.url.first()
        val currentToken = sessionStorage.accessToken.first()

        if (currentUrl.isNullOrEmpty() || currentToken.isNullOrEmpty()) {
            error("Session expired")
        }

        val response = client.delete("$currentUrl/files/delete") {
            header(HttpHeaders.Authorization, "Bearer $currentToken")
            parameter("path", relativePath)
        }

        when (response.status) {
            HttpStatusCode.NoContent -> emit(relativePath)
            HttpStatusCode.Forbidden -> throw SecurityException(response.errorMessage("Access denied"))
            HttpStatusCode.NotFound -> throw NoSuchFileException(File(relativePath))
            else -> error("Delete failed: ${response.status}")
        }
    }

    override suspend fun getLocalDisks(): List<DiskInfo> {
        val currentUrl = sessionStorage.url.first()
        val currentToken = sessionStorage.accessToken.first()
        if (currentUrl.isNullOrEmpty() || currentToken.isNullOrEmpty()) {
            throw Exception("Session expired")
        }

        val response = client.get("$currentUrl/disks") {
            header(HttpHeaders.Authorization, "Bearer $currentToken")
        }

        return when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.Unauthorized -> throw Exception("Session expired")
            else -> throw Exception("Server error: ${response.status}")
        }
    }

    private suspend fun HttpResponse.errorMessage(fallback: String): String =
        runCatching { body<Map<String, String>>()["message"] }.getOrNull() ?: fallback
}