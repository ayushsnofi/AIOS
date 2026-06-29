# ADR-002: Spring AI as the LLM & Embedding Abstraction

| Field | Value |
|-------|-------|
| **Status** | Accepted |
| **Date** | 2026-06-29 |
| **Deciders** | AIOS Core Team |

## Context

AIOS must abstract LLM providers behind an internal AI Gateway. The gateway needs unified APIs for chat completion, embeddings, vector storage, retry/fallback, and future tool-calling — without locking the platform to a single vendor.

## What Did We Choose?

**Spring AI 1.1.8** (BOM-managed) as the abstraction layer for:

- `ChatModel` — chat completions (wrapped exclusively by `AIGatewayService`)
- `EmbeddingModel` — text embeddings (`nomic-embed-text` via LiteLLM)
- `VectorStore` — pgvector-backed semantic memory (`PgVectorStore`)
- `spring-ai-starter-model-openai` — OpenAI-compatible client pointed at LiteLLM
- `spring-ai-starter-vector-store-pgvector` — PostgreSQL vector store

All LLM access is mediated by `com.aios.ai.AIGatewayService`; controllers and chat services never inject `ChatModel` directly.

## Why?

1. **Vendor-neutral interface** — Swap LiteLLM routes (Ollama, OpenAI, Anthropic) without rewriting gateway logic.
2. **Native Spring Boot integration** — Auto-configuration, property binding, and observability hooks align with our stack.
3. **Vector store abstraction** — `VectorStore` + `PgVectorStore` integrates with our `memories` table and hybrid retrieval pipeline.
4. **Prompt model** — `SystemMessage`, `UserMessage`, `Prompt`, and `ChatOptions` support RAG injection via `MemoryContextPromptBuilder`.
5. **BOM versioning** — `spring-ai-bom:1.1.8` pins compatible artifact versions across starters.

## Alternatives Considered

| Alternative | Reason Not Chosen |
|-------------|-------------------|
| **Direct HTTP to Ollama/OpenAI** | No unified retry, observability, or embedding/vector abstractions; higher maintenance. |
| **LangChain4j** | Viable JVM option, but less aligned with Spring Boot auto-config and our Spring Security model. |
| **Raw LiteLLM HTTP client only** | Loses `EmbeddingModel` and `VectorStore` portability. |
| **OpenAI Java SDK directly** | Vendor-coupled; no embedding/vector store integration. |
| **Python sidecar (FastAPI + LangChain)** | Split runtime increases operational and security boundary complexity. |

## Trade-offs

| Benefit | Cost |
|---------|------|
| Clean abstraction over chat + embeddings + vectors | Spring AI APIs evolve quickly; upgrades require testing |
| OpenAI-compatible mode works with LiteLLM | Assumes LiteLLM maintains OpenAI API compatibility |
| `PgVectorStore` reduces custom vector SQL | Dual write paths (JPA + VectorStore) need careful orchestration |
| Centralized gateway enforcement | Extra indirection vs. direct model calls |
| Metadata-only audit logging compatible | Must discipline developers not to log `Prompt` content |

## Consequences

- `ChatModel` is injected only in `ai/AIGatewayService`
- Embeddings configured via `spring.ai.openai.embedding.options.model`
- Vector store table: `memories` (Flyway-owned schema, `initialize-schema: false`)
- RAG flow: `MemoryService.retrieveRelevantContext()` → `MemoryContextPromptBuilder` → downstream LLM

## References

- [docs/ARCHITECTURE.md](../ARCHITECTURE.md) — AI Gateway pipeline
- [ADR-003-LiteLLM.md](./ADR-003-LiteLLM.md)
- [ADR-004-Ollama.md](./ADR-004-Ollama.md)
- Spring AI 1.1 reference documentation
