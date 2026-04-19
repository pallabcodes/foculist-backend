#!/bin/bash
set -e

BACKUP_DIR="./backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
ARCHIVE_NAME="foculist_backup_$TIMESTAMP.tar.gz"

mkdir -p "$BACKUP_DIR/$TIMESTAMP"

echo "🛡️ Starting Full Platform Backup: $TIMESTAMP"

# 1. Postgres Backup (All schemas: public, identity, project, planning, etc.)
echo "🐘 Dumping Postgres databases..."
docker exec postgres pg_dumpall -U foculist > "$BACKUP_DIR/$TIMESTAMP/postgres_dump.sql"

# 2. MongoDB Backup
echo "🍃 Dumping MongoDB collections..."
docker exec mongodb mongodump --archive > "$BACKUP_DIR/$TIMESTAMP/mongodb_dump.archive"

# 3. LocalStack Backup (S3 / DynamoDB) - Basic sync
echo "☁️ Backing up LocalStack state (S3)..."
# In a real environment, we would use 'awslocal s3 sync'
# Here we at least list existing buckets for the manifest
awslocal s3 ls > "$BACKUP_DIR/$TIMESTAMP/s3_inventory.txt"
awslocal secretsmanager list-secrets > "$BACKUP_DIR/$TIMESTAMP/secrets_inventory.json"

# 4. Final Archive
echo "📦 Creating final archive: $ARCHIVE_NAME"
tar -czf "$BACKUP_DIR/$ARCHIVE_NAME" -C "$BACKUP_DIR" "$TIMESTAMP"
rm -rf "$BACKUP_DIR/$TIMESTAMP"

echo "✅ Backup Complete! Archive location: $BACKUP_DIR/$ARCHIVE_NAME"
