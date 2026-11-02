# scm-warehouse — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8204/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-warehouse (1.0.0)
- 认证: Bearer JWT (Authorization header)

## WavePicking

### GET /wms-wave-picking/{id}
- 说明: 查询波次拣货单详情
- 描述: 按主键查询波次拣货单，单据不存在时返回 404。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseWmsWavePicking`

### PUT /wms-wave-picking/{id}
- 说明: 更新波次拣货单
- 描述: 按主键更新波次拣货单信息，仅未开始拣货的波次允许修改。
- 参数: `id`(path,必填: string)
- 请求体: `WmsWavePicking`
- 响应: `ApiResponseWmsWavePicking`

### DELETE /wms-wave-picking/{id}
- 说明: 删除波次拣货单
- 描述: 按主键删除波次拣货单，删除后不再参与拣货作业。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /wms-wave-picking/{id}/start
- 说明: 开始拣货
- 描述: 指派拣货员并开始执行指定波次拣货单。
- 参数: `id`(path,必填: string) `pickerId`(query,必填: string) `pickerName`(query,必填: string)
- 响应: `ApiResponseVoid`

### PUT /wms-wave-picking/{id}/complete
- 说明: 完成拣货
- 描述: 将指定波次拣货单标记为拣货完成并记录操作人。
- 参数: `id`(path,必填: string) `operatorId`(query,必填: string)
- 响应: `ApiResponseVoid`

### PUT /wms-wave-picking/{id}/cancel
- 说明: 取消波次拣货单
- 描述: 取消指定波次拣货单并记录操作人，已完成拣货的波次不允许取消。
- 参数: `id`(path,必填: string) `operatorId`(query,必填: string)
- 响应: `ApiResponseVoid`

### POST /wms-wave-picking
- 说明: 创建波次拣货单
- 描述: 按仓库与订单数量新建波次拣货单并生成波次号。
- 请求体: `WmsWavePicking`
- 响应: `ApiResponseWmsWavePicking`

### GET /wms-wave-picking/page
- 说明: 分页查询波次拣货单
- 描述: 按仓库与波次状态分页查询波次拣货单列表。
- 参数: `page`(query: integer) `size`(query: integer) `warehouseId`(query: string) `status`(query: integer)
- 响应: `ApiResponsePageWmsWavePicking`

## Warehouse

### GET /wms-warehouse/{id}
- 说明: 查询仓库详情
- 描述: 按主键查询仓库，仓库不存在或已删除时返回 404。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseWmsWarehouse`

### PUT /wms-warehouse/{id}
- 说明: 更新仓库
- 描述: 按主键更新仓库的基础信息与联系人信息。
- 参数: `id`(path,必填: string)
- 请求体: `WmsWarehouse`
- 响应: `ApiResponseWmsWarehouse`

### DELETE /wms-warehouse/{id}
- 说明: 删除仓库
- 描述: 按主键逻辑删除仓库，已删除的仓库不再参与出入库作业。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /wms-warehouse/{id}/enable
- 说明: 启用仓库
- 描述: 将指定仓库置为启用状态，启用后可参与出入库作业。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /wms-warehouse/{id}/disable
- 说明: 停用仓库
- 描述: 将指定仓库置为停用状态，停用后不再接收新的出入库作业。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /wms-warehouse
- 说明: 创建仓库
- 描述: 新建仓库档案，仓库编码需保持唯一。
- 请求体: `WmsWarehouse`
- 响应: `ApiResponseWmsWarehouse`

### GET /wms-warehouse/page
- 说明: 分页查询仓库
- 描述: 按仓库名称、仓库类型与启用状态分页查询仓库列表。
- 参数: `page`(query: integer) `size`(query: integer) `warehouseName`(query: string) `warehouseType`(query: integer) `enabled`(query: boolean)
- 响应: `ApiResponsePageWmsWarehouse`

### GET /wms-warehouse/list
- 说明: 查询启用仓库列表
- 描述: 查询全部处于启用状态的仓库，用于下拉选择。
- 响应: `ApiResponseListWmsWarehouse`

## Outbound

