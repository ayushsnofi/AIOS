-- Phase 2: Long-term memory with pgvector semantic search
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE memories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content    TEXT NOT NULL,
    embedding  vector(1536),
    metadata   JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE memories IS 'Long-term semantic memory store with trust-classified metadata';
COMMENT ON COLUMN memories.embedding IS 'Null when pending human approval (EXTERNAL_SOURCE poisoning guard)';
COMMENT ON COLUMN memories.metadata IS 'source, trust_score, human_approved, tags, timestamp';

CREATE INDEX idx_memories_metadata_gin ON memories USING GIN (metadata jsonb_path_ops);

CREATE INDEX idx_memories_embedding_hnsw
    ON memories
    USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

CREATE INDEX idx_memories_created_at ON memories (created_at DESC);
