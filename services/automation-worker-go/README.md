# automation-worker-go

Optional Go worker for Java+Go deployments.

Use this for high-concurrency async workloads:
- meeting transcript post-processing
- bulk priority recomputation
- retry workers for outbox/CDC event handlers

Run locally:
```bash
go run ./cmd/worker
```

Tenant context:
- Required via `X-Tenant-ID` (or configured fallback resolvers)
