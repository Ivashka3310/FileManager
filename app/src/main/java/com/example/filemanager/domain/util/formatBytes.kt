package com.example.filemanager.domain.util

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 Б"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    val tb = gb / 1024.0

    return when {
        tb >= 1.0 -> String.format("%.1f ТБ", tb)
        gb >= 1.0 -> String.format("%.1f ГБ", gb)
        mb >= 1.0 -> String.format("%.1f МБ", mb)
        kb >= 1.0 -> String.format("%.1f КБ", kb)
        else -> "$bytes Б"
    }
}