-- ======================================================================
-- ClickHouse DWH Schema ODS/DWD/DWS/ADS (analytics)
-- V1 — creates 7 tables: ods_order, ods_inventory, dwd_sales_detail,
-- dws_sales_day, dws_inventory_day, ads_sales_dashboard, ads_inventory_health
-- Engines: MergeTree, SummingMergeTree, ReplacingMergeTree
-- Partitioning: toYYYYMM, ORDER BY starts with tenant_id
-- ======================================================================

CREATE DATABASE IF NOT EXISTS analytics;

-- ======================================================================
-- ODS (Operational Data Store) — raw CDC from PostgreSQL via Kafka
-- ======================================================================

-- 1. ODS order — raw order CDC
CREATE TABLE IF NOT EXISTS analytics.ods_order
(
    tenant_id    String,
    order_no     String,
    order_id     String,
    user_id      String,
    order_status String,
    total_amount Decimal(18, 2),
    pay_amount   Decimal(18, 2),
    pay_time     DateTime,
    order_source String,
    remark       String,
    create_time  DateTime,
    update_time  DateTime,
    kafka_offset UInt64,
    kafka_timestamp DateTime DEFAULT now(),
    _raw         String COMMENT 'original CDC JSON payload'
)
ENGINE = ReplacingMergeTree(update_time)
PARTITION BY toYYYYMM(create_time)
ORDER BY (tenant_id, order_no, create_time)
SETTINGS index_granularity = 8192;

-- 2. ODS inventory — raw inventory movement CDC
CREATE TABLE IF NOT EXISTS analytics.ods_inventory
(
    tenant_id    String,
    inventory_id String,
    sku_id       String,
    warehouse_id String,
    location_id  String,
    change_type  String COMMENT 'INBOUND/OUTBOUND/ADJUST/RESERVE/RELEASE',
    quantity     Int32,
    before_qty   Int32,
    after_qty    Int32,
    biz_no       String COMMENT 'source biz number (order_no/purchase_no etc.)',
    biz_type     String,
    create_time  DateTime,
    update_time  DateTime,
    kafka_offset UInt64,
    kafka_timestamp DateTime DEFAULT now(),
    _raw         String COMMENT 'original CDC JSON payload'
)
ENGINE = ReplacingMergeTree(update_time)
PARTITION BY toYYYYMM(create_time)
ORDER BY (tenant_id, sku_id, warehouse_id, create_time)
SETTINGS index_granularity = 8192;

-- ======================================================================
-- DWD (Data Warehouse Detail) — cleansed, standardized detail layer
-- ======================================================================

-- 3. DWD sales detail — cleansed fact derived from ods_order + order_item
CREATE TABLE IF NOT EXISTS analytics.dwd_sales_detail
(
    tenant_id       String,
    order_no        String,
    order_item_id   String,
    sku_id          String,
    product_name    String,
    category_id     String,
    category_name   String,
    quantity        Int32,
    unit_price      Decimal(18, 2),
    amount          Decimal(18, 2),
    discount_amount Decimal(18, 2),
    pay_amount      Decimal(18, 2),
    pay_time        DateTime,
    order_status    String,
    user_id         String,
    create_time     DateTime,
    update_time     DateTime
)
ENGINE = MergeTree
PARTITION BY toYYYYMM(pay_time)
ORDER BY (tenant_id, pay_time, order_no, sku_id)
SETTINGS index_granularity = 8192;

-- ======================================================================
-- DWS (Data Warehouse Summary) — aggregated by day, per tenant
-- ======================================================================

-- 4. DWS sales per day — daily sales aggregation
CREATE TABLE IF NOT EXISTS analytics.dws_sales_day
(
    tenant_id    String,
    stat_date    Date,
    category_id  String,
    sku_id       String,
    order_cnt    UInt32,
    sales_qty    Int64,
    sales_amount Decimal(18, 2),
    pay_amount   Decimal(18, 2),
    discount_amount Decimal(18, 2),
    create_time  DateTime DEFAULT now()
)
ENGINE = SummingMergeTree((order_cnt, sales_qty, sales_amount, pay_amount, discount_amount))
PARTITION BY toYYYYMM(stat_date)
ORDER BY (tenant_id, stat_date, category_id, sku_id)
SETTINGS index_granularity = 8192;

-- 5. DWS inventory per day — daily inventory snapshot / movement aggregation
CREATE TABLE IF NOT EXISTS analytics.dws_inventory_day
(
    tenant_id    String,
    stat_date    Date,
    warehouse_id String,
    sku_id       String,
    opening_qty  Int64,
    closing_qty  Int64,
    inbound_qty  Int64,
    outbound_qty Int64,
    adjust_qty   Int64,
    reserved_qty Int64,
    create_time  DateTime DEFAULT now()
)
ENGINE = SummingMergeTree((opening_qty, closing_qty, inbound_qty, outbound_qty, adjust_qty, reserved_qty))
PARTITION BY toYYYYMM(stat_date)
ORDER BY (tenant_id, stat_date, warehouse_id, sku_id)
SETTINGS index_granularity = 8192;

-- ======================================================================
-- ADS (Application Data Service) — serving layer for dashboards/APIs
-- ======================================================================

-- 6. ADS sales dashboard — pre-aggregated serving table for sales dashboard
CREATE TABLE IF NOT EXISTS analytics.ads_sales_dashboard
(
    tenant_id       String,
    stat_date       Date,
    total_orders    UInt64,
    total_gmv       Decimal(18, 2),
    total_pay       Decimal(18, 2),
    avg_order_value Decimal(18, 2),
    active_users    UInt32,
    new_users       UInt32,
    refund_amount   Decimal(18, 2),
    update_time     DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(update_time)
PARTITION BY toYYYYMM(stat_date)
ORDER BY (tenant_id, stat_date)
SETTINGS index_granularity = 8192;

-- 7. ADS inventory health — pre-aggregated serving table for inventory health
CREATE TABLE IF NOT EXISTS analytics.ads_inventory_health
(
    tenant_id     String,
    stat_date     Date,
    warehouse_id  String,
    sku_id        String,
    stock_qty     Int64,
    available_qty Int64,
    reserved_qty  Int64,
    turnover_days Decimal(10, 2),
    health_status String COMMENT 'HEALTHY/WARNING/CRITICAL',
    alert_level   String COMMENT 'NONE/LOW/MEDIUM/HIGH',
    stock_value   Decimal(18, 2),
    update_time   DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(update_time)
PARTITION BY toYYYYMM(stat_date)
ORDER BY (tenant_id, warehouse_id, sku_id, stat_date)
SETTINGS index_granularity = 8192;
