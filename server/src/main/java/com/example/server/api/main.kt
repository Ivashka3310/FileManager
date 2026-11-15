package com.example.server.api

import com.example.server.api.security.AuthManager
import com.example.server.api.security.JwtService
import com.example.server.api.security.configureSecurity
import com.example.server.data.FileServiceImpl
import com.google.gson.Gson
import io.ktor.server.application.Application
import io.ktor.server.websocket.WebSockets
import io.ktor.server.application.install
import io.ktor.server.cio.EngineMain
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds
import io.ktor.serialization.gson.gson
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation


fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val secretKey = AuthManager.generateNewSecret()
    val serverIp = environment.config.propertyOrNull("ktor.deployment.ip")?.getString() ?: "localhost"
    val serverPort = environment.config.propertyOrNull("ktor.deployment.port")?.getString() ?: "8888"
    val qrData = "http://$serverIp:$serverPort/login?secret=$secretKey"

    ProcessBuilder("qrencode", "-t", "ansiutf8", qrData)
        .inheritIO()
        .start()
        .waitFor()
    println("Server is working! Scan the QR code to connect")

    val jwtService = JwtService(
        audience = environment.config.propertyOrNull("jwt.audience")?.getString() ?: "default-audience",
        issuer = environment.config.propertyOrNull("jwt.issuer")?.getString() ?: "file.manager.issuer-default",
        secret = environment.config.propertyOrNull("jwt.secret")?.getString() ?: "fallback-secret-key",
        validityMs = environment.config.propertyOrNull("jwt.validityMs")?.getString()?.toLong() ?: 604800000L
    )

    install(ContentNegotiation) {
        gson()
    }

    configureSecurity(jwtService)

    val fileService = FileServiceImpl(
        rootPath = Path(environment.config.propertyOrNull("storage.rootPath")?.getString() ?: "/")
    )
    configureRouting(fileService, jwtService)
}