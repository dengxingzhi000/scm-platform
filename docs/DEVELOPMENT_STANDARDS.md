# SCM Platform 开发规范

> **版本**: v1.0.0
> **最后更新**: 2025-12-26
> **维护者**: SCM Platform Team

---

## 目录

1. [代码规范](#代码规范)
2. [API 设计规范](#api-设计规范)
3. [数据库规范](#数据库规范)
4. [测试规范](#测试规范)
5. [Git 工作流](#git-工作流)
6. [命名规范](#命名规范)
7. [日志规范](#日志规范)
8. [异常处理](#异常处理)
9. [性能规范](#性能规范)
10. [安全规范](#安全规范)

---

## 1. 代码规范

### 1.1 Java 编码规范

遵循 [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c) 与 Google Java Style Guide。

**强制要求**:
- ✅ 使用 Java 21 特性（Record、Pattern Matching、Virtual Threads）
- ✅ 使用 Lombok 减少样板代码（@Data, @Builder, @Slf4j）
- ✅ 所有公共方法必须有 Javadoc 注释
- ✅ 代码格式化使用 Google Java Format（自动格式化）
- ✅ 使用 Stream API 处理集合（禁止过度使用 for 循环）

**示例**:
```java
/**
 * 订单服务实现类
 *
 * <p>负责订单的创建、查询、状态流转等核心业务逻辑。
 * 使用 Seata 分布式事务保证数据一致性。
 *
 * @author Zhang San
 * @since 2025-12-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderMapper orderMapper;
    private final InventoryClient inventoryClient;

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单 DTO
     * @throws BusinessException 库存不足、商品不存在等业务异常
     */
    @Override
    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());

        // 1. 参数校验
        validateOrderRequest(request);

        // 2. 库存扣减
        inventoryClient.deductStock(request.getItems());

        // 3. 创建订单
        Order order = OrderConverter.toEntity(request);
        orderMapper.insert(order);

        log.info("Order created successfully: {}", order.getOrderNo());
        return OrderConverter.toDTO(order);
    }
}
```

### 1.2 包结构规范

统一使用以下包结构：

```
scm-{module}/
├── api/                          # Dubbo RPC 接口
│   └── src/main/java/
│       └── scm.{module}.api/
│           ├── dto/              # 数据传输对象
│           ├── enums/            # 枚举
│           └── {Module}DubboService.java
│
└── service/                      # 服务实现
    ├── src/main/java/
    │   └── scm.{module}/
    │       ├── {Module}ServiceApplication.java  # 启动类
    │       ├── controller/        # REST 控制器
    │       ├── service/           # 业务逻辑层
    │       │   ├── I{Entity}Service.java
    │       │   └── impl/
    │       │       └── {Entity}ServiceImpl.java
    │       ├── mapper/            # MyBatis Mapper
    │       ├── domain/
    │       │   ├── entity/        # 数据库实体
    │       │   ├── vo/            # 视图对象
    │       │   └── dto/           # 数据传输对象
    │       ├── config/            # 配置类
    │       ├── util/              # 工具类
    │       └── exception/         # 自定义异常
    │
    └── src/main/resources/
        ├── application.yml
        ├── application-dev.yml
        ├── application-prod.yml
        └── mapper/                # MyBatis XML
```

### 1.3 依赖管理

- ✅ 所有依赖版本在父 POM 中统一管理
- ✅ 禁止使用 SNAPSHOT 版本依赖（生产环境）
- ✅ 定期检查依赖安全漏洞（OWASP Dependency Check）
- ✅ 禁止引入功能重复的依赖（如同时引入 Gson 和 Jackson）

---

## 2. API 设计规范

### 2.1 RESTful API 设计

遵循 REST 最佳实践：

**URL 设计**:
```
GET    /api/v1/orders              # 查询订单列表
GET    /api/v1/orders/{id}         # 查询单个订单
POST   /api/v1/orders              # 创建订单
PUT    /api/v1/orders/{id}         # 完整更新订单
PATCH  /api/v1/orders/{id}         # 部分更新订单
DELETE /api/v1/orders/{id}         # 删除订单

# 嵌套资源
GET    /api/v1/orders/{id}/items   # 查询订单明细
POST   /api/v1/orders/{id}/cancel  # 取消订单（操作）
```

**HTTP 状态码**:
- `200 OK` - 成功
- `201 Created` - 创建成功
- `204 No Content` - 删除成功
- `400 Bad Request` - 参数错误
- `401 Unauthorized` - 未认证
- `403 Forbidden` - 无权限
- `404 Not Found` - 资源不存在
- `409 Conflict` - 资源冲突
- `500 Internal Server Error` - 服务器错误

**统一响应格式**:
```java
@Data
@Builder
public class ApiResponse<T> {
    private Integer code;        // 业务状态码
    private String message;      // 消息
    private T data;              // 数据
    private String traceId;      // 链路追踪ID
    private Long timestamp;      // 时间戳

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .code(200)
            .message("Success")
            .data(data)
            .traceId(TraceContext.getTraceId())
            .timestamp(System.currentTimeMillis())
            .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .code(500)
            .message(message)
            .traceId(TraceContext.getTraceId())
            .timestamp(System.currentTimeMillis())
            .build();
    }
}
```

### 2.2 Swagger / OpenAPI 规范

所有 API 必须使用 Swagger v3 注解：

```java
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单管理", description = "订单相关接口")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService orderService;

    @PostMapping
    @Operation(summary = "创建订单", description = "用户下单接口")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    public ApiResponse<OrderDTO> createOrder(
        @RequestBody @Valid CreateOrderRequest request
    ) {
        OrderDTO order = orderService.createOrder(request);
        return ApiResponse.success(order);
    }
}
```

### 2.3 分页查询

统一使用 PageResult：

```java
@Data
public class PageResult<T> {
    private Long total;          // 总记录数
    private Integer page;        // 当前页码
    private Integer size;        // 每页大小
    private Integer pages;       // 总页数
    private List<T> records;     // 数据列表
}

// 使用 MyBatis-Plus 分页
Page<Order> page = new Page<>(pageNum, pageSize);
IPage<Order> result = orderMapper.selectPage(page, queryWrapper);
return PageResult.of(result);
```

---

## 3. 数据库规范

### 3.1 表设计规范

**命名规范**:
- 表名：`{module}_{entity}` (全小写，下划线分隔)
- 字段名：`column_name` (全小写，下划线分隔)
- 主键：统一使用 `id` (UUID v7)
- 索引：`idx_{table}_{column}` (普通索引), `uk_{table}_{column}` (唯一索引)

**示例**:
```sql
CREATE TABLE ord_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_no VARCHAR(32) NOT NULL,
    user_id UUID NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING_PAYMENT',
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    update_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted BOOLEAN DEFAULT FALSE,

    CONSTRAINT uk_order_no UNIQUE (order_no)
);

CREATE INDEX idx_order_user_id ON ord_order(user_id);
CREATE INDEX idx_order_status ON ord_order(status) WHERE deleted = FALSE;
```

### 3.2 字段规范

**必需字段**:
- `id` - 主键（UUID）
- `create_time` - 创建时间（TIMESTAMPTZ）
- `update_time` - 更新时间（TIMESTAMPTZ）
- `deleted` - 逻辑删除标记（BOOLEAN，默认 FALSE）

**可选字段**:
- `create_by` - 创建人（UUID）
- `update_by` - 更新人（UUID）
- `tenant_id` - 租户ID（多租户场景）
- `remark` - 备注（TEXT）

### 3.3 索引规范

- ✅ 所有外键字段必须加索引
- ✅ 高频查询字段添加组合索引
- ✅ 使用部分索引过滤 deleted = FALSE
- ✅ 避免过多索引（单表不超过 5 个）

---

## 4. 测试规范

### 4.1 单元测试

**要求**:
- ✅ 核心业务逻辑覆盖率 > 80%
- ✅ 使用 JUnit 5 + Mockito
- ✅ 测试类命名：`{Class}Test`
- ✅ 测试方法命名：`should{ExpectedBehavior}_when{StateUnderTest}`

**示例**:
```java
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void shouldCreateOrder_whenStockIsAvailable() {
        // Given
        CreateOrderRequest request = CreateOrderRequest.builder()
            .userId(UUID.randomUUID())
            .items(List.of(createOrderItem()))
            .build();
        when(inventoryClient.checkStock(any())).thenReturn(true);

        // When
        OrderDTO result = orderService.createOrder(request);

        // Then
        assertNotNull(result);
        verify(orderMapper).insert(any(Order.class));
    }

    @Test
    void shouldThrowException_whenStockIsInsufficient() {
        // Given
        when(inventoryClient.checkStock(any())).thenReturn(false);

        // When & Then
        assertThrows(BusinessException.class,
            () -> orderService.createOrder(request));
    }
}
```

### 4.2 集成测试

使用 Testcontainers 进行集成测试：

```java
@SpringBootTest
@Testcontainers
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test_db");

    @Autowired
    private IOrderService orderService;

    @Test
    void shouldCreateAndQueryOrder() {
        // 测试完整流程
        OrderDTO created = orderService.createOrder(request);
        OrderDTO queried = orderService.getById(created.getId());

        assertEquals(created.getOrderNo(), queried.getOrderNo());
    }
}
```

### 4.3 性能测试

使用 JMH 进行基准测试：

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class InventoryBenchmark {

    @Benchmark
    public void testRedisDeduction() {
        inventoryService.deductStock(skuId, 1);
    }
}
```

---

## 5. Git 工作流

### 5.1 分支策略

采用 **Git Flow** 模型：

```
master       # 生产环境分支（只读）
  ↑
develop      # 开发分支
  ↑
feature/*    # 功能分支 (feature/order-service)
hotfix/*     # 紧急修复 (hotfix/fix-inventory-bug)
release/*    # 发布分支 (release/v1.0.0)
```

### 5.2 Commit 规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type**:
- `feat` - 新功能
- `fix` - Bug 修复
- `docs` - 文档更新
- `style` - 代码格式（不影响功能）
- `refactor` - 重构
- `perf` - 性能优化
- `test` - 测试
- `chore` - 构建/工具变更

**示例**:
```
feat(order): add distributed transaction support for order creation

- Integrate Seata AT mode
- Add @GlobalTransactional annotation
- Implement inventory rollback on failure

Closes #123
```

### 5.3 Pull Request 规范

- ✅ PR 标题：`[类型] 简短描述`
- ✅ PR 描述：包含背景、改动、测试、截图
- ✅ 至少 1 人 Code Review 通过
- ✅ CI 全部通过（构建、测试、代码质量）
- ✅ 无冲突

---

## 6. 命名规范

### 6.1 类命名

| 类型 | 命名规则 | 示例 |
|-----|---------|------|
| Controller | `{Entity}Controller` | `OrderController` |
| Service 接口 | `I{Entity}Service` | `IOrderService` |
| Service 实现 | `{Entity}ServiceImpl` | `OrderServiceImpl` |
| Mapper | `{Entity}Mapper` | `OrderMapper` |
| Entity | `{Entity}` | `Order` |
| DTO | `{Entity}DTO` | `OrderDTO` |
| VO | `{Entity}VO` | `OrderVO` |
| Request | `{Action}{Entity}Request` | `CreateOrderRequest` |
| Response | `{Action}{Entity}Response` | `CreateOrderResponse` |

### 6.2 方法命名

| 操作 | 命名规则 | 示例 |
|-----|---------|------|
| 查询单个 | `getBy{Condition}` | `getById`, `getByOrderNo` |
| 查询列表 | `listBy{Condition}` | `listByUserId` |
| 分页查询 | `pageBy{Condition}` | `pageByStatus` |
| 创建 | `create` | `createOrder` |
| 更新 | `update` | `updateOrder` |
| 删除 | `delete` | `deleteById` |
| 统计 | `count{Entity}` | `countOrders` |
| 存在判断 | `exists{Entity}` | `existsOrderNo` |

---

## 7. 日志规范

### 7.1 日志级别

- `ERROR` - 系统错误，需要立即处理
- `WARN` - 警告信息，需要关注
- `INFO` - 重要业务流程（创建订单、扣减库存）
- `DEBUG` - 调试信息（本地开发）
- `TRACE` - 详细追踪（性能分析）

### 7.2 日志格式

```java
// ✅ 推荐
log.info("Creating order for user: {}, items: {}", userId, items.size());
log.error("Failed to deduct inventory for SKU: {}", skuId, exception);

// ❌ 禁止
log.info("Creating order for user: " + userId); // 字符串拼接
log.error(exception.getMessage()); // 丢失堆栈
```

### 7.3 敏感数据脱敏

```java
log.info("User login: {}, phone: {}", username, maskPhone(phone));
// 输出: User login: zhangsan, phone: 138****8000
```

---

## 8. 异常处理

### 8.1 异常分类

```java
// 业务异常（可预期）
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}

// 系统异常（不可预期）
public class SystemException extends RuntimeException {
}

// 第三方服务异常
public class ThirdPartyException extends RuntimeException {
}
```

### 8.2 全局异常处理

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<?> handleException(Exception e) {
        log.error("Unexpected exception", e);
        return ApiResponse.error("系统异常，请联系管理员");
    }
}
```

---

## 9. 性能规范

### 9.1 数据库查询

- ✅ 避免 `SELECT *`，只查询需要的字段
- ✅ 使用批量操作（`batchInsert`, `batchUpdate`）
- ✅ 分页查询必须有上限（单次最多 1000 条）
- ✅ 避免 N+1 查询（使用 JOIN 或批量查询）

### 9.2 缓存策略

- ✅ 热点数据缓存（商品详情、用户信息）
- ✅ 设置合理的过期时间（避免缓存雪崩）
- ✅ 使用缓存穿透保护（BloomFilter）
- ✅ 缓存更新采用 Cache Aside 模式

### 9.3 并发控制

- ✅ 使用分布式锁（Redis）控制并发
- ✅ 使用乐观锁（version 字段）更新数据
- ✅ 库存扣减使用 Lua 脚本保证原子性

---

## 10. 安全规范

### 10.1 认证与授权

- ✅ 所有 API 必须经过认证（除公开接口）
- ✅ 使用 JWT Token 进行身份验证
- ✅ Token 有效期：Access Token 2小时，Refresh Token 7天
- ✅ 使用 RBAC 模型进行权限控制

### 10.2 数据安全

- ✅ 敏感数据加密存储（密码使用 Argon2）
- ✅ 传输层使用 HTTPS（生产环境）
- ✅ SQL 注入防护（使用参数化查询）
- ✅ XSS 防护（输入验证 + 输出转义）

### 10.3 接口安全

- ✅ 接口限流（网关层 + 服务层）
- ✅ 请求签名验证（防止重放攻击）
- ✅ IP 白名单（敏感操作）
- ✅ 审计日志（记录所有敏感操作）

---

## 附录

### A. IDE 配置

**推荐插件**:
- Lombok
- CheckStyle
- SonarLint
- GitToolBox
- Rainbow Brackets

**代码格式化**:
导入 `config/google-java-format.xml` 到 IDE。

### B. 开发工具

- IDE: IntelliJ IDEA 2024+
- JDK: OpenJDK 21 (Temurin)
- Maven: 3.8+
- Docker: 24+
- Git: 2.40+

### C. 参考资料

- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
- [Spring Boot Best Practices](https://spring.io/guides/gs/rest-service/)
- [RESTful API Design Guidelines](https://restfulapi.net/)
- [Conventional Commits](https://www.conventionalcommits.org/)

---

**文档维护**: 所有团队成员有责任更新和完善本文档。如有疑问，请联系架构师。