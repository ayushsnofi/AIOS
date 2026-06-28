# AIOS — Agent Instructions

Instructions for AI coding agents (Cursor, Copilot, etc.) working in this repository.

## Mandatory Workflow

Every task must follow this sequence. **Do not skip steps.**

### Step 1 — READ CONTEXT (required)

Before planning or writing code, read these files in order:

1. **`docs/CONTEXT.md`** — master context, non-negotiable rules, package boundaries
2. **`docs/ARCHITECTURE.md`** — system design (read relevant sections for your task)
3. **`docs/FILE_STRUCTURE.md`** — locate where new code belongs

If the task touches security or AI routing, also read:

- `.cursor/rules/security-standards.mdc`
- `.cursor/rules/aios-architecture.mdc`

### Step 2 — PLAN (required)

Before editing files, state:

- **Goal** — what the change accomplishes
- **Affected packages** — which modules are touched (e.g., `ai/`, `chat/`)
- **Files to create/modify** — explicit list
- **Approach** — brief description of the implementation
- **Risks** — security, breaking changes, migration needs

Do not start coding until the plan is clear.

### Step 3 — EXECUTE

- Match existing code style and conventions
- Keep changes minimal and focused on the task
- Place code in the correct package (see `docs/FILE_STRUCTURE.md`)
- Never bypass `AIGatewayService` for LLM calls
- Never log sensitive data (prompts, keys, message content)
- Schema changes require Flyway migrations, not Hibernate DDL

### Step 4 — VERIFY

```bash
cd backend
./gradlew build    # must pass
```

Fix any compilation or test failures before marking the task complete.

## Hard Constraints

| Rule | Detail |
|------|--------|
| Gateway-only LLM access | `ChatModel` is injected only in `ai/AIGatewayService` |
| No prompt logging | Audit logs contain metadata only |
| Stateless security | No server-side sessions |
| Flyway for schema | Never use `ddl-auto: update` or `create` in production config |
| Package boundaries | Controllers don't call repositories directly; services own transactions |
| API versioning | All endpoints under `/api/v1/` |
| Auth on chat | All `/api/v1/chat/**` requires authentication |

## Package → Responsibility Map

```
gateway/  → filters, rate limiting, request shaping
ai/       → LLM gateway, firewall, routing, audit
chat/     → conversations, messages, REST API
memory/   → context orchestration (future)
auth/     → security configuration
common/   → DTOs, exceptions, shared utilities
config/   → Spring beans, property binding
```

## Common Tasks

### Add a new API endpoint

1. DTO in `chat/dto/`
2. Service method in `chat/service/ChatService.java`
3. Controller mapping in `chat/controller/ChatController.java`
4. Register auth rule in `auth/SecurityConfig.java` if new path prefix

### Add a database table

1. Flyway migration in `backend/src/main/resources/db/migration/`
2. Entity in appropriate package (usually `chat/entity/`)
3. Repository interface
4. Service layer integration

### Modify AI behavior

1. Work in `ai/` package only
2. Update `ModelRouter` for new models
3. Update `PromptFirewall` for new security rules
4. Update `application.yml` and `AiosProperties` for new config

## Documentation Updates

When making architectural changes, update the corresponding doc:

| Change type | Update |
|-------------|--------|
| New module or package | `docs/FILE_STRUCTURE.md` |
| New service or data flow | `docs/ARCHITECTURE.md` |
| New rule or constraint | `docs/CONTEXT.md` + `.cursor/rules/` |
