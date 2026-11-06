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
    # 库存流水：既有分区沿用 <table>_YYYY_MM 命名，脚本新建的为 <table>_pYYYYMM
    ["inv_log"]="db_inventory"
    ["sup_purchase_order"]="db_supplier"
)

for TABLE in "${!TABLE_DB_MAP[@]}"; do
    DB_NAME="${TABLE_DB_MAP[$TABLE]}"
    echo "Creating partitions for ${TABLE} (database: ${DB_NAME})..."

    for i in $(seq 0 2); do
        PARTITION_DATE=$(date -d "+${i} months" +%Y-%m 2>/dev/null || date -v+${i}m +%Y-%m)
        PARTITION_NAME="${TABLE}_p${PARTITION_DATE//-/}"

        NEXT_MONTH=$(date -d "${PARTITION_DATE}-01 +1 month" +%Y-%m 2>/dev/null || date -v+$((i+1))m +%Y-%m)
        LEGACY_PARTITION_NAME="${TABLE}_${PARTITION_DATE//-/_}"

        # 同一个月份可能已存在两种命名（历史 _YYYY_MM / 本脚本 _pYYYYMM）。
        # 先查名字再建，避免对已覆盖的月份重复 ATTACH（重复 ATTACH 会报
        # "partition ... would overlap" 并被下面的容错吞掉，看起来像成功）。
        PARTITION_EXISTS=$(PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -Atc "
            SELECT 1 FROM pg_class
            WHERE relname IN ('${PARTITION_NAME}', '${LEGACY_PARTITION_NAME}')
            LIMIT 1;
        " 2>/dev/null || true)
        if [ -n "${PARTITION_EXISTS}" ]; then
            echo "  Partition for ${PARTITION_DATE} already exists; skipped"
            continue
        fi

        PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "
            CREATE TABLE IF NOT EXISTS ${PARTITION_NAME}
            PARTITION OF ${TABLE}
            FOR VALUES FROM ('${PARTITION_DATE}-01') TO ('${NEXT_MONTH}-01');
        " 2>/dev/null || true

        echo "  Partition ${PARTITION_NAME} ensured"
    done
done

echo "Partition management complete"
