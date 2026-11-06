-- ======================================================================
-- legacy_sku_id 回填方案（人工执行，不在 CI / 本地自动跑）
-- 目标库：db_inventory
-- 前置：010_inventory_tcc_precision.sql 已给 inv_inventory 加列
--
-- 用途
--   scm-order 仍以 Long skuId 调用 TCC。inv_inventory.sku_id 是 UUID，
--   需要一个“数字 SKU → 台账行”的桥。没有可信映射时不要猜，先只读探查。
--
-- 执行顺序
--   0) 备份（必做）
--   1) 只读探查（只看，不写）
--   2) 导入映射（由业务/DBA 提供 CSV 或临时表）
--   3) 幂等 UPDATE（仅填 NULL，只影响匹配到的行）
--   4) 回读校验
-- ======================================================================

-- ----------------------------------------------------------------------
-- 0) 备份：只备份定位列，便于比对与回滚
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inv_inventory_legacy_backup_20261106 AS
SELECT id, tenant_id, sku_id, warehouse_id, legacy_sku_id
FROM inv_inventory;

-- 回滚参考（人工确认后执行，脚本不自动跑）：
-- UPDATE inv_inventory i
--    SET legacy_sku_id = b.legacy_sku_id
--   FROM inv_inventory_legacy_backup_20261106 b
--  WHERE i.id = b.id;

-- ----------------------------------------------------------------------
-- 1) 只读探查
-- ----------------------------------------------------------------------
-- 1.1 缺口规模
SELECT COUNT(*) FILTER (WHERE legacy_sku_id IS NULL) AS missing_rows,
       COUNT(*) FILTER (WHERE legacy_sku_id IS NOT NULL) AS filled_rows,
       COUNT(*) AS total_rows
FROM inv_inventory
WHERE deleted = false;

-- 1.2 同租户同 SKU 多仓（回填时 legacy_sku_id 必须每仓唯一，否则 TCC 无法唯一定位）
SELECT tenant_id, sku_id, COUNT(*) AS warehouse_rows
FROM inv_inventory
WHERE deleted = false
GROUP BY tenant_id, sku_id
HAVING COUNT(*) > 1;

-- 1.3 已填写的兼容 ID 是否冲突（应返回 0 行）
SELECT tenant_id, legacy_sku_id, COUNT(*) AS rows
FROM inv_inventory
WHERE deleted = false AND legacy_sku_id IS NOT NULL
GROUP BY tenant_id, legacy_sku_id
HAVING COUNT(*) > 1;

-- 1.4 待回填清单（交给业务确认映射，不要在库里凭 sku 名称猜）
SELECT id, tenant_id, sku_id, warehouse_id
FROM inv_inventory
WHERE deleted = false AND legacy_sku_id IS NULL
ORDER BY tenant_id, sku_id;

-- ----------------------------------------------------------------------
-- 2) 映射来源
--    推荐做法：由商品域导出 (tenant_id, sku_uuid, legacy_sku_id) 到临时表，
--    再由 DBA 复核。下面建表语句默认不执行，取消注释后使用。
-- ----------------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS inv_legacy_sku_mapping (
--     tenant_id UUID NOT NULL,
--     sku_uuid UUID NOT NULL,
--     legacy_sku_id BIGINT NOT NULL,
--     PRIMARY KEY (tenant_id, sku_uuid)
-- );
-- \copy inv_legacy_sku_mapping FROM 'legacy_sku_mapping.csv' CSV HEADER

-- ----------------------------------------------------------------------
-- 3) 幂等 UPDATE：只填 NULL，且要求映射与台账行一一对应
--    没有 inv_legacy_sku_mapping 表时本段会报错，属预期：先完成第 2 步。
-- ----------------------------------------------------------------------
-- UPDATE inv_inventory i
--    SET legacy_sku_id = m.legacy_sku_id,
--        update_time = NOW()
--   FROM inv_legacy_sku_mapping m
--  WHERE i.tenant_id = m.tenant_id
--    AND i.sku_id = m.sku_uuid
--    AND i.deleted = false
--    AND i.legacy_sku_id IS NULL;

-- 若同一 legacy_sku_id 命中多个台账行，UPDATE 会因唯一索引
-- uk_inv_tenant_legacy_sku_warehouse 失败并整条语句回滚（PostgreSQL 语义），
-- 这是有意的保护：先修映射，再回填。

-- ----------------------------------------------------------------------
-- 4) 回读校验
-- ----------------------------------------------------------------------
SELECT COUNT(*) FILTER (WHERE legacy_sku_id IS NULL) AS missing_after,
       COUNT(*) FILTER (WHERE legacy_sku_id IS NOT NULL) AS filled_after
FROM inv_inventory
WHERE deleted = false;
