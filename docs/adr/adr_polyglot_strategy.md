# ADR-006: Polyglot Strategy for High-Performance Service Evolution

## Status
Proposed (2026-02-20)

## Context
The `foculist` project is currently standardized on a Java 21/Spring Boot 3.2 backend. While this provides high developer velocity and a robust ecosystem, specific services face scaling and latency constraints that JVM-based solutions cannot optimally resolve at "Principle Architect" level (Google L6+ standards).

## Decision: Targeted Language Specialization

We define a "Right Tool for the Job" matrix based on the following three vectors: **Predictability**, **Throughput**, and **Operational Surface Area**.

### 1. Go: High-Concurrency Async Workers
*   **Target**: `automation-worker-go` (and future outbox handlers).
*   **Justification**: Goroutines provide the most efficient mapping for I/O-bound, high-concurrency tasks (CDC, Outbox handlers) without the memory overhead of JVM thread management.
*   **Reference**: Industry shift for "Infrastructure Glue" and short-lived workers where binary size and startup time are critical for autoscaling responsiveness.

### 2. Rust: Stateful Synchronization & Edge Compute
*   **Target**: `sync-service` refactor and WASM edge logic.
*   **Justification**: 
    - **Back-end**: Real-time sync requires complex data structure merging (CRDTs/OT). JVM's non-deterministic GC pauses (STW) can disrupt the sync-loop stability. Rust provides safe, deterministic memory management.
    - **Front-end**: Compiling sync logic to **WebAssembly (WASM)** allows for a "Local First" implementation in `next-boilarplate`, meeting the "Apple-Grade" experience requirement for zero-latency UI interactions.
*   **Reference**: [Distributed Systems Theory](file:///Users/picon/Learning/knowledge/references/system-design/networking/distributed-systems-theory.md) — Safety and performance in state-merging logic.

### 3. C++: Performance-Critical Infrastructure
*   **Target**: `gateway-bff` custom filters/extensions (L7 proxy).
*   **Justification**: The API Gateway is a "Centralized Tax." Every millisecond added here is multiplied by total request volume. While Spring Cloud Gateway is sufficient for early scale, moving critical routing/auth logic into a C++ Envoy Filter reduces P99.99 tail latency to microsecond levels.
*   **Reference**: [API Gateway Comprehensive Guide](file:///Users/picon/Learning/knowledge/references/system-design/infrastructure-techniques/api-gateway-comprehensive.md) — Envoy (C++) is the "Lowest Latency" baseline.

## Consequences
*   **Pros**: Significant reduction in cloud infrastructure costs (VRAM/CPU); removal of GC-related stutters in real-time features.
*   **Cons**: Increased pipeline complexity (multi-language CI/CD); higher bar for cross-functional engineering skills.

## Fitness Functions
1.  **Latency**: If `gateway-bff` P99.99 overhead exceeds 5ms, evaluate C++ migration.
2.  **Memory**: If `sync-service` Heap usage grows exponentially with concurrent users, evaluate Rust migration.
