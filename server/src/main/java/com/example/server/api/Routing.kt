package com.example.server.api

import com.example.server.api.security.AuthManager
import com.example.server.api.security.JwtService
import com.example.server.data.audit.AuditLogService
import com.example.server.domain.FileService
import com.example.shared.requests.CopyFileRequest
import com.example.shared.requests.CreateFileRequest
import com.example.shared.requests.CreateZipRequest
import com.example.shared.requests.DeleteFilesRequest
import com.example.shared.requests.FileListRequest
import com.example.shared.requests.RenameFileRequest
import com.example.shared.requests.ExtractZipRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import java.nio.file.NoSuchFileException
import java.util.UUID

private val logger = LoggerFactory.getLogger("Routing")

fun Application.configureRouting(
    fileService: FileService,
    jwtService: JwtService,
    auditLogService: AuditLogService,
    onShutdown: () -> Unit
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
                    val type = if (request.isDirectory) "DIRECTORY" else "FILE"
                    auditLogService.logAction("CREATE", request.path, "Type: $type")
                    call.respond(HttpStatusCode.Created, mapOf("message" to "Created"))
                } else {
                    call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "File already exists or cannot be created")
                    )
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
                    auditLogService.logAction("COPY", request.destinationPath, "From: ${request.sourcePath}")
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Copied"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Copy failed"))
                }
            }

            put("/files/rename") {
                val request = call.receive<RenameFileRequest>()
                val success = fileService.renameFile(request.oldPath, request.newPath)
                if (success) {
                    auditLogService.logAction("RENAME_OR_MOVE", request.newPath, "Old path: ${request.oldPath}")
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Renamed"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Rename failed"))
                }
            }

            delete("/files/delete") {
                val request = call.receive<DeleteFilesRequest>()
                val failedPaths = mutableListOf<String>()

                request.relativePaths.forEach { relativePath ->
                    val deleted = fileService.deleteFile(relativePath, request.recursive)
                    if (deleted) {
                        auditLogService.logAction("DELETE", relativePath, "Recursive: ${request.recursive}")
                    } else {
                        failedPaths.add(relativePath)
                    }
                }

                if (failedPaths.isEmpty()) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        mapOf("error" to "Failed to delete some items", "failed" to failedPaths)
                    )
                }
            }

            get("/files/content/json") {
                val relativePath = call.request.queryParameters["path"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing path"))

                try {
                    val content = fileService.getJsonContent(relativePath)
                    call.respondText(content, ContentType.Application.Json)
                } catch (e: SecurityException) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to (e.message ?: "Access denied")))
                } catch (_: NoSuchFileException) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid path")))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
                }
            }

            get("/files/content/xml") {
                val relativePath = call.request.queryParameters["path"]
                    ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing path"))

                try {
                    val content = fileService.getXmlContent(relativePath)
                    call.respondText(content, ContentType.Application.Xml)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid XML")))
                } catch (e: Exception) {
                    throw e
                }
            }

            post("/files/zip") {
                val request = call.receive<CreateZipRequest>()
                val safeName = request.archiveName
                    .replace(Regex("[^a-zA-Zа-яА-Я0-9_-]"), "_")
                    .take(50)
                    .takeIf { it.isNotBlank() } ?: "archive_${UUID.randomUUID()}"

                val targetZipRelativePath = "${request.archivePath}/$safeName.zip"

                try {
                    fileService.createZip(
                        relativePaths = request.relativePaths,
                        targetZipRelativePath = targetZipRelativePath
                    )
                    auditLogService.logAction("ZIP_CREATE", targetZipRelativePath, "Files count: ${request.relativePaths.size}")
                    call.respond(HttpStatusCode.Created, mapOf("message" to "Created"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Invalid request")))
                }
            }

            post("/files/extract") {
                val request = call.receive<ExtractZipRequest>()
                try {
                    fileService.extractZip(
                        zipPath = request.zipPath,
                        relativePath = request.targetDir
                    )
                    auditLogService.logAction("ZIP_EXTRACT", request.targetDir, "Archive: ${request.zipPath}")
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Extracted"))
                } catch (e: Exception) {
                    try {
                        fileService.deleteFile(request.targetDir, recursive = true)
                    } catch (_: Exception) { /* ignore */ }
                    throw e
                }
            }

            get("/admin/logs") {
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 100
                val offset = call.request.queryParameters["offset"]?.toLongOrNull()?.coerceAtLeast(0) ?: 0L
                val logs = auditLogService.getLogs(limit, offset)
                call.respond(HttpStatusCode.OK, logs)
            }

            delete("/admin/logs") {
                val deletedCount = auditLogService.clearLogs()
                auditLogService.logAction("LOGS_CLEARED", "SYSTEM", "Deleted rows: $deletedCount")
                call.respond(HttpStatusCode.OK, mapOf("message" to "Logs cleared", "deletedCount" to deletedCount))
            }

            post("/admin/shutdown") {
                auditLogService.logAction("SERVER_SHUTDOWN", "SYSTEM", "Initiated via API")
                call.respond(HttpStatusCode.OK, mapOf("message" to "Server is shutting down..."))
                onShutdown()
            }
        }

        post("/login") {
            val providedSecret = call.receiveParameters()["secret"] ?: ""
            val clientIp = call.request.origin.remoteHost

            if (AuthManager.consumeSecret(providedSecret)) {
                val token = jwtService.generateToken()
                logger.info("Authorized from $clientIp")
                println("Client connected from $clientIp")

                auditLogService.logAction(
                    action = "LOGIN_SUCCESS",
                    targetPath = "AUTH",
                    details = "Client IP: $clientIp"
                )

                call.respond(mapOf("token" to token))
            } else {
                logger.warn("Failed login attempt from $clientIp")

                auditLogService.logAction(
                    action = "LOGIN_FAILED",
                    targetPath = "AUTH",
                    details = "Invalid secret from IP: $clientIp"
                )

                call.respond(HttpStatusCode.Unauthorized, "Invalid secret key!")
            }
        }
    }
}