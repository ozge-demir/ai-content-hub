package com.aicontenthub.health

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes() {
    get("/health") {
        call.respondText("""{"status":"ok"}""", ContentType.Application.Json, HttpStatusCode.OK)
    }
}
