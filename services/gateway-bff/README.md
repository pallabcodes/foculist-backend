# gateway-bff

Architecture: `BFF + API Gateway`

Responsibilities:
- Single backend entrypoint for frontend traffic
- Tenant enforcement and propagation (`X-Tenant-ID`)
- Route mapping to domain microservices
- Aggregated read endpoints for frontend composition

Port: `8080`
