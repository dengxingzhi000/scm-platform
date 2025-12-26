# SCM Platform - 数据库设计文档

## 概述

SCM Platform采用微服务架构，数据库按服务拆分为12个独立数据库。本文档描述了SCM业务相关的6个新增数据库。

## 技术特性

### 核心技术栈
- **数据库**: PostgreSQL 16+
- **主键策略**: UUIDv7（时间有序UUID）
- **ORM框架**: MyBatis-Plus 3.5.15
- **分区策略**: 按月分区（订单、预占、日志等大数据量表）
- **时间字段**: 由MyBatis-Plus自动填充（create_time, update_time）

### UUIDv7 优势
```sql
-- 时间有序的UUID，兼具UUID和自增ID的优点
-- 1. 包含时间戳信息，天然有序
-- 2. 128位全局唯一
-- 3. 索引友好，减少B树分裂
-- 4. 支持分布式环境
CREATE OR REPLACE FUNCTION uuid_generate_v7() RETURNS uuid AS ...
```

### 分区表策略
对于高增长表（订单、库存日志、预占记录等），采用按月Range分区：

```sql
-- 自动按月分区，便于数据归档和查询优化
CREATE TABLE ord_order (...) PARTITION BY RANGE (create_time);

CREATE TABLE ord_order_2025_01 PARTITION OF ord_order
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');
```

**定期维护**: 每月创建下月分区，归档/删除旧分区。

## 数据库列表

### 基础设施服务（已有）

| 数据库 | 服务 | 职责 |
|--------|------|------|
| db_user | 用户服务 | 用户账户、OAuth、WebAuthn |
| db_org | 组织服务 | 部门管理、组织架构 |
| db_permission | 权限服务 | RBAC、角色、数据权限 |
| db_approval | 审批服务 | 权限申请、审批流程 |
| db_audit | 审计服务 | 操作审计、敏感日志 |
| db_notify | 通知服务 | 消息通知、模板 |

### SCM业务服务（新增）

| 数据库 | 服务 | 职责 | 核心表 |
|--------|------|------|--------|
| **db_product** | 商品服务 | 商品SPU/SKU、分类、品牌 | prod_spu, prod_sku, prod_category, prod_brand |
| **db_inventory** | 库存服务 | 库存管理、预占、日志 | inv_inventory, inv_reservation, inv_log |
| **db_order** | 订单服务 | 订单、支付、退款 | ord_order, ord_order_item, ord_payment |
| **db_warehouse** | 仓储服务 | 仓库、入库、出库、波次拣货 | wms_warehouse, wms_inbound, wms_outbound |
| **db_logistics** | 物流服务 | 运单、轨迹、路线规划 | tms_waybill, tms_tracking, tms_route |
| **db_supplier** | 供应商服务 | 供应商、采购、对账 | sup_supplier, sup_purchase_order, sup_settlement |

## 核心表设计

### 1. 商品服务 (db_product)

#### prod_spu - SPU表
```sql
CREATE TABLE prod_spu (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    spu_code VARCHAR(128) NOT NULL UNIQUE,
    spu_name VARCHAR(256) NOT NULL,
    category_id UUID NOT NULL,  -- 分类
    brand_id UUID,              -- 品牌
    images JSONB DEFAULT '[]',  -- 图片数组
    min_price DECIMAL(12, 2),   -- 最低价（从SKU计算）
    max_price DECIMAL(12, 2),   -- 最高价（从SKU计算）
    total_stock INT DEFAULT 0,  -- 总库存（从SKU计算）
    total_sales INT DEFAULT 0,  -- 总销量
    status SMALLINT DEFAULT 0,  -- 0:草稿 1:上架 2:下架
    ...
);
```

#### prod_sku - SKU表
```sql
CREATE TABLE prod_sku (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    spu_id UUID NOT NULL,
    sku_code VARCHAR(128) NOT NULL UNIQUE,
    attributes JSONB NOT NULL,  -- SKU属性 {"color":"黑色","storage":"256GB"}
    selling_price DECIMAL(12, 2) NOT NULL,
    stock INT DEFAULT 0,
    sales_count INT DEFAULT 0,
    status SMALLINT DEFAULT 1,  -- 0:停用 1:启用 2:缺货
    ...
);
```

