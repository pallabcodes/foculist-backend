# Multi-tenancy Blueprint

This blueprint is derived from:
- `references/system-design/distributive-backend/multi-tenancy/*`
- `references/microservices/Multitenancy/*`

## Mandatory controls
- Every request must resolve a tenant context.
- Tenant context must be cleared after request completion.
- Tenant identifier must be present in logs/traces.
- Tenant mismatch between request source and JWT claim is denied.
- Tenant-scoped data tables must include `tenant_id` with supporting indexes and constraints.

## Resolution order
1. `X-Tenant-ID` header
2. `tenant` query parameter
3. Path token `/v1/tenants/{tenantId}/...`
4. Subdomain token
5. JWT claim `tenant`

## Isolation strategy
- Baseline: pooled data model with strict tenant discriminator.
- Enterprise extension: schema-per-tenant or database-per-tenant.
- Compliance extension: optional Postgres RLS per table.

## Operational safeguards
- Cache keys must include tenant.
- Async messages must carry tenant metadata.
- Object storage keys should be prefixed by tenant.
- Metrics dimensions should avoid high-cardinality tenant tags; traces/logs can carry tenant.

## Current implementation in this repo
- Shared module: `platform/tenancy-core`
- Auto-configured request filter resolves and validates tenant context.
- Gateway edge filter (`services/gateway-bff`) resolves and validates tenant before routing to services.
- JWT tenant claim extraction validates signature/expiration before claim use.
- Service schemas now include `tenant_id` in baseline migration.
- OpenAPI contracts require `X-Tenant-ID` for service endpoints.
