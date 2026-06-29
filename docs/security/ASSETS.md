# AIOS Security — Asset Inventory

> **Purpose:** Identify what we protect, sensitivity levels, and storage locations.
> Companion document: [THREATS.md](./THREATS.md)

## Asset Classification Legend

| Level | Meaning |
|-------|---------|
| **Critical** | Compromise causes severe privacy, financial, or identity harm |
| **High** | Sensitive personal data; regulated or irreversible if leaked |
| **Medium** | Operational data; limited harm if disclosed |
| **Low** | Public or easily rotated metadata |

---

## Assets

### 1. Gmail

| Attribute | Detail |
|-----------|--------|
| **Description** | Email content, headers, attachments, thread metadata accessed via future Gmail integration |
| **Sensitivity** | Critical |
| **Storage (planned)** | Encrypted at rest in PostgreSQL; transient processing in memory only |
| **Access path** | OAuth-scoped API (read/send) via authenticated user session |
| **Owner** | End user |

**Protection requirements:**
- OAuth scopes limited to minimum necessary (least privilege)
- Email bodies never logged in plain text
- External email content ingested into memory must pass `MemoryIngestionGuard` as `EXTERNAL_SOURCE`

---

### 2. OAuth Tokens

| Attribute | Detail |
|-----------|--------|
| **Description** | Access tokens, refresh tokens, and token metadata for Gmail, Calendar, and third-party integrations |
| **Sensitivity** | Critical |
| **Storage (planned)** | Encrypted column or vault (e.g. application-level envelope encryption); never in logs |
| **Access path** | `auth/` module only; services receive scoped credentials via secure token service |
| **Owner** | End user |

**Protection requirements:**
- Encrypt at rest with per-user or per-tenant keys
- Rotate refresh tokens; revoke on logout or compromise
- Never expose tokens in API responses, error messages, or frontend URLs
- Short-lived access tokens where possible

---

### 3. Personal Notes

| Attribute | Detail |
|-----------|--------|
| **Description** | User-authored notes, journals, and free-form text used as assistant context |
| **Sensitivity** | High |
| **Storage** | PostgreSQL (`messages`, future `notes` table); encryption at application layer planned |
| **Access path** | Chat API + future notes API; authenticated requests only |
| **Owner** | End user |

**Protection requirements:**
- Authenticated API access only
- No note content in application logs or audit trails
- Prompt firewall on all user-submitted text before LLM calls

---

### 4. Memory (Long-Term Semantic Store)

| Attribute | Detail |
|-----------|--------|
| **Description** | Vectorized long-term memories with JSONB metadata (`source`, `trust_score`, `human_approved`, `tags`) |
| **Sensitivity** | High |
| **Storage** | PostgreSQL `memories` table + pgvector embeddings; Redis semantic cache (derived, TTL-bound) |
| **Access path** | `MemoryService` via `AIGatewayService`; no direct public API in Phase 2 |
| **Owner** | End user |

**Protection requirements:**
- `MemoryIngestionGuard` blocks unapproved `EXTERNAL_SOURCE` vector insertion
- Retrieval filtered by `trust_score` and `human_approved`
- Memory content treated as untrusted in system prompts (`MemoryContextPromptBuilder` anti-injection instructions)
- Redis cache entries expire (`semantic-cache-ttl-seconds`)

---

### 5. Calendar

| Attribute | Detail |
|-----------|--------|
| **Description** | Events, attendees, locations, reminders from Google Calendar or similar |
| **Sensitivity** | High |
| **Storage (planned)** | PostgreSQL; synced via OAuth |
| **Access path** | Future calendar integration service; read/write scoped by OAuth |
| **Owner** | End user |

**Protection requirements:**
- Read-only by default; write scopes require explicit user consent
- Calendar data classified as `EXTERNAL_SOURCE` when ingested into memory
- No calendar PII in logs

---

### 6. Contacts

| Attribute | Detail |
|-----------|--------|
| **Description** | Names, emails, phone numbers, relationships from contact providers |
| **Sensitivity** | High |
| **Storage (planned)** | PostgreSQL with field-level encryption for phone/email |
| **Access path** | Future contacts API; authenticated only |
| **Owner** | End user |

**Protection requirements:**
- Minimize fields stored; retention policy per user preference
- Never send full contact export to LLM without user action
- Redact in audit logs

---

### 7. Voice Recordings

| Attribute | Detail |
|-----------|--------|
| **Description** | Audio recordings from voice input; transcriptions derived from speech-to-text |
| **Sensitivity** | Critical |
| **Storage (planned)** | Object storage (encrypted blobs); transcriptions in PostgreSQL |
| **Access path** | Future voice pipeline; local STT preferred where possible |
| **Owner** | End user |

**Protection requirements:**
- Encrypt audio at rest; configurable retention and deletion
- Transcriptions subject to same prompt firewall and memory ingestion rules as text
- Voice data never used for model training
- Prefer on-device or local STT (Ollama/Whisper) over cloud STT for sensitive use cases

---

## Asset → Component Map

| Asset | Primary Module | Database / Cache |
|-------|----------------|------------------|
| Gmail | `auth/` + future integrations | PostgreSQL (planned) |
| OAuth tokens | `auth/` | PostgreSQL / vault (planned) |
| Personal notes | `chat/` | PostgreSQL |
| Memory | `memory/` | PostgreSQL + Redis |
| Calendar | future integrations | PostgreSQL (planned) |
| Contacts | future integrations | PostgreSQL (planned) |
| Voice recordings | future voice module | Object store + PostgreSQL (planned) |

## Data Flow Principles

1. **Least privilege** — Each integration receives minimum OAuth scopes.
2. **No plain-text logging** — Content of any asset above is never written to logs.
3. **Gateway mediation** — LLM access only through `AIGatewayService`.
4. **Untrusted until classified** — External payloads require metadata classification before permanent memory storage.

## Related Documents

- [THREATS.md](./THREATS.md)
- [../adr/ADR-005-Postgres.md](../adr/ADR-005-Postgres.md)
- [../CONTEXT.md](../CONTEXT.md)
