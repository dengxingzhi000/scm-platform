# scm-logistics — OpenAPI 文档摘要

- 来源: `http://192.168.0.132:8205/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-logistics (1.0.0)
- 认证: Bearer JWT (Authorization header)

## Waybill

### PUT /api/v1/tms-waybill/{id}/status
- 说明: 更新运单状态
- 描述: 按目标状态更新指定运单的状态，可记录操作人。
- 参数: `id`(path,必填: string) `status`(query,必填: integer) `operator`(query: string)
- 响应: `ApiResponseVoid`

### POST /api/v1/tms-waybill
- 说明: 创建运单
- 描述: 根据订单与承运商信息创建运单并返回创建结果。
- 请求体: `TmsWaybill`
- 响应: `ApiResponseTmsWaybill`

### POST /api/v1/tms-waybill/{id}/cancel
- 说明: 取消运单
- 描述: 取消指定运单并记录取消原因与操作人。
- 参数: `id`(path,必填: string) `reason`(query: string) `operator`(query: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/tms-waybill/{id}
- 说明: 查询运单详情
- 描述: 根据运单主键 ID 获取运单详细信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseTmsWaybill`

### DELETE /api/v1/tms-waybill/{id}
- 说明: 删除运单
- 描述: 根据主键 ID 逻辑删除指定运单。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### GET /api/v1/tms-waybill/order/{orderId}
- 说明: 按订单查询运单
- 描述: 根据订单主键 ID 获取关联的全部运单。
- 参数: `orderId`(path,必填: string)
- 响应: `ApiResponseListTmsWaybill`

### GET /api/v1/tms-waybill/order-no/{orderNo}
- 说明: 按订单号查询运单
- 描述: 根据订单号获取关联的全部运单。
- 参数: `orderNo`(path,必填: string)
- 响应: `ApiResponseListTmsWaybill`

### GET /api/v1/tms-waybill/no/{waybillNo}
- 说明: 按运单号查询
- 描述: 根据运单号获取对应的运单信息。
- 参数: `waybillNo`(path,必填: string)
- 响应: `ApiResponseTmsWaybill`

### GET /api/v1/tms-waybill/list
- 说明: 分页查询运单
- 描述: 按运单号、运单状态和承运商分页查询运单列表。
- 参数: `page`(query: integer) `size`(query: integer) `waybillNo`(query: string) `status`(query: integer) `carrierId`(query: string)
- 响应: `ApiResponsePageTmsWaybill`

## TransportRoute

### GET /api/v1/tms-route/{id}
- 说明: 查询路线详情
- 描述: 根据路线主键 ID 获取运输路线详细信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseTmsRoute`

### PUT /api/v1/tms-route/{id}
- 说明: 更新路线
- 描述: 根据主键 ID 更新运输路线信息并返回更新后的数据。
- 参数: `id`(path,必填: string)
- 请求体: `TmsRoute`
- 响应: `ApiResponseTmsRoute`

### DELETE /api/v1/tms-route/{id}
- 说明: 删除路线
- 描述: 根据主键 ID 删除指定运输路线。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /api/v1/tms-route
- 说明: 新增路线
- 描述: 创建新的运输路线并返回保存后的路线信息。
- 请求体: `TmsRoute`
- 响应: `ApiResponseTmsRoute`

### GET /api/v1/tms-route/no/{routeNo}
- 说明: 按路线编号查询
- 描述: 根据路线编号获取对应的运输路线信息。
- 参数: `routeNo`(path,必填: string)
- 响应: `ApiResponseTmsRoute`

### GET /api/v1/tms-route/list
- 说明: 分页查询路线
- 描述: 按骑手、路线状态和配送日期分页查询运输路线列表。
- 参数: `page`(query: integer) `size`(query: integer) `courierId`(query: string) `status`(query: integer) `deliveryDate`(query: string)
- 响应: `ApiResponsePageTmsRoute`

### GET /api/v1/tms-route/courier/{courierId}
- 说明: 查询骑手的路线
- 描述: 获取指定骑手负责的全部运输路线。
- 参数: `courierId`(path,必填: string)
- 响应: `ApiResponseListTmsRoute`

## DeliveryArea

### GET /api/v1/tms-delivery-area/{id}
- 说明: 查询配送区域详情
- 描述: 根据配送区域主键 ID 获取区域详细信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseTmsDeliveryArea`

### PUT /api/v1/tms-delivery-area/{id}
- 说明: 更新配送区域
- 描述: 根据主键 ID 更新配送区域信息并返回更新后的数据。
- 参数: `id`(path,必填: string)
- 请求体: `TmsDeliveryArea`
- 响应: `ApiResponseTmsDeliveryArea`

### DELETE /api/v1/tms-delivery-area/{id}
- 说明: 删除配送区域
- 描述: 根据主键 ID 逻辑删除指定配送区域。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### POST /api/v1/tms-delivery-area
- 说明: 新增配送区域
- 描述: 创建新的配送区域并返回保存后的区域信息。
- 请求体: `TmsDeliveryArea`
- 响应: `ApiResponseTmsDeliveryArea`

