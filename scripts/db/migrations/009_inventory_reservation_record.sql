-- ======================================================================
-- 009 — 库存预占持久化表（REST 预占链路）
--
-- 背景
--   REST 预占链路（/api/v1/inventory/reservation/*）此前只把预占记录写在
--   Redis 里，Redis TTL 到期后记录消失，locked_stock 却不会归还，
--   release-expired 又是空实现；重复 confirm/release 也无法区分
--   “已确认”与“不存在”。本表把预占落成持久化状态机：
--
--     RESERVED --confirm--> CONFIRMED（不可逆）
--     RESERVED --release--> RELEASED（不可逆）
--     RESERVED --expire---> EXPIRED （不可逆）
--
--   幂等键为 (tenant_id, business_key)：同一租户同一业务键只能有一条记录，
--   重复预占不会二次扣减；不同租户可以使用相同业务键。
--
-- 与既有的 inv_reservation 分区表的关系
--   inv_reservation 是 011_db_inventory.sql 里的历史分区表（order_id 必填，
--   分区只覆盖 2025 年），当前代码没有任何写入方。为避免改动既有表结构与
--   分区策略，这里新增独立表，后续批次再评估合并。
--
-- 幂等性
--   全部 DDL 使用 IF NOT EXISTS，可重复执行；不包含任何 DELETE/TRUNCATE。
-- ======================================================================

CREATE TABLE IF NOT EXISTS inv_reservation_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    tenant_id UUID NOT NULL,
    business_key VARCHAR(128) NOT NULL,

    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    -- 指向 inv_inventory.id，扣减/归还只按这一行做条件更新
    inventory_id UUID NOT NULL,

    quantity INT NOT NULL
        CONSTRAINT chk_inv_reservation_record_quantity CHECK (quantity > 0),

    status VARCHAR(16) NOT NULL DEFAULT 'RESERVED'
        CONSTRAINT chk_inv_reservation_record_status
        CHECK (status IN ('RESERVED', 'CONFIRMED', 'RELEASED', 'EXPIRED')),

    expire_at TIMESTAMP NOT NULL,
    reserved_at TIMESTAMP NOT NULL DEFAULT NOW(),
    confirmed_at TIMESTAMP,
    released_at TIMESTAMP,

    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_inv_reservation_record_tenant_business UNIQUE (tenant_id, business_key)
);

-- 过期扫描只关心仍然处于 RESERVED 的记录
CREATE INDEX IF NOT EXISTS idx_inv_reservation_record_expire
    ON inv_reservation_record(expire_at) WHERE status = 'RESERVED';

CREATE INDEX IF NOT EXISTS idx_inv_reservation_record_tenant_sku
    ON inv_reservation_record(tenant_id, sku_id, warehouse_id);

COMMENT ON TABLE inv_reservation_record IS 'REST 库存预占记录（持久化状态机）';
COMMENT ON COLUMN inv_reservation_record.status IS '状态: RESERVED/CONFIRMED/RELEASED/EXPIRED';
COMMENT ON COLUMN inv_reservation_record.expire_at IS '预占过期时间，过期后由定时任务释放';
COMMENT ON COLUMN inv_reservation_record.inventory_id IS '对应 inv_inventory.id，库存条件更新依据';