### GET /wms-outbound/{id}
- 说明: 查询出库单详情
- 描述: 按主键查询出库单，单据不存在或已删除时返回 404。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseWmsOutbound`

### PUT /wms-outbound/{id}
- 说明: 更新出库单
- 描述: 按主键更新出库单信息，仅允许在未发货状态下修改。
- 参数: `id`(path,必填: string)
- 请求体: `WmsOutbound`
- 响应: `ApiResponseWmsOutbound`

### DELETE /wms-outbound/{id}
- 说明: 删除出库单
- 描述: 按主键逻辑删除出库单，已删除的单据不再对外可见。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /wms-outbound/{id}/ship
- 说明: 出库发货
- 描述: 对指定出库单执行发货确认，并记录操作人信息。
- 参数: `id`(path,必填: string) `operatorId`(query,必填: string) `operatorName`(query,必填: string)
- 响应: `ApiResponseVoid`

### PUT /wms-outbound/{id}/cancel
- 说明: 取消出库单
- 描述: 取消指定出库单并记录操作人，仅未发货的单据允许取消。
- 参数: `id`(path,必填: string) `operatorId`(query,必填: string) `operatorName`(query,必填: string)
- 响应: `ApiResponseVoid`

### POST /wms-outbound
- 说明: 创建出库单
- 描述: 根据仓库与出库类型新建一张出库单并生成出库单号。
- 请求体: `WmsOutbound`
- 响应: `ApiResponseWmsOutbound`

### GET /wms-outbound/page
- 说明: 分页查询出库单
- 描述: 按仓库、出库类型与单据状态分页查询出库单列表。
- 参数: `page`(query: integer) `size`(query: integer) `warehouseId`(query: string) `outboundType`(query: integer) `status`(query: integer)
- 响应: `ApiResponsePageWmsOutbound`

## OutboundItem

### GET /wms-outbound-item/{id}
- 说明: 查询出库明细详情
- 描述: 按主键查询出库明细，明细不存在或已删除时返回 404。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseWmsOutboundItem`

### PUT /wms-outbound-item/{id}
- 说明: 更新出库明细
- 描述: 按主键更新出库明细的数量、库位等信息。
- 参数: `id`(path,必填: string)
- 请求体: `WmsOutboundItem`
- 响应: `ApiResponseWmsOutboundItem`

### DELETE /wms-outbound-item/{id}
- 说明: 删除出库明细
- 描述: 按主键逻辑删除出库明细行。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /wms-outbound-item
- 说明: 创建出库明细
- 描述: 为指定出库单新增一条商品明细行。
- 请求体: `WmsOutboundItem`
- 响应: `ApiResponseWmsOutboundItem`

### GET /wms-outbound-item/list
- 说明: 查询出库单明细列表
- 描述: 根据出库单 ID 查询该出库单下的全部明细行。
- 参数: `outboundId`(query,必填: string)
- 响应: `ApiResponseListWmsOutboundItem`

## Location

### GET /wms-location/{id}
- 说明: 查询库位详情
- 描述: 按主键查询库位，库位不存在或已删除时返回 404。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseWmsLocation`

### PUT /wms-location/{id}
- 说明: 更新库位
- 描述: 按主键更新库位的基础信息与状态。
- 参数: `id`(path,必填: string)
- 请求体: `WmsLocation`
- 响应: `ApiResponseWmsLocation`

### DELETE /wms-location/{id}
- 说明: 删除库位
- 描述: 按主键逻辑删除库位，已删除的库位不再参与上架分配。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /wms-location
- 说明: 创建库位
- 描述: 在指定仓库下新建库位，库位编码需保持唯一。
- 请求体: `WmsLocation`
- 响应: `ApiResponseWmsLocation`

### GET /wms-location/page
- 说明: 分页查询库位
- 描述: 按仓库、库位类型与状态分页查询库位列表。
- 参数: `page`(query: integer) `size`(query: integer) `warehouseId`(query: string) `locationType`(query: integer) `status`(query: integer)
- 响应: `ApiResponsePageWmsLocation`

### GET /wms-location/list
- 说明: 查询仓库库位列表
- 描述: 根据仓库 ID 查询该仓库下的全部库位。
- 参数: `warehouseId`(query,必填: string)
- 响应: `ApiResponseListWmsLocation`

## Inbound

### GET /wms-inbound/{id}
- 说明: 查询入库单详情
- 描述: 按主键查询入库单，单据不存在或已删除时返回 404。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseWmsInbound`

