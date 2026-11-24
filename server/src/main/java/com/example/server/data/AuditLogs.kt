package com.example.server.data

import org.jetbrains.exposed.v1.core.Table

object AuditLogs : Table("audit_logs") {
    val id = integer("id").autoIncrement()
    val timestamp = varchar("timestamp", 64)
    val action = varchar("action", 32)
    val targetPath = text("target_path")
    val details = text("details").nullable()

    override val primaryKey = PrimaryKey(id)
}