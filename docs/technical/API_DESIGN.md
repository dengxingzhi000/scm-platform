# API 设计文档 (API Design Specification)
# SCM Platform - RESTful API & Dubbo RPC

> **文档类型**: API 接口设计规范
> **技术负责人**: 架构师
> **目标用户**: 后端开发、前端开发、第三方集成
> **版本**: v1.0
> **最后更新**: 2025-12-24

---

## 一、API 设计原则

### 1.1 设计理念

**RESTful 风格**:
- 资源导向: URL 代表资源，HTTP 方法代表操作
- 无状态: 每个请求包含完整信息，服务器不保存上下文
- 幂等性: GET/PUT/DELETE 幂等，POST 非幂等
- HATEOAS: 返回相关资源链接（可选）

**参考标准**:
- [Microsoft REST API Guidelines](https://github.com/microsoft/api-guidelines)
- [Google API Design Guide](https://cloud.google.com/apis/design)
- [Alibaba Java Coding Guidelines](https://alibaba.github.io/Alibaba-Java-Coding-Guidelines/)

### 1.2 命名规范

**URL 命名**:
```
✅ 好的示例:
GET  /api/v1/products                    # 获取商品列表
GET  /api/v1/products/{id}               # 获取商品详情
POST /api/v1/products                    # 创建商品
PUT  /api/v1/products/{id}               # 更新商品
DELETE /api/v1/products/{id}             # 删除商品
GET  /api/v1/products/{id}/inventory     # 获取商品库存

❌ 不好的示例:
GET  /api/v1/getProducts                 # 不要在 URL 中使用动词
POST /api/v1/product/create              # URL 中不要包含操作
GET  /api/v1/Product/{id}                # URL 全小写，不要大写
```

**字段命名**:
- JSON 字段: 小驼峰 `camelCase` (productName, createdAt)
- 数据库字段: 蛇形 `snake_case` (product_name, created_at)
- Java 类: 大驼峰 `PascalCase` (ProductDTO, OrderService)

### 1.3 版本管理

**URL 版本号**:
```
/api/v1/products    # 版本 1
/api/v2/products    # 版本 2
```

**版本策略**:
- v1: 初始版本
- v2: 破坏性变更（字段删除、类型变更）
- v1.1: 向后兼容的增强（新增字段）

**废弃流程**:
1. 提前 3 个月通知（Response Header: `X-API-Deprecated: true`）
2. 同时维护新旧版本
3. 下线旧版本

---

## 二、通用规范

### 2.1 请求格式

**Request Header**:
```http
Content-Type: application/json
Accept: application/json
Authorization: Bearer <JWT_TOKEN>
X-Request-Id: 550e8400-e29b-41d4-a716-446655440000
X-Device-Id: DEVICE_12345
X-Platform: iOS/Android/Web
```

**Request Body** (JSON):
```json
{
  "productName": "iPhone 15 Pro",
  "price": 7999.00,
  "stock": 100,
  "categoryId": 123
}
```

### 2.2 响应格式

**统一响应结构** (`ApiResponse<T>`):
```json
{
  "code": 200,                               // 业务状态码
  "message": "Success",                      // 提示信息
  "data": { ... },                           // 业务数据
  "traceId": "550e8400-e29b-41d4-a716...",  // 链路追踪ID
  "timestamp": "2025-12-24T10:30:00Z"        // 响应时间
}
```

**成功响应**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 123,
    "productName": "iPhone 15 Pro",
    "price": 7999.00,
    "stock": 100
  },
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-12-24T10:30:00Z"
}
```

**失败响应**:
```json
{
  "code": 40001,
  "message": "商品库存不足",
  "data": null,
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2025-12-24T10:30:00Z"
}
```

### 2.3 状态码规范

**HTTP 状态码**:
```
200 OK              - 请求成功
201 Created         - 创建成功
204 No Content      - 删除成功（无返回数据）
400 Bad Request     - 请求参数错误
401 Unauthorized    - 未授权（JWT过期/无效）
403 Forbidden       - 无权限
404 Not Found       - 资源不存在
409 Conflict        - 资源冲突（如重复创建）
429 Too Many Requests - 请求限流
500 Internal Server Error - 服务器错误
503 Service Unavailable - 服务不可用
```

**业务状态码** (code 字段):
```
2xxxx - 成功
  20000 - 成功
  20001 - 创建成功

4xxxx - 客户端错误
  40000 - 参数错误
  40001 - 库存不足
  40002 - 商品不存在
  40003 - 订单不存在
  40101 - JWT 过期
  40102 - JWT 无效
  40301 - 无权限

5xxxx - 服务器错误
  50000 - 内部错误
  50001 - 数据库错误
  50002 - Redis 连接失败
  50003 - 第三方服务调用失败
```

### 2.4 分页规范

**请求参数**:
```
GET /api/v1/products?page=1&size=20&sort=createdAt,desc
```

**响应格式** (`PageResult<T>`):
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "total": 1000,              // 总记录数
    "page": 1,                  // 当前页
    "size": 20,                 // 每页大小
    "pages": 50,                // 总页数
    "records": [                // 数据列表
      { "id": 1, "name": "..." },
      { "id": 2, "name": "..." }
    ]
  }
}
```

---

## 三、核心 API 设计

### 3.1 商品服务 API (scm-product)

#### 3.1.1 创建商品

**接口**: `POST /api/v1/products`

**请求**:
```json
{
  "spuCode": "IPHONE15PRO",
  "productName": "iPhone 15 Pro",
  "categoryId": 123,
  "brandId": 1,
  "description": "Apple iPhone 15 Pro 256GB",
  "images": [
    "https://cdn.example.com/iphone15pro_1.jpg",
    "https://cdn.example.com/iphone15pro_2.jpg"
  ],
  "skus": [
    {
      "skuCode": "IPHONE15PRO-256G-BLACK",
      "attributes": {
        "color": "黑色",
        "storage": "256G"
      },
      "price": 7999.00,
      "stock": 100,
      "weight": 221
    },
    {
      "skuCode": "IPHONE15PRO-256G-WHITE",
      "attributes": {
        "color": "白色",
        "storage": "256G"
      },
      "price": 7999.00,
      "stock": 50,
      "weight": 221
    }
  ]
}
```

**响应**:
```json
{
  "code": 20001,
  "message": "商品创建成功",
  "data": {
    "id": 123456,
    "spuCode": "IPHONE15PRO",
    "productName": "iPhone 15 Pro",
    "createdAt": "2025-12-24T10:30:00Z"
  }
}
```

**错误码**:
- `40000` - 参数错误（必填字段缺失）
- `40901` - 商品代码已存在
- `50000` - 创建失败

#### 3.1.2 搜索商品（Elasticsearch）

**接口**: `GET /api/v1/products/search`

**请求参数**:
```
keyword: 搜索关键词
categoryId: 类目ID
minPrice: 最低价格
maxPrice: 最高价格
sort: 排序（price_asc, price_desc, sales, relevance）
page: 页码
size: 每页大小
```

**示例**:
```
GET /api/v1/products/search?keyword=手机&categoryId=123&minPrice=5000&maxPrice=10000&sort=sales&page=1&size=20
```

**响应**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "total": 150,
    "page": 1,
    "size": 20,
    "records": [
      {
        "id": 123,
        "productName": "iPhone 15 Pro",
        "price": 7999.00,
        "stock": 100,
        "sales": 5000,
        "images": ["https://..."],
        "highlight": "<em>手机</em>"  // 搜索高亮
      }
    ],
    "aggregations": {              // 聚合结果
      "brands": [
        { "name": "Apple", "count": 50 },
        { "name": "Samsung", "count": 40 }
      ],
      "priceRanges": [
        { "range": "0-3000", "count": 30 },
        { "range": "3000-6000", "count": 50 }
      ]
    }
  }
}
```

### 3.2 库存服务 API (scm-inventory)

#### 3.2.1 库存扣减

**接口**: `POST /api/v1/inventory/deduct`

**请求**:
```json
{
  "skuId": 123,
  "quantity": 10,
  "orderId": 456,
  "requestId": "550e8400-e29b-41d4-a716-446655440000"  // 幂等性ID
}
```

**响应**:
```json
{
  "code": 200,
  "message": "库存扣减成功",
  "data": {
    "skuId": 123,
    "beforeStock": 100,
    "afterStock": 90,
    "deductedQuantity": 10
  }
}
```

**错误码**:
- `40001` - 库存不足
- `40002` - SKU不存在
- `40901` - 重复请求（幂等性检查）

#### 3.2.2 预占库存

**接口**: `POST /api/v1/inventory/reserve`

**请求**:
```json
{
  "skuId": 123,
  "quantity": 5,
  "orderId": 789,
  "expireMinutes": 15  // 过期时间（分钟）
}
```

**响应**:
```json
{
  "code": 200,
  "message": "库存预占成功",
  "data": {
    "reservationId": "RSV_550e8400-e29b-41d4-a716-446655440000",
    "expireAt": "2025-12-24T10:45:00Z"
  }
}
```

#### 3.2.3 确认预占

**接口**: `POST /api/v1/inventory/reserve/{reservationId}/confirm`

**响应**:
```json
{
  "code": 200,
  "message": "预占确认成功",
  "data": {
    "reservationId": "RSV_550e8400...",
    "status": "CONFIRMED"
  }
}
```

#### 3.2.4 释放预占

**接口**: `POST /api/v1/inventory/reserve/{reservationId}/release`

**响应**:
```json
{
  "code": 200,
  "message": "预占释放成功",
  "data": {
    "reservationId": "RSV_550e8400...",
    "releasedQuantity": 5
  }
}
```

### 3.3 订单服务 API (scm-order)

#### 3.3.1 创建订单

**接口**: `POST /api/v1/orders`

**请求**:
```json
{
  "userId": 12345,
  "items": [
    {
      "skuId": 123,
      "quantity": 2,
      "price": 7999.00
    },
    {
      "skuId": 124,
      "quantity": 1,
      "price": 299.00
    }
  ],
  "shippingAddress": {
    "receiverName": "张三",
    "phone": "13800138000",
    "province": "北京市",
    "city": "北京市",
    "district": "朝阳区",
    "detail": "三里屯SOHO A座 1001室"
  },
  "paymentMethod": "ALIPAY",
  "remark": "请尽快发货"
}
```

**响应**:
```json
{
  "code": 20001,
  "message": "订单创建成功",
  "data": {
    "orderId": 1234567890,
    "orderNo": "ORD20251224103000001",
    "totalAmount": 16297.00,
    "status": "PENDING_PAYMENT",
    "paymentDeadline": "2025-12-24T10:45:00Z"
  }
}
```

**Seata 分布式事务流程**:
```
1. 订单服务: 创建订单记录 (本地事务)
2. 库存服务: 预占库存 (Dubbo RPC)
3. 支付服务: 创建支付单 (Dubbo RPC)
4. 提交全局事务 / 回滚
```

#### 3.3.2 订单状态流转

**接口**: `POST /api/v1/orders/{orderId}/events`

**请求**:
```json
{
  "event": "PAYMENT_SUCCESS",
  "data": {
    "paymentId": "PAY_123456",
    "paymentTime": "2025-12-24T10:35:00Z"
  }
}
```

**支持的事件**:
```
PAYMENT_SUCCESS       - 支付成功
WAREHOUSE_ALLOCATED   - 仓库分配
SHIP_OUT              - 发货
TRANSIT_UPDATE        - 物流更新
ARRIVE_DESTINATION    - 到达目的地
CONFIRM_RECEIVED      - 确认收货
CANCEL                - 取消订单
```

**响应**:
```json
{
  "code": 200,
  "message": "订单状态更新成功",
  "data": {
    "orderId": 1234567890,
    "previousStatus": "PENDING_PAYMENT",
    "currentStatus": "PAID",
    "eventTime": "2025-12-24T10:35:00Z"
  }
}
```

#### 3.3.3 订单查询

**接口**: `GET /api/v1/orders/{orderId}`

**响应**:
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "orderId": 1234567890,
    "orderNo": "ORD20251224103000001",
    "status": "IN_TRANSIT",
    "userId": 12345,
    "totalAmount": 16297.00,
    "items": [
      {
        "skuId": 123,
        "skuName": "iPhone 15 Pro 256G 黑色",
        "quantity": 2,
        "price": 7999.00,
        "subtotal": 15998.00
      }
    ],
    "shippingAddress": { ... },
    "logistics": {
      "waybillNo": "SF1234567890",
      "carrier": "顺丰速运",
      "status": "IN_TRANSIT",
      "estimatedArrival": "2025-12-25T18:00:00Z",
      "tracks": [
        {
          "time": "2025-12-24T14:00:00Z",
          "location": "北京市朝阳区",
          "description": "快件已发出"
        },
        {
          "time": "2025-12-24T16:00:00Z",
          "location": "北京市顺义区",
          "description": "快件已到达中转站"
        }
      ]
    },
    "timeline": [
      { "time": "2025-12-24T10:30:00Z", "status": "PENDING_PAYMENT", "description": "订单已创建，等待支付" },
      { "time": "2025-12-24T10:35:00Z", "status": "PAID", "description": "支付成功" },
      { "time": "2025-12-24T11:00:00Z", "status": "PENDING_SHIP", "description": "仓库已分配" },
      { "time": "2025-12-24T14:00:00Z", "status": "SHIPPED", "description": "已发货" },
      { "time": "2025-12-24T14:05:00Z", "status": "IN_TRANSIT", "description": "运输中" }
    ]
  }
}
```

