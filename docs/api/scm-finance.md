# scm-finance — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8208/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-finance (1.0.0)
- 认证: Bearer JWT (Authorization header)

## Settlement

### GET /settlement-order/{id}
- 说明: 查询结算单详情
- 描述: 根据结算单 ID 查询结算单的完整信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseSettlementOrder`

### PUT /settlement-order/{id}
- 说明: 更新结算单
- 描述: 按结算单 ID 更新结算单信息并刷新更新时间。
- 参数: `id`(path,必填: string)
- 请求体: `SettlementOrder`
- 响应: `ApiResponseSettlementOrder`

### DELETE /settlement-order/{id}
- 说明: 删除结算单
- 描述: 按结算单 ID 对结算单执行逻辑删除。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /settlement-order
- 说明: 创建结算单
- 描述: 根据结算数据创建结算单并汇总实际应收应付金额。
- 请求体: `SettlementOrder`
- 响应: `ApiResponseSettlementOrder`

### POST /settlement-order/{id}/pay
- 说明: 登记结算单付款
- 描述: 按结算单 ID 登记付款金额并更新已付与未付金额。
- 参数: `id`(path,必填: string) `amount`(query,必填: number)
- 响应: `ApiResponseSettlementOrder`

### POST /settlement-order/{id}/confirm
- 说明: 确认结算单
- 描述: 记录审批人并确认结算单，使其进入可付款状态。
- 参数: `id`(path,必填: string) `approverId`(query,必填: string) `approverName`(query,必填: string)
- 响应: `ApiResponseSettlementOrder`

### GET /settlement-order/list
- 说明: 分页查询结算单
- 描述: 按结算状态分页查询结算单列表，状态为空时查询全部。
- 参数: `status`(query: integer) `page`(query: integer) `size`(query: integer)
- 响应: `ApiResponsePageSettlementOrder`

## SettlementItem

### GET /settlement-item/{id}
- 说明: 查询结算明细详情
- 描述: 根据明细 ID 查询结算明细的完整信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseSettlementItem`

### PUT /settlement-item/{id}
- 说明: 更新结算明细
- 描述: 按明细 ID 更新结算明细信息。
- 参数: `id`(path,必填: string)
- 请求体: `SettlementItem`
- 响应: `ApiResponseSettlementItem`

### DELETE /settlement-item/{id}
- 说明: 删除结算明细
- 描述: 按明细 ID 删除结算明细。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /settlement-item
- 说明: 创建结算明细
- 描述: 新增一条结算明细并生成主键与创建时间。
- 请求体: `SettlementItem`
- 响应: `ApiResponseSettlementItem`

### GET /settlement-item/by-settlement/{settlementId}
- 说明: 按结算单查询明细
- 描述: 查询指定结算单下的全部结算明细。
- 参数: `settlementId`(path,必填: string)
- 响应: `ApiResponseListSettlementItem`

## Reconciliation

