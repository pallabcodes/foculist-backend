#!/bin/bash
set -e

# Configuration
DURATION=${1:-30s}
CONTAINER_NAME="sync-service"
OUTPUT_FILE="sync-flamegraph.html"

echo "🔥 Initiating 30-second profiling for $CONTAINER_NAME (CPU & Allocation)..."

# Ensure the container is running and has async-profiler
docker exec $CONTAINER_NAME sh -c "/app/profiler.sh -d $DURATION -f /tmp/flamegraph.html jps"

# Retrieve the flamegraph
docker cp $CONTAINER_NAME:/tmp/flamegraph.html ./$OUTPUT_FILE

echo "✅ Profiling complete. Flamegraph saved to: $OUTPUT_FILE"
echo "💡 Open this file in your browser to analyze the Sync Engine's performance."
