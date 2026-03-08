# resource-service

Architecture: `DDD + Clean`

Responsibilities:
- worklog entries
- bookmarks
- vault metadata

Port: `8087`

Tenant context:
- Required via `X-Tenant-ID` (or configured fallback resolvers)

Baseline implementation:
- Clean-style application service + domain model with validation rules
- Tenant-aware persistence tables: `resource_bookmark`, `resource_worklog_entry`, `resource_vault_item`
- API endpoints backed by JPA repositories instead of in-memory/stub responses
