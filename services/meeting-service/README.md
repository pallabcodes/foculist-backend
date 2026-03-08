# meeting-service

Architecture: `DDD + Clean`

Responsibilities:
- meeting summaries/transcripts
- AI-assisted action extraction to tasks

Port: `8085`

Tenant context:
- Required via `X-Tenant-ID` (or configured fallback resolvers)

Baseline implementation:
- Clean-style application service + domain validation for summary style/content
- Tenant-aware persistence table: `meeting_summary`
- Deterministic transcript task extraction baseline with priority inference
