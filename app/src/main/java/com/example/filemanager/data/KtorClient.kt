package com.example.filemanager.data

import com.example.filemanager.domain.SessionStorage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.flow.first

object KtorClient {
    val client = HttpClient {
        install(ContentNegotiation) { gson() }
    }
}