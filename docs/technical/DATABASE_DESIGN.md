# 数据库设计文档 (Database Design Specification)
# SCM Platform - Database Schema

> **文档类型**: 数据库设计规范
> **DBA**: 数据库架构师
> **目标用户**: 后端开发、DBA、数据分析
> **版本**: v1.0
> **最后更新**: 2025-12-24

---

## 一、数据库架构设计

### 1.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                   应用层（微服务）                        │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐           │
│  │ 商品   │ │ 库存   │ │ 订单   │ │ 仓储   │ ...        │
│  └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘           │
└──────┼──────────┼──────────┼──────────┼─────────────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
┌─────────────────────────────────────────────────────────┐
│                ShardingSphere 中间件层                   │
│  ┌────────────────────────────────────────────────┐     │
│  │  读写分离 + 分库分表 + 数据加密                  │     │
│  └────────────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────┘
       │
       ▼
┌─────────────────────────────────────────────────────────┐
│                   数据库集群层                           │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │ Master 0 │  │ Master 1 │  │ Master 2 │  ...        │
│  │ (写库)   │  │ (写库)   │  │ (写库)   │             │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘             │
│       │             │             │                     │
│  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐             │
│  │ Slave 0  │  │ Slave 1  │  │ Slave 2  │  ...        │
│  │ (读库)   │  │ (读库)   │  │ (读库)   │             │
│  └──────────┘  └──────────┘  └──────────┘             │
└─────────────────────────────────────────────────────────┘
```

### 1.2 数据库分配

| 服务 | 数据库名 | 分库策略 | 分表策略 |
|-----|---------|---------|---------|
| **商品服务** | scm_product | 单库 | 不分表 |
| **库存服务** | scm_inventory | 2个库 | 按 sku_id % 8 分表 |
| **订单服务** | scm_order | 2个库 | 按 user_id % 16 分表 |
| **仓储服务** | scm_warehouse | 单库 | 不分表 |
| **物流服务** | scm_logistics | 单库 | 不分表 |
| **采购服务** | scm_purchase | 单库 | 不分表 |
| **系统服务** | scm_system | 单库 | 不分表 |

### 1.3 技术选型

**主数据库**: PostgreSQL 14+
- 优势: 支持 JSONB、GIS、全文检索、MVCC
- 场景: 订单、库存、仓储等核心业务

**备选方案**: MySQL 8.0+
- 优势: 生态成熟、运维工具丰富
- 场景: 读多写少的业务

**数据一致性**:
- 主从复制: 流式复制（Streaming Replication）
- 延迟监控: < 100ms
- 自动切换: Patroni + HAProxy

---

## 二、核心表设计

### 2.1 商品表 (t_product)

**表名**: `scm_product.t_product`

**字段设计**:
```sql
CREATE TABLE t_product (
    -- 主键（UUIDv7）
    id                  BIGINT          PRIMARY KEY,

    -- 商品信息
    spu_code            VARCHAR(50)     NOT NULL UNIQUE COMMENT 'SPU编码',
    product_name        VARCHAR(200)    NOT NULL COMMENT '商品名称',
    category_id         BIGINT          NOT NULL COMMENT '类目ID',
    brand_id            BIGINT          COMMENT '品牌ID',

    -- 描述信息
    description         TEXT            COMMENT '商品描述',
    detail_html         TEXT            COMMENT '详情页HTML',
    images              JSONB           COMMENT '商品图片数组',
    video_url           VARCHAR(500)    COMMENT '商品视频',

    -- 属性（JSONB）
    attributes          JSONB           COMMENT '商品属性 {"color": ["黑色","白色"], "storage": ["128G","256G"]}',

    -- 状态
    status              SMALLINT        DEFAULT 1 COMMENT '状态: 1-上架 0-下架',

    -- 审计字段
    created_by          BIGINT          COMMENT '创建人',
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by          BIGINT          COMMENT '更新人',
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted             SMALLINT        DEFAULT 0 COMMENT '删除标记: 0-未删除 1-已删除'
);

-- 索引
CREATE INDEX idx_category_id ON t_product(category_id);
CREATE INDEX idx_brand_id ON t_product(brand_id);
CREATE INDEX idx_created_at ON t_product(created_at DESC);
CREATE INDEX idx_product_name_gin ON t_product USING GIN(to_tsvector('chinese', product_name));  -- 全文索引

