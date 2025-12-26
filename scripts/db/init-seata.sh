#!/bin/bash

# Seata 数据库初始化脚本
# 1. 创建 Seata Server 数据库
# 2. 在所有业务数据库中添加 undo_log 表

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 配置
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_USER=${DB_USER:-admin}

# 检查密码
if [ -z "$PGPASSWORD" ]; then
    echo -e "${RED}错误: 请设置 PGPASSWORD 环境变量${NC}"
    exit 1
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Seata 数据库初始化${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 第一步: 初始化 Seata Server 数据库
echo -e "${YELLOW}第一步: 初始化 Seata Server 数据库${NC}"
echo -n "创建 seata 数据库... "
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres \
    -f "$SCRIPT_DIR/microservices/019_db_seata.sql" > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ 成功${NC}"
else
    echo -e "${RED}✗ 失败${NC}"
    exit 1
fi
echo ""

# 第二步: 在所有业务数据库中添加 undo_log 表
echo -e "${YELLOW}第二步: 在业务数据库中添加 undo_log 表${NC}"

business_dbs=(
    "db_product"
    "db_inventory"
    "db_order"
    "db_warehouse"
    "db_logistics"
    "db_supplier"
    "db_purchase"
    "db_finance"
)

for db_name in "${business_dbs[@]}"; do
    echo -n "初始化 $db_name... "
    psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db_name" \
        -f "$SCRIPT_DIR/microservices/020_undo_log_tables.sql" > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 成功${NC}"
    else
        echo -e "${YELLOW}⚠ 跳过（可能已存在）${NC}"
    fi
done
echo ""

# 第三步: 验证
echo -e "${YELLOW}第三步: 验证 Seata 表${NC}"
echo -n "检查 Seata Server 表... "
table_count=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d seata -t \
    -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';" | xargs)
if [ "$table_count" -eq "4" ]; then
    echo -e "${GREEN}✓ 4 张表 (global_table, branch_table, lock_table, distributed_lock)${NC}"
else
    echo -e "${RED}✗ 表数量不正确: $table_count${NC}"
fi

echo -n "检查业务数据库 undo_log 表... "
success_count=0
for db_name in "${business_dbs[@]}"; do
    has_undo=$(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db_name" -t \
        -c "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'undo_log');" | xargs)
    if [ "$has_undo" = "t" ]; then
        ((success_count++))
    fi
done
echo -e "${GREEN}✓ $success_count/${#business_dbs[@]} 个数据库${NC}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Seata 数据库初始化完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "下一步:"
echo "  1. 启动 Seata Server: docker-compose restart seata-server"
echo "  2. 验证注册: 访问 Nacos http://localhost:8848/nacos"
echo "  3. 集成客户端: 在微服务中添加 Seata 依赖"
echo ""