# 🚀 Foculist Platform (Sovereign Edition)

**"Sovereign First. Privacy by Design. Industrial High-Fidelity."**

Foculist is a next-generation focus and task management platform built for speed, offline-first reliability, and total data sovereignty. This repository contains the reference Backend Microservice Mesh (11+ services) and the industrial Local Development Stack.

---

## ⚡ Quick Start: 1-Command Ignition

Go from a fresh clone to a fully functional, seeded, and observable environment in **3 minutes**:

```bash
# 1. Ignite the platform (Checks Pre-reqs -> Launches Infra -> Provisions Clouds -> Seeds Data)
make ignite

# 2. Check health
make status
```

> [!TIP]
> Use `make doctor` to verify your local system's health (JDK 21, Docker, RAM, optional tools).

---

## 🏛️ Architecture & Service Mesh

The platform is designed with **Domain Driven Design (DDD)** and **Hexagonal Architecture**. Every service is tenant-aware by default.

| Service | Port | Responsibility | Tech Stack |
| :--- | :---: | :--- | :--- |
| **Gateway-BFF** | `8080` | Entrypoint, JWT Auth, Route Aggregated Swagger | Spring Gateway |
| **Identity-Service** | `8081` | Users, Roles, MFA, Onboarding | Keycloak + Postgres |
| **Planning-Service** | `8082` | Tasks, Sprints, Epics, Outbox Events | Postgres + RabbitMQ |
| **Sync-Service** | `8083` | LWW-Merge Sync, Binary Storage, Backup | Redis + S3 + Mongo |
| **Project-Service** | `8084` | Workspace Settings, Team Members | Postgres |
| **AI-Worker (Python)** | `-` | Asynchronous Focus Enrichment | LangChain + Ollama |

---

## 🛠️ Sovereign Ops Dashboard

This platform is 100% self-hosted and offline-capable. Use these local endpoints for development:

| Tool | Local URL | Purpose |
| :--- | :--- | :--- |
| **Swagger UI** | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | API Discovery (All Services) |
| **Grafana** | [http://localhost:3000](http://localhost:3000) | Observability (Metrics/Logs/Traces) |
| **Loki** | [http://localhost:3100](http://localhost:3100) | Log Aggregation Engine |
| **Spring Admin** | [http://localhost:8888](http://localhost:8888) | Service Health & Thread Dumps |
| **Feature Flags** | [http://localhost:4242](http://localhost:4242) | Unleash Toggle Management |
| **Redis Insight** | [http://localhost:8001](http://localhost:8001) | Cache & Sync Engine Visualization |
| **Local SMTP** | [http://localhost:8025](http://localhost:8025) | Mailpit (Onboarding/MFA Emails) |
| **LocalStack** | [http://localhost:4566](http://localhost:4566) | S3, SNS, SQS, Secrets Manager |
| **Jaeger** | [http://localhost:16686](http://localhost:16686) | Distributed Tracing UI |

---

## 🔌 API Testing & Verification

Foculist provides a world-class suite for API verification, designed for zero-bafflement onboarding.

### 🛡️ Postman Collection
A pre-configured **Postman Collection** is available in the root directory.
- **File**: `foculist-api.json`
- **Features**: 
  - Automated JWT extraction and environment variable updates.
  - Pre-configured requests for Identity, Onboarding, Planning, and Feature Flags.
  - Logic for multi-tenancy header injection.

### 🤖 Automated Verification Script (Headless)
Run the headless verification suite to confirm the integrity of the entire microservice mesh.
```bash
chmod +x scripts/verify-api.sh
./scripts/verify-api.sh
```
This script validates:
- [x] **Connectivity**: All 11 services are responding.
- [x] **Identity**: Registration and JWT Login flows.
- [x] **Tenancy**: Onboarding workspace initialization.
- [x] **Persistence**: Functional task creation and retrieval.
- [x] **Toggles**: Active feature flag gating.

---

## 🔒 Security & Compliance
- **Data Isolation**: Multi-tenancy enforced at the persistence layer via `platform/tenancy-core`.
- **Hardened Containers**: Images use `distroless` for minimal attack surface.
- **Privacy**: Automated PII masking in logs and WAF filters in the Gateway.
- **Sovereign AI**: All LLM processing is proxied via a local Ollama instance (no cloud API leakage).

---

## 🧪 Developer Commands
| Command | Action |
| :--- | :--- |
| `make ignite` | Full platform start + initialization. |
| `make seed` | Re-seed the platform with admin data. |
| `make scan` | High-fidelity security scan (Trivy + SBOM). |
| `make stress-test` | Run API performance benchmark (k6). |
| `make chaos` | Inject network latency via Pumba. |
| `make reset-db` | **CAUTION**: Wipe all data and restart fresh. |

---

## 📂 Repository Structure
- `platform/compose/`: Core Docker infrastructure.
- `platform/tenancy-core/`: Shared primitives for 11 microservices.
- `localstack-init/`: Automated AWS resource provisioning.
- `scripts/`: Platform orchestration and seeding utilities.
- `docs/`: Architecture decisions (ADRs) and handoff checklists.

---

© 2026 Foculist Open Source. Built for the **Sovereign Developer**.
