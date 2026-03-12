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

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo -e "${RED}❌ Java version is $JAVA_VERSION. Please use Java 21 or higher.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java 21+ found.${NC}"

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
    echo -e "${GREEN}✅ .env created. Please review it for any custom configurations.${NC}"
else
    echo -e "${GREEN}✅ .env file already exists.${NC}"
fi

# 3. Clean and Build
echo -e "\n${BOLD}🔨 Building services...${NC}"
./gradlew clean build -x test --no-daemon

# 4. Launch Infrastructure
echo -e "\n${BOLD}🐳 Launching infrastructure containers...${NC}"
docker-compose -f platform/compose/docker-compose.dev.yml up -d

# 5. Wait for infrastructure to be healthy (Simplified)
echo -e "\n${BOLD}⏳ Waiting for core infrastructure...${NC}"
sleep 10

# 6. Final Summary
echo -e "\n${BOLD}${GREEN}✨ Bootstrap Complete!${NC}"
echo -e "---------------------------------------------------"
echo -e "${BOLD}Next Steps:${NC}"
echo -e "1. Start core services:"
echo -e "   ${BLUE}./gradlew :services:gateway-bff:bootRun${NC} (Port 8080)"
echo -e "   ${BLUE}./gradlew :services:identity-service:bootRun${NC} (Port 8081)"
echo -e "   ${BLUE}./gradlew :services:planning-service:bootRun${NC} (Port 8083)"
echo -e "\n2. Access Documentation:"
echo -e "   ${BOLD}Swagger UI:${NC} http://localhost:8080/swagger-ui.html"
echo -e "   ${BOLD}API Versions:${NC} http://localhost:8080/api/versions"
echo -e "---------------------------------------------------"
