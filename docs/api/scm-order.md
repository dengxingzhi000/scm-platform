# scm-order — OpenAPI 文档摘要

- 来源: `http://192.168.0.125:8203/api/public/v3/api-docs`
- 标题: SCM Platform API - scm-order (1.0.0)
- 认证: Bearer JWT (Authorization header)

## Refund

### PUT /api/v1/refunds/{id}/reject
- 说明: 拒绝退款
- 描述: 拒绝退款申请并记录处理人信息。
- 参数: `id`(path,必填: string)
- 请求体: `RefundRejectRequest`
- 响应: `ApiResponseRefundResponse`

### PUT /api/v1/refunds/{id}/complete
- 说明: 完成退款
- 描述: 标记退款单已完成，用于线下退款成功后的回调。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseRefundResponse`

### PUT /api/v1/refunds/{id}/approve
- 说明: 审核通过退款
- 描述: 审核通过退款申请并记录处理人信息。
- 参数: `id`(path,必填: string)
- 请求体: `RefundApproveRequest`
- 响应: `ApiResponseRefundResponse`

### POST /api/v1/refunds
- 说明: 创建退款单
- 描述: 创建退款单并同时写入退款明细。
- 请求体: `RefundCreateRequest`
- 响应: `ApiResponseRefundResponse`

### POST /api/v1/refunds/query
- 说明: 分页查询退款单
- 描述: 按条件分页查询退款单，支持退款单号、订单、用户、状态及时间区间过滤。
- 请求体: `RefundQueryRequest`
- 响应: `ApiResponsePageRefundResponse`

### GET /api/v1/refunds/{id}
- 说明: 查询退款单
- 描述: 根据退款单 ID 查询退款详情及其明细。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseRefundResponse`

### GET /api/v1/refunds/{id}/items
- 说明: 查询退款明细
- 描述: 根据退款单 ID 查询退款明细列表。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseListRefundItemResponse`

### GET /api/v1/refunds/order/{orderId}
- 说明: 按订单查询退款单
- 描述: 根据订单 ID 查询该订单关联的退款单列表。
- 参数: `orderId`(path,必填: string)
- 响应: `ApiResponseListRefundResponse`

## Payment

### PUT /api/v1/payments/{id}/status
- 说明: 更新支付状态
- 描述: 按状态流转规则更新支付记录状态并记录变更原因。
- 参数: `id`(path,必填: string)
- 请求体: `PaymentStatusUpdateRequest`
- 响应: `ApiResponsePaymentResponse`

### POST /api/v1/payments
- 说明: 创建支付记录
- 描述: 创建新的支付记录。
- 请求体: `PaymentCreateRequest`
- 响应: `ApiResponsePaymentResponse`

### POST /api/v1/payments/query
- 说明: 分页查询支付记录
- 描述: 按条件分页查询支付记录，支持支付单号、订单、用户、状态及时间区间过滤。
- 请求体: `PaymentQueryRequest`
- 响应: `ApiResponsePagePaymentResponse`

### GET /api/v1/payments/{id}
- 说明: 查询支付记录
- 描述: 根据支付记录 ID 查询支付详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponsePaymentResponse`

### GET /api/v1/payments/order/{orderId}
- 说明: 按订单查询支付记录
- 描述: 根据订单 ID 查询该订单关联的全部支付记录。
- 参数: `orderId`(path,必填: string)
- 响应: `ApiResponseListPaymentResponse`

## Order

### PUT /api/v1/orders/{id}/status
- 说明: 订单状态流转
- 描述: 按状态机规则更新订单状态，非法流转将被拒绝。
- 参数: `id`(path,必填: string)
- 请求体: `OrderStatusUpdateRequest`
- 响应: `ApiResponseOrderResponse`

### PUT /api/v1/orders/{id}/cancel
- 说明: 取消订单
- 描述: 取消待支付或已支付的订单并记录取消原因。
- 参数: `id`(path,必填: string)
- 请求体: `OrderCancelRequest`
- 响应: `ApiResponseOrderResponse`

### POST /api/v1/orders
- 说明: 创建订单
- 描述: 创建新订单并同时写入订单明细。
- 请求体: `OrderCreateRequest`
- 响应: `ApiResponseOrderResponse`

### POST /api/v1/orders/query
- 说明: 分页查询订单
- 描述: 按条件分页查询订单，支持订单号、用户、状态、来源及时间区间过滤。
- 请求体: `OrderQueryRequest`
- 响应: `ApiResponsePageOrderResponse`

### GET /api/v1/orders/{id}
- 说明: 查询订单
- 描述: 根据订单 ID 查询订单详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseOrderResponse`

### GET /api/v1/orders/{id}/items
- 说明: 查询订单明细
- 描述: 根据订单 ID 查询该订单下的全部明细项。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseListOrderItemResponse`

