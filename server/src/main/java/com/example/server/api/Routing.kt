package com.example.server.api

import com.example.server.api.security.AuthManager
import com.example.server.api.security.JwtService
import com.example.server.domain.FileService
import com.google.gson.Gson
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

fun Application.configureRouting(
    fileService: FileService,
    gson: Gson,
    jwtService: JwtService
) {
    routing {
        authenticate("auth-jwt") {
            webSocket("/files") {
                try {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val receivedText = frame.readText()

                            try {
                                val files = fileService.getFiles(receivedText)
                                send(Frame.Text(gson.toJson(files)))
                            } catch (e: Exception) {
                                send(Frame.Text(gson.toJson(mapOf("Error" to e.message))))
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("WebSocket error: ${e.localizedMessage}")
                } finally {
                    println("Client disconnected")
                }
            }
        }

        post("/login") {
            val providedSecret = call.receiveParameters()["secret"] ?: ""
            if (AuthManager.consumeSecret(providedSecret)) {
                val token = jwtService.generateToken()
                call.respond(mapOf("token" to token))
                AuthManager.tempQrSecret = null
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid secret key!")
            }
        }
    }
}