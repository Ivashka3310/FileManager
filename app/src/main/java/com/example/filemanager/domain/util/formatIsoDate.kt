package com.example.filemanager.domain.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
fun formatIsoDate(isoString: String): String {
    if (isoString == "unknown" || isoString.isBlank()) return "Неизвестно"

    return try {
        val instant = Instant.parse(isoString)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()

        val formatter = DateTimeFormatter.ofPattern("d MMM yyyy г., HH:mm", Locale("ru"))
        localDateTime.format(formatter)
    } catch (_: Exception) {
        isoString.replace("T", " ").substringBefore(".")
    }
}