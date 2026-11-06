-- ======================================================================
-- 财务批次 4：账期重开（已结账 CLOSED → 已重开 REOPENED）
--
-- 约束：
--   * 幂等：所有 DDL 使用 IF NOT EXISTS / pg_constraint 判断，重复执行安全。
--   * 历史不回填：只新增列与放宽状态约束，不改写任何既有账期行；
--     reopened_by / reopened_at / reopen_reason 对历史行保持 NULL。
--   * 只报错不改写：发现状态越界（0/1 之外，或未来版本的 2 之外）时
--     RAISE EXCEPTION 中止迁移，绝不在脚本里修正数据。
--   * 状态扩展为 0-未结账 / 1-已结账 / 2-已重开；0/1 语义不变。
--   * 重开是已结账账期唯一的合法回退路径，再次关账回到 CLOSED。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 历史数据体检：异常即中止（不修改任何数据）
-- ----------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM fin_accounting_period WHERE status NOT IN (0, 1, 2)) THEN
        RAISE EXCEPTION 'fin_accounting_period 存在 0-2 之外的状态值，停止迁移，请人工核对';
    END IF;
END $$;

-- ----------------------------------------------------------------------
-- 2. 重开留痕列（幂等，历史行保持 NULL，不回填）
-- ----------------------------------------------------------------------
ALTER TABLE fin_accounting_period ADD COLUMN IF NOT EXISTS reopened_by UUID;
ALTER TABLE fin_accounting_period ADD COLUMN IF NOT EXISTS reopened_at TIMESTAMP;
ALTER TABLE fin_accounting_period ADD COLUMN IF NOT EXISTS reopen_reason TEXT;

COMMENT ON COLUMN fin_accounting_period.reopened_by IS '最近一次重开操作人；历史行不回填';
COMMENT ON COLUMN fin_accounting_period.reopened_at IS '最近一次重开时间；历史行不回填';
COMMENT ON COLUMN fin_accounting_period.reopen_reason IS '最近一次重开原因；历史行不回填';

-- ----------------------------------------------------------------------
-- 3. 状态约束：0/1 → 0/1/2（新增 REOPENED）
-- ----------------------------------------------------------------------
DO $$
DECLARE
    constraint_def TEXT;
BEGIN
    SELECT pg_get_constraintdef(oid) INTO constraint_def
      FROM pg_constraint
     WHERE conname = 'chk_accounting_period_status';

    IF constraint_def IS NULL THEN
        ALTER TABLE fin_accounting_period
            ADD CONSTRAINT chk_accounting_period_status CHECK (status IN (0, 1, 2));
    ELSIF constraint_def NOT LIKE '%2%' THEN
        ALTER TABLE fin_accounting_period DROP CONSTRAINT chk_accounting_period_status;
        ALTER TABLE fin_accounting_period
            ADD CONSTRAINT chk_accounting_period_status CHECK (status IN (0, 1, 2));
    END IF;
END $$;

COMMENT ON COLUMN fin_accounting_period.status IS '状态:0-未结账,1-已结账,2-已重开';

-- ----------------------------------------------------------------------
-- 4. 重开回溯索引（按账期重开时间查询/报表）
-- ----------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_accounting_period_reopened
    ON fin_accounting_period(tenant_id, reopened_at) WHERE status = 2 AND NOT deleted;
