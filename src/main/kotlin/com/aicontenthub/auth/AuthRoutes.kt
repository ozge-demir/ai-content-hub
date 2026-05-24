package com.aicontenthub.auth

import com.aicontenthub.config.UserSession
import com.aicontenthub.config.userSession
import com.aicontenthub.web.respondPage
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.html.ButtonType
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p

fun Route.authRoutes() {
    val users = UserRepository()

    get("/signup") {
        if (call.userSession() != null) {
            call.respondRedirect("/")
            return@get
        }
        call.respondPage("Sign up") {
            renderAuthForm(title = "Sign up", action = "/signup", switchHref = "/login", switchLabel = "Log in")
        }
    }

    post("/signup") {
        val params = call.receiveParameters()
        val email = params["email"]?.trim().orEmpty()
        val password = params["password"].orEmpty()
        val error = validateCredentials(email, password)
        if (error != null) {
            call.respondPage("Sign up") {
                p { +error }
                renderAuthForm(title = "Sign up", action = "/signup", switchHref = "/login", switchLabel = "Log in", emailValue = email)
            }
            return@post
        }
        if (users.findByEmail(email) != null) {
            call.respondPage("Sign up") {
                p { +"An account with that email already exists." }
                renderAuthForm(title = "Sign up", action = "/signup", switchHref = "/login", switchLabel = "Log in", emailValue = email)
            }
            return@post
        }
        val user = users.create(email, password)
        call.sessions.set(UserSession(userId = user.id.toString(), email = user.email))
        call.respondRedirect("/")
    }

    get("/login") {
        if (call.userSession() != null) {
            call.respondRedirect("/")
            return@get
        }
        call.respondPage("Log in") {
            renderAuthForm(title = "Log in", action = "/login", switchHref = "/signup", switchLabel = "Sign up")
        }
    }

    post("/login") {
        val params = call.receiveParameters()
        val email = params["email"]?.trim().orEmpty()
        val password = params["password"].orEmpty()
        val user = users.findByEmail(email)
        if (user == null || !users.verifyPassword(password, user.passwordHash)) {
            call.respondPage("Log in") {
                p { +"Email or password is incorrect." }
                renderAuthForm(title = "Log in", action = "/login", switchHref = "/signup", switchLabel = "Sign up", emailValue = email)
            }
            return@post
        }
        call.sessions.set(UserSession(userId = user.id.toString(), email = user.email))
        call.respondRedirect("/")
    }

    post("/logout") {
        call.sessions.clear<UserSession>()
        call.respondRedirect("/")
    }
}

private fun validateCredentials(email: String, password: String): String? = when {
    email.isBlank() -> "Email is required."
    !email.contains('@') -> "Email looks invalid."
    password.length < 8 -> "Password must be at least 8 characters."
    else -> null
}

private fun kotlinx.html.BODY.renderAuthForm(
    title: String,
    action: String,
    switchHref: String,
    switchLabel: String,
    emailValue: String = "",
) {
    h1 { +title }
    form(action = action, method = FormMethod.post) {
        p {
            label { +"Email " }
            input(type = InputType.email, name = "email") {
                required = true
                value = emailValue
            }
        }
        p {
            label { +"Password " }
            input(type = InputType.password, name = "password") {
                required = true
                attributes["minlength"] = "8"
            }
        }
        p {
            button(type = ButtonType.submit) { +title }
        }
    }
    p {
        a(href = switchHref) { +switchLabel }
    }
}