### GET /reconciliation-record/{id}
- 说明: 查询对账记录详情
- 描述: 根据对账记录 ID 查询双方对账的完整信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseReconciliationRecord`

### PUT /reconciliation-record/{id}
- 说明: 更新对账记录
- 描述: 按对账记录 ID 更新对账信息并刷新更新时间。
- 参数: `id`(path,必填: string)
- 请求体: `ReconciliationRecord`
- 响应: `ApiResponseReconciliationRecord`

### DELETE /reconciliation-record/{id}
- 说明: 删除对账记录
- 描述: 按对账记录 ID 对对账记录执行逻辑删除。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /reconciliation-record
- 说明: 创建对账记录
- 描述: 根据对账数据创建对账记录并计算双方金额差异。
- 请求体: `ReconciliationRecord`
- 响应: `ApiResponseReconciliationRecord`

### POST /reconciliation-record/{id}/reconcile
- 说明: 执行对账
- 描述: 记录对账人与对账结果，将对账记录置为已对账状态。
- 参数: `id`(path,必填: string) `reconcilerId`(query,必填: string) `reconcilerName`(query,必填: string)
- 响应: `ApiResponseReconciliationRecord`

### POST /reconciliation-record/{id}/mark-diff
- 说明: 标记对账差异
- 描述: 将对账记录标记为存在差异并记录差异原因。
- 参数: `id`(path,必填: string) `diffReason`(query,必填: string)
- 响应: `ApiResponseReconciliationRecord`

### POST /reconciliation-record/{id}/confirm
- 说明: 确认对账
- 描述: 记录确认人并确认对账结果，完成对账流程。
- 参数: `id`(path,必填: string) `confirmerId`(query,必填: string) `confirmerName`(query,必填: string)
- 响应: `ApiResponseReconciliationRecord`

### GET /reconciliation-record/page
- 说明: 分页查询对账记录
- 描述: 按单号/往来单位关键字、账期、状态和差异标记分页查询对账记录。
- 参数: `page`(query: integer) `size`(query: integer) `keyword`(query: string) `reconciliationPeriod`(query: string) `status`(query: integer) `hasDiff`(query: boolean)
- 响应: `ApiResponsePageReconciliationRecord`

## PlatformFee

### GET /platform-service-fee/{id}
- 说明: 查询服务费详情
- 描述: 根据账单 ID 查询平台服务费的完整计费信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponsePlatformServiceFee`

### PUT /platform-service-fee/{id}
- 说明: 更新服务费账单
- 描述: 按账单 ID 更新平台服务费账单信息并刷新更新时间。
- 参数: `id`(path,必填: string)
- 请求体: `PlatformServiceFee`
- 响应: `ApiResponsePlatformServiceFee`

### DELETE /platform-service-fee/{id}
- 说明: 删除服务费账单
- 描述: 按账单 ID 删除平台服务费账单。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /platform-service-fee
- 说明: 创建服务费账单
- 描述: 新增平台服务费账单，生成主键并将状态置为待支付。
- 请求体: `PlatformServiceFee`
- 响应: `ApiResponsePlatformServiceFee`

### POST /platform-service-fee/{id}/pay
- 说明: 登记服务费付款
- 描述: 按账单 ID 登记本次实付金额并更新账单的已付金额。
- 参数: `id`(path,必填: string) `paidAmount`(query,必填: number)
- 响应: `ApiResponsePlatformServiceFee`

### GET /platform-service-fee/pending
- 说明: 查询待支付服务费
- 描述: 获取所有尚未结清的平台服务费账单列表。
- 响应: `ApiResponseListPlatformServiceFee`

## Invoice

### GET /invoice/{id}
- 说明: 查询发票详情
- 描述: 根据发票 ID 查询发票的完整开票信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseInvoice`

### PUT /invoice/{id}
- 说明: 更新发票
- 描述: 按发票 ID 更新发票中允许修改的开票信息。
- 参数: `id`(path,必填: string)
- 请求体: `UpdateInvoiceRequest`
- 响应: `ApiResponseInvoice`

### DELETE /invoice/{id}
- 说明: 删除发票
- 描述: 按发票 ID 删除对应的发票记录。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /invoice
- 说明: 创建发票
- 描述: 根据请求参数创建一张新的发票记录。
- 请求体: `CreateInvoiceRequest`
- 响应: `ApiResponseInvoice`

### POST /invoice/{id}/void
- 说明: 作废发票
- 描述: 将指定发票作废，作废后该发票不可继续使用。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseInvoice`

