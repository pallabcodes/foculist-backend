.PHONY: bootstrap infra-up infra-down test compile run-gateway run-planning k8s-local k8s-dev k8s-prod

bootstrap:
	./scripts/bootstrap.sh

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
