# AIOS — AI Operating System

Enterprise-grade, secure-by-design personal AI assistant platform. The backend abstracts LLM providers through an internal **AI Gateway** that enforces security, routing, resilience, and audit logging before any external model call.

## Core Principles

- **Secure-by-Design** — Zero trust, least privilege, no sensitive data in logs
- **Gateway-first** — All LLM traffic flows through `AIGatewayService`, never directly from controllers
- **Modular packages** — Strict separation by domain (`gateway`, `ai`, `chat`, `memory`, `auth`, `common`, `config`)

## Tech Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 21 |
| Framework | Spring Boot 3.5.15 |
| AI | Spring AI 1.1.8 → LiteLLM proxy → Ollama / cloud APIs |
| Database | PostgreSQL 16 + Flyway |
| Cache | Redis 7 |
| Security | Spring Security (stateless API key stub → JWT planned) |
| Build | Gradle 8.14 |
| Deploy | Docker Compose (local) |

## Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- LiteLLM proxy running at `http://localhost:4000` (separate setup)

### 1. Start infrastructure

```bash
docker compose up -d
```

### 2. Run the backend

```bash
cd backend
./gradlew bootRun        # Linux/macOS
.\gradlew.bat bootRun    # Windows
```

### 3. Verify health

```bash
curl http://localhost:8080/actuator/health
```

### 4. Create a conversation

```bash
curl -X POST http://localhost:8080/api/v1/chat/conversations \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-api-key-change-me" \
  -d '{"title": "Test Session", "model": "qwen3"}'
```

### 5. Send a message

```bash
curl -X POST http://localhost:8080/api/v1/chat/conversations/{conversation-id}/messages \
  -H "Content-Type: application/json" \
  -H "X-API-Key: dev-api-key-change-me" \
  -d '{"content": "Hello, what can you help me with?"}'
```

### Environment setup

```bash
cp .env.example .env          # fill in secrets locally — never commit .env
export SPRING_PROFILES_ACTIVE=local
```

| Profile | File | Use case |
|---------|------|----------|
| `local` (default) | `application-local.yml` | Docker Compose on localhost |
| `dev` | `application-dev.yml` | Shared dev/staging server |
| `prod` | `application-prod.yml` | Production hardened |

## Environment Variables

See [.env.example](.env.example) for the full list. Key variables:

| Variable | Default (local) | Description |
|----------|-----------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `local` | Config profile: `local`, `dev`, `prod` |
| `AIOS_API_KEY` | `dev-api-key-change-me` | API key for `/api/v1/chat/**` |
| `LITELLM_BASE_URL` | `http://localhost:4000` | LiteLLM proxy base URL |
| `LITELLM_API_KEY` | `sk-litellm-local` | LiteLLM API key |
| `POSTGRES_*` | see `.env.example` | PostgreSQL connection |
| `REDIS_*` | see `.env.example` | Redis connection |

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/actuator/health` | None | Health check |
| `POST` | `/api/v1/chat/conversations` | `X-API-Key` | Start new session |
| `POST` | `/api/v1/chat/conversations/{id}/messages` | `X-API-Key` | Send message, invoke AI |

## Documentation

| Document | Purpose |
|----------|---------|
| [docs/CONTEXT.md](docs/CONTEXT.md) | **Start here** — master context for AI agents and developers |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System design, request flows, security model |
| [docs/FILE_STRUCTURE.md](docs/FILE_STRUCTURE.md) | Directory layout and package responsibilities |
| [docs/adr/](docs/adr/) | Architecture Decision Records (Spring Boot, Spring AI, LiteLLM, Ollama, Postgres) |
| [docs/security/](docs/security/) | Threat model and asset inventory |
| [AGENTS.md](AGENTS.md) | Agent workflow: read context → plan → execute |
| [.cursor/rules/](.cursor/rules/) | Cursor AI rules enforced during development |

## Project Layout (summary)

```
AIOS/
├── backend/                 # Spring Boot application
│   └── src/main/java/com/aios/
│       ├── gateway/         # API gateway filters, rate limiting
│       ├── ai/              # AI Gateway service layer
│       ├── chat/            # Conversations & messages
│       ├── memory/          # Context orchestration (placeholder)
│       ├── auth/            # Security configuration
│       ├── common/          # Shared DTOs, exceptions, utilities
│       └── config/          # App-wide Spring configuration
├── docker-compose.yml       # PostgreSQL + Redis
├── docs/                    # Architecture & context documentation
│   ├── adr/                 # Architecture Decision Records
│   └── security/            # Threat model & asset inventory
├── .env.example             # Environment variable template (copy to .env)
└── .cursor/rules/           # AI agent rules
```

## Development

```bash
cd backend
./gradlew build      # compile + test
./gradlew test       # run tests only
```

## Roadmap

- [ ] JWT / OAuth2 resource server (replace API key stub)
- [ ] Redis-backed rate limiting in `gateway`
- [ ] Application-layer message encryption
- [ ] Memory / RAG orchestration in `memory` module
- [ ] LiteLLM + Ollama in Docker Compose
- [ ] Frontend client
