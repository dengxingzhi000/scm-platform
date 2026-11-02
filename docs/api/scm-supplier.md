# scm-supplier — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8206/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-supplier (1.0.0)
- 认证: Bearer JWT (Authorization header)

## 供应商联系人

### PUT /api/v1/suppliers/{supplierId}/contacts/{id}
- 说明: 更新联系人
- 描述: 更新联系人信息；设为主联系人时会自动取消其他主联系人。
- 参数: `supplierId`(path,必填: string) `id`(path,必填: string)
- 请求体: `SupSupplierContact`
- 响应: `ApiResponseSupSupplierContact`

### DELETE /api/v1/suppliers/{supplierId}/contacts/{id}
- 说明: 删除联系人
- 描述: 删除联系人；若为主联系人则自动提升下一位联系人。
- 参数: `supplierId`(path,必填: string) `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/suppliers/{supplierId}/contacts/{id}/primary
- 说明: 设置主联系人
- 描述: 将指定联系人设为主联系人，同步供应商档案联系方式。
- 参数: `supplierId`(path,必填: string) `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/suppliers/{supplierId}/contacts
- 说明: 查询联系人列表
- 描述: 查询指定供应商的全部联系人，主联系人置顶。
- 参数: `supplierId`(path,必填: string)
- 响应: `ApiResponseListSupSupplierContact`

### POST /api/v1/suppliers/{supplierId}/contacts
- 说明: 新增联系人
- 描述: 新增供应商联系人；首个联系人自动成为主联系人。
- 参数: `supplierId`(path,必填: string)
- 请求体: `SupSupplierContact`
- 响应: `ApiResponseSupSupplierContact`

## 供应商资质证照

### PUT /api/v1/suppliers/{supplierId}/certificates/{id}
- 说明: 更新资质证照
- 描述: 更新资质证照；有效期变化时重新判定有效/临期/过期。
- 参数: `supplierId`(path,必填: string) `id`(path,必填: string)
- 请求体: `SupSupplierCertificate`
- 响应: `ApiResponseSupSupplierCertificate`

### DELETE /api/v1/suppliers/{supplierId}/certificates/{id}
- 说明: 删除资质证照
- 描述: 逻辑删除供应商资质证照。
- 参数: `supplierId`(path,必填: string) `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/suppliers/{supplierId}/certificates
- 说明: 查询资质列表
- 描述: 按有效期升序查询指定供应商的资质证照。
- 参数: `supplierId`(path,必填: string)
- 响应: `ApiResponseListSupSupplierCertificate`

### POST /api/v1/suppliers/{supplierId}/certificates
- 说明: 新增资质证照
- 描述: 新增供应商资质，未指定状态时按有效期自动判定。
- 参数: `supplierId`(path,必填: string)
- 请求体: `SupSupplierCertificate`
- 响应: `ApiResponseSupSupplierCertificate`

## 供应商银行账户

### PUT /api/v1/suppliers/{supplierId}/bank-accounts/{id}
- 说明: 更新银行账户
- 描述: 更新银行账户；设为默认时会自动取消其他默认账户。
- 参数: `supplierId`(path,必填: string) `id`(path,必填: string)
- 请求体: `SupSupplierBankAccount`
- 响应: `ApiResponseSupSupplierBankAccount`

### DELETE /api/v1/suppliers/{supplierId}/bank-accounts/{id}
- 说明: 删除银行账户
- 描述: 删除银行账户；若为默认账户则自动提升下一位账户。
- 参数: `supplierId`(path,必填: string) `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/suppliers/{supplierId}/bank-accounts/{id}/default
- 说明: 设置默认账户
- 描述: 将指定账户设为默认结算账户，同步供应商档案银行信息。
- 参数: `supplierId`(path,必填: string) `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/suppliers/{supplierId}/bank-accounts
- 说明: 查询银行账户列表
- 描述: 查询指定供应商的全部银行账户，默认账户置顶。
- 参数: `supplierId`(path,必填: string)
- 响应: `ApiResponseListSupSupplierBankAccount`

### POST /api/v1/suppliers/{supplierId}/bank-accounts
- 说明: 新增银行账户
- 描述: 新增供应商银行账户；首个账户自动成为默认账户。
- 参数: `supplierId`(path,必填: string)
- 请求体: `SupSupplierBankAccount`
- 响应: `ApiResponseSupSupplierBankAccount`

## Supplier

### GET /api/v1/suppliers/{id}
- 说明: 查询供应商详情
- 描述: 根据供应商ID查询未删除的供应商详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseSupSupplier`

