-- ======================================================================
-- 本地开发种子数据 014 — 仓库 / 入库 / 出库
-- 目标库：db_warehouse
-- ======================================================================
BEGIN;

ALTER TABLE wms_warehouse     ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE wms_inbound       ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE wms_inbound_item  ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE wms_inbound_item  ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE wms_outbound      ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE wms_outbound_item ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE wms_outbound_item ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO wms_warehouse (
    id, tenant_id, warehouse_code, warehouse_name, warehouse_type,
    province, city, district, address, manager_name, phone, email,
    total_capacity, used_capacity, enabled, sort_order,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000601', '019a0aee-0000-7000-8000-000000000002', 'WH-001', '主仓库', 1, '上海市', '上海市', '浦东新区', '张江高科技园区 100 号', '张仓管', '13800000001', 'wh001@scm.local', 100000, 32000, TRUE, 10, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000602', '019a0aee-0000-7000-8000-000000000002', 'WH-002', '华东仓', 2, '浙江省', '杭州市', '余杭区', '未来科技城 200 号', '李仓管', '13800000002', 'wh002@scm.local', 60000, 18000, TRUE, 20, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000603', '019a0aee-0000-7000-8000-000000000002', 'WH-003', '华南仓', 2, '广东省', '广州市', '黄埔区', '科学城 300 号', '王仓管', '13800000003', 'wh003@scm.local', 50000, 12000, TRUE, 30, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000604', '019a0aee-0000-7000-8000-000000000002', 'WH-004', '华北仓', 2, '北京市', '北京市', '大兴区', '亦庄经济开发区 400 号', '赵仓管', '13800000004', 'wh004@scm.local', 40000, 9000, TRUE, 40, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000605', '019a0aee-0000-7000-8000-000000000002', 'WH-005', '西南仓', 3, '四川省', '成都市', '双流区', '空港经济区 500 号', '周仓管', '13800000005', 'wh005@scm.local', 20000, 0, FALSE, 50, NOW(), NOW(), FALSE, '本地开发种子数据')
ON CONFLICT (id) DO UPDATE SET
    warehouse_name = EXCLUDED.warehouse_name,
    tenant_id = EXCLUDED.tenant_id,
    manager_name = EXCLUDED.manager_name,
    phone = EXCLUDED.phone,
    enabled = EXCLUDED.enabled,
    update_time = NOW();

INSERT INTO wms_inbound (
    id, tenant_id, inbound_no, warehouse_id, inbound_type, source_type, source_id, source_no,
    supplier_id, status, total_quantity, received_quantity,
    expected_at, started_at, completed_at, operator_id, operator_name,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000611', '019a0aee-0000-7000-8000-000000000002', 'IN-2026-1001', '019a0aee-0000-7000-8000-000000000601', 1, 'PURCHASE', '019a0aee-0000-7000-8000-000000000801', 'PO-2026-1001', '019a0aee-0000-7000-8000-000000000701', 3, 100, 100, NOW() - INTERVAL '8 days', NOW() - INTERVAL '9 days', NOW() - INTERVAL '8 days', '019a0aee-0000-7000-8000-000000000101', '张仓管', NOW() - INTERVAL '10 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000612', '019a0aee-0000-7000-8000-000000000002', 'IN-2026-1002', '019a0aee-0000-7000-8000-000000000601', 1, 'PURCHASE', '019a0aee-0000-7000-8000-000000000802', 'PO-2026-1002', '019a0aee-0000-7000-8000-000000000701', 1, 50, 30, NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days', NULL, '019a0aee-0000-7000-8000-000000000101', '张仓管', NOW() - INTERVAL '4 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000613', '019a0aee-0000-7000-8000-000000000002', 'IN-2026-1003', '019a0aee-0000-7000-8000-000000000602', 1, 'PURCHASE', '019a0aee-0000-7000-8000-000000000803', 'PO-2026-1003', '019a0aee-0000-7000-8000-000000000702', 0, 80, 0, NOW() + INTERVAL '2 days', NULL, NULL, '019a0aee-0000-7000-8000-000000000102', '李仓管', NOW() - INTERVAL '1 day', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000614', '019a0aee-0000-7000-8000-000000000002', 'IN-2026-1004', '019a0aee-0000-7000-8000-000000000603', 2, 'RETURN', NULL, NULL, '019a0aee-0000-7000-8000-000000000704', 2, 200, 180, NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NULL, '019a0aee-0000-7000-8000-000000000103', '王仓管', NOW() - INTERVAL '3 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000615', '019a0aee-0000-7000-8000-000000000002', 'IN-2026-1005', '019a0aee-0000-7000-8000-000000000604', 1, 'PURCHASE', NULL, NULL, '019a0aee-0000-7000-8000-000000000705', 3, 120, 120, NOW() - INTERVAL '5 days', NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days', '019a0aee-0000-7000-8000-000000000104', '赵仓管', NOW() - INTERVAL '7 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000616', '019a0aee-0000-7000-8000-000000000002', 'IN-2026-1006', '019a0aee-0000-7000-8000-000000000601', 4, 'OTHER', NULL, NULL, NULL, 4, 40, 0, NOW() - INTERVAL '6 days', NULL, NULL, '019a0aee-0000-7000-8000-000000000101', '张仓管', NOW() - INTERVAL '6 days', NOW(), FALSE, '本地开发种子数据')
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status,
    tenant_id = EXCLUDED.tenant_id,
    received_quantity = EXCLUDED.received_quantity,
    update_time = NOW();

INSERT INTO wms_inbound_item (
    id, tenant_id, inbound_id, inbound_no, sku_id, sku_code, sku_name,
    location_code, plan_quantity, actual_quantity, quality_status,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000621', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000611', 'IN-2026-1001', '019a0aee-0000-7000-8000-000000000501', 'SKU-IP15P-128', 'iPhone 15 Pro 128G', 'A01-01-01', 100, 100, 1, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000622', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000612', 'IN-2026-1002', '019a0aee-0000-7000-8000-000000000503', 'SKU-MBA-M3-256', 'MacBook Air M3 256G', 'A01-02-01', 50, 30, 1, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000623', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000613', 'IN-2026-1003', '019a0aee-0000-7000-8000-000000000504', 'SKU-S24U-512', 'Galaxy S24 Ultra 512G', 'B01-01-01', 80, 0, 2, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000624', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000614', 'IN-2026-1004', '019a0aee-0000-7000-8000-000000000505', 'SKU-APP2', 'AirPods Pro 2', 'C01-01-01', 200, 180, 1, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000625', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000615', 'IN-2026-1005', '019a0aee-0000-7000-8000-000000000506', 'SKU-MI14U-512', '小米 14 Ultra 512G', 'D01-01-01', 120, 120, 1, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000626', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000616', 'IN-2026-1006', '019a0aee-0000-7000-8000-000000000507', 'SKU-X1C-16-512', 'ThinkPad X1 Carbon 16G/512G', 'A02-01-01', 40, 0, 2, NOW(), NOW(), FALSE, NULL)
ON CONFLICT (id) DO NOTHING;

INSERT INTO wms_outbound (
    id, tenant_id, outbound_no, warehouse_id, outbound_type, source_type, source_id, source_no,
    priority, status, total_quantity, picked_quantity,
    expected_at, started_at, completed_at, picker_id, picker_name,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000631', '019a0aee-0000-7000-8000-000000000002', 'OUT-2026-1001', '019a0aee-0000-7000-8000-000000000601', 1, 'ORDER', '019a0aee-0000-7000-8000-000000000801', 'ORD-2026-1001', 1, 3, 2, 2, NOW() - INTERVAL '9 days', NOW() - INTERVAL '10 days', NOW() - INTERVAL '9 days', '019a0aee-0000-7000-8000-000000000101', '张仓管', NOW() - INTERVAL '10 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000632', '019a0aee-0000-7000-8000-000000000002', 'OUT-2026-1002', '019a0aee-0000-7000-8000-000000000601', 1, 'ORDER', '019a0aee-0000-7000-8000-000000000802', 'ORD-2026-1002', 1, 1, 1, 0, NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days', NULL, '019a0aee-0000-7000-8000-000000000101', '张仓管', NOW() - INTERVAL '3 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000633', '019a0aee-0000-7000-8000-000000000002', 'OUT-2026-1003', '019a0aee-0000-7000-8000-000000000602', 1, 'ORDER', '019a0aee-0000-7000-8000-000000000803', 'ORD-2026-1003', 2, 2, 4, 4, NOW() - INTERVAL '1 day', NOW() - INTERVAL '2 days', NULL, '019a0aee-0000-7000-8000-000000000102', '李仓管', NOW() - INTERVAL '2 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000634', '019a0aee-0000-7000-8000-000000000002', 'OUT-2026-1004', '019a0aee-0000-7000-8000-000000000603', 1, 'ORDER', '019a0aee-0000-7000-8000-000000000804', 'ORD-2026-1004', 1, 1, 10, 0, NOW() + INTERVAL '1 day', NULL, NULL, '019a0aee-0000-7000-8000-000000000103', '王仓管', NOW() - INTERVAL '1 day', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000635', '019a0aee-0000-7000-8000-000000000002', 'OUT-2026-1005', '019a0aee-0000-7000-8000-000000000604', 1, 'ORDER', '019a0aee-0000-7000-8000-000000000805', 'ORD-2026-1005', 1, 0, 3, 0, NOW() + INTERVAL '2 days', NULL, NULL, '019a0aee-0000-7000-8000-000000000104', '赵仓管', NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000636', '019a0aee-0000-7000-8000-000000000002', 'OUT-2026-1006', '019a0aee-0000-7000-8000-000000000601', 2, 'TRANSFER', NULL, NULL, 1, 4, 5, 0, NOW() - INTERVAL '6 days', NULL, NULL, '019a0aee-0000-7000-8000-000000000101', '张仓管', NOW() - INTERVAL '6 days', NOW(), FALSE, '本地开发种子数据')
ON CONFLICT (id) DO UPDATE SET
    status = EXCLUDED.status,
    tenant_id = EXCLUDED.tenant_id,
    picked_quantity = EXCLUDED.picked_quantity,
    update_time = NOW();

INSERT INTO wms_outbound_item (
    id, tenant_id, outbound_id, outbound_no, sku_id, sku_code, sku_name,
    location_code, plan_quantity, actual_quantity,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000641', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000631', 'OUT-2026-1001', '019a0aee-0000-7000-8000-000000000501', 'SKU-IP15P-128', 'iPhone 15 Pro 128G', 'A01-01-01', 2, 2, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000642', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000632', 'OUT-2026-1002', '019a0aee-0000-7000-8000-000000000503', 'SKU-MBA-M3-256', 'MacBook Air M3 256G', 'A01-02-01', 1, 0, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000643', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000633', 'OUT-2026-1003', '019a0aee-0000-7000-8000-000000000504', 'SKU-S24U-512', 'Galaxy S24 Ultra 512G', 'B01-01-01', 4, 4, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000644', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000634', 'OUT-2026-1004', '019a0aee-0000-7000-8000-000000000505', 'SKU-APP2', 'AirPods Pro 2', 'C01-01-01', 10, 0, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000645', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000635', 'OUT-2026-1005', '019a0aee-0000-7000-8000-000000000508', 'SKU-IPADAIR11-128', 'iPad Air 11 128G', 'A03-01-01', 3, 0, NOW(), NOW(), FALSE, NULL),
    ('019a0aee-0000-7000-8000-000000000646', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000636', 'OUT-2026-1006', '019a0aee-0000-7000-8000-000000000506', 'SKU-MI14U-512', '小米 14 Ultra 512G', 'D01-01-01', 5, 0, NOW(), NOW(), FALSE, NULL)
ON CONFLICT (id) DO NOTHING;

COMMIT;
