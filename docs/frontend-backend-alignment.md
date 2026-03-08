# Frontend to Backend Alignment

This mapping was derived from routes and local-storage usage in `references/next-boilarplate`.

## Route-domain mapping
- Frontend entrypoint: `gateway-bff` on `/api/*` and `/bff/*`.
- `/login`, `/signup`, `/register`, `/verify`, `/forgot-password`, `/reset-password` -> `identity-service`
- `/projects`, `/projects/[id]`, `/projects/[id]/settings/*` -> `project-service`
- `/sprints`, `/sprints/[id]`, task creation/edit flows -> `planning-service`
- `/calendar` and agenda handoff to AI meetings -> `calendar-service`
- `/ai-meetings` summary/transcript/task-extract flows -> `meeting-service`
- `/sync` data push/pull/import/export -> `sync-service`
- `/worklog`, `/bookmarks`, `/vault` -> `resource-service`

## Gateway route map
- `/api/user` -> `identity-service` compatibility endpoint (`/v1/user`)
- `/api/auth/*` and `/api/users/*` -> `identity-service`
- `/api/projects/*` -> `project-service`
- `/api/planning/*` -> `planning-service`
- `/api/calendar/*` -> `calendar-service`
- `/api/meetings/*` -> `meeting-service`
- `/api/sync/*` -> `sync-service`
- `/api/resources/*` -> `resource-service`
- `/bff/dashboard` -> gateway-composed response from project + planning + resource services

## Integration checklist
- See `docs/next-boilerplate-integration-checklist.md` for env setup, route usage, and mock-route collision handling.

## Existing local-first model to backend model
Local keys currently used in frontend:
- `foculist_data`
- `foculist_sync_status`
- project and portfolio settings keys

Backend migration direction:
1. Keep local-first UX in frontend.
2. Replace local writes with `sync-service` push envelope.
3. Route domain payloads to owning services.
4. Return merged server changes via `sync-service` pull.

## Tenant mapping for frontend
- Every backend call must include `X-Tenant-ID`.
- If frontend is tenant-subdomain based, backend supports subdomain resolution as fallback.
- If frontend moves to path tenancy, `/v1/tenants/{tenantId}/...` is supported by resolver.
- Auth token claim `tenant` must match request tenant when both are present.

## Primary contracts
- Gateway/BFF: `contracts/gateway-bff.openapi.yaml`
- Identity: `contracts/identity-service.openapi.yaml`
- Project: `contracts/project-service.openapi.yaml`
- Planning: `contracts/planning-service.openapi.yaml`
- Calendar: `contracts/calendar-service.openapi.yaml`
- Meeting: `contracts/meeting-service.openapi.yaml`
- Sync: `contracts/sync-service.openapi.yaml`
- Resource: `contracts/resource-service.openapi.yaml`
