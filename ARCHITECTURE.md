# Architecture

This document explains the architectural choices in AI Content Hub. The
goal is to be honest about what was picked, what was rejected, what each
choice cost, and what is intentionally missing. The code in `main`
implements what is described here; `CLAUDE.md` is a working brief for an
AI assistant and overlaps with this document by design.

## What this project is

AI Content Hub is a web app that turns long content (newsletter drafts,
blog posts, podcast transcripts) into Twitter threads, LinkedIn posts,
and TL;DRs. The audience for v1 is solo content creators. The product
exists for two reasons:

1. To ship a focused AI-integration showcase as part of an active job
   hunt for senior/remote roles.
2. As a Kotlin learning vehicle following a transition from Spring Boot.

It is not a startup, has no monetization in v1, and is built to be
finished, not to scale.

## Decision 0 — A 4–6 week wedge, not the 16-week plan

The original plan was 16 weeks: foundation, AI, monetization, growth.
That plan was abandoned on 2026-05-08.

The wedge that replaced it is narrower on every axis:

- **Audience:** solo content creators only.
- **Feature:** summarization only — no SEO, translation, or semantic
  search.
- **Surface:** web app only — no public API.
- **Monetization:** none in v1.

The trade-off is breadth for ship probability. A half-finished 16-week
project is a worse portfolio piece than a shipped 5-week one, and the
opportunity cost of every week not interviewing is real. The cost of
the wedge is that several headline features from the long-term vision
(API tier, pgvector search, multi-provider AI) are not demonstrated.
That is acceptable because the hiring signal a wedge needs to send
("can finish a thing") is different from the signal a SaaS launch needs
to send.

This decision is the most load-bearing one in the project. Every later
decision defers to it.

## Decision 1 — Kotlin + Ktor, not Spring Boot

Spring Boot is the familiar option and would have been the fastest
path to a running app. Ktor was chosen anyway.

**Why Ktor:** Kotlin transition is one of two project goals, and Ktor
is the idiomatic Kotlin server framework — small, explicit, and free
of the annotation-heavy reflection model that Spring uses. Each plugin
is installed by an explicit call in `Application.module()`, which makes
the application's behavior trivial to read top to bottom. There is no
"magic" startup behavior to learn.

**What it costs:** the Spring ecosystem is larger. Auth helpers, admin
panels, observability integrations, and Stack Overflow coverage are all
deeper for Spring. When something fails inside Ktor, the resolution
path is reading Ktor source rather than searching for a known fix.
This is a deliberate trade — the unfamiliarity is the learning.

**Alternatives considered:** Spring Boot (Kotlin transition would have
been weaker), Java + Spring (defeats the Kotlin goal entirely), Go +
chi / echo (would have been faster to ship, but does not advance the
Kotlin goal), Node + Express (same).

## Decision 2 — PostgreSQL via JDBC + Exposed, not R2DBC

Postgres is uncontroversial. The interesting choice is JDBC, not R2DBC.

**Why JDBC + Exposed:** Exposed is JetBrains' Kotlin SQL DSL — type-safe
queries with column references that the compiler verifies, low ceremony
compared to JPA / Hibernate, and zero reflection. JDBC is synchronous
but well understood, and Postgres connection pooling via HikariCP is
mature. Database calls are dispatched to `Dispatchers.IO` from suspend
functions, which gives the coroutine ergonomics of a reactive stack
without the reactive complexity.

**What it costs:** every DB call occupies an IO thread for the duration
of the query. At the wedge's traffic level (single user testing locally)
this is invisible. At meaningful concurrency it would matter.

**Alternatives considered:** R2DBC (rejected — reactive complexity is
not earned by a wedge with no concurrency story), raw JDBC + a custom
DAO layer (rejected — Exposed gives the same result with less code),
jOOQ (rejected — code generation step adds friction for a small schema).

R2DBC is the most likely thing to revisit in v2 if the API tier ever
ships.

## Decision 3 — HTMX + server-rendered Kotlin, not a SPA

Every page is rendered server-side with `kotlinx.html` and `respondHtml`.
HTMX is loaded from a CDN in the base layout. There is no JavaScript
build step in the repository.

**Why HTMX:** the wedge has six routes that the user can see, none of
them with rich client state. A SPA would add a second language, a second
build pipeline, and a separate deploy story for what amounts to
form-driven CRUD with progressive enhancement. HTMX covers the cases
that justify a SPA at this size (partial updates, streaming responses)
with a single CDN script tag. When week 3 adds Anthropic streaming, HTMX
SSE handles the UI side without any client framework.

