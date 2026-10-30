-- ======================================================================
-- 企业级供应商管理扩展
-- 覆盖：准入审核、分级分类、风险与黑名单、联系人、银行账户、
--       资质证照、评价绩效、变更留痕
-- 说明：Flyway 只执行一次；所有变更均做 IF NOT EXISTS / 约束存在性判断，
--       可在已有 db_supplier 上平滑升级。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 0. 基础列补全（Windows 初始化脚本未给 sup_supplier 加 tenant_id）
-- ----------------------------------------------------------------------
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS tenant_id UUID;

-- ----------------------------------------------------------------------
-- 1. 供应商主数据扩展
-- ----------------------------------------------------------------------
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS supplier_level SMALLINT DEFAULT 3;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS audit_status SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS audit_remark VARCHAR(512);
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS audited_by VARCHAR(64);
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS audited_at TIMESTAMP;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS risk_level SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS blacklisted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS blacklist_reason VARCHAR(512);
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS blacklisted_at TIMESTAMP;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS overall_score DECIMAL(5, 2);
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS last_evaluation_at TIMESTAMP;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS website VARCHAR(256);

-- 兼容历史环境：同名列可能已经存在，但为空、无默认值或没有 NOT NULL
UPDATE sup_supplier SET audit_status = 2 WHERE audit_status IS NULL OR audit_status = 0;
UPDATE sup_supplier SET supplier_level = 3 WHERE supplier_level IS NULL;
UPDATE sup_supplier SET risk_level = 1 WHERE risk_level IS NULL;
UPDATE sup_supplier SET blacklisted = FALSE WHERE blacklisted IS NULL;

ALTER TABLE sup_supplier ALTER COLUMN audit_status SET DEFAULT 0;
ALTER TABLE sup_supplier ALTER COLUMN audit_status SET NOT NULL;
ALTER TABLE sup_supplier ALTER COLUMN supplier_level SET DEFAULT 3;
ALTER TABLE sup_supplier ALTER COLUMN supplier_level SET NOT NULL;
ALTER TABLE sup_supplier ALTER COLUMN risk_level SET DEFAULT 1;
ALTER TABLE sup_supplier ALTER COLUMN risk_level SET NOT NULL;
ALTER TABLE sup_supplier ALTER COLUMN blacklisted SET DEFAULT FALSE;
ALTER TABLE sup_supplier ALTER COLUMN blacklisted SET NOT NULL;

-- 历史列类型与实体映射不一致（数组/JSONB 无法直接映射 String），统一收敛为 TEXT
DO $do$
BEGIN
    -- 历史环境可能把审核人建成了 UUID，统一为 VARCHAR(64) 以兼容非 UUID 操作人标识
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'sup_supplier'
          AND column_name = 'audited_by'
          AND data_type = 'uuid'
    ) THEN
        ALTER TABLE sup_supplier
            ALTER COLUMN audited_by TYPE VARCHAR(64) USING audited_by::text;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'sup_supplier'
          AND column_name = 'business_type'
          AND data_type = 'ARRAY'
    ) THEN
        ALTER TABLE sup_supplier
            ALTER COLUMN business_type TYPE TEXT USING array_to_string(business_type, ',');
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'sup_supplier'
          AND column_name = 'certificates'
          AND data_type = 'jsonb'
    ) THEN
        ALTER TABLE sup_supplier
            ALTER COLUMN certificates TYPE TEXT USING certificates::text;
    END IF;
END $do$;

-- 视图依赖评分列，先删除后重建，否则无法变更列类型
DROP VIEW IF EXISTS v_supplier_performance;

-- 评分列放宽到 DECIMAL(5,2)，原 DECIMAL(3,1) 无法保存 100.0
ALTER TABLE sup_supplier ALTER COLUMN quality_score TYPE DECIMAL(5, 2);
ALTER TABLE sup_supplier ALTER COLUMN delivery_score TYPE DECIMAL(5, 2);
ALTER TABLE sup_supplier ALTER COLUMN service_score TYPE DECIMAL(5, 2);

ALTER TABLE sup_supplier_evaluation ALTER COLUMN quality_score TYPE DECIMAL(5, 2);
ALTER TABLE sup_supplier_evaluation ALTER COLUMN delivery_score TYPE DECIMAL(5, 2);
ALTER TABLE sup_supplier_evaluation ALTER COLUMN service_score TYPE DECIMAL(5, 2);
ALTER TABLE sup_supplier_evaluation ALTER COLUMN price_score TYPE DECIMAL(5, 2);
ALTER TABLE sup_supplier_evaluation ALTER COLUMN total_score TYPE DECIMAL(5, 2);

