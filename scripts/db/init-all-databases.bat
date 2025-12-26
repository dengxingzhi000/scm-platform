@echo off
REM SCM Platform 数据库初始化脚本 (Windows 版本)
REM
REM 使用方法:
REM   set PGPASSWORD=admin123
REM   init-all-databases.bat
REM
REM 环境变量:
REM   DB_HOST - PostgreSQL 主机 (默认: localhost)
REM   DB_PORT - PostgreSQL 端口 (默认: 5432)
REM   DB_USER - PostgreSQL 用户 (默认: admin)
REM   PGPASSWORD - PostgreSQL 密码 (必需)

setlocal enabledelayedexpansion

REM 配置
if not defined DB_HOST set DB_HOST=localhost
if not defined DB_PORT set DB_PORT=5432
if not defined DB_USER set DB_USER=admin

REM 检查密码
if not defined PGPASSWORD (
    echo [错误] 请设置 PGPASSWORD 环境变量
    echo 示例: set PGPASSWORD=admin123
    exit /b 1
)

echo ========================================
echo SCM Platform 数据库初始化
echo ========================================
echo 数据库主机: %DB_HOST%:%DB_PORT%
echo 数据库用户: %DB_USER%
echo.

REM 检查 PostgreSQL 连接
echo 检查 PostgreSQL 连接...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "\q" 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [错误] 无法连接到 PostgreSQL
    echo 请确保 PostgreSQL 正在运行并且连接参数正确
    exit /b 1
)
echo [成功] PostgreSQL 连接成功
echo.

REM 创建数据库
echo 第一步: 创建数据库
for %%d in (db_user db_org db_permission db_approval db_audit db_notify db_product db_inventory db_order db_warehouse db_logistics db_supplier db_tenant db_finance db_purchase) do (
    echo 创建 %%d...
    psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "CREATE DATABASE %%d WITH ENCODING = 'UTF8';" 2>nul
    if !ERRORLEVEL! EQU 0 (
        echo [成功] %%d 创建成功
    ) else (
        echo [跳过] %%d 已存在
    )
)
echo.

REM 初始化表结构
echo 第二步: 初始化表结构
set SCRIPT_DIR=%~dp0

for %%s in (
    "db_user:microservices\001_db_user.sql"
    "db_org:microservices\002_db_org.sql"
    "db_permission:microservices\003_db_permission.sql"
    "db_approval:microservices\004_db_approval.sql"
    "db_audit:microservices\005_db_audit.sql"
    "db_notify:microservices\006_db_notify.sql"
    "db_product:microservices\010_db_product.sql"
    "db_inventory:microservices\011_db_inventory.sql"
    "db_order:microservices\012_db_order.sql"
    "db_warehouse:microservices\013_db_warehouse.sql"
    "db_logistics:microservices\014_db_logistics.sql"
    "db_supplier:microservices\015_db_supplier.sql"
    "db_tenant:microservices\016_db_tenant.sql"
    "db_finance:microservices\017_db_finance.sql"
    "db_purchase:microservices\018_db_purchase.sql"
) do (
    for /f "tokens=1,2 delims=:" %%a in (%%s) do (
        echo 初始化 %%a...
        if exist "%SCRIPT_DIR%%%b" (
            psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %%a -f "%SCRIPT_DIR%%%b" >nul 2>&1
            if !ERRORLEVEL! EQU 0 (
                echo [成功] %%a 初始化成功
            ) else (
                echo [失败] %%a 初始化失败
            )
        ) else (
            echo [跳过] 脚本不存在: %%b
        )
    )
)
echo.

echo ========================================
echo 数据库初始化完成！
echo ========================================
echo.
echo 下一步:
echo   1. 启动 Docker 中间件: docker-compose up -d
echo   2. 启动微服务: cd scm-gateway ^&^& mvn spring-boot:run
echo   3. 访问 Nacos: http://localhost:8848/nacos
echo.

endlocal