### 3.4 仓储服务 API (scm-warehouse)

#### 3.4.1 创建出库单

**接口**: `POST /api/v1/warehouse/outbound`

**请求**:
```json
{
  "orderId": 1234567890,
  "warehouseId": 1,
  "items": [
    {
      "skuId": 123,
      "locationCode": "A01-01-01",  // 库位码
      "quantity": 2
    }
  ],
  "priority": "HIGH"  // 优先级
}
```

**响应**:
```json
{
  "code": 20001,
  "message": "出库单创建成功",
  "data": {
    "outboundId": "OUT_20251224_001",
    "pickingPath": [
      { "locationCode": "A01-01-01", "skuId": 123, "quantity": 2, "distance": 0 },
      { "locationCode": "A01-02-05", "skuId": 124, "quantity": 1, "distance": 15 }
    ],
    "totalDistance": 15,  // 拣货总距离（米）
    "estimatedTime": 5    // 预计拣货时间（分钟）
  }
}
```

#### 3.4.2 波次拣货

**接口**: `POST /api/v1/warehouse/picking/wave`

**请求**:
```json
{
  "warehouseId": 1,
  "orderIds": [123, 124, 125, 126, 127],  // 合并拣货的订单
  "pickerId": 10
}
```

**响应**:
```json
{
  "code": 200,
  "message": "波次拣货任务创建成功",
  "data": {
    "waveId": "WAVE_20251224_001",
    "orderCount": 5,
    "totalItems": 15,
    "pickingPath": [ ... ],
    "optimizationRate": 45  // 路径优化率（%）
  }
}
```

