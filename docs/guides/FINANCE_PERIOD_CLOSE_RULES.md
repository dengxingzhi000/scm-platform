# 财务结账口径（本期纳入单据）

关账前可以用 `GET /accounting-period/{id}/close-inclusion` 预览「本期纳入哪些单据」。
口径全部落在配置项里，业务方以后调整配置即可，不需要改代码。

## 1. 配置项

```yaml
finance:
  close:
    # 归属期日期口径：BUSINESS_DATE（默认）| CREATE_TIME
    date-basis: ${FINANCE_CLOSE_DATE_BASIS:BUSINESS_DATE}
    # 是否把「已发货未开票」的结算明细纳入本期
    include-shipped-unbilled: ${FINANCE_CLOSE_INCLUDE_SHIPPED_UNBILLED:true}
    # 是否排除已驳回/作废发票（状态 3）
    exclude-rejected: ${FINANCE_CLOSE_EXCLUDE_REJECTED:true}
    # 视为「已发货」的结算明细单据类型
    shipped-unbilled-document-types: ${FINANCE_CLOSE_SHIPPED_UNBILLED_DOCUMENT_TYPES:SHIPMENT,OUTBOUND,SALE_OUTBOUND}
```

| 配置 | 默认 | 含义 |
|---|---|---|
| `date-basis` | `BUSINESS_DATE` | `BUSINESS_DATE`：结算明细取 `document_date`、发票取 `invoice_date`；`CREATE_TIME`：改用 `create_time` |
| `include-shipped-unbilled` | `true` | 已发货但未开票的结算明细是否纳入本期 |
| `exclude-rejected` | `true` | 是否排除已驳回/作废发票（状态 3）；红冲发票（状态 4）**始终**排除 |
| `shipped-unbilled-document-types` | `SHIPMENT,OUTBOUND,SALE_OUTBOUND` | 视为「已发货」的 `settlement_item.document_type`（忽略大小写） |

## 2. 纳入公式

设账期区间为 `[periodStart, periodEnd]`（由账期 `start_date` / `end_date` 决定）。

```
纳入本期 = 结算明细集合 ∪ 发票集合

结算明细集合：
  settlement_item 中，
    document_date ∈ [periodStart, periodEnd]        （date-basis=BUSINESS_DATE）
 或 create_time ∈ [periodStart 00:00, periodEnd+1 00:00)（date-basis=CREATE_TIME）
 且 若 include-shipped-unbilled=false，
    排除「已发货未开票」明细

发票集合：
  invoice 中，
    invoice_date ∈ [periodStart, periodEnd]         （date-basis=BUSINESS_DATE）
 或 create_time ∈ [periodStart 00:00, periodEnd+1 00:00)（date-basis=CREATE_TIME）
 且 status ∉ {4 红冲}
 且 若 exclude-rejected=true，status ∉ {3 作废/驳回}
```

### 「已发货未开票」判定

1. `settlement_item.document_type` 命中 `shipped-unbilled-document-types`（忽略大小写）；
2. 该明细的 `settlement_id` 没有任何「未作废、未红冲」的发票关联
   （不要求发票日期在本期——后来开票也视为已开票）。

`include-shipped-unbilled=true` 时这些明细计入本期，并在响应里单独标记为
`SHIPPED_UNBILLED`；为 `false` 时整条明细不纳入。

## 3. 边界与注意事项

- **金额不跨类别求和**：响应分别给出 `settlementItemAmount` 与 `invoiceAmount`。
  发票与结算明细可能描述同一笔业务，合并求和会重复计量；结账分录应按业务方
  （或财务手工）确认的单一类别取数。
- 结算明细金额取 `actual_amount`，为空回退 `amount`；发票金额取 `total_amount`。
- 发票状态码沿用现有枚举：`0 草稿 / 1 已开具 / 2 已寄出 / 3 作废(驳回) / 4 红冲`。
  现有表没有独立的「审批驳回」状态，因此「排除已驳回」落在状态 3。
- 结算单本身没有驳回状态；`settlement_order.deleted=true` 的明细不会出现在
  预览里（MyBatis-Plus 逻辑删除）。
- 本预览只做只读汇总，不写库、不生成凭证；关账后写入仍由
  `AccountingPeriodGuard` 拦截（`CLOSED` 拒绝，`REOPENED` 放行）。
- 历史数据不回填：该功能不新增表/列，也不修改任何既有单据。

## 4. 响应示例

```json
{
  "periodId": "...",
  "periodCode": "2026-10",
  "periodStart": "2026-10-01",
  "periodEnd": "2026-10-31",
  "dateBasis": "BUSINESS_DATE",
  "includeShippedUnbilled": true,
  "excludeRejected": true,
  "settlementItemCount": 12,
  "settlementItemAmount": 34500.00,
  "invoiceCount": 9,
  "invoiceAmount": 28900.00,
  "shippedUnbilledCount": 2,
  "shippedUnbilledAmount": 3600.00,
  "documents": [
    {
      "category": "SHIPPED_UNBILLED",
      "documentType": "SHIPMENT",
      "documentNo": "SHP-20261015-001",
      "businessDate": "2026-10-15",
      "amount": 1800.00
    }
  ]
}
```
