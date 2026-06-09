package com.aicontenthub.drafts

import com.aicontenthub.config.UserSession
import com.aicontenthub.config.userSession
import com.aicontenthub.web.respondPage
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.li
import kotlinx.html.p
import kotlinx.html.pre
import kotlinx.html.textArea
import kotlinx.html.ul
import java.time.format.DateTimeFormatter
import java.util.UUID

fun Route.draftRoutes() {
    val drafts = DraftRepository()

    route("/drafts") {
        get("/new") {
            val session = call.requireSession() ?: return@get
            call.respondPage("New draft") {
                renderDraftForm(action = "/drafts", title = "New draft")
                p { a(href = "/") { +"← Back to dashboard" } }
            }
        }

        post("") {
            val session = call.requireSession() ?: return@post
            val params = call.receiveParameters()
            val title = params["title"]?.trim().orEmpty()
            val body = params["body"]?.trim().orEmpty()
            val error = validateDraft(title, body)
            if (error != null) {
                call.respondPage("New draft") {
                    p { +error }
                    renderDraftForm(action = "/drafts", title = "New draft", titleValue = title, bodyValue = body)
                }
                return@post
            }
            val draft = drafts.create(UUID.fromString(session.userId), title, body)
            call.respondRedirect("/drafts/${draft.id}")
        }

        get("/{id}") {
            val session = call.requireSession() ?: return@get
            val draft = call.draftOr404(drafts, session) ?: return@get
            call.respondPage(draft.title) {
                h1 { +draft.title }
                p { +"Saved ${formatInstant(draft.updatedAt)}" }
                pre { +draft.body }
                p {
                    a(href = "/drafts/${draft.id}/edit") { +"Edit" }
                    +" · "
                    a(href = "/") { +"Back to dashboard" }
                }
                form(action = "/drafts/${draft.id}/delete", method = FormMethod.post) {
                    button(type = ButtonType.submit) { +"Delete draft" }
                }
            }
        }

        get("/{id}/edit") {
            val session = call.requireSession() ?: return@get
            val draft = call.draftOr404(drafts, session) ?: return@get
            call.respondPage("Edit draft") {
                renderDraftForm(
                    action = "/drafts/${draft.id}",
                    title = "Edit draft",
                    titleValue = draft.title,
                    bodyValue = draft.body,
                )
                p { a(href = "/drafts/${draft.id}") { +"← Cancel" } }
            }
        }

        post("/{id}") {
            val session = call.requireSession() ?: return@post
            val id = call.draftId() ?: return@post
            val params = call.receiveParameters()
            val title = params["title"]?.trim().orEmpty()
            val body = params["body"]?.trim().orEmpty()
            val error = validateDraft(title, body)
            if (error != null) {
                call.respondPage("Edit draft") {
                    p { +error }
                    renderDraftForm(
                        action = "/drafts/$id",
                        title = "Edit draft",
                        titleValue = title,
                        bodyValue = body,
                    )
                }
                return@post
            }
            val updated = drafts.update(id, UUID.fromString(session.userId), title, body)
            if (!updated) {
                call.respondText("Draft not found.", status = HttpStatusCode.NotFound)
                return@post
            }
            call.respondRedirect("/drafts/$id")
        }

        post("/{id}/delete") {
            val session = call.requireSession() ?: return@post
            val id = call.draftId() ?: return@post
            drafts.delete(id, UUID.fromString(session.userId))
            call.respondRedirect("/")
        }
    }
}

private suspend fun ApplicationCall.requireSession(): UserSession? {
    val s = userSession()
    if (s == null) respondRedirect("/login")
    return s
}

private suspend fun ApplicationCall.draftId(): UUID? {
    val raw = parameters["id"]
    val id = raw?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    if (id == null) respondText("Draft not found.", status = HttpStatusCode.NotFound)
    return id
}

private suspend fun ApplicationCall.draftOr404(drafts: DraftRepository, session: UserSession): Draft? {
    val id = draftId() ?: return null
    val draft = drafts.findOwned(id, UUID.fromString(session.userId))
    if (draft == null) respondText("Draft not found.", status = HttpStatusCode.NotFound)
    return draft
}

private fun validateDraft(title: String, body: String): String? = when {
    title.isBlank() -> "Title is required."
    title.length > 200 -> "Title must be 200 characters or fewer."
    body.isBlank() -> "Paste your content into the body field."
    else -> null
}

private val displayFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("yyyy-MM-dd HH:mm")
    .withZone(java.time.ZoneId.systemDefault())

private fun formatInstant(i: java.time.Instant) = displayFormatter.format(i)

private fun FlowContent.renderDraftForm(
    action: String,
    title: String,
    titleValue: String = "",
    bodyValue: String = "",
) {
    h1 { +title }
    form(action = action, method = FormMethod.post) {
        p {
            label { +"Title " }
            input(type = InputType.text, name = "title") {
                required = true
                attributes["maxlength"] = "200"
                value = titleValue
            }
        }
        p {
            label { +"Body " }
        }
        p {
            textArea {
                name = "body"
                attributes["rows"] = "16"
                attributes["cols"] = "80"
                required = true
                +bodyValue
            }
        }
        p {
            button(type = ButtonType.submit) { +"Save" }
        }
    }
}

internal fun FlowContent.renderDraftList(items: List<Draft>) {
    h2 { +"Your drafts" }
    if (items.isEmpty()) {
        p { +"No drafts yet." }
    } else {
        ul {
            items.forEach { d ->
                li {
                    a(href = "/drafts/${d.id}") { +d.title }
                    +"  —  ${formatInstant(d.updatedAt)}"
                }
            }
        }
    }
    p { a(href = "/drafts/new") { +"+ New draft" } }
}