### POST /invoice/{id}/red-flush
- 说明: 发票红冲
- 描述: 对指定发票执行红冲，生成用于冲销原发票的负数记录。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseInvoice`

### POST /invoice/{id}/issue
- 说明: 开具发票
- 描述: 按发票 ID 与开票人姓名将发票置为已开具状态。
- 参数: `id`(path,必填: string) `issuerName`(query,必填: string)
- 响应: `ApiResponseInvoice`

### GET /invoice/page
- 说明: 分页查询发票
- 描述: 按往来方 ID 分页查询发票；partyId 为空时返回当前租户全部发票。
- 参数: `partyId`(query: string) `pageNum`(query: integer) `pageSize`(query: integer)
- 响应: `ApiResponsePageInvoice`

## FreightRule

### GET /freight-rule/{id}
- 说明: 查询运费规则详情
- 描述: 根据规则 ID 查询运费规则的完整配置信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseFreightRule`

### PUT /freight-rule/{id}
- 说明: 更新运费规则
- 描述: 按规则 ID 覆盖更新运费规则的配置内容并刷新更新时间。
- 参数: `id`(path,必填: string)
- 请求体: `FreightRule`
- 响应: `ApiResponseFreightRule`

### DELETE /freight-rule/{id}
- 说明: 删除运费规则
- 描述: 按规则 ID 对运费规则执行逻辑删除。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /freight-rule
- 说明: 新增运费规则
- 描述: 创建一条运费规则，记录创建时间与更新时间后保存。
- 请求体: `FreightRule`
- 响应: `ApiResponseFreightRule`

### GET /freight-rule/calculate
- 说明: 试算运费
- 描述: 按指定运费规则与重量、件数、体积、订单金额试算应付运费。
- 参数: `ruleId`(query,必填: string) `weight`(query: number) `quantity`(query: integer) `volume`(query: number) `orderAmount`(query: number)
- 响应: `ApiResponseBigDecimal`

