# 审计数据：历史乱码、分区与留存

## 1. 2026-11 之前的历史乱码不做回溯修改

`db_audit.sys_audit_log.operation_desc` 在 2026-11 之前写入的行，有一部分是改名前的 GBK/UTF-8 二次编码乱码（如 `淇敼瑙掕壊`）。

处理口径：

- 审计表保持**不可篡改**语义，**不对历史行执行任何 `UPDATE`**，乱码原值保留；
- 新数据从 PR #314 起写入正确中文（`修改角色` / `删除角色` / `角色授权` / `修改权限` / `删除权限`）；
- 页面如果需要对历史行友好呈现，在**前端做旧值 → 正确文案的映射**，不改库、不改查询。

## 2. 分区现状与运维

`sys_audit_log` 是按月 `PARTITION BY RANGE (create_time)`，分区名沿用 `<table>_YYYY_MM`。

发现的问题（本批修复）：

| 问题 | 位置 | 影响 |
|---|---|---|
| bootstrap 只预建 `2026_01`–`2026_06` | `scripts/db/microservices/005_db_audit.sql` | 新装环境从 2026-07 起插入直接失败 |
| 分区函数只建「下个月」，且从未被调度 | 同上 | 跑起来也追不上 |
| `PartitionManagementJob` 的表清单不含 `sys_audit_log` | `platform/common/core` | XXL-Job 不会为审计表建分区 |
| 清理脚本查 `pg_partitions` | `scripts/db/partition/drop-old-partitions.sh` | PostgreSQL 没有该视图，脚本**静默不删任何分区** |

修复后：

- `005_db_audit.sql` 的 `create_audit_log_partition()` 改为「当月 + 未来 3 个月」幂等创建，并在脚本末尾调用；
- `scripts/db/partition/create-audit-partitions.sh`：对已存在的库补建「过去 6 个月 + 未来 3 个月」的审计分区（`CREATE TABLE IF NOT EXISTS`，可反复执行）；
- `PartitionManagementJob` 增加 `sys_audit_log`（后缀格式 `yyyy_MM`），`partitionManagementJob` handler 在 `scm-audit` 上注册即可自动滚动建分区；
- `drop-old-partitions.sh` 改用 `pg_inherits` 查子分区，兼容 `<table>_pYYYYMM` 与 `<table>_YYYY_MM` 两种命名。

补建历史分区（不影响已有数据，只让后续插入有落点）：

```bash
export PGPASSWORD=... && DB_HOST=<host> DB_NAME=db_audit bash scripts/db/partition/create-audit-partitions.sh
```

## 3. 留存口径统一为 24 个月

之前 `scripts/db/microservices/README.md` 写「保留 12 个月」，而 `apply-retention.sh`、`PartitionManagementJob` 与本文档都是 2 年（24 个月）——以 **24 个月**为准，README 已改。

`scripts/db/retention/apply-retention.sh` 对审计表的处理改为：先 DROP 超过 24 个月的整月分区（更省），再保留一条 `DELETE` 兜底默认分区里的散行。

## 4. `current_api_calls_today` 未真正采集

核查结论：这个列只有两处使用——`QuotaResetJob` 定期清零、月度配额查询读取。**没有任何拦截器/切面/服务在请求后对它 `+1`**，所以看板上的 API 调用量恒为 0。这属于计量能力缺口，不在本批改动范围内，需与配额 P0 一起补采集点。

## 5. 运行时核对状态（2026-11-04）

**未执行**：缺少 `192.168.0.121:5432/db_audit` 的只读凭据（用户名/密码），按要求不猜测结论。

拿到凭据后执行（只读）：

```sql
-- 1) 2026-07 之后的分区是否存在
SELECT c.relname, pg_get_expr(c.relpartbound, c.oid) AS bound
FROM pg_inherits i
JOIN pg_class c ON c.oid = i.inhrelid
JOIN pg_class p ON p.oid = i.inhparent
WHERE p.relname = 'sys_audit_log'
ORDER BY c.relname;

-- 2) 最近写入时间（若为 NULL 或早于当月，说明分区断档已影响写入）
SELECT max(create_time) FROM sys_audit_log;
```

需要提供的信息：`db_audit` 的**只读**用户名 + 密码（或已存在的只读角色名），以及是否走 PgBouncer（6432）还是直连 5432。

## 6. 库存流水与成本流水：13 个月归档（2026-11 口径）

`db_inventory` 的两张流水表同寿命，热数据保留 **13 个月**：

| 表 | 结构 | 到期处理 |
|---|---|---|
| `inv_log` | 按月 `RANGE (create_time)` 分区，分区名可能是 `<table>_YYYY_MM` 或 `<table>_pYYYYMM` | 先 `pg_dump` 整月分区归档，校验行数一致后再 `DROP TABLE` |
| `inv_cost_log` | 普通表（非分区） | 按 `create_time` 导出后 `DELETE` |

月度归档动作（建议与 `scripts/db/partition/create-partitions.sh` 同一维护窗口执行）：

```bash
# 1) 到期月份：13 个月前，例如 2026-11 执行时归档 <= 2025-10
CUTOFF=$(date -d "-13 months" +%Y-%m)

# 2) inv_log：先归档整月分区，再校验行数，最后删分区
#    分区名以实际查询结果为准（历史 _YYYY_MM 或脚本新建 _pYYYYMM）
pg_dump -U "$DB_USER" -h "$DB_HOST" -Fc \
  -t "inv_log_${CUTOFF//-/_}" "$DB_NAME" > "inv_log_${CUTOFF//-/_}.dump"
psql -U "$DB_USER" -h "$DB_HOST" -d "$DB_NAME" \
  -c "SELECT count(*) FROM inv_log_${CUTOFF//-/_};"
psql -U "$DB_USER" -h "$DB_HOST" -d "$DB_NAME" \
  -c "DROP TABLE inv_log_${CUTOFF//-/_};"

# 3) inv_cost_log：先导出再删除
psql -U "$DB_USER" -h "$DB_HOST" -d "$DB_NAME" -c \
  "\copy (SELECT * FROM inv_cost_log WHERE create_time < date_trunc('month', NOW()) - INTERVAL '13 months') TO 'inv_cost_log_${CUTOFF}.csv' CSV HEADER"
psql -U "$DB_USER" -h "$DB_HOST" -d "$DB_NAME" -c \
  "DELETE FROM inv_cost_log WHERE create_time < date_trunc('month', NOW()) - INTERVAL '13 months';"
```

要点：

- `inv_log_default` 里的散行不能用 `DROP`，用 `DELETE` 兜底；
- 归档包与库内行数一致才允许删除，归档批次、行数与校验和记入备份台账；
- `scripts/db/retention/apply-retention.sh` 尚未包含这两张表，归档按本节手工/调度执行；纳入该脚本时沿用本节口径。
