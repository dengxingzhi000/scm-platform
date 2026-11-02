-- ============================================================
-- 统一字典中心 + 状态字典 + 状态流转规则
-- 数据库: db_permission (与角色/权限同库)
-- ============================================================

\c db_permission

-- ============================================================
-- 1. 字典类型表 (sys_dict_type)
-- 用于: 单位、币种、仓库类型、产品分类、国家、地区、通知渠道、支付方式等
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_type (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id UUID,
    dict_code VARCHAR(64) NOT NULL,
    dict_name VARCHAR(128) NOT NULL,
    dict_name_en VARCHAR(128),
    description VARCHAR(512),
    status SMALLINT NOT NULL DEFAULT 1,
    is_system BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(36),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark VARCHAR(512)
);

-- 唯一索引: 租户内字典编码唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_dict_type_code
    ON sys_dict_type(tenant_id, dict_code) WHERE NOT deleted;

-- 索引
CREATE INDEX IF NOT EXISTS idx_dict_type_tenant ON sys_dict_type(tenant_id) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_dict_type_code ON sys_dict_type(dict_code) WHERE NOT deleted;

COMMENT ON TABLE sys_dict_type IS '字典类型表';
COMMENT ON COLUMN sys_dict_type.dict_code IS '字典编码 (如: currency, unit, warehouse_type)';
COMMENT ON COLUMN sys_dict_type.dict_name IS '字典名称 (中文)';
COMMENT ON COLUMN sys_dict_type.is_system IS '是否系统内置 (系统字典不可删除)';
COMMENT ON COLUMN sys_dict_type.status IS '状态: 0-禁用, 1-启用';

-- ============================================================
-- 2. 字典项表 (sys_dict_item)
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_item (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id UUID,
    dict_type_id VARCHAR(36) NOT NULL,
    dict_code VARCHAR(64) NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    item_name_en VARCHAR(128),
    item_value VARCHAR(256),
    css_class VARCHAR(128),
    label_class VARCHAR(64),
    icon VARCHAR(64),
    color VARCHAR(32),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(36),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark VARCHAR(512)
);

-- 唯一索引: 同一类型下项编码唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_dict_item_code
    ON sys_dict_item(tenant_id, dict_code, item_code) WHERE NOT deleted;

-- 索引
CREATE INDEX IF NOT EXISTS idx_dict_item_type ON sys_dict_item(dict_type_id) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_dict_item_code ON sys_dict_item(dict_code) WHERE NOT deleted;

COMMENT ON TABLE sys_dict_item IS '字典项表';
COMMENT ON COLUMN sys_dict_item.item_code IS '项编码 (如: CNY, USD, KG, PCS)';
COMMENT ON COLUMN sys_dict_item.item_name IS '项名称 (中文)';
COMMENT ON COLUMN sys_dict_item.item_value IS '项值 (可选，用于存储额外数据)';

-- ============================================================
-- 3. 状态字典表 (sys_status_dict)
-- 统一管理所有业务状态: 订单、采购、仓库、物流、审批等
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_status_dict (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id UUID,
    biz_type VARCHAR(32) NOT NULL,
    status_code VARCHAR(32) NOT NULL,
    status_name VARCHAR(64) NOT NULL,
    status_name_en VARCHAR(64),
    color VARCHAR(32),
    icon VARCHAR(64),
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_initial BOOLEAN NOT NULL DEFAULT FALSE,
    is_terminal BOOLEAN NOT NULL DEFAULT FALSE,
    is_cancellable BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(36),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark VARCHAR(512)
);

-- 唯一索引: 租户+业务类型+状态编码 唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_status_dict_code
    ON sys_status_dict(tenant_id, biz_type, status_code) WHERE NOT deleted;

-- 索引
CREATE INDEX IF NOT EXISTS idx_status_dict_biz ON sys_status_dict(biz_type) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_status_dict_tenant ON sys_status_dict(tenant_id) WHERE NOT deleted;

COMMENT ON TABLE sys_status_dict IS '状态字典表 — 统一管理所有业务状态';
COMMENT ON COLUMN sys_status_dict.biz_type IS '业务类型: ORDER, PURCHASE, INBOUND, OUTBOUND, TRANSFER, APPROVAL, LOGISTICS';
COMMENT ON COLUMN sys_status_dict.status_code IS '状态编码 (全局固定，租户不可修改): PENDING_PAYMENT, PAID, SHIPPED...';
COMMENT ON COLUMN sys_status_dict.status_name IS '状态名称 (租户可自定义): 待支付, 已支付, 待发货...';
COMMENT ON COLUMN sys_status_dict.is_initial IS '是否初始状态 (每个 biz_type 最多一个)';
COMMENT ON COLUMN sys_status_dict.is_terminal IS '是否终态 (如: COMPLETED, CANCELLED, REFUNDED)';

-- ============================================================
-- 4. 状态流转规则表 (sys_status_transition)
-- 定义合法的状态流转路径
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_status_transition (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id UUID,
    biz_type VARCHAR(32) NOT NULL,
    from_status VARCHAR(32) NOT NULL,
    to_status VARCHAR(32) NOT NULL,
    action_code VARCHAR(64) NOT NULL,
    action_name VARCHAR(128) NOT NULL,
    action_name_en VARCHAR(128),
    need_approval BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    condition_expression VARCHAR(512),
    pre_action VARCHAR(256),
    post_action VARCHAR(256),
    sort_order INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by VARCHAR(36),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(36),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark VARCHAR(512)
);

-- 唯一索引: 同一租户+业务类型下，from→to+action 唯一
CREATE UNIQUE INDEX IF NOT EXISTS uk_status_transition
    ON sys_status_transition(tenant_id, biz_type, from_status, to_status, action_code) WHERE NOT deleted;

-- 索引
CREATE INDEX IF NOT EXISTS idx_transition_biz ON sys_status_transition(biz_type) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_transition_from ON sys_status_transition(biz_type, from_status) WHERE NOT deleted;
CREATE INDEX IF NOT EXISTS idx_transition_to ON sys_status_transition(biz_type, to_status) WHERE NOT deleted;

COMMENT ON TABLE sys_status_transition IS '状态流转规则表';
COMMENT ON COLUMN sys_status_transition.action_code IS '动作编码: PAY, CANCEL, SHIP, APPROVE, REJECT...';
COMMENT ON COLUMN sys_status_transition.action_name IS '动作名称 (中文): 支付, 取消, 发货, 审批通过...';
COMMENT ON COLUMN sys_status_transition.need_approval IS '是否需要审批';
COMMENT ON COLUMN sys_status_transition.condition_expression IS '前置条件表达式 (SpEL): inventoryLocked == true && paidAmount > 0';
COMMENT ON COLUMN sys_status_transition.pre_action IS '前置动作 (SPI): 前置校验逻辑的 Bean 名称';
COMMENT ON COLUMN sys_status_transition.post_action IS '后置动作 (SPI): 如发送MQ、审计日志、通知';
