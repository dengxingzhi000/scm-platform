# scm-inventory — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8202/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-inventory (1.0.0)
- 认证: Bearer JWT (Authorization header)

## InventoryReservation

### PUT /api/v1/inventory/reservation/confirm/{businessKey}
- 说明: 确认预占
- 描述: 订单支付成功后确认预占，将预占库存转为实际扣减。
- 参数: `businessKey`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### POST /api/v1/inventory/reservation/reserve
- 说明: 预占库存
- 描述: 订单创建时按SKU、仓库和数量预占库存并生成预占记录。
- 请求体: `InventoryReservationRequest`
- 响应: `200,400,401,403,423,429,500`

### POST /api/v1/inventory/reservation/release-expired
- 说明: 释放过期预占
- 描述: 批量释放已超过保留期限的预占记录，供定时任务调用。
- 响应: `200,400,401,403,423,429,500`

### GET /api/v1/inventory/reservation/quantity/{businessKey}
- 说明: 查询预占数量
- 描述: 根据业务键查询对应预占记录占用的库存数量。
- 参数: `businessKey`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### GET /api/v1/inventory/reservation/check/{businessKey}
- 说明: 检查预占是否存在
- 描述: 根据业务键判断对应的预占记录是否仍然存在。
- 参数: `businessKey`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

### DELETE /api/v1/inventory/reservation/release/{businessKey}
- 说明: 释放预占
- 描述: 订单取消或超时后释放预占，将库存归还为可用库存。
- 参数: `businessKey`(path,必填: string)
- 响应: `200,400,401,403,423,429,500`

## 库存预警

### PUT /api/v1/inventory/alerts/{id}/resolve
- 说明: 处理告警
- 描述: 将指定告警标记为已解决，并记录处理人与处理时间。
- 参数: `id`(path,必填: string) `resolvedBy`(query: string)
- 响应: `ApiResponseBoolean`

### POST /api/v1/inventory/alerts/scan
- 说明: 触发告警扫描
- 描述: 扫描当前租户全部库存，生成或刷新库存水位告警，并自动解除已恢复库存的告警。
- 响应: `ApiResponseInvAlertScanResult`

### POST /api/v1/inventory/alerts/query
- 说明: 分页查询告警
- 描述: 按 SKU、仓库、告警类型和解决状态分页查询库存告警。
- 请求体: `InvAlertQueryRequest`
- 响应: `ApiResponsePageInvAlertResponse`

