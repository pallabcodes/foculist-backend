# planning-service

Architecture: `DDD + Hexagonal`

Responsibilities:
- sprint lifecycle
- task backlog and board state transitions
- workflow statuses and priority logic
- transactional outbox for task-created events

Port: `8083`

Tenant context:
- Required via `X-Tenant-ID` (or configured fallback resolvers)

Baseline implementation:
- Hexagonal ports/adapters for sprint/task repositories
- JPA persistence with tenant-aware schema (`planning_sprint`, `planning_task`)
- Outbox table + Kafka publisher (`planning_outbox_event`)
