# scm-tenant — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8212/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-tenant (1.0.0)
- 认证: Bearer JWT (Authorization header)

## Tenant

### PUT /tenant
- 说明: 更新租户
- 描述: 根据租户 ID 更新租户信息并返回更新结果。
- 请求体: `Tenant`
- 响应: `Tenant`

### POST /tenant
- 说明: 创建租户
- 描述: 新增一个租户并返回创建后的租户信息。
- 请求体: `Tenant`
- 响应: `Tenant`

### PUT /tenant/{id}/enable
- 说明: 启用租户
- 描述: 将指定租户状态置为可用，允许其正常访问平台。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### PUT /tenant/{id}/disable
- 说明: 禁用租户
- 描述: 将指定租户状态置为禁用，暂停其访问平台。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant/{id}
- 说明: 查询租户详情
- 描述: 根据租户 ID 查询租户详细信息。
- 参数: `id`(path,必填: string)
- 响应: `Tenant`

### DELETE /tenant/{id}
- 说明: 删除租户
- 描述: 根据租户 ID 删除指定租户。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant/page
- 说明: 分页查询租户
- 描述: 按名称、类型和状态分页查询租户列表。
- 参数: `page`(query: integer) `size`(query: integer) `tenantName`(query: string) `tenantType`(query: integer) `status`(query: integer)
- 响应: `PageTenant`

### GET /tenant/active
- 说明: 查询活跃租户列表
- 描述: 查询当前状态为活跃的全部租户。
- 响应: `200,400,401,403,423,429,500`

## TenantSubscription

### PUT /tenant-subscription
- 说明: 更新租户订阅
- 描述: 根据订阅 ID 更新订阅套餐、有效期等订阅信息。
- 请求体: `TenantSubscription`
- 响应: `TenantSubscription`

### POST /tenant-subscription
- 说明: 创建租户订阅
- 描述: 新增一条租户套餐订阅记录并返回创建结果。
- 请求体: `TenantSubscription`
- 响应: `TenantSubscription`

### POST /tenant-subscription/{id}/unsubscribe
- 说明: 取消租户订阅
- 描述: 根据订阅 ID 取消指定租户的套餐订阅。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /tenant-subscription/subscribe
- 说明: 订阅租户套餐
- 描述: 为指定租户开通指定套餐的订阅。
- 参数: `tenantId`(query,必填: string) `packageId`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-subscription/{id}
- 说明: 查询订阅详情
- 描述: 根据订阅 ID 查询租户订阅详情。
- 参数: `id`(path,必填: string)
- 响应: `TenantSubscription`

### DELETE /tenant-subscription/{id}
- 说明: 删除租户订阅
- 描述: 根据订阅 ID 删除指定的租户订阅记录。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-subscription/tenant/{tenantId}
- 说明: 查询租户订阅列表
- 描述: 查询指定租户的全部订阅记录。
- 参数: `tenantId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-subscription/page
- 说明: 分页查询租户订阅
- 描述: 按租户和订阅状态分页查询租户订阅列表。
- 参数: `page`(query: integer) `size`(query: integer) `tenantId`(query: string) `status`(query: integer)
- 响应: `PageTenantSubscription`

## TenantQuota

### PUT /tenant-resource-quota
- 说明: 更新资源配额
- 描述: 根据配额 ID 更新租户各项资源的上限配置。
- 请求体: `TenantResourceQuota`
- 响应: `TenantResourceQuota`

### POST /tenant-resource-quota
- 说明: 创建资源配额
- 描述: 为指定租户新增资源配额记录并返回创建结果。
- 请求体: `TenantResourceQuota`
- 响应: `TenantResourceQuota`

### GET /tenant-resource-quota/{id}
- 说明: 查询配额详情
- 描述: 根据配额 ID 查询租户资源配额详情。
- 参数: `id`(path,必填: string)
- 响应: `TenantResourceQuota`

### DELETE /tenant-resource-quota/{id}
- 说明: 删除资源配额
- 描述: 根据配额 ID 删除指定的租户资源配额。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-resource-quota/tenant/{tenantId}
- 说明: 查询租户配额列表
- 描述: 查询指定租户的全部资源配额记录。
- 参数: `tenantId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-resource-quota/page
- 说明: 分页查询资源配额
- 描述: 按租户分页查询租户资源配额列表。
- 参数: `page`(query: integer) `size`(query: integer) `tenantId`(query: string)
- 响应: `PageTenantResourceQuota`

