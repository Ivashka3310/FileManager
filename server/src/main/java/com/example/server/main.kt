package com.example.server

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    embeddedServer(CIO, port=8081, host="0.0.0.0") {
        routing {
            get("/") {
                call.respondText("Cloud file manager is run!")
            }
        }
    }.start(wait = true)
}