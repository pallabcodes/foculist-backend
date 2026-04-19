COMPOSE_PROJECT_NAME := foculist-main
export COMPOSE_PROJECT_NAME

.PHONY: bootstrap infra-up infra-down test compile run-gateway run-planning k8s-local k8s-dev k8s-prod

bootstrap:
	./scripts/ignite.sh

launch:
	./scripts/ignite.sh

ignite:
	./scripts/ignite.sh

doctor:
	./scripts/check-pre-reqs.sh

status:
	docker compose ps

seed:
	@chmod +x scripts/seed-data.sh
	./scripts/seed-data.sh

log:
	@if [ -z "$(service)" ]; then \
		echo "Usage: make log service=<service-name>"; \
		docker compose ps --services; \
	else \
		docker compose logs -f $(service); \
	fi

reset-db:
	@echo "⚠️ Warning: This will stop containers and delete all volumes (database data). Continue? [y/N]" && read ans && [ $${ans:-N} = y ]
	docker compose down -v
	./scripts/bootstrap.sh

k8s-up:
	@chmod +x scripts/local-k8s-up.sh
	./scripts/local-k8s-up.sh

k8s-down:
	kubectl delete -k platform/k8s/overlays/local

chaos:
	@chmod +x scripts/chaos-test.sh
	./scripts/chaos-test.sh

chaos-stop:
	docker stop pumba-chaos || true

infra-up:
	docker compose -f platform/compose/docker-compose.dev.yml up -d

infra-down:
	docker compose -f platform/compose/docker-compose.dev.yml down

test:
	./gradlew test --no-daemon

compile:
	./gradlew compileJava --no-daemon

run-gateway:
	./gradlew :services:gateway-bff:bootRun --no-daemon

run-planning:
	./gradlew :services:planning-service:bootRun --no-daemon

k8s-local:
	kubectl apply -k platform/k8s/overlays/local

k8s-dev:
	kubectl apply -k platform/k8s/overlays/dev

k8s-prod:
	kubectl apply -k platform/k8s/overlays/prod

# --- New Operation Tools ---
scan:
	@echo "🛡️ Scanning all services for vulnerabilities (Trivy)..."
	@trivy fs . --scanners misconfig,vuln --severity HIGH,CRITICAL --format cyclonedx --output sbom.json
	@echo "✅ Scan complete. SBOM generated at: sbom.json"

stress-test:
	@echo "🔥 Starting k6 API Stress Test (Sync Engine)..."
	@k6 run platform/load-test/k6-smoke-test.js

build-multi:
	@echo "🏗️ Building Multi-Arch Docker images (ARM64/AMD64)..."
	@docker buildx build --platform linux/amd64,linux/arm64 -t foculist/identity-service:latest ./services/identity-service --push
