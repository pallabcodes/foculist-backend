# tenancy-core

Shared tenancy context, resolver, and request filter for all foculist services.

Features:
- deterministic tenant resolution order
- tenant propagation to MDC (`tenantId`)
- mismatch protection between request tenant and JWT tenant claim
- fail-fast handling for missing tenant context on protected endpoints
