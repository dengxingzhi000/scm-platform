#!/bin/bash

# Debezium PostgreSQL Connector 配置脚本
#
# 用途：
# 1. 创建 PostgreSQL Publication（逻辑复制发布）
# 2. 注册 Debezium PostgreSQL Connector
# 3. 验证 Connector 状态
#
# 使用方法：
#   chmod +x setup-debezium-connector.sh
#   ./setup-debezium-connector.sh
#
# 作者: SCM Platform Team
# 日期: 2025-12-26

set -e

# 颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Debezium PostgreSQL Connector 配置${NC}"
echo -e "${GREEN}========================================${NC}"

# 步骤 1: 创建 PostgreSQL Publication
echo -e "\n${YELLOW}[步骤 1] 创建 PostgreSQL Publication...${NC}"

# 连接到 PostgreSQL 并创建 Publication
docker exec -it scm-postgres psql -U admin -d db_product <<EOF
-- 创建 Publication（逻辑复制发布）
CREATE PUBLICATION scm_product_publication FOR TABLE prod_spu, prod_sku, prod_category, prod_brand;

-- 验证 Publication
SELECT * FROM pg_publication WHERE pubname = 'scm_product_publication';

-- 查看 Publication 包含的表
SELECT * FROM pg_publication_tables WHERE pubname = 'scm_product_publication';
EOF

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Publication 创建成功${NC}"
else
    echo -e "${RED}✗ Publication 创建失败${NC}"
    exit 1
fi

# 步骤 2: 等待 Kafka Connect 启动
echo -e "\n${YELLOW}[步骤 2] 等待 Kafka Connect 启动...${NC}"

MAX_RETRIES=30
RETRY_COUNT=0

until $(curl --output /dev/null --silent --head --fail http://localhost:8083/); do
    RETRY_COUNT=$((RETRY_COUNT+1))
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo -e "${RED}✗ Kafka Connect 启动超时${NC}"
        exit 1
    fi
    echo -e "${YELLOW}等待 Kafka Connect 启动... ($RETRY_COUNT/$MAX_RETRIES)${NC}"
    sleep 2
done

echo -e "${GREEN}✓ Kafka Connect 已启动${NC}"

# 步骤 3: 注册 Debezium PostgreSQL Connector
echo -e "\n${YELLOW}[步骤 3] 注册 Debezium PostgreSQL Connector...${NC}"

curl -X POST http://localhost:8083/connectors \
  -H "Content-Type: application/json" \
  -d '{
  "name": "scm-product-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "plugin.name": "pgoutput",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "admin",
    "database.password": "scm_password_2025",
    "database.dbname": "db_product",
    "database.server.name": "scm_product_server",
    "table.include.list": "public.prod_spu,public.prod_sku,public.prod_category,public.prod_brand",
    "publication.name": "scm_product_publication",
    "slot.name": "scm_product_slot",
    "topic.prefix": "scm_product_server",
    "transforms": "unwrap",
    "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
    "transforms.unwrap.drop.tombstones": "false",
    "key.converter": "org.apache.kafka.connect.json.JsonConverter",
    "value.converter": "org.apache.kafka.connect.json.JsonConverter",
    "key.converter.schemas.enable": "false",
    "value.converter.schemas.enable": "false"
  }
}'

if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}✓ Connector 注册成功${NC}"
else
    echo -e "\n${RED}✗ Connector 注册失败${NC}"
    exit 1
fi

# 步骤 4: 验证 Connector 状态
echo -e "\n${YELLOW}[步骤 4] 验证 Connector 状态...${NC}"

sleep 5

CONNECTOR_STATUS=$(curl -s http://localhost:8083/connectors/scm-product-connector/status)

echo -e "\n${GREEN}Connector 状态:${NC}"
echo "$CONNECTOR_STATUS" | jq '.'

# 检查状态是否为 RUNNING
STATE=$(echo "$CONNECTOR_STATUS" | jq -r '.connector.state')
if [ "$STATE" == "RUNNING" ]; then
    echo -e "\n${GREEN}✓ Connector 运行正常${NC}"
else
    echo -e "\n${RED}✗ Connector 状态异常: $STATE${NC}"
    exit 1
fi

# 步骤 5: 查看 Kafka Topics
echo -e "\n${YELLOW}[步骤 5] 查看 Kafka Topics...${NC}"

docker exec scm-kafka kafka-topics --bootstrap-server localhost:9092 --list | grep scm_product

# 步骤 6: 查看 Topic 数据（仅前 5 条）
echo -e "\n${YELLOW}[步骤 6] 查看 Topic 数据（prod_spu）...${NC}"

docker exec scm-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic scm_product_server.public.prod_spu \
  --from-beginning \
  --max-messages 5 \
  --timeout-ms 5000 || true

# 完成
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}✓ Debezium Connector 配置完成！${NC}"
echo -e "${GREEN}========================================${NC}"

echo -e "\n${YELLOW}后续操作：${NC}"
echo -e "1. 插入测试数据到 PostgreSQL："
echo -e "   docker exec -it scm-postgres psql -U admin -d db_product"
echo -e "   INSERT INTO prod_spu (id, spu_code, spu_name, status) VALUES ('test001', 'SPU001', 'Test Product', 1);"
echo -e ""
echo -e "2. 查看 Kafka Topic 数据："
echo -e "   docker exec scm-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic scm_product_server.public.prod_spu --from-beginning"
echo -e ""
echo -e "3. 检查 Elasticsearch 数据："
echo -e "   curl http://localhost:9200/scm_product/_search"
echo -e ""