-- ======================================================================
-- 本地开发种子数据 016 — 结算单与发票
-- 目标库：db_finance
-- ======================================================================
BEGIN;

DELETE FROM invoice WHERE invoice_no LIKE 'INV-2026-10%';
DELETE FROM settlement_order WHERE settlement_no LIKE 'STL-2026-10%';

INSERT INTO settlement_order (
    id, tenant_id, settlement_no, settlement_type, partner_type, partner_id, partner_name,
    settlement_period, start_date, end_date, total_amount, discount_amount, adjustment_amount,
    actual_amount, paid_amount, unpaid_amount, status, approver_name, approved_at,
    create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000a01', '019a0aee-0000-7000-8000-000000000002', 'STL-2026-1001', 1, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000701', 'Apple供应链', '2026-09', DATE '2026-09-01', DATE '2026-09-30', 550000, 0, 0, 550000, 550000, 0, 4, '张管理员', NOW() - INTERVAL '20 days', NOW() - INTERVAL '25 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000a02', '019a0aee-0000-7000-8000-000000000002', 'STL-2026-1002', 1, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000702', 'Samsung代理', '2026-10', DATE '2026-10-01', DATE '2026-10-31', 480000, 0, 0, 480000, 200000, 280000, 3, '张管理员', NOW() - INTERVAL '10 days', NOW() - INTERVAL '15 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000a03', '019a0aee-0000-7000-8000-000000000002', 'STL-2026-1003', 1, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000704', '深圳华强电子', '2026-10', DATE '2026-10-01', DATE '2026-10-31', 320000, 0, 0, 320000, 0, 320000, 1, '张管理员', NOW() - INTERVAL '5 days', NOW() - INTERVAL '7 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000a04', '019a0aee-0000-7000-8000-000000000002', 'STL-2026-1004', 1, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000705', '上海联创供应链', '2026-10', DATE '2026-10-01', DATE '2026-10-31', 275000, 0, 0, 275000, 0, 275000, 2, '张管理员', NOW() - INTERVAL '4 days', NOW() - INTERVAL '6 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000a05', '019a0aee-0000-7000-8000-000000000002', 'STL-2026-1005', 3, 'CARRIER', '019a0aee-0000-7000-8000-000000000921', '顺丰速运', '2026-09', DATE '2026-09-01', DATE '2026-09-30', 98000, 0, 0, 98000, 0, 98000, 0, NULL, NULL, NOW() - INTERVAL '3 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000a06', '019a0aee-0000-7000-8000-000000000002', 'STL-2026-1006', 1, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000708', '苏州精工制造', '2026-08', DATE '2026-08-01', DATE '2026-08-31', 156000, 0, 0, 156000, 156000, 0, 4, '张管理员', NOW() - INTERVAL '40 days', NOW() - INTERVAL '45 days', NOW(), FALSE, '本地开发种子数据');

INSERT INTO invoice (
    id, tenant_id, invoice_no, invoice_type, invoice_kind, party_type, party_id, party_name,
    party_tax_no, invoice_code, invoice_number, invoice_date,
    amount, tax_amount, total_amount, tax_rate, settlement_id, issuer_name, issue_date,
    status, create_time, update_time, deleted, remark
) VALUES
    ('019a0aee-0000-7000-8000-000000000b01', '019a0aee-0000-7000-8000-000000000002', 'INV-2026-1001', 2, 2, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000701', 'Apple供应链', '91310000MA1FL00001', '3100000001', '10000001', DATE '2026-09-28', 550000, 71500, 621500, 0.13, '019a0aee-0000-7000-8000-000000000a01', 'Apple供应链', DATE '2026-09-28', 2, NOW() - INTERVAL '20 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000b02', '019a0aee-0000-7000-8000-000000000002', 'INV-2026-1002', 2, 2, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000702', 'Samsung代理', '91110000MA01FL0002', '3100000002', '10000002', DATE '2026-10-10', 480000, 62400, 542400, 0.13, '019a0aee-0000-7000-8000-000000000a02', 'Samsung代理', DATE '2026-10-10', 1, NOW() - INTERVAL '12 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000b03', '019a0aee-0000-7000-8000-000000000002', 'INV-2026-1003', 1, 2, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000704', '深圳华强电子', '91440300MA5FL00003', NULL, NULL, DATE '2026-10-12', 320000, 41600, 361600, 0.13, '019a0aee-0000-7000-8000-000000000a03', NULL, NULL, 0, NOW() - INTERVAL '8 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000b04', '019a0aee-0000-7000-8000-000000000002', 'INV-2026-1004', 3, 2, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000705', '上海联创供应链', '91310000MA1FL00004', '3100000004', '10000004', DATE '2026-10-14', 275000, 35750, 310750, 0.13, '019a0aee-0000-7000-8000-000000000a04', '上海联创供应链', DATE '2026-10-14', 2, NOW() - INTERVAL '6 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000b05', '019a0aee-0000-7000-8000-000000000002', 'INV-2026-1005', 2, 2, 'CARRIER', '019a0aee-0000-7000-8000-000000000921', '顺丰速运', '91440300MA5FL00005', NULL, NULL, DATE '2026-09-30', 98000, 12740, 110740, 0.13, '019a0aee-0000-7000-8000-000000000a05', NULL, NULL, 0, NOW() - INTERVAL '3 days', NOW(), FALSE, '本地开发种子数据'),
    ('019a0aee-0000-7000-8000-000000000b06', '019a0aee-0000-7000-8000-000000000002', 'INV-2026-1006', 2, 2, 'SUPPLIER', '019a0aee-0000-7000-8000-000000000708', '苏州精工制造', '91320500MA1FL00006', '3200000006', '10000006', DATE '2026-08-30', 156000, 20280, 176280, 0.13, '019a0aee-0000-7000-8000-000000000a06', '苏州精工制造', DATE '2026-08-30', 3, NOW() - INTERVAL '40 days', NOW(), FALSE, '本地开发种子数据');

COMMIT;
