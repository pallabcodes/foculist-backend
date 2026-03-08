# Decisions

## D-001 Build system
Use Gradle with Java 21 across all Java services.
Rationale: in a multi-service monorepo, Gradle gives faster incremental builds, flexible per-module dependency graphs, and easier composition for shared platform modules.

## D-002 Architecture split
Two patterns are supported simultaneously:
- DDD+Hexa for workflow-heavy domains.
- DDD+Clean for identity, orchestration, and supporting services.

## D-003 Persistence
PostgreSQL default with optional MongoDB/DynamoDB adapters for specific workloads.

## D-004 Hybrid option
`services/automation-worker-go` is included as an optional Go worker for high-concurrency background jobs.

## D-005 Multi-tenancy
Multi-tenancy is mandatory and enforced centrally through `platform:tenancy-core`.

## D-006 Tenant isolation strategy
Default is pooled storage (`tenant_id` discriminator + tenant-scoped indexes/uniques), with planned elevation to schema/database-per-tenant for enterprise tenants.

## D-007 Request-time tenant validation
Tenant is resolved in a fixed precedence order and validated against JWT tenant claim to prevent cross-tenant request spoofing.

## D-008 Planning domain hardening (Step 1)
We implemented real tenant-aware planning domain entities and repository ports/adapters.
Why: stub endpoints cannot enforce workflow invariants, tenant isolation, or durable writes.
Outcome: planning writes now execute through explicit domain rules and persistence boundaries.

## D-009 Outbox + Kafka baseline (Step 2)
We implemented a transactional outbox in planning service with a scheduled Kafka publisher.
Why: direct dual writes (DB + Kafka in request path) are not reliable under partial failures.
Outcome: domain writes and event intent are persisted atomically, then delivered asynchronously with retry and status tracking.

## D-010 Gateway/BFF entrypoint (Step 3)
We added `gateway-bff` as the single frontend backend entrypoint with tenant enforcement and downstream routing/composition.
Why: frontend should not own cross-service orchestration, tenant propagation, or per-service topology concerns.
Outcome: centralized tenant/auth propagation, stable frontend-facing API prefixes, and one aggregated dashboard endpoint.

## D-011 Config externalization baseline
Runtime endpoints and credentials are externalized via environment variables with local defaults.
Why: hardcoded localhost endpoints break Kubernetes and make environment promotion unsafe.
Outcome: the same artifact can run in local/dev/prod with only env-level overrides.

## D-012 Gateway exposure baseline
Gateway is the only external ingress target (`platform/k8s/base/gateway-bff-ingress.yaml`).
Why: internet exposure should be centralized at the BFF boundary for policy and observability control.
Outcome: internal services stay private (`ClusterIP`) and external traffic enters through one managed edge.

## D-013 Kustomize environment overlays
Kubernetes scaffolding is split into `local`, `dev`, and `prod` overlays with environment-specific runtime ConfigMap patches and ingress hosts.
Why: setup/scaffold must support repeatable promotion without editing shared base manifests.
Outcome: one base + overlay patch model for hostnames, runtime endpoints, and replica sizing.

## D-014 Next-boilerplate compatibility and CORS baseline
Gateway includes compatibility routing for `/api/user` and centralized CORS policy configuration.
Why: frontend reference currently mixes relative `/api/*` usage and env-based API base calls; compatibility and CORS reduce migration friction.
Outcome: backend can serve both canonical gateway routes and compatibility user route while keeping browser cross-origin behavior explicit by environment.

## D-015 JWT validation and tenant-claim hardening
JWT tenant extraction now verifies token signature/expiration at gateway and service tenancy filters; invalid bearer tokens are rejected with `401`.
Why: decoding claims without signature verification allows spoofed tenant claims and weakens isolation controls.
Outcome: tenant mismatch checks run only on validated JWTs, issued auth tokens include `tenant` claim, and identity auth responses no longer rely on stub refresh tokens or fallback demo users.

## D-016 Project service domain hardening
We replaced `project-service` stub controller responses with a real tenant-aware DDD+Hexagonal implementation.
Why: placeholder payloads do not enforce project invariants, cannot support reliable tenant-scoped writes, and break contract-driven frontend integration.
Outcome: `/v1/projects` and `/v1/projects/{id}/settings` now use domain/application/service boundaries, JPA repository adapters, and durable schema-backed persistence with tenant-safe constraints.

## D-017 Resource service domain hardening
We replaced `resource-service` stub responses with a real tenant-aware DDD+Clean baseline for bookmarks, worklog entries, and vault metadata.
Why: static payloads cannot support frontend persistence flows or enforce resource-specific validations (URL, duration, classification).
Outcome: `/v1/bookmarks`, `/v1/worklog/entries`, and `/v1/vault/items` are now backed by validated domain objects, repository ports/adapters, and migration-managed tenant-scoped tables.

## D-018 Calendar service domain hardening
We replaced `calendar-service` stub responses with a real tenant-aware DDD+Hexagonal baseline for calendar events and meeting agenda context.
Why: static calendar payloads cannot support durable scheduling flows or enforce date/time integrity.
Outcome: `/v1/calendar/events` and `/v1/calendar/agenda-context` now execute through domain/application/repository boundaries with migration-managed tenant-scoped tables and agenda upsert semantics by `tenant + meetingId`.

## D-019 Meeting service domain hardening
We replaced `meeting-service` stub responses with a real tenant-aware DDD+Clean baseline for meeting summaries and transcript task extraction.
Why: static summary/task payloads cannot support reliable meeting history or predictable backend extraction behavior.
Outcome: `/v1/meetings/summaries` now persists validated summary aggregates in `meeting_summary`, and `/v1/meetings/extract-tasks` provides deterministic extraction with priority inference for frontend consumption.

## D-020 Sync service domain hardening
We replaced `sync-service` stub responses with a real tenant-aware DDD+Clean baseline for push envelopes, change event ledgers, and device sync cursors.
Why: placeholder push/pull payloads cannot support durable offline sync handoff, deterministic cursoring, or tenant-scoped delta retrieval.
Outcome: `/v1/sync/push` and `/v1/sync/pull` now execute through application/domain/repository boundaries with migration-managed tables and validation of ISO-8601 sync cursors.
