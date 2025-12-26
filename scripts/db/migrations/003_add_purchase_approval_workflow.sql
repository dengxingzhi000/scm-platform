-- ======================================================================
-- 采购审批流程扩展脚本
-- 为 db_supplier 添加采购申请和审批流程功能
-- 流程：采购申请 → 多级审批 → 生成采购单
-- ======================================================================

-- 连接到 db_supplier 数据库
-- \c db_supplier

-- ======================================================================
-- 1. 采购申请单表 (purchase_request)
-- ======================================================================
CREATE TABLE IF NOT EXISTS purchase_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    request_no VARCHAR(128) NOT NULL,

    -- 申请信息
    request_type SMALLINT NOT NULL DEFAULT 1
        CONSTRAINT chk_request_type CHECK (request_type IN (1, 2, 3)),
    urgency_level SMALLINT DEFAULT 1
        CONSTRAINT chk_urgency_level CHECK (urgency_level IN (1, 2, 3)),

    -- 申请人
    requester_id UUID NOT NULL,
    requester_name VARCHAR(128) NOT NULL,
    requester_dept VARCHAR(128),

    -- 预计总金额
    estimated_amount DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_request_estimated CHECK (estimated_amount >= 0),

    -- 期望交付日期
    expected_delivery_date DATE,

    -- 采购原因
    purchase_reason TEXT NOT NULL,
    justification TEXT,                             -- 采购说明

    -- 附件
    attachments JSONB DEFAULT '[]',                 -- 附件URL数组

    -- 状态
    status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_request_status CHECK (status IN (0, 1, 2, 3, 4, 5)),

    -- 当前审批节点
    current_approval_node INT DEFAULT 1,
    current_approver_id UUID,
    current_approver_name VARCHAR(128),

    -- 审批结果
    approved_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    reject_reason TEXT,

    -- 采购单生成
    purchase_order_id UUID,                         -- 关联生成的采购单
    purchase_order_no VARCHAR(128),
    converted_at TIMESTAMPTZ,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT,

    CONSTRAINT uk_tenant_request_no UNIQUE (tenant_id, request_no)
);

CREATE INDEX idx_request_tenant ON purchase_request(tenant_id, status) WHERE NOT deleted;
CREATE INDEX idx_request_requester ON purchase_request(tenant_id, requester_id) WHERE NOT deleted;
CREATE INDEX idx_request_approver ON purchase_request(tenant_id, current_approver_id) WHERE status IN (1, 2);
CREATE INDEX idx_request_created ON purchase_request(create_time DESC);

COMMENT ON TABLE purchase_request IS '采购申请单表';
COMMENT ON COLUMN purchase_request.request_type IS '申请类型:1-常规采购,2-紧急采购,3-零星采购';
COMMENT ON COLUMN purchase_request.urgency_level IS '紧急程度:1-普通,2-紧急,3-特急';
COMMENT ON COLUMN purchase_request.status IS '状态:0-草稿,1-待审批,2-审批中,3-已批准,4-已拒绝,5-已转采购单';

-- ======================================================================
-- 2. 采购申请明细表 (purchase_request_item)
-- ======================================================================
CREATE TABLE IF NOT EXISTS purchase_request_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    request_id UUID NOT NULL,
    request_no VARCHAR(128) NOT NULL,

    -- SKU信息
    sku_id UUID,
    sku_code VARCHAR(128),
    sku_name VARCHAR(256) NOT NULL,
    sku_spec VARCHAR(256),                          -- SKU规格

    -- 数量
    request_quantity INT NOT NULL
        CONSTRAINT chk_request_item_quantity CHECK (request_quantity > 0),
    approved_quantity INT DEFAULT 0
        CONSTRAINT chk_approved_quantity CHECK (approved_quantity >= 0),

    -- 预估单价
    estimated_unit_price DECIMAL(12, 2)
        CONSTRAINT chk_request_item_price CHECK (estimated_unit_price IS NULL OR estimated_unit_price >= 0),
    estimated_total DECIMAL(15, 2)
        CONSTRAINT chk_request_item_total CHECK (estimated_total IS NULL OR estimated_total >= 0),

    -- 推荐供应商
    recommended_supplier_id UUID,
    recommended_supplier_name VARCHAR(256),

    -- 用途说明
    usage_description TEXT,

    -- 质量要求
    quality_requirement TEXT,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT,

    CONSTRAINT fk_request_item FOREIGN KEY (request_id) REFERENCES purchase_request(id)
);

CREATE INDEX idx_request_item_request ON purchase_request_item(tenant_id, request_id);
CREATE INDEX idx_request_item_sku ON purchase_request_item(tenant_id, sku_id) WHERE sku_id IS NOT NULL;