-- 注释
COMMENT ON TABLE t_product IS '商品主表（SPU）';
```

### 2.2 SKU表 (t_sku)

**表名**: `scm_product.t_sku`

**字段设计**:
```sql
CREATE TABLE t_sku (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- 关联
    product_id          BIGINT          NOT NULL COMMENT '商品ID',
    sku_code            VARCHAR(50)     NOT NULL UNIQUE COMMENT 'SKU编码',

    -- SKU信息
    sku_name            VARCHAR(200)    NOT NULL COMMENT 'SKU名称',
    attributes          JSONB           NOT NULL COMMENT '规格属性 {"color":"黑色", "storage":"256G"}',

    -- 价格库存（冗余，实际库存在 scm_inventory 库）
    price               DECIMAL(10,2)   NOT NULL COMMENT '销售价格',
    original_price      DECIMAL(10,2)   COMMENT '原价',
    cost_price          DECIMAL(10,2)   COMMENT '成本价',
    stock               INT             DEFAULT 0 COMMENT '库存数量（冗余）',

    -- 物理属性
    weight              INT             COMMENT '重量（克）',
    volume              INT             COMMENT '体积（立方厘米）',

    -- 图片
    image               VARCHAR(500)    COMMENT 'SKU图片',

    -- 状态
    status              SMALLINT        DEFAULT 1 COMMENT '状态: 1-上架 0-下架',

    -- 审计字段
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT        DEFAULT 0
);

-- 索引
CREATE INDEX idx_product_id ON t_sku(product_id);
CREATE INDEX idx_price ON t_sku(price);

-- 外键
ALTER TABLE t_sku ADD CONSTRAINT fk_product_id
    FOREIGN KEY (product_id) REFERENCES t_product(id);

COMMENT ON TABLE t_sku IS 'SKU表（库存单位）';
```

### 2.3 库存表 (t_inventory) ⭐核心表

**表名**: `scm_inventory.t_inventory_{0..7}`（分表）

**字段设计**:
```sql
CREATE TABLE t_inventory (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- SKU信息
    sku_id              BIGINT          NOT NULL UNIQUE COMMENT 'SKU ID',

    -- 库存数量
    total_stock         INT             NOT NULL DEFAULT 0 COMMENT '总库存',
    available_stock     INT             NOT NULL DEFAULT 0 COMMENT '可用库存',
    reserved_stock      INT             NOT NULL DEFAULT 0 COMMENT '预占库存',
    locked_stock        INT             NOT NULL DEFAULT 0 COMMENT '锁定库存（待发货）',

    -- 预警配置
    safety_stock        INT             DEFAULT 100 COMMENT '安全库存',
    alert_stock         INT             DEFAULT 50 COMMENT '预警库存',

    -- 版本号（乐观锁）
    version             INT             NOT NULL DEFAULT 0 COMMENT '版本号',

    -- 审计字段
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,

    -- 约束: total_stock = available_stock + reserved_stock + locked_stock
    CONSTRAINT chk_stock CHECK (total_stock = available_stock + reserved_stock + locked_stock)
);

-- 索引
CREATE INDEX idx_sku_id ON t_inventory(sku_id);
CREATE INDEX idx_available_stock ON t_inventory(available_stock);

-- 注释
COMMENT ON TABLE t_inventory IS '库存主表';
COMMENT ON COLUMN t_inventory.available_stock IS '可用库存 = 总库存 - 预占 - 锁定';
```

**分表策略**:
```yaml
# ShardingSphere 配置
tables:
  t_inventory:
    actual-data-nodes: scm_inventory_${0..1}.t_inventory_${0..7}
    table-strategy:
      standard:
        sharding-column: sku_id
        sharding-algorithm-name: inventory-mod

sharding-algorithms:
  inventory-mod:
    type: MOD
    props:
      sharding-count: 8  # 8 张表
