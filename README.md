# Foculist Backend

Backend monorepo for the `foculist` frontend reference (`references/next-boilarplate`).

## Service split
- `gateway-bff` (`BFF + API Gateway`): single frontend backend entrypoint, tenant enforcement, and route aggregation.
- `identity-service` (`DDD + Clean`): users, auth, roles.
- `project-service` (`DDD + Hexagonal`): projects, project settings, team membership, integrations.
- `planning-service` (`DDD + Hexagonal`): sprints, tasks, workflow states, priority.
- `calendar-service` (`DDD + Hexagonal`): calendar events, agenda context.
- `meeting-service` (`DDD + Clean`): meeting transcripts, summaries, action extraction.
- `sync-service` (`DDD + Clean`): offline sync ingest, pull/push, backup/restore.
- `resource-service` (`DDD + Clean`): worklog, bookmarks, vault metadata.

## Runtime baseline
- Java 21, Spring Boot 3, Gradle.
- PostgreSQL as system of record.
- Optional MongoDB/DynamoDB adapters per service where read/write patterns need it.
- Redis cache.
- Kafka, RabbitMQ, SQS/SNS for async messaging (planning service includes transactional outbox publisher baseline).
- OTEL + Prometheus + Loki + Grafana.
- Kubernetes-first manifests.

## Multi-tenancy baseline
- First-class tenant context via shared module: `platform/tenancy-core`.
- Tenant resolution order: `X-Tenant-ID` header -> `tenant` query param -> `/v1/tenants/{tenant}/...` path -> subdomain -> JWT `tenant` claim.
- Tenant mismatch protection: request tenant must match JWT tenant claim when both are present.
- Tenant observability: tenant ID propagated to MDC (`tenantId`) and echoed in response header.
- Persistence rule: tenant-scoped tables include `tenant_id` with index and tenant-scoped uniqueness.
- Isolation strategy: hybrid-ready (pool by default, extensible to schema/DB-per-tenant for enterprise tenants).

## Repo map
- `services/` microservices.
- `services/gateway-bff` API gateway + frontend BFF composition layer.
- `platform/tenancy-core` shared multi-tenancy primitives for all services.
- `contracts/` OpenAPI contracts consumed by frontend/BFF.
- `platform/compose` local infra stack.
- `platform/otel` observability configs.
- `platform/k8s` Kubernetes manifests.
- `docs/` architecture decisions and frontend-backend mapping.
- `.env.example` baseline environment variables for local/dev scaffolding.

## Quick Start (Google-Grade Automation)
The entire platform is now containerized. You can go from a fresh clone to a full-stack live environment (Infra + Services + Observability) with a single command:

```bash
# 1. Launch everything (Infra + Gateway + Identity + Planning + Project)
make launch

# 2. Check status
make status
```

**Developer Workflow:**
- **Infrastructure Only**: `make infra-up`
- **Full Stack**: `make launch`
- **Hot Reloading**: For active development, we recommend running infrastructure via Docker and services via Gradle (`./gradlew bootRun`) to leverage Hot Swap.

Detailed Documentation:
- **Swagger UI (Aggregated)**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **API Version Discovery**: [http://localhost:8080/api/versions](http://localhost:8080/api/versions)
- **Monitoring (Grafana)**: [http://localhost:3000](http://localhost:3000)

or:

```bash
make infra-up
```

## Run one service
```bash
./gradlew :services:planning-service:bootRun
```

## Run gateway
```bash
./gradlew :services:gateway-bff:bootRun
```

## Common scaffold commands
- `make compile`
- `make test`
- `make infra-up`
- `make infra-down`
- `make k8s-local`
- `make k8s-dev`
- `make k8s-prod`

## Configuration
- Runtime settings are environment-driven with local defaults in each service `application.yml`.
- Baseline variable set: `.env.example`
- Reference: `docs/configuration.md`
- Kubernetes templates: `platform/k8s/base/runtime-configmap.yaml`, `platform/k8s/base/runtime-secret.example.yaml`
- Kubernetes overlays: `platform/k8s/overlays/local`, `platform/k8s/overlays/dev`, `platform/k8s/overlays/prod`
- Next frontend handoff checklist: `docs/next-boilerplate-integration-checklist.md`
