# scm-purchase — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8207/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-purchase (1.0.0)
- 认证: Bearer JWT (Authorization header)

## Rfq

### PUT /pur-rfq
- 说明: 修改询价单
- 描述: 按主键更新已有的采购询价单。
- 请求体: `PurRfq`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-rfq
- 说明: 新增询价单
- 描述: 创建一张新的采购询价单。
- 请求体: `PurRfq`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-rfq/{id}/publish
- 说明: 发布询价单
- 描述: 将询价单发布给供应商，进入报价阶段。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-rfq/{id}/close
- 说明: 关闭询价单
- 描述: 关闭采购询价单，停止接收新的供应商报价。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-rfq/{id}
- 说明: 根据 ID 查询询价单
- 描述: 按主键查询单张采购询价单的完整信息。
- 参数: `id`(path,必填: string)
- 响应: `PurRfq`

### DELETE /pur-rfq/{id}
- 说明: 删除询价单
- 描述: 按主键删除采购询价单。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-rfq/page
- 说明: 分页查询询价单
- 描述: 按状态、询价类型和关键字分页查询采购询价单。
- 参数: `page`(query: integer) `size`(query: integer) `status`(query: integer) `rfqType`(query: integer) `keyword`(query: string)
- 响应: `PagePurRfq`

### GET /pur-rfq/no/{rfqNo}
- 说明: 根据询价单号查询
- 描述: 按业务单号查询采购询价单，用于单据核对与追溯。
- 参数: `rfqNo`(path,必填: string)
- 响应: `PurRfq`

### GET /pur-rfq/list
- 说明: 按状态查询询价单
- 描述: 查询指定状态下的全部采购询价单，用于列表展示。
- 参数: `status`(query,必填: integer)
- 响应: `200,400,401,403,423,429,500`

## RfqItem

### PUT /pur-rfq-item
- 说明: 修改询价明细
- 描述: 按主键更新已有的询价明细记录。
- 请求体: `PurRfqItem`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-rfq-item
- 说明: 新增询价明细
- 描述: 创建一条新的询价明细记录。
- 请求体: `PurRfqItem`
- 响应: `200,400,401,403,423,429,500`

### GET /pur-rfq-item/{id}
- 说明: 根据 ID 查询询价明细
- 描述: 按明细主键查询单条询价明细记录。
- 参数: `id`(path,必填: string)
- 响应: `PurRfqItem`

### DELETE /pur-rfq-item/{id}
- 说明: 删除询价明细
- 描述: 按明细主键删除单条询价明细记录。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-rfq-item/list/{rfqId}
- 说明: 查询询价单下的明细列表
- 描述: 根据询价单 ID 查询该询价单包含的全部明细行。
- 参数: `rfqId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### DELETE /pur-rfq-item/rfq/{rfqId}
- 说明: 按询价单删除明细
- 描述: 删除指定询价单下的全部明细行。
- 参数: `rfqId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## PurchaseRequest

### PUT /pur-request
- 说明: 修改采购申请
- 描述: 按主键更新已有的采购申请单。
- 请求体: `PurRequest`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-request
- 说明: 新增采购申请
- 描述: 创建一张新的采购申请单。
- 请求体: `PurRequest`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-request/{id}/submit
- 说明: 提交采购申请
- 描述: 将草稿状态的采购申请单提交进入审批流程。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-request/{id}/reject
- 说明: 驳回采购申请
- 描述: 记录审批人信息与驳回原因，将采购申请单置为驳回状态。
- 参数: `id`(path,必填: string) `approverId`(query,必填: string) `approverName`(query,必填: string) `reason`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-request/{id}/close
- 说明: 关闭采购申请
- 描述: 关闭采购申请单，使其不再参与后续采购执行。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-request/{id}/approve
- 说明: 审批通过采购申请
- 描述: 记录审批人信息，将采购申请单置为审批通过状态。
- 参数: `id`(path,必填: string) `approverId`(query,必填: string) `approverName`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-request/{id}
- 说明: 根据 ID 查询采购申请
- 描述: 按主键查询单张采购申请单的完整信息。
- 参数: `id`(path,必填: string)
- 响应: `PurRequest`