### GET /api/v1/tms-delivery-area/list
- 说明: 分页查询配送区域
- 描述: 按承运商、省份和城市分页查询配送区域列表。
- 参数: `page`(query: integer) `size`(query: integer) `carrierId`(query: string) `province`(query: string) `city`(query: string)
- 响应: `ApiResponsePageTmsDeliveryArea`

### GET /api/v1/tms-delivery-area/check-coverage
- 说明: 校验配送覆盖
- 描述: 判断指定承运商是否覆盖给定的省市区地址。
- 参数: `carrierId`(query,必填: string) `province`(query,必填: string) `city`(query,必填: string) `district`(query: string)
- 响应: `ApiResponseBoolean`

### GET /api/v1/tms-delivery-area/carrier/{carrierId}
- 说明: 查询承运商的配送区域
- 描述: 获取指定承运商配置的全部配送区域。
- 参数: `carrierId`(path,必填: string)
- 响应: `ApiResponseListTmsDeliveryArea`

## Carrier

### GET /api/v1/tms-carrier/{id}
- 说明: 查询承运商详情
- 描述: 根据承运商主键 ID 获取承运商完整信息。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseTmsCarrier`

### PUT /api/v1/tms-carrier/{id}
- 说明: 更新承运商
- 描述: 根据主键 ID 更新承运商信息并返回更新后的数据。
- 参数: `id`(path,必填: string)
- 请求体: `TmsCarrier`
- 响应: `ApiResponseTmsCarrier`

### DELETE /api/v1/tms-carrier/{id}
- 说明: 删除承运商
- 描述: 根据主键 ID 逻辑删除指定承运商。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseVoid`

### PUT /api/v1/tms-carrier/{id}/enable
- 说明: 启用或停用承运商
- 描述: 根据传入的启用标识切换指定承运商的启用状态。
- 参数: `id`(path,必填: string) `enabled`(query,必填: boolean)
- 响应: `ApiResponseVoid`

### POST /api/v1/tms-carrier
- 说明: 新增承运商
- 描述: 创建新的承运商档案并返回保存后的承运商信息。
- 请求体: `TmsCarrier`
- 响应: `ApiResponseTmsCarrier`

### GET /api/v1/tms-carrier/list
- 说明: 分页查询承运商
- 描述: 按承运商名称、类型和启用状态分页查询承运商列表。
- 参数: `page`(query: integer) `size`(query: integer) `carrierName`(query: string) `carrierType`(query: integer) `enabled`(query: boolean)
- 响应: `ApiResponsePageTmsCarrier`

### GET /api/v1/tms-carrier/enabled
- 说明: 查询启用的承运商
- 描述: 获取当前所有处于启用状态的承运商列表。
- 响应: `ApiResponseListTmsCarrier`

### GET /api/v1/tms-carrier/code/{carrierCode}
- 说明: 按编码查询承运商
- 描述: 根据承运商编码获取对应的承运商信息。
- 参数: `carrierCode`(path,必填: string)
- 响应: `ApiResponseTmsCarrier`

## Tracking

### POST /api/v1/tms-tracking
- 说明: 新增物流轨迹
- 描述: 上报一条新的运单物流轨迹节点并返回创建结果。
- 请求体: `TmsTracking`
- 响应: `ApiResponseTmsTracking`

