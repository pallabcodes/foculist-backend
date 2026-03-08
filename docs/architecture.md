# Foculist Backend Architecture

## Reference analyzed
Frontend source was read from `references/next-boilarplate` (spelling on disk is `next-boilarplate`).
No files in the frontend reference were modified.

## Architecture modes per service
- `BFF + Gateway`: `gateway-bff`
- `DDD + Hexagonal`: `project-service`, `planning-service`, `calendar-service`
- `DDD + Clean`: `identity-service`, `meeting-service`, `sync-service`, `resource-service`

## Bounded contexts
- Gateway/BFF: frontend entrypoint, route composition, tenant/auth propagation.
- Identity: authentication, authorization, user profile.
- Project: project metadata, team members, views/settings, integrations.
- Planning: sprints, tasks, workflow states, priority model.
- Calendar: events and agenda context consumed by AI meetings.
- Meeting: transcripts/summaries and AI-assisted task extraction.
- Sync: offline-first data push/pull, sync bookkeeping, import/export hooks.
- Resource: worklog, bookmarks, vault metadata.

## Data ownership
- Each service owns its schema in PostgreSQL.
- Cross-service data is exchanged via APIs/events, not shared mutable tables.
- Optional read-model projections can be built via outbox/CDC streams.

## Multi-tenancy architecture
- Shared tenancy module: `platform/tenancy-core`.
- Request tenant resolution order follows system-design guidance:
  - header (`X-Tenant-ID`)
  - query parameter (`tenant`)
  - path pattern (`/v1/tenants/{tenant}/...`)
  - subdomain
  - JWT claim (`tenant`)
- Safety controls:
  - fail fast when tenant is missing on protected routes
  - fail fast when header/path tenant and JWT tenant mismatch
  - clear tenant context after each request
- Logging/trace controls:
  - tenant ID is injected into MDC for log correlation
  - response includes resolved `X-Tenant-ID`

## Isolation model
- Default model is pooled storage with strict tenant discriminator (`tenant_id`) and tenant-aware indexes/unique constraints.
- Upgrade path supports schema-per-tenant or database-per-tenant for premium/compliance tenants without API contract changes.
- Optional Postgres RLS remains off by default and can be enabled only when compliance requires DB-level enforcement.

## Async/eventing baseline
- Kafka for event streaming and analytics fan-out.
- RabbitMQ for command/work queues where ordered workflows are needed.
- SQS/SNS adapters for AWS-native async integrations.
- Outbox table pattern enabled per service as needed, with planning-service as the first production baseline (DB outbox + scheduled Kafka publisher).

## Planning service baseline implementation
- Domain core: task/sprint entities with workflow and priority validation.
- Ports/adapters: repository interfaces in domain layer, JPA adapters in infrastructure layer.
- Persistence: tenant-aware `planning_sprint` and `planning_task` tables with scoped indexes.
- Eventing: task creation writes an outbox event in the same DB transaction.

## Project service baseline implementation
- Domain core: project aggregate + project settings aggregate with enum and workflow validation.
- Ports/adapters: repository interfaces in domain layer, JPA adapters in infrastructure layer.
- Persistence: tenant-aware `project_item`, `project_settings`, `project_workflow_status` tables.
- API behavior: `/v1/projects` and `/v1/projects/{id}/settings` now execute real tenant-scoped writes/reads.

## Resource service baseline implementation
- Domain core: bookmark, worklog entry, and vault item aggregates with validation on URL, duration, and classification.
- Clean layering: application service orchestration with repository ports and JPA adapters.
- Persistence: tenant-aware `resource_bookmark`, `resource_worklog_entry`, `resource_vault_item` tables.
- API behavior: `/v1/bookmarks`, `/v1/worklog/entries`, `/v1/vault/items` now execute real tenant-scoped persistence flows.

## Calendar service baseline implementation
- Domain core: calendar event and agenda-context aggregates with strict date/time validation.
- Ports/adapters: repository interfaces in domain layer, JPA adapters in infrastructure layer.
- Persistence: tenant-aware `calendar_event` and `calendar_agenda_context` tables.
- API behavior: `/v1/calendar/events` and `/v1/calendar/agenda-context` now execute real tenant-scoped reads/writes.

## Meeting service baseline implementation
- Domain core: meeting summary aggregate with explicit style/content validation.
- Clean layering: application service orchestration with summary repository port and JPA adapter.
- Persistence: tenant-aware `meeting_summary` table.
- API behavior: `/v1/meetings/summaries` is persistence-backed and `/v1/meetings/extract-tasks` returns deterministic extraction output from transcript signals.

## Sync service baseline implementation
- Domain core: sync push envelope, change event ledger, and per-device cursor aggregates with validation on payload, pending counts, and cursors.
- Clean layering: application orchestration with repository ports for envelopes, events, and cursors backed by JPA adapters.
- Persistence: tenant-aware `sync_push_envelope`, `sync_change_event`, and `sync_device_cursor` tables with tenant-scoped indexes.
- API behavior: `/v1/sync/push` durably stores inbound envelopes and emits change ledger entries, while `/v1/sync/pull` returns tenant-scoped deltas since client cursor.

## Gateway/BFF baseline implementation
- Route prefixes under `/api/*` map to domain services.
- Tenant resolution/enforcement at gateway edge with JWT/header mismatch checks.
- Aggregated endpoint `GET /bff/dashboard` composes project, planning, and resource views.

## Observability baseline
- OpenTelemetry instrumentation from each service.
- Metrics: Prometheus scrape of actuator endpoints.
- Logs: Loki via OTEL collector.
- Dashboards: Grafana.

## Deployment baseline
- Kubernetes-first manifests in `platform/k8s`.
- Kustomize overlays for `local`, `dev`, and `prod` in `platform/k8s/overlays`.
- Local platform stack in `platform/compose/docker-compose.dev.yml`.
- Environment-first runtime config (`${VAR:default}`) for promotion across local/dev/prod.