### DELETE /pur-request/{id}
- 说明: 删除采购申请
- 描述: 按主键删除采购申请单。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-request/page
- 说明: 分页查询采购申请
- 描述: 按状态、申请类型和关键字分页查询采购申请单。
- 参数: `page`(query: integer) `size`(query: integer) `status`(query: integer) `requestType`(query: integer) `keyword`(query: string)
- 响应: `PagePurRequest`

### GET /pur-request/no/{requestNo}
- 说明: 根据申请单号查询
- 描述: 按业务单号查询采购申请单，用于单据核对与追溯。
- 参数: `requestNo`(path,必填: string)
- 响应: `PurRequest`

### GET /pur-request/list
- 说明: 按状态查询采购申请
- 描述: 查询指定状态下的全部采购申请单，用于待办列表展示。
- 参数: `status`(query,必填: integer)
- 响应: `200,400,401,403,423,429,500`

## PurchaseRequestItem

### PUT /pur-request-item
- 说明: 修改申请明细
- 描述: 按主键更新已有的采购申请明细记录。
- 请求体: `PurRequestItem`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-request-item
- 说明: 新增申请明细
- 描述: 创建一条新的采购申请明细记录。
- 请求体: `PurRequestItem`
- 响应: `200,400,401,403,423,429,500`

### GET /pur-request-item/{id}
- 说明: 根据 ID 查询申请明细
- 描述: 按明细主键查询单条采购申请明细记录。
- 参数: `id`(path,必填: string)
- 响应: `PurRequestItem`

### DELETE /pur-request-item/{id}
- 说明: 删除申请明细
- 描述: 按明细主键删除单条采购申请明细记录。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-request-item/list/{requestId}
- 说明: 查询申请单下的明细列表
- 描述: 根据采购申请单 ID 查询该申请单包含的全部明细行。
- 参数: `requestId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### DELETE /pur-request-item/request/{requestId}
- 说明: 按申请单删除明细
- 描述: 删除指定采购申请单下的全部明细行。
- 参数: `requestId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## PurchaseReceipt

