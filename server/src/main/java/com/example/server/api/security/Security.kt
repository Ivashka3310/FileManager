package com.example.server.api.security

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

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