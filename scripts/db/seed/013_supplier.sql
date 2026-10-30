-- ======================================================================
-- 本地开发种子数据 013 — 供应商
-- 目标库：db_supplier
-- ======================================================================
BEGIN;

ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS website VARCHAR(256);
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS supplier_level INT;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS audit_status INT;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS audit_remark TEXT;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS audited_by UUID;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS audited_at TIMESTAMP;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS risk_level INT;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS blacklisted BOOLEAN DEFAULT FALSE;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS blacklist_reason TEXT;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS blacklisted_at TIMESTAMP;
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS overall_score DECIMAL(5, 2);
ALTER TABLE sup_supplier ADD COLUMN IF NOT EXISTS last_evaluation_at TIMESTAMP;

INSERT INTO sup_supplier (
    id, tenant_id, supplier_code, supplier_name, supplier_name_en, supplier_type,
    legal_person, contact_name, contact_phone, contact_email, contact_address,
    credit_rating, quality_score, delivery_score, service_score,
    cooperation_start_date, cooperation_status, payment_terms, payment_method,
    enabled, sort_order, create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000701', '019a0aee-0000-7000-8000-000000000002', 'SUP-001', 'Apple供应链', 'Apple Supply', 2, '张经理', '张经理', '13800000001', 'zhang@apple-supply.example.com', '上海市浦东新区世纪大道 88 号', 'A+', 96, 94, 98, DATE '2024-01-01', 1, 'NET30', 'BANK_TRANSFER', TRUE, 10, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000702', '019a0aee-0000-7000-8000-000000000002', 'SUP-002', 'Samsung代理', 'Samsung Agent', 3, '李经理', '李经理', '13800000002', 'li@samsung-agent.example.com', '北京市朝阳区建国路 99 号', 'A', 90, 88, 92, DATE '2024-02-01', 1, 'NET30', 'BANK_TRANSFER', TRUE, 20, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000703', '019a0aee-0000-7000-8000-000000000002', 'SUP-003', 'Huawei授权', 'Huawei Agent', 3, '王经理', '王经理', '13800000003', 'wang@huawei-supply.example.com', '深圳市龙岗区坂田华为基地', 'B+', 84, 80, 86, DATE '2024-03-01', 2, 'NET60', 'BANK_TRANSFER', FALSE, 30, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000704', '019a0aee-0000-7000-8000-000000000002', 'SUP-004', '深圳华强电子', 'HQ Electronic', 2, '陈经理', '陈经理', '13800000004', 'chen@hqelectronic.example.com', '深圳市福田区华强北路 1002 号', 'A', 92, 90, 88, DATE '2024-04-12', 1, 'NET30', 'BANK_TRANSFER', TRUE, 40, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000705', '019a0aee-0000-7000-8000-000000000002', 'SUP-005', '上海联创供应链', 'Lianchuang Supply', 2, '周经理', '周经理', '13800000005', 'zhou@lianchuang.example.com', '上海市闵行区虹梅路 2588 号', 'B+', 82, 86, 84, DATE '2024-05-08', 1, 'NET30', 'BANK_TRANSFER', TRUE, 50, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000706', '019a0aee-0000-7000-8000-000000000002', 'SUP-006', '广州捷普物流', 'Jabil Logistics', 2, '吴经理', '吴经理', '13800000006', 'wu@jabil-logistics.example.com', '广州市黄埔区科学大道 182 号', 'A', 88, 92, 90, DATE '2024-06-18', 1, 'NET30', 'BANK_TRANSFER', TRUE, 60, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000707', '019a0aee-0000-7000-8000-000000000002', 'SUP-007', '杭州锐迈科技', 'Ruimai Tech', 1, '郑经理', '郑经理', '13800000007', 'zheng@ruimai.example.com', '杭州市余杭区文一西路 969 号', 'C', 70, 68, 72, DATE '2024-07-22', 3, 'COD', 'CASH', FALSE, 70, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000708', '019a0aee-0000-7000-8000-000000000002', 'SUP-008', '苏州精工制造', 'Jinggong Manufacturing', 1, '孙经理', '孙经理', '13800000008', 'sun@jinggong.example.com', '苏州市工业园区星湖街 328 号', 'A', 95, 90, 94, DATE '2024-08-30', 1, 'NET60', 'BANK_TRANSFER', TRUE, 80, NOW(), NOW(), FALSE, '本地开发种子数据')
ON CONFLICT (id) DO UPDATE SET
    supplier_name = EXCLUDED.supplier_name,
    tenant_id = EXCLUDED.tenant_id,
    contact_name = EXCLUDED.contact_name,
    contact_phone = EXCLUDED.contact_phone,
    quality_score = EXCLUDED.quality_score,
    delivery_score = EXCLUDED.delivery_score,
    service_score = EXCLUDED.service_score,
    enabled = EXCLUDED.enabled,
    update_time = NOW();

-- 企业级字段由 Flyway V1 添加：种子数据在迁移前/后执行都要保持一致。
-- 迁移前执行时 V1 会统一回填，这里只在列存在时补齐分级与综合分。
DO $do$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'sup_supplier'
          AND column_name = 'audit_status'
    ) THEN
        EXECUTE $sql$
            UPDATE sup_supplier
            SET audit_status = 2,
                overall_score = ROUND((quality_score + delivery_score + service_score) / 3.0, 2),
                supplier_level = CASE
                    WHEN (quality_score + delivery_score + service_score) / 3.0 >= 90 THEN 1
                    WHEN (quality_score + delivery_score + service_score) / 3.0 >= 80 THEN 2
                    WHEN (quality_score + delivery_score + service_score) / 3.0 >= 70 THEN 3
                    ELSE 4
                END,
                risk_level = COALESCE(risk_level, 1),
                blacklisted = COALESCE(blacklisted, FALSE),
                update_time = NOW()
            WHERE id IN (
                '019a0aee-0000-7000-8000-000000000701',
                '019a0aee-0000-7000-8000-000000000702',
                '019a0aee-0000-7000-8000-000000000703',
                '019a0aee-0000-7000-8000-000000000704',
                '019a0aee-0000-7000-8000-000000000705',
                '019a0aee-0000-7000-8000-000000000706',
                '019a0aee-0000-7000-8000-000000000707',
                '019a0aee-0000-7000-8000-000000000708'
            )
        $sql$;
    END IF;
END $do$;

COMMIT;
