#!/bin/bash
set -x

echo "Initializing LocalStack AWS Cognito..."

export AWS_ACCESS_KEY_ID="test"
export AWS_SECRET_ACCESS_KEY="test"
export AWS_DEFAULT_REGION="us-east-1"
export ENDPOINT="http://localhost:4566"

# Create User Pool
POOL_ID=$(awslocal cognito-idp create-user-pool \
    --pool-name foculist-local-users \
    --admin-create-user-config AllowAdminCreateUserOnly=false \
    --auto-verified-attributes email \
    --query 'UserPool.Id' \
    --output text)

echo "Created User Pool: $POOL_ID"

# Create App Client
CLIENT_ID=$(awslocal cognito-idp create-user-pool-client \
    --user-pool-id $POOL_ID \
    --client-name foculist-local-client \
    --generate-secret \
    --explicit-auth-flows ADMIN_NO_SRP_AUTH USER_PASSWORD_AUTH \
    --query 'UserPoolClient.ClientId' \
    --output text)

echo "Created User Pool Client: $CLIENT_ID"

# Save values to a local file so the Spring Boot app can read them if needed
cat <<EOF > /tmp/localstack-cognito-config.env
AWS_COGNITO_USER_POOL_ID=$POOL_ID
AWS_COGNITO_APP_CLIENT_ID=$CLIENT_ID
EOF

echo "LocalStack Cognito Initialization Complete."