---

## 四、Dubbo RPC 接口

### 4.1 用户服务 Dubbo 接口

**接口定义** (`UserDubboService.java`):
```java
package com.scm.system.api;

public interface UserDubboService {

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息
     */
    UserDTO getUserById(Long userId);

    /**
     * 根据用户名获取用户信息
     * @param username 用户名
     * @return 用户信息
     */
    UserDTO getUserByUsername(String username);

    /**
     * 批量获取用户信息
     * @param userIds 用户ID列表
     * @return 用户信息列表
     */
    List<UserDTO> batchGetUsers(List<Long> userIds);
}
```

### 4.2 库存服务 Dubbo 接口

**接口定义** (`InventoryDubboService.java`):
```java
package com.scm.inventory.api;

public interface InventoryDubboService {

    /**
     * 扣减库存（支持Seata分布式事务）
     * @param skuId SKU ID
     * @param quantity 扣减数量
     * @return 是否成功
     */
    @GlobalTransactional
    boolean deductStock(Long skuId, Integer quantity);

    /**
     * 预占库存
     * @param dto 预占请求
     * @return 预占ID
     */
    String reserveStock(StockReservationDTO dto);

    /**
     * 确认预占
     * @param reservationId 预占ID
     * @return 是否成功
     */
    boolean confirmReservation(String reservationId);

    /**
     * 释放预占
     * @param reservationId 预占ID
     * @return 释放数量
     */
    int releaseReservation(String reservationId);

    /**
     * 批量查询库存
     * @param skuIds SKU ID列表
     * @return 库存Map
     */
    Map<Long, Integer> batchGetStock(List<Long> skuIds);
}
```

