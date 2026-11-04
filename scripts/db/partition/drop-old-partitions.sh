#!/bin/bash
# scripts/db/partition/drop-old-partitions.sh
# Drop monthly partitions older than the retention period.
#
# Partitions are discovered through pg_inherits (the catalog source of truth);
# the previous implementation queried pg_partitions, which does not exist on a
# plain PostgreSQL server, so it silently dropped nothing.

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
    ["sys_audit_log"]="db_audit"
)

CUTOFF_YYYYMM=$(date -d "-${RETENTION_MONTHS} months" +%Y%m 2>/dev/null || date -v-${RETENTION_MONTHS}m +%Y%m)

for TABLE in "${!TABLE_DB_MAP[@]}"; do
    DB_NAME="${TABLE_DB_MAP[$TABLE]}"
    echo "Dropping partitions of ${TABLE} older than ${CUTOFF_YYYYMM} (database: ${DB_NAME})..."

    PARTITIONS=$(PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -t -A -c "
        SELECT c.relname
        FROM pg_inherits i
        JOIN pg_class c ON c.oid = i.inhrelid
        JOIN pg_class p ON p.oid = i.inhparent
        WHERE p.relname = '${TABLE}'
        ORDER BY c.relname;
    ")

    for PARTITION in ${PARTITIONS}; do
        # Accept both naming styles: <table>_pYYYYMM and <table>_YYYY_MM
        SUFFIX="${PARTITION#${TABLE}_}"
        SUFFIX="${SUFFIX#p}"
        SUFFIX="${SUFFIX//_/}"

        if [[ "${SUFFIX}" =~ ^[0-9]{6}$ ]] && [ "${SUFFIX}" -lt "${CUTOFF_YYYYMM}" ]; then
            PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${DB_NAME}" -c "
                DROP TABLE IF EXISTS ${PARTITION};
            " >/dev/null
            echo "  Dropped ${PARTITION}"
        fi
    done
done

echo "Old partition cleanup complete"
