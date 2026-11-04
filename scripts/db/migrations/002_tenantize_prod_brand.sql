-- 002_tenantize_prod_brand.sql
-- 目标库: db_product（prod_brand 在 db_product，不在 db_mall）
-- 用途: prod_brand.brand_code 由“全局唯一”改为“租户内唯一” UNIQUE (tenant_id, brand_code)
-- 特性: 可重复执行；先核对约束名 / tenant_id / 同租户重复数据，发现冲突立即中止且不修改数据
--
-- 用法: psql "$PGURL/db_product" -f scripts/db/migrations/002_tenantize_prod_brand.sql

\set ON_ERROR_STOP on

DO $$
DECLARE
    v_dup_groups  integer;
    v_null_tenant integer;
    v_conname     text;
BEGIN
    IF to_regclass('prod_brand') IS NULL THEN
        RAISE EXCEPTION 'prod_brand 不存在，请确认当前连接的是 db_product';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'prod_brand'
          AND column_name = 'tenant_id'
    ) THEN
        RAISE EXCEPTION 'prod_brand 缺少 tenant_id 列，请先执行 001_add_tenant_id_to_business_tables.sql';
    END IF;

    SELECT count(*) INTO v_null_tenant FROM prod_brand WHERE tenant_id IS NULL;
    IF v_null_tenant > 0 THEN
        RAISE EXCEPTION 'prod_brand 有 % 行 tenant_id 为空，未做任何修改，请先补齐归属', v_null_tenant;
    END IF;

    SELECT count(*) INTO v_dup_groups
    FROM (
        SELECT tenant_id, brand_code
        FROM prod_brand
        GROUP BY tenant_id, brand_code
        HAVING count(*) > 1
    ) d;
    IF v_dup_groups > 0 THEN
        RAISE EXCEPTION 'prod_brand 存在 % 组“同租户重复 brand_code”，未做任何修改，请人工确认后再迁移', v_dup_groups;
    END IF;

    -- 只删除“仅 brand_code 单列”的唯一约束，保留已有的复合唯一约束
    FOR v_conname IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'prod_brand'
          AND c.contype = 'u'
          AND pg_get_constraintdef(c.oid) LIKE '%(brand_code)%'
          AND pg_get_constraintdef(c.oid) NOT LIKE '%tenant_id%'
    LOOP
        EXECUTE format('ALTER TABLE prod_brand DROP CONSTRAINT %I', v_conname);
        RAISE NOTICE '已删除单列唯一约束 %', v_conname;
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'prod_brand'
          AND c.conname = 'uk_tenant_brand_code'
    ) THEN
        ALTER TABLE prod_brand ADD CONSTRAINT uk_tenant_brand_code UNIQUE (tenant_id, brand_code);
        RAISE NOTICE '已新增约束 uk_tenant_brand_code UNIQUE (tenant_id, brand_code)';
    END IF;
END $$;

-- 查询索引；软删除行仍参与唯一约束，这是既有语义，未在本脚本中改变
DROP INDEX IF EXISTS idx_brand_code;
CREATE INDEX IF NOT EXISTS idx_brand_code ON prod_brand (tenant_id, brand_code) WHERE deleted = false;

-- 回读结果
SELECT 'unique_constraints' AS check_name,
       string_agg(conname, ', ' ORDER BY conname) AS detail
FROM pg_constraint c
JOIN pg_class t ON t.oid = c.conrelid
JOIN pg_namespace n ON n.oid = t.relnamespace
WHERE n.nspname = current_schema()
  AND t.relname = 'prod_brand'
  AND c.contype = 'u';