### GET /api/v1/inventory/alerts/{id}
- 说明: 查询告警详情
- 描述: 根据告警 ID 查询告警明细。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseInvAlertResponse`

### GET /api/v1/inventory/alerts/stats
- 说明: 查询告警统计
- 描述: 统计告警总数、未解决数量以及库存不足与缺货告警数量。
- 响应: `ApiResponseInvAlertStatsResponse`

## Inventory

### POST /api/v1/inventory/transfer
- 说明: 库存调拨
- 描述: 将库存从一个仓库调拨到另一个仓库，请求按调拨单号做幂等保护。
- 请求体: `InventoryTransferRequest`
- 响应: `ApiResponseBoolean`

### POST /api/v1/inventory/query
- 说明: 分页查询库存
- 描述: 按请求中的多种过滤条件分页查询库存记录。
- 请求体: `InventoryQueryRequest`
- 响应: `ApiResponsePageInventoryResponse`

### POST /api/v1/inventory/init
- 说明: 初始化库存
- 描述: 为指定SKU和仓库初始化库存记录，可指定初始库存数量，重复请求会被幂等拦截。
- 参数: `skuId`(query,必填: string) `warehouseId`(query,必填: string) `initialStock`(query: integer)
- 响应: `ApiResponseInventoryResponse`

### POST /api/v1/inventory/batch
- 说明: 批量查询库存
- 描述: 根据SKU列表批量查询库存，可限定仓库，单次最多查询100个SKU。
- 参数: `warehouseId`(query: string)
- 响应: `ApiResponseListInventoryResponse`

### POST /api/v1/inventory/adjust
- 说明: 调整库存
- 描述: 对库存执行入库、出库或盘点调整，请求按业务单号做幂等保护。
- 请求体: `InventoryAdjustRequest`
- 响应: `ApiResponseInventoryResponse`

### GET /api/v1/inventory
- 说明: 查询单仓库存
- 描述: 查询指定SKU在指定仓库中的当前库存信息。
- 参数: `skuId`(query,必填: string) `warehouseId`(query,必填: string)
- 响应: `ApiResponseInventoryResponse`

### GET /api/v1/inventory/stats
- 说明: 查询库存统计
- 描述: 获取全局库存统计信息，用于总览与监控。
- 响应: `ApiResponseInventoryStatsResponse`

### GET /api/v1/inventory/check
- 说明: 检查库存是否充足
- 描述: 判断指定SKU在指定仓库的可用库存是否满足所需数量。
- 参数: `skuId`(query,必填: string) `warehouseId`(query,必填: string) `quantity`(query,必填: integer)
- 响应: `ApiResponseBoolean`

## 库存快照

### POST /api/v1/inventory/snapshots/query
- 说明: 分页查询快照
- 描述: 按日期、SKU 和仓库分页查询库存每日快照。
- 请求体: `InvSnapshotQueryRequest`
- 响应: `ApiResponsePageInvSnapshot`

### POST /api/v1/inventory/snapshots/generate
- 说明: 生成库存快照
- 描述: 为当前租户生成指定日期的库存快照，默认使用当天；同日重复执行会跳过已生成的记录。
- 参数: `date`(query: string)
- 响应: `ApiResponseInvSnapshotGenerationResult`

### GET /api/v1/inventory/snapshots/{id}
- 说明: 查询快照详情
- 描述: 根据快照 ID 查询库存快照明细。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseInvSnapshot`

