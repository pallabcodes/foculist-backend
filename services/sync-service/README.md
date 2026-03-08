# sync-service

Architecture: `DDD + Clean`

Responsibilities:
- Offline-first push/pull contract for tenant-scoped sync flows.
- Change-event ledger and per-device cursor bookkeeping.
- Import/export hook surface for future migration workloads.

Port: `8086`

Tenant context:
- Required via `X-Tenant-ID` (or configured fallback resolvers).

Primary endpoints:
- `POST /v1/sync/push`
- `POST /v1/sync/pull`

Persistence tables:
- `sync_push_envelope`
- `sync_change_event`
- `sync_device_cursor`