COMMENT ON TABLE purchase_request_item IS '采购申请明细表';

-- ======================================================================
-- 3. 采购审批记录表 (purchase_request_approval)
-- ======================================================================
CREATE TABLE IF NOT EXISTS purchase_request_approval (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    request_id UUID NOT NULL,
    request_no VARCHAR(128) NOT NULL,

    -- 审批节点
    approval_node INT NOT NULL,                     -- 审批节点序号
    node_name VARCHAR(128),                         -- 节点名称（如：部门经理审批、总经理审批）

    -- 审批人
    approver_id UUID NOT NULL,
    approver_name VARCHAR(128) NOT NULL,
    approver_role VARCHAR(128),

    -- 审批动作
    approval_action SMALLINT NOT NULL
        CONSTRAINT chk_approval_action CHECK (approval_action IN (1, 2, 3, 4)),

    -- 审批时间
    approval_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    -- 审批意见
    approval_comment TEXT,

    -- 数量调整（可选）
    quantity_adjustments JSONB,                     -- [{itemId, originalQty, approvedQty, reason}]

    -- 签名/凭证
    signature VARCHAR(512),                         -- 电子签名图片URL

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    remark TEXT,

    CONSTRAINT fk_approval_request FOREIGN KEY (request_id) REFERENCES purchase_request(id)
);

CREATE INDEX idx_approval_request ON purchase_request_approval(tenant_id, request_id, approval_node);
CREATE INDEX idx_approval_approver ON purchase_request_approval(tenant_id, approver_id, approval_time DESC);
CREATE INDEX idx_approval_time ON purchase_request_approval(approval_time DESC);

COMMENT ON TABLE purchase_request_approval IS '采购审批记录表';
COMMENT ON COLUMN purchase_request_approval.approval_action IS '审批动作:1-同意,2-拒绝,3-转审,4-退回';

-- ======================================================================
-- 4. 审批流程配置表 (purchase_approval_config)
-- ======================================================================
CREATE TABLE IF NOT EXISTS purchase_approval_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    config_name VARCHAR(128) NOT NULL,

    -- 触发条件
    amount_from DECIMAL(15, 2) DEFAULT 0
        CONSTRAINT chk_approval_amount_from CHECK (amount_from >= 0),
    amount_to DECIMAL(15, 2)
        CONSTRAINT chk_approval_amount_to CHECK (amount_to IS NULL OR amount_to > amount_from),
    urgency_level SMALLINT
        CONSTRAINT chk_approval_urgency CHECK (urgency_level IS NULL OR urgency_level IN (1, 2, 3)),

    -- 审批节点配置（JSONB）
    approval_nodes JSONB NOT NULL,
    -- 示例：[
    --   {node: 1, name: "部门经理审批", roleId: "xxx", required: true},
    --   {node: 2, name: "采购经理审批", roleId: "yyy", required: true},
    --   {node: 3, name: "总经理审批", roleId: "zzz", required: false}
    -- ]

    -- 自动审批规则
    auto_approve_enabled BOOLEAN DEFAULT FALSE,
    auto_approve_amount DECIMAL(15, 2)
        CONSTRAINT chk_auto_approve_amount CHECK (auto_approve_amount IS NULL OR auto_approve_amount >= 0),

    -- 状态
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT DEFAULT 0,

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    create_by UUID,
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_by UUID,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    remark TEXT
);

CREATE INDEX idx_approval_config_tenant ON purchase_approval_config(tenant_id, enabled) WHERE NOT deleted;
CREATE INDEX idx_approval_config_amount ON purchase_approval_config(amount_from, amount_to) WHERE enabled;

COMMENT ON TABLE purchase_approval_config IS '采购审批流程配置表';
COMMENT ON COLUMN purchase_approval_config.approval_nodes IS '审批节点配置（JSONB数组）';

-- ======================================================================
-- 5. 审批待办表 (purchase_approval_task)
-- ======================================================================
CREATE TABLE IF NOT EXISTS purchase_approval_task (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    -- 申请单
    request_id UUID NOT NULL,
    request_no VARCHAR(128) NOT NULL,

    -- 审批节点
    approval_node INT NOT NULL,
    node_name VARCHAR(128),

    -- 审批人
    approver_id UUID NOT NULL,
    approver_name VARCHAR(128),

    -- 任务状态
    task_status SMALLINT NOT NULL DEFAULT 0
        CONSTRAINT chk_task_status CHECK (task_status IN (0, 1, 2, 3)),

    -- 超时控制
    deadline TIMESTAMPTZ,
    is_overdue BOOLEAN DEFAULT FALSE,

    -- 提醒
    reminder_count INT DEFAULT 0,
    last_reminder_at TIMESTAMPTZ,

    -- 完成信息
    completed_at TIMESTAMPTZ,
    approval_action SMALLINT
        CONSTRAINT chk_task_approval_action CHECK (approval_action IS NULL OR approval_action IN (1, 2, 3, 4)),

    -- 元数据
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_task_request FOREIGN KEY (request_id) REFERENCES purchase_request(id)
);

