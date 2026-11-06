-- ======================================================================
-- 010 — TCC 预留精确到台账行 + 租户级幂等键
--
-- 背景
--   021_inventory_tcc.sql 里的 inv_tcc_reservation 存在三个问题：
--
--   1. 只按 sku_id 扣减，而 inv_inventory 的唯一键是 (sku_id, warehouse_id)，
--      同一 SKU 在多仓时会把锁定库存扣到错误的行；且 inv_inventory.sku_id 是
--      UUID、预留表 sku_id 是 BIGINT，两者无法直接比较。
--   2. business_key 是全局 UNIQUE，不同租户的同名单号无法各自预占。
--   3. 没有 inventory_id，Confirm/Cancel 无法精确落到 inv_inventory 的具体行。
--
-- 兼容策略（跨 scm-order）
--   - scm-order 目前仍以 Long skuId 调用 reserveInventory，本批次不改 order 代码。
--   - 预留表新增 sku_uuid UUID，与台账类型对齐；旧 sku_id BIGINT 保留做结转期的
--     兼容字段，新写入同时落 sku_uuid（能解析到台账时）。
--   - inv_inventory 新增 legacy_sku_id BIGINT，仅作为“数字 SKU”到台账行的桥。
--     需要数据侧回填该列，或 scm-order 迁移到 UUID 版 TCC 接口后即可不再使用。
--     该列可为空、不参与既有索引，属于只加不改。
--
-- 幂等性
--   全部使用 IF NOT EXISTS / 存在性判断，可重复执行；无 DELETE/TRUNCATE。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. inv_tcc_reservation：精确台账定位 + 租户级业务键
-- ----------------------------------------------------------------------
ALTER TABLE inv_tcc_reservation
    ADD COLUMN IF NOT EXISTS inventory_id UUID;

ALTER TABLE inv_tcc_reservation
    ADD COLUMN IF NOT EXISTS warehouse_id UUID;

ALTER TABLE inv_tcc_reservation
    ADD COLUMN IF NOT EXISTS sku_uuid UUID;

COMMENT ON COLUMN inv_tcc_reservation.inventory_id IS '对应 inv_inventory.id，Confirm/Cancel 的精确扣减依据';
COMMENT ON COLUMN inv_tcc_reservation.warehouse_id IS '仓库 ID，多仓场景下与 sku_uuid 一起定位台账行';
COMMENT ON COLUMN inv_tcc_reservation.sku_uuid IS 'SKU UUID，与 inv_inventory.sku_id 类型对齐（旧 sku_id BIGINT 为兼容字段）';

-- 旧的全局唯一约束（列级 UNIQUE 由 PostgreSQL 自动命名为 <table>_<column>_key）
DO $$
DECLARE
    constraint_name text;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'inv_tcc_reservation'::regclass
          AND contype = 'u'
          AND conname = 'inv_tcc_reservation_business_key_key'
    LOOP
        EXECUTE format('ALTER TABLE inv_tcc_reservation DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

-- 幂等键改为租户级：不同租户可以使用相同的业务键
CREATE UNIQUE INDEX IF NOT EXISTS uk_inv_tcc_tenant_business_key
    ON inv_tcc_reservation(tenant_id, business_key);

CREATE INDEX IF NOT EXISTS idx_inv_tcc_inventory_id
    ON inv_tcc_reservation(inventory_id);

COMMENT ON COLUMN inv_tcc_reservation.business_key IS '业务键（订单号），与 tenant_id 组合唯一';

-- ----------------------------------------------------------------------
-- 2. inv_inventory：数字 SKU 兼容桥（只加列，不改既有索引/约束）
-- ----------------------------------------------------------------------
ALTER TABLE inv_inventory
    ADD COLUMN IF NOT EXISTS legacy_sku_id BIGINT;

COMMENT ON COLUMN inv_inventory.legacy_sku_id IS '兼容 scm-order 的 Long skuId，需数据侧回填；新链路请直接用 sku_id(UUID)';

-- 同一租户同一仓库下，兼容 ID 唯一；为空的历史行不参与约束
CREATE UNIQUE INDEX IF NOT EXISTS uk_inv_tenant_legacy_sku_warehouse
    ON inv_inventory(tenant_id, legacy_sku_id, warehouse_id)
    WHERE legacy_sku_id IS NOT NULL AND NOT deleted;
