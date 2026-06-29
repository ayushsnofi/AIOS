# ADR-005: PostgreSQL with pgvector as the Primary Data Store

| Field | Value |
|-------|-------|
| **Status** | Accepted |
| **Date** | 2026-06-29 |
| **Deciders** | AIOS Core Team |

## Context

AIOS persists conversations, messages, and long-term semantic memories. Phase 2 introduced vector embeddings for RAG. We need ACID transactions, JSON metadata filtering, mature tooling, and vector similarity search in one database engine.

## What Did We Choose?

**PostgreSQL 16** with the **pgvector** extension (`pgvector/pgvector:pg16` Docker image), managed via:

- **Flyway** for all schema migrations (`V1__init_schema.sql`, `V2__memory_and_vector_setup.sql`)
- **Spring Data JPA** for relational entities (`conversations`, `messages`, `memories`)
- **Spring AI PgVectorStore** for embedding storage and similarity search on `memories`
- **HNSW index** on `embedding` column for cosine distance queries
- **JSONB** on `memories.metadata` for trust scores, source classification, and tags

Hibernate `ddl-auto: validate` — schema is never auto-generated.

## Why?

1. **Unified store** — Relational chat data and vector memory in one database reduces sync complexity.
2. **pgvector maturity** — HNSW indexes, cosine distance, and JSONB filters support hybrid retrieval.
3. **ACID transactions** — `ChatService` transactional boundaries for conversation/message integrity.
4. **Flyway discipline** — Versioned, reviewable schema changes align with secure-by-design principles.
5. **Operational familiarity** — PostgreSQL is well-understood for backup, replication, and monitoring.

## Alternatives Considered

| Alternative | Reason Not Chosen |
|-------------|-------------------|
| **PostgreSQL + separate Pinecone/Weaviate** | Two data stores; consistency and ops overhead for personal-scale deployment. |
| **MongoDB Atlas Vector Search** | Document model mismatch for relational chat; less natural JSONB + SQL hybrid queries. |
| **Redis as primary DB** | No durable relational model; Redis used only for semantic cache. |
| **H2 (production)** | Not suitable for production vector workloads or pgvector. |
| **Elasticsearch** | Heavier ops footprint for current scope; better as future search add-on. |
| **Hibernate `ddl-auto: update`** | Forbidden by project security rules; Flyway only. |

## Trade-offs

| Benefit | Cost |
|---------|------|
| Single database for SQL + vectors | pgvector extension required; not vanilla Postgres |
| JSONB metadata filtering + HNSW | Hybrid JPA + VectorStore writes need careful design |
| Strong consistency for chat | Vector index rebuilds can be expensive at scale |
| Flyway audit trail for schema | Migration discipline required for every schema change |
| Docker Compose local parity | Production HA/replication not configured in Phase 2 |

## Schema Summary

| Table | Purpose |
|-------|---------|
| `conversations` | Chat sessions (UUID PK, title, model_used) |
| `messages` | User/assistant/system messages per conversation |
| `memories` | Long-term memory with `embedding vector(1536)`, JSONB metadata |

## Consequences

- Docker Compose uses `pgvector/pgvector:pg16` not `postgres:16-alpine`
- `spring.ai.vectorstore.pgvector.initialize-schema: false` — Flyway owns DDL
- Pending external memories stored without embedding until human approval
- Connection pooling via HikariCP; credentials from environment variables in prod

## References

- [docs/ARCHITECTURE.md](../ARCHITECTURE.md) — Database Design
- [docs/security/THREATS.md](../security/THREATS.md) — SQL injection mitigations
- pgvector: https://github.com/pgvector/pgvector
