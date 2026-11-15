package com.example.filemanager.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.gson.gson

object KtorClient {
    val client = HttpClient {
        install(ContentNegotiation) { gson() }
    }
}