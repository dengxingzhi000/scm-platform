#!/bin/bash
# scripts/db/partition/create-audit-partitions.sh
# Ensure db_audit.sys_audit_log has a monthly partition for every month that
# can receive rows: the previous BACKFILL_MONTHS months (default 6, so an
# already-running database stops failing on "no partition of relation") and the
# next LOOKAHEAD_MONTHS months (default 3).
#
# sys_audit_log is PARTITION BY RANGE (create_time) and uses the
# `<table>_YYYY_MM` naming from scripts/db/microservices/005_db_audit.sql.

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"
DB_NAME="${DB_NAME:-db_audit}"
BACKFILL_MONTHS="${BACKFILL_MONTHS:-6}"
LOOKAHEAD_MONTHS="${LOOKAHEAD_MONTHS:-3}"

echo "Ensuring audit partitions on ${DB_HOST}:${DB_PORT}/${DB_NAME}..."

for OFFSET in $(seq $(( -BACKFILL_MONTHS )) "${LOOKAHEAD_MONTHS}"); do
    if [ "${OFFSET}" -ge 0 ]; then
        MONTH=$(date -d "+${OFFSET} months" +%Y-%m 2>/dev/null || date -v+${OFFSET}m +%Y-%m)
    else
        MONTH=$(date -d "${OFFSET} months" +%Y-%m 2>/dev/null || date -v${OFFSET}m +%Y-%m)
    fi
    NEXT_MONTH=$(date -d "${MONTH}-01 +1 month" +%Y-%m 2>/dev/null || date -v+1m +%Y-%m)
    PARTITION_NAME="sys_audit_log_${MONTH//-/_}"

    PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" \
        -v ON_ERROR_STOP=1 -c "
            CREATE TABLE IF NOT EXISTS ${PARTITION_NAME}
            PARTITION OF sys_audit_log
            FOR VALUES FROM ('${MONTH}-01') TO ('${NEXT_MONTH}-01');
        " >/dev/null

    echo "  Partition ${PARTITION_NAME} ensured"
done

echo "Audit partition maintenance complete"
