package com.example.filemanager.data

import com.example.filemanager.domain.ConnectionManager
import com.example.filemanager.domain.ServerResponse
import com.example.shared.models.FileItem
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.forms.FormDataContent
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
import java.net.URI

class ConnectionManagerImpl : ConnectionManager {
    private val client = HttpClient {
        install(ContentNegotiation) { gson() }
        install(WebSockets)
    }

    private val _messageFlow = MutableSharedFlow<ServerResponse>(
        replay = 0,
        extraBufferCapacity = 64
    )
    private var jwtToken: String? = null
    private var session: DefaultClientWebSocketSession? = null
    private var connectionEstablished = kotlinx.coroutines.CompletableDeferred<Unit>()

    override val messageFlow = _messageFlow.asSharedFlow()

    override suspend fun connectViaQrData(qrData: String) {
        if (!qrData.contains("secret=")) {
            throw Exception("Secret not found in QR")
        }

        try {
            val uri = URI(qrData)
            val host = uri.host ?: throw Exception("Invalid host in QR")
            val port = uri.port
            val secret = uri.query?.split("=")?.getOrNull(1)
                ?: throw Exception("Secret not found in QR")

            val response = client.post("http://$host:$port/login") {
                setBody(FormDataContent(Parameters.build {
                    append("secret", secret)
                }))
            }

            if (response.status != HttpStatusCode.OK) {
                throw Exception("Auth failed with status: ${response.status}")
            } else {
                _messageFlow.emit(ServerResponse.Status("Сервер найден, устанавливаем подключение..."))
            }

            val body = response.body<Map<String, String>>()
            val token = body["token"] ?: throw Exception("Token not found in response")
            this.jwtToken = token

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    openWebSocket(host, port)
                } catch (e: Exception) {
                    connectionEstablished.completeExceptionally(e)
                }
            }

            connectionEstablished.await()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    val gson = Gson()
    private suspend fun openWebSocket(host: String, port: Int) {
        client.webSocket(
            method = HttpMethod.Get,
            host = host,
            port = port,
            path = "/files",
            request = {
                header(HttpHeaders.Authorization, "Bearer $jwtToken")
            }
        ) {
            session = this@webSocket
            connectionEstablished.complete(Unit)

            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    val response = when {
                        text.startsWith("SUCCESS") || text.startsWith("ERROR") -> {
                            ServerResponse.Status(text)
                        }
                        text.startsWith("[") -> {
                                try {
                                    val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, FileItem::class.java).type
                                    val files = gson.fromJson<List<FileItem>>(text, type)
                                    ServerResponse.FileList(files)
                                } catch (e: Exception) {
                                ServerResponse.Error("Parsing error: ${e.message}")
                            }
                        }
                        else -> ServerResponse.Status(text)
                    }
                    _messageFlow.emit(response)
                }
            }
        }
    }

    override suspend fun requestFiles(path: String) {
        session?.send(Frame.Text(path)) ?: throw Exception("Not connected to server")
    }
}