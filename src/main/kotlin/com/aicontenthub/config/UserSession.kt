package com.aicontenthub.config

import io.ktor.server.application.ApplicationCall
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val userId: String,
    val email: String,
)

fun ApplicationCall.userSession(): UserSession? = sessions.get<UserSession>()
