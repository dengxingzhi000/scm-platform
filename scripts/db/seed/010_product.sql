-- ======================================================================
-- 本地开发种子数据 010 — 商品（分类 / 品牌 / SPU / SKU）
-- 目标库：db_product
-- ======================================================================
BEGIN;

ALTER TABLE prod_category ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE prod_brand    ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE prod_spu      ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE prod_sku      ADD COLUMN IF NOT EXISTS tenant_id UUID;

INSERT INTO prod_category (
    id, tenant_id, category_code, category_name, parent_id, level,
    sort_order, is_leaf, enabled, create_time, update_time, deleted
) VALUES
    ('019a0aee-0000-7000-8000-000000000201', '019a0aee-0000-7000-8000-000000000002', 'CAT-PHONE', '手机数码', NULL, 1, 10, TRUE, TRUE, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000202', '019a0aee-0000-7000-8000-000000000002', 'CAT-COMPUTER', '电脑办公', NULL, 1, 20, TRUE, TRUE, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000203', '019a0aee-0000-7000-8000-000000000002', 'CAT-AUDIO', '音频配件', NULL, 1, 30, TRUE, TRUE, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO UPDATE SET
    category_name = EXCLUDED.category_name,
    tenant_id = EXCLUDED.tenant_id,
    enabled = EXCLUDED.enabled,
    update_time = NOW();

INSERT INTO prod_brand (
    id, tenant_id, brand_code, brand_name, brand_name_en, country,
    featured, sort_order, enabled, create_time, update_time, deleted
) VALUES
    ('019a0aee-0000-7000-8000-000000000301', '019a0aee-0000-7000-8000-000000000002', 'BRAND-APPLE', 'Apple', 'Apple', 'US', TRUE, 10, TRUE, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000302', '019a0aee-0000-7000-8000-000000000002', 'BRAND-SAMSUNG', '三星', 'Samsung', 'KR', TRUE, 20, TRUE, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000303', '019a0aee-0000-7000-8000-000000000002', 'BRAND-XIAOMI', '小米', 'Xiaomi', 'CN', TRUE, 30, TRUE, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000304', '019a0aee-0000-7000-8000-000000000002', 'BRAND-LENOVO', '联想', 'Lenovo', 'CN', FALSE, 40, TRUE, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO UPDATE SET
    brand_name = EXCLUDED.brand_name,
    tenant_id = EXCLUDED.tenant_id,
    enabled = EXCLUDED.enabled,
    update_time = NOW();

INSERT INTO prod_spu (
    id, tenant_id, spu_code, spu_name, category_id, brand_id,
    description, main_image, min_price, max_price, total_stock, total_sales,
    sort_order, status, published_at, create_time, update_time, deleted
) VALUES
    ('019a0aee-0000-7000-8000-000000000401', '019a0aee-0000-7000-8000-000000000002', 'SPU-IP15P', 'iPhone 15 Pro', '019a0aee-0000-7000-8000-000000000201', '019a0aee-0000-7000-8000-000000000301', 'A17 Pro 芯片，钛金属设计', NULL, 8999, 9999, 120, 36, 10, 1, NOW(), NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000402', '019a0aee-0000-7000-8000-000000000002', 'SPU-MBA-M3', 'MacBook Air M3', '019a0aee-0000-7000-8000-000000000202', '019a0aee-0000-7000-8000-000000000301', 'M3 芯片，13 英寸轻薄本', NULL, 8499, 8499, 8, 12, 20, 1, NOW(), NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000403', '019a0aee-0000-7000-8000-000000000002', 'SPU-S24U', 'Galaxy S24 Ultra', '019a0aee-0000-7000-8000-000000000201', '019a0aee-0000-7000-8000-000000000302', '2 亿像素影像旗舰', NULL, 6999, 6999, 45, 18, 30, 1, NOW(), NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000404', '019a0aee-0000-7000-8000-000000000002', 'SPU-APP2', 'AirPods Pro 2', '019a0aee-0000-7000-8000-000000000203', '019a0aee-0000-7000-8000-000000000301', '主动降噪无线耳机', NULL, 1899, 1899, 3, 25, 40, 1, NOW(), NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000405', '019a0aee-0000-7000-8000-000000000002', 'SPU-MI14U', '小米 14 Ultra', '019a0aee-0000-7000-8000-000000000201', '019a0aee-0000-7000-8000-000000000303', '徕卡光学全焦段四摄', NULL, 6499, 6499, 0, 9, 50, 1, NOW(), NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000406', '019a0aee-0000-7000-8000-000000000002', 'SPU-X1C', 'ThinkPad X1 Carbon', '019a0aee-0000-7000-8000-000000000202', '019a0aee-0000-7000-8000-000000000304', '商务旗舰轻薄本', NULL, 12999, 12999, 25, 6, 60, 1, NOW(), NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000407', '019a0aee-0000-7000-8000-000000000002', 'SPU-IPADAIR11', 'iPad Air 11', '019a0aee-0000-7000-8000-000000000201', '019a0aee-0000-7000-8000-000000000301', 'M2 芯片平板电脑', NULL, 4799, 4799, 60, 14, 70, 1, NOW(), NOW(), NOW(), FALSE)
ON CONFLICT (id) DO UPDATE SET
    spu_name = EXCLUDED.spu_name,
    tenant_id = EXCLUDED.tenant_id,
    min_price = EXCLUDED.min_price,
    max_price = EXCLUDED.max_price,
    total_stock = EXCLUDED.total_stock,
    status = EXCLUDED.status,
    update_time = NOW();

INSERT INTO prod_sku (
    id, tenant_id, spu_id, sku_code, sku_name, attributes,
    original_price, selling_price, cost_price, stock, available_stock, locked_stock,
    sales_count, weight, barcode, status, create_time, update_time, deleted
) VALUES
    ('019a0aee-0000-7000-8000-000000000501', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000401', 'SKU-IP15P-128', 'iPhone 15 Pro 128G', '{"color":"原色钛金属","storage":"128GB"}'::jsonb, 9999, 9999, 8200, 120, 120, 0, 20, 0.19, '690000000001', 1, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000502', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000401', 'SKU-IP15P-256', 'iPhone 15 Pro 256G', '{"color":"蓝色钛金属","storage":"256GB"}'::jsonb, 10999, 8999, 9000, 0, 0, 0, 8, 0.19, '690000000002', 1, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000503', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000402', 'SKU-MBA-M3-256', 'MacBook Air M3 256G', '{"color":"午夜色","storage":"256GB"}'::jsonb, 8999, 8499, 7600, 8, 8, 0, 12, 1.24, '690000000003', 1, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000504', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000403', 'SKU-S24U-512', 'Galaxy S24 Ultra 512G', '{"color":"钛灰","storage":"512GB"}'::jsonb, 7499, 6999, 6000, 45, 45, 0, 18, 0.23, '690000000004', 1, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000505', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000404', 'SKU-APP2', 'AirPods Pro 2', '{"color":"白色"}'::jsonb, 1999, 1899, 1400, 3, 3, 0, 25, 0.05, '690000000005', 1, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000506', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000405', 'SKU-MI14U-512', '小米 14 Ultra 512G', '{"color":"黑色","storage":"512GB"}'::jsonb, 6999, 6499, 5600, 0, 0, 0, 9, 0.22, '690000000006', 1, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000507', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000406', 'SKU-X1C-16-512', 'ThinkPad X1 Carbon 16G/512G', '{"memory":"16GB","storage":"512GB"}'::jsonb, 13999, 12999, 11200, 25, 25, 0, 6, 1.12, '690000000007', 1, NOW(), NOW(), FALSE),
    ('019a0aee-0000-7000-8000-000000000508', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000407', 'SKU-IPADAIR11-128', 'iPad Air 11 128G', '{"color":"星光色","storage":"128GB"}'::jsonb, 4999, 4799, 4000, 60, 60, 0, 14, 0.46, '690000000008', 1, NOW(), NOW(), FALSE)
ON CONFLICT (id) DO UPDATE SET
    sku_name = EXCLUDED.sku_name,
    tenant_id = EXCLUDED.tenant_id,
    selling_price = EXCLUDED.selling_price,
    stock = EXCLUDED.stock,
    available_stock = EXCLUDED.available_stock,
    status = EXCLUDED.status,
    update_time = NOW();

COMMIT;
