#!/bin/bash
set -e

GATEWAY_URL="http://localhost:8080"
TENANT_ID="public"

echo "🌱 Seeding Foculist Platform Data..."

# 1. Register User
echo "👤 Registering admin user..."
curl -s -X POST "$GATEWAY_URL/api/identity/v1/auth/register" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -d '{
    "name": "Admin User",
    "email": "admin@foculist.local",
    "password": "Password123!"
  }' | jq .

# 2. Login (with Dev Bypass for MFA)
echo "🔑 Logging in..."
LOGIN_RES=$(curl -s -X POST "$GATEWAY_URL/api/identity/v1/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -H "X-Dev-Bypass: true" \
  -d '{
    "email": "admin@foculist.local",
    "password": "Password123!"
  }')

TOKEN=$(echo $LOGIN_RES | jq -r '.accessToken')
USER_ID=$(echo $LOGIN_RES | jq -r '.userId')

if [ "$TOKEN" == "null" ]; then
  echo "❌ Login failed"
  echo $LOGIN_RES
  exit 1
fi

echo "✅ Logged in. Token acquired."

# 3. Onboard Workspace
echo "🏢 Onboarding workspace (Acme Corp)..."
ONBOARD_RES=$(curl -s -X POST "$GATEWAY_URL/api/identity/v1/onboard" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: $TENANT_ID" \
  -d '{
    "projectName": "Acme Corp",
    "projectKey": "ACME",
    "experience": "B2B",
    "invites": []
  }')

ORG_ID=$(echo $ONBOARD_RES | jq -r '.id')
echo "✅ Workspace created: $ORG_ID"

# 4. Create Initial Task (Wait for outbox to sync the project first)
echo "⏳ Waiting for RabbitMQ/Outbox to provision project service (3s)..."
sleep 3

echo "📝 Creating initial task in Planning Service..."
curl -s -X POST "$GATEWAY_URL/api/planning/v1/tasks" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: $ORG_ID" \
  -d '{
    "title": "Setup Foculist Dashboard",
    "description": "Initialize the workspace and invite the team.",
    "status": "TODO",
    "priority": "HIGH"
  }' | jq .

echo "✨ Seeding complete! You can now log into http://localhost:8080 with admin@foculist.local"
