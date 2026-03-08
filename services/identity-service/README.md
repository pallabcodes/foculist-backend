# identity-service

Architecture: `DDD + Clean`

Responsibilities:
- credentials and oauth login flows
- registration and user profile lookup
- role claims for route protection

Port: `8081`

Tenant context:
- Required via `X-Tenant-ID` (or configured fallback resolvers)
