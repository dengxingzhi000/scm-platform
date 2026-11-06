-- ======================================================================
-- 审计日志：服务间写入幂等（request_nonce + 唯一索引）
--
-- 目标（对应 audit_004 的两条 P0 之一）：
--   * POST /sys-audit-log 携带签名头 X-Nonce，落库到 request_nonce；
--   * 同一个 nonce 的重复投递只保留一行，冲突不报错、不产生重复行。
--
-- 约束与设计说明：
--   * 幂等：ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS，重复执行安全；
--     只新增列与索引，不改写任何既有行（审计日志保持 append-only）。
--   * 历史行不回填：request_nonce 保持 NULL。唯一索引是 partial index
--     （WHERE request_nonce IS NOT NULL），历史行既不进索引也不会互相冲突，
--     因而不需要改写任何历史数据。
--   * 为什么唯一键不是 (tenant_id, request_nonce)：sys_audit_log 按月分区
--     （PARTITION BY RANGE (create_time)），PostgreSQL 要求分区表上的唯一索引
--     必须包含分区键，单列 nonce 的唯一索引会直接建不出来。
--     （唯一的“写”只发生在本脚本新增的列与索引上，历史行保持原样。）
--     于是唯一键取 (tenant_id, request_nonce, create_time)。
--   * 这对重试为什么仍然成立：调用方在首次发送前只生成一次 nonce，并且
--     create_time 也在首次发送前确定（RemoteSysAuditLogService 只补一次，
--     scm-audit 不再用本地时钟覆盖调用方给的时间）。重试原样复用这两个值，
--     落进同一个分区月，因此第二次写入必然命中唯一键。
--   * 列顺序 tenant_id 在前：TenantInterceptor 会给每个查询追加 tenant_id 谓词，
--     复合唯一索引同时服务租户隔离与幂等判定。
--
-- 执行方式（运维手工，不走 Flyway）：
--   本文件与 audit_003 一样放在 scripts/db/migrations/，不属于 platform-audit 的
--   Flyway locations（classpath:db/migration 为空）。服务发布后请等到恢复窗口再执行：
--     psql -h <host> -U <admin> -d db_audit -f audit_004_audit_log_request_nonce.sql
--   执行顺序：先执行本脚本，再重启/滚动 platform-audit。代码在缺少该列时写入会失败
--   （INSERT 里带 request_nonce），所以必须先加列再上新版本；旧版本在加列之后仍可正常工作。
-- ======================================================================

ALTER TABLE sys_audit_log
    ADD COLUMN IF NOT EXISTS request_nonce VARCHAR(64);

COMMENT ON COLUMN sys_audit_log.request_nonce
    IS '服务间写入幂等键，取自签名头 X-Nonce；历史行保持 NULL（不回填）';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_audit_log_tenant_nonce
    ON sys_audit_log (tenant_id, request_nonce, create_time)
    WHERE request_nonce IS NOT NULL;

COMMENT ON INDEX uk_sys_audit_log_tenant_nonce
    IS '审计写入幂等：同一租户 + nonce + create_time 只有一行；含 create_time 是因为 sys_audit_log 按它分区';
