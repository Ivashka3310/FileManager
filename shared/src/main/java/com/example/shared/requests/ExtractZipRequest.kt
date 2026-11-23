package com.example.shared.requests

data class ExtractZipRequest(
    val zipPath: String,
    val targetDir: String
)