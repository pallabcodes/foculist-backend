#!/bin/bash
set -xe

echo "🚀 Initializing LocalStack Infrastructure (Cognito, S3, SNS, SQS)..."

export AWS_ACCESS_KEY_ID="test"
export AWS_SECRET_ACCESS_KEY="test"
export AWS_DEFAULT_REGION="us-east-1"
export ENDPOINT="http://localhost:4566"

# 1. AWS Cognito Initialization
echo "--- Initializing AWS Cognito ---"
POOL_ID=$(awslocal cognito-idp create-user-pool \
    --pool-name foculist-local-users \
    --admin-create-user-config AllowAdminCreateUserOnly=false \
    --auto-verified-attributes email \
    --query 'UserPool.Id' \
    --output text)

echo "Created User Pool: $POOL_ID"

CLIENT_ID=$(awslocal cognito-idp create-user-pool-client \
    --user-pool-id $POOL_ID \
    --client-name foculist-local-client \
    --generate-secret \
    --explicit-auth-flows ADMIN_NO_SRP_AUTH USER_PASSWORD_AUTH \
    --query 'UserPoolClient.ClientId' \
    --output text)

echo "Created User Pool Client: $CLIENT_ID"

cat <<EOF > /tmp/localstack-cognito-config.env
AWS_COGNITO_USER_POOL_ID=$POOL_ID
AWS_COGNITO_APP_CLIENT_ID=$CLIENT_ID
EOF

# 2. AWS S3 Initialization
echo "--- Initializing AWS S3 ---"
awslocal s3 mb s3://foculist-vault
awslocal s3 mb s3://foculist-assets
echo "Created S3 Buckets: foculist-vault, foculist-assets"

# 3. AWS SNS & SQS (Job System) Initialization
echo "--- Initializing AWS SNS & SQS ---"
TOPIC_ARN=$(awslocal sns create-topic --name foculist.workspace.events --query 'TopicArn' --output text)
echo "Created SNS Topic: $TOPIC_ARN"

QUEUE_URL=$(awslocal sqs create-queue --queue-name foculist.automation.jobs --query 'QueueUrl' --output text)
QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url $QUEUE_URL --attribute-names QueueArn --query 'Attributes.QueueArn' --output text)
echo "Created SQS Queue: $QUEUE_URL ($QUEUE_ARN)"

# Subscribe SQS to SNS Topic
awslocal sns subscribe \
    --topic-arn "$TOPIC_ARN" \
    --protocol sqs \
    --notification-endpoint "$QUEUE_ARN"

echo "Subscribed SQS to SNS Topic"

# 4. AWS Secrets Manager Initialization
echo "--- Initializing AWS Secrets Manager ---"
awslocal secretsmanager create-secret \
    --name foculist/common/secrets \
    --secret-string '{"JWT_SECRET":"Zm9jdWxpc3QtZGV2LWp3dC1zZWNyZXQta2VlcC1jaGFuZ2U=","DB_PASSWORD":"foculist","RABBITMQ_PASSWORD":"guest","UNLEASH_TOKEN":"default:development.unleash-insecure-api-token"}'
echo "Created Secrets Manager secret: foculist/common/secrets"

echo "✅ LocalStack Initialization Complete."