**亮点**:
- JSONB存储灵活属性（颜色、尺寸等）
- 触发器自动更新SPU价格区间和库存
- pg_trgm扩展支持商品名模糊搜索

### 2. 库存服务 (db_inventory)

#### inv_inventory - 库存表
```sql
CREATE TABLE inv_inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    total_stock INT DEFAULT 0,      -- 总库存
    available_stock INT DEFAULT 0,  -- 可用库存
    locked_stock INT DEFAULT 0,     -- 锁定库存（预占）
    damaged_stock INT DEFAULT 0,    -- 损坏库存
    safety_stock INT DEFAULT 0,     -- 安全库存
    version INT DEFAULT 0,          -- 乐观锁版本号
    ...
    CONSTRAINT chk_inv_stock_balance CHECK (
        total_stock = available_stock + locked_stock + damaged_stock
    )
);
```

#### inv_reservation - 库存预占表（分区）
```sql
CREATE TABLE inv_reservation (
    id UUID DEFAULT uuid_generate_v7(),
    reservation_no VARCHAR(128) UNIQUE,
    sku_id UUID NOT NULL,
    order_id UUID NOT NULL,
    quantity INT NOT NULL,
    status SMALLINT DEFAULT 0,  -- 0:已预占 1:已确认 2:已释放 3:已过期
    expire_at TIMESTAMPTZ NOT NULL,  -- 15分钟后自动释放
    ...
) PARTITION BY RANGE (reserved_at);
```

**亮点**:
- 原子库存扣减函数（fn_deduct_stock）
- 预占机制：订单创建时预占库存，支付成功确认，超时自动释放
- 库存日志分区表，记录所有变动

**高并发方案**:
```java
// 应用层使用Redis Lua脚本处理秒杀等高并发场景
// 数据库函数作为兜底方案，保证最终一致性
SELECT fn_deduct_stock(sku_id, warehouse_id, quantity, order_id, order_no, operator_id);
```

### 3. 订单服务 (db_order)

#### ord_order - 订单表（分区）
```sql
CREATE TABLE ord_order (
    id UUID DEFAULT uuid_generate_v7(),
    order_no VARCHAR(128) UNIQUE,
    user_id UUID NOT NULL,
    status SMALLINT DEFAULT 0,  -- Spring State Machine状态
    total_amount DECIMAL(12, 2) NOT NULL,
    payable_amount DECIMAL(12, 2) NOT NULL,
    shipping_address JSONB NOT NULL,  -- 收货地址
    payment_deadline TIMESTAMPTZ,     -- 支付截止时间
    auto_cancel_at TIMESTAMPTZ,       -- 自动取消时间（XXL-Job）
    reservation_id UUID,              -- 库存预占ID
    ...
) PARTITION BY RANGE (create_time);
```

**订单状态流转** (Spring State Machine):
```
PENDING_PAYMENT(0) → PAID(1) → PENDING_SHIP(2) → SHIPPED(3)
→ IN_TRANSIT(4) → DELIVERED(5) → COMPLETED(6)
                    ↓
                CANCELLED(7)
```

#### ord_status_history - 状态流转历史
```sql
CREATE TABLE ord_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    order_id UUID NOT NULL,
    from_status SMALLINT,
    to_status SMALLINT NOT NULL,
    event VARCHAR(64),  -- State Machine事件：PAYMENT_SUCCESS, SHIP_OUT等
    operator_id UUID,
    transitioned_at TIMESTAMPTZ DEFAULT NOW()
);
```

**亮点**:
- 触发器自动记录状态变更到历史表
- JSONB存储收货地址和发票信息
- 视图v_pending_payment_orders用于XXL-Job定时任务自动取消超时订单

### 4. 仓储服务 (db_warehouse)

#### wms_warehouse - 仓库表
```sql
CREATE TABLE wms_warehouse (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    warehouse_code VARCHAR(64) UNIQUE,
    warehouse_type SMALLINT DEFAULT 1,  -- 1:中心仓 2:区域仓 3:前置仓
    province VARCHAR(64),
    city VARCHAR(64),
    total_capacity INT,    -- 总容量
    used_capacity INT,     -- 已用容量
    ...
);
```

