#!/bin/bash
set -e

BOLD='\033[1m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
NC='\033[0m'

echo -e "${BOLD}🔍 Running Foculist System Health Check (Doctor)...${NC}"

# 1. Java Check
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java 21 NOT found. Please install it to continue.${NC}"
    exit 1
fi
JAVA_VER=$(java -version 2>&1 | head -n 1)
echo -e "${GREEN}✅ Java found: $JAVA_VER${NC}"

# 2. Docker Check
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker NOT found. Please install Docker Desktop or Engine.${NC}"
    exit 1
fi
if ! docker info &> /dev/null; then
    echo -e "${RED}❌ Docker is not running. Please start Docker.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker is running.${NC}"

# 3. Memory Check (Recommended 8GB+ for the full stack)
if [[ "$OSTYPE" == "darwin"* ]]; then
    TOTAL_MEM=$(sysctl hw.memsize | awk '{print $2/1024/1024/1024}')
    if (( $(echo "$TOTAL_MEM < 8" | bc -l) )); then
        echo -e "${YELLOW}⚠️ Warning: You only have ${TOTAL_MEM}GB RAM. 16GB is recommended for the full 15+ service stack.${NC}"
    fi
fi

# 4. Optional Tools (Trivy, k6)
if ! command -v trivy &> /dev/null; then
    echo -e "${YELLOW}⚠️ Trivy (Security Scanner) not found. 'make scan' will fail.${NC}"
fi
if ! command -v k6 &> /dev/null; then
    echo -e "${YELLOW}⚠️ k6 (Load Testing) not found. 'make stress-test' will fail.${NC}"
fi

echo -e "\n${BOLD}${GREEN}✅ System looks ready for Ignition!${NC}"
