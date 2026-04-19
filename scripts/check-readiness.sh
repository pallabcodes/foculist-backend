#!/bin/bash
set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m'

SERVICES=(
    "http://localhost:8080/health|Gateway"
    "http://localhost:8081/health|Identity"
    "http://localhost:8082/health|Planning"
    "http://localhost:4242/health|Unleash"
    "http://localhost:3000|Grafana"
)

echo -e "${BOLD}🚦 Checking Platform Readiness...${NC}"

ALL_READY=true

for S in "${SERVICES[@]}"; do
    URL="${S%%|*}"
    NAME="${S##*|}"
    
    if curl -s --head --fail "$URL" &> /dev/null; then
        echo -e "  - ${GREEN}✅ $NAME is UP${NC} ($URL)"
    else
        echo -e "  - ${RED}❌ $NAME is DOWN${NC} ($URL)"
        ALL_READY=false
    fi
done

if [ "$ALL_READY" = true ]; then
    echo -e "\n${BOLD}${GREEN}✨ ALL SYSTEMS GO!${NC}"
    exit 0
else
    echo -e "\n${BOLD}${RED}⚠️ Some services are not yet ready.${NC}"
    exit 1
fi