### PUT /wms-inbound/{id}
- 说明: 更新入库单
- 描述: 按主键更新入库单信息，仅允许在未收货状态下修改。
- 参数: `id`(path,必填: string)
- 请求体: `WmsInbound`
- 响应: `ApiResponseWmsInbound`

### DELETE /wms-inbound/{id}
- 说明: 删除入库单
- 描述: 按主键逻辑删除入库单，已删除的单据不再对外可见。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /wms-inbound/{id}/receive
- 说明: 入库收货
- 描述: 对指定入库单执行收货确认，并记录操作人信息。
- 参数: `id`(path,必填: string) `operatorId`(query,必填: string) `operatorName`(query,必填: string)
- 响应: `ApiResponseVoid`

### PUT /wms-inbound/{id}/cancel
- 说明: 取消入库单
- 描述: 取消指定入库单并记录操作人，仅未完成的单据允许取消。
- 参数: `id`(path,必填: string) `operatorId`(query,必填: string) `operatorName`(query,必填: string)
- 响应: `ApiResponseVoid`

### POST /wms-inbound
- 说明: 创建入库单
- 描述: 根据仓库与入库类型新建一张入库单并生成入库单号。
- 请求体: `WmsInbound`
- 响应: `ApiResponseWmsInbound`

### GET /wms-inbound/page
- 说明: 分页查询入库单
- 描述: 按仓库、入库类型与单据状态分页查询入库单列表。
- 参数: `page`(query: integer) `size`(query: integer) `warehouseId`(query: string) `inboundType`(query: integer) `status`(query: integer)
- 响应: `ApiResponsePageWmsInbound`

## InboundItem

