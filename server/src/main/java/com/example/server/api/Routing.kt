package com.example.server.api

import com.example.server.domain.FileService
import com.google.gson.Gson
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

fun Application.configureRouting(
    fileService: FileService,
    gson: Gson
) {
    routing {
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
//        get("/") {
//            val relativePath = call.request.queryParameters["path"]
//
//            if (relativePath == null) {
//                call.respondText("Error! Parameter 'path' is missing")
//                return@get
//            }
//
//            try {
//                call.respond(fileService.getFiles(relativePath))
//            } catch (e: Exception) {
//                call.respond("Error: ${e.message}")
//            }
//        }
    }
}

// curl -X GET "http://localhost:8081/?path=/home/ni/Downloads"