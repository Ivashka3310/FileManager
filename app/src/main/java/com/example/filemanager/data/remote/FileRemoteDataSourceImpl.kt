package com.example.filemanager.data.remote

import io.ktor.client.HttpClient

class FileRemoteDataSource(
    private val client: HttpClient
) {
}