### GET /tenant-resource-quota/check
- 说明: 校验资源配额
- 描述: 检查租户在指定资源类型下是否仍有可用配额。
- 参数: `tenantId`(query,必填: string) `resourceType`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

## TenantPlan

### PUT /tenant-package
- 说明: 更新租户套餐
- 描述: 根据套餐 ID 更新套餐配置与资源上限。
- 请求体: `TenantPackage`
- 响应: `TenantPackage`

### POST /tenant-package
- 说明: 创建租户套餐
- 描述: 新增一个租户套餐并返回创建后的套餐信息。
- 请求体: `TenantPackage`
- 响应: `TenantPackage`

### GET /tenant-package/{id}
- 说明: 查询套餐详情
- 描述: 根据套餐 ID 查询租户套餐详情。
- 参数: `id`(path,必填: string)
- 响应: `TenantPackage`

### DELETE /tenant-package/{id}
- 说明: 删除租户套餐
- 描述: 根据套餐 ID 删除指定租户套餐。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-package/page
- 说明: 分页查询租户套餐
- 描述: 按套餐名称、套餐等级和启用状态分页查询租户套餐。
- 参数: `page`(query: integer) `size`(query: integer) `packageName`(query: string) `packageLevel`(query: integer) `enabled`(query: boolean)
- 响应: `PageTenantPackage`

### GET /tenant-package/active
- 说明: 查询已启用套餐
- 描述: 查询当前处于启用状态的全部租户套餐。
- 响应: `200,400,401,403,423,429,500`

## TenantFeature

### PUT /tenant-feature
- 说明: 更新租户功能
- 描述: 根据功能记录 ID 更新租户功能开关配置。
- 请求体: `TenantFeature`
- 响应: `TenantFeature`

### POST /tenant-feature
- 说明: 创建租户功能
- 描述: 为指定租户新增一个功能开关记录并返回创建结果。
- 请求体: `TenantFeature`
- 响应: `TenantFeature`

### GET /tenant-feature/{id}
- 说明: 查询功能详情
- 描述: 根据功能记录 ID 查询租户功能开关详情。
- 参数: `id`(path,必填: string)
- 响应: `TenantFeature`

