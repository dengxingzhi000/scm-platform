#!/bin/bash
# scripts/db/partition/drop-old-partitions.sh
# Drop partitions older than retention period

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"
RETENTION_MONTHS="${RETENTION_MONTHS:-24}"

declare -A TABLE_DB_MAP=(
    ["ord_order"]="db_order"
    ["inv_reservation"]="db_inventory"
    ["sup_purchase_order"]="db_supplier"
)

for TABLE in "${!TABLE_DB_MAP[@]}"; do
    DB_NAME="${TABLE_DB_MAP[$TABLE]}"
    echo "Dropping old partitions for ${TABLE} (database: ${DB_NAME})..."

    CUTOFF_DATE=$(date -d "-${RETENTION_MONTHS} months" +%Y-%m 2>/dev/null || date -v-${RETENTION_MONTHS}m +%Y-%m)

    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "
        SELECT partname FROM pg_partitions
        WHERE tablename = '${TABLE}'
        AND partname < '${TABLE}_p${CUTOFF_DATE//-/}'
    " -t -A | while read PARTITION; do
        if [ -n "$PARTITION" ]; then
            PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "
                DROP TABLE IF EXISTS ${PARTITION};
            "
            echo "  Dropped ${PARTITION}"
        fi
    done
done

echo "Old partition cleanup complete"
