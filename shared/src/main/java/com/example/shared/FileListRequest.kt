package com.example.shared

data class FileListRequest(
    val path: String,
    val offset: Int,
    val limit: Int,
    val filter: String? = null,
    val extension: String? = null,
    val nameQuery: String? = null,
    val orderBy: String? = null
)