```

### 2.4 库存预占记录表 (t_stock_reservation)

**表名**: `scm_inventory.t_stock_reservation`

**字段设计**:
```sql
CREATE TABLE t_stock_reservation (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- 预占信息
    reservation_id      VARCHAR(50)     NOT NULL UNIQUE COMMENT '预占ID',
    sku_id              BIGINT          NOT NULL COMMENT 'SKU ID',
    quantity            INT             NOT NULL COMMENT '预占数量',

    -- 关联订单
    order_id            BIGINT          COMMENT '订单ID',
    user_id             BIGINT          NOT NULL COMMENT '用户ID',

    -- 状态
    status              VARCHAR(20)     NOT NULL COMMENT '状态: RESERVED-已预占 CONFIRMED-已确认 RELEASED-已释放',

    -- 时间
    expire_time         TIMESTAMP       NOT NULL COMMENT '过期时间',
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    confirmed_at        TIMESTAMP       COMMENT '确认时间',
    released_at         TIMESTAMP       COMMENT '释放时间'
);

-- 索引
CREATE INDEX idx_reservation_id ON t_stock_reservation(reservation_id);
CREATE INDEX idx_sku_id ON t_stock_reservation(sku_id);
CREATE INDEX idx_order_id ON t_stock_reservation(order_id);
CREATE INDEX idx_status_expire ON t_stock_reservation(status, expire_time);

COMMENT ON TABLE t_stock_reservation IS '库存预占记录表';
```

### 2.5 订单表 (t_order) ⭐核心表

**表名**: `scm_order.t_order_{0..15}`（分表）

**字段设计**:
```sql
CREATE TABLE t_order (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- 订单号
    order_no            VARCHAR(30)     NOT NULL UNIQUE COMMENT '订单号: ORD20251224103000001',

    -- 用户信息
    user_id             BIGINT          NOT NULL COMMENT '用户ID',

    -- 金额
    total_amount        DECIMAL(10,2)   NOT NULL COMMENT '订单总金额',
    paid_amount         DECIMAL(10,2)   COMMENT '实付金额',
    discount_amount     DECIMAL(10,2)   DEFAULT 0 COMMENT '优惠金额',
    shipping_fee        DECIMAL(10,2)   DEFAULT 0 COMMENT '运费',

    -- 收货地址
    receiver_name       VARCHAR(50)     NOT NULL COMMENT '收货人',
    receiver_phone      VARCHAR(20)     NOT NULL COMMENT '手机号',
    province            VARCHAR(50)     NOT NULL COMMENT '省',
    city                VARCHAR(50)     NOT NULL COMMENT '市',
    district            VARCHAR(50)     NOT NULL COMMENT '区',
    detail_address      VARCHAR(200)    NOT NULL COMMENT '详细地址',

    -- 支付信息
    payment_method      VARCHAR(20)     COMMENT '支付方式: ALIPAY, WECHAT, BANK_CARD',
    payment_time        TIMESTAMP       COMMENT '支付时间',
    payment_id          VARCHAR(50)     COMMENT '支付流水号',

    -- 物流信息
    waybill_no          VARCHAR(50)     COMMENT '运单号',
    carrier_code        VARCHAR(20)     COMMENT '物流公司编码',

    -- 订单状态
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING_PAYMENT' COMMENT '订单状态',

    -- 预占信息
    reservation_id      VARCHAR(50)     COMMENT '库存预占ID',

    -- 备注
    remark              VARCHAR(500)    COMMENT '用户备注',
    cancel_reason       VARCHAR(200)    COMMENT '取消原因',

    -- 时间
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    paid_at             TIMESTAMP       COMMENT '支付时间',
    shipped_at          TIMESTAMP       COMMENT '发货时间',
    delivered_at        TIMESTAMP       COMMENT '送达时间',
    completed_at        TIMESTAMP       COMMENT '完成时间',
    cancelled_at        TIMESTAMP       COMMENT '取消时间',

    -- 审计
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted             SMALLINT        DEFAULT 0
);

-- 索引
CREATE INDEX idx_order_no ON t_order(order_no);
CREATE INDEX idx_user_id ON t_order(user_id);
CREATE INDEX idx_status ON t_order(status);
CREATE INDEX idx_created_at ON t_order(created_at DESC);
CREATE INDEX idx_payment_time ON t_order(payment_time DESC);

COMMENT ON TABLE t_order IS '订单主表';
```

**分表策略**:
```yaml
tables:
  t_order:
    actual-data-nodes: scm_order_${0..1}.t_order_${0..15}
    database-strategy:
      standard:
        sharding-column: user_id
        sharding-algorithm-name: order-db-mod
    table-strategy:
      standard:
        sharding-column: user_id
        sharding-algorithm-name: order-table-mod