---

## 五、性能优化

### 5.1 缓存策略

**三级缓存**:
```
L1: Caffeine 本地缓存（热点数据）
L2: Redis 缓存（常用数据）
L3: 数据库（全量数据）
```

**缓存 Key 设计**:
```
product:{id}                      # 商品详情
inventory:stock:{skuId}           # 库存数量
order:{orderId}                   # 订单详情
user:{userId}                     # 用户信息
```

**缓存过期时间**:
```
商品详情: 1小时
库存数量: 30秒
订单详情: 10分钟
用户信息: 30分钟
```

### 5.2 限流策略

**Sentinel 限流规则**:
```yaml
# 商品搜索: QPS 50000
- resource: /api/v1/products/search
  grade: QPS
  count: 50000
  strategy: DIRECT

# 订单创建: QPS 10000
- resource: /api/v1/orders
  grade: QPS
  count: 10000
  strategy: WARM_UP  # 预热模式
```

### 5.3 批量接口

**批量查询商品**:
```
POST /api/v1/products/batch
Body: { "ids": [1, 2, 3, ..., 100] }
```

**批量查询库存**:
```
POST /api/v1/inventory/batch
Body: { "skuIds": [123, 124, 125, ...] }
```

---

## 六、安全规范

### 6.1 认证授权

