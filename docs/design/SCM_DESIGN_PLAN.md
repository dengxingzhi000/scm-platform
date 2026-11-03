# 供应链管理系统（SCM）技术设计方案
**对标互联网大厂的企业级供应链平台**

## 📚 对标大厂参考

- **阿里巴巴**: 菜鸟供应链、1688采购平台
- **京东**: 京东物流WMS、JD Supply Chain
- **美团**: 美团配送、快驴进货
- **拼多多**: 多多买菜供应链
- **字节跳动**: 抖音电商供应链

---

## 一、当前系统能力评估

### ✅ 已具备的核心能力

#### 1. 基础设施层
- **微服务架构**: Spring Cloud 2025 + Nacos + Dubbo
- **API网关**: Spring Cloud Gateway（限流、签名、IP控制）
- **服务治理**: Sentinel熔断降级、Resilience4j容错
- **配置中心**: Nacos动态配置

#### 2. 数据访问层
- **读写分离**: 5种负载均衡策略 + 自动故障转移
- **分库分表**: ShardingSphere ready
- **两级缓存**: Caffeine(L1) + Redis(L2)
- **ORM框架**: MyBatis-Plus 3.5.15

#### 3. 安全体系
- **认证授权**: JWT + OAuth2 + WebAuthn
- **权限管理**: RBAC + 数据权限（@DataScope）
- **API安全**: HMAC-SHA256签名 + SQL注入防护
- **审计日志**: 敏感操作记录

#### 4. 消息驱动
- **消息队列**: Kafka + RabbitMQ双引擎
- **可靠投递**: 幂等性保证 + DLQ重试
- **事件溯源**: CloudEvents标准

#### 5. 可观测性
- **链路追踪**: SkyWalking集成
- **指标监控**: Prometheus + Micrometer
- **健康检查**: Actuator端点

### ❌ 供应链系统需要补充的能力

#### 1. 分布式事务（关键缺失 ⚠️）
- ❌ **Seata/TCC/SAGA**: 订单-库存-支付一致性
- ❌ **本地消息表**: 最终一致性保证
- ❌ **幂等框架增强**: 防止重复扣减库存

#### 2. 高并发处理
- ⚠️ **限流降级**: Sentinel已有，需针对秒杀场景加强
- ❌ **流量削峰**: 消息队列异步化（需专门设计）
- ❌ **热点数据**: Redis集群 + 本地缓存二级防护

#### 3. 搜索引擎
- ❌ **Elasticsearch**: 商品搜索、订单检索
- ❌ **分词器**: IK中文分词
- ❌ **搜索优化**: 同义词、拼音搜索

#### 4. 库存系统（核心）
- ❌ **预占库存**: Redis分布式锁
- ❌ **库存扣减**: Lua脚本原子操作
- ❌ **库存快照**: MVCC多版本并发控制
- ❌ **安全库存**: 预警机制

#### 5. 定时调度
- ❌ **XXL-Job**: 分布式任务调度
- ❌ **订单超时**: 延迟消息/时间轮
- ❌ **报表生成**: 定时任务

#### 6. 数据同步
- ❌ **Canal/Debezium**: MySQL binlog同步到ES
- ❌ **数据湖**: Hudi/Iceberg（可选）
- ❌ **实时计算**: Flink（可选）

#### 7. 业务中台
- ❌ **商品中心**: SPU/SKU管理
- ❌ **订单中心**: 订单状态机
- ❌ **库存中心**: WMS仓储管理
- ❌ **物流中心**: TMS运输管理

---

## 二、大厂供应链系统架构对标

### 阿里菜鸟架构（参考）

```
┌─────────────────────────────────────────────────────────────┐
│                        前端应用层                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │供应商门户│  │WMS仓管  │  │TMS运输  │  │数据大屏 │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      API网关层（BFF）                        │
│  ├─ 身份认证  ├─ 限流熔断  ├─ 路由聚合  ├─ 灰度发布        │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      业务中台层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │商品中心  │  │订单中心  │  │库存中心  │  │物流中心  │   │
│  │(SPU/SKU) │  │(状态机)  │  │(扣减锁)  │  │(路由算)  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │采购中心  │  │供应商中心│  │结算中心  │  │报表中心  │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      数据访问层                              │
│  ├─ 读写分离  ├─ 分库分表  ├─ 缓存策略  ├─ 搜索引擎        │
└─────────────────────────────────────────────────────────────┘
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                      基础设施层                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │MySQL集群 │  │Redis集群 │  │ES集群    │  │Kafka集群 │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 京东供应链核心特点

**技术特点:**
1. **异地多活**: 多机房容灾（北京、上海、广州）
2. **弹性伸缩**: K8s自动扩容
3. **智能算法**: 路径规划（TSP算法）、库存预测（LSTM）
4. **实时计算**: Flink流处理订单、库存

**业务特点:**
1. **211限时达**: 精准时效承诺
2. **智能调度**: 基于AI的运力调度
3. **全程可视**: 从下单到签收实时追踪

---

## 三、供应链系统技术完善方案

### 3.1 分布式事务解决方案 ⭐⭐⭐⭐⭐

#### 方案一: Seata AT模式（推荐）

**为什么选择Seata:**
- 阿里开源，经过双11验证
- 对业务代码侵入小，只需加@GlobalTransactional
- 支持MySQL、Oracle、PostgreSQL

**集成步骤:**

```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
    <version>2.0.0</version>
