-- ======================================================================
-- 财务批次：对账记录差异类型与驳回流转
--
-- 约束：
--   * 幂等：所有 DDL 使用 IF NOT EXISTS / pg_constraint 判断，重复执行安全。
--   * 只报错不改写：历史数据不合规（状态越界、差异类型为空但标记有差异）时
--     RAISE EXCEPTION 中止迁移，绝不改写原始单据。
--   * reconciliation_record.status 从 0-3 扩展到 0-4：
--     0-待对账, 1-已对账, 2-已确认, 3-有差异, 4-已驳回。
--   * diff_type 仅允许 AMOUNT / MISSING_DOC / DUPLICATE / OTHER，未标记差异时为 NULL。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 历史数据体检：异常即中止（不修改任何数据）
-- ----------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM reconciliation_record WHERE status NOT IN (0, 1, 2, 3)) THEN
        RAISE EXCEPTION 'reconciliation_record 存在 0-3 之外的状态值，停止迁移，请人工核对';
    END IF;
    IF EXISTS (SELECT 1 FROM reconciliation_record WHERE deleted = FALSE AND has_diff = TRUE AND diff_reason IS NULL) THEN
        RAISE EXCEPTION 'reconciliation_record 存在标记差异但无差异原因的历史行，停止迁移，请人工核对';
    END IF;
END $$;

-- ----------------------------------------------------------------------
-- 2. 新增列（幂等）
-- ----------------------------------------------------------------------
ALTER TABLE reconciliation_record ADD COLUMN IF NOT EXISTS diff_type VARCHAR(32);
ALTER TABLE reconciliation_record ADD COLUMN IF NOT EXISTS rejected_by UUID;
ALTER TABLE reconciliation_record ADD COLUMN IF NOT EXISTS rejected_at TIMESTAMP;
ALTER TABLE reconciliation_record ADD COLUMN IF NOT EXISTS reject_reason TEXT;

COMMENT ON COLUMN reconciliation_record.diff_type IS '差异类型:AMOUNT-金额差异,MISSING_DOC-单据缺失,DUPLICATE-重复单据,OTHER-其他';
COMMENT ON COLUMN reconciliation_record.status IS '状态:0-待对账,1-已对账,2-已确认,3-有差异,4-已驳回';

-- ----------------------------------------------------------------------
-- 3. 状态约束：0/1/2/3 → 0/1/2/3/4（新增 REJECTED）
-- ----------------------------------------------------------------------
DO $$
DECLARE
    constraint_def TEXT;
BEGIN
    SELECT pg_get_constraintdef(oid) INTO constraint_def
      FROM pg_constraint
     WHERE conname = 'chk_reconciliation_status';

    IF constraint_def IS NULL THEN
        ALTER TABLE reconciliation_record
            ADD CONSTRAINT chk_reconciliation_status CHECK (status IN (0, 1, 2, 3, 4));
    ELSIF constraint_def NOT LIKE '%4%' THEN
        ALTER TABLE reconciliation_record DROP CONSTRAINT chk_reconciliation_status;
        ALTER TABLE reconciliation_record
            ADD CONSTRAINT chk_reconciliation_status CHECK (status IN (0, 1, 2, 3, 4));
    END IF;
END $$;

-- ----------------------------------------------------------------------
-- 4. 差异类型约束（新增列，历史行均为 NULL，不会阻断）
-- ----------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_reconciliation_diff_type') THEN
        ALTER TABLE reconciliation_record
            ADD CONSTRAINT chk_reconciliation_diff_type
            CHECK (diff_type IS NULL OR diff_type IN ('AMOUNT', 'MISSING_DOC', 'DUPLICATE', 'OTHER'));
    END IF;
END $$;

-- ----------------------------------------------------------------------
-- 5. 查询索引（差异类型筛选、驳回记录回溯）
-- ----------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_reconciliation_diff_type
    ON reconciliation_record(tenant_id, diff_type) WHERE has_diff = TRUE AND NOT deleted;

CREATE INDEX IF NOT EXISTS idx_reconciliation_rejected
    ON reconciliation_record(tenant_id, rejected_at) WHERE status = 4 AND NOT deleted;
