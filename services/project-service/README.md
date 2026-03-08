# project-service

Architecture: `DDD + Hexagonal`

Responsibilities:
- project lifecycle and metadata
- project settings (views/workflow/team)
- integration connections metadata

Port: `8082`

Tenant context:
- Required via `X-Tenant-ID` (or configured fallback resolvers)

Baseline implementation:
- Hexagonal ports/adapters for `Project` and `ProjectSettings` repositories
- Tenant-aware persistence tables: `project_item`, `project_settings`, `project_workflow_status`
- Domain validations for status/priority/default-view enums and workflow status normalization
