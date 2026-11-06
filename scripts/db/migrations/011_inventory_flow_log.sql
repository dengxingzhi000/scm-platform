-- ======================================================================
-- 011 — 库存流水 inv_log：业务字段、前后四元组、幂等去重
--
-- 背景
--   * 011_db_inventory.sql 建了 inv_log，但除手工 SQL 示例外没有任何写入点；
--     列只有 quantity/before_stock/after_stock，无法回答“这次变更前后
--     available/locked/damaged 各是多少、由谁在什么业务单据下触发”。
--   * 月分区只建到 2025-03，当前时间写入会报
--     “no partition of relation inv_log found for row”。
--   * 回调/重试链路（TCC Confirm、Dubbo requestId）需要“同一业务动作只写一条”。
--
-- 设计
--   1. 只加列、放开两列 NOT NULL（Redis 路径的非 UUID SKU / 伪仓库需要），
--      不改既有索引与约束语义。
--   2. 补 DEFAULT 分区承接当前及未来写入；历史分区不重建。
--   3. 独立去重表 inv_log_idempotency，唯一键 (tenant_id, idempotency_key)。
--      分区表无法建不含分区键的全局唯一索引，因此去重放在普通表。
--      去重记录与流水在同一个业务事务内插入，业务失败会一起回滚，
--      重试时仍能重新占用键。
--
-- 幂等：全部 IF NOT EXISTS / 存在性判断，可重复执行；无 DELETE/TRUNCATE。
-- 目标库：db_inventory
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. inv_log 业务字段 + 变更前后四元组
-- ----------------------------------------------------------------------
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS business_type VARCHAR(32);
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS reason VARCHAR(512);
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS business_key VARCHAR(128);
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS request_id VARCHAR(128);

ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS before_total INT;
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS before_available INT;
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS before_locked INT;
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS before_damaged INT;
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS after_total INT;
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS after_available INT;
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS after_locked INT;
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS after_damaged INT;

-- 账本来源：DB=以 inv_inventory 为准；REDIS=以 Redis 扣减账本为准（Dubbo 路径）
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS ledger_source VARCHAR(16) NOT NULL DEFAULT 'DB';
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS legacy_sku_id BIGINT;
ALTER TABLE inv_log ADD COLUMN IF NOT EXISTS warehouse_code VARCHAR(64);

COMMENT ON COLUMN inv_log.business_type IS '业务动作：ADJUST/INIT/TRANSFER_OUT/TRANSFER_IN/RESERVE/CONFIRM/RELEASE/RELEASE_EXPIRED/TCC_TRY/TCC_CONFIRM/TCC_CANCEL/DUBBO_DEDUCT';
COMMENT ON COLUMN inv_log.reason IS '变更原因（业务备注）';
COMMENT ON COLUMN inv_log.business_key IS '业务单据号（订单号等），与 tenant_id 一起定位';
COMMENT ON COLUMN inv_log.request_id IS '调用方请求号，用于重复回调去重';
COMMENT ON COLUMN inv_log.before_total IS '变更前 total_stock';
COMMENT ON COLUMN inv_log.before_available IS '变更前 available_stock';
COMMENT ON COLUMN inv_log.before_locked IS '变更前 locked_stock';
COMMENT ON COLUMN inv_log.before_damaged IS '变更前 damaged_stock';
COMMENT ON COLUMN inv_log.after_total IS '变更后 total_stock';
COMMENT ON COLUMN inv_log.after_available IS '变更后 available_stock';
COMMENT ON COLUMN inv_log.after_locked IS '变更后 locked_stock';
COMMENT ON COLUMN inv_log.after_damaged IS '变更后 damaged_stock';
COMMENT ON COLUMN inv_log.ledger_source IS '数量口径来源：DB 或 REDIS';
COMMENT ON COLUMN inv_log.legacy_sku_id IS '兼容 scm-order 的数字 SKU；Redis 路径可能只有该列';
COMMENT ON COLUMN inv_log.warehouse_code IS '非 UUID 仓库编码（Redis 路径的 DEFAULT 等）';

-- Redis 路径的 skuId 可能是 Long（非 UUID）、warehouseId 可能是编码（非 UUID）。
-- 放开 NOT NULL 不改变已有数据，DB 路径仍会写 UUID。
ALTER TABLE inv_log ALTER COLUMN sku_id DROP NOT NULL;
ALTER TABLE inv_log ALTER COLUMN warehouse_id DROP NOT NULL;

-- ----------------------------------------------------------------------
-- 2. 承接当前与未来写入的分区
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inv_log_default PARTITION OF inv_log DEFAULT;

CREATE INDEX IF NOT EXISTS idx_log_tenant_time ON inv_log(tenant_id, create_time DESC);
CREATE INDEX IF NOT EXISTS idx_log_business_key ON inv_log(business_key);

-- ----------------------------------------------------------------------
-- 3. 流水幂等去重表
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inv_log_idempotency (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    idempotency_key VARCHAR(256) NOT NULL,
    log_id UUID,
    create_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_inv_log_idempotency
    ON inv_log_idempotency(tenant_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_inv_log_idempotency_time
    ON inv_log_idempotency(create_time);

COMMENT ON TABLE inv_log_idempotency IS '流水去重键：(tenant_id, idempotency_key) 唯一；与业务事务同提交/回滚';
COMMENT ON COLUMN inv_log_idempotency.log_id IS '首次写入的 inv_log.id';