</dependency>
```

```yaml
# application.yaml
seata:
  enabled: true
  application-id: scm-order-service
  tx-service-group: scm-tx-group
  registry:
    type: nacos
    nacos:
      server-addr: ${spring.cloud.nacos.server-addr}
      group: SEATA_GROUP
  config:
    type: nacos
```

```java
// 订单服务
@Service
public class OrderService {

    @Autowired
    private InventoryServiceClient inventoryClient;  // Dubbo RPC

    @Autowired
    private PaymentServiceClient paymentClient;

    @GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
    public Order createOrder(OrderDTO dto) {
        // 1. 创建订单（本地事务）
        Order order = new Order();
        order.setUserId(dto.getUserId());
        order.setAmount(dto.getAmount());
        orderMapper.insert(order);

        // 2. RPC调用库存服务扣减库存（远程事务）
        inventoryClient.deduct(dto.getSkuId(), dto.getQuantity());

        // 3. RPC调用支付服务创建支付单（远程事务）
        paymentClient.createPayment(order.getId(), dto.getAmount());

        // 任何一步失败，全部回滚
        return order;
    }
}
```

**数据库准备（每个库都要）:**
```sql
-- Seata UNDO_LOG表
CREATE TABLE IF NOT EXISTS `undo_log`
(
    `branch_id`     BIGINT       NOT NULL COMMENT 'branch transaction id',
    `xid`           VARCHAR(128) NOT NULL COMMENT 'global transaction id',
    `context`       VARCHAR(128) NOT NULL COMMENT 'undo_log context',
    `rollback_info` LONGBLOB     NOT NULL COMMENT 'rollback info',
    `log_status`    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    `log_created`   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    `log_modified`  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE = InnoDB COMMENT ='AT transaction mode undo table';
```

#### 方案二: 本地消息表（兜底方案）

**适用场景:** 对一致性要求不是特别高，可以接受短暂不一致

```java
@Service
public class OrderService {

    @Transactional
    public void createOrder(OrderDTO dto) {
        // 1. 创建订单
        Order order = new Order();
        orderMapper.insert(order);

        // 2. 写入本地消息表（同一个事务）
        LocalMessage msg = LocalMessage.builder()
            .bizId(order.getId())
            .topic("order.created")
            .payload(JsonUtils.toJson(order))
            .status(MessageStatus.PENDING)
            .maxRetry(3)
            .build();
        messageMapper.insert(msg);

        // 3. 事务提交后，定时任务会扫描消息表发送到MQ
    }
}

// 定时任务扫描并发送
@Component
public class MessageScanJob {

    @Scheduled(fixedDelay = 1000)  // 每秒扫描一次
    public void scanAndSend() {
        List<LocalMessage> messages = messageMapper.selectPending(100);

        for (LocalMessage msg : messages) {
            try {
                // 发送到MQ
                kafkaTemplate.send(msg.getTopic(), msg.getPayload());

                // 标记为已发送
                msg.setStatus(MessageStatus.SENT);
                messageMapper.updateById(msg);

            } catch (Exception e) {
                // 重试次数+1
                msg.setRetryCount(msg.getRetryCount() + 1);
                if (msg.getRetryCount() >= msg.getMaxRetry()) {
                    msg.setStatus(MessageStatus.FAILED);
                }
                messageMapper.updateById(msg);
            }
        }
    }
}
```

### 3.2 库存系统设计（核心竞争力） ⭐⭐⭐⭐⭐

#### 3.2.1 Redis + Lua 原子扣减

```lua
-- deduct_inventory.lua
local key = KEYS[1]                -- 库存key: stock:sku:123
local quantity = tonumber(ARGV[1]) -- 扣减数量

local stock = tonumber(redis.call('GET', key) or '0')

if stock >= quantity then
    redis.call('DECRBY', key, quantity)
    return 1  -- 成功
else
    return 0  -- 库存不足
end
```

```java
@Service
public class InventoryService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private DefaultRedisScript<Long> deductScript;

    public boolean deductStock(Long skuId, Integer quantity) {
        String key = "stock:sku:" + skuId;

        Long result = redisTemplate.execute(
            deductScript,
            Collections.singletonList(key),
            quantity.toString()
        );

        if (result != null && result == 1) {
            // 扣减成功，异步更新数据库
            asyncUpdateDatabase(skuId, quantity);
            return true;
        }

        return false;
    }

    @Async
    public void asyncUpdateDatabase(Long skuId, Integer quantity) {
        inventoryMapper.deduct(skuId, quantity);
    }
}
```

#### 3.2.2 预占库存 + 定时释放

```java
@Service
public class StockReservationService {