### GET /api/v1/inventory/snapshots/trend
- 说明: 查询库存趋势
- 描述: 查询指定 SKU/仓库最近若干天的库存快照，用于绘制水位趋势。
- 参数: `skuId`(query: string) `warehouseId`(query: string) `days`(query: integer)
- 响应: `ApiResponseListInvSnapshot`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **ApiResponseBoolean** — `code`: integer, `message`: string, `data`: boolean, `timestamp`: integer
- **InventoryTransferRequest** — `skuId`: string, `fromWarehouseId`: string, `toWarehouseId`: string, `quantity`: integer, `transferNo`: string, `operatorId`: string, `operatorName`: string, `remark`: string
- **InvSnapshotQueryRequest** — `snapshotDate`: string, `startDate`: string, `endDate`: string, `skuId`: string, `warehouseId`: string, `page`: integer, `size`: integer
- **ApiResponsePageInvSnapshot** — `code`: integer, `message`: string, `data`: PageInvSnapshot, `timestamp`: integer
- **InvSnapshot** — `tenantId`: string, `id`: string, `snapshotDate`: string, `skuId`: string, `warehouseId`: string, `totalStock`: integer, `availableStock`: integer, `lockedStock`: integer, `damagedStock`: integer, `averageCost`: number, `totalValue`: number, `createTime`: string
- **OrderItem** — `column`: string, `asc`: boolean
- **PageInvSnapshot** — `records`: InvSnapshot[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageInvSnapshot, `searchCount`: PageInvSnapshot, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseInvSnapshotGenerationResult** — `code`: integer, `message`: string, `data`: InvSnapshotGenerationResult, `timestamp`: integer
- **InvSnapshotGenerationResult** — `snapshotDate`: string, `scannedInventories`: integer, `created`: integer, `skipped`: integer, `failed`: integer
- **InventoryReservationRequest** — `skuId`: string, `warehouseId`: string, `quantity`: integer, `businessKey`: string, `timeoutSeconds`: integer, `operatorId`: string, `operatorName`: string, `remark`: string
- **InventoryQueryRequest** — `skuIds`: string[], `warehouseIds`: string[], `locationCode`: string, `stockStatus`: string, `minAvailableStock`: integer, `maxAvailableStock`: integer, `onlyInStock`: boolean, `page`: integer, `size`: integer, `sortBy`: string, `sortOrder`: string
- **ApiResponsePageInventoryResponse** — `code`: integer, `message`: string, `data`: PageInventoryResponse, `timestamp`: integer
- **InventoryResponse** — `id`: string, `skuId`: string, `warehouseId`: string, `totalStock`: integer, `availableStock`: integer, `lockedStock`: integer, `damagedStock`: integer, `safetyStock`: integer, `maxStock`: integer, `locationCode`: string, `averageCost`: number, `stockStatus`: string, `version`: integer, `lastInboundAt`: string, `lastOutboundAt`: string, `createTime`: string, `updateTime`: string, `remark`: string
- **PageInventoryResponse** — `records`: InventoryResponse[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageInventoryResponse, `searchCount`: PageInventoryResponse, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseInventoryResponse** — `code`: integer, `message`: string, `data`: InventoryResponse, `timestamp`: integer
- **ApiResponseListInventoryResponse** — `code`: integer, `message`: string, `data`: InventoryResponse[], `timestamp`: integer
- **ApiResponseInvAlertScanResult** — `code`: integer, `message`: string, `data`: InvAlertScanResult, `timestamp`: integer
- **InvAlertScanResult** — `scannedInventories`: integer, `created`: integer, `updated`: integer, `resolved`: integer
- **InvAlertQueryRequest** — `skuId`: string, `warehouseId`: string, `alertType`: integer, `isResolved`: boolean, `page`: integer, `size`: integer
- **ApiResponsePageInvAlertResponse** — `code`: integer, `message`: string, `data`: PageInvAlertResponse, `timestamp`: integer
- **InvAlertResponse** — `id`: string, `skuId`: string, `warehouseId`: string, `alertType`: integer, `alertTypeName`: string, `currentStock`: integer, `safetyStock`: integer, `thresholdValue`: integer, `isResolved`: boolean, `resolvedAt`: string, `resolvedBy`: string, `notified`: boolean, `notifiedAt`: string, `createTime`: string, `updateTime`: string, `remark`: string
- **PageInvAlertResponse** — `records`: InvAlertResponse[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageInvAlertResponse, `searchCount`: PageInvAlertResponse, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **InventoryAdjustRequest** — `skuId`: string, `warehouseId`: string, `quantity`: integer, `adjustType`: integer, `referenceNo`: string, `referenceId`: string, `operatorId`: string, `operatorName`: string, `remark`: string
- **ApiResponseInventoryStatsResponse** — `code`: integer, `message`: string, `data`: InventoryStatsResponse, `timestamp`: integer
- **InventoryStatsResponse** — `totalSkuCount`: integer, `totalWarehouseCount`: integer, `totalStockQuantity`: integer, `availableStockQuantity`: integer, `lockedStockQuantity`: integer, `damagedStockQuantity`: integer, `totalStockValue`: number, `outOfStockCount`: integer, `lowStockCount`: integer, `normalStockCount`: integer
- **ApiResponseInvSnapshot** — `code`: integer, `message`: string, `data`: InvSnapshot, `timestamp`: integer
- **ApiResponseListInvSnapshot** — `code`: integer, `message`: string, `data`: InvSnapshot[], `timestamp`: integer
- **ApiResponseInvAlertResponse** — `code`: integer, `message`: string, `data`: InvAlertResponse, `timestamp`: integer
- **ApiResponseInvAlertStatsResponse** — `code`: integer, `message`: string, `data`: InvAlertStatsResponse, `timestamp`: integer
- **InvAlertStatsResponse** — `total`: integer, `unresolved`: integer, `resolved`: integer, `lowStock`: integer, `outOfStock`: integer