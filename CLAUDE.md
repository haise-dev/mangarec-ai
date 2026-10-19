# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MangaRec AI — AI-powered multilingual manga recommendation Micro-SaaS. Monorepo with three services orchestrated via Docker Compose.

**Agent tooling:** `.agent/` contains an installed Everything Claude Code (ECC) plugin providing 64 specialized agents (e.g. `java-reviewer`, `python-reviewer`, `typescript-reviewer`, `database-reviewer`, `fastapi-reviewer`, `planner`, `tdd-guide`) plus skills and slash commands. Its generic git commit format (`<type>: <desc>`) is overridden by the project convention below (`MR-XXXX | ...`).

## Quick Start (Development)

```bash
# Each service has its own .env.example — copy and fill in real values
cp ai-service/.env.example ai-service/.env
cp be-service/.env.example be-service/.env
cp fe-service/.env.example fe-service/.env

# Create shared Docker network (one-time)
docker network create mangarec_global_network

# Start all services
docker compose -f docker-compose.dev.yml up -d
```

| Service | URL |
|---------|-----|
| Frontend | `http://localhost:3000` |
| Backend API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| AI Service | `http://localhost:8000` |
| AI API Docs | `http://localhost:8000/docs` |
| Qdrant Dashboard | `http://localhost:6333/dashboard` |

## Architecture

```
[fe-service: React/Vite :3000]
        │ REST (Axios)
        ▼
[be-service: Spring Boot :8080] ─── PostgreSQL (primary DB)
        │        │                  ─── Redis (cache/sessions/rate-limit)
        │        └── WebSocket (real-time notifications)
        │ REST
        ▼
[ai-service: FastAPI :8000] ─── Qdrant (vector search)
        │                       ─── SQLite (manga metadata, /app/data/)
        │ LangChain/Groq
        ▼
[Groq LLM API] (external)
```

- **fe-service** (`fe-service/`): React 19 + TypeScript + Vite + Tailwind CSS v4. Port 3000 in dev, Nginx serving static in prod.
- **be-service** (`be-service/`): Java 17, Spring Boot 3.3.5, Maven. Handles auth (JWT + Google OAuth), user management, chat session persistence, rate limiting, WebSocket. Flyway migrations. Domain-driven package layout (`domain/`, `features/`, `security/`). **Note:** payOS payments schema exists (V2 migration) but the payment controller/service is not yet implemented. Also, `be-service/docker-compose.dev.yml` references a `Dockerfile.dev` that does not exist — be-service cannot currently be built via the dev compose file.
- **ai-service** (`ai-service/`): Python ≥3.12, FastAPI, `uv` package manager. Two containers: `ai-service` (API server) and `ai-worker` (background scheduler running `scripts/scheduler.py` — auto-seeds MangaDex data + daily delta sync). LangGraph chatbot with Groq LLM, sentence-transformers embeddings, Qdrant vector search. Alembic migrations for SQLite.

## Reference Documentation

Detailed, maintainer-authored docs live in `docs/` — read these before deep work in a service:
- `docs/system-architecture.md` — full architecture, data flows (recommendation / chat RAG / auth / payment / AI worker), DB design, security, container map.
- `docs/code-standards.md` — per-service coding standards (the summary below is the condensed version).
- `docs/deployment-guide.md` — production deployment; `docs/docker_architecture.md` — compose include pattern.
- `docs/project-roadmap.md` / `docs/project-overview-pdr.md` / `docs/design-guidelines.md` — product roadmap, overview, design guidelines.

## Build, Test, Lint

### ai-service (Python)

```bash
cd ai-service
uv sync                # install dependencies
uv run ruff check .    # lint
uv run pytest          # run all tests
uv run pytest tests/test_search.py -v   # single test file
```

### be-service (Java)

```bash
cd be-service
./mvnw test            # run all tests (includes JaCoCo coverage report)
./mvnw test -Dtest=ChatServiceImplTest   # single test class
./mvnw compile         # compile only
```

### fe-service (TypeScript/React)

```bash
cd fe-service
npm ci                 # install dependencies
npm run lint           # ESLint
npm run format:check   # Prettier check
npm run test           # Vitest (all)
npm run test:cov       # Vitest with coverage
npm run build          # type-check + production build
```

## Docker Compose Structure

Root compose files use `include:` to merge per-service compose files:
- `docker-compose.dev.yml` → `{service}/docker-compose.dev.yml` (dev, hot-reload)
- `docker-compose.yml` → `{service}/docker-compose.yml` (production, multi-stage builds)

All services share `mangarec_global_network` (external Docker network).

## Key Environment Variables

**ai-service (.env):** `GROQ_API_KEY` (required), `QDRANT_HOST`, `QDRANT_PORT`, `DATABASE_URL` (SQLite path), `LANGCHAIN_API_KEY` (optional, for LangSmith tracing).
**be-service (.env):** `POSTGRES_*`, `SPRING_DATASOURCE_URL`, `REDIS_HOST`/`REDIS_PORT`, `GOOGLE_AUTH_CLIENT_ID`, `SPRING_MAIL_*` (SMTP for OTP), `PAYOS_*` (payment gateway), `AI_SERVICE_URL` (default: `http://localhost:8000`).
**fe-service (.env):** `VITE_API_BASE_URL`, `VITE_GOOGLE_CLIENT_ID`.

## Git Conventions

- **Branch naming:** `{type}/MR-{ticket_number}-{short-description}` (e.g., `feat/MR-0021-chat-rate-limit`)
- **Commit format:** `MR-XXXX | <type>: <description>` — types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`
- **Main branch:** `develop`

## Code Standards (project-specific)

- **API:** Endpoints use `/api/v1/` (auth) or `/api/` (chat, guests) prefixes — not yet fully unified. Response envelope: `{ success, data, message, pagination? }`.
- **BE:** Constructor injection (no `@Autowired` on fields). Flyway migrations in `resources/db/migration/`. Lombok for boilerplate reduction. Schema namespace: `mangarec`.
- **AI:** Type hints on all function signatures. Pydantic models for request/response validation. Repository pattern — no direct DB queries from service layer. Ruff config: line-length 88, rules E/F/I/W/B.
- **FE:** Functional components + hooks only. Tailwind utility classes (no inline styles except dynamic values). Component files organized by feature: `components/common/`, `components/chat/`, etc.
- **Testing:** TDD expected, 80%+ coverage target. BE: JUnit 5 + Mockito. AI: pytest (mock external APIs). FE: Vitest + React Testing Library (test behavior, not implementation).
