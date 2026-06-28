# AIOS File Structure

Complete reference for the project directory layout, package conventions, and file responsibilities.

## Repository Root

```
AIOS/
├── README.md                          # Project overview and quick start
├── AGENTS.md                          # AI agent workflow instructions
├── docker-compose.yml                 # PostgreSQL + Redis for local dev
├── package.json                       # Node deps (MCP SDK — ancillary)
├── docs/
│   ├── CONTEXT.md                     # ★ Master context — read first
│   ├── ARCHITECTURE.md                # System design and flows
│   └── FILE_STRUCTURE.md              # This file
├── .cursor/
│   └── rules/                         # Cursor AI enforcement rules
│       ├── 00-read-context-first.mdc
│       ├── aios-architecture.mdc
│       ├── java-backend-standards.mdc
│       └── security-standards.mdc
└── backend/                           # Spring Boot application
```

## Backend Root

```
backend/
├── build.gradle                       # Dependencies, Java 21 toolchain
├── settings.gradle                    # Project name: aios-backend
├── gradlew / gradlew.bat              # Gradle wrapper scripts
├── gradle/wrapper/                    # Wrapper JAR and properties
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/aios/
    │   └── resources/
    └── test/
        ├── java/com/aios/
        └── resources/
```

## Java Source — `com.aios`

### Root

| File | Purpose |
|------|---------|
| `AiosApplication.java` | Spring Boot entry point, `@EnableAsync` |

### `gateway/` — API Gateway Layer

| File | Purpose |
|------|---------|
| `GatewayRequestFilter.java` | Servlet filter — gateway headers, future request shaping |
| `RateLimitingService.java` | Stub — future Redis token-bucket rate limiting |

### `ai/` — AI Gateway Service Layer

| File | Purpose |
|------|---------|
| `AIGatewayService.java` | **Core** — firewall → route → execute → retry → fallback → audit |
| `AIGatewayResult.java` | Value object — content, model, tokens, latency |
| `PromptFirewall.java` | Input validation — jailbreak detection, length limits |
| `ModelRouter.java` | Model alias resolution (`phi4`, `qwen3`) and fallback |
| `AuditLogger.java` | Async metadata-only audit logging |
| `LiteLLMClient.java` | Stub — future direct LiteLLM admin/health operations |

### `chat/` — Conversation Domain

```
chat/
├── controller/
│   └── ChatController.java            # REST: POST /api/v1/chat/**
├── service/
│   └── ChatService.java               # @Transactional business logic
├── repository/
│   ├── ConversationRepository.java    # JPA repo for conversations
│   └── MessageRepository.java         # JPA repo for messages
├── entity/
│   ├── Conversation.java              # JPA entity
│   ├── Message.java                   # JPA entity
│   ├── MessageRole.java               # Enum: USER, ASSISTANT, SYSTEM
│   └── MessageRoleConverter.java      # JPA converter (lowercase DB values)
└── dto/
    ├── CreateConversationRequest.java
    ├── SendMessageRequest.java
    ├── ConversationResponse.java
    ├── MessageResponse.java
    └── ChatExchangeResponse.java
```

### `memory/` — Context Orchestration (Placeholder)

| File | Purpose |
|------|---------|
| `MemoryOrchestrationService.java` | Stub — future RAG, long-term memory, context windows |

### `auth/` — Security

| File | Purpose |
|------|---------|
| `SecurityConfig.java` | Filter chain, CORS, headers, endpoint auth rules |
| `ApiKeyAuthenticationFilter.java` | Placeholder API key auth (→ JWT planned) |

### `common/` — Shared Utilities

```
common/
├── dto/
│   ├── ApiResponse.java               # Uniform success envelope
│   └── ApiErrorResponse.java          # Uniform error envelope
├── exception/
│   ├── GlobalExceptionHandler.java    # @RestControllerAdvice
│   ├── ResourceNotFoundException.java
│   └── PromptFirewallException.java
├── interceptor/
│   └── RequestLoggingInterceptor.java
└── security/
    └── SecurityUtils.java             # Principal extraction helper
```

### `config/` — Spring Configuration

| File | Purpose |
|------|---------|
| `AiosProperties.java` | `@ConfigurationProperties("aios")` binding |
| `JpaConfig.java` | JPA repositories, transaction management |
| `RedisConfig.java` | RedisTemplate bean configuration |
| `WebConfig.java` | CORS registry, interceptor registration |
| `AsyncConfig.java` | Async executor configuration placeholder |

## Resources — `src/main/resources`

```
resources/
├── application.yml                    # Main configuration
└── db/migration/
    └── V1__init_schema.sql            # Flyway: conversations + messages tables
```

## Test — `src/test`

```
test/
├── java/com/aios/
│   └── AiosApplicationTests.java      # Context load smoke test
└── resources/
    └── application-test.yml           # H2 in-memory, Flyway disabled
```

## Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Packages | lowercase, singular domain | `com.aios.chat.service` |
| Classes | PascalCase, role suffix | `ChatService`, `ChatController` |
| DTOs | `{Action}{Entity}Request/Response` | `SendMessageRequest` |
| Entities | Singular noun | `Conversation`, `Message` |
| Repositories | `{Entity}Repository` | `MessageRepository` |
| Flyway migrations | `V{version}__{description}.sql` | `V1__init_schema.sql` |
| Config properties | kebab-case in YAML | `aios.ai.default-model` |
| API paths | `/api/v1/{domain}/...` | `/api/v1/chat/conversations` |

## Where to Add New Code

| Task | Location |
|------|----------|
| New REST endpoint | `chat/controller/` + `chat/service/` + `chat/dto/` |
| New DB table | `db/migration/V{n}__*.sql` + `chat/entity/` + `chat/repository/` |
| New LLM behavior | `ai/AIGatewayService.java` or `ai/ModelRouter.java` |
| New security rule | `ai/PromptFirewall.java` or `auth/` |
| New config property | `application.yml` + `config/AiosProperties.java` |
| New shared exception | `common/exception/` + register in `GlobalExceptionHandler` |
| New gateway feature | `gateway/` |
| New memory feature | `memory/` |

## Build Artifacts (generated, not committed)

```
backend/build/
├── classes/                           # Compiled .class files
├── libs/
│   ├── aios-backend-0.0.1-SNAPSHOT.jar          # Executable boot JAR
│   └── aios-backend-0.0.1-SNAPSHOT-plain.jar    # Library JAR
└── reports/tests/                     # Test reports
```