### GET /api/v1/tms-tracking/{id}
- 说明: 查询轨迹详情
- 描述: 根据轨迹主键 ID 获取单条物流轨迹记录。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseTmsTracking`

### GET /api/v1/tms-tracking/waybill/{waybillId}
- 说明: 按运单查询轨迹
- 描述: 根据运单主键 ID 获取该运单的全部物流轨迹。
- 参数: `waybillId`(path,必填: string)
- 响应: `ApiResponseListTmsTracking`

### GET /api/v1/tms-tracking/waybill-no/{waybillNo}
- 说明: 按运单号查询轨迹
- 描述: 根据运单号获取该运单的全部物流轨迹。
- 参数: `waybillNo`(path,必填: string)
- 响应: `ApiResponseListTmsTracking`

### GET /api/v1/tms-tracking/list
- 说明: 分页查询轨迹
- 描述: 按运单号和轨迹状态分页查询物流轨迹记录。
- 参数: `page`(query: integer) `size`(query: integer) `waybillNo`(query: string) `trackStatus`(query: string)
- 响应: `ApiResponsePageTmsTracking`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **TmsRoute** — `tenantId`: string, `id`: string, `routeNo`: string, `deliveryDate`: string, `courierId`: string, `courierName`: string, `vehicleNo`: string, `vehicleType`: string, `startLocation`: string, `endLocation`: string, `waypoints`: string, `waybillIds`: string, `waybillCount`: integer, `totalDistance`: number, `estimatedDuration`: integer, `actualDuration`: integer, `optimizationAlgorithm`: string, `optimizationScore`: number, `status`: integer, `startedAt`: string, `completedAt`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `remark`: string
- **ApiResponseTmsRoute** — `code`: integer, `message`: string, `data`: TmsRoute, `timestamp`: integer
- **TmsDeliveryArea** — `tenantId`: string, `id`: string, `areaCode`: string, `areaName`: string, `province`: string, `city`: string, `districts`: string, `carrierId`: string, `standardDuration`: integer, `deliveryType`: string, `baseFreight`: number, `additionalFreight`: number, `enabled`: boolean, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseTmsDeliveryArea** — `code`: integer, `message`: string, `data`: TmsDeliveryArea, `timestamp`: integer
- **TmsCarrier** — `tenantId`: string, `id`: string, `carrierCode`: string, `carrierName`: string, `carrierType`: integer, `contactName`: string, `contactPhone`: string, `contactEmail`: string, `website`: string, `apiUrl`: string, `apiKey`: string, `apiSecret`: string, `serviceArea`: string, `serviceTypes`: string, `baseRate`: number, `enabled`: boolean, `sortOrder`: integer, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseTmsCarrier** — `code`: integer, `message`: string, `data`: TmsCarrier, `timestamp`: integer
- **TmsWaybill** — `tenantId`: string, `id`: string, `waybillNo`: string, `carrierId`: string, `carrierName`: string, `orderId`: string, `orderNo`: string, `senderName`: string, `senderPhone`: string, `senderAddress`: string, `receiverName`: string, `receiverPhone`: string, `receiverAddress`: string, `goodsName`: string, `goodsWeight`: number, `goodsVolume`: number, `goodsValue`: number, `freightAmount`: number, `insuranceAmount`: number, `status`: integer, `estimatedDelivery`: string, `actualDelivery`: string, `courierId`: string, `courierName`: string, `courierPhone`: string, `signType`: integer, `signPerson`: string, `signTime`: string, `signImage`: string, `exceptionType`: string, `exceptionReason`: string, `createTime`: string, `createBy`: string, `updateTime`: string, `updateBy`: string, `deleted`: boolean, `remark`: string
- **ApiResponseTmsWaybill** — `code`: integer, `message`: string, `data`: TmsWaybill, `timestamp`: integer
- **TmsTracking** — `tenantId`: string, `id`: string, `waybillId`: string, `waybillNo`: string, `trackTime`: string, `location`: string, `description`: string, `longitude`: number, `latitude`: number, `trackStatus`: string, `operator`: string, `source`: string, `createTime`: string, `extraData`: string
- **ApiResponseTmsTracking** — `code`: integer, `message`: string, `data`: TmsTracking, `timestamp`: integer
- **ApiResponseListTmsWaybill** — `code`: integer, `message`: string, `data`: TmsWaybill[], `timestamp`: integer
- **ApiResponsePageTmsWaybill** — `code`: integer, `message`: string, `data`: PageTmsWaybill, `timestamp`: integer
- **OrderItem** — `column`: string, `asc`: boolean
- **PageTmsWaybill** — `records`: TmsWaybill[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTmsWaybill, `searchCount`: PageTmsWaybill, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListTmsTracking** — `code`: integer, `message`: string, `data`: TmsTracking[], `timestamp`: integer
- **ApiResponsePageTmsTracking** — `code`: integer, `message`: string, `data`: PageTmsTracking, `timestamp`: integer
- **PageTmsTracking** — `records`: TmsTracking[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTmsTracking, `searchCount`: PageTmsTracking, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponsePageTmsRoute** — `code`: integer, `message`: string, `data`: PageTmsRoute, `timestamp`: integer
- **PageTmsRoute** — `records`: TmsRoute[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTmsRoute, `searchCount`: PageTmsRoute, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListTmsRoute** — `code`: integer, `message`: string, `data`: TmsRoute[], `timestamp`: integer
- **ApiResponsePageTmsDeliveryArea** — `code`: integer, `message`: string, `data`: PageTmsDeliveryArea, `timestamp`: integer
- **PageTmsDeliveryArea** — `records`: TmsDeliveryArea[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTmsDeliveryArea, `searchCount`: PageTmsDeliveryArea, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseBoolean** — `code`: integer, `message`: string, `data`: boolean, `timestamp`: integer
- **ApiResponseListTmsDeliveryArea** — `code`: integer, `message`: string, `data`: TmsDeliveryArea[], `timestamp`: integer
- **ApiResponsePageTmsCarrier** — `code`: integer, `message`: string, `data`: PageTmsCarrier, `timestamp`: integer
- **PageTmsCarrier** — `records`: TmsCarrier[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageTmsCarrier, `searchCount`: PageTmsCarrier, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseListTmsCarrier** — `code`: integer, `message`: string, `data`: TmsCarrier[], `timestamp`: integer