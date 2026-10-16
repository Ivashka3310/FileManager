package com.example.server.api

import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

fun main() {
    embeddedServer(CIO, port = 8081, host = "0.0.0.0") {
        configureSerialization()
        configureSecurity()
        configureRouting()
    }.start(wait = true)
}