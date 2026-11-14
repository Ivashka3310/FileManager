package com.example.filemanager.data

import android.util.Log
import com.example.filemanager.domain.ConnectionManager
import com.example.filemanager.domain.ServerResponse
import com.example.filemanager.domain.parseServerResponse
import com.example.shared.models.FileItem
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.gson.gson
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI

class ConnectionManagerImpl(private val client: HttpClient) : ConnectionManager {
    private val _messageFlow = MutableSharedFlow<ServerResponse>(
        replay = 0,
        extraBufferCapacity = 64
    )
    private var jwtToken: String? = null
    private var domain: String? = null

    override val messageFlow = _messageFlow.asSharedFlow()

    override suspend fun connectViaQrData(qrData: String) {
        if (!qrData.contains("secret=")) {
            throw Exception("Secret not found in QR")
        }

        try {
            val uri = URI(qrData)
            domain = "http://${uri.host ?: throw Exception("Invalid host in QR")}:${uri.port}"
            val secret = uri.query?.split('=')[1] ?: throw Exception("Secret not found")

            val response = client.post("$domain/login") {
                setBody(FormDataContent(Parameters.build {
                    append("secret", secret)
                }))
            }

            if (response.status != HttpStatusCode.OK) {
                throw Exception("Auth failed with status: ${response.status}")
            } else {
                _messageFlow.emit(ServerResponse.Status("Connecting..."))
            }

            val body = response.body<Map<String, String>>()
            val token = body["token"] ?: throw Exception("Token not found in response")
            this.jwtToken = token
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    val gson = Gson()


    override suspend fun requestFiles(path: String) {
        if (jwtToken.isNullOrEmpty()) throw Exception("Session expired")

        val response = client.get("$domain/files/list") {
            header("Authorization", "Bearer $jwtToken")
            setBody(FormDataContent(Parameters.build {
                append("path", path)
            }))
        }
        when (response.status) {
            HttpStatusCode.OK -> {
                val files = response.body<List<FileItem>>()
                Log.d("RRR", files.toString())
                _messageFlow.emit(ServerResponse.FileList(files))
            }
            HttpStatusCode.Locked -> {
                val message = response.body<Pair<String, String>>().second
                _messageFlow.emit(ServerResponse.Status("ERROR: $message"))
            }
            else -> {
                _messageFlow.emit(ServerResponse.Status("Unknown error!"))
            }
        }
    }
}