### GET /wms-inbound-item/{id}
- 说明: 查询入库明细详情
- 描述: 按主键查询入库明细，明细不存在或已删除时返回 404。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseWmsInboundItem`

### PUT /wms-inbound-item/{id}
- 说明: 更新入库明细
- 描述: 按主键更新入库明细的数量、库位等信息。
- 参数: `id`(path,必填: string)
- 请求体: `WmsInboundItem`
- 响应: `ApiResponseWmsInboundItem`

### DELETE /wms-inbound-item/{id}
- 说明: 删除入库明细
- 描述: 按主键逻辑删除入库明细行。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /wms-inbound-item
- 说明: 创建入库明细
- 描述: 为指定入库单新增一条商品明细行。
- 请求体: `WmsInboundItem`
- 响应: `ApiResponseWmsInboundItem`

### GET /wms-inbound-item/list
- 说明: 查询入库单明细列表
- 描述: 根据入库单 ID 查询该入库单下的全部明细行。
- 参数: `inboundId`(query,必填: string)
- 响应: `ApiResponseListWmsInboundItem`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **WmsWavePicking** — `tenantId`: string, `id`: string, `waveNo`: string, `warehouseId`: string, `outboundIds`: string, `orderCount`: integer, `totalItems`: integer, `pickingPath`: string, `totalDistance`: integer, `optimizationRate`: number, `status`: integer, `pickerId`: string, `pickerName`: string, `startedAt`: string, `completedAt`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `remark`: string
- **ApiResponseWmsWavePicking** — `code`: integer, `message`: string, `data`: WmsWavePicking, `timestamp`: integer
- **WmsWarehouse** — `tenantId`: string, `id`: string, `warehouseCode`: string, `warehouseName`: string, `warehouseType`: integer, `province`: string, `city`: string, `district`: string, `address`: string, `longitude`: number, `latitude`: number, `managerId`: string, `managerName`: string, `phone`: string, `email`: string, `totalCapacity`: integer, `usedCapacity`: integer, `enabled`: boolean, `sortOrder`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseWmsWarehouse** — `code`: integer, `message`: string, `data`: WmsWarehouse, `timestamp`: integer
- **WmsOutbound** — `tenantId`: string, `id`: string, `outboundNo`: string, `warehouseId`: string, `outboundType`: integer, `sourceType`: string, `sourceId`: string, `sourceNo`: string, `priority`: integer, `status`: integer, `totalQuantity`: integer, `pickedQuantity`: integer, `pickingPath`: string, `totalDistance`: integer, `expectedAt`: string, `startedAt`: string, `completedAt`: string, `pickerId`: string, `pickerName`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseWmsOutbound** — `code`: integer, `message`: string, `data`: WmsOutbound, `timestamp`: integer
- **WmsOutboundItem** — `tenantId`: string, `id`: string, `outboundId`: string, `outboundNo`: string, `skuId`: string, `skuCode`: string, `skuName`: string, `locationId`: string, `locationCode`: string, `planQuantity`: integer, `actualQuantity`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `remark`: string, `deleted`: boolean
- **ApiResponseWmsOutboundItem** — `code`: integer, `message`: string, `data`: WmsOutboundItem, `timestamp`: integer
- **WmsLocation** — `tenantId`: string, `id`: string, `warehouseId`: string, `locationCode`: string, `zone`: string, `shelf`: string, `layer`: string, `position`: string, `locationType`: integer, `maxCapacity`: integer, `currentCapacity`: integer, `status`: integer, `enabled`: boolean, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseWmsLocation** — `code`: integer, `message`: string, `data`: WmsLocation, `timestamp`: integer
- **WmsInbound** — `tenantId`: string, `id`: string, `inboundNo`: string, `warehouseId`: string, `inboundType`: integer, `sourceType`: string, `sourceId`: string, `sourceNo`: string, `supplierId`: string, `status`: integer, `totalQuantity`: integer, `receivedQuantity`: integer, `expectedAt`: string, `startedAt`: string, `completedAt`: string, `operatorId`: string, `operatorName`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseWmsInbound** — `code`: integer, `message`: string, `data`: WmsInbound, `timestamp`: integer
- **WmsInboundItem** — `tenantId`: string, `id`: string, `inboundId`: string, `inboundNo`: string, `skuId`: string, `skuCode`: string, `skuName`: string, `locationId`: string, `locationCode`: string, `planQuantity`: integer, `actualQuantity`: integer, `qualityStatus`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `remark`: string, `deleted`: boolean
- **ApiResponseWmsInboundItem** — `code`: integer, `message`: string, `data`: WmsInboundItem, `timestamp`: integer
- **ApiResponsePageWmsWavePicking** — `code`: integer, `message`: string, `data`: PageWmsWavePicking, `timestamp`: integer
- **OrderItem** — `column`: string, `asc`: boolean
- **PageWmsWavePicking** — `records`: WmsWavePicking[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageWmsWavePicking, `searchCount`: PageWmsWavePicking, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponsePageWmsWarehouse** — `code`: integer, `message`: string, `data`: PageWmsWarehouse, `timestamp`: integer
- **PageWmsWarehouse** — `records`: WmsWarehouse[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageWmsWarehouse, `searchCount`: PageWmsWarehouse, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListWmsWarehouse** — `code`: integer, `message`: string, `data`: WmsWarehouse[], `timestamp`: integer
- **ApiResponsePageWmsOutbound** — `code`: integer, `message`: string, `data`: PageWmsOutbound, `timestamp`: integer
- **PageWmsOutbound** — `records`: WmsOutbound[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageWmsOutbound, `searchCount`: PageWmsOutbound, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListWmsOutboundItem** — `code`: integer, `message`: string, `data`: WmsOutboundItem[], `timestamp`: integer
- **ApiResponsePageWmsLocation** — `code`: integer, `message`: string, `data`: PageWmsLocation, `timestamp`: integer
- **PageWmsLocation** — `records`: WmsLocation[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageWmsLocation, `searchCount`: PageWmsLocation, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListWmsLocation** — `code`: integer, `message`: string, `data`: WmsLocation[], `timestamp`: integer
- **ApiResponsePageWmsInbound** — `code`: integer, `message`: string, `data`: PageWmsInbound, `timestamp`: integer
- **PageWmsInbound** — `records`: WmsInbound[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageWmsInbound, `searchCount`: PageWmsInbound, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListWmsInboundItem** — `code`: integer, `message`: string, `data`: WmsInboundItem[], `timestamp`: integer