-- ======================================================================
-- 005 — sys_user.last_login_ip 类型调整
-- 目标库：db_user
--
-- 背景：建库脚本把 last_login_ip 定义为 INET，但 SysUser.lastLoginIp 与
-- SysUserMapper.updateLastLogin 都以 String 绑定参数，登录后写入会报
--   column "last_login_ip" is of type inet but expression is of type character varying
-- 这里统一改为 VARCHAR(45)，可容纳 IPv4/IPv6 文本地址。
--
-- 幂等：仅当当前类型不是 character varying 时执行。
-- ======================================================================
BEGIN;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'sys_user'
          AND column_name = 'last_login_ip'
          AND data_type <> 'character varying'
    ) THEN
        ALTER TABLE sys_user
            ALTER COLUMN last_login_ip TYPE VARCHAR(45)
            USING last_login_ip::text;
    END IF;
END $$;

COMMIT;
