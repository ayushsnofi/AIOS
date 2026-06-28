# AIOS — Master Context Document

> **This is the single source of truth for AI agents and developers working on AIOS.**
> Read this file completely before planning or writing any code.

## What Is AIOS?

AIOS (AI Operating System) is a secure personal assistant platform. It is **not** a thin wrapper around an LLM. Every AI request passes through internal gateway layers that enforce security policy, model routing, retry/fallback, and audit logging before reaching external endpoints.

## Non-Negotiable Rules

1. **Never bypass the AI Gateway** — Controllers and services must call `AIGatewayService`, not `ChatModel` directly.
2. **Never log prompt content** — Audit logs record metadata only (model, latency, tokens, conversation ID).
3. **Never weaken security defaults** — Stateless sessions, strict headers, authenticated chat endpoints.
4. **Never use `ddl-auto: update`** — Schema changes go through Flyway migrations only.
5. **Respect package boundaries** — Each module owns its domain; cross-module calls go through service interfaces.

## Architecture Layers (top → bottom)

```
Client Request
    │
    ▼
GatewayRequestFilter          ← gateway/ — rate limiting stub, request shaping
    │
    ▼
ApiKeyAuthenticationFilter    ← auth/ — zero-trust gate (JWT planned)
    │
    ▼
ChatController                ← chat/ — REST API, validation only
    │
    ▼
ChatService                   ← chat/ — transactions, persistence
    │
    ▼
AIGatewayService              ← ai/ — THE core AI execution pipeline
    ├── PromptFirewall        ← blocks jailbreak / override patterns
    ├── MemoryService         ← semantic retrieval + poisoning guard (Phase 2)
    ├── SemanticCacheService  ← Redis cosine-similarity cache
    ├── ModelRouter           ← phi4 (utility) / qwen3 (main)
    ├── ChatModel (Spring AI) ← OpenAI-compatible client → LiteLLM
    └── AuditLogger           ← async metadata logging
    │
    ▼
LiteLLM Proxy → Ollama / Cloud APIs
```

## Package Responsibilities

| Package | Owns | Must NOT |
|---------|------|----------|
| `gateway` | Request filters, rate limiting | Business logic, DB access |
| `ai` | LLM routing, firewall, audit | REST endpoints, JPA entities |
| `chat` | Conversations, messages, chat API | Direct LLM calls |
| `memory` | Context orchestration, RAG, ingestion guard, semantic cache | Chat persistence |
| `auth` | Security config, authentication filters | Business logic |
| `common` | DTOs, exceptions, shared utilities | Domain-specific logic |
| `config` | Spring beans, properties binding | Feature implementation |

## Key Classes

| Class | Location | Role |
|-------|----------|------|
| `AIGatewayService` | `ai/` | Orchestrates firewall → memory retrieval → route → execute → audit |
| `MemoryService` | `memory/` | Hybrid vector retrieval, ingestion guard, save pipeline |
| `MemoryIngestionGuard` | `memory/` | Poisoning protection for external/untrusted content |
| `SemanticCacheService` | `memory/cache/` | Redis-backed embedding similarity cache |
| `PromptFirewall` | `ai/` | Validates prompts against security patterns |
| `ModelRouter` | `ai/` | Resolves model aliases to `phi4` or `qwen3` |
| `ChatService` | `chat/` | Transactional chat operations |
| `ChatController` | `chat/` | `POST /api/v1/chat/**` endpoints |
| `SecurityConfig` | `auth/` | Stateless security, CORS, headers |
| `GlobalExceptionHandler` | `common/` | Unified error responses |

## Database Schema

- **conversations** — `id` (UUID), `title`, `created_at`, `updated_at`, `model_used`
- **messages** — `id` (UUID), `conversation_id` (FK), `role` (user/assistant/system), `content` (TEXT), `tokens_used`, `latency_ms`, `created_at`
- **memories** — `id` (UUID), `content` (TEXT), `embedding` (VECTOR 1536), `metadata` (JSONB: source, trust_score, human_approved, tags), `created_at`
- Migrations live in `backend/src/main/resources/db/migration/`
- JPA entities: `Conversation.java`, `Message.java` in `chat/entity/`; `Memory.java` in `memory/`

## Configuration

- Main config: `backend/src/main/resources/application.yml`
- Custom properties prefix: `aios.*` (bound via `AiosProperties`)
- AI models configured via `aios.ai.default-model` and `aios.ai.fallback-model`
- LiteLLM connection via `spring.ai.openai.base-url`

## Supported Models

| Alias | Resolved Model | Use Case |
|-------|---------------|----------|
| `phi4`, `phi-4`, `utility` | `phi4` | Fast utility tasks |
| `qwen3`, `qwen-3`, `main` | `qwen3` | Primary reasoning tasks |

## Adding New Features — Decision Guide

| If you need to... | Work in... |
|-------------------|------------|
| Add a new API endpoint | `chat/controller/` + `chat/service/` |
| Change LLM routing logic | `ai/ModelRouter.java` |
| Add prompt security rules | `ai/PromptFirewall.java` |
| Add a DB table | Flyway migration + `chat/entity/` |
| Add authentication | `auth/` |
| Add rate limiting | `gateway/RateLimitingService.java` |
| Add context/memory | `memory/MemoryService.java`, `MemoryIngestionGuard.java` |
| Add shared error type | `common/exception/` |

## Agent Workflow (mandatory)

```
1. READ   → docs/CONTEXT.md (this file) + relevant docs/ARCHITECTURE.md sections
2. PLAN   → Identify affected packages, state the approach, list files to create/modify
3. EXECUTE → Implement with minimal scope, matching existing conventions
4. VERIFY → Run `./gradlew build` in backend/
```

## Related Documents

- [ARCHITECTURE.md](ARCHITECTURE.md) — detailed system design
- [FILE_STRUCTURE.md](FILE_STRUCTURE.md) — complete file tree
- [../AGENTS.md](../AGENTS.md) — agent instructions
- [../.cursor/rules/](../.cursor/rules/) — enforced Cursor rules