### GET /freight-rule/active
- 说明: 查询启用的运费规则
- 描述: 获取当前所有处于启用状态的运费规则列表。
- 响应: `ApiResponseListFreightRule`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **SettlementOrder** — `id`: string, `tenantId`: string, `settlementNo`: string, `settlementType`: integer, `partnerType`: string, `partnerId`: string, `partnerName`: string, `settlementPeriod`: string, `startDate`: string, `endDate`: string, `totalAmount`: number, `discountAmount`: number, `adjustmentAmount`: number, `actualAmount`: number, `paidAmount`: number, `unpaidAmount`: number, `status`: integer, `approverId`: string, `approverName`: string, `approvedAt`: string, `attachments`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseSettlementOrder** — `code`: integer, `message`: string, `data`: SettlementOrder, `timestamp`: integer
- **SettlementItem** — `id`: string, `tenantId`: string, `settlementId`: string, `settlementNo`: string, `documentType`: string, `documentId`: string, `documentNo`: string, `documentDate`: string, `amount`: number, `discount`: number, `actualAmount`: number, `isSettled`: boolean, `createTime`: string, `remark`: string
- **ApiResponseSettlementItem** — `code`: integer, `message`: string, `data`: SettlementItem, `timestamp`: integer
- **ReconciliationRecord** — `id`: string, `tenantId`: string, `reconciliationNo`: string, `partyType`: string, `partyId`: string, `partyName`: string, `reconciliationPeriod`: string, `startDate`: string, `endDate`: string, `ourTotalAmount`: number, `ourPaidAmount`: number, `ourUnpaidAmount`: number, `theirTotalAmount`: number, `theirPaidAmount`: number, `theirUnpaidAmount`: number, `diffAmount`: number, `hasDiff`: boolean, `diffReason`: string, `status`: integer, `reconcilerId`: string, `reconcilerName`: string, `reconciledAt`: string, `confirmerId`: string, `confirmerName`: string, `confirmedAt`: string, `attachments`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseReconciliationRecord** — `code`: integer, `message`: string, `data`: ReconciliationRecord, `timestamp`: integer
- **PlatformServiceFee** — `id`: string, `tenantId`: string, `feeType`: integer, `billingPeriod`: string, `startDate`: string, `endDate`: string, `orderCount`: integer, `transactionAmount`: number, `storageUsedGb`: number, `apiCalls`: integer, `feeRate`: number, `unitPrice`: number, `baseFee`: number, `transactionFee`: number, `storageFee`: number, `apiFee`: number, `totalFee`: number, `discountAmount`: number, `finalFee`: number, `status`: integer, `paidAmount`: number, `paidAt`: string, `invoiceId`: string, `invoiceNo`: string, `createTime`: string, `updateTime`: string, `remark`: string
- **ApiResponsePlatformServiceFee** — `code`: integer, `message`: string, `data`: PlatformServiceFee, `timestamp`: integer
- **UpdateInvoiceRequest** — `invoiceNo`: string, `invoiceType`: integer, `invoiceKind`: integer, `partyType`: string, `partyId`: string, `partyName`: string, `partyTaxNo`: string, `invoiceCode`: string, `invoiceNumber`: string, `amount`: number, `taxAmount`: number, `taxRate`: number, `relatedOrders`: string, `settlementId`: string, `remark`: string
- **ApiResponseInvoice** — `code`: integer, `message`: string, `data`: Invoice, `timestamp`: integer
- **Invoice** — `id`: string, `tenantId`: string, `invoiceNo`: string, `invoiceType`: integer, `invoiceKind`: integer, `partyType`: string, `partyId`: string, `partyName`: string, `partyTaxNo`: string, `invoiceCode`: string, `invoiceNumber`: string, `invoiceDate`: string, `amount`: number, `taxAmount`: number, `totalAmount`: number, `taxRate`: number, `relatedOrders`: string, `settlementId`: string, `issuerName`: string, `issueDate`: string, `status`: integer, `invoiceFileUrl`: string, `invoicePdfUrl`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **FreightRule** — `id`: string, `tenantId`: string, `ruleCode`: string, `ruleName`: string, `carrierId`: string, `carrierName`: string, `regionType`: string, `regions`: string, `billingType`: integer, `firstWeight`: number, `firstPrice`: number, `additionalWeight`: number, `additionalPrice`: number, `freeThreshold`: number, `fixedFreight`: number, `remoteAreaFee`: number, `handlingFee`: number, `priority`: integer, `effectiveDate`: string, `expiryDate`: string, `enabled`: boolean, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseFreightRule** — `code`: integer, `message`: string, `data`: FreightRule, `timestamp`: integer
- **CreateInvoiceRequest** — `invoiceNo`: string, `invoiceType`: integer, `invoiceKind`: integer, `partyId`: string, `partyType`: string, `partyName`: string, `partyTaxNo`: string, `invoiceCode`: string, `invoiceNumber`: string, `amount`: number, `taxAmount`: number, `taxRate`: number, `relatedOrders`: string, `settlementId`: string, `remark`: string
- **ApiResponsePageSettlementOrder** — `code`: integer, `message`: string, `data`: PageSettlementOrder, `timestamp`: integer
- **OrderItem** — `column`: string, `asc`: boolean
- **PageSettlementOrder** — `records`: SettlementOrder[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSettlementOrder, `searchCount`: PageSettlementOrder, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListSettlementItem** — `code`: integer, `message`: string, `data`: SettlementItem[], `timestamp`: integer
- **ApiResponsePageReconciliationRecord** — `code`: integer, `message`: string, `data`: PageReconciliationRecord, `timestamp`: integer
- **PageReconciliationRecord** — `records`: ReconciliationRecord[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageReconciliationRecord, `searchCount`: PageReconciliationRecord, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListPlatformServiceFee** — `code`: integer, `message`: string, `data`: PlatformServiceFee[], `timestamp`: integer
- **ApiResponsePageInvoice** — `code`: integer, `message`: string, `data`: PageInvoice, `timestamp`: integer
- **PageInvoice** — `records`: Invoice[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageInvoice, `searchCount`: PageInvoice, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseBigDecimal** — `code`: integer, `message`: string, `data`: number, `timestamp`: integer
- **ApiResponseListFreightRule** — `code`: integer, `message`: string, `data`: FreightRule[], `timestamp`: integer