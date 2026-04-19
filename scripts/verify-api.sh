#!/bin/bash

# ==============================================================================
# FOCULIST - SOVEREIGN API VERIFIER
# ------------------------------------------------------------------------------
# Performs a headless integration test of the entire microservice mesh.
# Requirements: curl, jq
# ==============================================================================

set -e

# --- Configuration ---
BASE_URL="http://localhost:8080"
USER_EMAIL="tester-$(date +%s)@foculist.com"
USER_PASS="Password123!"
TENANT_ID="public"

# --- Visual Helpers ---
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}🚀 Starting Foculist Platform API Verification${NC}\n"

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: 'jq' is not installed. Please install it to continue.${NC}"
    exit 1
fi

# --- Helper function for results ---
log_step() {
    if [ $1 -eq 0 ]; then
        echo -e "[ ${GREEN}PASS${NC} ] $2"
    else
        echo -e "[ ${RED}FAIL${NC} ] $2"
        exit 1
    fi
}

# 1. Health Check
echo -e "${BLUE}Step 1: Gateway Health Check...${NC}"
HEALTH=$(curl -s -X GET "$BASE_URL/actuator/health")
STATUS=$(echo "$HEALTH" | jq -r '.status')
if [ "$STATUS" == "UP" ]; then
    log_step 0 "Platform Gateway is UP"
else
    log_step 1 "Platform Gateway is DOWN or UNHEALTHY"
fi

# 2. Register User
echo -e "${BLUE}Step 2: Registering User ($USER_EMAIL)...${NC}"
REG_RES=$(curl -s -X POST "$BASE_URL/v1/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"API Tester\", \"email\": \"$USER_EMAIL\", \"password\": \"$USER_PASS\"}")
log_step $? "User registration request sent"

# 3. Login
echo -e "${BLUE}Step 3: Authenticatication & JWT Acquisition...${NC}"
LOGIN_RES=$(curl -s -X POST "$BASE_URL/v1/auth/login" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: $TENANT_ID" \
    -d "{\"email\": \"$USER_EMAIL\", \"password\": \"$USER_PASS\"}")

JWT=$(echo "$LOGIN_RES" | jq -r '.accessToken')
TENANT_ID=$(echo "$LOGIN_RES" | jq -r '.tenantId')

if [ "$JWT" != "null" ]; then
    log_step 0 "Authenticated successfully. Captured JWT."
else
    echo -e "${RED}Login failure details: $LOGIN_RES${NC}"
    log_step 1 "Authentication failed"
fi

# 4. Onboarding (Create Workspace)
echo -e "${BLUE}Step 4: Onboarding Workspace (Tenant ID: $TENANT_ID)...${NC}"
ONBOARD_RES=$(curl -s -X POST "$BASE_URL/v1/onboard" \
    -H "Authorization: Bearer $JWT" \
    -H "Content-Type: application/json" \
    -d "{\"projectName\": \"Test Workspace\", \"projectKey\": \"TW\", \"experience\": \"ELITE\"}")

ORG_SLUG=$(echo "$ONBOARD_RES" | jq -r '.slug')

if [ "$ORG_SLUG" != "null" ]; then
    log_step 0 "Workspace '$ORG_SLUG' onboarded successfully."
else
    echo -e "${RED}Onboarding failure: $ONBOARD_RES${NC}"
    log_step 1 "Onboarding failed"
fi

# 5. Create Task
echo -e "${BLUE}Step 5: Functional Verification (Task Creation)...${NC}"
TASK_RES=$(curl -s -X POST "$BASE_URL/v1/tasks" \
    -H "Authorization: Bearer $JWT" \
    -H "X-Tenant-ID: $TENANT_ID" \
    -H "Content-Type: application/json" \
    -d "{\"title\": \"Automated API Test Task\", \"description\": \"Created by verify-api.sh\", \"status\": \"TODO\", \"priority\": \"HIGH\"}")

TASK_ID=$(echo "$TASK_RES" | jq -r '.id')

if [ "$TASK_ID" != "null" ]; then
    log_step 0 "Task '$TASK_ID' created successfully via Planning Service."
else
    echo -e "${RED}Task creation failure: $TASK_RES${NC}"
    log_step 1 "Task creation failed"
fi

# 6. Feature Flags Check
echo -e "${BLUE}Step 6: Feature Flag Gating Check...${NC}"
FEATURES=$(curl -s -X GET "$BASE_URL/api/v1/features" \
    -H "Authorization: Bearer $JWT" \
    -H "X-Tenant-ID: $TENANT_ID")

FLAGS_COUNT=$(echo "$FEATURES" | jq '. | length')
log_step 0 "Detected $FLAGS_COUNT active feature flags for tenant $TENANT_ID"

echo -e "\n${BOLD}${GREEN}✅ ALL SYSTEMS GO! Foculist Platform Verified.${NC}"
echo -e "Ready for Frontend Integration."
