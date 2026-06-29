# ADR-001: Spring Boot as the Application Runtime

| Field | Value |
|-------|-------|
| **Status** | Accepted |
| **Date** | 2026-06-29 |
| **Deciders** | AIOS Core Team |

## Context

AIOS requires an enterprise-grade backend that supports REST APIs, security, persistence, caching, observability, and modular package boundaries. The platform must integrate cleanly with Spring AI, Flyway, PostgreSQL, and Redis while remaining maintainable by a small team.

## What Did We Choose?

**Spring Boot 3.5.15** on **Java 21**, using Gradle as the build tool, with a modular monolith structure under `com.aios.*` packages.

Key stack choices bundled with Spring Boot:

- Spring Web (REST)
- Spring Security (stateless auth)
- Spring Data JPA + Flyway
- Spring Data Redis
- Spring Boot Actuator
- Spring AI integration via BOM

## Why?

1. **Mature ecosystem** — First-class support for security, transactions, validation, and observability out of the box.
2. **Spring AI alignment** — Spring AI 1.1.x targets Spring Boot 3.5.x, reducing integration friction for `ChatModel`, `EmbeddingModel`, and `VectorStore`.
3. **Enterprise conventions** — `@Transactional` service layers, `@ConfigurationProperties`, actuator health checks, and profile-based configuration match our secure-by-design goals.
4. **Team velocity** — Auto-configuration, dependency injection, and widespread documentation lower onboarding cost.
5. **Deployment flexibility** — Executable JAR, Docker-friendly, and compatible with future Kubernetes deployment.

## Alternatives Considered

| Alternative | Reason Not Chosen |
|-------------|-------------------|
| **Quarkus** | Smaller Spring AI ecosystem; team familiarity with Spring is higher. |
| **Micronaut** | Excellent performance, but fewer Spring AI examples and less alignment with existing Spring Security patterns. |
| **Node.js / NestJS** | Weaker typing and JVM ecosystem fit for long-running enterprise services with JPA + Flyway. |
| **Plain Spring Framework (no Boot)** | More boilerplate for actuator, auto-config, and profile management. |
| **Microservices from day one** | Premature complexity; modular monolith satisfies Phase 1–2 scope. |

## Trade-offs

| Benefit | Cost |
|---------|------|
| Rapid feature development via auto-config | Heavier runtime than Quarkus/Micronaut |
| Consistent enterprise patterns | Spring Boot opinionation can hide complexity until misconfigured |
| Large community and hiring pool | Startup time and memory footprint higher than lightweight frameworks |
| Strong security primitives | Misconfigured defaults (e.g. actuator exposure) require discipline |
| Profile-based config (`local`, `dev`, `prod`) | Requires discipline to keep secrets out of committed YAML |

## Consequences

- All REST endpoints live under `/api/v1/`
- Schema managed exclusively via Flyway (`ddl-auto: validate`)
- Configuration split across `application.yml` + profile-specific files
- Build verification: `./gradlew build` is mandatory before merge

## References

- [docs/ARCHITECTURE.md](../ARCHITECTURE.md)
- [docs/CONTEXT.md](../CONTEXT.md)
- Spring Boot 3.5.15 release notes
