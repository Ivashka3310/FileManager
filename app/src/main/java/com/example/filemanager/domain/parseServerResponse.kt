package com.example.filemanager.domain

import com.example.shared.models.FileItem
import com.google.gson.Gson

private val gson = Gson()

fun parseServerResponse(text: String): ServerResponse = when {
    text.startsWith("SUCCESS") || text.startsWith("ERROR") -> {
        ServerResponse.Status(text)
    }
    text.startsWith("[") -> {
        try {
            val type = com.google.gson.reflect.TypeToken.getParameterized(List::class.java, FileItem::class.java).type
            val files = gson.fromJson<List<FileItem>>(text, type)
            ServerResponse.FileList(files)
        } catch (e: Exception) {
            ServerResponse.Error("Parsing error: ${e.message}")
        }
    }
    else -> ServerResponse.Status(text)
}