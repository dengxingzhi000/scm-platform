-- ClickHouse init for dev (docker-entrypoint-initdb.d)
-- Creates analytics database and ODS/DWD/DWS/ADS schemas.
-- Production schema is managed via scm-analytics/service/src/main/resources/clickhouse/V1__ods_dwd_dws_ads.sql
-- This file is mounted into /docker-entrypoint-initdb.d/init.sql for local dev auto-init.

CREATE DATABASE IF NOT EXISTS analytics;

-- ODS order
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

-- ODS inventory
CREATE TABLE IF NOT EXISTS analytics.ods_inventory
(
    tenant_id    String,
    inventory_id String,
    sku_id       String,
    warehouse_id String,
    location_id  String,
    change_type  String,
    quantity     Int32,
    before_qty   Int32,
    after_qty    Int32,
    biz_no       String,
    biz_type     String,
    create_time  DateTime,
    update_time  DateTime,
    kafka_offset UInt64,
    kafka_timestamp DateTime DEFAULT now(),
    _raw         String
)
ENGINE = ReplacingMergeTree(update_time)
PARTITION BY toYYYYMM(create_time)
ORDER BY (tenant_id, sku_id, warehouse_id, create_time)
SETTINGS index_granularity = 8192;

-- DWD sales detail
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

-- DWS sales day
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

-- DWS inventory day
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

-- ADS sales dashboard
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

-- ADS inventory health
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
    health_status String,
    alert_level   String,
    stock_value   Decimal(18, 2),
    update_time   DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(update_time)
PARTITION BY toYYYYMM(stat_date)
ORDER BY (tenant_id, warehouse_id, sku_id, stat_date)
SETTINGS index_granularity = 8192;
