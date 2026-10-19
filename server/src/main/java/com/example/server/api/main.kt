package com.example.server.api

import com.example.server.data.FileServiceImpl
import com.google.gson.Gson
import io.ktor.server.application.Application
import io.ktor.server.websocket.WebSockets
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.EngineMain
import io.ktor.server.engine.embeddedServer
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

fun Application.module() {
    //configureSerialization()
    configureSecurity()

    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    val gson = Gson()
    val fileService = FileServiceImpl(
        rootPath = Path(environment.config.propertyOrNull("storage.rootPath")?.getString() ?: "/")
    )
    configureRouting(fileService, gson)
}

fun main(args: Array<String>) {
    EngineMain.main(args)
//
//    embeddedServer(CIO, port = 8081, host = "0.0.0.0") {
//        //configureSerialization()
//        configureSecurity()
//
//        install(WebSockets) {
//            pingPeriod = 15.seconds
//            timeout = 15.seconds
//            maxFrameSize = Long.MAX_VALUE
//            masking = false
//        }
//
//        val gson = Gson()
//        val fileService = FileServiceImpl(
//            rootPath = Path(environment.config.propertyOrNull("storage.rootPath")?.getString() ?: "/")
//        )
//        configureRouting(fileService, gson)
//    }.start(wait = true)
}