package com.example.shared.requests

data class CreateZipRequest(
    val archivePath: String,
    val archiveName: String,
    val relativePaths: List<String>
)