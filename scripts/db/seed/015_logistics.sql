-- ======================================================================
-- 本地开发种子数据 015 — 物流（承运商 / 运单 / 轨迹）
-- 目标库：db_logistics
-- ======================================================================
BEGIN;

ALTER TABLE tms_carrier  ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE tms_waybill  ADD COLUMN IF NOT EXISTS tenant_id UUID;
ALTER TABLE tms_tracking ADD COLUMN IF NOT EXISTS tenant_id UUID;

INSERT INTO tms_carrier (
    id, tenant_id, carrier_code, carrier_name, carrier_type,
    contact_name, contact_phone, contact_email, website,
    service_area, service_types, base_rate, enabled, sort_order,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000921', '019a0aee-0000-7000-8000-000000000002', 'SF', '顺丰速运', 1, '顺丰客服', '95338', 'service@sf.example.com', 'https://www.sf-express.com', ARRAY['全国'], ARRAY['标准快递','次日达'], 12.00, TRUE, 10, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000922', '019a0aee-0000-7000-8000-000000000002', 'JD', '京东物流', 1, '京东客服', '950616', 'service@jd.example.com', 'https://www.jdl.com', ARRAY['全国'], ARRAY['标准快递','仓配一体'], 10.00, TRUE, 20, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000923', '019a0aee-0000-7000-8000-000000000002', 'ZTO', '中通快递', 1, '中通客服', '95311', 'service@zto.example.com', 'https://www.zto.com', ARRAY['全国'], ARRAY['标准快递'], 8.00, TRUE, 30, NOW(), NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000924', '019a0aee-0000-7000-8000-000000000002', 'YTO', '圆通速递', 1, '圆通客服', '95554', 'service@yto.example.com', 'https://www.yto.net.cn', ARRAY['全国'], ARRAY['标准快递'], 7.50, TRUE, 40, NOW(), NOW(), FALSE, '本地开发种子数据')
ON CONFLICT (id) DO UPDATE SET
    carrier_name = EXCLUDED.carrier_name,
    tenant_id = EXCLUDED.tenant_id,
    contact_phone = EXCLUDED.contact_phone,
    base_rate = EXCLUDED.base_rate,
    enabled = EXCLUDED.enabled,
    update_time = NOW();

DELETE FROM tms_tracking WHERE waybill_no LIKE 'WB-2026-10%';
DELETE FROM tms_waybill WHERE waybill_no LIKE 'WB-2026-10%';

INSERT INTO tms_waybill (
    id, tenant_id, waybill_no, carrier_id, carrier_name, order_id, order_no,
    sender_name, sender_phone, sender_address, receiver_name, receiver_phone, receiver_address,
    goods_name, goods_weight, goods_value, freight_amount, insurance_amount, status,
    estimated_delivery, actual_delivery, courier_name, courier_phone,
    sign_type, sign_person, sign_time, exception_type, exception_reason,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000931', '019a0aee-0000-7000-8000-000000000002', 'WB-2026-1001', '019a0aee-0000-7000-8000-000000000921', '顺丰速运', '019a0aee-0000-7000-8000-000000000801', 'ORD-2026-1001', '主仓库', '13800000001', '{"province":"上海市","city":"上海市","district":"浦东新区","detail":"张江高科技园区 100 号"}'::jsonb, '张三', '13800138000', '{"province":"北京市","city":"北京市","district":"朝阳区","detail":"三里屯SOHO A座 1001室"}'::jsonb, 'iPhone 15 Pro 128G', 0.19, 9999, 12, 0, 4, date_trunc('day', NOW()) - INTERVAL '10 days', date_trunc('day', NOW()) - INTERVAL '10 days', '刘师傅', '13900000001', 1, '张三', date_trunc('day', NOW()) - INTERVAL '10 days', NULL, NULL, date_trunc('day', NOW()) - INTERVAL '12 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000932', '019a0aee-0000-7000-8000-000000000002', 'WB-2026-1002', '019a0aee-0000-7000-8000-000000000922', '京东物流', '019a0aee-0000-7000-8000-000000000802', 'ORD-2026-1002', '主仓库', '13800000001', '{"province":"上海市","city":"上海市","district":"浦东新区","detail":"张江高科技园区 100 号"}'::jsonb, '李四', '13800138001', '{"province":"广东省","city":"广州市","district":"天河区","detail":"珠江新城 B 座 2201室"}'::jsonb, 'MacBook Air M3 256G', 1.24, 8499, 10, 0, 2, date_trunc('day', NOW()) - INTERVAL '8 days', NULL, '陈师傅', '13900000002', NULL, NULL, NULL, NULL, NULL, date_trunc('day', NOW()) - INTERVAL '11 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000933', '019a0aee-0000-7000-8000-000000000002', 'WB-2026-1003', '019a0aee-0000-7000-8000-000000000922', '京东物流', '019a0aee-0000-7000-8000-000000000803', 'ORD-2026-1003', '华东仓', '13800000002', '{"province":"浙江省","city":"杭州市","district":"余杭区","detail":"未来科技城 200 号"}'::jsonb, '王五', '13800138002', '{"province":"四川省","city":"成都市","district":"高新区","detail":"天府三街 88 号"}'::jsonb, 'Galaxy S24 Ultra 512G', 0.23, 6999, 10, 0, 1, date_trunc('day', NOW()) - INTERVAL '7 days', NULL, '周师傅', '13900000003', NULL, NULL, NULL, NULL, NULL, date_trunc('day', NOW()) - INTERVAL '10 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000934', '019a0aee-0000-7000-8000-000000000002', 'WB-2026-1004', '019a0aee-0000-7000-8000-000000000923', '中通快递', '019a0aee-0000-7000-8000-000000000804', 'ORD-2026-1004', '华南仓', '13800000003', '{"province":"广东省","city":"广州市","district":"黄埔区","detail":"科学城 300 号"}'::jsonb, '赵六', '13800138003', '{"province":"湖北省","city":"武汉市","district":"洪山区","detail":"光谷大道 100 号"}'::jsonb, 'AirPods Pro 2', 0.05, 1899, 8, 0, 5, date_trunc('day', NOW()) - INTERVAL '7 days', NULL, '吴师傅', '13900000004', NULL, NULL, NULL, 'DELIVERY_FAILED', '收件人电话无人接听，待再次派送', date_trunc('day', NOW()) - INTERVAL '9 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000935', '019a0aee-0000-7000-8000-000000000002', 'WB-2026-1005', '019a0aee-0000-7000-8000-000000000924', '圆通速递', '019a0aee-0000-7000-8000-000000000805', 'ORD-2026-1005', '华南仓', '13800000003', '{"province":"广东省","city":"广州市","district":"黄埔区","detail":"科学城 300 号"}'::jsonb, '张三', '13800138000', '{"province":"北京市","city":"北京市","district":"朝阳区","detail":"三里屯SOHO A座 1001室"}'::jsonb, '小米 14 Ultra 512G', 0.22, 6499, 7.5, 0, 0, date_trunc('day', NOW()) + INTERVAL '2 days', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, date_trunc('day', NOW()) - INTERVAL '1 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000936', '019a0aee-0000-7000-8000-000000000002', 'WB-2026-1006', '019a0aee-0000-7000-8000-000000000921', '顺丰速运', '019a0aee-0000-7000-8000-000000000806', 'ORD-2026-1006', '主仓库', '13800000001', '{"province":"上海市","city":"上海市","district":"浦东新区","detail":"张江高科技园区 100 号"}'::jsonb, '李四', '13800138001', '{"province":"广东省","city":"广州市","district":"天河区","detail":"珠江新城 B 座 2201室"}'::jsonb, 'ThinkPad X1 Carbon 16G/512G', 1.12, 12999, 12, 0, 3, date_trunc('day', NOW()) + INTERVAL '1 days', NULL, '刘师傅', '13900000001', NULL, NULL, NULL, NULL, NULL, date_trunc('day', NOW()) - INTERVAL '2 days', NOW(), FALSE, '本地开发种子数据')
ON CONFLICT (id) DO NOTHING;

INSERT INTO tms_tracking (
    id, tenant_id, waybill_id, waybill_no, track_time, location, description,
    track_status, operator, source, create_time, extra_data
) VALUES
    ('019a0aee-0000-7000-8000-000000000941', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000931', 'WB-2026-1001', date_trunc('day', NOW()) - INTERVAL '12 days' + INTERVAL '14 hours', '上海市浦东新区', '快件已揽收', 'picked', '顺丰速运', 'API', NOW(), '{}'::jsonb),
    ('019a0aee-0000-7000-8000-000000000942', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000931', 'WB-2026-1001', date_trunc('day', NOW()) - INTERVAL '11 days' + INTERVAL '8 hours', '上海市转运中心', '快件已发出', 'in_transit', '顺丰速运', 'API', NOW(), '{}'::jsonb),
    ('019a0aee-0000-7000-8000-000000000943', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000931', 'WB-2026-1001', date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '9 hours', '北京市朝阳区', '快件派送中', 'in_transit', '刘师傅', 'API', NOW(), '{}'::jsonb),
    ('019a0aee-0000-7000-8000-000000000944', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000931', 'WB-2026-1001', date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '16 hours', '北京市朝阳区', '快件已签收，签收人：张三', 'delivered', '刘师傅', 'API', NOW(), '{}'::jsonb),
    ('019a0aee-0000-7000-8000-000000000945', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000932', 'WB-2026-1002', date_trunc('day', NOW()) - INTERVAL '11 days' + INTERVAL '18 hours', '上海市浦东新区', '快件已揽收', 'picked', '京东物流', 'API', NOW(), '{}'::jsonb),
    ('019a0aee-0000-7000-8000-000000000946', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000932', 'WB-2026-1002', date_trunc('day', NOW()) - INTERVAL '9 days' + INTERVAL '11 hours', '广州市天河区', '快件运输中', 'in_transit', '京东物流', 'API', NOW(), '{}'::jsonb),
    ('019a0aee-0000-7000-8000-000000000947', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000933', 'WB-2026-1003', date_trunc('day', NOW()) - INTERVAL '10 days' + INTERVAL '12 hours', '杭州市余杭区', '快件已揽收', 'picked', '京东物流', 'API', NOW(), '{}'::jsonb),
    ('019a0aee-0000-7000-8000-000000000948', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000934', 'WB-2026-1004', date_trunc('day', NOW()) - INTERVAL '9 days' + INTERVAL '10 hours', '广州市黄埔区', '快件已揽收', 'picked', '中通快递', 'API', NOW(), '{}'::jsonb),
    ('019a0aee-0000-7000-8000-000000000949', '019a0aee-0000-7000-8000-000000000002', '019a0aee-0000-7000-8000-000000000934', 'WB-2026-1004', date_trunc('day', NOW()) - INTERVAL '7 days' + INTERVAL '15 hours', '武汉市洪山区', '派送异常：收件人电话无人接听', 'exception', '吴师傅', 'API', NOW(), '{}'::jsonb)
ON CONFLICT (id) DO NOTHING;

COMMIT;