**What it costs:** rich client-side interactivity (drag-to-reorder, live
collaborative editing, complex client-side validation) becomes awkward.
None of those are in scope for v1.

**Alternatives considered:** React + Vite (rejected — too much surface
area for the feature set), Vue (same), Svelte (same), no client
interactivity at all (rejected — AI streaming would feel broken without
partial updates).

## Decision 4 — Server-side sessions, not JWT

Auth uses Ktor's `Sessions` plugin with a signed cookie containing the
user id and email. Session secret rotation is a single config change.

**Why sessions:** the wedge runs on one server. There is no service
boundary that needs a self-contained token, no API client that needs
auth without a cookie store, and no scale-out plan. Sessions get
revocation for free (rotate the secret, every cookie is invalid) and
keep the API surface trivial.

**What it costs:** the app is stateful in the sense that a horizontal
scale-out would need sticky sessions or a shared session store. For v1
this is theoretical — the deploy target is a single box.

**Alternatives considered:** JWT (rejected — solves a distribution
problem that the wedge does not have, and the cost is real:
revocation list infrastructure, key rotation, larger cookies), OAuth via
a third party (rejected — adds external dependency and signup friction
for an app whose first 5 users will be friends).

## Decision 5 — Anthropic only, not multi-provider

When AI integration lands in week 3, it will call Anthropic's API
directly. There is no provider abstraction layer.

**Why Anthropic only:** one provider means one SDK, one prompt format,
one set of streaming semantics, one set of failure modes to learn. A
multi-provider abstraction is the wrong place to spend complexity
before there is a single shipped prompt. If OpenAI is added later, it
will be added against a concrete second prompt — at that point the
abstraction has design pressure to point at. Without that pressure, a
multi-provider layer becomes a least-common-denominator interface that
loses each provider's strengths.

**What it costs:** lock-in. Switching providers later requires touching
every call site. For the small number of call sites planned (one
endpoint, one prompt in v1; three output format prompts in v2) that
cost is small.

**Alternatives considered:** OpenAI only (rejected — Claude's longer
context and better stylistic instruction-following matter for the
summarization use case), multi-provider via LangChain or a custom
abstraction (rejected — adds dependency and complexity before the first
working prompt).

## Things deliberately not in v1

Each of these has been excluded with intent. None are "we forgot."

- **Stripe / monetization.** No users yet. Building checkout before
  there is anything to check out is busywork.
- **pgvector / semantic search.** Not in the feature set.
- **Redis.** There is no caching pressure. The session store is in the
  cookie itself.
- **Kafka or any message broker.** There are no domain events worth
  publishing across a broker boundary.
- **Kubernetes.** One box is enough; Fly.io or Railway will run a
  single container in week 5.
- **Automated tests.** This is the most controversial omission. The
  rationale: the wedge is small enough that booting the app and
  exercising the flow with curl is the cheapest way to verify behavior,
  and it catches a different class of bug than unit tests would. Three
  real boot-time failures (YAML config syntax, missing serialization
  setup, missing content-negotiation install) surfaced from running the
  app that a green `gradle build` had hidden. Automated tests get added
  in week 5 when there is enough surface area for regressions to be a
  real risk. This decision will not survive a second developer joining.
- **API keys / public API.** The web app is the only surface in v1.

## Open risks and questions

The honest list of things that are not solved.

- **No automated tests.** The mitigations above are real but finite. The
  threshold for adding them is "I broke something I had already
  verified."
- **BCrypt cost factor 12.** Picked from memory, not measured. Cold
  start signup latency on a small box is unknown.
- **No rate limiting on auth.** Fine for a personal wedge, unacceptable
  for any real launch.
- **Session secret in `application.yaml` default.** The default is a
  loud dev-only string and the production deploy will override it via
  env var, but there is no enforcement that prevents booting in prod
  with the default. A startup-time assertion is owed.
- **Anthropic spend is not gated.** Week 3 must add a per-user or
  per-day spend ceiling before any external user touches the app.
- **Single-box deploy has no failover story.** Acceptable for a
  portfolio piece, not for paying users.

## What I would revisit if v2 happens

- Move to R2DBC if a public API tier ships and concurrency becomes a
  real signal.
- Introduce a provider abstraction the moment a second AI provider has
  a concrete reason to exist.
- Add Redis only when a specific call site has measurable latency from
  re-doing work.
- Replace HTMX with a SPA only if a feature needs client state that
  HTMX cannot reasonably model — not because the team grew.
