-- ======================================================================
-- 本地开发种子数据 011 — 库存
-- 目标库：db_inventory
--
-- 覆盖正常、低库存、缺货三种状态，供仪表盘库存预警和库存页使用。
-- ======================================================================
BEGIN;

ALTER TABLE inv_inventory ADD COLUMN IF NOT EXISTS tenant_id UUID;

INSERT INTO inv_inventory (
    id, tenant_id, sku_id, warehouse_id,
    total_stock, available_stock, locked_stock, damaged_stock,
    safety_stock, max_stock, location_code, average_cost, version,
    last_inbound_at, last_outbound_at, create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000901', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000501', '019a0aee-0000-7000-8000-000000000601', 120, 120, 0, 0, 20, 500, 'A01-01-01', 8200, 0, NOW() - INTERVAL '8 days', NOW() - INTERVAL '2 days', NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000902', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000502', '019a0aee-0000-7000-8000-000000000601', 0, 0, 0, 0, 10, 300, 'A01-01-02', 9000, 0, NOW() - INTERVAL '30 days', NOW() - INTERVAL '3 days', NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000903', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000503', '019a0aee-0000-7000-8000-000000000601', 8, 8, 0, 0, 10, 100, 'A01-02-01', 7600, 0, NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day', NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000904', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000504', '019a0aee-0000-7000-8000-000000000602', 45, 45, 0, 0, 10, 200, 'B01-01-01', 6000, 0, NOW() - INTERVAL '20 days', NOW() - INTERVAL '4 days', NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000905', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000505', '019a0aee-0000-7000-8000-000000000601', 3, 3, 0, 0, 20, 300, 'C01-01-01', 1400, 0, NOW() - INTERVAL '5 days', NOW() - INTERVAL '1 day', NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000906', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000506', '019a0aee-0000-7000-8000-000000000603', 0, 0, 0, 0, 10, 200, 'D01-01-01', 5600, 0, NOW() - INTERVAL '25 days', NOW() - INTERVAL '6 days', NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000907', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000507', '019a0aee-0000-7000-8000-000000000601', 25, 25, 0, 0, 5, 100, 'A02-01-01', 11200, 0, NOW() - INTERVAL '12 days', NOW() - INTERVAL '7 days', NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000908', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000508', '019a0aee-0000-7000-8000-000000000604', 60, 60, 0, 0, 10, 200, 'A03-01-01', 4000, 0, NOW() - INTERVAL '9 days', NOW() - INTERVAL '5 days', NOW(), NOW(), FALSE, '本地开发种子数据')
ON CONFLICT (sku_id, warehouse_id) DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    total_stock = EXCLUDED.total_stock,
    available_stock = EXCLUDED.available_stock,
    locked_stock = EXCLUDED.locked_stock,
    damaged_stock = EXCLUDED.damaged_stock,
    safety_stock = EXCLUDED.safety_stock,
    average_cost = EXCLUDED.average_cost,
    update_time = NOW(),
    deleted = FALSE;

COMMIT;
