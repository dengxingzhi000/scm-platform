-- ============================================================
-- Seata Server 数据库初始化脚本
-- 用于 AT 模式的分布式事务管理
-- ============================================================

-- 创建 Seata 数据库
CREATE DATABASE IF NOT EXISTS seata WITH ENCODING = 'UTF8';

\c seata;

-- ============================================================
-- 全局事务表
-- ============================================================
CREATE TABLE IF NOT EXISTS global_table (
    xid VARCHAR(128) NOT NULL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    application_id VARCHAR(128) NOT NULL,
    transaction_service_group VARCHAR(128) NOT NULL,
    transaction_name VARCHAR(128),
    timeout INT NOT NULL,
    begin_time BIGINT NOT NULL,
    application_data VARCHAR(2000),
    gmt_create TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_gmt_modified_status ON global_table (gmt_modified, status);
CREATE INDEX idx_transaction_id ON global_table (transaction_id);

COMMENT ON TABLE global_table IS 'Seata 全局事务表';
COMMENT ON COLUMN global_table.xid IS '全局事务ID';
COMMENT ON COLUMN global_table.transaction_id IS '事务ID';
COMMENT ON COLUMN global_table.status IS '状态: 1-Begin, 2-Committing, 3-Committed, 4-Rollbacking, 5-RolledBack, 6-TimeoutRollbacking, 7-TimeoutRolledBack, 8-Finish, 9-CommitFailed, 10-RollbackFailed, 11-TimeoutRollbackFailed, 12-AsyncCommitting';

-- ============================================================
-- 分支事务表
-- ============================================================
CREATE TABLE IF NOT EXISTS branch_table (
    branch_id BIGINT NOT NULL PRIMARY KEY,
    xid VARCHAR(128) NOT NULL,
    transaction_id BIGINT NOT NULL,
    resource_group_id VARCHAR(32),
    resource_id VARCHAR(256) NOT NULL,
    branch_type VARCHAR(8) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    client_id VARCHAR(64),
    application_data VARCHAR(2000),
    gmt_create TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_xid ON branch_table (xid);

COMMENT ON TABLE branch_table IS 'Seata 分支事务表';
COMMENT ON COLUMN branch_table.branch_id IS '分支事务ID';
COMMENT ON COLUMN branch_table.xid IS '全局事务ID';
COMMENT ON COLUMN branch_table.branch_type IS '分支类型: AT, TCC, SAGA, XA';
COMMENT ON COLUMN branch_table.status IS '状态: 1-Registered, 2-PhaseOne_Done, 3-PhaseOne_Failed, 4-PhaseOne_Timeout, 5-PhaseTwo_Committed, 6-PhaseTwo_RolledBack, 7-PhaseTwo_CommitFailed_Retryable, 8-PhaseTwo_CommitFailed_Unretryable, 9-PhaseTwo_RollbackFailed_Retryable, 10-PhaseTwo_RollbackFailed_Unretryable';

-- ============================================================
-- 分布式锁表
-- ============================================================
CREATE TABLE IF NOT EXISTS lock_table (
    row_key VARCHAR(128) NOT NULL PRIMARY KEY,
    xid VARCHAR(128) NOT NULL,
    transaction_id BIGINT NOT NULL,
    branch_id BIGINT NOT NULL,
    resource_id VARCHAR(256) NOT NULL,
    table_name VARCHAR(64) NOT NULL,
    pk VARCHAR(36) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 0,
    gmt_create TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP(0) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_branch_id ON lock_table (branch_id);
CREATE INDEX idx_xid_and_branch_id ON lock_table (xid, branch_id);
CREATE INDEX idx_status ON lock_table (status);

COMMENT ON TABLE lock_table IS 'Seata 全局锁表';
COMMENT ON COLUMN lock_table.row_key IS '行锁Key: resourceId_tableName_pk';
COMMENT ON COLUMN lock_table.pk IS '主键值';
COMMENT ON COLUMN lock_table.status IS '状态: 0-Locked, 1-Unlocked';

-- ============================================================
-- 分布式锁辅助表
-- ============================================================
CREATE TABLE IF NOT EXISTS distributed_lock (
    lock_key VARCHAR(128) NOT NULL PRIMARY KEY,
    lock_value VARCHAR(128) NOT NULL,
    expire BIGINT NOT NULL,
    UNIQUE (lock_key)
);

COMMENT ON TABLE distributed_lock IS 'Seata 分布式锁辅助表';

-- ============================================================
-- 验证表结构
-- ============================================================
SELECT 'Seata database initialized successfully!' AS status;

\dt