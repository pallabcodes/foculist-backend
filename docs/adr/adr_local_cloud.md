# ADR: Local-First Cloud Simulation Strategy

## Status
Accepted

## Context
As a commercialized SaaS product built by high-performing engineers, the application relies heavily on managed cloud services (AWS S3, Cognito, SQS, etc.).
Relying strictly on actual AWS infrastructure for local development introduces several critical blockers:
1. **Rate Limiting & Cost**: Hitting real AWS APIs from every developer machine quickly drains Free Tier quotas and incurs unnecessary costs.
2. **Offline Development**: Developers cannot work on airplanes or without stable internet.
3. **Speed & Latency**: Network hops to physical cloud infrastructure drastically slow down the TDD and frontend/backend integration feedback loops.
4. **LocalStack Weight**: While LocalStack provides API parity, running a heavy Dockerized python simulation of the entire AWS suite on every local machine drains laptop batteries and RAM.

## Decision
We will employ a "Google-grade" **Hexagonal Architecture Simulation** strategy.

### 1. Zero Direct Cloud SDK Coupling in Core Logic
The domain and application core layers are strictly forbidden from knowing about AWS SDKs.
Business logic must depend solely on abstract Java Ports (Interfaces) defined in the application layer.

### 2. The Service Selection Decision Tree
To ensure we maintain a realistic, commercial-grade product without incurring unnecessary costs, every new external service must be evaluated against this strict hierarchy:

1. **AWS Free Tier (Native)**: If the service is fully available within the AWS Free Tier, we use it natively. However, to prevent accidental loops from exhausting the quota, it *must* be wrapped in a Local Rate Limiter (e.g., max 5 requests/day).
2. **Local Docker/K8s Equivalents**: If the AWS service isn't free or has low limits, we hunt for an API-compatible Docker emulator (e.g., LocalStack for Cognito/SQS, MinIO for S3, Redpanda for Kafka). This allows us to use the actual AWS SDK locally without hitting the cloud.
3. **Cheap or Trial SaaS**: If no Docker equivalent exists, we look for an alternative managed service that possesses a generous free tier or costs less than $10/month.
4. **Modifiable Open Source**: If finding a managed service fails, we adopt an open-source tool and build on top of it to fit our use case.
5. **Paid AWS Services (Guarded)**: As an absolute last resort, if the feature matters but is completely unavailable for free, we will implement it using the actual paid AWS service. **Critical Rule**: All code interacting with this paid service MUST be gated behind an explicit authorization flag/comment outlining the exact cost (e.g., "$0.002 per request") so it never executes locally without developer consent.

### 3. Feature Parity Protocol
To ensure that a developer using the `local` profile does not experience a false sense of security that breaks in staging, all Adapters must be tested against the exact same **Contract Test Suite**.
If an S3 bucket throws a `NotAuthorized` Exception for a missing key instead of a `NotFound`, the alternative adapter must be hardcoded to throw the exact same exception.

## Consequences
*   **Pros**: Infinite local rate limits, zero-cost local dev, instant offline testing, instant switch to real AWS by altering a boolean environment variable.
*   **Cons**: Requires upfront engineering discipline to maintain the alternative Adapters and ensure they don't drift from actual AWS behavior via rigid Contract Tests.
