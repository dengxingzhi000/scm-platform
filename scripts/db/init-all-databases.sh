#!/bin/bash

# ======================================================================
# SCM Platform - Database Initialization Script
# 一键初始化所有微服务数据库（PostgreSQL 16+）
# ======================================================================

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# PostgreSQL连接信息
PG_HOST="${PG_HOST:-localhost}"
PG_PORT="${PG_PORT:-5432}"
PG_ADMIN_USER="${PG_ADMIN_USER:-postgres}"
PG_ADMIN_PASSWORD="${PG_ADMIN_PASSWORD}"

# 数据库列表
DATABASES=(
    "db_user:用户服务"
    "db_org:组织服务"
    "db_permission:权限服务"
    "db_approval:审批服务"
    "db_audit:审计服务"
    "db_notify:通知服务"
    "db_product:商品服务"
    "db_inventory:库存服务"
    "db_order:订单服务"
    "db_warehouse:仓储服务"
    "db_logistics:物流服务"
    "db_supplier:供应商服务"
)

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}SCM Platform Database Initialization${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "PostgreSQL Host: ${YELLOW}${PG_HOST}:${PG_PORT}${NC}"
echo -e "Admin User: ${YELLOW}${PG_ADMIN_USER}${NC}"
echo ""

# 检查PostgreSQL连接
echo -e "${YELLOW}检查PostgreSQL连接...${NC}"
if PGPASSWORD="${PG_ADMIN_PASSWORD}" psql -h "${PG_HOST}" -p "${PG_PORT}" -U "${PG_ADMIN_USER}" -d postgres -c "SELECT version();" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ PostgreSQL连接成功${NC}"
else
    echo -e "${RED}✗ PostgreSQL连接失败，请检查配置${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}步骤 1: 创建数据库${NC}"
echo -e "${YELLOW}========================================${NC}"

for db_entry in "${DATABASES[@]}"; do
    IFS=':' read -r db_name db_desc <<< "$db_entry"

    echo -n "创建数据库 ${db_name} (${db_desc})... "

    # 检查数据库是否存在
    EXISTS=$(PGPASSWORD="${PG_ADMIN_PASSWORD}" psql -h "${PG_HOST}" -p "${PG_PORT}" -U "${PG_ADMIN_USER}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${db_name}'")

    if [ "$EXISTS" = "1" ]; then
        echo -e "${YELLOW}已存在（跳过）${NC}"
    else
        PGPASSWORD="${PG_ADMIN_PASSWORD}" psql -h "${PG_HOST}" -p "${PG_PORT}" -U "${PG_ADMIN_USER}" -d postgres -c "CREATE DATABASE ${db_name} WITH ENCODING = 'UTF8';" > /dev/null 2>&1
        echo -e "${GREEN}✓ 成功${NC}"
    fi
done

echo ""
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}步骤 2: 初始化表结构${NC}"
echo -e "${YELLOW}========================================${NC}"

# 初始化基础服务数据库（已存在的）
EXISTING_SCRIPTS=(
    "microservices/001_db_user.sql:db_user"
    "microservices/002_db_org.sql:db_org"
    "microservices/003_db_permission.sql:db_permission"
    "microservices/004_db_approval.sql:db_approval"
    "microservices/005_db_audit.sql:db_audit"
    "microservices/006_db_notify.sql:db_notify"
)

for script_entry in "${EXISTING_SCRIPTS[@]}"; do
    IFS=':' read -r script_path db_name <<< "$script_entry"

    if [ -f "$script_path" ]; then
        echo -e "执行脚本: ${YELLOW}${script_path}${NC} -> ${db_name}"
        PGPASSWORD="${PG_ADMIN_PASSWORD}" psql -h "${PG_HOST}" -p "${PG_PORT}" -U "${PG_ADMIN_USER}" -d "${db_name}" -f "$script_path" > /dev/null 2>&1
        echo -e "${GREEN}✓ 完成${NC}"
    else
        echo -e "${RED}✗ 脚本不存在: ${script_path}${NC}"
    fi
done

# 初始化SCM业务服务数据库（新创建的）
SCM_SCRIPTS=(
    "microservices/010_db_product.sql:db_product"
    "microservices/011_db_inventory.sql:db_inventory"
    "microservices/012_db_order.sql:db_order"
    "microservices/013_db_warehouse.sql:db_warehouse"
    "microservices/014_db_logistics.sql:db_logistics"
    "microservices/015_db_supplier.sql:db_supplier"
)

for script_entry in "${SCM_SCRIPTS[@]}"; do
    IFS=':' read -r script_path db_name <<< "$script_entry"

    if [ -f "$script_path" ]; then
        echo -e "执行脚本: ${YELLOW}${script_path}${NC} -> ${db_name}"
        PGPASSWORD="${PG_ADMIN_PASSWORD}" psql -h "${PG_HOST}" -p "${PG_PORT}" -U "${PG_ADMIN_USER}" -d "${db_name}" -f "$script_path" > /dev/null 2>&1
        echo -e "${GREEN}✓ 完成${NC}"
    else
        echo -e "${RED}✗ 脚本不存在: ${script_path}${NC}"
    fi
done

# 添加数据冗余字段（如果存在）
if [ -f "microservices/007_data_redundancy.sql" ]; then
    echo ""
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}步骤 3: 添加数据冗余字段${NC}"
    echo -e "${YELLOW}========================================${NC}"

    # 这个脚本需要在多个数据库中执行，根据实际情况调整
    echo -e "执行脚本: ${YELLOW}microservices/007_data_redundancy.sql${NC}"
    echo -e "${YELLOW}（需要手动确认具体执行的数据库）${NC}"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}数据库初始化完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "已创建以下数据库："
for db_entry in "${DATABASES[@]}"; do
    IFS=':' read -r db_name db_desc <<< "$db_entry"
    echo -e "  ${GREEN}✓${NC} ${db_name} - ${db_desc}"
done

echo ""
echo -e "${YELLOW}下一步：${NC}"
echo -e "1. 配置各微服务的数据源连接（application.yml）"
echo -e "2. 启动Nacos配置中心"
echo -e "3. 依次启动各微服务"
echo ""