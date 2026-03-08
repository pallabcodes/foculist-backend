# Configuration Baseline

## Principle
All services read infrastructure endpoints and credentials from environment variables with local defaults.

## Shared variables
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `MONGODB_URI`
- `REDIS_HOST`
- `REDIS_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`
- `RABBITMQ_HOST`
- `RABBITMQ_PORT`
- `AWS_REGION`
- `AWS_ENDPOINT`
- `JWT_SECRET`
- `JWT_ACCESS_EXPIRATION`
- `JWT_REFRESH_EXPIRATION`
- `OAUTH2_SUCCESS_REDIRECT_URL`

## Gateway variables
- `IDENTITY_BASE_URL`
- `PROJECT_BASE_URL`
- `PLANNING_BASE_URL`
- `CALENDAR_BASE_URL`
- `MEETING_BASE_URL`
- `SYNC_BASE_URL`
- `RESOURCE_BASE_URL`
- `CORS_ALLOWED_ORIGINS`
- `CORS_ALLOWED_METHODS`
- `CORS_ALLOWED_HEADERS`
- `CORS_ALLOW_CREDENTIALS`

## Local setup
Use `.env.example` as the baseline values for local execution.

## Kubernetes setup
- Keep sensitive variables in `Secret`.
- Keep endpoint/runtime variables in `ConfigMap`.
- Keep gateway as the only public ingress target.
- Templates:
  - `platform/k8s/base/runtime-configmap.yaml`
  - `platform/k8s/base/runtime-secret.example.yaml`
- Kustomize overlays:
  - `platform/k8s/overlays/local`
  - `platform/k8s/overlays/dev`
  - `platform/k8s/overlays/prod`
