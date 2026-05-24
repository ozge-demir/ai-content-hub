package com.aicontenthub

import com.aicontenthub.auth.authRoutes
import com.aicontenthub.config.DatabaseFactory
import com.aicontenthub.config.UserSession
import com.aicontenthub.health.healthRoutes
import com.aicontenthub.web.homeRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerMessageAuthentication
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.http.HttpStatusCode
import org.slf4j.LoggerFactory

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val log = LoggerFactory.getLogger("Application")

    DatabaseFactory.init(environment.config)

    install(DefaultHeaders)
    install(CallLogging)

    val sessionSecret = environment.config.property("session.secret").getString()
    install(Sessions) {
        cookie<UserSession>("ACH_SESSION") {
            cookie.path = "/"
            cookie.httpOnly = true
            cookie.maxAgeInSeconds = 60L * 60 * 24 * 30
            transform(SessionTransportTransformerMessageAuthentication(sessionSecret.toByteArray()))
        }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.error("Unhandled error", cause)
            call.respondText("Something went wrong", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        healthRoutes()
        authRoutes()
        homeRoutes()
    }
}
