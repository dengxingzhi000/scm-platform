-- ======================================================================
-- LMS 批次 3：SLA 预警、异常件/理赔闭环、承运商派发幂等
-- 适用库: db_logistics
-- 执行方式: 幂等（IF NOT EXISTS），仅新增表/索引，异常中止不删除数据
-- ======================================================================

BEGIN;

-- ----------------------------------------------------------------------
-- SLA 超时预警：同运单同承诺时间唯一，扫描幂等
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tms_sla_alert (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID NOT NULL,
    waybill_id     UUID,
    waybill_no     VARCHAR(128) NOT NULL,
    carrier_id     UUID,
    promised_time  TIMESTAMP NOT NULL,
    overdue_hours  DECIMAL(10, 2) NOT NULL DEFAULT 0,
    level          VARCHAR(16) NOT NULL DEFAULT 'WARN',
    status         VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    ack_by         VARCHAR(64),
    ack_time       TIMESTAMP,
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark         VARCHAR(512),
    CONSTRAINT chk_sla_level CHECK (level IN ('WARN', 'CRITICAL')),
    CONSTRAINT chk_sla_status CHECK (status IN ('OPEN', 'ACK', 'CLOSED')),
    CONSTRAINT chk_sla_overdue CHECK (overdue_hours >= 0)
);

COMMENT ON TABLE tms_sla_alert IS 'SLA 超时预警：承诺时效 vs 实际签收/当前时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_tms_sla_alert_waybill_promised
    ON tms_sla_alert(tenant_id, waybill_no, promised_time);

CREATE INDEX IF NOT EXISTS idx_tms_sla_alert_status
    ON tms_sla_alert(tenant_id, status, level);

-- ----------------------------------------------------------------------
-- 异常件：责任方认领与处理状态机
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tms_exception_case (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID NOT NULL,
    waybill_id        UUID NOT NULL,
    waybill_no        VARCHAR(128) NOT NULL,
    exception_type    VARCHAR(64) NOT NULL,
    description       VARCHAR(512),
    responsible_party VARCHAR(32),
    status            VARCHAR(16) NOT NULL DEFAULT 'NEW',
    claimed_by        VARCHAR(64),
    claimed_time      TIMESTAMP,
    resolution        VARCHAR(512),
    resolved_time     TIMESTAMP,
    create_by         VARCHAR(64),
    create_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark            VARCHAR(512),
    CONSTRAINT chk_exception_case_status
        CHECK (status IN ('NEW', 'CLAIMED', 'PROCESSING', 'RESOLVED', 'CLOSED')),
    CONSTRAINT chk_exception_party
        CHECK (responsible_party IS NULL OR responsible_party IN ('CARRIER', 'WAREHOUSE', 'CUSTOMER', 'PLATFORM'))
);

COMMENT ON TABLE tms_exception_case IS '异常件单据：认领、责任方与处理闭环';

CREATE INDEX IF NOT EXISTS idx_tms_exception_case_waybill
    ON tms_exception_case(tenant_id, waybill_no, status);

-- ----------------------------------------------------------------------
-- 理赔单据：金额与审批/赔付状态机
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tms_claim_order (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID NOT NULL,
    case_id         UUID NOT NULL,
    waybill_no      VARCHAR(128) NOT NULL,
    claim_amount    DECIMAL(12, 2) NOT NULL,
    currency        VARCHAR(8) NOT NULL DEFAULT 'CNY',
    status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    reason          VARCHAR(512),
    evidence_url    VARCHAR(512),
    submitted_by    VARCHAR(64),
    submitted_time  TIMESTAMP,
    reviewed_by     VARCHAR(64),
    reviewed_time   TIMESTAMP,
    paid_time       TIMESTAMP,
    create_by       VARCHAR(64),
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    remark          VARCHAR(512),
    CONSTRAINT chk_claim_status
        CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'PAID')),
    CONSTRAINT chk_claim_amount CHECK (claim_amount > 0)
);

COMMENT ON TABLE tms_claim_order IS '理赔单据：金额、证据与审批/赔付状态';

CREATE INDEX IF NOT EXISTS idx_tms_claim_order_case
    ON tms_claim_order(tenant_id, case_id, status);

-- ----------------------------------------------------------------------
-- 承运商派发记录：下单幂等键 + 降级结果留痕
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tms_carrier_dispatch (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id          UUID NOT NULL,
    idempotency_key    VARCHAR(64) NOT NULL,
    waybill_no         VARCHAR(128),
    primary_carrier_id UUID,
    actual_carrier_id  UUID,
    attempt_count      INTEGER NOT NULL DEFAULT 0,
    result             VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    error_message      VARCHAR(512),
    create_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dispatch_result
        CHECK (result IN ('RUNNING', 'SUCCESS', 'FALLBACK', 'FAILED'))
);

COMMENT ON TABLE tms_carrier_dispatch IS '承运商下单派发：幂等键防重复下单，记录降级承运商';
COMMENT ON COLUMN tms_carrier_dispatch.result IS 'SUCCESS 主承运商成功；FALLBACK 降级成功；FAILED 全部失败';

CREATE UNIQUE INDEX IF NOT EXISTS uk_tms_carrier_dispatch_idem
    ON tms_carrier_dispatch(tenant_id, idempotency_key);

COMMIT;
