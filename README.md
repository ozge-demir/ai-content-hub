# AI Content Hub

Web app that turns long content (newsletter drafts, blog posts, podcast transcripts) into Twitter threads, LinkedIn posts, and TL;DRs.

v1 wedge for solo content creators. See [ARCHITECTURE.md](ARCHITECTURE.md) for the architectural decisions, trade-offs, and what is deliberately not in v1.

## Stack

- Kotlin + Ktor 3
- PostgreSQL + JDBC + Exposed + Flyway
- HTMX + server-rendered Kotlin (kotlinx.html)
- Anthropic Claude (added in week 3)

## Local dev

Prereqs: JDK 21+, Docker.

```bash
# 1. Start Postgres
docker compose up -d

# 2. Run the app (Flyway migrations apply on boot)
./gradlew run

# Visit http://localhost:8080
```

## Project layout

```
src/main/kotlin/com/aicontenthub/
  Application.kt            entrypoint, plugin install, route registration
  config/                   DB pool, sessions, app config
  auth/                     signup / login / logout, users table
  web/                      shared HTML layout, home page
  health/                   /health
src/main/resources/
  application.yaml          Ktor + db + ai config
  logback.xml               logging
  db/migration/             Flyway SQL migrations
```

## Environment variables

| Var | Default | Notes |
| --- | --- | --- |
| `PORT` | `8080` | HTTP port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/aicontenthub` | JDBC URL |
| `DATABASE_USER` | `aicontenthub` | |
| `DATABASE_PASSWORD` | `aicontenthub` | |
| `SESSION_SECRET` | dev-only fallback | **must override in prod** |
| `ANTHROPIC_API_KEY` | empty | wired in week 3 |
