# 坏账核销单据化规则（可配置默认实现）

适用服务：`scm-finance`（服务端口 8208，数据库 `db_finance`）。

坏账核销不再是一次性的"余额扣减"，而是一张可追溯的单据：**申请 → 审批 → 核销生效**。
每一步都记录操作人，金额与账期绑定，核销生效后同一单据不允许再次核销。

## 1. 状态机

| 状态 | code | 可流转到 | 说明 |
|------|------|----------|------|
| 申请中 | 0 `PENDING` | 已批准(1) / 已驳回(3) | 提交后即进入申请中 |
| 已批准 | 1 `APPROVED` | 已核销(2) | 只有批准后才能生效 |
| 已核销 | 2 `WRITTEN_OFF` | — | 终态，不再占用流程 |
| 已驳回 | 3 `REJECTED` | — | 终态，可基于同一单据重新申请 |

- `PENDING → WRITTEN_OFF` 被状态机拒绝（409），因此**不存在跳过审批直接核销的路径**。
- `APPROVED → REJECTED` 同样被拒绝：批准后只能核销生效。
- `WRITTEN_OFF` / `REJECTED` 为终态，任何流转返回 409。

## 2. 金额与账期绑定

- 申请时必须填写 `periodCode`（`YYYY-MM`，格式错误 400），核销金额落在该账期。
- 申请与**核销生效**时各校验一次账期可写（`AccountingPeriodGuard`）：
  - 账期为 `CLOSED` → 409，禁止核销；
  - `OPEN` / `REOPENED` → 放行（重开账期后可以继续核销）。
- 生效前二次校验是必要的：审批与生效之间账期可能被关账。
- 金额口径走 `FinanceAmount.positive(...)`：必须大于 0、最多两位小数、上限 `99999999.99`。

## 3. 不允许重复核销

同一 `settlement_id` 或同一 `invoice_id`，在**申请中 / 已批准 / 已核销**三个状态下
只允许存在一张核销单：

| 已有单据状态 | 再次申请的结果 |
|--------------|----------------|
| 申请中 / 已批准 | 409「已有进行中的坏账核销申请」 |
| 已核销 | 409「已核销，禁止重复核销」 |
| 已驳回 | 允许重新申请 |

服务层先查重，数据库再用两个部分唯一索引兜底并发（见迁移脚本）：

- `uk_bad_debt_tenant_settlement`：`(tenant_id, settlement_id) WHERE status IN (0,1,2) AND deleted = FALSE`
- `uk_bad_debt_tenant_invoice`：`(tenant_id, invoice_id) WHERE status IN (0,1,2) AND deleted = FALSE`

结算单或发票至少要填一个，两者都为空返回 400；填了就必须存在且属于当前租户
（跨租户 403）。

## 4. 接口与权限码

| 方法 | 路径 | 权限码 | 审计风险级别 |
|------|------|--------|--------------|
| POST | `/bad-debt-write-off` | `finance:bad-debt:apply` | 3 |
| POST | `/bad-debt-write-off/{id}/approve` | `finance:bad-debt:approve` | 4 |
| POST | `/bad-debt-write-off/{id}/reject` | `finance:bad-debt:reject` | 4 |
| POST | `/bad-debt-write-off/{id}/write-off` | `finance:bad-debt:write-off` | 4 |
| GET | `/bad-debt-write-off/page` | `finance:bad-debt:page` | 1 |
| GET | `/bad-debt-write-off/{id}` | `finance:bad-debt:view` | 1 |

坏账核销使用独立权限码 `finance:bad-debt:*`，**不**复用对账差异的
`finance:reconciliation-diff:write-off`：两者业务含义不同（后者针对对账差异记录）。

权限由 `scripts/db/seed/026_finance_bad_debt_permissions.sql` 登记（固定 UUID v5 +
`ON CONFLICT`），`seed-all.bat` / `seed-all.sh` 已包含；默认绑定
`ROLE_SUPER_ADMIN` / `ROLE_TENANT_ADMIN`。未登记的端点会被 `ApiAccessControlFilter`
默认拒绝，新增路径必须同步补 seed。

## 5. 迁移（人工执行）

```bash
psql -d db_finance -f scripts/db/migrations/finance_006_bad_debt_write_off.sql
```

- 幂等：`CREATE TABLE IF NOT EXISTS`、`CREATE INDEX IF NOT EXISTS`、约束经
  `pg_constraint` 判断后添加，可重复执行。
- **不回填历史数据**：只建表、建索引、加约束，不改写任何既有结算单、发票或账期。
- 没有历史的坏账核销记录——该表从上线起才有数据。

## 6. 边界与后续可配置点

- 本期不做坏账准备计提、不做核销后的分录生成（记账凭证仍由现有结算/发票流程负责）。
- 批准的金额上限等于申请金额；如需部分核销，请拆成多张核销单（同一单据在
  已核销终态后仍会被重复核销校验拦截，因此部分核销需要业务方先确认口径——这是
  目前唯一"依赖业务口径"的开放点）。
- 若业务方要求"审批人与申请人必须是不同人"，在 `approve` 前补一条校验即可，
  当前默认实现允许同一人操作（尚未接到该要求）。