-- 枚举约束（PostgreSQL 不支持 ADD CONSTRAINT IF NOT EXISTS，用 DO 块判重）
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_supplier_level') THEN
        ALTER TABLE sup_supplier ADD CONSTRAINT chk_supplier_level
            CHECK (supplier_level IN (1, 2, 3, 4));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_supplier_audit_status') THEN
        ALTER TABLE sup_supplier ADD CONSTRAINT chk_supplier_audit_status
            CHECK (audit_status IN (0, 1, 2, 3));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_supplier_risk_level') THEN
        ALTER TABLE sup_supplier ADD CONSTRAINT chk_supplier_risk_level
            CHECK (risk_level IN (1, 2, 3));
    END IF;
END $$;

COMMENT ON COLUMN sup_supplier.supplier_level IS '供应商分级:1-战略,2-优选,3-合格,4-备选';
COMMENT ON COLUMN sup_supplier.audit_status IS '准入状态:0-草稿,1-待审核,2-已通过,3-已驳回';
COMMENT ON COLUMN sup_supplier.risk_level IS '风险等级:1-低,2-中,3-高';
COMMENT ON COLUMN sup_supplier.blacklisted IS '是否已列入黑名单';
COMMENT ON COLUMN sup_supplier.overall_score IS '综合绩效得分(0-100)';
COMMENT ON COLUMN sup_supplier.last_evaluation_at IS '最近一次评价时间';

-- 历史数据视为已完成准入，避免存量供应商因默认草稿而无法启用
UPDATE sup_supplier
SET overall_score = ROUND((quality_score + delivery_score + service_score) / 3.0, 2)
WHERE overall_score IS NULL
  AND quality_score IS NOT NULL
  AND delivery_score IS NOT NULL
  AND service_score IS NOT NULL;
UPDATE sup_supplier
SET supplier_level = CASE
        WHEN overall_score >= 90 THEN 1
        WHEN overall_score >= 80 THEN 2
        WHEN overall_score >= 70 THEN 3
        ELSE 4
    END
WHERE overall_score IS NOT NULL;

-- 供应商编码唯一性收敛到租户维度（原表为全局唯一）
ALTER TABLE sup_supplier DROP CONSTRAINT IF EXISTS sup_supplier_supplier_code_key;
CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_tenant_code
    ON sup_supplier(tenant_id, supplier_code) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_supplier_tenant_tax_no
    ON sup_supplier(tenant_id, tax_no) WHERE NOT deleted AND tax_no IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_supplier_audit
    ON sup_supplier(tenant_id, audit_status) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_supplier_level
    ON sup_supplier(tenant_id, supplier_level) WHERE NOT deleted;

