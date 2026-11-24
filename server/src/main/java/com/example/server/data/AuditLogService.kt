package com.example.server.data

import com.example.server.domain.AuditLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Instant

class AuditLogService(dbUrl: String = "jdbc:sqlite:server_audit.db") {

    private val database: Database = Database.connect(
        url = dbUrl,
        driver = "org.sqlite.JDBC"
    )

    init {
        transaction(database) {
            SchemaUtils.create(AuditLogs)
        }
    }

    suspend fun logAction(
        action: String,
        targetPath: String,
        details: String? = null
    ) = withContext(Dispatchers.IO) {
        transaction(database) {
            AuditLogs.insert {
                it[timestamp] = Instant.now().toString()
                it[AuditLogs.action] = action
                it[AuditLogs.targetPath] = targetPath
                it[AuditLogs.details] = details
            }
        }
    }

    suspend fun getLogs(
        limit: Int = 100,
        offset: Long = 0
    ): List<AuditLogEntry> = withContext(Dispatchers.IO) {
        transaction(database) {
            AuditLogs.selectAll()
                .orderBy(AuditLogs.id to SortOrder.DESC)
                .limit(limit)
                .offset(offset)
                .map {
                    AuditLogEntry(
                        id = it[AuditLogs.id],
                        timestamp = it[AuditLogs.timestamp],
                        action = it[AuditLogs.action],
                        targetPath = it[AuditLogs.targetPath],
                        details = it[AuditLogs.details]
                    )
                }
        }
    }

    suspend fun clearLogs(): Int = withContext(Dispatchers.IO) {
        transaction(database) {
            AuditLogs.deleteAll()
        }
    }
}