### DELETE /tenant-feature/{id}
- 说明: 删除租户功能
- 描述: 根据功能记录 ID 删除指定的租户功能开关。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-feature/tenant/{tenantId}
- 说明: 查询租户功能列表
- 描述: 查询指定租户下全部功能开关记录。
- 参数: `tenantId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-feature/page
- 说明: 分页查询租户功能
- 描述: 按租户、功能编码和启用状态分页查询租户功能开关。
- 参数: `page`(query: integer) `size`(query: integer) `tenantId`(query: string) `featureCode`(query: string) `enabled`(query: boolean)
- 响应: `PageTenantFeature`

### GET /tenant-feature/check
- 说明: 校验功能是否启用
- 描述: 判断指定租户的某个功能开关当前是否处于启用状态。
- 参数: `tenantId`(query,必填: string) `featureCode`(query,必填: string)
- 响应: `200,400,401,403,423,429,500`

## TenantConfig

### PUT /tenant-config
- 说明: 更新租户配置
- 描述: 根据配置 ID 更新租户配置内容并返回更新结果。
- 请求体: `TenantConfig`
- 响应: `TenantConfig`

### POST /tenant-config
- 说明: 创建租户配置
- 描述: 新增一条租户配置项并返回创建后的配置内容。
- 请求体: `TenantConfig`
- 响应: `TenantConfig`

### GET /tenant-config/{id}
- 说明: 查询配置详情
- 描述: 根据配置 ID 查询租户配置详情。
- 参数: `id`(path,必填: string)
- 响应: `TenantConfig`

### DELETE /tenant-config/{id}
- 说明: 删除租户配置
- 描述: 根据配置 ID 删除指定租户配置。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-config/tenant/{tenantId}/key/{configKey}
- 说明: 按租户与键查询配置
- 描述: 根据租户 ID 和配置键查询唯一的配置项。
- 参数: `tenantId`(path,必填: string) `configKey`(path,必填: string)
- 响应: `TenantConfig`

### GET /tenant-config/page
- 说明: 分页查询租户配置
- 描述: 按租户、配置分类和配置键分页查询租户配置列表。
- 参数: `page`(query: integer) `size`(query: integer) `tenantId`(query: string) `configCategory`(query: string) `configKey`(query: string)
- 响应: `PageTenantConfig`

## TenantProvisioning

### POST /tenant/provisioning
- 说明: 开通租户
- 描述: 根据入驻信息一次性完成租户及其默认订阅、资源配额的开通流程。
- 请求体: `TenantCreateCommand`
- 响应: `ApiResponseTenantProvisioningResult`

## PlatformFee

### POST /tenant-platform-fee/calculate
- 说明: 计算平台服务费
- 描述: 按账期计算租户平台服务费，支持指定单个租户；同一账期重复执行会更新未支付账单。
- 参数: `billingPeriod`(query: string) `tenantId`(query: string)
- 响应: `ApiResponseInteger`

### GET /tenant-platform-fee/tenant/{tenantId}
- 说明: 查询租户服务费账单
- 描述: 查询指定租户的全部平台服务费账单，按账期倒序。
- 参数: `tenantId`(path,必填: string)
- 响应: `ApiResponseListTenantPlatformFee`

### GET /tenant-platform-fee/page
- 说明: 分页查询服务费账单
- 描述: 按租户、账期和状态分页查询平台服务费账单。
- 参数: `page`(query: integer) `size`(query: integer) `tenantId`(query: string) `billingPeriod`(query: string) `status`(query: integer)
- 响应: `ApiResponsePageTenantPlatformFee`

## TenantOperationLog

### POST /tenant-operation-log
- 说明: 创建操作日志
- 描述: 写入一条租户操作日志记录并返回创建结果。
- 请求体: `TenantOperationLog`
- 响应: `TenantOperationLog`

### GET /tenant-operation-log/{id}
- 说明: 查询日志详情
- 描述: 根据日志 ID 查询租户操作日志详情。
- 参数: `id`(path,必填: string)
- 响应: `TenantOperationLog`

### DELETE /tenant-operation-log/{id}
- 说明: 删除操作日志
- 描述: 根据日志 ID 删除指定的租户操作日志。
- 参数: `id`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-operation-log/tenant/{tenantId}
- 说明: 查询租户日志列表
- 描述: 查询指定租户的全部操作日志。
- 参数: `tenantId`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /tenant-operation-log/page
- 说明: 分页查询操作日志
- 描述: 按租户、操作类型和操作模块分页查询租户操作日志。
- 参数: `page`(query: integer) `size`(query: integer) `tenantId`(query: string) `operationType`(query: string) `operationModule`(query: string)
- 响应: `PageTenantOperationLog`

### GET /tenant-operation-log/operation-type/{operationType}
- 说明: 按操作类型查询日志
- 描述: 查询指定操作类型下的全部租户操作日志。
- 参数: `operationType`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## Schemas

- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **Tenant** — `id`: string, `tenantCode`: string, `tenantName`: string, `tenantNameEn`: string, `tenantType`: integer, `companyName`: string, `legalPerson`: string, `registrationNo`: string, `taxNo`: string, `industry`: string, `companySize`: integer, `contactName`: string, `contactPhone`: string, `contactEmail`: string, `address`: string, `adminUserId`: string, `adminUsername`: string, `adminEmail`: string, `status`: integer, `trialStartDate`: string, `trialEndDate`: string, `contractStartDate`: string, `contractEndDate`: string, `activatedAt`: string, `suspendedAt`: string, `logoUrl`: string, `domain`: string, `themeConfig`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **TenantSubscription** — `id`: string, `tenantId`: string, `packageId`: string, `subscriptionType`: integer, `status`: integer, `startDate`: string, `endDate`: string, `autoRenew`: boolean, `originalPrice`: number, `actualPrice`: number, `discountAmount`: number, `paymentStatus`: integer, `paidAt`: string, `paymentNo`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `remark`: string, `deleted`: boolean
- **TenantResourceQuota** — `id`: string, `tenantId`: string, `maxUsers`: integer, `currentUsers`: integer, `maxWarehouses`: integer, `currentWarehouses`: integer, `maxSkus`: integer, `currentSkus`: integer, `maxOrdersPerDay`: integer, `currentOrdersToday`: integer, `lastOrderResetDate`: string, `maxStorageGb`: integer, `currentStorageGb`: number, `maxApiCallsPerDay`: integer, `currentApiCallsToday`: integer, `lastApiResetDate`: string, `customQuotas`: string, `createTime`: string, `updateTime`: string
- **TenantPackage** — `id`: string, `packageCode`: string, `packageName`: string, `packageLevel`: integer, `priceMonthly`: number, `priceYearly`: number, `discountRate`: number, `maxUsers`: integer, `maxWarehouses`: integer, `maxSkus`: integer, `maxOrdersPerDay`: integer, `maxStorageGb`: integer, `maxApiCallsPerDay`: integer, `features`: string, `description`: string, `highlights`: string, `enabled`: boolean, `isTrial`: boolean, `trialDays`: integer, `sortOrder`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **TenantFeature** — `id`: string, `tenantId`: string, `featureCode`: string, `featureName`: string, `enabled`: boolean, `isBeta`: boolean, `usageLimit`: integer, `currentUsage`: integer, `expireAt`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string
- **TenantConfig** — `id`: string, `tenantId`: string, `configCategory`: string, `configKey`: string, `configValue`: string, `valueType`: string, `description`: string, `defaultValue`: string, `isEncrypted`: boolean, `isPublic`: boolean, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string
- **TenantCreateCommand** — `tenantName`: string, `tenantNameEn`: string, `tenantCode`: string, `tenantType`: integer, `companyName`: string, `contactName`: string, `contactPhone`: string, `contactEmail`: string, `address`: string, `industry`: string, `packageId`: string, `adminUsername`: string, `adminEmail`: string
- **ApiResponseTenantProvisioningResult** — `code`: integer, `message`: string, `data`: TenantProvisioningResult, `timestamp`: integer
- **TenantProvisioningResult** — `tenantId`: string, `tenantCode`: string, `adminUserId`: string, `adminUsername`: string, `adminRoleId`: string, `adminRoleCode`: string, `subscriptionId`: string, `success`: boolean, `errorMessage`: string
- **ApiResponseInteger** — `code`: integer, `message`: string, `data`: integer, `timestamp`: integer
- **TenantOperationLog** — `id`: string, `tenantId`: string, `operationType`: string, `operationModule`: string, `operationDesc`: string, `operatorId`: string, `operatorName`: string, `operatorIp`: string, `userAgent`: string, `requestUrl`: string, `requestMethod`: string, `requestParams`: string, `responseStatus`: integer, `executionTime`: integer, `createTime`: string
- **OrderItem** — `column`: string, `asc`: boolean
- **PageTenant** — `records`: Tenant[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTenant, `searchCount`: PageTenant, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PageTenantSubscription** — `records`: TenantSubscription[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTenantSubscription, `searchCount`: PageTenantSubscription, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PageTenantResourceQuota** — `records`: TenantResourceQuota[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTenantResourceQuota, `searchCount`: PageTenantResourceQuota, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListTenantPlatformFee** — `code`: integer, `message`: string, `data`: TenantPlatformFee[], `timestamp`: integer
- **TenantPlatformFee** — `id`: string, `tenantId`: string, `billingPeriod`: string, `startDate`: string, `endDate`: string, `packageId`: string, `packageName`: string, `baseFee`: number, `storageFee`: number, `apiFee`: number, `totalFee`: number, `discountAmount`: number, `finalFee`: number, `status`: integer, `paidAt`: string, `createTime`: string, `updateTime`: string, `remark`: string
- **ApiResponsePageTenantPlatformFee** — `code`: integer, `message`: string, `data`: PageTenantPlatformFee, `timestamp`: integer
- **PageTenantPlatformFee** — `records`: TenantPlatformFee[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTenantPlatformFee, `searchCount`: PageTenantPlatformFee, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PageTenantPackage** — `records`: TenantPackage[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTenantPackage, `searchCount`: PageTenantPackage, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PageTenantOperationLog** — `records`: TenantOperationLog[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTenantOperationLog, `searchCount`: PageTenantOperationLog, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PageTenantFeature** — `records`: TenantFeature[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTenantFeature, `searchCount`: PageTenantFeature, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PageTenantConfig** — `records`: TenantConfig[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTenantConfig, `searchCount`: PageTenantConfig, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer