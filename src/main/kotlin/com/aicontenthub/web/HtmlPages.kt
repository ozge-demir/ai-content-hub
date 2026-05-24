package com.aicontenthub.web

import io.ktor.server.application.ApplicationCall
import io.ktor.server.html.respondHtml
import kotlinx.html.BODY
import kotlinx.html.HTML
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.title
import kotlinx.html.unsafe

suspend fun ApplicationCall.respondPage(
    pageTitle: String,
    block: BODY.() -> Unit,
) {
    respondHtml {
        baseLayout(pageTitle, block)
    }
}

fun HTML.baseLayout(pageTitle: String, content: BODY.() -> Unit) {
    head {
        meta(charset = "utf-8")
        meta(name = "viewport", content = "width=device-width, initial-scale=1")
        title(pageTitle)
        script(src = "https://unpkg.com/htmx.org@2.0.4") {
            attributes["integrity"] = "sha384-HGfztofotfshcF7+8n44JQL2oJmowVChPTg48S+jvZoztPfvwD79OC/LTtG6dMp+"
            attributes["crossorigin"] = "anonymous"
        }
        link(rel = "stylesheet", href = "/static/styles.css")
    }
    body {
        unsafe { +"" }
        content()
    }
}