### PUT /api/v1/suppliers/{id}
- 说明: 更新供应商
- 描述: 更新供应商主数据；准入状态、黑名单与评分由流程维护。
- 参数: `id`(path,必填: string)
- 请求体: `SupplierUpsertRequest`
- 响应: `ApiResponseSupSupplier`

### DELETE /api/v1/suppliers/{id}
- 说明: 删除供应商
- 描述: 逻辑删除供应商；合作中的供应商需先终止合作。
- 参数: `id`(path,必填: string) `operatorId`(query: string) `operatorName`(query: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/suppliers/{id}/unblacklist
- 说明: 移出黑名单
- 描述: 将供应商移出黑名单，启用状态与准入状态保持不变。
- 参数: `id`(path,必填: string) `operatorId`(query: string) `operatorName`(query: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/suppliers/{id}/enable
- 说明: 启用供应商
- 描述: 仅准入审核通过且未列入黑名单的供应商可启用。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/suppliers/{id}/disable
- 说明: 停用供应商
- 描述: 将指定供应商状态置为停用。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/suppliers/{id}/cooperation-status
- 说明: 变更合作状态
- 描述: 合作状态 0-潜在,1-合作中,2-暂停,3-终止，按合法路径流转。
- 参数: `id`(path,必填: string) `status`(query,必填: integer) `operatorId`(query: string) `operatorName`(query: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/suppliers/{id}/categories
- 说明: 查询供应商分类
- 描述: 查询指定供应商已关联的分类。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseListSupSupplierCategory`

### PUT /api/v1/suppliers/{id}/categories
- 说明: 设置供应商分类
- 描述: 全量替换指定供应商的分类关联。
- 参数: `id`(path,必填: string) `operatorId`(query: string) `operatorName`(query: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/suppliers/{id}/blacklist
- 说明: 列入黑名单
- 描述: 将供应商列入黑名单，同时停用并终止合作。
- 参数: `id`(path,必填: string) `reason`(query,必填: string) `operatorId`(query: string) `operatorName`(query: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/suppliers/{id}/audit
- 说明: 审核供应商准入
- 描述: APPROVE 通过并启用供应商；REJECT 驳回并记录原因。
- 参数: `id`(path,必填: string)
- 请求体: `SupplierAuditRequest`
- 响应: `ApiResponseVoid`

### GET /api/v1/suppliers
- 说明: 分页查询供应商
- 描述: 按关键字、类型、合作/准入/分级/风险等条件分页查询供应商。
- 参数: `page`(query: integer) `size`(query: integer) `keyword`(query: string) `supplierType`(query: integer) `cooperationStatus`(query: integer) `enabled`(query: boolean) `auditStatus`(query: integer) `supplierLevel`(query: integer) `riskLevel`(query: integer) `blacklisted`(query: boolean) `categoryId`(query: string)
- 响应: `ApiResponsePageSupSupplier`

### POST /api/v1/suppliers
- 说明: 创建供应商
- 描述: 新增供应商主数据，初始化为草稿准入状态并自动生成编码。
- 请求体: `SupplierUpsertRequest`
- 响应: `ApiResponseSupSupplier`

### POST /api/v1/suppliers/{id}/submit-audit
- 说明: 提交准入审核
- 描述: 将草稿或被驳回的供应商提交审核，要求税号已补全。
- 参数: `id`(path,必填: string) `operatorId`(query: string) `operatorName`(query: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/suppliers/{id}/profile
- 说明: 查询供应商画像
- 描述: 返回主数据、联系人、银行账户、资质、分类与最近变更日志。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseSupplierProfileVO`

### GET /api/v1/suppliers/{id}/performance
- 说明: 查询供应商绩效评分卡
- 描述: 按质量/交付/服务/价格维度汇总评价得分并给出分级建议。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseSupplierPerformanceVO`

### GET /api/v1/suppliers/{id}/change-logs
- 说明: 查询供应商变更日志
- 描述: 按时间倒序返回供应商主数据、准入与黑名单变更记录。
- 参数: `id`(path,必填: string) `limit`(query: integer)
- 响应: `ApiResponseListSupSupplierChangeLog`

### GET /api/v1/suppliers/stats
- 说明: 查询供应商统计
- 描述: 返回供应商总数、启用数、合作中、待审核、黑名单与资质到期数量。
- 响应: `ApiResponseSupplierStatsVO`

### GET /api/v1/suppliers/search
- 说明: 搜索供应商
- 描述: 根据供应商名称关键字模糊搜索供应商。
- 参数: `name`(query,必填: string)
- 响应: `ApiResponseListSupSupplier`

### GET /api/v1/suppliers/active
- 说明: 查询可合作供应商
- 描述: 查询启用、准入通过、非黑名单且合作中的供应商。
- 响应: `ApiResponseListSupSupplier`

## SupplierEvaluation

### GET /api/v1/supplier-evaluations/{id}
- 说明: 查询评价详情
- 描述: 根据评价ID查询供应商评价详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseSupSupplierEvaluation`

### PUT /api/v1/supplier-evaluations/{id}
- 说明: 更新评价
- 描述: 根据评价ID更新供应商评价并重新计算总分、回写供应商绩效。
- 参数: `id`(path,必填: string)
- 请求体: `SupSupplierEvaluation`
- 响应: `ApiResponseSupSupplierEvaluation`

### DELETE /api/v1/supplier-evaluations/{id}
- 说明: 删除评价
- 描述: 根据评价ID删除供应商评价并重新回写供应商绩效。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/supplier-evaluations
- 说明: 分页查询评价
- 描述: 按供应商与评价周期分页查询供应商评价列表。
- 参数: `page`(query: integer) `size`(query: integer) `supplierId`(query: string) `evaluationPeriod`(query: string)
- 响应: `ApiResponsePageSupSupplierEvaluation`

### POST /api/v1/supplier-evaluations
- 说明: 创建供应商评价
- 描述: 新增供应商评价，按质量/交付/服务/价格加权计算总分并回写供应商绩效。
- 请求体: `SupSupplierEvaluation`
- 响应: `ApiResponseSupSupplierEvaluation`

### GET /api/v1/supplier-evaluations/supplier/{supplierId}
- 说明: 查询供应商评价列表
- 描述: 查询指定供应商的全部评价记录。
- 参数: `supplierId`(path,必填: string)
- 响应: `ApiResponseListSupSupplierEvaluation`

### GET /api/v1/supplier-evaluations/supplier/{supplierId}/average-score
- 说明: 查询供应商平均得分
- 描述: 计算并返回指定供应商的平均评价得分。
- 参数: `supplierId`(path,必填: string)
- 响应: `ApiResponseBigDecimal`

## 供应商分类字典

### PUT /api/v1/supplier-categories/{id}
- 说明: 更新分类
- 描述: 更新分类名称、上级与排序。
- 参数: `id`(path,必填: string)
- 请求体: `SupSupplierCategory`
- 响应: `ApiResponseSupSupplierCategory`

### DELETE /api/v1/supplier-categories/{id}
- 说明: 删除分类
- 描述: 删除未被供应商引用的分类。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/supplier-categories
- 说明: 查询分类列表
- 描述: 按层级与排序返回分类字典，可按关键字与启用状态过滤。
- 参数: `keyword`(query: string) `enabled`(query: boolean)
- 响应: `ApiResponseListSupSupplierCategory`

### POST /api/v1/supplier-categories
- 说明: 新增分类
- 描述: 新增供应商分类，编码在租户内唯一。
- 请求体: `SupSupplierCategory`
- 响应: `ApiResponseSupSupplierCategory`

## SupplierSettlement

### GET /api/v1/settlements/{id}
- 说明: 查询对账单详情
- 描述: 根据对账单ID查询未删除的对账单详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseSupSettlement`

### PUT /api/v1/settlements/{id}
- 说明: 更新对账单
- 描述: 根据对账单ID更新对账单信息并校验状态变更是否合法。
- 参数: `id`(path,必填: string)
- 请求体: `SupSettlement`
- 响应: `ApiResponseSupSettlement`

### DELETE /api/v1/settlements/{id}
- 说明: 删除对账单
- 描述: 对指定对账单执行逻辑删除。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/settlements/{id}/pay
- 说明: 标记对账单已付款
- 描述: 将对账单标记为已付款并记录更新人。
- 参数: `id`(path,必填: string) `updateBy`(query,必填: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/settlements/{id}/confirm
- 说明: 确认对账单
- 描述: 确认供应商对账单并记录审核人信息。
- 参数: `id`(path,必填: string) `approverId`(query,必填: string) `approverName`(query,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/settlements
- 说明: 分页查询对账单
- 描述: 按供应商、状态与结算周期分页查询对账单列表。
- 参数: `page`(query: integer) `size`(query: integer) `supplierId`(query: string) `status`(query: integer) `settlementPeriod`(query: string)
- 响应: `ApiResponsePageSupSettlement`

### POST /api/v1/settlements
- 说明: 创建对账单
- 描述: 新建供应商对账单并初始化为草稿状态。
- 请求体: `SupSettlement`
- 响应: `ApiResponseSupSettlement`

### GET /api/v1/settlements/supplier/{supplierId}
- 说明: 查询供应商对账单
- 描述: 查询指定供应商的全部对账单。
- 参数: `supplierId`(path,必填: string)
- 响应: `ApiResponseListSupSettlement`

## SupplierPurchaseItem

### GET /api/v1/purchase-order-items/{id}
- 说明: 查询采购单明细
- 描述: 根据明细ID查询采购订单明细详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseSupPurchaseOrderItem`

### PUT /api/v1/purchase-order-items/{id}
- 说明: 更新采购单明细
- 描述: 根据明细ID更新采购订单明细，并重新计算小计金额。
- 参数: `id`(path,必填: string)
- 请求体: `SupPurchaseOrderItem`
- 响应: `ApiResponseSupPurchaseOrderItem`

### DELETE /api/v1/purchase-order-items/{id}
- 说明: 删除采购单明细
- 描述: 根据明细ID删除采购订单明细。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/purchase-order-items
- 说明: 分页查询采购单明细
- 描述: 按采购单ID与SKU分页查询采购订单明细列表。
- 参数: `page`(query: integer) `size`(query: integer) `purchaseId`(query: string) `skuId`(query: string)
- 响应: `ApiResponsePageSupPurchaseOrderItem`

### POST /api/v1/purchase-order-items
- 说明: 创建采购单明细
- 描述: 新增采购订单明细，未填写小计时按单价与数量自动计算。
- 请求体: `SupPurchaseOrderItem`
- 响应: `ApiResponseSupPurchaseOrderItem`

### GET /api/v1/purchase-order-items/purchase/{purchaseId}
- 说明: 查询采购单明细列表
- 描述: 查询指定采购单下的全部明细。
- 参数: `purchaseId`(path,必填: string)
- 响应: `ApiResponseListSupPurchaseOrderItem`

## 供应商资质到期预警

### GET /api/v1/supplier-certificates/expiring
- 说明: 查询即将到期的资质
- 描述: 按到期日升序返回未来 N 天内到期的资质证照。
- 参数: `page`(query: integer) `size`(query: integer) `days`(query: integer)
- 响应: `ApiResponsePageSupSupplierCertificate`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **SupSupplierContact** — `tenantId`: string, `id`: string, `supplierId`: string, `contactName`: string, `contactTitle`: string, `contactRole`: string, `contactPhone`: string, `contactEmail`: string, `contactAddress`: string, `isPrimary`: boolean, `enabled`: boolean, `sortOrder`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseSupSupplierContact** — `code`: integer, `message`: string, `data`: SupSupplierContact, `timestamp`: integer
- **SupSupplierCertificate** — `tenantId`: string, `id`: string, `supplierId`: string, `certificateType`: string, `certificateName`: string, `certificateNo`: string, `issuingAuthority`: string, `validFrom`: string, `validTo`: string, `fileUrl`: string, `status`: integer, `verifiedBy`: string, `verifiedAt`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string, `daysToExpiry`: integer, `supplierName`: string
- **ApiResponseSupSupplierCertificate** — `code`: integer, `message`: string, `data`: SupSupplierCertificate, `timestamp`: integer
- **SupSupplierBankAccount** — `tenantId`: string, `id`: string, `supplierId`: string, `bankName`: string, `bankBranch`: string, `bankAccount`: string, `accountName`: string, `currency`: string, `swiftCode`: string, `isDefault`: boolean, `enabled`: boolean, `sortOrder`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseSupSupplierBankAccount** — `code`: integer, `message`: string, `data`: SupSupplierBankAccount, `timestamp`: integer
- **SupplierUpsertRequest** — `supplierCode`: string, `supplierName`: string, `supplierNameEn`: string, `supplierType`: integer, `businessType`: string, `legalPerson`: string, `registeredCapital`: number, `registrationNo`: string, `taxNo`: string, `establishmentDate`: string, `contactName`: string, `contactPhone`: string, `contactEmail`: string, `contactAddress`: string, `bankName`: string, `bankAccount`: string, `bankAccountName`: string, `creditRating`: string, `cooperationStartDate`: string, `paymentTerms`: string, `paymentMethod`: string, `businessLicense`: string, `certificates`: string, `website`: string, `sortOrder`: integer, `remark`: string
- **ApiResponseSupSupplier** — `code`: integer, `message`: string, `data`: SupSupplier, `timestamp`: integer
- **SupSupplier** — `tenantId`: string, `id`: string, `supplierCode`: string, `supplierName`: string, `supplierNameEn`: string, `supplierType`: integer, `businessType`: string, `legalPerson`: string, `registeredCapital`: number, `registrationNo`: string, `taxNo`: string, `establishmentDate`: string, `contactName`: string, `contactPhone`: string, `contactEmail`: string, `contactAddress`: string, `bankName`: string, `bankAccount`: string, `bankAccountName`: string, `creditRating`: string, `qualityScore`: number, `deliveryScore`: number, `serviceScore`: number, `cooperationStartDate`: string, `cooperationStatus`: integer, `paymentTerms`: string, `paymentMethod`: string, `businessLicense`: string, `certificates`: string, `website`: string, `supplierLevel`: integer, `auditStatus`: integer, `auditRemark`: string, `auditedBy`: string, `auditedAt`: string, `riskLevel`: integer, `blacklisted`: boolean, `blacklistReason`: string, `blacklistedAt`: string, `overallScore`: number, `lastEvaluationAt`: string, `enabled`: boolean, `sortOrder`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **SupplierAuditRequest** — `action`: string, `operatorId`: string, `operatorName`: string, `remark`: string
- **SupSupplierEvaluation** — `tenantId`: string, `id`: string, `supplierId`: string, `evaluationPeriod`: string, `startDate`: string, `endDate`: string, `qualityScore`: number, `deliveryScore`: number, `serviceScore`: number, `priceScore`: number, `totalScore`: number, `purchaseCount`: integer, `totalAmount`: number, `onTimeDeliveryRate`: number, `qualityPassRate`: number, `evaluatorId`: string, `evaluatorName`: string, `evaluatedAt`: string, `improvementSuggestions`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `remark`: string
- **ApiResponseSupSupplierEvaluation** — `code`: integer, `message`: string, `data`: SupSupplierEvaluation, `timestamp`: integer
- **SupSupplierCategory** — `tenantId`: string, `id`: string, `categoryCode`: string, `categoryName`: string, `parentId`: string, `level`: integer, `enabled`: boolean, `sortOrder`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseSupSupplierCategory** — `code`: integer, `message`: string, `data`: SupSupplierCategory, `timestamp`: integer
- **SupSettlement** — `tenantId`: string, `id`: string, `settlementNo`: string, `supplierId`: string, `supplierName`: string, `settlementPeriod`: string, `startDate`: string, `endDate`: string, `purchaseOrderIds`: string, `purchaseCount`: integer, `totalAmount`: number, `discountAmount`: number, `actualAmount`: number, `paymentAmount`: number, `paymentDate`: string, `status`: integer, `approverId`: string, `approverName`: string, `approvedAt`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseSupSettlement** — `code`: integer, `message`: string, `data`: SupSettlement, `timestamp`: integer
- **SupPurchaseOrderItem** — `tenantId`: string, `id`: string, `purchaseId`: string, `purchaseNo`: string, `skuId`: string, `skuCode`: string, `skuName`: string, `quantity`: integer, `receivedQuantity`: integer, `unitPrice`: number, `taxRate`: number, `subtotal`: number, `qualityRequirement`: string, `createTime`: string, `remark`: string
- **ApiResponseSupPurchaseOrderItem** — `code`: integer, `message`: string, `data`: SupPurchaseOrderItem, `timestamp`: integer
- **ApiResponsePageSupSupplier** — `code`: integer, `message`: string, `data`: PageSupSupplier, `timestamp`: integer
- **OrderItem** — `column`: string, `asc`: boolean
- **PageSupSupplier** — `records`: SupSupplier[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSupSupplier, `searchCount`: PageSupSupplier, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListSupSupplierContact** — `code`: integer, `message`: string, `data`: SupSupplierContact[], `timestamp`: integer
- **ApiResponseListSupSupplierCertificate** — `code`: integer, `message`: string, `data`: SupSupplierCertificate[], `timestamp`: integer
- **ApiResponseListSupSupplierBankAccount** — `code`: integer, `message`: string, `data`: SupSupplierBankAccount[], `timestamp`: integer
- **ApiResponseSupplierProfileVO** — `code`: integer, `message`: string, `data`: SupplierProfileVO, `timestamp`: integer
- **SupSupplierChangeLog** — `tenantId`: string, `id`: string, `supplierId`: string, `changeType`: string, `changeContent`: string, `reason`: string, `operatorId`: string, `operatorName`: string, `createTime`: string
- **SupplierProfileVO** — `supplier`: SupSupplier, `contacts`: SupSupplierContact[], `bankAccounts`: SupSupplierBankAccount[], `certificates`: SupSupplierCertificate[], `categories`: SupSupplierCategory[], `changeLogs`: SupSupplierChangeLog[]
- **ApiResponseSupplierPerformanceVO** — `code`: integer, `message`: string, `data`: SupplierPerformanceVO, `timestamp`: integer
- **SupplierPerformanceVO** — `supplierId`: string, `supplierName`: string, `supplierLevel`: integer, `overallScore`: number, `qualityScore`: number, `deliveryScore`: number, `serviceScore`: number, `priceScore`: number, `evaluationCount`: integer, `latestPeriod`: string, `latestTotalScore`: number, `latestEvaluatedAt`: string, `suggestedLevel`: integer, `recentEvaluations`: SupSupplierEvaluation[]
- **ApiResponseListSupSupplierChangeLog** — `code`: integer, `message`: string, `data`: SupSupplierChangeLog[], `timestamp`: integer
- **ApiResponseListSupSupplierCategory** — `code`: integer, `message`: string, `data`: SupSupplierCategory[], `timestamp`: integer
- **ApiResponseSupplierStatsVO** — `code`: integer, `message`: string, `data`: SupplierStatsVO, `timestamp`: integer
- **SupplierStatsVO** — `total`: integer, `enabled`: integer, `cooperating`: integer, `pendingAudit`: integer, `blacklisted`: integer, `expiringCertificates`: integer, `byCooperationStatus`: object, `byAuditStatus`: object
- **ApiResponseListSupSupplier** — `code`: integer, `message`: string, `data`: SupSupplier[], `timestamp`: integer
- **ApiResponsePageSupSupplierEvaluation** — `code`: integer, `message`: string, `data`: PageSupSupplierEvaluation, `timestamp`: integer
- **PageSupSupplierEvaluation** — `records`: SupSupplierEvaluation[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSupSupplierEvaluation, `searchCount`: PageSupSupplierEvaluation, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListSupSupplierEvaluation** — `code`: integer, `message`: string, `data`: SupSupplierEvaluation[], `timestamp`: integer
- **ApiResponseBigDecimal** — `code`: integer, `message`: string, `data`: number, `timestamp`: integer
- **ApiResponsePageSupSupplierCertificate** — `code`: integer, `message`: string, `data`: PageSupSupplierCertificate, `timestamp`: integer
- **PageSupSupplierCertificate** — `records`: SupSupplierCertificate[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSupSupplierCertificate, `searchCount`: PageSupSupplierCertificate, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponsePageSupSettlement** — `code`: integer, `message`: string, `data`: PageSupSettlement, `timestamp`: integer
- **PageSupSettlement** — `records`: SupSettlement[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSupSettlement, `searchCount`: PageSupSettlement, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListSupSettlement** — `code`: integer, `message`: string, `data`: SupSettlement[], `timestamp`: integer
- **ApiResponsePageSupPurchaseOrderItem** — `code`: integer, `message`: string, `data`: PageSupPurchaseOrderItem, `timestamp`: integer
- **PageSupPurchaseOrderItem** — `records`: SupPurchaseOrderItem[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageSupPurchaseOrderItem, `searchCount`: PageSupPurchaseOrderItem, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListSupPurchaseOrderItem** — `code`: integer, `message`: string, `data`: SupPurchaseOrderItem[], `timestamp`: integer