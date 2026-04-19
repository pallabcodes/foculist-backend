#!/bin/bash
set -e

echo "⚠️ Starting Schema Evolution Compatibility Audit..."

# Patterns that typically break backward compatibility
DESTRUCTIVE_PATTERNS=(
    "DROP COLUMN"
    "DROP TABLE"
    "RENAME COLUMN"
    "RENAME TABLE"
    "ALTER COLUMN .* SET NOT NULL"
    "ALTER COLUMN .* TYPE"
)

# Iterate through every microservice's migration folder
MIGRATION_DIRS=$(find services -type d -name "migration")

TOTAL_ISSUES=0

for DIR in $MIGRATION_DIRS; do
    echo "🔍 Auditing: $DIR"
    
    for PATTERN in "${DESTRUCTIVE_PATTERNS[@]}"; do
        # Use grep with Extended Regexp (-E) across all SQL files in the directory
        MATCHES=$(grep -Ei "$PATTERN" "$DIR"/*.sql 2>/dev/null || true)
        
        if [ ! -z "$MATCHES" ]; then
            echo "❌ POTENTIAL DESTRUCTIVE CHANGE DETECTED (Pattern: $PATTERN):"
            echo "$MATCHES"
            TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
        fi
    done
done

if [ "$TOTAL_ISSUES" -gt 0 ]; then
    echo ""
    echo "🔴 COMPATIBILITY AUDIT FAILED with $TOTAL_ISSUES issues."
    echo "💡 PRO-TIP: Avoid 'DROP' or 'NOT NULL' in rolling deployments. Add columns as nullable first, migrate data, then add constraints in a separate 'Phase 2' release."
    # exit 1 # Uncomment to block CI/CD
else
    echo "✅ SCHEMA EVOLUTION AUDIT PASSED! All migrations appear toward backward-compatible growth."
fi