#### wms_outbound - 出库单表
```sql
CREATE TABLE wms_outbound (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    outbound_no VARCHAR(128) UNIQUE,
    warehouse_id UUID NOT NULL,
    priority SMALLINT DEFAULT 1,  -- 1:普通 2:紧急 3:特急
    picking_path JSONB,           -- 拣货路径优化 [{locationCode, skuId, quantity, distance}]
    total_distance INT,           -- 总距离（米）
    ...
);
```

#### wms_wave_picking - 波次拣货表
```sql
CREATE TABLE wms_wave_picking (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    wave_no VARCHAR(128) UNIQUE,
    outbound_ids UUID[],          -- 合并拣货的出库单ID数组
    order_count INT DEFAULT 0,
    picking_path JSONB,           -- 优化后的路径
    optimization_rate DECIMAL(5, 2),  -- 优化率（%）
    ...
);
```

**亮点**:
- 支持库位管理（zone-shelf-layer-position）
- 波次拣货：合并多个订单，优化拣货路径（应用层实现TSP算法）
- JSONB存储拣货路径，支持复杂路径规划

### 5. 物流服务 (db_logistics)

#### tms_waybill - 运单表
```sql
CREATE TABLE tms_waybill (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    waybill_no VARCHAR(128) UNIQUE,
    carrier_id UUID NOT NULL,
    order_id UUID NOT NULL,
    receiver_address JSONB NOT NULL,
    status SMALLINT DEFAULT 0,  -- 0:待揽件 1:已揽件 2:运输中 3:派送中 4:已签收
    estimated_delivery TIMESTAMPTZ,
    sign_type SMALLINT,  -- 1:本人 2:代签 3:快递柜
    ...
);
```

#### tms_tracking - 物流轨迹表
```sql
CREATE TABLE tms_tracking (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    waybill_id UUID NOT NULL,
    track_time TIMESTAMPTZ NOT NULL,
    location VARCHAR(256),
    description TEXT NOT NULL,
    longitude DECIMAL(11, 8),  -- 经度
    latitude DECIMAL(10, 8),   -- 纬度
    ...
);
```

#### tms_route - 配送路线表
```sql
CREATE TABLE tms_route (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    route_no VARCHAR(128) UNIQUE,
    delivery_date DATE NOT NULL,
    courier_id UUID,
    waypoints JSONB,  -- 途经点 [{waybillId, address, lng, lat, seq}]
    total_distance DECIMAL(10, 2),
    optimization_algorithm VARCHAR(32),  -- TSP, GA, ACO
    ...
);
```

**亮点**:
- PostGIS扩展支持地理坐标计算
- 路径优化（应用层实现：遗传算法、蚁群算法）
- 支持第三方物流API对接（顺丰、京东物流等）

### 6. 供应商服务 (db_supplier)

#### sup_supplier - 供应商表
```sql
CREATE TABLE sup_supplier (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    supplier_code VARCHAR(64) UNIQUE,
    supplier_name VARCHAR(256) NOT NULL,
    supplier_type SMALLINT DEFAULT 1,  -- 1:生产商 2:贸易商 3:代理商
    credit_rating VARCHAR(2),  -- A+, A, B+, B, C, D
    quality_score DECIMAL(3, 1),
    delivery_score DECIMAL(3, 1),
    cooperation_status SMALLINT DEFAULT 1,  -- 0:潜在 1:合作中 2:暂停 3:终止
    ...
);
```

#### sup_purchase_order - 采购单表（分区）
```sql
CREATE TABLE sup_purchase_order (
    id UUID DEFAULT uuid_generate_v7(),
    purchase_no VARCHAR(128) UNIQUE,
    supplier_id UUID NOT NULL,
    total_amount DECIMAL(15, 2) NOT NULL,
    status SMALLINT DEFAULT 0,  -- 0:草稿 1:待审批 2:已审批 3:已发货 4:部分收货 5:已完成
    expected_delivery TIMESTAMPTZ,
    payment_status SMALLINT DEFAULT 0,  -- 0:未付款 1:部分付款 2:已付款
    ...
) PARTITION BY RANGE (create_time);
```

