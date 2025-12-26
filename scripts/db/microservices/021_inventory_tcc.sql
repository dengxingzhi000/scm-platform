-- ============================================================
-- 库存 TCC 预留记录表
-- 用于 Seata TCC 模式的库存预留管理
-- ============================================================

\c db_inventory

-- ============================================================
-- 库存预留记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS inv_tcc_reservation (
    id BIGSERIAL PRIMARY KEY,

    -- 业务信息
    business_key VARCHAR(128) NOT NULL UNIQUE COMMENT '业务键（订单号等），用于幂等性',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    quantity INTEGER NOT NULL COMMENT '预留数量',

    -- TCC 事务信息
    xid VARCHAR(128) NOT NULL COMMENT 'Seata 全局事务 ID',
    branch_id BIGINT NOT NULL COMMENT 'Seata 分支事务 ID',

    -- 状态信息
    status VARCHAR(20) NOT NULL DEFAULT 'TRYING' COMMENT '状态: TRYING, CONFIRMED, CANCELLED',

    -- 时间信息
    try_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP COMMENT 'Try 阶段时间',
    confirm_time TIMESTAMPTZ COMMENT 'Confirm 阶段时间',
    cancel_time TIMESTAMPTZ COMMENT 'Cancel 阶段时间',

    -- 创建和更新时间
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_inv_tcc_business_key ON inv_tcc_reservation(business_key);
CREATE INDEX idx_inv_tcc_xid ON inv_tcc_reservation(xid);
CREATE INDEX idx_inv_tcc_status ON inv_tcc_reservation(status);
CREATE INDEX idx_inv_tcc_try_time ON inv_tcc_reservation(try_time);

-- 注释
COMMENT ON TABLE inv_tcc_reservation IS '库存 TCC 预留记录表';
COMMENT ON COLUMN inv_tcc_reservation.business_key IS '业务键（订单号），用于幂等性控制';
COMMENT ON COLUMN inv_tcc_reservation.sku_id IS 'SKU ID';
COMMENT ON COLUMN inv_tcc_reservation.quantity IS '预留数量';
COMMENT ON COLUMN inv_tcc_reservation.xid IS 'Seata 全局事务 ID';
COMMENT ON COLUMN inv_tcc_reservation.branch_id IS 'Seata 分支事务 ID';
COMMENT ON COLUMN inv_tcc_reservation.status IS '状态: TRYING-预留中, CONFIRMED-已确认, CANCELLED-已取消';
COMMENT ON COLUMN inv_tcc_reservation.try_time IS 'Try 阶段执行时间';
COMMENT ON COLUMN inv_tcc_reservation.confirm_time IS 'Confirm 阶段执行时间';
COMMENT ON COLUMN inv_tcc_reservation.cancel_time IS 'Cancel 阶段执行时间';

-- ============================================================
-- 清理过期的 TCC 预留记录（保留 7 天）
-- ============================================================
-- DELETE FROM inv_tcc_reservation
-- WHERE (status = 'CONFIRMED' OR status = 'CANCELLED')
--   AND update_time < NOW() - INTERVAL '7 days';