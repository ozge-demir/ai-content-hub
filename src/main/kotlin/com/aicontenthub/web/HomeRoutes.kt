package com.aicontenthub.web

import com.aicontenthub.config.userSession
import com.aicontenthub.drafts.DraftRepository
import com.aicontenthub.drafts.renderDraftList
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.p
import java.util.UUID

fun Route.homeRoutes() {
    val drafts = DraftRepository()

    get("/") {
        val session = call.userSession()
        if (session == null) {
            call.respondPage("AI Content Hub") {
                h1 { +"AI Content Hub" }
                p { +"Turn long content into Twitter threads, LinkedIn posts, and TL;DRs." }
                p {
                    a(href = "/signup") { +"Sign up" }
                    +" · "
                    a(href = "/login") { +"Log in" }
                }
            }
            return@get
        }
        val mine = drafts.listForUser(UUID.fromString(session.userId))
        call.respondPage("Dashboard") {
            h1 { +"Welcome, ${session.email}" }
            renderDraftList(mine)
            form(action = "/logout", method = FormMethod.post) {
                button(type = ButtonType.submit) { +"Log out" }
            }
        }
    }
}
