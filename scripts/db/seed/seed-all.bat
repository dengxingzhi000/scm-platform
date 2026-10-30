@echo off
REM ============================================================
REM SCM Platform 本地开发种子数据
REM
REM 使用方法:
REM   set PGPASSWORD=admin123
REM   seed-all.bat
REM
REM 环境变量:
REM   DB_HOST - PostgreSQL 主机 (默认: localhost)
REM   DB_PORT - PostgreSQL 端口 (默认: 5432)
REM   DB_USER - PostgreSQL 用户 (默认: admin)
REM   PGPASSWORD - PostgreSQL 密码 (必需)
REM
REM 脚本可重复执行；数据使用固定 ID 与 ON CONFLICT 更新。
REM ============================================================
setlocal enabledelayedexpansion

if not defined DB_HOST set DB_HOST=localhost
if not defined DB_PORT set DB_PORT=5432
if not defined DB_USER set DB_USER=admin

if not defined PGPASSWORD (
    echo [错误] 请设置 PGPASSWORD 环境变量
    echo 示例: set PGPASSWORD=admin123
    exit /b 1
)

set SCRIPT_DIR=%~dp0

for %%s in (
    "db_tenant:001_tenant.sql"
    "db_permission:002_rbac_permission.sql"
    "db_org:003_org_dept.sql"
    "db_user:004_admin_user.sql"
    "db_product:010_product.sql"
    "db_supplier:013_supplier.sql"
    "db_warehouse:014_warehouse.sql"
    "db_inventory:011_inventory.sql"
    "db_order:012_order.sql"
    "db_logistics:015_logistics.sql"
    "db_finance:016_finance.sql"
    "db_purchase:017_purchase.sql"
    "db_notify:018_notify.sql"
) do (
    for /f "tokens=1,2 delims=:" %%a in (%%s) do (
        echo [种子数据] %%a ^<- %%b
        psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %%a -v ON_ERROR_STOP=1 -f "%SCRIPT_DIR%%%b"
        if !ERRORLEVEL! NEQ 0 (
            echo [失败] %%a 种子数据执行失败
            exit /b 1
        )
        echo [成功] %%a 种子数据完成
    )
)

echo.
echo ========================================
echo 本地开发种子数据执行完成
echo ========================================
echo 默认租户 ID: 019a0aee-0000-7000-8000-000000000002
echo 如 scm-web/.env.local 未设置，请添加:
echo   NEXT_PUBLIC_DEFAULT_TENANT_ID=019a0aee-0000-7000-8000-000000000002
echo   NEXT_PUBLIC_DEFAULT_TENANT_CODE=default

endlocal
