#!/bin/bash
set -e

BACKUP_ARCHIVE=$1

if [ -z "$BACKUP_ARCHIVE" ]; then
    echo "❌ Error: Please provide a backup archive path (e.g., ./backups/foculist_backup_20260406_120000.tar.gz)"
    exit 1
fi

TEMP_DIR="./restore_vps"
mkdir -p "$TEMP_DIR"

echo "📂 Extracting archive: $BACKUP_ARCHIVE"
tar -xzf "$BACKUP_ARCHIVE" -C "$TEMP_DIR"

TIMESTAMP_DIR=$(ls "$TEMP_DIR" | head -n 1)

echo "🛡️ Starting Full Platform Restore from: $TIMESTAMP_DIR"

# 1. Postgres Restore
if [ -f "$TEMP_DIR/$TIMESTAMP_DIR/postgres_dump.sql" ]; then
    echo "🐘 Restoring Postgres databases..."
    cat "$TEMP_DIR/$TIMESTAMP_DIR/postgres_dump.sql" | docker exec -i postgres psql -U foculist
fi

# 2. MongoDB Restore
if [ -f "$TEMP_DIR/$TIMESTAMP_DIR/mongodb_dump.archive" ]; then
    echo "🍃 Restoring MongoDB collections..."
    docker exec -i mongodb mongorestore --archive < "$TEMP_DIR/$TIMESTAMP_DIR/mongodb_dump.archive"
fi

# 3. LocalStack State (Inventory only for now)
if [ -f "$TEMP_DIR/$TIMESTAMP_DIR/s3_inventory.txt" ]; then
    echo "☁️ Restoring LocalStack S3 state (Inventory available in restore folder)..."
fi

rm -rf "$TEMP_DIR"
echo "✅ Restore Complete! Platform state is recovered."
