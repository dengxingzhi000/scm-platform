# 财务税率规则（fin_tax_rate）

税率不再由前端逐单传入，而是按「品类 × 地区 × 生效区间」的租户规则解析。
结算单与发票都调用同一个 `TaxRateResolver`，保证两处取到的税率一致。

## 1. 数据模型

表 `fin_tax_rate`（迁移 `scripts/db/migrations/finance_005_tax_rate.sql`）：

| 列 | 说明 |
|---|---|
| `tenant_id` | 租户，规则只在租户内生效 |
| `category` | 品类编码；`NULL` 表示不限品类 |
| `region` | 地区编码；`NULL` 表示不限地区 |
| `rate` | 税率，`0..1` 小数（13% 存 `0.1300`） |
| `effective_from` | 生效开始日期（含） |
| `effective_to` | 生效结束日期（含）；`NULL` 表示长期有效 |
| `is_default` | 租户默认税率标记；默认规则不参与品类/地区匹配 |
| `enabled` | 启停；停用规则不参与匹配 |

约束：

- `rate` 必须在 `0..1`；
- `effective_to` 为空或 `>= effective_from`；
- 每个租户最多一条「启用中的默认规则」（部分唯一索引
  `uk_tax_rate_tenant_default`），新增/改为默认时服务端自动把原默认降级。

历史数据不回填：新表初始为空，`settlement_order.tax_rate` 新增列对历史单据保持
`NULL`（表示未采集）。

## 2. 匹配顺序

输入：`category`、`region`、`businessDate`（结算取 `start_date`，发票取开票日）。

1. 取该租户 `enabled=true`、未删除、且 `businessDate` 落在
   `[effective_from, effective_to]` 内的规则。
2. 过滤 `is_default=false` 的规则，按维度匹配：
   - 规则该维度为空 → 不限，匹配任意输入；
   - 规则该维度有值 → 必须与输入完全一致（忽略大小写）。
3. 命中多条时取 `effective_from` **最新**的一条；同一天再取 `create_time` 最新。
   → `source = RULE`，`ruleId` = 命中规则，`effectiveFrom` = 规则生效日。
4. 未命中规则时，取同租户 `is_default=true` 的启用规则（同样要求业务日期在
   生效区间内，多条取最新生效）。
   → `source = DEFAULT`，`ruleId` = 默认规则（可能为 `null`）。
5. 仍无命中时，取配置 `finance.tax.default-rate`（默认 `0.13`）。
   → `source = DEFAULT`，`ruleId = null`，`effectiveFrom = null`。

响应里的 `source` 就是「按规则命中」还是「默认值兜底」的标记：
`RULE` = 规则命中，`DEFAULT` = 默认兜底。前端/对账可据此区分报表口径。

## 3. 配置

```yaml
finance:
  tax:
    # 租户没有默认规则时的兜底税率，0..1 小数
    default-rate: ${FINANCE_TAX_DEFAULT_RATE:0.13}
```

业务方调整口径只需要新增/停用 `fin_tax_rate` 规则或改这个配置，不需要改代码。

## 4. 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/tax-rate` | 新增规则（`isDefault=true` 会替换原默认） |
| `PUT` | `/tax-rate/{id}` | 局部修改；`enabled=false` 等价停用 |
| `DELETE` | `/tax-rate/{id}` | 停用（不物理删除，保留追溯） |
| `GET` | `/tax-rate/{id}` | 查看 |
| `GET` | `/tax-rate/page` | 分页（可按 category/region/enabled 过滤） |
| `GET` | `/tax-rate/resolve` | 预览解析结果（结算/开票前确认税率） |

## 5. 与单据的衔接

- `POST /invoice`：请求未传 `taxRate` 时按 `taxCategory`/`taxRegion` 解析并落库，
  响应带 `taxRateSource` / `taxRateRuleId`；显式传 `taxRate` 时以传入值为准。
- `POST /settlement-order`：请求未传 `taxRate` 时按 `taxCategory`/`taxRegion`
  和 `startDate` 解析并写入 `settlement_order.tax_rate`，响应回显来源；
  显式传入只做 `0..1` / 四位小数校验。更新接口不允许改税率（与其它金额字段一致，
  由服务端维护）。
