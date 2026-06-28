CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE conversations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title       VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    model_used  VARCHAR(100)
);

CREATE TABLE messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id  UUID NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    role             VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content          TEXT NOT NULL,
    tokens_used      INT,
    latency_ms       BIGINT,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation_id ON messages (conversation_id);
CREATE INDEX idx_conversations_updated_at ON conversations (updated_at DESC);

COMMENT ON COLUMN messages.content IS 'Encrypted payload placeholder — application-layer encryption to be applied';
