-- ======================================================================
-- 租户配置：支持平台级配置（tenant_id IS NULL）
--
-- 约束：
--   * 幂等：DROP NOT NULL 与 CREATE INDEX IF NOT EXISTS 可重复执行。
--   * 只报错不改写：若已存在重复的平台级配置键，RAISE EXCEPTION 中止迁移，
--     由人工决定保留哪一条，不做自动合并或删除。
--   * 平台级配置行由平台运营方写入（当前没有生产接口）；本迁移只放开约束，
--     不插入任何默认数据。
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 放开 NOT NULL，使平台级配置可以用 tenant_id IS NULL 表达
-- ----------------------------------------------------------------------
ALTER TABLE tenant_config ALTER COLUMN tenant_id DROP NOT NULL;

-- ----------------------------------------------------------------------
-- 2. 体检：平台级配置键重复即中止（不修改任何数据）
-- ----------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM tenant_config
         WHERE tenant_id IS NULL
         GROUP BY config_category, config_key
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'tenant_config 存在重复的平台级配置键，停止迁移，请人工核对';
    END IF;
END $$;

-- ----------------------------------------------------------------------
-- 3. 平台级唯一键：tenant_id 为 NULL 时数据库的唯一约束不生效，单独补部分唯一索引
-- ----------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS uk_tenant_config_platform
    ON tenant_config(config_category, config_key) WHERE tenant_id IS NULL;

COMMENT ON COLUMN tenant_config.tenant_id IS '租户ID；平台级配置为 NULL，仅平台管理员可读写';