#### sup_supplier_evaluation - 供应商评价表
```sql
CREATE TABLE sup_supplier_evaluation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    supplier_id UUID NOT NULL,
    evaluation_period VARCHAR(32),  -- 2025Q1, 2025-01
    quality_score DECIMAL(3, 1),
    delivery_score DECIMAL(3, 1),
    on_time_delivery_rate DECIMAL(5, 2),  -- 准时交货率
    quality_pass_rate DECIMAL(5, 2),      -- 质量合格率
    ...
);
```

**亮点**:
- 供应商评价体系（质量、交期、服务、价格）
- 对账单自动生成（定时任务）
- 视图v_supplier_performance统计供应商绩效

## 跨库关联

由于微服务架构，跨库关联通过应用层维护（Dubbo RPC / Feign）：

### 关联关系

| 源表 | 外键字段 | 目标库.目标表 |
|------|----------|--------------|
| db_product.prod_sku | - | db_inventory.inv_inventory (同步库存) |
| db_order.ord_order | user_id | db_user.sys_user |
| db_order.ord_order | warehouse_id | db_warehouse.wms_warehouse |
| db_order.ord_order | reservation_id | db_inventory.inv_reservation |
| db_inventory.inv_inventory | sku_id | db_product.prod_sku |
| db_inventory.inv_inventory | warehouse_id | db_warehouse.wms_warehouse |
| db_warehouse.wms_outbound | order_id | db_order.ord_order |
| db_logistics.tms_waybill | order_id | db_order.ord_order |
| db_supplier.sup_purchase_order | supplier_id | db_supplier.sup_supplier |

### 数据同步策略

**实时同步** (Kafka事件驱动):
```
商品SKU库存变更 → Kafka → 订单服务更新可售库存
订单支付成功 → Kafka → 库存服务确认预占 → 仓储服务创建出库单
```

**定时同步** (XXL-Job):
- 每小时全量同步库存到商品服务（防止消息丢失）
- 每分钟扫描超时未支付订单并自动取消
- 每日生成库存快照

## 部署指南

### 1. 环境准备

```bash
# PostgreSQL 16+
sudo apt install postgresql-16 postgresql-contrib-16

# 扩展
sudo apt install postgresql-16-postgis-3  # GIS扩展（物流服务需要）
```

### 2. 一键初始化

```bash
cd scripts/db

# 设置环境变量
export PG_HOST=localhost
export PG_PORT=5432
export PG_ADMIN_USER=postgres
export PG_ADMIN_PASSWORD=your_password

# 执行初始化脚本
chmod +x init-all-databases.sh
./init-all-databases.sh
```

### 3. 手动初始化

```bash
# 1. 创建数据库
psql -U postgres -c "CREATE DATABASE db_product WITH ENCODING = 'UTF8';"
psql -U postgres -c "CREATE DATABASE db_inventory WITH ENCODING = 'UTF8';"
psql -U postgres -c "CREATE DATABASE db_order WITH ENCODING = 'UTF8';"
psql -U postgres -c "CREATE DATABASE db_warehouse WITH ENCODING = 'UTF8';"
psql -U postgres -c "CREATE DATABASE db_logistics WITH ENCODING = 'UTF8';"
psql -U postgres -c "CREATE DATABASE db_supplier WITH ENCODING = 'UTF8';"

# 2. 初始化表结构
psql -U postgres -d db_product -f microservices/010_db_product.sql
psql -U postgres -d db_inventory -f microservices/011_db_inventory.sql
psql -U postgres -d db_order -f microservices/012_db_order.sql
psql -U postgres -d db_warehouse -f microservices/013_db_warehouse.sql
psql -U postgres -d db_logistics -f microservices/014_db_logistics.sql
psql -U postgres -d db_supplier -f microservices/015_db_supplier.sql
```

### 4. 配置微服务

```yaml
# scm-product/service/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/db_product
    username: scm_product_user
    password: ${DB_PRODUCT_PASSWORD}
```

## 分区维护