CREATE INDEX idx_task_tenant ON purchase_approval_task(tenant_id, task_status);
CREATE INDEX idx_task_approver ON purchase_approval_task(tenant_id, approver_id, task_status) WHERE task_status = 0;
CREATE INDEX idx_task_request ON purchase_approval_task(tenant_id, request_id);
CREATE INDEX idx_task_overdue ON purchase_approval_task(tenant_id) WHERE is_overdue = TRUE AND task_status = 0;

COMMENT ON TABLE purchase_approval_task IS '采购审批待办任务表';
COMMENT ON COLUMN purchase_approval_task.task_status IS '任务状态:0-待处理,1-已完成,2-已转审,3-已取消';

-- ======================================================================
-- 视图：我的待审批
-- ======================================================================
CREATE OR REPLACE VIEW v_my_approval_tasks AS
SELECT
    t.id AS task_id,
    t.tenant_id,
    t.approver_id,
    t.approver_name,
    r.id AS request_id,
    r.request_no,
    r.requester_name,
    r.estimated_amount,
    r.purchase_reason,
    r.urgency_level,
    t.approval_node,
    t.node_name,
    t.deadline,
    t.is_overdue,
    r.create_time AS request_time,
    t.create_time AS task_create_time
FROM purchase_approval_task t
INNER JOIN purchase_request r ON t.request_id = r.id
WHERE t.task_status = 0  -- 待处理
  AND NOT r.deleted
ORDER BY t.is_overdue DESC, r.urgency_level DESC, r.create_time DESC;

COMMENT ON VIEW v_my_approval_tasks IS '我的待审批任务视图';

-- ======================================================================
-- 函数：提交采购申请（触发审批流程）
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_submit_purchase_request(
    p_request_id UUID,
    p_operator_id UUID
)
RETURNS JSONB AS $$
DECLARE
    v_request RECORD;
    v_config RECORD;
    v_node JSONB;
    v_approver_role_id UUID;
BEGIN
    -- 获取申请单
    SELECT * INTO v_request
    FROM purchase_request
    WHERE id = p_request_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'REQUEST_NOT_FOUND');
    END IF;

    IF v_request.status != 0 THEN
        RETURN jsonb_build_object('success', false, 'error', 'INVALID_STATUS');
    END IF;

    -- 查找匹配的审批流程配置
    SELECT * INTO v_config
    FROM purchase_approval_config
    WHERE tenant_id = v_request.tenant_id
      AND enabled = TRUE
      AND v_request.estimated_amount >= amount_from
      AND (amount_to IS NULL OR v_request.estimated_amount < amount_to)
      AND (urgency_level IS NULL OR urgency_level = v_request.urgency_level)
      AND NOT deleted
    ORDER BY sort_order ASC
    LIMIT 1;

    IF NOT FOUND THEN
        RETURN jsonb_build_object(
            'success', false,
            'error', 'NO_APPROVAL_CONFIG',
            'message', '未找到匹配的审批流程配置'
        );
    END IF;

    -- 检查是否自动审批
    IF v_config.auto_approve_enabled AND
       v_request.estimated_amount <= v_config.auto_approve_amount THEN
        -- 自动批准
        UPDATE purchase_request
        SET status = 3,
            approved_at = NOW(),
            current_approval_node = 0,
            update_time = NOW()
        WHERE id = p_request_id;

        RETURN jsonb_build_object(
            'success', true,
            'auto_approved', true,
            'message', '金额未超限，自动批准'
        );
    END IF;

    -- 获取第一个审批节点
    v_node := (v_config.approval_nodes->0);

    -- 更新申请单状态
    UPDATE purchase_request
    SET status = 1,
        current_approval_node = (v_node->>'node')::INT,
        update_time = NOW()
    WHERE id = p_request_id;

    -- 创建审批任务（TODO：需要根据role_id查找具体的审批人）
    INSERT INTO purchase_approval_task (
        tenant_id, request_id, request_no,
        approval_node, node_name,
        approver_id, approver_name,
        deadline
    ) VALUES (
        v_request.tenant_id,
        p_request_id,
        v_request.request_no,
        (v_node->>'node')::INT,
        v_node->>'name',
        p_operator_id,  -- TODO: 应该根据roleId查找
        'Approver',     -- TODO: 应该根据roleId查找
        NOW() + INTERVAL '3 days'
    );

    RETURN jsonb_build_object(
        'success', true,
        'approval_started', true,
        'current_node', (v_node->>'node')::INT,
        'node_name', v_node->>'name'
    );
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_submit_purchase_request IS '提交采购申请（启动审批流程）';

