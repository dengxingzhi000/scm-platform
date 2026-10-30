#!/usr/bin/env bash
# ============================================================
# SCM Platform 本地开发种子数据
#
# 使用方法:
#   export PGPASSWORD=admin123
#   ./seed-all.sh
#
# 兼容变量:
#   PG_HOST / DB_HOST              默认 localhost
#   PG_PORT / DB_PORT              默认 5432
#   PG_ADMIN_USER / DB_USER        默认 admin
#   PG_ADMIN_PASSWORD / PGPASSWORD 必需
#
# 脚本可重复执行；数据使用固定 ID 与 ON CONFLICT 更新。
# ============================================================
set -euo pipefail

DB_HOST="${PG_HOST:-${DB_HOST:-localhost}}"
DB_PORT="${PG_PORT:-${DB_PORT:-5432}}"
DB_USER="${PG_ADMIN_USER:-${DB_USER:-admin}}"
DB_PASSWORD="${PG_ADMIN_PASSWORD:-${PGPASSWORD:-}}"

if [[ -z "${DB_PASSWORD}" ]]; then
    echo "[错误] 请设置 PGPASSWORD 或 PG_ADMIN_PASSWORD 环境变量" >&2
    exit 1
fi

export PGPASSWORD="${DB_PASSWORD}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

while IFS=: read -r database file; do
    [[ -z "${database}" || "${database}" == \#* ]] && continue
    echo "[种子数据] ${database} <- ${file}"
    psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USER}" -d "${database}" \
        -v ON_ERROR_STOP=1 -f "${SCRIPT_DIR}/${file}"
    echo "[成功] ${database} 种子数据完成"
done <<'EOF'
db_tenant:001_tenant.sql
db_permission:002_rbac_permission.sql
db_org:003_org_dept.sql
db_user:004_admin_user.sql
db_product:010_product.sql
db_supplier:013_supplier.sql
db_warehouse:014_warehouse.sql
db_inventory:011_inventory.sql
db_order:012_order.sql
db_logistics:015_logistics.sql
db_finance:016_finance.sql
db_purchase:017_purchase.sql
db_notify:018_notify.sql
EOF

echo
echo "========================================"
echo "本地开发种子数据执行完成"
echo "========================================"
echo "默认租户 ID: 019a0aee-0000-7000-8000-000000000002"
echo "如 scm-web/.env.local 未设置，请添加:"
echo "  NEXT_PUBLIC_DEFAULT_TENANT_ID=019a0aee-0000-7000-8000-000000000002"
echo "  NEXT_PUBLIC_DEFAULT_TENANT_CODE=default"