## OrderItem

### POST /api/v1/order-items/query
- 说明: 分页查询订单明细
- 描述: 按订单 ID 分页查询该订单的明细项。
- 请求体: `OrdOrderItemQueryRequest`
- 响应: `ApiResponsePageOrderItemResponse`

### GET /api/v1/order-items/{id}
- 说明: 查询订单明细
- 描述: 根据订单明细 ID 查询明细详情。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseOrderItemResponse`

## OrderStatusHistory

### GET /api/v1/status-history/{id}
- 说明: 查询状态历史
- 描述: 根据状态历史 ID 查询单条状态变更记录。
- 参数: `id`(path,必填: string)
- 响应: `ApiResponseStatusHistoryResponse`

### GET /api/v1/status-history/order/{orderId}
- 说明: 按订单查询状态历史
- 描述: 根据订单 ID 查询该订单的全部状态变更记录。
- 参数: `orderId`(path,必填: string)
- 响应: `ApiResponseListStatusHistoryResponse`

## Schemas

- **ApiResponseVoid** — `code`: integer, `message`: string, `data`: any, `timestamp`: integer
- **ApiResponseMapStringString** — `code`: integer, `message`: string, `data`: object, `timestamp`: integer
- **RefundRejectRequest** — `handlerId`: string, `handlerName`: string, `handlerRemark`: string
- **ApiResponseRefundResponse** — `code`: integer, `message`: string, `data`: RefundResponse, `timestamp`: integer
- **RefundItemResponse** — `id`: string, `refundId`: string, `refundNo`: string, `orderId`: string, `orderNo`: string, `orderItemId`: string, `skuId`: string, `skuCode`: string, `skuName`: string, `quantity`: integer, `refundAmount`: number, `remark`: string
- **RefundResponse** — `id`: string, `refundNo`: string, `orderId`: string, `orderNo`: string, `userId`: string, `refundType`: integer, `reason`: string, `description`: string, `refundAmount`: number, `refundStatus`: string, `approvedAt`: string, `rejectedAt`: string, `completedAt`: string, `handlerId`: string, `handlerName`: string, `handlerRemark`: string, `returnWaybillNo`: string, `returnCarrier`: string, `createTime`: string, `updateTime`: string, `remark`: string, `items`: RefundItemResponse[]
- **RefundApproveRequest** — `handlerId`: string, `handlerName`: string, `handlerRemark`: string
- **PaymentStatusUpdateRequest** — `targetStatus`: string, `reason`: string
- **ApiResponsePaymentResponse** — `code`: integer, `message`: string, `data`: PaymentResponse, `timestamp`: integer
- **PaymentResponse** — `id`: string, `paymentNo`: string, `orderId`: string, `orderNo`: string, `userId`: string, `paymentMethod`: integer, `paymentChannel`: string, `paymentAmount`: number, `paymentStatus`: string, `thirdPartyNo`: string, `initiatedAt`: string, `paidAt`: string, `failedAt`: string, `refundedAt`: string, `refundAmount`: number, `refundReason`: string, `createTime`: string, `updateTime`: string, `remark`: string
- **OrderStatusUpdateRequest** — `targetStatus`: string, `reason`: string
- **ApiResponseOrderResponse** — `code`: integer, `message`: string, `data`: OrderResponse, `timestamp`: integer
- **OrderItemResponse** — `id`: string, `skuId`: string, `spuId`: string, `skuCode`: string, `skuName`: string, `spuName`: string, `attributes`: string, `imageUrl`: string, `originalPrice`: number, `sellingPrice`: number, `quantity`: integer, `subtotal`: number, `discountAmount`: number, `finalAmount`: number, `warehouseId`: string, `refundQuantity`: integer, `refundAmount`: number, `remark`: string
- **OrderResponse** — `id`: string, `orderNo`: string, `userId`: string, `username`: string, `orderType`: integer, `orderSource`: string, `status`: string, `statusName`: string, `totalAmount`: number, `discountAmount`: number, `freightAmount`: number, `payableAmount`: number, `paidAmount`: number, `paymentMethod`: integer, `paymentNo`: string, `paidAt`: string, `shippingAddress`: string, `warehouseId`: string, `skuId`: string, `quantity`: integer, `waybillNo`: string, `carrier`: string, `shippedAt`: string, `estimatedArrival`: string, `paymentDeadline`: string, `autoCancelAt`: string, `autoCompleteAt`: string, `completedAt`: string, `cancelledAt`: string, `cancelReason`: string, `reservationId`: string, `buyerMessage`: string, `sellerMessage`: string, `tags`: string, `remark`: string, `createTime`: string, `updateTime`: string, `items`: OrderItemResponse[]
- **OrderCancelRequest** — `reason`: string
- **RefundCreateRequest** — `refundNo`: string, `orderId`: string, `orderNo`: string, `userId`: string, `refundType`: integer, `reason`: string, `description`: string, `refundAmount`: number, `items`: RefundItemRequest[]
- **RefundItemRequest** — `orderItemId`: string, `skuId`: string, `skuCode`: string, `skuName`: string, `quantity`: integer, `refundAmount`: number, `remark`: string
- **RefundQueryRequest** — `refundNo`: string, `orderId`: string, `userId`: string, `status`: string, `startTime`: string, `endTime`: string, `pageNum`: integer, `pageSize`: integer
- **ApiResponsePageRefundResponse** — `code`: integer, `message`: string, `data`: PageRefundResponse, `timestamp`: integer
- **OrderItem** — `column`: string, `asc`: boolean
- **PageRefundResponse** — `records`: RefundResponse[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageRefundResponse, `searchCount`: PageRefundResponse, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **PaymentCreateRequest** — `paymentNo`: string, `orderId`: string, `orderNo`: string, `userId`: string, `paymentMethod`: integer, `paymentChannel`: string, `paymentAmount`: number, `remark`: string
- **PaymentQueryRequest** — `paymentNo`: string, `orderId`: string, `userId`: string, `status`: string, `startTime`: string, `endTime`: string, `pageNum`: integer, `pageSize`: integer
- **ApiResponsePagePaymentResponse** — `code`: integer, `message`: string, `data`: PagePaymentResponse, `timestamp`: integer
- **PagePaymentResponse** — `records`: PaymentResponse[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PagePaymentResponse, `searchCount`: PagePaymentResponse, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **OrderCreateRequest** — `orderNo`: string, `userId`: string, `username`: string, `orderType`: integer, `orderSource`: string, `shippingAddress`: string, `paymentMethod`: integer, `discountAmount`: number, `freightAmount`: number, `buyerMessage`: string, `items`: OrderItemRequest[]
- **OrderItemRequest** — `skuId`: string, `spuId`: string, `skuCode`: string, `skuName`: string, `spuName`: string, `sellingPrice`: number, `quantity`: integer, `warehouseId`: string, `remark`: string
- **OrderQueryRequest** — `orderNo`: string, `userId`: string, `status`: string, `orderSource`: string, `startTime`: string, `endTime`: string, `pageNum`: integer, `pageSize`: integer
- **ApiResponsePageOrderResponse** — `code`: integer, `message`: string, `data`: PageOrderResponse, `timestamp`: integer
- **PageOrderResponse** — `records`: OrderResponse[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageOrderResponse, `searchCount`: PageOrderResponse, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **OrdOrderItemQueryRequest** — `orderId`: string, `pageNum`: integer, `pageSize`: integer
- **ApiResponsePageOrderItemResponse** — `code`: integer, `message`: string, `data`: PageOrderItemResponse, `timestamp`: integer
- **PageOrderItemResponse** — `records`: OrderItemResponse[], `total`: integer, `size`: integer, `current`: integer, `orders`: OrderItem[], `optimizeCountSql`: PageOrderItemResponse, `searchCount`: PageOrderItemResponse, `optimizeJoinOfCountSql`: boolean, `maxLimit`: integer, `countId`: string, `pages`: integer
- **ApiResponseStatusHistoryResponse** — `code`: integer, `message`: string, `data`: StatusHistoryResponse, `timestamp`: integer
- **StatusHistoryResponse** — `id`: string, `orderId`: string, `orderNo`: string, `fromStatus`: integer, `fromStatusName`: string, `toStatus`: string, `toStatusName`: string, `event`: string, `operatorId`: string, `operatorName`: string, `operatorType`: string, `remark`: string, `transitionedAt`: string
- **ApiResponseListStatusHistoryResponse** — `code`: integer, `message`: string, `data`: StatusHistoryResponse[], `timestamp`: integer
- **ApiResponseListRefundItemResponse** — `code`: integer, `message`: string, `data`: RefundItemResponse[], `timestamp`: integer
- **ApiResponseListRefundResponse** — `code`: integer, `message`: string, `data`: RefundResponse[], `timestamp`: integer
- **ApiResponseListPaymentResponse** — `code`: integer, `message`: string, `data`: PaymentResponse[], `timestamp`: integer
- **ApiResponseListOrderItemResponse** — `code`: integer, `message`: string, `data`: OrderItemResponse[], `timestamp`: integer
- **ApiResponseOrderItemResponse** — `code`: integer, `message`: string, `data`: OrderItemResponse, `timestamp`: integer