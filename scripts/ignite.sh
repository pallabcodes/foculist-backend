#!/bin/bash
set -e

BOLD='\033[1m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BOLD}${BLUE}🚀 FOCULIST PLATFORM IGNITION${NC}"

# 1. Pre-flight Checks
chmod +x scripts/*.sh
./scripts/check-pre-reqs.sh

# 2. Infra Launch
echo -e "\n${BOLD}🏗️ Launching Infrastructure (Postgres, LocalStack, Redis, RabbitMQ)...${NC}"
docker compose -f platform/compose/docker-compose.dev.yml up -d

# 3. Readiness Barrier (Wait for Postgres)
echo -ne "\n${BOLD}⏳ Waiting for Database Readiness...${NC}"
until docker exec platform-compose-postgres-1 pg_isready -U foculist &> /dev/null; do
    echo -n "."
    sleep 2
done
echo -e " ${GREEN}✅ Ready!${NC}"

# 4. Readiness Barrier (Wait for LocalStack)
echo -ne "${BOLD}⏳ Waiting for LocalStack Readiness...${NC}"
until curl -s http://localhost:4566/_localstack/health | grep -q "\"s3\": \"running\""; do
    echo -n "."
    sleep 2
done
echo -e " ${GREEN}✅ Ready!${NC}"

# 5. Initialization
echo -e "\n${BOLD}📦 Provisioning Cloud Infrastructure (LocalStack)...${NC}"
./localstack-init/01-infra.sh

echo -e "\n${BOLD}🌱 Seeding Database with Admin Data...${NC}"
# Note: In a real ignition, we might need to build and start the Gateway/Identity first
# But for now, we assume the dev might run them via IDE/Gradle
# We'll try to seed if the Gateway is up, otherwise warn.
if curl -s http://localhost:8080/health &> /dev/null; then
    ./scripts/seed-data.sh
else
    echo -e "${YELLOW}⚠️ Gateway not detected on port 8080. Skipping automated seed.${NC}"
    echo -e "${YELLOW}👉 Run the Gateway/Identity and then 'make seed' manually.${NC}"
fi

# 6. Final Dashboard
echo -e "\n${BOLD}${GREEN}✨ PLATFORM IGNITED!${NC}"
echo -e "------------------------------------------------------------"
echo -e "${BOLD}Sovereign Entry Points:${NC}"
echo -e "  - ${BOLD}API Gateway:${NC}    http://localhost:8080"
echo -e "  - ${BOLD}Swagger UI:${NC}    http://localhost:8080/swagger-ui.html"
echo -e "  - ${BOLD}Feature Flags:${NC} http://localhost:4242 (Unleash)"
echo -e "  - ${BOLD}Local SMTP:${NC}    http://localhost:8025 (Mailpit)"
echo -e "\n${BOLD}Observability Dashboard:${NC}"
echo -e "  - ${BOLD}Grafana:${NC}        http://localhost:3000"
echo -e "  - ${BOLD}Spring Admin:${NC}   http://localhost:8888"
echo -e "  - ${BOLD}Redis Insight:${NC}  http://localhost:8001"
echo -e "------------------------------------------------------------"
echo -e "${BLUE}💡 Tip: Use 'make doctor' to re-verify your system health.${NC}"