-- ======================================================================
-- 函数：审批采购申请
-- ======================================================================
CREATE OR REPLACE FUNCTION fn_approve_purchase_request(
    p_request_id UUID,
    p_approver_id UUID,
    p_approval_action SMALLINT,
    p_comment TEXT
)
RETURNS JSONB AS $$
DECLARE
    v_request RECORD;
    v_task RECORD;
    v_config RECORD;
    v_next_node JSONB;
BEGIN
    -- 获取申请单和待办任务
    SELECT * INTO v_request
    FROM purchase_request
    WHERE id = p_request_id;

    SELECT * INTO v_task
    FROM purchase_approval_task
    WHERE request_id = p_request_id
      AND approver_id = p_approver_id
      AND task_status = 0;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'TASK_NOT_FOUND');
    END IF;

    -- 记录审批历史
    INSERT INTO purchase_request_approval (
        tenant_id, request_id, request_no,
        approval_node, node_name,
        approver_id, approver_name,
        approval_action, approval_comment
    ) VALUES (
        v_request.tenant_id, p_request_id, v_request.request_no,
        v_task.approval_node, v_task.node_name,
        p_approver_id, v_task.approver_name,
        p_approval_action, p_comment
    );

    -- 更新待办任务
    UPDATE purchase_approval_task
    SET task_status = 1,
        completed_at = NOW(),
        approval_action = p_approval_action,
        update_time = NOW()
    WHERE id = v_task.id;

    -- 根据审批动作处理
    IF p_approval_action = 2 THEN  -- 拒绝
        UPDATE purchase_request
        SET status = 4,
            rejected_at = NOW(),
            reject_reason = p_comment,
            update_time = NOW()
        WHERE id = p_request_id;

        RETURN jsonb_build_object('success', true, 'result', 'REJECTED');
    ELSIF p_approval_action = 1 THEN  -- 同意
        -- 查找流程配置，判断是否还有下一个节点
        SELECT * INTO v_config
        FROM purchase_approval_config
        WHERE tenant_id = v_request.tenant_id
          AND enabled = TRUE
          AND v_request.estimated_amount >= amount_from
          AND (amount_to IS NULL OR v_request.estimated_amount < amount_to)
          AND NOT deleted
        ORDER BY sort_order ASC
        LIMIT 1;

        -- 查找下一个节点
        v_next_node := NULL;
        FOR i IN 0..jsonb_array_length(v_config.approval_nodes)-1 LOOP
            IF ((v_config.approval_nodes->i)->>'node')::INT > v_task.approval_node THEN
                v_next_node := v_config.approval_nodes->i;
                EXIT;
            END IF;
        END LOOP;

        IF v_next_node IS NULL THEN
            -- 所有节点已批准
            UPDATE purchase_request
            SET status = 3,
                approved_at = NOW(),
                update_time = NOW()
            WHERE id = p_request_id;

            RETURN jsonb_build_object('success', true, 'result', 'APPROVED');
        ELSE
            -- 进入下一审批节点
            UPDATE purchase_request
            SET status = 2,
                current_approval_node = (v_next_node->>'node')::INT,
                update_time = NOW()
            WHERE id = p_request_id;

            -- TODO: 创建下一节点的待办任务

            RETURN jsonb_build_object(
                'success', true,
                'result', 'NEXT_NODE',
                'next_node', (v_next_node->>'node')::INT
            );
        END IF;
    END IF;

    RETURN jsonb_build_object('success', true);
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION fn_approve_purchase_request IS '审批采购申请';

-- ======================================================================
-- 注意事项：
-- 1. 审批流程配置需要与组织架构和角色系统配合
-- 2. 审批人员应从用户权限系统中根据角色查询
-- 3. 审批超时需要定时任务扫描并发送提醒
-- 4. 采购申请批准后，需要手动或自动生成采购单
-- 5. 支持审批退回、转审等复杂流程
-- 6. 建议集成工作流引擎（如Activiti/Camunda）实现更复杂的审批流
-- ======================================================================