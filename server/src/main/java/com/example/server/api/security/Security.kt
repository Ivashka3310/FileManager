package com.example.server.api.security

import io.ktor.server.application.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.websocket.WebSockets

fun Application.configureSecurity(
    jwtService: JwtService
) {
    install(Authentication) {
        jwt("auth-jwt") {
            verifier(jwtService.verifier)
            validate { credential ->
                val hasAudience = credential.payload.audience.contains(jwtService.audience)
                val exp = credential.payload.expiresAt?.time ?: 0
                val isNotExpired = exp > System.currentTimeMillis()

                if (hasAudience && isNotExpired) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}