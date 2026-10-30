-- ======================================================================
-- 本地开发种子数据 017 — 采购订单
-- 目标库：db_purchase
-- ======================================================================
BEGIN;

DO $$
DECLARE
    start_date DATE;
    end_date DATE;
    partition_name TEXT;
BEGIN
    FOR i IN -1..1 LOOP
        start_date := (date_trunc('month', CURRENT_DATE) + (i || ' months')::interval)::date;
        end_date := (start_date + INTERVAL '1 month')::date;
        partition_name := 'pur_order_p' || to_char(start_date, 'YYYYMM');
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF pur_order FOR VALUES FROM (%L) TO (%L)',
            partition_name, start_date, end_date
        );
    END LOOP;
END $$;

DELETE FROM pur_order_item WHERE order_no LIKE 'PO-2026-10%';
DELETE FROM pur_order WHERE order_no LIKE 'PO-2026-10%';

INSERT INTO pur_order (
    id, tenant_id, order_no, supplier_id, supplier_name, supplier_contact, order_type,
    total_amount, tax_amount, freight_amount, discount_amount, payable_amount, status,
    warehouse_id, warehouse_name, warehouse_address, order_date, expected_delivery,
    buyer_id, buyer_name, approved_by, approved_by_name, approved_at,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000801', '019a0aee-0000-7000-8000-000000000002', 'PO-2026-1001', '019a0aee-0000-7000-8000-000000000701', 'Apple供应链', '张经理', 1, 550000, 71500, 0, 0, 550000, 5, '019a0aee-0000-7000-8000-000000000601', '主仓库', '上海市浦东新区张江高科技园区 100 号', CURRENT_DATE - 20, NOW() - INTERVAL '5 days', '019a0aee-0000-7000-8000-000000000101', '张采购', '019a0aee-0000-7000-8000-000000000101', '张管理员', NOW() - INTERVAL '19 days', NOW() - INTERVAL '20 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000802', '019a0aee-0000-7000-8000-000000000002', 'PO-2026-1002', '019a0aee-0000-7000-8000-000000000701', 'Apple供应链', '张经理', 1, 350000, 45500, 0, 0, 350000, 4, '019a0aee-0000-7000-8000-000000000601', '主仓库', '上海市浦东新区张江高科技园区 100 号', CURRENT_DATE - 12, NOW() + INTERVAL '2 days', '019a0aee-0000-7000-8000-000000000102', '李采购', '019a0aee-0000-7000-8000-000000000101', '张管理员', NOW() - INTERVAL '11 days', NOW() - INTERVAL '12 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000803', '019a0aee-0000-7000-8000-000000000002', 'PO-2026-1003', '019a0aee-0000-7000-8000-000000000702', 'Samsung代理', '李经理', 1, 480000, 62400, 0, 0, 480000, 2, '019a0aee-0000-7000-8000-000000000602', '华东仓', '浙江省杭州市余杭区未来科技城 200 号', CURRENT_DATE - 8, NOW() + INTERVAL '4 days', '019a0aee-0000-7000-8000-000000000103', '王采购', '019a0aee-0000-7000-8000-000000000101', '张管理员', NOW() - INTERVAL '7 days', NOW() - INTERVAL '8 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000804', '019a0aee-0000-7000-8000-000000000002', 'PO-2026-1004', '019a0aee-0000-7000-8000-000000000704', '深圳华强电子', '陈经理', 2, 360000, 46800, 0, 0, 360000, 1, '019a0aee-0000-7000-8000-000000000603', '华南仓', '广东省广州市黄埔区科学城 300 号', CURRENT_DATE - 5, NOW() + INTERVAL '6 days', '019a0aee-0000-7000-8000-000000000104', '赵采购', NULL, NULL, NULL, NOW() - INTERVAL '5 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000805', '019a0aee-0000-7000-8000-000000000002', 'PO-2026-1005', '019a0aee-0000-7000-8000-000000000705', '上海联创供应链', '周经理', 3, 744000, 96720, 0, 0, 744000, 3, '019a0aee-0000-7000-8000-000000000604', '华北仓', '北京市大兴区亦庄经济开发区 400 号', CURRENT_DATE - 3, NOW() + INTERVAL '8 days', '019a0aee-0000-7000-8000-000000000101', '张采购', '019a0aee-0000-7000-8000-000000000101', '张管理员', NOW() - INTERVAL '2 days', NOW() - INTERVAL '3 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000806', '019a0aee-0000-7000-8000-000000000002', 'PO-2026-1006', '019a0aee-0000-7000-8000-000000000708', '苏州精工制造', '孙经理', 1, 360000, 46800, 0, 0, 360000, 0, '019a0aee-0000-7000-8000-000000000601', '主仓库', '上海市浦东新区张江高科技园区 100 号', CURRENT_DATE - 1, NOW() + INTERVAL '10 days', '019a0aee-0000-7000-8000-000000000102', '李采购', NULL, NULL, NULL, NOW() - INTERVAL '1 days', NOW(), FALSE, '本地开发种子数据');

INSERT INTO pur_order_item (
    id, tenant_id, order_id, order_no, material_code, material_name, material_spec,
    material_category, sku_id, sku_code, quantity, unit, received_quantity, qualified_quantity,
    unit_price, tax_rate, discount_rate, subtotal, delivery_date, create_time, item_remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000881', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000801', 'PO-2026-1001', 'IP15P-128', 'iPhone 15 Pro 128G', '原色钛金属 / 128GB', '手机数码', '019a0aee-0000-7000-8000-000000000501', 'SKU-IP15P-128', 100, '台', 100, 100, 5500, 0.13, 0, 550000, CURRENT_DATE - 5, NOW(), NULL),
    ('019a0aee-0000-7000-8000-000000000882', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000802', 'PO-2026-1002', 'MBA-M3-256', 'MacBook Air M3 256G', '午夜色 / 256GB', '电脑办公', '019a0aee-0000-7000-8000-000000000503', 'SKU-MBA-M3-256', 50, '台', 30, 30, 7000, 0.13, 0, 350000, CURRENT_DATE + 2, NOW(), NULL),
    ('019a0aee-0000-7000-8000-000000000883', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000803', 'PO-2026-1003', 'S24U-512', 'Galaxy S24 Ultra 512G', '钛灰 / 512GB', '手机数码', '019a0aee-0000-7000-8000-000000000504', 'SKU-S24U-512', 80, '台', 0, 0, 6000, 0.13, 0, 480000, CURRENT_DATE + 4, NOW(), NULL),
    ('019a0aee-0000-7000-8000-000000000884', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000804', 'PO-2026-1004', 'APP2', 'AirPods Pro 2', '白色', '音频配件', '019a0aee-0000-7000-8000-000000000505', 'SKU-APP2', 200, '副', 0, 0, 1800, 0.13, 0, 360000, CURRENT_DATE + 6, NOW(), NULL),
    ('019a0aee-0000-7000-8000-000000000885', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000805', 'PO-2026-1005', 'MI14U-512', '小米 14 Ultra 512G', '黑色 / 512GB', '手机数码', '019a0aee-0000-7000-8000-000000000506', 'SKU-MI14U-512', 120, '台', 60, 60, 6200, 0.13, 0, 744000, CURRENT_DATE + 8, NOW(), NULL),
    ('019a0aee-0000-7000-8000-000000000886', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000806', 'PO-2026-1006', 'X1C-16-512', 'ThinkPad X1 Carbon 16G/512G', '16GB / 512GB', '电脑办公', '019a0aee-0000-7000-8000-000000000507', 'SKU-X1C-16-512', 40, '台', 0, 0, 9000, 0.13, 0, 360000, CURRENT_DATE + 10, NOW(), NULL)
ON CONFLICT (id) DO NOTHING;

COMMIT;
