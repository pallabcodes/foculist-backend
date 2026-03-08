# Edge Functions

Lightweight edge handlers keep the JVM services focused on the core domain.

Functions included:
- `image-resize`: image normalization and thumbnail metadata.
- `pdf-report`: project snapshot to PDF job envelope.
- `ai-summarize`: Vertex-style summarization request envelope.

Each function exposes a simple `handler(request)` signature so the code can be deployed to a serverless edge runtime or wrapped by a local adapter.
