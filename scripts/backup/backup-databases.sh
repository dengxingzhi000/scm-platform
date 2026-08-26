#!/bin/bash
# scripts/backup/backup-databases.sh

set -e

# Configuration
# Use RUNNER_TEMP when invoked from GitHub Actions (read-only root filesystem);
# fall back to /tmp/backups/postgresql for local runs.
BACKUP_DIR="${RUNNER_TEMP:-/tmp}/backups/postgresql"
RETENTION_DAYS=30
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATABASES=(
    "db_user"
    "db_org"
    "db_permission"
    "db_approval"
    "db_audit"
    "db_notify"
    "db_product"
    "db_inventory"
    "db_order"
    "db_warehouse"
    "db_logistics"
    "db_supplier"
    "db_tenant"
    "db_finance"
    "db_purchase"
)

# Create backup directory
mkdir -p "${BACKUP_DIR}/${TIMESTAMP}"

# Backup each database
for DB in "${DATABASES[@]}"; do
    echo "Backing up ${DB}..."
    pg_dump -h "${DB_HOST:-localhost}" -U "${DB_USER:-admin}" -d "${DB}" -F c -f "${BACKUP_DIR}/${TIMESTAMP}/${DB}.dump"
    echo "Completed ${DB}"
done

# Compress backups
tar -czf "${BACKUP_DIR}/backup_${TIMESTAMP}.tar.gz" -C "${BACKUP_DIR}/${TIMESTAMP}" .
rm -rf "${BACKUP_DIR}/${TIMESTAMP}"

# Clean old backups
find "${BACKUP_DIR}" -name "backup_*.tar.gz" -mtime +${RETENTION_DAYS} -delete

echo "Backup completed: ${BACKUP_DIR}/backup_${TIMESTAMP}.tar.gz"
