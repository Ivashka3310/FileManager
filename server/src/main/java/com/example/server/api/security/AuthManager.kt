package com.example.server.api.security

import java.util.UUID

object AuthManager {
    @Volatile
    var tempQrSecret: String? = null

    fun generateNewSecret(): String {
        val secret = UUID.randomUUID().toString()
        tempQrSecret = secret
        return secret
    }

    fun consumeSecret(providedSecret: String): Boolean {
        synchronized(this) {
            return if (providedSecret==tempQrSecret) {
                tempQrSecret = null
                true
            } else {
                false
            }
        }
    }
}