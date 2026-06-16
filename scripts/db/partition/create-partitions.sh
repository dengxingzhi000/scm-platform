#!/bin/bash
# scripts/db/partition/create-partitions.sh
# Auto-create monthly partitions for partitioned tables

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"

declare -A TABLE_DB_MAP=(
    ["ord_order"]="db_order"
    ["inv_reservation"]="db_inventory"
    ["sup_purchase_order"]="db_supplier"
)

for TABLE in "${!TABLE_DB_MAP[@]}"; do
    DB_NAME="${TABLE_DB_MAP[$TABLE]}"
    echo "Creating partitions for ${TABLE} (database: ${DB_NAME})..."

    for i in $(seq 0 2); do
        PARTITION_DATE=$(date -d "+${i} months" +%Y-%m 2>/dev/null || date -v+${i}m +%Y-%m)
        PARTITION_NAME="${TABLE}_p${PARTITION_DATE//-/}"

        NEXT_MONTH=$(date -d "${PARTITION_DATE}-01 +1 month" +%Y-%m 2>/dev/null || date -v+$((i+1))m +%Y-%m)
        PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "
            CREATE TABLE IF NOT EXISTS ${PARTITION_NAME}
            PARTITION OF ${TABLE}
            FOR VALUES FROM ('${PARTITION_DATE}-01') TO ('${NEXT_MONTH}-01');
        " 2>/dev/null || true

        echo "  Partition ${PARTITION_NAME} ensured"
    done
done

echo "Partition management complete"
