-- ======================================================================
-- 财务批次 2：账期、对账差异闭环、资金流水关联校验
--
-- 约束：
--   * 幂等：所有 DDL 使用 IF NOT EXISTS / pg_constraint 判断，重复执行安全。
--   * 只报错不改写：发现历史异常数据（负差异、孤儿流水）时 RAISE EXCEPTION
--     中止迁移，绝不改写或删除原始单据。
--   * 差异状态扩展为 0-待处理 / 1-已确认 / 2-已核销 / 3-已驳回；
--     既有 0/1/2 数据语义不变。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 历史数据体检：异常即中止（不修改任何数据）
-- ----------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM fin_reconciliation_diff WHERE diff_amount < 0) THEN
        RAISE EXCEPTION 'fin_reconciliation_diff 存在负差异金额，停止迁移，请人工核对';
    END IF;
    IF EXISTS (SELECT 1 FROM fin_fund_flow
               WHERE settlement_id IS NULL AND invoice_id IS NULL
                 AND source_type <> 'ADJUSTMENT') THEN
        RAISE EXCEPTION 'fin_fund_flow 存在未关联结算单/发票的孤儿流水，停止迁移，请人工核对';
    END IF;
    IF EXISTS (SELECT 1 FROM fin_fund_flow f
               WHERE f.settlement_id IS NOT NULL
                 AND NOT EXISTS (SELECT 1 FROM settlement_order s
                                 WHERE s.id = f.settlement_id AND s.tenant_id = f.tenant_id)) THEN
        RAISE EXCEPTION 'fin_fund_flow 存在跨租户或不存在的结算单关联，停止迁移，请人工核对';
    END IF;
END $$;

-- ----------------------------------------------------------------------
-- 2. 对账差异状态约束：0/1/2 → 0/1/2/3（新增 REJECTED）
-- ----------------------------------------------------------------------
DO $$
DECLARE
    constraint_def TEXT;
BEGIN
    SELECT pg_get_constraintdef(oid) INTO constraint_def
      FROM pg_constraint
     WHERE conname = 'chk_recon_diff_status';

    IF constraint_def IS NULL THEN
        ALTER TABLE fin_reconciliation_diff
            ADD CONSTRAINT chk_recon_diff_status CHECK (status IN (0, 1, 2, 3));
    ELSIF constraint_def NOT LIKE '%3%' THEN
        ALTER TABLE fin_reconciliation_diff DROP CONSTRAINT chk_recon_diff_status;
        ALTER TABLE fin_reconciliation_diff
            ADD CONSTRAINT chk_recon_diff_status CHECK (status IN (0, 1, 2, 3));
    END IF;
END $$;

COMMENT ON COLUMN fin_reconciliation_diff.status IS '状态:0-待处理,1-已确认,2-已核销,3-已驳回';

-- ----------------------------------------------------------------------
-- 3. 账期查询索引补齐（关账守卫按租户 + 账期编码查询）
-- ----------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_accounting_period_tenant_code_live
    ON fin_accounting_period(tenant_id, period_code) WHERE NOT deleted;

-- ----------------------------------------------------------------------
-- 4. 资金流水关联索引补齐
-- ----------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_fund_flow_tenant_invoice
    ON fin_fund_flow(tenant_id, invoice_id);
