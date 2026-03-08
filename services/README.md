# Services

Java services in this repo are intentionally split across two architecture styles:

- Gateway/BFF: `gateway-bff`
- Hexagonal: `project-service`, `planning-service`, `calendar-service`
- Clean: `identity-service`, `meeting-service`, `sync-service`, `resource-service`

Optional hybrid worker:
- Go: `automation-worker-go`
