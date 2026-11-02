-- ======================================================================
-- 本地开发种子数据 005 — 决策矩阵（规则 / 权重方案）
-- 目标库：db_permission
--
-- 覆盖前端 ENGINE_TYPES 的三种引擎：SCORING / NEAREST_NEIGHBOR / GREEDY。
-- 每个引擎都有若干规则与权重方案；权重各维度之和为 1.0（与 WeightProfile.validate()
-- 的约束一致），默认场景之外的 FLASH_SALE 场景用于演示场景筛选与 conditions。
--
-- 幂等：固定 id + ON CONFLICT 更新；可重复执行。
-- ======================================================================
BEGIN;

SET client_encoding TO 'UTF8';

-- ----------------------------------------------------------------------
-- 1. 决策规则（expression 为 SpEL，rule_type 供冲突检测区分用途）
-- ----------------------------------------------------------------------
INSERT INTO sys_decision_rule (
    id, tenant_id, engine_type, scene, rule_type, expression, description,
    enabled, priority, version, deleted, create_time, update_time
) VALUES
    -- SCORING：评分引擎
    ('dec-rule-scoring-amount',   '019a0aee-0000-7000-8000-000000000002', 'SCORING', NULL, 'SCORING',
     '#orderAmount >= 1000 ? 1.0 : #orderAmount / 1000.0', '订单金额', TRUE, 10, 1, FALSE, NOW(), NOW()),
    ('dec-rule-scoring-level',    '019a0aee-0000-7000-8000-000000000002', 'SCORING', NULL, 'SCORING',
     '#customerLevel * 0.2 + 0.2', '客户等级', TRUE, 20, 1, FALSE, NOW(), NOW()),
    ('dec-rule-scoring-distance', '019a0aee-0000-7000-8000-000000000002', 'SCORING', NULL, 'SCORING',
     '#distanceKm <= 50 ? 1.0 : 50.0 / #distanceKm', '履约距离', TRUE, 30, 1, FALSE, NOW(), NOW()),
    ('dec-rule-scoring-blacklist','019a0aee-0000-7000-8000-000000000002', 'SCORING', NULL, 'FILTER',
     '!#blacklisted', '黑名单拦截', TRUE, 5, 1, FALSE, NOW(), NOW()),
    ('dec-rule-scoring-stock',    '019a0aee-0000-7000-8000-000000000002', 'SCORING', 'FLASH_SALE', 'FILTER',
     '#availableStock >= #requiredQty', '秒杀库存校验', TRUE, 5, 1, FALSE, NOW(), NOW()),

    -- NEAREST_NEIGHBOR：最近邻引擎
    ('dec-rule-nn-distance',      '019a0aee-0000-7000-8000-000000000002', 'NEAREST_NEIGHBOR', NULL, 'SCORING',
     '1.0 / (1.0 + #distanceKm)', '距离衰减', TRUE, 10, 1, FALSE, NOW(), NOW()),
    ('dec-rule-nn-capacity',      '019a0aee-0000-7000-8000-000000000002', 'NEAREST_NEIGHBOR', NULL, 'FILTER',
     '#remainingCapacity >= #requiredQty', '运力可用', TRUE, 20, 1, FALSE, NOW(), NOW()),
    ('dec-rule-nn-service-area',  '019a0aee-0000-7000-8000-000000000002', 'NEAREST_NEIGHBOR', NULL, 'GUARD',
     '#serviceableArea.contains(#region)', '服务区域校验', FALSE, 30, 1, FALSE, NOW(), NOW()),

    -- GREEDY：贪心分配引擎
    ('dec-rule-greedy-fill',      '019a0aee-0000-7000-8000-000000000002', 'GREEDY', NULL, 'SCORING',
     '#remainingVolume / #vehicleVolume', '装载率优先', TRUE, 10, 1, FALSE, NOW(), NOW()),
    ('dec-rule-greedy-hazmat',    '019a0aee-0000-7000-8000-000000000002', 'GREEDY', NULL, 'FILTER',
     '!#hazmat || #vehicleHazmatCertified', '危品资质校验', TRUE, 20, 1, FALSE, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    engine_type = EXCLUDED.engine_type,
    scene = EXCLUDED.scene,
    rule_type = EXCLUDED.rule_type,
    expression = EXCLUDED.expression,
    description = EXCLUDED.description,
    enabled = EXCLUDED.enabled,
    priority = EXCLUDED.priority,
    version = EXCLUDED.version,
    deleted = FALSE,
    update_time = NOW();

-- ----------------------------------------------------------------------
-- 2. 权重方案（同一引擎 + 场景只允许一个 active，由 activate 接口维护；
--    这里按版本递减给出历史版本）
-- ----------------------------------------------------------------------
INSERT INTO sys_weight_profile (
    id, tenant_id, engine_type, scene, version, weights, conditions,
    active, deleted, create_time
) VALUES
    ('dec-weight-scoring-default-v1', '019a0aee-0000-7000-8000-000000000002', 'SCORING', NULL, 1,
     '{"amount": 0.4, "level": 0.3, "distance": 0.3}'::jsonb, NULL, FALSE, FALSE, NOW()),
    ('dec-weight-scoring-default-v2', '019a0aee-0000-7000-8000-000000000002', 'SCORING', NULL, 2,
     '{"amount": 0.5, "level": 0.3, "distance": 0.2}'::jsonb, NULL, TRUE, FALSE, NOW()),
    ('dec-weight-scoring-flash-v1',   '019a0aee-0000-7000-8000-000000000002', 'SCORING', 'FLASH_SALE', 1,
     '{"amount": 0.6, "level": 0.2, "distance": 0.2}'::jsonb,
     '[{"field": "channel", "operator": "==", "value": "APP"}]'::jsonb, TRUE, FALSE, NOW()),

    ('dec-weight-nn-default-v1', '019a0aee-0000-7000-8000-000000000002', 'NEAREST_NEIGHBOR', NULL, 1,
     '{"distance": 0.6, "stock": 0.25, "cost": 0.15}'::jsonb, NULL, TRUE, FALSE, NOW()),

    ('dec-weight-greedy-default-v1', '019a0aee-0000-7000-8000-000000000002', 'GREEDY', NULL, 1,
     '{"fillRate": 0.5, "cost": 0.3, "distance": 0.2}'::jsonb, NULL, TRUE, FALSE, NOW())
ON CONFLICT (id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    engine_type = EXCLUDED.engine_type,
    scene = EXCLUDED.scene,
    version = EXCLUDED.version,
    weights = EXCLUDED.weights,
    conditions = EXCLUDED.conditions,
    active = EXCLUDED.active,
    deleted = FALSE;

COMMIT;