### PUT /pur-receipt
- 说明: 修改收货单
- 描述: 按主键更新已有的采购收货单。
- 请求体: `PurReceipt`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-receipt
- 说明: 新增收货单
- 描述: 创建一张新的采购收货单。
- 请求体: `PurReceipt`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-receipt/{id}/shelve
- 说明: 收货上架
- 描述: 记录上架操作人，将质检合格的货物上架入库。
- 参数: `id`(path,必填: string) `shelvedBy`(query,必填: string) `shelvedByName`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-receipt/{id}/receive
- 说明: 确认收货
- 描述: 登记收货人和收货人名称，将收货单推进至已收货状态。
- 参数: `id`(path,必填: string) `receiverId`(query,必填: string) `receiverName`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-receipt/{id}/quality-inspect
- 说明: 收货质检
- 描述: 记录质检人、质检结果与备注，完成收货单的质量检验。
- 参数: `id`(path,必填: string) `inspectorId`(query,必填: string) `inspectorName`(query,必填: string) `result`(query,必填: integer) `remark`(query: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-receipt/{id}
- 说明: 根据 ID 查询收货单
- 描述: 按主键查询单张采购收货单的完整信息。
- 参数: `id`(path,必填: string)
- 响应: `PurReceipt`

### DELETE /pur-receipt/{id}
- 说明: 删除收货单
- 描述: 按主键删除采购收货单。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-receipt/page
- 说明: 分页查询收货单
- 描述: 按状态、收货类型、供应商和关键字分页查询采购收货单。
- 参数: `page`(query: integer) `size`(query: integer) `status`(query: integer) `receiptType`(query: integer) `supplierId`(query: string) `keyword`(query: string)
- 响应: `PagePurReceipt`

### GET /pur-receipt/order/{orderId}
- 说明: 查询采购订单的收货单
- 描述: 根据采购订单 ID 查询该订单关联的全部收货单。
- 参数: `orderId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-receipt/no/{receiptNo}
- 说明: 根据收货单号查询
- 描述: 按业务单号查询采购收货单，用于外部单据核对。
- 参数: `receiptNo`(path,必填: string)
- 响应: `PurReceipt`

## ReceiptItem

### PUT /pur-receipt-item
- 说明: 修改收货明细
- 描述: 按主键更新已有的收货明细记录。
- 请求体: `PurReceiptItem`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-receipt-item
- 说明: 新增收货明细
- 描述: 创建一条新的采购收货明细记录。
- 请求体: `PurReceiptItem`
- 响应: `200,400,401,403,423,429,500`

### GET /pur-receipt-item/{id}
- 说明: 根据 ID 查询收货明细
- 描述: 按明细主键查询单条采购收货明细记录。
- 参数: `id`(path,必填: string)
- 响应: `PurReceiptItem`

### DELETE /pur-receipt-item/{id}
- 说明: 删除收货明细
- 描述: 按明细主键删除单条收货明细记录。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-receipt-item/list/{receiptId}
- 说明: 查询收货单下的明细列表
- 描述: 根据收货单 ID 查询该收货单包含的全部明细行。
- 参数: `receiptId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### DELETE /pur-receipt-item/receipt/{receiptId}
- 说明: 按收货单删除明细
- 描述: 删除指定收货单下的全部明细行。
- 参数: `receiptId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## SupplierQuotation

### PUT /pur-quotation
- 说明: 更新报价单
- 描述: 按主键更新供应商报价单信息。
- 请求体: `PurQuotation`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-quotation
- 说明: 新增报价单
- 描述: 创建一份新的供应商报价单。
- 请求体: `PurQuotation`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-quotation/{id}/submit
- 说明: 提交报价单
- 描述: 将供应商报价单提交，推进到待评审状态。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-quotation/{id}/select
- 说明: 选用报价单
- 描述: 记录选用人信息并将该报价单标记为已选用。
- 参数: `id`(path,必填: string) `selectedBy`(query,必填: string) `selectedByName`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-quotation/{id}
- 说明: 根据 ID 查询报价单
- 描述: 按主键查询供应商报价单的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `PurQuotation`

### DELETE /pur-quotation/{id}
- 说明: 删除报价单
- 描述: 按主键删除供应商报价单。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-quotation/supplier/{supplierId}
- 说明: 按供应商查询报价列表
- 描述: 查询指定供应商提交的全部报价单。
- 参数: `supplierId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-quotation/rfq/{rfqId}
- 说明: 按询价单查询报价列表
- 描述: 查询指定询价单收到的全部供应商报价。
- 参数: `rfqId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-quotation/page
- 说明: 分页查询报价单
- 描述: 按状态、供应商和关联询价单分页查询报价单。
- 参数: `page`(query: integer) `size`(query: integer) `status`(query: integer) `supplierId`(query: string) `rfqId`(query: string)
- 响应: `PagePurQuotation`

### GET /pur-quotation/no/{quotationNo}
- 说明: 根据报价单号查询
- 描述: 通过报价单号查询供应商报价单详情。
- 参数: `quotationNo`(path,必填: string)
- 响应: `PurQuotation`

## QuotationItem

### PUT /pur-quotation-item
- 说明: 修改报价明细
- 描述: 按主键更新已有的报价明细记录。
- 请求体: `PurQuotationItem`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-quotation-item
- 说明: 新增报价明细
- 描述: 创建一条新的供应商报价明细记录。
- 请求体: `PurQuotationItem`
- 响应: `200,400,401,403,423,429,500`

### GET /pur-quotation-item/{id}
- 说明: 根据 ID 查询报价明细
- 描述: 按明细主键查询单条供应商报价明细记录。
- 参数: `id`(path,必填: string)
- 响应: `PurQuotationItem`

### DELETE /pur-quotation-item/{id}
- 说明: 删除报价明细
- 描述: 按明细主键删除单条报价明细记录。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-quotation-item/list/{quotationId}
- 说明: 查询报价单下的明细列表
- 描述: 根据报价单 ID 查询该报价单包含的全部明细行。
- 参数: `quotationId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### DELETE /pur-quotation-item/quotation/{quotationId}
- 说明: 按报价单删除明细
- 描述: 删除指定报价单下的全部明细行。
- 参数: `quotationId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## PriceComparison

### PUT /pur-price-comparison
- 说明: 更新比价单
- 描述: 按主键更新比价单信息。
- 请求体: `PurPriceComparison`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-price-comparison
- 说明: 新增比价单
- 描述: 创建一份新的供应商比价单。
- 请求体: `PurPriceComparison`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-price-comparison/{id}/approve
- 说明: 审批比价单
- 描述: 记录审批人信息并通过比价单审批。
- 参数: `id`(path,必填: string) `approverId`(query,必填: string) `approverName`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-price-comparison/{id}
- 说明: 根据 ID 查询比价单
- 描述: 按主键查询比价单的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `PurPriceComparison`

### DELETE /pur-price-comparison/{id}
- 说明: 删除比价单
- 描述: 按主键删除比价单。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-price-comparison/rfq/{rfqId}
- 说明: 按询价单查询比价单列表
- 描述: 查询指定询价单关联的全部比价单。
- 参数: `rfqId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-price-comparison/page
- 说明: 分页查询比价单
- 描述: 按状态和关联询价单分页查询比价单。
- 参数: `page`(query: integer) `size`(query: integer) `status`(query: integer) `rfqId`(query: string)
- 响应: `PagePurPriceComparison`

### GET /pur-price-comparison/no/{comparisonNo}
- 说明: 根据比价单号查询
- 描述: 通过比价单号查询比价单详情。
- 参数: `comparisonNo`(path,必填: string)
- 响应: `PurPriceComparison`

## ComparisonItem

### PUT /pur-price-comparison-item
- 说明: 更新比价明细
- 描述: 按主键更新比价明细信息。
- 请求体: `PurPriceComparisonItem`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-price-comparison-item
- 说明: 新增比价明细
- 描述: 为比价单新增一条供应商报价明细。
- 请求体: `PurPriceComparisonItem`
- 响应: `200,400,401,403,423,429,500`

### GET /pur-price-comparison-item/{id}
- 说明: 根据 ID 查询比价明细
- 描述: 按主键查询单条比价明细。
- 参数: `id`(path,必填: string)
- 响应: `PurPriceComparisonItem`

### DELETE /pur-price-comparison-item/{id}
- 说明: 删除比价明细
- 描述: 按主键删除单条比价明细。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-price-comparison-item/list/{comparisonId}
- 说明: 按比价单查询明细列表
- 描述: 查询指定比价单下的全部比价明细。
- 参数: `comparisonId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### DELETE /pur-price-comparison-item/comparison/{comparisonId}
- 说明: 按比价单删除明细
- 描述: 删除指定比价单下的全部比价明细。
- 参数: `comparisonId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## PurchasePlan

### PUT /pur-plan
- 说明: 更新计划
- 描述: 按主键更新采购计划信息。
- 请求体: `PurPlan`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-plan
- 说明: 新增计划
- 描述: 创建一份新的采购计划。
- 请求体: `PurPlan`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-plan/{id}/submit
- 说明: 提交计划
- 描述: 将采购计划提交审批，推进到待审批状态。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-plan/{id}/complete
- 说明: 完结计划
- 描述: 将采购计划标记为已完成并结束执行。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-plan/{id}/approve
- 说明: 审批计划
- 描述: 记录审批人信息并通过采购计划审批。
- 参数: `id`(path,必填: string) `approverId`(query,必填: string) `approverName`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-plan/{id}
- 说明: 根据 ID 查询计划
- 描述: 按主键查询采购计划的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `PurPlan`

### DELETE /pur-plan/{id}
- 说明: 删除计划
- 描述: 按主键删除采购计划。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-plan/page
- 说明: 分页查询计划
- 描述: 按状态、计划类型和关键字分页查询采购计划。
- 参数: `page`(query: integer) `size`(query: integer) `status`(query: integer) `planType`(query: integer) `keyword`(query: string)
- 响应: `PagePurPlan`

### GET /pur-plan/no/{planNo}
- 说明: 根据计划编号查询
- 描述: 通过计划编号查询采购计划详情。
- 参数: `planNo`(path,必填: string)
- 响应: `PurPlan`

### GET /pur-plan/list
- 说明: 按状态查询计划列表
- 描述: 查询指定状态下的全部采购计划。
- 参数: `status`(query,必填: integer)
- 响应: `200,400,401,403,423,429,500`

## PlanItem

### PUT /pur-plan-item
- 说明: 更新计划明细
- 描述: 按主键更新采购计划明细信息。
- 请求体: `PurPlanItem`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-plan-item
- 说明: 新增计划明细
- 描述: 为采购计划新增一条明细行。
- 请求体: `PurPlanItem`
- 响应: `200,400,401,403,423,429,500`

### GET /pur-plan-item/{id}
- 说明: 根据 ID 查询计划明细
- 描述: 按主键查询单条采购计划明细。
- 参数: `id`(path,必填: string)
- 响应: `PurPlanItem`

### DELETE /pur-plan-item/{id}
- 说明: 删除计划明细
- 描述: 按主键删除单条采购计划明细。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-plan-item/list/{planId}
- 说明: 按计划查询明细列表
- 描述: 查询指定采购计划下的全部明细行。
- 参数: `planId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### DELETE /pur-plan-item/plan/{planId}
- 说明: 按计划删除明细
- 描述: 删除指定采购计划下的全部明细行。
- 参数: `planId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## SupplierPurchaseItem

### PUT /pur-order-item
- 说明: 更新订单明细
- 描述: 按主键更新采购订单明细信息。
- 请求体: `PurOrderItem`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-order-item
- 说明: 新增订单明细
- 描述: 为采购订单新增一条明细行。
- 请求体: `PurOrderItem`
- 响应: `200,400,401,403,423,429,500`

### GET /pur-order-item/{id}
- 说明: 根据 ID 查询订单明细
- 描述: 按主键查询单条采购订单明细。
- 参数: `id`(path,必填: string)
- 响应: `PurOrderItem`

### DELETE /pur-order-item/{id}
- 说明: 删除订单明细
- 描述: 按主键删除单条采购订单明细。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-order-item/list/{orderId}
- 说明: 按订单查询明细列表
- 描述: 查询指定采购订单下的全部明细行。
- 参数: `orderId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### DELETE /pur-order-item/order/{orderId}
- 说明: 按订单删除明细
- 描述: 删除指定采购订单下的全部明细行。
- 参数: `orderId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## PurchaseContract

### PUT /pur-contract
- 说明: 更新合同
- 描述: 按主键更新采购合同信息。
- 请求体: `PurContract`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-contract
- 说明: 新增合同
- 描述: 创建一份新的采购合同记录。
- 请求体: `PurContract`
- 响应: `200,400,401,403,423,429,500`

### POST /pur-contract/{id}/terminate
- 说明: 终止合同
- 描述: 终止指定采购合同并更新其状态。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /pur-contract/{id}/sign
- 说明: 签订合同
- 描述: 记录合同签订人与签订时间并推进合同状态。
- 参数: `id`(path,必填: string) `signedBy`(query,必填: string) `signedByName`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-contract/{id}
- 说明: 根据 ID 查询合同
- 描述: 按主键查询采购合同的详细信息。
- 参数: `id`(path,必填: string)
- 响应: `PurContract`

### DELETE /pur-contract/{id}
- 说明: 删除合同
- 描述: 按主键删除采购合同。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-contract/supplier/{supplierId}
- 说明: 按供应商查询合同列表
- 描述: 查询指定供应商名下的全部采购合同。
- 参数: `supplierId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /pur-contract/page
- 说明: 分页查询合同
- 描述: 按状态、合同类型、供应商和关键字分页查询采购合同。
- 参数: `page`(query: integer) `size`(query: integer) `status`(query: integer) `contractType`(query: integer) `supplierId`(query: string) `keyword`(query: string)
- 响应: `PagePurContract`

### GET /pur-contract/no/{contractNo}
- 说明: 根据合同编号查询
- 描述: 通过合同编号查询采购合同详情。
- 参数: `contractNo`(path,必填: string)
- 响应: `PurContract`

### GET /pur-contract/list
- 说明: 按状态查询合同列表
- 描述: 查询指定状态下的全部采购合同。
- 参数: `status`(query,必填: integer)
- 响应: `200,400,401,403,423,429,500`

## PurchaseOrder

### GET /api/v1/pur-order/{id}
- 说明: 查询采购订单详情
- 描述: 按主键查询采购订单，不存在或已删除时返回 404。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponsePurOrder`

### GET /api/v1/pur-order/{id}/items
- 说明: 查询采购订单明细
- 描述: 查询指定采购订单下的全部明细行。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseListPurOrderItem`

### GET /api/v1/pur-order/page
- 说明: 分页查询采购订单
- 描述: 按单号/供应商关键字、状态与供应商分页查询采购订单。
- 参数: `page`(query: integer) `size`(query: integer) `keyword`(query: string) `status`(query: integer) `supplierId`(query: string)
- 响应: `ApiResponsePagePurOrder`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **PurRfq** — `id`: string, `tenantId`: string, `rfqNo`: string, `rfqTitle`: string, `requestId`: string, `requestNo`: string, `rfqType`: integer, `inquiryMethod`: integer, `deadline`: string, `quotationRequirement`: string, `paymentTerms`: string, `deliveryTerms`: string, `status`: integer, `initiatorId`: string, `initiatorName`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **PurRfqItem** — `id`: string, `tenantId`: string, `rfqId`: string, `rfqNo`: string, `materialCode`: string, `materialName`: string, `materialSpec`: string, `quantity`: number, `unit`: string, `technicalRequirement`: string, `qualityStandard`: string, `deliveryRequirement`: string, `createTime`: string, `remark`: string
- **PurRequest** — `id`: string, `tenantId`: string, `requestNo`: string, `requestType`: integer, `priority`: integer, `deptId`: string, `deptName`: string, `requesterId`: string, `requesterName`: string, `requesterPhone`: string, `expectedDelivery`: string, `purpose`: string, `budgetAmount`: number, `status`: integer, `approvalFlowId`: string, `currentApproverId`: string, `currentApproverName`: string, `submittedAt`: string, `approvedAt`: string, `rejectedAt`: string, `rejectReason`: string, `converted`: boolean, `purchaseOrderId`: string, `purchaseOrderNo`: string, `attachments`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **PurRequestItem** — `id`: string, `tenantId`: string, `requestId`: string, `requestNo`: string, `materialCode`: string, `materialName`: string, `materialSpec`: string, `materialCategory`: string, `quantity`: number, `unit`: string, `budgetPrice`: number, `requirementDesc`: string, `qualityStandard`: string, `suggestedSupplierId`: string, `suggestedSupplierName`: string, `createTime`: string, `remark`: string
- **PurReceipt** — `id`: string, `tenantId`: string, `receiptNo`: string, `orderId`: string, `orderNo`: string, `supplierId`: string, `supplierName`: string, `warehouseId`: string, `warehouseName`: string, `receiptType`: integer, `status`: integer, `receiverId`: string, `receiverName`: string, `receivedAt`: string, `qualityInspectorId`: string, `qualityInspectorName`: string, `qualityInspectedAt`: string, `qualityResult`: integer, `qualityRemark`: string, `shelved`: boolean, `shelvedBy`: string, `shelvedByName`: string, `shelvedAt`: string, `attachments`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **PurReceiptItem** — `id`: string, `tenantId`: string, `receiptId`: string, `receiptNo`: string, `orderItemId`: string, `materialCode`: string, `materialName`: string, `expectedQuantity`: number, `actualQuantity`: number, `qualifiedQuantity`: number, `unqualifiedQuantity`: number, `unit`: string, `batchNo`: string, `productionDate`: string, `expiryDate`: string, `qualityResult`: integer, `locationCode`: string, `createTime`: string, `remark`: string
- **PurQuotation** — `id`: string, `tenantId`: string, `quotationNo`: string, `rfqId`: string, `rfqNo`: string, `supplierId`: string, `supplierName`: string, `supplierContact`: string, `supplierPhone`: string, `supplierEmail`: string, `totalAmount`: number, `taxRate`: number, `paymentTerms`: string, `paymentMethod`: string, `deliveryTerms`: string, `deliveryPeriod`: string, `warrantyPeriod`: string, `warrantyTerms`: string, `validFrom`: string, `validUntil`: string, `status`: integer, `isSelected`: boolean, `selectedBy`: string, `selectedByName`: string, `selectedAt`: string, `attachments`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **PurQuotationItem** — `id`: string, `tenantId`: string, `quotationId`: string, `quotationNo`: string, `rfqItemId`: string, `materialCode`: string, `materialName`: string, `materialSpec`: string, `quantity`: number, `unit`: string, `unitPrice`: number, `taxRate`: number, `subtotal`: number, `deliveryDays`: integer, `brand`: string, `origin`: string, `createTime`: string, `remark`: string
- **PurPriceComparison** — `id`: string, `tenantId`: string, `comparisonNo`: string, `rfqId`: string, `rfqNo`: string, `comparisonDimensions`: string, `recommendedSupplierId`: string, `recommendedSupplierName`: string, `recommendationReason`: string, `status`: integer, `approvedBy`: string, `approvedByName`: string, `approvedAt`: string, `analystId`: string, `analystName`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **PurPriceComparisonItem** — `id`: string, `tenantId`: string, `comparisonId`: string, `quotationId`: string, `supplierId`: string, `supplierName`: string, `scores`: string, `totalScore`: number, `rank`: integer, `advantages`: string, `disadvantages`: string, `createTime`: string, `remark`: string
- **PurPlan** — `id`: string, `tenantId`: string, `planNo`: string, `planName`: string, `planType`: integer, `planPeriod`: string, `startDate`: string, `endDate`: string, `totalBudget`: number, `status`: integer, `approvedBy`: string, `approvedByName`: string, `approvedAt`: string, `executionRate`: number, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **PurPlanItem** — `id`: string, `tenantId`: string, `planId`: string, `planNo`: string, `materialCode`: string, `materialName`: string, `materialCategory`: string, `plannedQuantity`: number, `unit`: string, `purchasedQuantity`: number, `budgetPrice`: number, `budgetAmount`: number, `expectedDelivery`: string, `createTime`: string, `remark`: string
- **PurOrderItem** — `id`: string, `tenantId`: string, `orderId`: string, `orderNo`: string, `materialCode`: string, `materialName`: string, `materialSpec`: string, `materialCategory`: string, `skuId`: string, `skuCode`: string, `quantity`: number, `unit`: string, `receivedQuantity`: number, `qualifiedQuantity`: number, `unitPrice`: number, `taxRate`: number, `discountRate`: number, `subtotal`: number, `deliveryDate`: string, `qualityRequirement`: string, `itemRemark`: string, `createTime`: string
- **PurContract** — `id`: string, `tenantId`: string, `contractNo`: string, `contractName`: string, `contractType`: integer, `supplierId`: string, `supplierName`: string, `contractAmount`: number, `startDate`: string, `endDate`: string, `paymentTerms`: string, `deliveryTerms`: string, `qualityTerms`: string, `penaltyTerms`: string, `status`: integer, `signedBy`: string, `signedByName`: string, `signedAt`: string, `partyARepresentative`: string, `partyAContact`: string, `partyBRepresentative`: string, `partyBContact`: string, `contractFiles`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **OrderItem** — `column`: string, `asc`: boolean
- **PagePurRfq** — `records`: PurRfq[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PagePurRfq, `searchCount`: PagePurRfq, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PagePurRequest** — `records`: PurRequest[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PagePurRequest, `searchCount`: PagePurRequest, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PagePurReceipt** — `records`: PurReceipt[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PagePurReceipt, `searchCount`: PagePurReceipt, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PagePurQuotation** — `records`: PurQuotation[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PagePurQuotation, `searchCount`: PagePurQuotation, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PagePurPriceComparison** — `records`: PurPriceComparison[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PagePurPriceComparison, `searchCount`: PagePurPriceComparison, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PagePurPlan** — `records`: PurPlan[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PagePurPlan, `searchCount`: PagePurPlan, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PagePurContract** — `records`: PurContract[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PagePurContract, `searchCount`: PagePurContract, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponsePurOrder** — `code`: integer, `message`: string, `data`: PurOrder, `timestamp`: integer
- **PurOrder** — `tenantId`: string, `id`: string, `orderNo`: string, `requestId`: string, `requestNo`: string, `supplierId`: string, `supplierName`: string, `supplierContact`: string, `orderType`: integer, `totalAmount`: number, `taxAmount`: number, `freightAmount`: number, `discountAmount`: number, `payableAmount`: number, `status`: integer, `warehouseId`: string, `warehouseName`: string, `warehouseAddress`: string, `orderDate`: string, `expectedDelivery`: string, `actualDelivery`: string, `completedAt`: string, `cancelledAt`: string, `buyerId`: string, `buyerName`: string, `approvedBy`: string, `approvedByName`: string, `approvedAt`: string, `createTime`: string, `updateTime`: string, `deleted`: boolean, `remark`: string
- **ApiResponseListPurOrderItem** — `code`: integer, `message`: string, `data`: PurOrderItem[], `timestamp`: integer
- **ApiResponsePagePurOrder** — `code`: integer, `message`: string, `data`: PagePurOrder, `timestamp`: integer
- **PagePurOrder** — `records`: PurOrder[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PagePurOrder, `searchCount`: PagePurOrder, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer