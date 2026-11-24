package com.example.server.domain

data class AuditLogEntry(
    val id: Int,
    val timestamp: String,
    val action: String,
    val targetPath: String,
    val details: String?
)