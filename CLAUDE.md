# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**AI Content Hub** — long-term vision: a portfolio-level SaaS for content creators and businesses, offering AI summarization, SEO optimization, translation, and semantic search. Triple revenue model: SaaS subscriptions + pay-per-use API + freelance portfolio piece.

Doubles as a Kotlin learning vehicle (transitioning from Spring Boot) and an AI-integration showcase for a senior/remote job hunt.

## v1 wedge (active scope)

A 4-6 week narrow wedge — chosen 2026-05-08 over the original 16-week plan:

- **Audience:** solo content creators (newsletter writers, bloggers, podcasters)
- **Feature:** summarization, framed as "turn long content into a Twitter thread / LinkedIn post / TL;DR"
- **Surface:** web app only — no public API in v1
- **Monetization:** none in v1; serves as portfolio piece for the job hunt

**Resist scope creep back toward the long-term vision.** No Stripe, no pgvector, no Kafka, no Kubernetes, no SEO/translation/semantic-search features in v1. If a task seems to need one of those, push back.

## v1 stack

- **Backend:** Kotlin + Ktor
- **DB:** PostgreSQL via JDBC + Exposed (JetBrains' Kotlin SQL DSL), HikariCP pool, Flyway migrations. **Not R2DBC** — deferred to v2.
- **Frontend:** HTMX + server-rendered Kotlin (no JS build step, no SPA)
- **AI:** Anthropic Claude only (skip OpenAI in v1)
- **Auth:** server-side sessions (not JWT)
- **Deploy:** deferred until week 5 — runnable locally via `./gradlew run` until then

## Rough 4-6 week shape

- **Wk 1:** Ktor scaffold, Postgres + Flyway, session auth, health endpoint
- **Wk 2:** Content CRUD (paste/save/list drafts) + minimal HTMX UI
- **Wk 3:** Anthropic integration — one endpoint, one prompt, streaming
- **Wk 4:** Three output formats (Twitter thread / LinkedIn post / TL;DR), history view
- **Wk 5:** Polish — landing page, error states, loading states, deploy
- **Wk 6:** Buffer / get 5 real creators to try it / iterate on prompts

## Long-term vision (reference, NOT active)

Original 16-week plan, retained for context only:

- Wks 1–4 — Foundation: auth, CRUD, tests, CI/CD
- Wks 5–8 — AI integration: LLM wiring, streaming chat, pgvector semantic search
- Wks 9–12 — Monetization: Stripe, API keys, pricing tiers
- Wks 13–16 — Growth: dashboard, monitoring, Product Hunt launch

Long-term stack additions (not in v1): pgvector, Redis, Kafka, Kubernetes, OpenAI, Stripe.
