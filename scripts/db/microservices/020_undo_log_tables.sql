-- ============================================================
-- Seata Client UNDO_LOG 表初始化脚本
-- 每个业务数据库都需要创建此表用于 AT 模式回滚
-- ============================================================

-- 此脚本需要在每个业务数据库中执行

-- ============================================================
-- UNDO_LOG 表（回滚日志表）
-- ============================================================
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id BIGINT NOT NULL,
    xid VARCHAR(128) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info BYTEA NOT NULL,
    log_status INT NOT NULL,
    log_created TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    log_modified TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_undo_log PRIMARY KEY (branch_id, xid)
);

CREATE INDEX idx_undo_log_xid ON undo_log (xid);
CREATE INDEX idx_undo_log_log_created ON undo_log (log_created);

COMMENT ON TABLE undo_log IS 'Seata AT 模式回滚日志表';
COMMENT ON COLUMN undo_log.branch_id IS '分支事务ID';
COMMENT ON COLUMN undo_log.xid IS '全局事务ID';
COMMENT ON COLUMN undo_log.context IS '上下文信息';
COMMENT ON COLUMN undo_log.rollback_info IS '回滚数据快照';
COMMENT ON COLUMN undo_log.log_status IS '状态: 0-Normal, 1-Defensive';

-- ============================================================
-- 清理过期的 UNDO_LOG（可选，定期执行）
-- ============================================================
-- DELETE FROM undo_log WHERE log_created < NOW() - INTERVAL '7 days' AND log_status = 1;