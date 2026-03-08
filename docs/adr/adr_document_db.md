# ADR-007: Document Database Adoption Strategy

## Context
The `foculist` project currently relies on PostgreSQL for its core identity and multi-tenancy logic. As we expand the architecture into microservices handling unpredictable workloads, we must evaluate where a Document Database (like MongoDB or Firestore) provides a structural advantage over a Relational Database (PostgreSQL).

Based on the **[Polyglot Persistence Guide](file:///Users/picon/Learning/knowledge/references/system-design/distributive-backend/database/polyglot-persistance/polyglot-persistence-guide.md)**, we must balance feature benefits against operational overhead (the "Schema Tax" and "Operational Cost > Feature Benefit" rules).

## Decision: Strategic NoSQL Adoption

We will introduce MongoDB (or a similar document store) **strictly** for services where the data access pattern demands a *schema-on-read* approach or heavy hierarchical nesting that would otherwise require expensive SQL joins.

### 1. The `canvas-service` (Rich Text / Block Editor)
*   **Why NoSQL?**: Foculist acts as an authoring tool (like Notion). Users create documents composed of arbitrary "blocks" (text, images, code, embedded widgets).
*   **The Conflict**: Modeling limitless recursive blocks in an RDBMS requires complex adjacency lists or Closure Tables. This means recursive CTEs for every read, degrading read performance.
*   **The Solution**: A Document DB stores the entire nested tree structure of a Canvas as a single JSON/BSON document. Fetching a document is an O(1) key lookup. The flexible schema absorbs new block types without costly database migrations.

### 2. The `activity-feed-service` (Event Sourcing / Timeline)
*   **Why NoSQL?**: This service captures user interactions (comments, edits, status changes) for audit trails and notification timelines.
*   **The Conflict**: Event payloads vary wildly (a comment event has text; a status change event has old/new status codes). An RDBMS would either require a massive sparse table (many NULL columns) or an arbitrary JSONB column (sacrificing some relational benefits anyway).
*   **The Solution**: A Document DB natively handles varied structures within the same collection. It also excels at append-heavy workloads and time-series aggregations (via aggregation pipelines).

### 3. The `integration-service` (Third-Party Webhooks)
*   **Why NoSQL?**: This service ingests data from external APIS (GitHub, Slack, Jira) to sync external task metadata into Foculist.
*   **The Conflict**: We do not control the schema of external webhooks. If Jira adds a custom field, our RDBMS schema breaks or drops the data.
*   **The Solution**: A Document DB serves as a "Schemaless Data Lake" for raw ingestion. It stores the exact JSON payload. A downstream worker (like our `automation-worker-go`) can then parse it and insert structured summaries into PostgreSQL.

## Where PostgreSQL Remains the Standard
*   **`identity-service`**: Auth, Tenancy, and Billing require strict ACID transactions and relational integrity (users belong to organizations, invoices belong to tiers).
*   **`planning-service`**: Tasks, epics, and sprints require complex relational reporting, constraints (start date < end date), and foreign keys to ensure data consistency.

## Consequences
*   **Pros**: Significant reduction in development velocity friction for variable-schema domains like the Canvas editor. Optimizes read-latency for deep hierarchies.
*   **Cons**: Increases infra maintenance (+1 database to backup/monitor). Requires developers to carefully manage consistency across the PostgreSQL/MongoDB boundary using Eventual Consistency patterns (e.g., Outbox Pattern).
