#!/bin/bash
set -e

# Foculist Platform Bootstrap Script
# This script prepares the environment, builds the services, and launches the infrastructure.

BOLD='\033[1m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BOLD}${BLUE}🚀 Starting Foculist Platform Bootstrap...${NC}"

# 1. Check Dependencies
echo -e "\n${BOLD}🔍 Checking prerequisites...${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed. Please install Java 21.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java found.${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker found.${NC}"

# 2. Setup Environment Variables
echo -e "\n${BOLD}⚙️ Setting up environment variables...${NC}"
if [ ! -f .env ]; then
    echo -e "${BLUE}📝 .env file not found. Copying .env.example...${NC}"
    cp .env.example .env
    echo -e "${GREEN}✅ .env created.${NC}"
fi

# 3. Fast Build (Host-based Gradle cache)
echo -e "\n${BOLD}🔨 Building JARs (Fast Mode)...${NC}"
./gradlew build -x test --no-daemon

# 4. Build Docker Images (Local context)
echo -e "\n${BOLD}🐳 Building Docker Images...${NC}"
docker compose build --parallel

# 5. Launch Stack
echo -e "\n${BOLD}🚢 Launching full platform stack...${NC}"
docker compose up -d

# 6. Final Summary
echo -e "\n${BOLD}${GREEN}✨ Platform is LIVE!${NC}"
echo -e "---------------------------------------------------"
echo -e "${BOLD}Access Points:${NC}"
echo -e "  - ${BOLD}Gateway (Entrypoint):${NC} http://localhost:8080"
echo -e "  - ${BOLD}Swagger UI:${NC} http://localhost:8080/swagger-ui.html"
echo -e "  - ${BOLD}API Versions:${NC} http://localhost:8080/api/versions"
echo -e "\n${BOLD}Observability:${NC}"
echo -e "  - ${BOLD}Grafana:${NC} http://localhost:3000"
echo -e "  - ${BOLD}Jaeger:${NC} http://localhost:16686"
echo -e "---------------------------------------------------"
echo -e "${BLUE}Tip: Use 'docker compose logs -f' to watch the logs.${NC}"
