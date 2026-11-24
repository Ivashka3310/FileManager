package com.example.server.api

import com.example.server.api.security.AuthManager
import com.example.server.api.security.JwtService
import com.example.server.api.security.configureSecurity
import com.example.server.data.audit.AuditLogService
import com.example.server.data.filemanage.FileServiceImpl
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.cio.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.io.path.Path
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds


fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    val secretKey = AuthManager.generateNewSecret()
    val serverIp = environment.config.propertyOrNull("ktor.deployment.ip")?.getString() ?: "localhost"
    val serverPort = environment.config.propertyOrNull("ktor.deployment.port")?.getString() ?: "8888"
    require(serverPort.toInt() in 1..65535) { "Invalid server port: $serverPort" }
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

    val auditLogService = AuditLogService()

    launch {
        auditLogService.logAction("SERVER_START", "SYSTEM", "Listening on $serverIp:$serverPort")
    }

    val shutdownServer: () -> Unit = {
        this@module.launch {
            delay(500.milliseconds)
            println("Shutting down server...")

            this@module.monitor.raise(ApplicationStopping, this@module)
            this@module.monitor.raise(ApplicationStopped, this@module)

            exitProcess(0)
        }
    }

    launch(Dispatchers.IO) {
        val reader = System.`in`.bufferedReader()
        while (isActive) {
            val line = reader.readLine() ?: break
            when (line.trim().lowercase()) {
                "stop", "exit", "shutdown" -> {
                    println("Shutdown command received from console.")
                    shutdownServer()
                    break
                }
                "logs" -> {
                    val logs = auditLogService.getLogs(20)
                    if (logs.isEmpty()) {
                        println("Logs are empty.")
                    } else {
                        logs.forEach {
                            println("[${it.timestamp}] ${it.action} -> ${it.targetPath} (${it.details ?: ""})")
                        }
                    }
                }
                "clear" -> {
                    val count = auditLogService.clearLogs()
                    println("Cleared $count log entries.")
                }
                "" -> Unit
                else -> println("Unknown command. Available: logs, clear, stop")
            }
        }
    }

    install(ContentNegotiation) {
        gson()
    }

    configureSecurity(jwtService)

    val fileService = FileServiceImpl(
        rootPath = Path(environment.config.propertyOrNull("storage.rootPath")?.getString() ?: "/")
    )
    configureRouting(fileService, jwtService, auditLogService, shutdownServer)
}