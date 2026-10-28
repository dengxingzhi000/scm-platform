# scm-inventory — OpenAPI 文档摘要

- 来源: `http://192.168.0.125:8202/api/public/v3/api-docs`
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

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **InventoryTransferRequest** — `skuId`: string, `fromWarehouseId`: string, `toWarehouseId`: string, `quantity`: integer, `transferNo`: string, `operatorId`: string, `operatorName`: string, `remark`: string
- **ApiResponseBoolean** — `code`: integer, `message`: string, `data`: boolean, `timestamp`: integer
- **InventoryReservationRequest** — `skuId`: string, `warehouseId`: string, `quantity`: integer, `businessKey`: string, `timeoutSeconds`: integer, `operatorId`: string, `operatorName`: string, `remark`: string
- **InventoryQueryRequest** — `skuIds`: string[], `warehouseIds`: string[], `locationCode`: string, `stockStatus`: string, `minAvailableStock`: integer, `maxAvailableStock`: integer, `onlyInStock`: boolean, `page`: integer, `size`: integer, `sortBy`: string, `sortOrder`: string
- **ApiResponsePageInventoryResponse** — `code`: integer, `message`: string, `data`: PageInventoryResponse, `timestamp`: integer
- **InventoryResponse** — `id`: string, `skuId`: string, `warehouseId`: string, `totalStock`: integer, `availableStock`: integer, `lockedStock`: integer, `damagedStock`: integer, `safetyStock`: integer, `maxStock`: integer, `locationCode`: string, `averageCost`: number, `stockStatus`: string, `version`: integer, `lastInboundAt`: string, `lastOutboundAt`: string, `createTime`: string, `updateTime`: string, `remark`: string
- **OrderItem** — `column`: string, `asc`: boolean
- **PageInventoryResponse** — `records`: InventoryResponse[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageInventoryResponse, `searchCount`: PageInventoryResponse, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseInventoryResponse** — `code`: integer, `message`: string, `data`: InventoryResponse, `timestamp`: integer
- **ApiResponseListInventoryResponse** — `code`: integer, `message`: string, `data`: InventoryResponse[], `timestamp`: integer
- **InventoryAdjustRequest** — `skuId`: string, `warehouseId`: string, `quantity`: integer, `adjustType`: integer, `referenceNo`: string, `referenceId`: string, `operatorId`: string, `operatorName`: string, `remark`: string
- **ApiResponseInventoryStatsResponse** — `code`: integer, `message`: string, `data`: InventoryStatsResponse, `timestamp`: integer
- **InventoryStatsResponse** — `totalSkuCount`: integer, `totalWarehouseCount`: integer, `totalStockQuantity`: integer, `availableStockQuantity`: integer, `lockedStockQuantity`: integer, `damagedStockQuantity`: integer, `totalStockValue`: number, `outOfStockCount`: integer, `lowStockCount`: integer, `normalStockCount`: integer