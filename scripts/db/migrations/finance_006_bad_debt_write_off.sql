-- ======================================================================
-- 财务批次 6：坏账核销单据表 — db_finance
--
-- 人工执行（本模块未接入 Flyway）。要求：
--   * 可重复执行：表/索引/约束全部走 IF NOT EXISTS 或 pg_constraint 判断；
--   * 历史数据不回填：只建表建索引，不改写既有结算单、发票与账期数据。
--
-- 执行：psql -d db_finance -f finance_006_bad_debt_write_off.sql
-- ======================================================================

BEGIN;

-- ----------------------------------------------------------------------
-- 1. 核销单表
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS fin_bad_debt_write_off (
    id              VARCHAR(36)    PRIMARY KEY,
    tenant_id       VARCHAR(36)    NOT NULL,
    write_off_no    VARCHAR(64)    NOT NULL,
    settlement_id   VARCHAR(36),
    invoice_id      VARCHAR(36),
    partner_id      VARCHAR(36),
    partner_name    VARCHAR(128),
    amount          DECIMAL(15,2)  NOT NULL,
    period_code     VARCHAR(7)     NOT NULL,
    status          SMALLINT       NOT NULL DEFAULT 0,
    reason          VARCHAR(500)   NOT NULL,
    applicant_id    VARCHAR(64)    NOT NULL,
    applicant_name  VARCHAR(128)   NOT NULL,
    applied_at      TIMESTAMP,
    approver_id     VARCHAR(64),
    approver_name   VARCHAR(128),
    approved_at     TIMESTAMP,
    approve_remark  VARCHAR(500),
    rejected_by     VARCHAR(64),
    rejected_at     TIMESTAMP,
    reject_reason   VARCHAR(500),
    written_off_by  VARCHAR(64),
    written_off_at  TIMESTAMP,
    create_time     TIMESTAMP,
    create_by       VARCHAR(64),
    update_time     TIMESTAMP,
    update_by       VARCHAR(64),
    deleted         BOOLEAN        NOT NULL DEFAULT FALSE
);

COMMENT ON TABLE fin_bad_debt_write_off IS '坏账核销单：申请(0) -> 已批准(1) -> 已核销(2)，可驳回(3)';

-- ----------------------------------------------------------------------
-- 2. 约束（可重复执行）
-- ----------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_bad_debt_status'
    ) THEN
        ALTER TABLE fin_bad_debt_write_off
            ADD CONSTRAINT chk_bad_debt_status CHECK (status BETWEEN 0 AND 3);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_bad_debt_amount_positive'
    ) THEN
        ALTER TABLE fin_bad_debt_write_off
            ADD CONSTRAINT chk_bad_debt_amount_positive CHECK (amount > 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_bad_debt_target'
    ) THEN
        ALTER TABLE fin_bad_debt_write_off
            ADD CONSTRAINT chk_bad_debt_target
            CHECK (settlement_id IS NOT NULL OR invoice_id IS NOT NULL);
    END IF;
END $$;

-- ----------------------------------------------------------------------
-- 3. 索引
--    部分唯一索引：同一结算单/发票在「申请中/已批准/已核销」只允许一张核销单，
--    驳回(3) 不占用，允许重新申请。服务层已先查重，这里由数据库兜底并发。
-- ----------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS uk_bad_debt_tenant_settlement
    ON fin_bad_debt_write_off (tenant_id, settlement_id)
    WHERE settlement_id IS NOT NULL AND status IN (0, 1, 2) AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_bad_debt_tenant_invoice
    ON fin_bad_debt_write_off (tenant_id, invoice_id)
    WHERE invoice_id IS NOT NULL AND status IN (0, 1, 2) AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_bad_debt_tenant_no
    ON fin_bad_debt_write_off (tenant_id, write_off_no);

CREATE INDEX IF NOT EXISTS idx_bad_debt_tenant_period
    ON fin_bad_debt_write_off (tenant_id, period_code, status);

COMMIT;