    public String reserveStock(Long skuId, Integer quantity, Long orderId) {
        String reservationId = UUIDv7Util.generate();

        // 1. 扣减可用库存
        boolean success = inventoryService.deductStock(skuId, quantity);
        if (!success) {
            throw new StockInsufficientException("库存不足");
        }

        // 2. 记录预占记录（数据库）
        StockReservation reservation = StockReservation.builder()
            .reservationId(reservationId)
            .skuId(skuId)
            .quantity(quantity)
            .orderId(orderId)
            .expireTime(LocalDateTime.now().plusMinutes(15))
            .status(ReservationStatus.RESERVED)
            .build();
        reservationMapper.insert(reservation);

        // 3. Redis设置过期时间（双重保险）
        redisTemplate.opsForValue().set(
            "reservation:" + reservationId,
            quantity.toString(),
            15,
            TimeUnit.MINUTES
        );

        // 4. 发送延迟消息（15分钟后检查是否需要释放）
        RabbitMessage msg = RabbitMessage.builder()
            .body(reservationId)
            .delay(15 * 60 * 1000)  // 15分钟
            .build();
        rabbitTemplate.convertAndSend("stock.release.delayed", msg);

        return reservationId;
    }

    // 监听延迟消息
    @RabbitListener(queues = "stock.release.queue")
    public void handleStockRelease(String reservationId) {
        StockReservation reservation = reservationMapper.selectByReservationId(reservationId);

        // 如果预占记录还是RESERVED状态，说明订单未支付，需要释放
        if (reservation.getStatus() == ReservationStatus.RESERVED) {
            // 回退库存
            inventoryService.increaseStock(reservation.getSkuId(), reservation.getQuantity());

            // 更新预占记录
            reservation.setStatus(ReservationStatus.RELEASED);
            reservationMapper.updateById(reservation);
        }
    }

    // 确认占用（订单支付成功后调用）
    public void confirmReservation(String reservationId) {
        StockReservation reservation = reservationMapper.selectByReservationId(reservationId);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationMapper.updateById(reservation);
    }
}
```

### 3.3 订单状态机（美团方案） ⭐⭐⭐⭐

```xml
<dependency>
    <groupId>org.springframework.statemachine</groupId>
    <artifactId>spring-statemachine-core</artifactId>
    <version>3.2.0</version>
</dependency>
```

```java
// 订单状态枚举
public enum OrderStatus {
    PENDING_PAYMENT,    // 待支付
    PAID,               // 已支付
    PENDING_SHIP,       // 待发货
    SHIPPED,            // 已发货
    IN_TRANSIT,         // 运输中
    DELIVERED,          // 已送达
    COMPLETED,          // 已完成
    CANCELLED           // 已取消
}

// 订单事件枚举
public enum OrderEvent {
    PAYMENT_SUCCESS,      // 支付成功
    WAREHOUSE_ALLOCATED,  // 仓库分配
    SHIP_OUT,             // 发货
    TRANSIT_UPDATE,       // 物流更新
    ARRIVE_DESTINATION,   // 到达目的地
    CONFIRM_RECEIVED,     // 确认收货
    CANCEL                // 取消订单
}