### 自动创建下月分区（每月执行）

```sql
-- 订单分区
CREATE TABLE ord_order_2025_04 PARTITION OF ord_order
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

-- 库存预占分区
CREATE TABLE inv_reservation_2025_04 PARTITION OF inv_reservation
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

-- 库存日志分区
CREATE TABLE inv_log_2025_04 PARTITION OF inv_log
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');

-- 采购单分区
CREATE TABLE sup_purchase_order_2025_04 PARTITION OF sup_purchase_order
    FOR VALUES FROM ('2025-04-01') TO ('2025-05-01');
```

### 归档旧数据（保留策略：12个月）

```sql
-- 归档并删除12个月前的订单分区
ALTER TABLE ord_order DETACH PARTITION ord_order_2024_01;
-- 导出到归档存储
pg_dump -t ord_order_2024_01 ... > archive/ord_order_2024_01.sql
-- 删除分区
DROP TABLE ord_order_2024_01;
```

## 性能优化

### 索引策略
- 分区表的每个分区自动继承主表索引
- WHERE子句常用字段添加部分索引 `WHERE NOT deleted`
- JSONB字段使用GIN索引：`CREATE INDEX ... USING GIN(attributes)`
- 全文搜索使用pg_trgm：`CREATE INDEX ... USING gin (name gin_trgm_ops)`

### 查询优化
```sql
-- 利用分区裁剪
SELECT * FROM ord_order
WHERE create_time >= '2025-01-01' AND create_time < '2025-02-01'
  AND status = 0;
-- 只扫描ord_order_2025_01分区

-- 使用物化视图缓存复杂查询
CREATE MATERIALIZED VIEW mv_hot_products AS
SELECT ... FROM prod_spu ... ORDER BY total_sales DESC LIMIT 1000;
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_hot_products;  -- 定期刷新
```

## 监控与告警

### 关键指标

| 指标 | 阈值 | 说明 |
|------|------|------|
| 数据库连接数 | < 80% | 避免连接池耗尽 |
| 慢查询（>1s） | < 5/min | 及时优化SQL |
| 分区表大小 | < 10GB/分区 | 分区过大影响查询 |
| 死锁次数 | 0 | 检查事务逻辑 |
| 库存一致性 | 100% | 定期对账检查 |

### Prometheus监控示例

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'postgres'
    static_configs:
      - targets: ['postgres-exporter:9187']
```

## 故障恢复

### 备份策略
```bash
# 每日全量备份
pg_dumpall -U postgres | gzip > /backup/scm_$(date +%Y%m%d).sql.gz

# 单库备份
pg_dump -U postgres db_order | gzip > /backup/db_order_$(date +%Y%m%d).sql.gz
```

### 恢复
```bash
# 恢复整个数据库集群
gunzip < /backup/scm_20250101.sql.gz | psql -U postgres

# 恢复单个数据库
gunzip < /backup/db_order_20250101.sql.gz | psql -U postgres -d db_order
```

## 附录

### SQL脚本清单

| 脚本 | 数据库 | 说明 |
|------|--------|------|
| 010_db_product.sql | db_product | 商品服务（SPU/SKU/分类/品牌） |
| 011_db_inventory.sql | db_inventory | 库存服务（库存/预占/日志） |
| 012_db_order.sql | db_order | 订单服务（订单/支付/退款） |
| 013_db_warehouse.sql | db_warehouse | 仓储服务（仓库/出入库/波次） |
| 014_db_logistics.sql | db_logistics | 物流服务（运单/轨迹/路线） |
| 015_db_supplier.sql | db_supplier | 供应商服务（供应商/采购/对账） |

### 参考文档

- [PostgreSQL 16 文档](https://www.postgresql.org/docs/16/)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [Spring State Machine](https://spring.io/projects/spring-statemachine)
- [SCM_DESIGN_PLAN.md](../../docs/SCM_DESIGN_PLAN.md) - 技术设计方案
- [API_DESIGN.md](../../docs/technical/API_DESIGN.md) - API设计文档

---

**文档版本**: v1.0
**最后更新**: 2025-12-24
**维护团队**: SCM Platform Team