package com.example.filemanager.data

import com.example.filemanager.domain.FileRepository
import com.example.filemanager.domain.SessionStorage
import com.example.shared.FileListRequest
import com.example.shared.models.FileItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.contentType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.net.URI

class FileRepositoryImpl(
    private val client: HttpClient,
    private val sessionStorage: SessionStorage
) : FileRepository {
    override suspend fun connectViaQrData(qrData: String): Result<Unit> = runCatching {
        if (!qrData.contains("secret=")) {
            throw Exception("Secret not found in QR")
        }

        val uri = URI(qrData)
        val host = uri.host ?: throw IllegalArgumentException("Invalid host in QR")
        val secret = uri.query?.split('=')[1] ?: throw Exception("Secret not found")
        val url = "http://$host:${uri.port}"
        sessionStorage.saveUrl(url)


        val response = client.post("$url/login") {
            setBody(FormDataContent(Parameters.build {
                append("secret", secret)
            }))
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Auth failed with status: ${response.status}")
        }

        val body = response.body<Map<String, String>>()

        body["token"]?.let { receivedToken ->
            sessionStorage.saveToken(receivedToken)
        } ?: throw Exception("Token not found in response")
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
            throw Exception("Session expired")
        }

        val response = client.get("$currentUrl/files/list") {
            header(HttpHeaders.Authorization, "Bearer $currentToken")
            contentType(ContentType.Application.Json)
            setBody(
                FileListRequest(
                    path = path,
                    offset = offset,
                    limit = limit,
                    filter = filter
                )
            )
        }

        return when(response.status) {
            HttpStatusCode.OK -> response.body<List<FileItem>>()
            HttpStatusCode.Locked -> {
                val message = response.body<Map<String, String>>()["message"] ?: "Folder is locked"
                throw Exception(message)
            }
            else -> throw Exception("Server error: ${response.status}")
        }
    }

    override suspend fun removeFile(relativePath: String): Flow<String> {
        TODO("Not yet implemented")
    }
}