sharding-algorithms:
  order-db-mod:
    type: MOD
    props:
      sharding-count: 2  # 2 个库

  order-table-mod:
    type: INLINE
    props:
      algorithm-expression: t_order_${user_id % 16}  # 16 张表
```

### 2.6 订单明细表 (t_order_item)

**表名**: `scm_order.t_order_item`

**字段设计**:
```sql
CREATE TABLE t_order_item (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- 关联
    order_id            BIGINT          NOT NULL COMMENT '订单ID',
    sku_id              BIGINT          NOT NULL COMMENT 'SKU ID',

    -- 商品信息（冗余，避免 JOIN）
    sku_name            VARCHAR(200)    NOT NULL COMMENT 'SKU名称',
    sku_image           VARCHAR(500)    COMMENT 'SKU图片',
    sku_attributes      JSONB           COMMENT 'SKU属性',

    -- 价格数量
    price               DECIMAL(10,2)   NOT NULL COMMENT '单价',
    quantity            INT             NOT NULL COMMENT '数量',
    subtotal            DECIMAL(10,2)   NOT NULL COMMENT '小计',

    -- 审计
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_order_id ON t_order_item(order_id);
CREATE INDEX idx_sku_id ON t_order_item(sku_id);

-- 外键
ALTER TABLE t_order_item ADD CONSTRAINT fk_order_id
    FOREIGN KEY (order_id) REFERENCES t_order(id);

COMMENT ON TABLE t_order_item IS '订单明细表';
```

### 2.7 订单状态流转记录表 (t_order_state_log)

**表名**: `scm_order.t_order_state_log`

**字段设计**:
```sql
CREATE TABLE t_order_state_log (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- 关联
    order_id            BIGINT          NOT NULL COMMENT '订单ID',

    -- 状态流转
    previous_state      VARCHAR(30)     COMMENT '前状态',
    current_state       VARCHAR(30)     NOT NULL COMMENT '当前状态',
    event               VARCHAR(30)     NOT NULL COMMENT '触发事件',

    -- 附加信息
    description         VARCHAR(200)    COMMENT '描述',
    operator            VARCHAR(50)     COMMENT '操作人',

    -- 时间
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_order_id ON t_order_state_log(order_id);
CREATE INDEX idx_created_at ON t_order_state_log(created_at DESC);

COMMENT ON TABLE t_order_state_log IS '订单状态流转日志';
```

### 2.8 仓库表 (t_warehouse)

**表名**: `scm_warehouse.t_warehouse`

**字段设计**:
```sql
CREATE TABLE t_warehouse (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- 仓库信息
    warehouse_code      VARCHAR(20)     NOT NULL UNIQUE COMMENT '仓库编码',
    warehouse_name      VARCHAR(100)    NOT NULL COMMENT '仓库名称',
    warehouse_type      VARCHAR(20)     NOT NULL COMMENT '类型: CENTRAL-中心仓 REGIONAL-区域仓 FRONT-前置仓',

    -- 地址
    province            VARCHAR(50)     NOT NULL,
    city                VARCHAR(50)     NOT NULL,
    district            VARCHAR(50)     NOT NULL,
    address             VARCHAR(200)    NOT NULL,

    -- 地理位置（GIS）
    longitude           DECIMAL(10,6)   COMMENT '经度',
    latitude            DECIMAL(10,6)   COMMENT '纬度',
    geom                GEOGRAPHY(POINT, 4326) COMMENT '地理坐标（PostGIS）',

    -- 容量
    total_capacity      INT             COMMENT '总容量（立方米）',
    used_capacity       INT             DEFAULT 0 COMMENT '已用容量',

    -- 状态
    status              SMALLINT        DEFAULT 1 COMMENT '状态: 1-启用 0-停用',

    -- 审计
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 空间索引（PostGIS）
CREATE INDEX idx_warehouse_geom ON t_warehouse USING GIST(geom);

COMMENT ON TABLE t_warehouse IS '仓库主表';
```

### 2.9 库位表 (t_warehouse_location)

**表名**: `scm_warehouse.t_warehouse_location`

**字段设计**:
```sql
CREATE TABLE t_warehouse_location (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- 关联
    warehouse_id        BIGINT          NOT NULL COMMENT '仓库ID',

    -- 库位信息
    location_code       VARCHAR(20)     NOT NULL COMMENT '库位编码: A01-01-01',
    location_type       VARCHAR(20)     NOT NULL COMMENT '类型: SHELF-货架 FLOOR-地面 COLD-冷库',

    -- 位置（区-排-列）
    zone                VARCHAR(10)     NOT NULL COMMENT '区',
    row_no              VARCHAR(10)     NOT NULL COMMENT '排',
    column_no           VARCHAR(10)     NOT NULL COMMENT '列',

    -- 容量
    capacity            INT             COMMENT '容量（立方厘米）',
    used_capacity       INT             DEFAULT 0,

    -- 状态
    status              SMALLINT        DEFAULT 1 COMMENT '状态: 1-空闲 2-占用 0-禁用',

    -- 当前存储的SKU
    current_sku_id      BIGINT          COMMENT '当前SKU ID',
    current_quantity    INT             DEFAULT 0,

    -- 审计
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE UNIQUE INDEX uk_warehouse_location ON t_warehouse_location(warehouse_id, location_code);
CREATE INDEX idx_warehouse_id ON t_warehouse_location(warehouse_id);
CREATE INDEX idx_status ON t_warehouse_location(status);

COMMENT ON TABLE t_warehouse_location IS '库位表';
```

### 2.10 物流运单表 (t_waybill)

**表名**: `scm_logistics.t_waybill`

**字段设计**:
```sql
CREATE TABLE t_waybill (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- 运单号
    waybill_no          VARCHAR(50)     NOT NULL UNIQUE COMMENT '运单号',

    -- 关联
    order_id            BIGINT          NOT NULL COMMENT '订单ID',
    warehouse_id        BIGINT          NOT NULL COMMENT '发货仓库ID',

    -- 物流公司
    carrier_code        VARCHAR(20)     NOT NULL COMMENT '承运商编码: SF-顺丰 JD-京东',
    carrier_name        VARCHAR(50)     NOT NULL COMMENT '承运商名称',

    -- 收货地址
    receiver_name       VARCHAR(50)     NOT NULL,
    receiver_phone      VARCHAR(20)     NOT NULL,
    receiver_address    VARCHAR(500)    NOT NULL,
    receiver_longitude  DECIMAL(10,6),
    receiver_latitude   DECIMAL(10,6),

    -- 状态
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待揽收 PICKED_UP-已揽收 IN_TRANSIT-运输中 DELIVERED-已送达',

    -- 时间
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    picked_up_at        TIMESTAMP       COMMENT '揽收时间',
    delivered_at        TIMESTAMP       COMMENT '送达时间',
    estimated_arrival   TIMESTAMP       COMMENT '预计送达时间',

    -- 审计
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_waybill_no ON t_waybill(waybill_no);
CREATE INDEX idx_order_id ON t_waybill(order_id);
CREATE INDEX idx_status ON t_waybill(status);

COMMENT ON TABLE t_waybill IS '物流运单表';
```

### 2.11 物流轨迹表 (t_waybill_track)

**表名**: `scm_logistics.t_waybill_track`

**字段设计**:
```sql
CREATE TABLE t_waybill_track (
    -- 主键
    id                  BIGINT          PRIMARY KEY,

    -- 关联
    waybill_no          VARCHAR(50)     NOT NULL COMMENT '运单号',

    -- 轨迹信息
    track_time          TIMESTAMP       NOT NULL COMMENT '轨迹时间',
    location            VARCHAR(100)    COMMENT '位置',
    description         VARCHAR(200)    NOT NULL COMMENT '描述',

    -- 地理位置
    longitude           DECIMAL(10,6),
    latitude            DECIMAL(10,6),

    -- 操作人
    operator            VARCHAR(50)     COMMENT '操作人（配送员）',

    -- 审计
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_waybill_no ON t_waybill_track(waybill_no);
CREATE INDEX idx_track_time ON t_waybill_track(track_time DESC);

COMMENT ON TABLE t_waybill_track IS '物流轨迹表';
```

---

## 三、性能优化

### 3.1 索引策略

**索引类型**:
```sql
-- B-Tree 索引（默认）
CREATE INDEX idx_user_id ON t_order(user_id);

-- GIN 索引（JSONB 字段）
CREATE INDEX idx_attributes_gin ON t_product USING GIN(attributes);

-- GiST 索引（全文检索）
CREATE INDEX idx_product_name_gist ON t_product USING GIST(to_tsvector('chinese', product_name));

-- 空间索引（PostGIS）
CREATE INDEX idx_warehouse_geom ON t_warehouse USING GIST(geom);

-- 部分索引（过滤条件）
CREATE INDEX idx_active_products ON t_product(id) WHERE deleted = 0;

-- 覆盖索引（Include）
CREATE INDEX idx_order_user_amount ON t_order(user_id) INCLUDE (total_amount, created_at);
```

**索引监控**:
```sql
-- 查看索引使用情况
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'public'
ORDER BY idx_scan;

-- 查看未使用的索引
SELECT schemaname, tablename, indexname
FROM pg_stat_user_indexes
WHERE idx_scan = 0 AND schemaname = 'public';
```

### 3.2 分区表

**订单归档表**（按时间分区）:
```sql
-- 创建分区表
CREATE TABLE t_order_archive (
    id BIGINT,
    order_no VARCHAR(30),
    user_id BIGINT,
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP,
    ...
) PARTITION BY RANGE (created_at);

-- 创建分区（按月）
CREATE TABLE t_order_archive_2025_01 PARTITION OF t_order_archive
    FOR VALUES FROM ('2025-01-01') TO ('2025-02-01');

CREATE TABLE t_order_archive_2025_02 PARTITION OF t_order_archive
    FOR VALUES FROM ('2025-02-01') TO ('2025-03-01');

-- 自动创建分区（扩展插件）
CREATE EXTENSION pg_partman;
```

### 3.3 物化视图

**热销商品统计**:
```sql
CREATE MATERIALIZED VIEW mv_hot_products AS
SELECT
    p.id AS product_id,
    p.product_name,
    COUNT(oi.id) AS order_count,
    SUM(oi.quantity) AS total_sales,
    SUM(oi.subtotal) AS total_revenue
FROM t_product p
INNER JOIN t_sku s ON p.id = s.product_id
INNER JOIN t_order_item oi ON s.id = oi.sku_id
INNER JOIN t_order o ON oi.order_id = o.id
WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED', 'COMPLETED')
  AND o.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY p.id, p.product_name
ORDER BY total_sales DESC;

-- 创建索引
CREATE INDEX idx_mv_hot_products_sales ON mv_hot_products(total_sales DESC);

-- 定时刷新（每小时）
REFRESH MATERIALIZED VIEW CONCURRENTLY mv_hot_products;
```

---

## 四、数据安全

### 4.1 敏感数据加密

**字段级加密** (AES-256):
```sql
-- 创建加密扩展
CREATE EXTENSION pgcrypto;

-- 加密手机号
INSERT INTO t_order (receiver_phone, ...)
VALUES (
    encode(encrypt('13800138000', 'encryption_key', 'aes'), 'hex'),
    ...
);

-- 解密查询
SELECT
    order_no,
    convert_from(decrypt(decode(receiver_phone, 'hex'), 'encryption_key', 'aes'), 'utf-8') AS phone
FROM t_order;
```

### 4.2 行级安全策略 (RLS)

**多租户数据隔离**:
```sql
-- 启用 RLS
ALTER TABLE t_order ENABLE ROW LEVEL SECURITY;

-- 创建策略（只能查看自己的订单）
CREATE POLICY order_isolation_policy ON t_order
    USING (user_id = current_setting('app.current_user_id')::BIGINT);

-- 应用层设置用户ID
SET app.current_user_id = 12345;

-- 自动过滤
SELECT * FROM t_order;  -- 只返回 user_id = 12345 的订单
```

### 4.3 审计日志

**审计表** (t_audit_log):
```sql
CREATE TABLE t_audit_log (
    id                  BIGINT          PRIMARY KEY,
    table_name          VARCHAR(50)     NOT NULL,
    operation           VARCHAR(10)     NOT NULL COMMENT 'INSERT, UPDATE, DELETE',
    record_id           BIGINT          NOT NULL,
    old_value           JSONB,
    new_value           JSONB,
    operator_id         BIGINT,
    operator_ip         VARCHAR(50),
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

-- 触发器（自动记录修改）
CREATE OR REPLACE FUNCTION audit_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'UPDATE') THEN
        INSERT INTO t_audit_log (table_name, operation, record_id, old_value, new_value)
        VALUES (TG_TABLE_NAME, TG_OP, NEW.id, row_to_json(OLD), row_to_json(NEW));
    ELSIF (TG_OP = 'DELETE') THEN
        INSERT INTO t_audit_log (table_name, operation, record_id, old_value)
        VALUES (TG_TABLE_NAME, TG_OP, OLD.id, row_to_json(OLD));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 绑定触发器
CREATE TRIGGER audit_order_changes
AFTER UPDATE OR DELETE ON t_order
FOR EACH ROW EXECUTE FUNCTION audit_trigger_func();
```

---

## 五、数据库运维

### 5.1 备份策略

**全量备份**:
```bash
# pg_dump 备份
pg_dump -h localhost -U postgres -d scm_order -F c -f /backup/scm_order_full_$(date +%Y%m%d).dump

# 保留最近 7 天的备份
find /backup -name "scm_order_full_*.dump" -mtime +7 -delete
```

**增量备份** (WAL归档):
```ini
# postgresql.conf
wal_level = replica
archive_mode = on
archive_command = 'cp %p /backup/wal/%f'
```

**PITR (Point-In-Time Recovery)**:
```bash
# 恢复到指定时间点
pg_basebackup -h master -D /data/recovery -P -X stream
# 修改 recovery.conf
restore_command = 'cp /backup/wal/%f %p'
recovery_target_time = '2025-12-24 10:30:00'
```

### 5.2 监控指标

**Prometheus 监控**:
```yaml
# postgresql_exporter 指标
- pg_stat_database_tup_inserted    # 插入行数
- pg_stat_database_tup_updated     # 更新行数
- pg_stat_database_tup_deleted     # 删除行数
- pg_stat_database_blks_hit        # 缓存命中
- pg_stat_database_blks_read       # 磁盘读取
- pg_stat_replication_lag          # 主从延迟
```

**慢查询监控**:
```sql
-- 启用慢查询日志
ALTER SYSTEM SET log_min_duration_statement = 1000;  -- 1秒

-- 查询慢SQL
SELECT
    query,
    mean_exec_time,
    calls
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

---

## 六、附录

### 6.1 ER 图

```
                     ┌──────────────┐
                     │  t_product   │
                     │  (商品主表)   │
                     └──────┬───────┘
                            │ 1
                            │
                            │ N
                     ┌──────┴───────┐
                     │    t_sku     │
                     │  (SKU表)     │
                     └──────┬───────┘
                            │ 1
                            │
                            │ 1
                     ┌──────┴────────┐
                     │ t_inventory   │
                     │  (库存表)     │
                     └───────────────┘

┌────────────┐         ┌────────────┐
│  t_order   │1       N│t_order_item│
│  (订单表)  ├─────────┤ (订单明细) │
└─────┬──────┘         └────────────┘
      │ 1
      │
      │ 1
┌─────┴──────┐
│ t_waybill  │
│ (运单表)   │
└─────┬──────┘
      │ 1
      │
      │ N
┌─────┴────────┐
│t_waybill_track│
│ (物流轨迹)    │
└──────────────┘
```

### 6.2 初始化脚本

**位置**: `scripts/db/init_database.sql`

```sql
-- 创建数据库
CREATE DATABASE scm_product OWNER scm_user;
CREATE DATABASE scm_inventory OWNER scm_user;
CREATE DATABASE scm_order OWNER scm_user;
CREATE DATABASE scm_warehouse OWNER scm_user;
CREATE DATABASE scm_logistics OWNER scm_user;

-- 创建用户
CREATE USER scm_user WITH PASSWORD 'scm_password';

-- 授权
GRANT ALL PRIVILEGES ON DATABASE scm_product TO scm_user;
GRANT ALL PRIVILEGES ON DATABASE scm_inventory TO scm_user;
GRANT ALL PRIVILEGES ON DATABASE scm_order TO scm_user;
GRANT ALL PRIVILEGES ON DATABASE scm_warehouse TO scm_user;
GRANT ALL PRIVILEGES ON DATABASE scm_logistics TO scm_user;

-- 安装扩展
\c scm_warehouse
CREATE EXTENSION postgis;  -- 空间数据
CREATE EXTENSION pg_trgm;  -- 模糊搜索
CREATE EXTENSION pgcrypto; -- 加密
```

---

**文档维护**: DBA团队
**审批**: 技术总监
**版本**: v1.0
**最后更新**: 2025-12-24