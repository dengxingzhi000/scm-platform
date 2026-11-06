-- ======================================================================
-- 库存移动加权均价回填方案（人工执行，不在 CI / 本地自动跑）
-- 目标库：db_inventory
-- 前置：012_inventory_moving_average_cost.sql（average_cost 扩到 DECIMAL(18,6)）
--
-- 结论（先读后写）
--   * inv_inventory.average_cost 是批次 5 之前唯一的历史均价来源；
--     它可能为 NULL 或被历史流程写成 0，但“0”不等于“真实成本为 0”。
--   * 批次 4 的 inv_log 只有数量前后四元组，没有单价/金额列，
--     无法从 inv_log 反推历史均价（见 1.3 探查）。
--   * 如果库内存在批次账 inv_batch（002_add_inventory_batch_management.sql），
--     可用“批次可用数量 × 批次单价”的加权平均作为推导来源；
--     没有批次数据或数量为 0 的行，保持原值，不凭空写数。
--
-- 执行顺序
--   0) 备份（必做）
--   1) 只读探查（只看，不写）
--   2) 幂等 UPDATE（仅填充 NULL/0，且只在可推导时更新）
--   3) 回读校验
-- ======================================================================

-- ----------------------------------------------------------------------
-- 0) 备份：先备份成本相关列，便于比对与回滚
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS inv_inventory_avgcost_backup_20261106 AS
SELECT id, tenant_id, sku_id, warehouse_id, average_cost, version, update_time
FROM inv_inventory;

-- 回滚参考（人工确认后执行，脚本不自动跑）：
-- UPDATE inv_inventory i
--    SET average_cost = b.average_cost,
--        version = b.version,
--        update_time = b.update_time
--   FROM inv_inventory_avgcost_backup_20261106 b
--  WHERE i.id = b.id;

-- ----------------------------------------------------------------------
-- 1) 只读探查
-- ----------------------------------------------------------------------
-- 1.1 现有均价缺口规模（0 也可能是真实值，需要业务确认）
SELECT COUNT(*) FILTER (WHERE average_cost IS NULL) AS null_rows,
       COUNT(*) FILTER (WHERE average_cost = 0) AS zero_rows,
       COUNT(*) FILTER (WHERE average_cost > 0) AS positive_rows,
       COUNT(*) AS total_rows
FROM inv_inventory
WHERE deleted = false;

-- 1.2 有库存但均价为 0/NULL 的行（优先需要业务确认）
SELECT id, tenant_id, sku_id, warehouse_id, total_stock, average_cost
FROM inv_inventory
WHERE deleted = false
  AND (average_cost IS NULL OR average_cost = 0)
  AND total_stock > 0
ORDER BY tenant_id, sku_id, warehouse_id;

-- 1.3 inv_log 是否具备反推均价所需的单价/金额列（预期 0 行，说明无法反推）
SELECT column_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'inv_log'
  AND column_name IN ('unit_price', 'amount', 'cost_amount')
ORDER BY column_name;

-- 1.4 批次账可推导覆盖率（inv_batch 不存在时该查询会报错，可跳过）
SELECT COUNT(*) AS derivable_rows,
       COUNT(*) FILTER (WHERE d.avg_unit_cost > 0) AS positive_avg_rows
FROM inv_inventory i
LEFT JOIN (
    SELECT tenant_id, sku_id, warehouse_id,
           SUM(available_quantity::numeric * unit_cost)
               / NULLIF(SUM(available_quantity), 0) AS avg_unit_cost
    FROM inv_batch
    WHERE deleted = false
      AND available_quantity > 0
      AND unit_cost > 0
    GROUP BY tenant_id, sku_id, warehouse_id
) d ON d.tenant_id = i.tenant_id
     AND d.sku_id = i.sku_id
     AND d.warehouse_id = i.warehouse_id
WHERE i.deleted = false;

-- ----------------------------------------------------------------------
-- 2) 幂等 UPDATE：仅填充 NULL/0，且只使用批次账可推导的加权均价
--    * 不覆盖已有正数均价（避免用推导值篡改已入账成本）
--    * 推导值四舍五入到 6 位，与 012 的 average_cost 精度一致
-- ----------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('public.inv_batch') IS NULL THEN
        RAISE NOTICE 'inv_batch 不存在，跳过均价回填；平均成本保持原值';
        RETURN;
    END IF;

    EXECUTE $sql$
        WITH derived AS (
            SELECT tenant_id, sku_id, warehouse_id,
                   ROUND(SUM(available_quantity::numeric * unit_cost)
                         / NULLIF(SUM(available_quantity), 0), 6) AS avg_unit_cost
            FROM inv_batch
            WHERE deleted = false
              AND available_quantity > 0
              AND unit_cost > 0
            GROUP BY tenant_id, sku_id, warehouse_id
        )
        UPDATE inv_inventory i
           SET average_cost = derived.avg_unit_cost,
               version = i.version + 1,
               update_time = NOW()
          FROM derived
         WHERE i.tenant_id = derived.tenant_id
           AND i.sku_id = derived.sku_id
           AND i.warehouse_id = derived.warehouse_id
           AND i.deleted = false
           AND (i.average_cost IS NULL OR i.average_cost = 0)
           AND derived.avg_unit_cost > 0
    $sql$;
END $$;

-- ----------------------------------------------------------------------
-- 3) 回读校验
-- ----------------------------------------------------------------------
-- 3.1 回填后缺口（仍为 0/NULL 的行需要业务提供成本来源，保持不动）
SELECT COUNT(*) FILTER (WHERE average_cost IS NULL) AS null_rows,
       COUNT(*) FILTER (WHERE average_cost = 0) AS zero_rows,
       COUNT(*) FILTER (WHERE average_cost > 0) AS positive_rows
FROM inv_inventory
WHERE deleted = false;

-- 3.2 与备份对比：只应有 NULL/0 → 正数的行发生变化
SELECT COUNT(*) AS changed_rows
FROM inv_inventory i
JOIN inv_inventory_avgcost_backup_20261106 b ON b.id = i.id
WHERE i.average_cost IS DISTINCT FROM b.average_cost;
