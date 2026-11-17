package com.example.server.api

import com.example.server.api.security.AuthManager
import com.example.server.api.security.JwtService
import com.example.server.domain.FileService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.nio.file.NoSuchFileException

private val logger = LoggerFactory.getLogger("Routing")

fun Application.configureRouting(
    fileService: FileService,
    jwtService: JwtService
) {
    install(StatusPages) {
        exception<SecurityException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, mapOf("message" to (cause.message ?: "Forbidden")))
        }
        exception<NoSuchFileException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, mapOf("message" to (cause.message ?: "Not Found")))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, mapOf("message" to (cause.message ?: "Bad Request")))
        }
    }

    routing {
        authenticate("auth-jwt") {
            get("/disks") {
                val disks = fileService.getLocalDisks()
                call.respond(HttpStatusCode.OK, disks)
            }

            get("/files/list") {
                val path = call.request.queryParameters["path"]
                    ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing required query parameter: path")
                    )

                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val filter = call.request.queryParameters["filter"]
                val extension = call.request.queryParameters["extension"]
                val nameQuery = call.request.queryParameters["nameQuery"]
                val orderBy = call.request.queryParameters["orderBy"]

                val files = fileService.getFiles(
                    relativePath = path,
                    offset = offset,
                    limit = limit,
                    filter = filter,
                    extension = extension,
                    nameQuery = nameQuery,
                    orderBy = orderBy
                )
                call.respond(HttpStatusCode.OK, files)
            }

            delete("/files/delete") {
                val path = call.request.queryParameters["path"]
                    ?: return@delete call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Missing required query parameter: path")
                    )

                val deleted = fileService.deleteFile(path)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to delete: $path")
                    )
                }
            }
        }

        post("/login") {
            val providedSecret = call.receiveParameters()["secret"] ?: ""

            if (AuthManager.consumeSecret(providedSecret)) {
                val token = jwtService.generateToken()
                logger.info("Authorized")
                AuthManager.tempQrSecret = null
                call.respond(mapOf("token" to token))
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid secret key!")
            }
        }
    }
}