**JWT Token 格式**:
```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1c2VySWQiOjEyMzQ1...
```

**Token 解析**:
```json
{
  "userId": 12345,
  "username": "zhangsan",
  "roles": ["USER", "VIP"],
  "permissions": ["order:create", "order:view"],
  "deviceId": "DEVICE_12345",
  "exp": 1735046400
}
```

### 6.2 签名验证

**API 签名**（Gateway 层）:
```
X-Sign: MD5(appKey + timestamp + nonce + body + appSecret)
X-Timestamp: 1703404800000
X-Nonce: 550e8400-e29b-41d4-a716-446655440000
```

### 6.3 敏感数据脱敏

**响应脱敏**:
```json
{
  "phone": "138****8000",           // 手机号
  "idCard": "110101****1234",       // 身份证
  "bankCard": "6222****5678"        // 银行卡
}
```

---

## 七、错误处理

### 7.1 异常分类

**业务异常** (`BusinessException`):
```java
throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT);
```

**系统异常** (`SystemException`):
```java
throw new SystemException("Redis连接失败", e);
```

### 7.2 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResponse.error(50000, "系统繁忙，请稍后重试");
    }
}
```

---

## 八、监控指标

### 8.1 API 监控

**Prometheus 指标**:
```
# API 调用次数
http_requests_total{method="POST", endpoint="/api/v1/orders", status="200"}

# API 响应时间
http_request_duration_seconds{method="POST", endpoint="/api/v1/orders"}

# API 错误率
http_request_errors_total{method="POST", endpoint="/api/v1/orders"}
```

### 8.2 业务监控

```
# 订单创建成功率
scm_order_create_success_rate

# 库存扣减成功率
scm_inventory_deduct_success_rate

# 支付成功率
scm_payment_success_rate
```

---

## 九、附录

### 9.1 完整 API 清单

| 模块 | 方法 | 路径 | 说明 |
|-----|------|------|------|
| **商品** | POST | /api/v1/products | 创建商品 |
| | GET | /api/v1/products/{id} | 获取商品详情 |
| | PUT | /api/v1/products/{id} | 更新商品 |
| | DELETE | /api/v1/products/{id} | 删除商品 |
| | GET | /api/v1/products/search | 搜索商品 |
| **库存** | POST | /api/v1/inventory/deduct | 扣减库存 |
| | POST | /api/v1/inventory/reserve | 预占库存 |
| | POST | /api/v1/inventory/reserve/{id}/confirm | 确认预占 |
| | POST | /api/v1/inventory/reserve/{id}/release | 释放预占 |
| | GET | /api/v1/inventory/{skuId} | 查询库存 |
| **订单** | POST | /api/v1/orders | 创建订单 |
| | GET | /api/v1/orders/{id} | 查询订单 |
| | POST | /api/v1/orders/{id}/events | 订单状态流转 |
| | POST | /api/v1/orders/{id}/cancel | 取消订单 |
| **仓储** | POST | /api/v1/warehouse/outbound | 创建出库单 |
| | POST | /api/v1/warehouse/inbound | 创建入库单 |
| | POST | /api/v1/warehouse/picking/wave | 波次拣货 |
| **物流** | GET | /api/v1/logistics/{waybillNo} | 查询物流轨迹 |
| | POST | /api/v1/logistics/waybill | 创建运单 |

### 9.2 DTO 类设计示例

```java
@Data
@Builder
public class ProductDTO {
    private Long id;
    private String spuCode;
    private String productName;
    private Long categoryId;
    private Long brandId;
    private String description;
    private List<String> images;
    private List<SkuDTO> skus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

@Data
@Builder
public class OrderDTO {
    private Long orderId;
    private String orderNo;
    private Long userId;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private List<OrderItemDTO> items;
    private ShippingAddressDTO shippingAddress;
    private LogisticsDTO logistics;
    private LocalDateTime createdAt;
}
```

---

**文档维护**: 架构组
**审批**: 技术总监
**版本**: v1.0
**最后更新**: 2025-12-24