-- ----------------------------------------------------------------------
-- 2. 供应商联系人（多联系人，一个主联系人）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sup_supplier_contact (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    supplier_id UUID NOT NULL,
    contact_name VARCHAR(128) NOT NULL,
    contact_title VARCHAR(128),
    contact_role VARCHAR(32) NOT NULL DEFAULT 'OTHER',
    contact_phone VARCHAR(32),
    contact_email VARCHAR(128),
    contact_address TEXT,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_by VARCHAR(64),
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by VARCHAR(64),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,
    CONSTRAINT fk_supplier_contact FOREIGN KEY (supplier_id) REFERENCES sup_supplier(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_primary_contact
    ON sup_supplier_contact(supplier_id) WHERE is_primary AND NOT deleted;
CREATE INDEX IF NOT EXISTS idx_supplier_contact
    ON sup_supplier_contact(tenant_id, supplier_id, sort_order) WHERE NOT deleted;

COMMENT ON TABLE sup_supplier_contact IS '供应商联系人表';
COMMENT ON COLUMN sup_supplier_contact.contact_role IS '联系人角色:SALES-销售,FINANCE-财务,LOGISTICS-物流,TECH-技术,OTHER-其他';

-- ----------------------------------------------------------------------
-- 3. 供应商银行账户（多账户，一个默认账户）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sup_supplier_bank_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    supplier_id UUID NOT NULL,
    bank_name VARCHAR(128) NOT NULL,
    bank_branch VARCHAR(256),
    bank_account VARCHAR(64) NOT NULL,
    account_name VARCHAR(128) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'CNY',
    swift_code VARCHAR(32),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_by VARCHAR(64),
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by VARCHAR(64),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,
    CONSTRAINT fk_supplier_bank FOREIGN KEY (supplier_id) REFERENCES sup_supplier(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_default_bank
    ON sup_supplier_bank_account(supplier_id) WHERE is_default AND NOT deleted;
CREATE INDEX IF NOT EXISTS idx_supplier_bank
    ON sup_supplier_bank_account(tenant_id, supplier_id, sort_order) WHERE NOT deleted;

COMMENT ON TABLE sup_supplier_bank_account IS '供应商银行账户表';

-- ----------------------------------------------------------------------
-- 4. 供应商资质证照（有效期与到期预警）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sup_supplier_certificate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    supplier_id UUID NOT NULL,
    certificate_type VARCHAR(64) NOT NULL,
    certificate_name VARCHAR(256) NOT NULL,
    certificate_no VARCHAR(128),
    issuing_authority VARCHAR(256),
    valid_from DATE,
    valid_to DATE,
    file_url VARCHAR(512),
    status SMALLINT NOT NULL DEFAULT 1,
    verified_by VARCHAR(64),
    verified_at TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_by VARCHAR(64),
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by VARCHAR(64),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,
    CONSTRAINT fk_supplier_certificate FOREIGN KEY (supplier_id) REFERENCES sup_supplier(id)
);

CREATE INDEX IF NOT EXISTS idx_supplier_certificate
    ON sup_supplier_certificate(tenant_id, supplier_id, valid_to) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_supplier_certificate_expiry
    ON sup_supplier_certificate(tenant_id, valid_to) WHERE NOT deleted AND valid_to IS NOT NULL;

COMMENT ON TABLE sup_supplier_certificate IS '供应商资质证照表';
COMMENT ON COLUMN sup_supplier_certificate.certificate_type IS '证照类型:BUSINESS_LICENSE-营业执照,TAX_REGISTRATION-税务登记,QUALITY_ISO-质量体系,PRODUCTION_LICENSE-生产许可,AGENT_AUTHORIZATION-代理授权,OTHER-其他';
COMMENT ON COLUMN sup_supplier_certificate.status IS '状态:0-待审核,1-有效,2-即将到期,3-已过期,4-已驳回';

-- ----------------------------------------------------------------------
-- 5. 供应商分类字典与关联
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sup_supplier_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    category_code VARCHAR(64) NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    parent_id UUID,
    level INT NOT NULL DEFAULT 1,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_by VARCHAR(64),
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_by VARCHAR(64),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_category_code
    ON sup_supplier_category(tenant_id, category_code) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_supplier_category
    ON sup_supplier_category(tenant_id, parent_id, sort_order) WHERE NOT deleted;

COMMENT ON TABLE sup_supplier_category IS '供应商分类字典表';

CREATE TABLE IF NOT EXISTS sup_supplier_category_rel (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    supplier_id UUID NOT NULL,
    category_id UUID NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_category_rel_supplier FOREIGN KEY (supplier_id) REFERENCES sup_supplier(id),
    CONSTRAINT fk_category_rel_category FOREIGN KEY (category_id) REFERENCES sup_supplier_category(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_category_rel
    ON sup_supplier_category_rel(supplier_id, category_id);
CREATE INDEX IF NOT EXISTS idx_supplier_category_rel_category
    ON sup_supplier_category_rel(tenant_id, category_id);

COMMENT ON TABLE sup_supplier_category_rel IS '供应商与分类关联表';

-- ----------------------------------------------------------------------
-- 6. 供应商变更日志（准入/黑名单/关键字段变更留痕）
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sup_supplier_change_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    supplier_id UUID NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    change_content TEXT,
    reason VARCHAR(512),
    operator_id VARCHAR(64),
    operator_name VARCHAR(128),
    create_time TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_supplier_change_log
    ON sup_supplier_change_log(tenant_id, supplier_id, create_time DESC);

COMMENT ON TABLE sup_supplier_change_log IS '供应商变更日志表';
COMMENT ON COLUMN sup_supplier_change_log.change_type IS '变更类型:CREATE,UPDATE,DELETE,SUBMIT_AUDIT,AUDIT_APPROVE,AUDIT_REJECT,ENABLE,DISABLE,BLACKLIST,UNBLACKLIST,RISK_LEVEL,COOPERATION_STATUS,CATEGORY_ASSIGN';

-- ----------------------------------------------------------------------
-- 7. 重建供应商绩效视图（增加分级、综合得分、风险与黑名单）
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW v_supplier_performance AS
SELECT
    s.id,
    s.tenant_id,
    s.supplier_code,
    s.supplier_name,
    s.credit_rating,
    s.supplier_level,
    s.overall_score,
    s.quality_score,
    s.delivery_score,
    s.service_score,
    COUNT(DISTINCT po.id) AS purchase_count,
    SUM(po.payable_amount) AS total_purchase_amount,
    AVG(e.total_score) AS avg_evaluation_score,
    s.cooperation_status,
    s.risk_level,
    s.blacklisted,
    s.enabled
FROM sup_supplier s
LEFT JOIN sup_purchase_order po ON s.id = po.supplier_id AND NOT po.deleted
LEFT JOIN sup_supplier_evaluation e ON s.id = e.supplier_id
WHERE NOT s.deleted
GROUP BY
    s.id,
    s.tenant_id,
    s.supplier_code,
    s.supplier_name,
    s.credit_rating,
    s.supplier_level,
    s.overall_score,
    s.quality_score,
    s.delivery_score,
    s.service_score,
    s.cooperation_status,
    s.risk_level,
    s.blacklisted,
    s.enabled;

COMMENT ON VIEW v_supplier_performance IS '供应商绩效统计视图';
