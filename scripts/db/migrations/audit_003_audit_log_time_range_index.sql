-- ======================================================================
-- 审计日志：分页查询支持 create_time 范围下推
--
-- 约束：
--   * 幂等：CREATE INDEX IF NOT EXISTS，重复执行安全。
--   * 只新增索引，不改写任何既有行。
--   * sys_audit_log 按月分区；PostgreSQL 会为既有分区创建子索引，并为后续
--     新分区自动挂接同一索引，因此无需逐分区建索引。
--   * 列顺序 tenant_id 在前：TenantInterceptor 在每个查询上追加
--     tenant_id 谓词，复合索引同时服务租户隔离与时间范围过滤。
-- ======================================================================

CREATE INDEX IF NOT EXISTS idx_sys_audit_log_tenant_create_time
    ON sys_audit_log (tenant_id, create_time DESC);

COMMENT ON INDEX idx_sys_audit_log_tenant_create_time
    IS '审计分页：租户隔离 + create_time 范围下推';
