package com.example.server.api

import com.example.server.api.security.AuthManager
import com.example.server.api.security.JwtService
import com.example.server.domain.FileService
import com.example.shared.FileListRequest
import com.google.gson.Gson
import io.ktor.client.request.request
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

fun Application.configureRouting(
    fileService: FileService,
    jwtService: JwtService
) {
    routing {
        authenticate("auth-jwt") {
            get("/files/list") {
                val fileListRequest = try {
                    call.receive<FileListRequest>()
                } catch (_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("ERROR" to "Invalid JSON body"))
                    return@get
                }

                try {
                    val files = fileService.getFiles(
                        relativePath = fileListRequest.path,
                        offset = fileListRequest.offset,
                        limit = fileListRequest.limit
                    )
                    call.respond(HttpStatusCode.OK, files)
                } catch (e: SecurityException) {
                    call.respond(HttpStatusCode.Locked, mapOf("message" to e.message))
                }
            }

            delete("/files/delete") {

            }

            get("/files/download") {

            }

            post("/files/copy") {

            }
        }

        post("/login") {
            val providedSecret = call.receiveParameters()["secret"] ?: ""

            if (AuthManager.consumeSecret(providedSecret)) {
                val token = jwtService.generateToken()
                call.respond(mapOf("token" to token))
                println("Authorized")
                AuthManager.tempQrSecret = null
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid secret key!")
            }
        }
    }
}