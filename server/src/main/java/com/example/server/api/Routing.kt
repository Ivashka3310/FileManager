package com.example.server.api

import com.example.server.api.security.AuthManager
import com.example.server.api.security.JwtService
import com.example.server.domain.FileService
import com.example.shared.requests.CopyFileRequest
import com.example.shared.requests.CreateFileRequest
import com.example.shared.requests.FileListRequest
import com.example.shared.requests.RenameFileRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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
                val fileListRequest = FileListRequest(
                    path = call.request.queryParameters["path"] ?: "",
                    offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0,
                    limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50,
                    filter = call.request.queryParameters["filter"],
                    extension = call.request.queryParameters["extension"],
                    nameQuery = call.request.queryParameters["nameQuery"],
                    orderBy = call.request.queryParameters["orderBy"]
                )

                val files = fileService.getFiles(fileListRequest)
                call.respond(HttpStatusCode.OK, files)
            }

            post("/files/create") {
                val request = call.receive<CreateFileRequest>()
                val success = fileService.createFile(request.path, request.isDirectory)
                if (success) {
                    call.respond(HttpStatusCode.Created, mapOf("message" to "Created"))
                } else {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "File already exists or cannot be created"))
                }
            }

            post("/files/copy") {
                val request = call.receive<CopyFileRequest>()
                val success = fileService.copyFile(
                    sourcePath = request.sourcePath,
                    destinationPath = request.destinationPath,
                    overwrite = request.overwrite
                )
                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Copied"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Copy failed"))
                }
            }

            put("/files/rename") {
                val request = call.receive<RenameFileRequest>()
                val success = fileService.renameFile(request.oldPath, request.newPath)
                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Renamed"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Rename failed"))
                }
            }

            delete("/files/delete") {
                val path = call.request.queryParameters["path"]
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing path"))
                val recursive = call.request.queryParameters["recursive"]?.toBoolean() ?: false

                val deleted = fileService.deleteFile(path, recursive)
                if (deleted) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Delete failed. Directory might not be empty."))
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