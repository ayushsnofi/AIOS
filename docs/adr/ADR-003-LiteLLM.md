# ADR-003: LiteLLM as the LLM Routing Proxy

| Field | Value |
|-------|-------|
| **Status** | Accepted |
| **Date** | 2026-06-29 |
| **Deciders** | AIOS Core Team |

## Context

AIOS must route requests to multiple model backends (local Ollama models, future cloud APIs) through a single controlled egress point. The backend should not hold credentials for every provider or implement per-vendor HTTP clients.

## What Did We Choose?

**LiteLLM** as a local (or centrally deployed) **OpenAI-compatible proxy** that Spring AI talks to via:

```yaml
spring.ai.openai.base-url: http://localhost:4000
spring.ai.openai.api-key: ${LITELLM_API_KEY}
```

LiteLLM routes model names (`phi4`, `qwen3`, `nomic-embed-text`) to configured downstream providers (primarily Ollama in local development).

## Why?

1. **Single egress point** — All LLM traffic exits through one proxy, simplifying firewall rules and audit.
2. **OpenAI-compatible API** — Spring AI's OpenAI starter works without custom adapters.
3. **Model routing** — LiteLLM config maps logical model names to Ollama or cloud endpoints.
4. **Centralized API keys** — Cloud provider keys live in LiteLLM, not in the AIOS backend JAR.
5. **Future flexibility** — Add fallbacks, load balancing, and budget caps in one place without changing AIOS gateway code.

## Alternatives Considered

| Alternative | Reason Not Chosen |
|-------------|-------------------|
| **Direct Ollama API from Spring AI** | No unified path for cloud models; separate clients per provider. |
| **Custom Java router in `ai/` package** | Reinvents LiteLLM; higher maintenance and security review burden. |
| **OpenRouter / Together.ai only** | Cloud-dependent; conflicts with local-first and data residency goals. |
| **vLLM / TGI directly** | Lower-level; no unified chat + embedding routing for multiple models. |
| **Spring AI dynamic model per provider** | Multiple `ChatModel` beans; complex credential and routing management. |

## Trade-offs

| Benefit | Cost |
|---------|------|
| One integration surface for Spring AI | Additional service to deploy and monitor |
| Provider keys isolated from AIOS | LiteLLM becomes a critical dependency and attack surface |
| Easy model alias mapping | OpenAI API compatibility assumptions may break on edge cases |
| Supports local + cloud hybrid | Network hop adds latency vs. direct Ollama |
| Operational knobs (rate limits, budgets) in proxy | Two config surfaces: AIOS `ModelRouter` + LiteLLM config |

## Consequences

- `LiteLLMClient` stub exists for future health checks and model listing
- `ModelRouter` resolves logical aliases before requests reach LiteLLM
- `LITELLM_BASE_URL` and `LITELLM_API_KEY` are environment variables (see `.env.example`)
- LiteLLM is not yet in `docker-compose.yml` — manual or future Compose service

## Security Notes

- LiteLLM must not be exposed publicly without authentication
- API key between AIOS and LiteLLM should be rotated in production
- LiteLLM logs must not contain end-user prompt content in production

## References

- [ADR-002-Spring-AI.md](./ADR-002-Spring-AI.md)
- [ADR-004-Ollama.md](./ADR-004-Ollama.md)
- LiteLLM documentation: https://docs.litellm.ai/
