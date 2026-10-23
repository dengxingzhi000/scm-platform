#!/bin/bash
# scripts/db/retention/apply-retention.sh
# Apply data retention policies

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_USER="${DB_USER:-admin}"
DB_PASSWORD="${DB_PASSWORD:-admin123}"

CH_HOST="${CH_HOST:-localhost}"
CH_PORT="${CH_PORT:-8123}"
CH_USERNAME="${CH_USERNAME:-default}"
CH_PASSWORD="${CH_PASSWORD:-}"

echo "Applying data retention policies..."

# Audit logs: keep 2 years
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_audit -c "
    DELETE FROM sys_audit_log WHERE create_time < NOW() - INTERVAL '2 years';
"
echo "  Audit logs: retained 2 years"

# Login history: keep 1 year
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_auth -c "
    DELETE FROM auth_login_history WHERE create_time < NOW() - INTERVAL '1 year';
"
echo "  Login history: retained 1 year"

# API access logs: keep 90 days
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_auth -c "
    DELETE FROM auth_api_access_log WHERE create_time < NOW() - INTERVAL '90 days';
"
echo "  API access logs: retained 90 days"

# Notification history: keep 6 months
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_notify -c "
    DELETE FROM notify_history WHERE create_time < NOW() - INTERVAL '6 months';
"
echo "  Notification history: retained 6 months"

# Order events: keep 3 years (for audit trail)
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_order -c "
    DELETE FROM ord_order_event WHERE create_time < NOW() - INTERVAL '3 years';
"
echo "  Order events: retained 3 years"

# Analytics quality results: keep 1 year
PGPASSWORD="${DB_PASSWORD}" psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d db_analytics -c "
    DELETE FROM analytics_quality_result WHERE create_time < NOW() - INTERVAL '1 year';
" 2>/dev/null || echo "  Analytics quality results: db_analytics not initialized (skipped)"
echo "  Analytics quality results: retained 1 year"

# ClickHouse DWH: ODS 1 year, DWD 2 years, DWS/ADS 3 years.
# Guarded so a missing ClickHouse instance does not fail the whole retention run.
if command -v curl >/dev/null 2>&1; then
    ch_query() {
        curl -sS --fail --user "${CH_USERNAME}:${CH_PASSWORD}" \
            "http://${CH_HOST}:${CH_PORT}/" --data-binary "$1"
    }
    if ch_query "SELECT 1" >/dev/null 2>&1; then
        ch_query "ALTER TABLE analytics.ods_order DELETE WHERE create_time < now() - INTERVAL 365 DAY SETTINGS mutations_sync = 2" >/dev/null
        ch_query "ALTER TABLE analytics.ods_inventory DELETE WHERE create_time < now() - INTERVAL 365 DAY SETTINGS mutations_sync = 2" >/dev/null
        ch_query "ALTER TABLE analytics.dwd_sales_detail DELETE WHERE pay_time < now() - INTERVAL 730 DAY SETTINGS mutations_sync = 2" >/dev/null
        ch_query "ALTER TABLE analytics.dws_sales_day DELETE WHERE stat_date < today() - 1095 SETTINGS mutations_sync = 2" >/dev/null
        ch_query "ALTER TABLE analytics.dws_inventory_day DELETE WHERE stat_date < today() - 1095 SETTINGS mutations_sync = 2" >/dev/null
        ch_query "ALTER TABLE analytics.ads_sales_dashboard DELETE WHERE stat_date < today() - 1095 SETTINGS mutations_sync = 2" >/dev/null
        ch_query "ALTER TABLE analytics.ads_inventory_health DELETE WHERE stat_date < today() - 1095 SETTINGS mutations_sync = 2" >/dev/null
        echo "  ClickHouse DWH: ODS 1y / DWD 2y / DWS+ADS 3y"
    else
        echo "  ClickHouse not reachable at ${CH_HOST}:${CH_PORT} (skipped)"
    fi
else
    echo "  curl not available; ClickHouse retention skipped"
fi

echo "Data retention policies applied successfully"
