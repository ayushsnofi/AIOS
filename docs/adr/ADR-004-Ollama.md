# ADR-004: Ollama as the Local Model Runtime

| Field | Value |
|-------|-------|
| **Status** | Accepted |
| **Date** | 2026-06-29 |
| **Deciders** | AIOS Core Team |

## Context

AIOS is designed as a secure personal assistant with a **local-first** posture. Users need capable LLMs for chat and embeddings without sending all data to cloud APIs. Hardware constraints and privacy requirements favor on-device or on-LAN inference.

## What Did We Choose?

**Ollama** as the local model runtime, accessed **indirectly** through LiteLLM (not directly from AIOS).

Default model assignments:

| Logical Name | Ollama Model | Use Case |
|--------------|--------------|----------|
| `phi4` | Phi-4 (or equivalent) | Fast utility tasks |
| `qwen3` | Qwen3 | Primary reasoning / main tasks |
| `nomic-embed-text` | Nomic Embed Text | Embedding generation for memory/RAG |

## Why?

1. **Privacy** — Inference can run entirely on local hardware; sensitive prompts never leave the LAN.
2. **Cost** — No per-token cloud billing for development and personal use.
3. **Offline capability** — Assistant functions without internet when models are pulled locally.
4. **Simple operations** — `ollama pull`, `ollama run`; widely adopted for local LLM workflows.
5. **LiteLLM compatibility** — Ollama is a first-class LiteLLM provider; routing is configuration-only.

## Alternatives Considered

| Alternative | Reason Not Chosen |
|-------------|------------------|
| **llama.cpp server directly** | Lower-level; no model management UX; more ops burden. |
| **Cloud-only (OpenAI, Anthropic)** | Data residency and cost concerns for a personal assistant. |
| **LM Studio** | Desktop-focused; less suitable for headless server/Docker deployment. |
| **vLLM** | Better for GPU clusters; heavier setup for personal/small-team use. |
| **Direct Ollama from Spring AI** | Bypasses LiteLLM routing layer (see ADR-003). |

## Trade-offs

| Benefit | Cost |
|---------|------|
| Data stays local | Model quality may lag frontier cloud models |
| No API costs locally | GPU/RAM requirements; slower inference on CPU |
| Simple model management | User must pull and update models manually |
| Works air-gapped | Cold start latency; single-machine throughput limits |
| Good dev experience | `nomic-embed-text` dimensions (768) may differ from configured 1536 — must align config |

## Consequences

- LiteLLM config must register Ollama as a provider with model name mappings
- `aios.ai.default-model: phi4` and `aios.ai.fallback-model: qwen3` in application config
- Embedding dimension in Flyway (`VECTOR`) and `aios.memory.embedding-dimensions` must match the Ollama embedding model output
- Ollama is not containerized in AIOS `docker-compose.yml` yet (runs on host by default)

## Operational Requirements

```bash
ollama pull phi4
ollama pull qwen3
ollama pull nomic-embed-text
```

Ensure LiteLLM `config.yaml` routes `phi4`, `qwen3`, and `nomic-embed-text` to the corresponding Ollama models.

## References

- [ADR-003-LiteLLM.md](./ADR-003-LiteLLM.md)
- [ADR-002-Spring-AI.md](./ADR-002-Spring-AI.md)
- Ollama: https://ollama.com/