@Configuration
@EnableStateMachine
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderStatus, OrderEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<OrderStatus, OrderEvent> states) throws Exception {
        states
            .withStates()
            .initial(OrderStatus.PENDING_PAYMENT)
            .states(EnumSet.allOf(OrderStatus.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStatus, OrderEvent> transitions) throws Exception {
        transitions
            // 待支付 -> 已支付
            .withExternal()
                .source(OrderStatus.PENDING_PAYMENT)
                .target(OrderStatus.PAID)
                .event(OrderEvent.PAYMENT_SUCCESS)
                .guard(paymentGuard())
                .action(afterPaymentAction())
            .and()
            // 已支付 -> 待发货
            .withExternal()
                .source(OrderStatus.PAID)
                .target(OrderStatus.PENDING_SHIP)
                .event(OrderEvent.WAREHOUSE_ALLOCATED)
            .and()
            // 待发货 -> 已发货
            .withExternal()
                .source(OrderStatus.PENDING_SHIP)
                .target(OrderStatus.SHIPPED)
                .event(OrderEvent.SHIP_OUT)
                .action(notifyLogisticsAction())
            .and()
            // 已发货 -> 运输中
            .withExternal()
                .source(OrderStatus.SHIPPED)
                .target(OrderStatus.IN_TRANSIT)
                .event(OrderEvent.TRANSIT_UPDATE)
            .and()
            // 运输中 -> 已送达
            .withExternal()
                .source(OrderStatus.IN_TRANSIT)
                .target(OrderStatus.DELIVERED)
                .event(OrderEvent.ARRIVE_DESTINATION)
            .and()
            // 已送达 -> 已完成
            .withExternal()
                .source(OrderStatus.DELIVERED)
                .target(OrderStatus.COMPLETED)
                .event(OrderEvent.CONFIRM_RECEIVED);
    }

    @Bean
    public Guard<OrderStatus, OrderEvent> paymentGuard() {
        return context -> {
            // 验证支付是否成功
            Long orderId = (Long) context.getMessage().getHeaders().get("orderId");
            Payment payment = paymentService.getByOrderId(orderId);
            return payment != null && payment.getStatus() == PaymentStatus.SUCCESS;
        };
    }

    @Bean
    public Action<OrderStatus, OrderEvent> afterPaymentAction() {
        return context -> {
            Long orderId = (Long) context.getMessage().getHeaders().get("orderId");

            // 1. 确认库存预占
            Order order = orderMapper.selectById(orderId);
            stockReservationService.confirmReservation(order.getReservationId());

            // 2. 分配仓库
            warehouseService.allocate(orderId);

            // 3. 发送通知
            notificationService.send(order.getUserId(), "支付成功，正在准备发货");
        };
    }

    @Bean
    public Action<OrderStatus, OrderEvent> notifyLogisticsAction() {
        return context -> {
            Long orderId = (Long) context.getMessage().getHeaders().get("orderId");

            // 调用物流服务创建运单
            logisticsService.createWaybill(orderId);
        };
    }
}

// 使用状态机
@Service
public class OrderFSMService {

    @Autowired
    private StateMachine<OrderStatus, OrderEvent> stateMachine;

    public void fireEvent(Long orderId, OrderEvent event) {
        stateMachine.sendEvent(MessageBuilder
            .withPayload(event)
            .setHeader("orderId", orderId)
            .build());
    }
}
```

### 3.4 搜索引擎集成（Elasticsearch） ⭐⭐⭐⭐

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: elastic
```

```java
// 商品搜索文档
@Document(indexName = "products")
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Integer)
    private Integer sales;  // 销量

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime createTime;
}

// Repository
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    // 关键词搜索
    List<ProductDocument> findByNameContaining(String keyword);

    // 价格区间搜索
    List<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // 复杂查询
    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}], \"filter\": [{\"range\": {\"price\": {\"gte\": ?1, \"lte\": ?2}}}]}}")
    List<ProductDocument> searchByNameAndPriceRange(String name, BigDecimal minPrice, BigDecimal maxPrice);
}

// Service
@Service
public class ProductSearchService {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;

    public Page<ProductDocument> search(ProductSearchDTO searchDTO) {
        // 构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 1. 关键词搜索（name和description）
        if (StringUtils.isNotBlank(searchDTO.getKeyword())) {
            boolQuery.should(QueryBuilders.matchQuery("name", searchDTO.getKeyword()).boost(2.0f));
            boolQuery.should(QueryBuilders.matchQuery("description", searchDTO.getKeyword()));
        }

        // 2. 类目过滤
        if (StringUtils.isNotBlank(searchDTO.getCategory())) {
            boolQuery.filter(QueryBuilders.termQuery("category", searchDTO.getCategory()));
        }

        // 3. 价格区间
        if (searchDTO.getMinPrice() != null || searchDTO.getMaxPrice() != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if (searchDTO.getMinPrice() != null) {
                rangeQuery.gte(searchDTO.getMinPrice());
            }
            if (searchDTO.getMaxPrice() != null) {
                rangeQuery.lte(searchDTO.getMaxPrice());
            }
            boolQuery.filter(rangeQuery);
        }

        // 4. 排序
        SortBuilder<?> sortBuilder = switch (searchDTO.getSort()) {
            case "price_asc" -> SortBuilders.fieldSort("price").order(SortOrder.ASC);
            case "price_desc" -> SortBuilders.fieldSort("price").order(SortOrder.DESC);
            case "sales" -> SortBuilders.fieldSort("sales").order(SortOrder.DESC);
            default -> SortBuilders.scoreSort();  // 相关性排序
        };

        // 5. 执行查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(boolQuery)
            .withSorts(sortBuilder)
            .withPageable(PageRequest.of(searchDTO.getPage(), searchDTO.getSize()))
            .build();

        SearchHits<ProductDocument> searchHits = elasticsearchTemplate.search(searchQuery, ProductDocument.class);

        return SearchHitSupport.searchPageFor(searchHits, searchQuery.getPageable());
    }
}
```

**数据同步（Canal）:**

```java
@Component
public class ProductCanalListener extends AbstractCanalClient {

    @Autowired
    private ProductSearchRepository searchRepository;

    @Override
    protected void processEntry(CanalEntry.Entry entry) {
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());

        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
            if (entry.getHeader().getTableName().equals("t_product")) {

                if (rowChange.getEventType() == CanalEntry.EventType.INSERT ||
                    rowChange.getEventType() == CanalEntry.EventType.UPDATE) {

                    // 同步到ES
                    ProductDocument doc = convertToDocument(rowData);
                    searchRepository.save(doc);

                } else if (rowChange.getEventType() == CanalEntry.EventType.DELETE) {

                    // 从ES删除
                    Long id = Long.parseLong(getColumnValue(rowData, "id"));
                    searchRepository.deleteById(id);
                }
            }
        }
    }
}
```

### 3.5 分布式任务调度（XXL-Job） ⭐⭐⭐⭐

```xml
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.0</version>
</dependency>
```

```yaml
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
    executor:
      appname: scm-order-executor
      port: 9999
```

```java
@Component
public class OrderJobHandler {

    // 每5分钟扫描超时未支付订单
    @XxlJob("cancelTimeoutOrderJob")
    public void cancelTimeoutOrder() {
        log.info("开始扫描超时未支付订单...");

        LocalDateTime timeout = LocalDateTime.now().minusMinutes(15);

        List<Order> timeoutOrders = orderMapper.selectList(
            new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, OrderStatus.PENDING_PAYMENT)
                .lt(Order::getCreateTime, timeout)
                .last("LIMIT 1000")  // 分批处理
        );

        for (Order order : timeoutOrders) {
            try {
                // 1. 取消订单
                order.setStatus(OrderStatus.CANCELLED);
                order.setCancelReason("支付超时自动取消");
                orderMapper.updateById(order);

                // 2. 释放库存
                if (StringUtils.isNotBlank(order.getReservationId())) {
                    stockReservationService.releaseReservation(order.getReservationId());
                }

                // 3. 发送通知
                notificationService.send(order.getUserId(), "订单已超时取消");

            } catch (Exception e) {
                log.error("取消订单失败: orderId={}", order.getId(), e);
            }
        }

        log.info("超时订单处理完成，共处理{}笔", timeoutOrders.size());
    }

    // 每天凌晨2点生成销售报表
    @XxlJob("generateSalesReportJob")
    public void generateSalesReport() {
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 聚合昨日销售数据
        SalesReport report = reportService.generateDailySalesReport(yesterday);

        // 保存到数据库
        reportMapper.insert(report);

        // 发送到管理员邮箱
        emailService.sendSalesReport(report);
    }

    // 每小时同步库存到ES
    @XxlJob("syncInventoryToESJob")
    public void syncInventoryToES() {
        // Canal可能有延迟，定时全量同步一次
        List<Inventory> inventories = inventoryMapper.selectList(null);

        for (Inventory inventory : inventories) {
            ProductDocument doc = searchRepository.findById(inventory.getSkuId()).orElse(null);
            if (doc != null) {
                doc.setStock(inventory.getAvailableStock());
                searchRepository.save(doc);
            }
        }
    }
}
```

---

## 四、微服务拆分设计（DDD领域驱动）

### 4.1 服务划分

```
cognora-aura-platform/
├── scm-gateway/              # API网关（已有）
├── scm-auth/                 # 认证授权服务（已有）
├── scm-user/                 # 用户服务（已有）
│
├── scm-product/              # 商品服务
│   ├── api/                  # Dubbo接口
│   └── service/              # 服务实现
│       ├── domain/           # 领域模型（SPU/SKU）
│       ├── application/      # 应用服务
│       └── infrastructure/   # 基础设施
│
├── scm-inventory/            # 库存服务（核心）
│   ├── api/
│   └── service/
│       ├── domain/           # 库存领域（预占、扣减）
│       ├── application/
│       └── infrastructure/
│
├── scm-order/                # 订单服务
│   ├── api/
│   └── service/
│       ├── domain/           # 订单聚合根（状态机）
│       ├── application/
│       └── infrastructure/
│
├── scm-warehouse/            # 仓储服务（WMS）
│   ├── api/
│   └── service/
│       ├── domain/           # 入库、出库、盘点
│       ├── application/
│       └── infrastructure/
│
├── scm-logistics/            # 物流服务（TMS）
│   ├── api/
│   └── service/
│       ├── domain/           # 运单、配送
│       ├── application/
│       └── infrastructure/
│
├── scm-purchase/             # 采购服务
│   ├── api/
│   └── service/
│       ├── domain/           # 采购单、供应商
│       ├── application/
│       └── infrastructure/
│
├── scm-supplier/             # 供应商服务
├── scm-settlement/           # 结算服务
├── scm-report/               # 报表服务
│
└── scm-common/               # 公共模块（从NewNearSync复制）
    ├── common-core/
    ├── common-data/
    ├── common-security/
    └── common-mq/
```

### 4.2 数据库分库策略

```yaml
# ShardingSphere配置示例
spring:
  shardingsphere:
    datasource:
      names: master0,master1,slave0,slave1

    rules:
      sharding:
        tables:
          # 订单表：按用户ID取模分16张表
          t_order:
            actual-data-nodes: master${0..1}.t_order_${0..15}
            database-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: order-db-mod
            table-strategy:
              standard:
                sharding-column: user_id
                sharding-algorithm-name: order-table-mod

          # 库存表：按SKU ID取模分8张表
          t_inventory:
            actual-data-nodes: master${0..1}.t_inventory_${0..7}
            table-strategy:
              standard:
                sharding-column: sku_id
                sharding-algorithm-name: inventory-mod

        sharding-algorithms:
          order-db-mod:
            type: MOD
            props:
              sharding-count: 2

          order-table-mod:
            type: INLINE
            props:
              algorithm-expression: t_order_${user_id % 16}

          inventory-mod:
            type: MOD
            props:
              sharding-count: 8
```

---

## 五、性能优化方案

### 5.1 三级缓存架构

```java
@Service
public class ProductService {

    // L1: JVM本地缓存（Caffeine）
    private LoadingCache<Long, Product> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .recordStats()  // 记录命中率
        .build(this::loadFromRedis);

    // L2: Redis缓存
    @Cacheable(value = "product", key = "#id", unless = "#result == null")
    public Product getFromRedis(Long id) {
        // L3: 数据库
        return productMapper.selectById(id);
    }

    public Product getProduct(Long id) {
        try {
            // 先查L1
            return localCache.get(id);
        } catch (Exception e) {
            // L1未命中，直接查库
            return getFromRedis(id);
        }
    }

    private Product loadFromRedis(Long id) {
        return getFromRedis(id);
    }
}
```

### 5.2 热点数据保护

```java
@Component
public class HotspotProtector {

    // 热点计数器
    private final LoadingCache<String, AtomicLong> hotspotCounter = Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build(key -> new AtomicLong(0));

    // 热点数据本地缓存
    private final LoadingCache<String, Object> hotspotCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build(key -> null);

    public boolean isHotspot(String key) {
        AtomicLong counter = hotspotCounter.get(key);
        return counter.incrementAndGet() > 1000;  // 10秒超过1000次
    }

    @Around("@annotation(Hotspot)")
    public Object protectHotspot(ProceedingJoinPoint pjp) throws Throwable {
        String key = generateKey(pjp);

        // 判断是否为热点
        if (isHotspot(key)) {
            // 热点数据，优先返回本地缓存
            Object cached = hotspotCache.getIfPresent(key);
            if (cached != null) {
                return cached;
            }
        }

        // 正常执行
        Object result = pjp.proceed();

        // 缓存结果
        if (result != null) {
            hotspotCache.put(key, result);
        }

        return result;
    }
}
```

### 5.3 限流降级（秒杀场景）

```java
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    // 限流: 每秒10000个请求
    @SentinelResource(
        value = "seckill",
        blockHandler = "handleBlock",
        fallback = "handleFallback"
    )
    @RateLimiter(name = "seckill", rateLimit = 10000)
    @PostMapping("/order")
    public ApiResponse<SeckillOrder> createSeckillOrder(@RequestBody SeckillDTO dto) {
        return ApiResponse.success(seckillService.createOrder(dto));
    }

    // 限流兜底
    public ApiResponse<SeckillOrder> handleBlock(SeckillDTO dto, BlockException ex) {
        return ApiResponse.error("活动太火爆，请稍后再试");
    }

    // 降级处理（异步化）
    public ApiResponse<SeckillOrder> handleFallback(SeckillDTO dto, Throwable ex) {
        // 写入MQ异步处理
        kafkaTemplate.send("seckill.async", dto);
        return ApiResponse.success("订单已提交，正在排队处理...");
    }
}

// 秒杀服务实现
@Service
public class SeckillService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public SeckillOrder createOrder(SeckillDTO dto) {
        Long userId = dto.getUserId();
        Long skuId = dto.getSkuId();

        // 1. 防重复下单（Redis SETNX）
        String dedupeKey = "seckill:order:" + userId + ":" + skuId;
        Boolean setSuccess = redisTemplate.opsForValue().setIfAbsent(
            dedupeKey,
            "1",
            10,
            TimeUnit.MINUTES
        );
        if (Boolean.FALSE.equals(setSuccess)) {
            throw new BusinessException("您已经参与过该活动");
        }

        // 2. Lua脚本原子扣减库存
        boolean deducted = inventoryService.deductStock(skuId, 1);
        if (!deducted) {
            throw new BusinessException("商品已售罄");
        }

        // 3. 创建订单（异步）
        SeckillOrder order = new SeckillOrder();
        order.setUserId(userId);
        order.setSkuId(skuId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderMapper.insert(order);

        // 4. 发送MQ异步处理后续流程
        kafkaTemplate.send("order.created", order);

        return order;
    }
}
```

---

## 六、监控告警体系

### 6.1 业务指标监控

```java
@Component
public class BusinessMetrics {

    @Autowired
    private MeterRegistry meterRegistry;

    // 订单创建成功率
    public void recordOrderCreate(boolean success) {
        Counter.builder("scm.order.create")
            .tag("status", success ? "success" : "fail")
            .description("订单创建次数")
            .register(meterRegistry)
            .increment();
    }

    // 库存扣减耗时
    public void recordInventoryDeduct(long duration) {
        Timer.builder("scm.inventory.deduct.duration")
            .description("库存扣减耗时")
            .register(meterRegistry)
            .record(duration, TimeUnit.MILLISECONDS);
    }

    // 实时库存水位
    public void recordStockLevel(Long skuId, Integer stock) {
        Gauge.builder("scm.stock.level", stock, Integer::intValue)
            .tag("sku_id", skuId.toString())
            .description("实时库存数量")
            .register(meterRegistry);
    }

    // 秒杀抢购成功率
    public void recordSeckillSuccess(boolean success) {
        Counter.builder("scm.seckill.order")
            .tag("result", success ? "success" : "fail")
            .register(meterRegistry)
            .increment();
    }
}
```

### 6.2 告警规则（Prometheus + AlertManager）

```yaml
# prometheus_alerts.yaml
groups:
  - name: scm-business-alerts
    rules:
      # 订单失败率 > 5%
      - alert: HighOrderFailureRate
        expr: |
          sum(rate(scm_order_create_total{status="fail"}[5m]))
          /
          sum(rate(scm_order_create_total[5m]))
          > 0.05
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "订单失败率过高"
          description: "最近5分钟订单失败率: {{ $value | humanizePercentage }}"

      # 库存不足告警
      - alert: LowStockLevel
        expr: scm_stock_level < 100
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "SKU {{ $labels.sku_id }} 库存不足"
          description: "当前库存: {{ $value }}"

      # 库存扣减耗时 > 500ms
      - alert: SlowInventoryDeduct
        expr: |
          histogram_quantile(0.99,
            sum(rate(scm_inventory_deduct_duration_bucket[5m])) by (le)
          ) > 500
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "库存扣减响应过慢"
          description: "P99耗时: {{ $value }}ms"

      # Seata事务回滚率 > 10%
      - alert: HighTransactionRollbackRate
        expr: |
          sum(rate(seata_transaction_total{status="rollback"}[5m]))
          /
          sum(rate(seata_transaction_total[5m]))
          > 0.1
        for: 3m
        labels:
          severity: critical
        annotations:
          summary: "分布式事务回滚率过高"
```

---

## 七、创建新代码库步骤

### 步骤1: 创建GitHub仓库

```bash
# 1. 创建本地目录
cd D:/ProgramProject
mkdir cognora-aura-platform
cd cognora-aura-platform

# 2. 初始化Git
git init

# 3. 创建README
cat > README.md << 'EOF'
# SCM Platform - 供应链管理系统

基于 CommonPermissionsFramework 构建的企业级供应链管理平台

## 技术栈

- Java 21
- Spring Boot 4.0.0
- Spring Cloud 2025.1.0
- Seata 2.0.0（分布式事务）
- XXL-Job 2.4.0（分布式任务）
- Elasticsearch 8.11.0（搜索引擎）

## 核心能力

- ✅ 分布式事务一致性（Seata）
- ✅ 高并发库存扣减（Redis Lua）
- ✅ 订单状态机（Spring StateMachine）
- ✅ 全文搜索（Elasticsearch + IK分词）
- ✅ 任务调度（XXL-Job）

## 对标大厂

- 阿里菜鸟供应链
- 京东物流WMS
- 美团配送系统
EOF

# 4. 创建.gitignore
cat > .gitignore << 'EOF'
target/
*.class
.idea/
*.iml
*.log
EOF

# 5. 创建GitHub仓库
gh repo create cognora-aura-platform --public --description "Enterprise Supply Chain Management Platform | 企业级供应链管理平台"

# 6. 提交并推送
git add .
git commit -m "feat: 初始化供应链管理平台项目

- 基于CommonPermissionsFramework v1.3.0
- 集成Seata分布式事务
- 集成XXL-Job任务调度
- 集成Elasticsearch搜索引擎
- 微服务拆分：商品、库存、订单、仓储、物流

参考架构: 阿里菜鸟、京东物流、美团配送"

git branch -M main
git remote add origin https://github.com/你的用户名/cognora-aura-platform.git
git push -u origin main
```

### 步骤2: 复制基础框架

```bash
# 从NewNearSync复制公共模块
cp -r ../NewNearSync/common ./scm-common

# 复制网关
cp -r ../NewNearSync/gateway ./scm-gateway

# 复制认证服务
cp -r ../NewNearSync/auth ./scm-auth

# 复制系统服务（改造为用户服务）
cp -r ../NewNearSync/system ./scm-user
```

### 步骤3: 创建父POM

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.scm</groupId>
    <artifactId>cognora-aura-platform</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>SCM Platform</name>
    <description>Supply Chain Management Platform</description>

    <modules>
        <module>scm-common</module>
        <module>scm-gateway</module>
        <module>scm-auth</module>
        <module>scm-user</module>
        <module>scm-product</module>
        <module>scm-inventory</module>
        <module>scm-order</module>
        <module>scm-warehouse</module>
        <module>scm-logistics</module>
    </modules>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.0.0</version>
    </parent>

    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2025.1.0</spring-cloud.version>
        <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>

        <!-- 新增依赖版本 -->
        <seata.version>2.0.0</seata.version>
        <xxl-job.version>2.4.0</xxl-job.version>
        <elasticsearch.version>8.11.0</elasticsearch.version>
        <canal.version>1.1.7</canal.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Spring Cloud -->
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <!-- Seata 分布式事务 -->
            <dependency>
                <groupId>io.seata</groupId>
                <artifactId>seata-spring-boot-starter</artifactId>
                <version>${seata.version}</version>
            </dependency>

            <!-- XXL-Job 任务调度 -->
            <dependency>
                <groupId>com.xuxueli</groupId>
                <artifactId>xxl-job-core</artifactId>
                <version>${xxl-job.version}</version>
            </dependency>

            <!-- Elasticsearch -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
                <version>4.0.0</version>
            </dependency>

            <!-- Canal -->
            <dependency>
                <groupId>com.alibaba.otter</groupId>
                <artifactId>canal.client</artifactId>
                <version>${canal.version}</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### 步骤4: 创建核心服务骨架

```bash
# 使用Maven Archetype创建服务模块
cd cognora-aura-platform

# 创建商品服务
mvn archetype:generate \
    -DgroupId=com.scm \
    -DartifactId=scm-product \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false

# 创建库存服务
mvn archetype:generate \
    -DgroupId=com.scm \
    -DartifactId=scm-inventory \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false

# 创建订单服务
mvn archetype:generate \
    -DgroupId=com.scm \
    -DartifactId=scm-order \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false
```

---

## 八、项目路线图

### Phase 1: 基础设施搭建（1-2周）

**目标:** 完成技术选型和基础框架

- [x] 从CommonPermissionsFramework复制并改造公共模块
- [ ] 集成Seata分布式事务中间件
- [ ] 集成XXL-Job分布式任务调度
- [ ] 集成Elasticsearch搜索引擎
- [ ] 配置ShardingSphere分库分表策略
- [ ] 编写技术选型文档

**交付物:**
- 可运行的微服务骨架
- Seata、XXL-Job、ES集成Demo
- 技术选型评审PPT

### Phase 2: 核心服务开发（3-4周）

**目标:** 完成核心业务能力

**Week 1-2: 商品 + 库存服务**
- [ ] 商品服务（SPU/SKU管理）
- [ ] 库存服务（Redis扣减 + 预占机制）
- [ ] Elasticsearch商品搜索
- [ ] Canal数据同步到ES

**Week 3-4: 订单服务**
- [ ] 订单服务（Spring StateMachine状态机）
- [ ] Seata分布式事务验证（订单-库存-支付）
- [ ] 订单超时取消（XXL-Job）
- [ ] 订单搜索（ES）

**交付物:**
- 核心服务API文档（Swagger）
- 单元测试（覆盖率>80%）
- 压测报告（JMeter）

### Phase 3: 仓储物流服务（2-3周）

**目标:** 完成仓储物流能力

- [ ] 仓储服务（入库、出库、盘点）
- [ ] 物流服务（TMS运输管理、路径规划）
- [ ] 配送服务（骑手调度、轨迹追踪）
- [ ] 电子面单对接（菜鸟、顺丰）

**交付物:**
- WMS/TMS功能演示
- 路径规划算法文档

### Phase 4: 采购结算服务（1-2周）

**目标:** 完成供应链闭环

- [ ] 采购服务（采购单、供应商管理）
- [ ] 供应商协同（EDI对接、API开放）
- [ ] 结算服务（对账、开票）
- [ ] 财务报表

**交付物:**
- 供应商门户
- 对账报表

### Phase 5: 数据平台（2周）

**目标:** 数据驱动决策

- [ ] 实时数据同步（Canal -> Kafka -> Flink）
- [ ] 数据仓库（Hive/Iceberg）
- [ ] BI报表中心（大屏展示）
- [ ] 智能补货算法（销量预测）

**交付物:**
- 数据大屏
- 补货算法模型

### Phase 6: 性能优化 & 上线（1-2周）

**目标:** 生产就绪

- [ ] JMeter压测（订单：1000 TPS，库存：10000 TPS）
- [ ] 性能调优（缓存、限流、降级）
- [ ] 容量评估（服务器、数据库、Redis）
- [ ] 灰度发布验证
- [ ] 监控告警完善

**交付物:**
- 压测报告
- 容量规划文档
- 上线检查清单

---

## 九、差异化亮点（对标大厂）

### 9.1 技术亮点

1. **分布式事务一致性** ⭐⭐⭐⭐⭐
   - Seata AT模式 + 本地消息表兜底
   - 订单-库存-支付三段式事务保证

2. **高并发库存扣减** ⭐⭐⭐⭐⭐
   - Redis Lua脚本原子操作（10000 TPS）
   - 预占机制 + 定时释放
   - 库存快照（MVCC）

3. **订单状态机** ⭐⭐⭐⭐
   - Spring StateMachine + 事件溯源
   - 状态流转可视化

4. **智能路由算法** ⭐⭐⭐⭐
   - 基于距离、成本、时效的多目标优化
   - TSP问题求解（遗传算法）

5. **实时数据同步** ⭐⭐⭐⭐
   - Canal binlog解析 -> Kafka -> Flink
   - 毫秒级数据同步到ES

### 9.2 业务亮点

1. **多仓协同** - 跨仓库智能调拨
2. **供应商协同** - 开放API + EDI对接
3. **智能补货** - 基于LSTM销量预测
4. **全链路追踪** - 从下单到收货可视化
5. **异常检测** - AI识别异常订单

---

## 十、总结

### 当前系统的优势

✅ **基础设施完善**: 微服务、网关、配置中心、服务治理全套齐全
✅ **数据访问成熟**: 读写分离、分库分表、两级缓存已实现
✅ **安全体系健全**: JWT、RBAC、数据权限、审计日志完整
✅ **可观测性强**: SkyWalking、Prometheus、Actuator配套

### 需要补充的关键能力

⚠️ **分布式事务**: Seata是必须集成的（5星重要）
⚠️ **库存系统**: Redis Lua + 预占是核心竞争力
⚠️ **搜索引擎**: Elasticsearch必须有
⚠️ **任务调度**: XXL-Job替代Spring @Scheduled
⚠️ **数据同步**: Canal实现MySQL -> ES同步

### 实施建议

1. **先复制后改造**: 复用CommonPermissionsFramework 90%代码
2. **渐进式增强**: 按Phase 1-6逐步完善，不求一步到位
3. **突出核心**: 重点做好库存扣减和分布式事务
4. **参考大厂**: 多研究阿里、京东的开源项目
5. **持续优化**: 上线后根据压测结果持续调优

---

**文档版本**: v1.0
**最后更新**: 2024-12-24
**作者**: Claude Code AI Assistant