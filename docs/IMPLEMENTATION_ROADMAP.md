# SCM Platform 详细实施路线图

## 项目概览

**目标**: 构建对标阿里菜鸟、京东物流的企业级供应链管理平台
**技术栈**: Java 21 + Spring Cloud 2025 + Seata + Elasticsearch + XXL-Job
**总工期**: 12 周（3 个月）
**团队规模**: 建议 3-5 人（1 架构师 + 2-4 开发）

---

## Phase 0: 基础设施准备（第 1 周）

### 🎯 阶段目标
搭建完整的开发、测试环境，确保所有中间件就绪，CI/CD 流程可用。

### 📋 具体实施任务

#### Task 0.1: 中间件部署（2 天）

**实施内容**:
```yaml
# docker-compose.yml 需要部署的服务
services:
  - Nacos 2.3.0 (服务注册与配置中心)
  - PostgreSQL 16 (主数据库)
  - Redis 7.2 (缓存与分布式锁)
  - Kafka 3.6 (消息队列)
  - RabbitMQ 3.12 (备用消息队列)
  - Elasticsearch 8.11.4 (搜索引擎)
  - Kibana 8.11.4 (ES 可视化)
  - Seata Server 2.2.0 (分布式事务协调器)
  - XXL-Job Admin 2.4.3 (任务调度中心)
  - Sentinel Dashboard 1.8.7 (流控面板)
  - Prometheus + Grafana (监控)
  - Zipkin/SkyWalking (链路追踪)
```

**验收标准**:
- ✅ 所有服务 `docker ps` 状态为 healthy
- ✅ Nacos Console 可访问 (http://localhost:8848/nacos)
- ✅ PostgreSQL 可连接，创建数据库 `scm_platform`
- ✅ Redis 可连接，支持 Lua 脚本
- ✅ Elasticsearch 集群健康状态为 green
- ✅ Seata Server 启动成功，日志无报错
- ✅ XXL-Job Admin 可登录 (admin/123456)
- ✅ Prometheus 可抓取 metrics

**交付物**:
- `docker-compose-infra.yml` (完整中间件配置)
- `scripts/init-db.sql` (数据库初始化脚本)
- `docs/infrastructure-setup.md` (部署文档)

---

#### Task 0.2: 数据库设计（2 天）

**实施内容**:
设计所有业务表结构，包括：

**商品服务 (scm_product)**:
```sql
-- 商品表
CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    product_name VARCHAR(200) NOT NULL,
    category_id UUID,
    brand_id UUID,
    price DECIMAL(10,2) NOT NULL,
    stock_quantity INT DEFAULT 0,
    status SMALLINT DEFAULT 1, -- 1:在售 2:下架 3:售罄
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 商品 SKU 表
CREATE TABLE product_sku (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    product_id UUID NOT NULL,
    sku_code VARCHAR(50) UNIQUE NOT NULL,
    attributes JSONB, -- {"颜色":"红色","尺寸":"XL"}
    price DECIMAL(10,2),
    stock_quantity INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 商品分类表
CREATE TABLE product_category (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    category_name VARCHAR(100) NOT NULL,
    parent_id UUID,
    level SMALLINT,
    sort_order INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**库存服务 (scm_inventory)**:
```sql
-- 库存主表
CREATE TABLE inventory (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    available_quantity INT DEFAULT 0, -- 可用库存
    locked_quantity INT DEFAULT 0,    -- 锁定库存
    total_quantity INT DEFAULT 0,     -- 总库存
    version INT DEFAULT 0,            -- 乐观锁版本号
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(sku_id, warehouse_id)
);

-- 库存预占记录表
CREATE TABLE inventory_reservation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    sku_id UUID NOT NULL,
    order_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    reserved_quantity INT NOT NULL,
    status SMALLINT DEFAULT 1, -- 1:预占中 2:已确认 3:已释放
    expire_time TIMESTAMP,     -- 预占过期时间
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 库存流水表（用于审计）
CREATE TABLE inventory_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    sku_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    change_type VARCHAR(20), -- INBOUND/OUTBOUND/RESERVE/RELEASE
    quantity INT NOT NULL,
    before_quantity INT,
    after_quantity INT,
    order_id UUID,
    operator_id UUID,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**订单服务 (scm_order)**:
```sql
-- 订单主表
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    order_no VARCHAR(32) UNIQUE NOT NULL,
    user_id UUID NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING_PAYMENT',
    -- 状态流转: PENDING_PAYMENT → PAID → PENDING_SHIP → SHIPPED → COMPLETED
    payment_method VARCHAR(20),
    shipping_address JSONB,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    payment_time TIMESTAMP,
    ship_time TIMESTAMP,
    complete_time TIMESTAMP,
    cancel_time TIMESTAMP,
    version INT DEFAULT 0
);

-- 订单明细表
CREATE TABLE order_item (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    order_id UUID NOT NULL,
    sku_id UUID NOT NULL,
    product_name VARCHAR(200),
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单状态变更日志
CREATE TABLE order_state_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    order_id UUID NOT NULL,
    from_state VARCHAR(20),
    to_state VARCHAR(20),
    event VARCHAR(50),
    operator_id UUID,
    remark TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**仓库服务 (scm_warehouse)**:
```sql
-- 仓库表
CREATE TABLE warehouse (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    warehouse_code VARCHAR(50) UNIQUE NOT NULL,
    warehouse_name VARCHAR(100) NOT NULL,
    address JSONB,
    capacity INT,
    status SMALLINT DEFAULT 1, -- 1:启用 2:禁用
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 入库单
CREATE TABLE inbound_order (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    inbound_no VARCHAR(32) UNIQUE NOT NULL,
    warehouse_id UUID NOT NULL,
    supplier_id UUID,
    status SMALLINT DEFAULT 1, -- 1:待入库 2:部分入库 3:已完成
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 出库单
CREATE TABLE outbound_order (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    outbound_no VARCHAR(32) UNIQUE NOT NULL,
    warehouse_id UUID NOT NULL,
    order_id UUID,
    status SMALLINT DEFAULT 1, -- 1:待出库 2:拣货中 3:已出库
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**物流服务 (scm_logistics)**:
```sql
-- 物流单表
CREATE TABLE logistics_order (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    tracking_no VARCHAR(50) UNIQUE NOT NULL,
    order_id UUID NOT NULL,
    carrier_code VARCHAR(20), -- 物流公司代码
    sender_info JSONB,
    receiver_info JSONB,
    status VARCHAR(20) DEFAULT 'PENDING',
    -- PENDING → PICKED_UP → IN_TRANSIT → OUT_FOR_DELIVERY → DELIVERED
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 物流轨迹表
CREATE TABLE logistics_track (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    tracking_no VARCHAR(50) NOT NULL,
    status VARCHAR(20),
    location VARCHAR(200),
    description TEXT,
    track_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**供应商服务 (scm_supplier)**:
```sql
-- 供应商表
CREATE TABLE supplier (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    supplier_code VARCHAR(50) UNIQUE NOT NULL,
    supplier_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    address JSONB,
    rating DECIMAL(3,2), -- 供应商评分 0-5
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 采购订单
CREATE TABLE purchase_order (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    po_no VARCHAR(32) UNIQUE NOT NULL,
    supplier_id UUID NOT NULL,
    total_amount DECIMAL(10,2),
    status SMALLINT DEFAULT 1, -- 1:待确认 2:已确认 3:部分到货 4:已完成
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**验收标准**:
- ✅ 所有表创建成功，包含索引和外键约束
- ✅ 主键使用 UUIDv7（时间有序）
- ✅ 包含软删除字段 `deleted`
- ✅ 包含审计字段 `create_time`, `update_time`, `create_by`, `update_by`
- ✅ 关键表包含乐观锁 `version` 字段
- ✅ 分库分表策略定义（订单、库存按 sku_id 分片）
- ✅ ER 图绘制完成

**交付物**:
- `scripts/db/schema/*.sql` (各服务建表脚本)
- `docs/database-design.md` (数据库设计文档)
- `docs/database-er-diagram.png` (ER 图)

---

#### Task 0.3: CI/CD 流程配置（1 天）

**实施内容**:
```yaml
# .github/workflows/ci.yml
name: CI Pipeline

on:
  push:
    branches: [ master, develop ]
  pull_request:
    branches: [ master ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Maven packages
        uses: actions/cache@v3
        with:
          path: ~/.m2
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

      - name: Build with Maven
        run: mvn clean install -DskipTests

      - name: Run Tests
        run: mvn test

      - name: Generate Test Report
        run: mvn jacoco:report

      - name: Upload Coverage
        uses: codecov/codecov-action@v3

      - name: Build Docker Images
        run: |
          docker build -t scm-gateway:latest ./scm-gateway
          docker build -t scm-auth:latest ./scm-auth
```

**验收标准**:
- ✅ GitHub Actions 配置成功
- ✅ 代码提交自动触发构建
- ✅ 单元测试覆盖率 > 60%
- ✅ Docker 镜像构建成功
- ✅ 代码质量扫描（SonarQube）通过

**交付物**:
- `.github/workflows/ci.yml`
- `.github/workflows/cd.yml`
- `Dockerfile` (各服务)
- `docs/cicd-guide.md`

---

#### Task 0.4: 开发规范与文档（1 天）

**实施内容**:
1. 创建 `CONTRIBUTING.md` - 贡献指南
2. 创建 `CODE_OF_CONDUCT.md` - 行为准则
3. 创建 `docs/api-design-guidelines.md` - API 设计规范
4. 创建 `docs/git-workflow.md` - Git 工作流
5. 配置代码格式化工具（Checkstyle + Spotless）

**验收标准**:
- ✅ 所有文档已创建并推送到仓库
- ✅ Checkstyle 规则配置完成
- ✅ 代码格式化配置生效（`mvn spotless:apply`）
- ✅ API 响应格式统一（`ApiResponse<T>`）
- ✅ Git Commit 规范（Conventional Commits）

**交付物**:
- 完整的项目文档集
- 代码规范配置文件

---

### 📊 Phase 0 验收总结

**必须达成的指标**:
- [ ] 所有中间件健康运行，可通过 `docker-compose ps` 验证
- [ ] 数据库建表完成，可通过 `psql -d scm_platform -c "\dt"` 查看
- [ ] CI/CD 流程至少成功运行 1 次
- [ ] 代码覆盖率基线 > 60%
- [ ] 所有开发文档已提交到 `docs/` 目录

**输出物清单**:
```
scm-platform/
├── docker-compose-infra.yml
├── scripts/
│   ├── init-db.sql
│   └── db/schema/*.sql
├── docs/
│   ├── infrastructure-setup.md
│   ├── database-design.md
│   ├── database-er-diagram.png
│   ├── api-design-guidelines.md
│   └── cicd-guide.md
├── .github/workflows/
│   ├── ci.yml
│   └── cd.yml
└── Dockerfile (各服务)
```

---

## Phase 1: 分布式事务与任务调度（第 2-3 周）

### 🎯 阶段目标
完成 Seata 分布式事务和 XXL-Job 任务调度的集成，实现跨服务的事务一致性保证。

### 📋 具体实施任务

#### Task 1.1: Seata Server 配置（1 天）

**实施内容**:
```yaml
# seata/application.yml
server:
  port: 7091

spring:
  application:
    name: seata-server

seata:
  config:
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace: seata
      group: SEATA_GROUP
      data-id: seataServer.properties

  registry:
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace: seata
      group: SEATA_GROUP
      application: seata-server

  store:
    mode: db
    db:
      datasource: druid
      db-type: postgresql
      driver-class-name: org.postgresql.Driver
      url: jdbc:postgresql://localhost:5432/seata
      user: postgres
      password: postgres

  # AT 模式配置
  service:
    vgroup-mapping:
      scm-order-group: default
      scm-inventory-group: default
      scm-payment-group: default
```

**验收标准**:
- ✅ Seata Server 启动成功，端口 7091 可访问
- ✅ Nacos 注册中心可以看到 seata-server 实例
- ✅ Seata 控制台可访问（如果部署了）
- ✅ 数据库创建 `global_table`, `branch_table`, `lock_table`

---

#### Task 1.2: 订单服务集成 Seata（2 天）

**实施内容**:

**1. POM 依赖**:
```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
</dependency>
<dependency>
    <groupId>io.seata</groupId>
    <artifactId>seata-spring-boot-starter</artifactId>
</dependency>
```

**2. 配置文件**:
```yaml
# scm-order/service/src/main/resources/application.yaml
seata:
  enabled: true
  application-id: scm-order-service
  tx-service-group: scm-order-group

  service:
    vgroup-mapping:
      scm-order-group: default

  registry:
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace: seata
      group: SEATA_GROUP

  config:
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace: seata
      group: SEATA_GROUP
```

**3. 分布式事务代码实现**:
```java
package com.frog.order.service.impl;

import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderMapper orderMapper;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    /**
     * 创建订单 - AT 模式分布式事务
     *
     * 事务流程:
     * 1. 本地: 创建订单记录
     * 2. 远程: 调用库存服务扣减库存
     * 3. 远程: 调用支付服务创建支付单
     *
     * 任一步骤失败，整个事务回滚
     */
    @Override
    @GlobalTransactional(
        name = "create-order-tx",
        rollbackFor = Exception.class,
        timeoutMills = 30000
    )
    public OrderDTO createOrder(CreateOrderRequest request) {
        log.info("开始创建订单，全局事务 XID: {}", RootContext.getXID());

        try {
            // Step 1: 创建订单（本地事务）
            Order order = new Order();
            order.setId(UUIDv7Util.generate());
            order.setOrderNo(generateOrderNo());
            order.setUserId(request.getUserId());
            order.setTotalAmount(request.getTotalAmount());
            order.setStatus("PENDING_PAYMENT");
            orderMapper.insert(order);

            log.info("订单创建成功: orderId={}, orderNo={}", order.getId(), order.getOrderNo());

            // Step 2: 扣减库存（远程 RPC 调用）
            for (OrderItemRequest item : request.getItems()) {
                DeductStockRequest deductRequest = DeductStockRequest.builder()
                    .skuId(item.getSkuId())
                    .quantity(item.getQuantity())
                    .orderId(order.getId())
                    .build();

                ApiResponse<Void> result = inventoryClient.deductStock(deductRequest);

                if (!result.isSuccess()) {
                    // 库存不足，抛出异常触发回滚
                    throw new BusinessException("库存不足: " + item.getSkuId());
                }

                log.info("库存扣减成功: skuId={}, quantity={}", item.getSkuId(), item.getQuantity());
            }

            // Step 3: 创建支付单（远程 RPC 调用）
            CreatePaymentRequest paymentRequest = CreatePaymentRequest.builder()
                .orderId(order.getId())
                .amount(request.getTotalAmount())
                .paymentMethod(request.getPaymentMethod())
                .build();

            ApiResponse<PaymentDTO> paymentResult = paymentClient.createPayment(paymentRequest);

            if (!paymentResult.isSuccess()) {
                throw new BusinessException("支付单创建失败");
            }

            log.info("支付单创建成功: paymentId={}", paymentResult.getData().getId());

            // 所有步骤成功，事务提交
            log.info("订单创建事务提交成功，XID: {}", RootContext.getXID());

            return OrderConverter.toDTO(order);

        } catch (Exception e) {
            log.error("订单创建失败，事务回滚，XID: {}, error: {}",
                     RootContext.getXID(), e.getMessage(), e);
            throw e; // 抛出异常触发 Seata 回滚
        }
    }

    private String generateOrderNo() {
        return "ORD" + System.currentTimeMillis() +
               RandomStringUtils.randomNumeric(6);
    }
}
```

**4. Undo Log 表创建**:
```sql
-- 每个参与分布式事务的数据库都需要创建此表
CREATE TABLE undo_log (
    id BIGSERIAL PRIMARY KEY,
    branch_id BIGINT NOT NULL,
    xid VARCHAR(100) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info BYTEA NOT NULL,
    log_status INT NOT NULL,
    log_created TIMESTAMP NOT NULL,
    log_modified TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX ux_undo_log ON undo_log(xid, branch_id);
```

**验收标准**:
- ✅ 订单服务启动时可以连接到 Seata Server
- ✅ 日志显示 `Global Transaction XID` 生成
- ✅ 正常流程：订单、库存、支付全部成功
- ✅ 异常回滚测试：
  - 库存不足时，订单记录回滚（数据库无订单数据）
  - 支付失败时，订单和库存都回滚
- ✅ 并发测试：50 并发创建订单，数据一致性 100%
- ✅ `undo_log` 表有数据写入和清理

---

#### Task 1.3: 库存服务集成 Seata（1 天）

**实施内容**:
```java
package com.frog.inventory.service.impl;

import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements IInventoryService {

    private final InventoryMapper inventoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 扣减库存 - Seata AT 模式参与者
     *
     * 注意:
     * 1. 不需要 @GlobalTransactional，只需要本地 @Transactional
     * 2. Seata 会自动拦截 SQL 并生成 undo_log
     * 3. 如果全局事务回滚，Seata 会自动执行补偿
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductStock(UUID skuId, Integer quantity, UUID orderId) {
        String xid = RootContext.getXID();
        log.info("参与全局事务，扣减库存: XID={}, skuId={}, quantity={}",
                 xid, skuId, quantity);

        // 查询当前库存
        Inventory inventory = inventoryMapper.selectBySkuId(skuId);

        if (inventory == null) {
            throw new BusinessException("SKU 不存在: " + skuId);
        }

        if (inventory.getAvailableQuantity() < quantity) {
            throw new BusinessException("库存不足: 可用=" +
                inventory.getAvailableQuantity() + ", 需要=" + quantity);
        }

        // 乐观锁更新库存
        int updated = inventoryMapper.deductStockWithVersion(
            skuId,
            quantity,
            inventory.getVersion()
        );

        if (updated == 0) {
            throw new BusinessException("库存扣减失败，请重试");
        }

        // 记录库存流水
        InventoryLog log = InventoryLog.builder()
            .skuId(skuId)
            .changeType("DEDUCT")
            .quantity(-quantity)
            .beforeQuantity(inventory.getAvailableQuantity())
            .afterQuantity(inventory.getAvailableQuantity() - quantity)
            .orderId(orderId)
            .build();
        inventoryLogMapper.insert(log);

        log.info("库存扣减成功: skuId={}, 剩余库存={}",
                 skuId, inventory.getAvailableQuantity() - quantity);
    }
}
```

**Mapper SQL**:
```xml
<update id="deductStockWithVersion">
    UPDATE inventory
    SET available_quantity = available_quantity - #{quantity},
        total_quantity = total_quantity - #{quantity},
        version = version + 1,
        update_time = CURRENT_TIMESTAMP
    WHERE sku_id = #{skuId}
      AND version = #{version}
      AND available_quantity >= #{quantity}
</update>
```

**验收标准**:
- ✅ 库存服务可以识别全局事务 XID
- ✅ 库存扣减成功时，数据库记录正确
- ✅ 当订单创建失败时，库存自动回滚到原值
- ✅ 乐观锁机制生效，并发冲突时重试成功
- ✅ `undo_log` 表记录了库存变更的前后镜像

---

#### Task 1.4: XXL-Job 调度中心集成（2 天）

**实施内容**:

**1. XXL-Job Admin 部署**:
```yaml
# docker-compose.yml
services:
  xxl-job-admin:
    image: xuxueli/xxl-job-admin:2.4.3
    container_name: xxl-job-admin
    ports:
      - "8088:8080"
    environment:
      PARAMS: >
        --spring.datasource.url=jdbc:postgresql://postgres:5432/xxl_job
        --spring.datasource.username=postgres
        --spring.datasource.password=postgres
    depends_on:
      - postgres
```

**2. 订单服务集成 XXL-Job**:
```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>2.4.3</version>
</dependency>
```

```java
// XXL-Job 配置
@Configuration
public class XxlJobConfig {

    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    @Value("${xxl.job.executor.appname}")
    private String appname;

    @Value("${xxl.job.executor.port}")
    private int port;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setPort(port);
        executor.setLogPath("/data/applogs/xxl-job/jobhandler");
        executor.setLogRetentionDays(30);
        return executor;
    }
}
```

**3. 定时任务实现 - 订单超时自动取消**:
```java
package com.frog.order.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutJob {

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /**
     * 订单超时自动取消任务
     *
     * 执行频率: 每 5 分钟一次
     * 处理逻辑: 查询创建时间超过 30 分钟且状态为 PENDING_PAYMENT 的订单，自动取消
     */
    @XxlJob("orderTimeoutCancelJob")
    public void execute() {
        long startTime = System.currentTimeMillis();

        XxlJobHelper.log("开始执行订单超时取消任务");

        try {
            // 查询超时订单（30分钟未支付）
            LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(30);
            List<Order> timeoutOrders = orderMapper.selectTimeoutOrders(
                "PENDING_PAYMENT",
                timeoutThreshold
            );

            XxlJobHelper.log("查询到超时订单数量: {}", timeoutOrders.size());

            int successCount = 0;
            int failCount = 0;

            for (Order order : timeoutOrders) {
                try {
                    // 取消订单（包含库存释放）
                    orderService.cancelOrder(order.getId(), "TIMEOUT");
                    successCount++;

                    XxlJobHelper.log("订单取消成功: orderId={}, orderNo={}",
                        order.getId(), order.getOrderNo());

                } catch (Exception e) {
                    failCount++;
                    XxlJobHelper.log("订单取消失败: orderId={}, error={}",
                        order.getId(), e.getMessage());
                }
            }

            long costTime = System.currentTimeMillis() - startTime;

            String result = String.format(
                "任务执行完成，耗时: %dms, 总数: %d, 成功: %d, 失败: %d",
                costTime, timeoutOrders.size(), successCount, failCount
            );

            XxlJobHelper.log(result);
            XxlJobHelper.handleSuccess(result);

        } catch (Exception e) {
            XxlJobHelper.log("任务执行异常: " + e.getMessage());
            XxlJobHelper.handleFail(e.getMessage());
        }
    }
}
```

**4. 在 XXL-Job Admin 配置任务**:
```
执行器: scm-order-executor
任务描述: 订单超时自动取消
Cron: 0 */5 * * * ?  (每5分钟执行一次)
JobHandler: orderTimeoutCancelJob
路由策略: 轮询
阻塞处理策略: 单机串行
```

**验收标准**:
- ✅ XXL-Job Admin 可访问 (http://localhost:8088/xxl-job-admin)
- ✅ 订单服务在 Admin 中注册成功（执行器列表可见）
- ✅ 任务配置完成并启动
- ✅ 任务每 5 分钟自动执行一次
- ✅ 超时订单被正确取消，状态变更为 `CANCELLED`
- ✅ 取消订单时库存自动释放（通过 Seata 保证）
- ✅ 任务执行日志清晰可见

---

#### Task 1.5: Seata TCC 模式实现（高级，2 天）

**实施内容**:
对于高并发场景（如秒杀），AT 模式性能不足，需要实现 TCC 模式。

```java
package com.frog.inventory.service.tcc;

import io.seata.rm.tcc.api.BusinessActionContext;
import io.seata.rm.tcc.api.BusinessActionContextParameter;
import io.seata.rm.tcc.api.LocalTCC;
import io.seata.rm.tcc.api.TwoPhaseBusinessAction;

/**
 * 库存扣减 TCC 接口
 */
@LocalTCC
public interface InventoryTccService {

    /**
     * Try 阶段: 预留库存
     *
     * @param skuId SKU ID
     * @param quantity 数量
     * @param orderId 订单 ID
     * @return 是否预留成功
     */
    @TwoPhaseBusinessAction(
        name = "inventoryTccService",
        commitMethod = "commit",
        rollbackMethod = "rollback"
    )
    boolean reserve(
        @BusinessActionContextParameter(paramName = "skuId") UUID skuId,
        @BusinessActionContextParameter(paramName = "quantity") Integer quantity,
        @BusinessActionContextParameter(paramName = "orderId") UUID orderId
    );

    /**
     * Confirm 阶段: 确认扣减库存
     */
    boolean commit(BusinessActionContext context);

    /**
     * Cancel 阶段: 释放预留库存
     */
    boolean rollback(BusinessActionContext context);
}
```

**实现类**:
```java
@Slf4j
@Service
public class InventoryTccServiceImpl implements InventoryTccService {

    @Autowired
    private InventoryMapper inventoryMapper;

    @Autowired
    private InventoryReservationMapper reservationMapper;

    /**
     * Try: 锁定库存（修改 locked_quantity）
     */
    @Override
    @Transactional
    public boolean reserve(UUID skuId, Integer quantity, UUID orderId) {
        String xid = RootContext.getXID();
        log.info("TCC Try阶段: 预留库存, XID={}, skuId={}, quantity={}",
                 xid, skuId, quantity);

        // 1. 扣减可用库存，增加锁定库存
        int updated = inventoryMapper.lockStock(skuId, quantity);

        if (updated == 0) {
            log.warn("库存不足，预留失败");
            return false;
        }

        // 2. 记录预留信息
        InventoryReservation reservation = InventoryReservation.builder()
            .skuId(skuId)
            .orderId(orderId)
            .reservedQuantity(quantity)
            .status(1) // 预留中
            .expireTime(LocalDateTime.now().plusMinutes(30))
            .build();
        reservationMapper.insert(reservation);

        log.info("库存预留成功");
        return true;
    }

    /**
     * Confirm: 确认扣减（从 locked_quantity 减少）
     */
    @Override
    @Transactional
    public boolean commit(BusinessActionContext context) {
        UUID skuId = (UUID) context.getActionContext("skuId");
        Integer quantity = (Integer) context.getActionContext("quantity");
        UUID orderId = (UUID) context.getActionContext("orderId");

        String xid = context.getXid();
        log.info("TCC Confirm阶段: 确认扣减库存, XID={}, skuId={}", xid, skuId);

        // 1. 减少锁定库存
        inventoryMapper.confirmDeduct(skuId, quantity);

        // 2. 更新预留记录状态
        reservationMapper.updateStatus(orderId, 2); // 已确认

        log.info("库存扣减确认成功");
        return true;
    }

    /**
     * Cancel: 释放预留库存
     */
    @Override
    @Transactional
    public boolean rollback(BusinessActionContext context) {
        UUID skuId = (UUID) context.getActionContext("skuId");
        Integer quantity = (Integer) context.getActionContext("quantity");
        UUID orderId = (UUID) context.getActionContext("orderId");

        String xid = context.getXid();
        log.info("TCC Cancel阶段: 释放预留库存, XID={}, skuId={}", xid, skuId);

        // 1. 恢复可用库存，减少锁定库存
        inventoryMapper.releaseStock(skuId, quantity);

        // 2. 更新预留记录状态
        reservationMapper.updateStatus(orderId, 3); // 已释放

        log.info("库存释放成功");
        return true;
    }
}
```

**SQL**:
```xml
<!-- 锁定库存 -->
<update id="lockStock">
    UPDATE inventory
    SET available_quantity = available_quantity - #{quantity},
        locked_quantity = locked_quantity + #{quantity},
        version = version + 1
    WHERE sku_id = #{skuId}
      AND available_quantity >= #{quantity}
</update>

<!-- 确认扣减 -->
<update id="confirmDeduct">
    UPDATE inventory
    SET locked_quantity = locked_quantity - #{quantity},
        total_quantity = total_quantity - #{quantity},
        version = version + 1
    WHERE sku_id = #{skuId}
</update>

<!-- 释放库存 -->
<update id="releaseStock">
    UPDATE inventory
    SET available_quantity = available_quantity + #{quantity},
        locked_quantity = locked_quantity - #{quantity},
        version = version + 1
    WHERE sku_id = #{skuId}
</update>
```

**验收标准**:
- ✅ TCC Try 阶段成功锁定库存
- ✅ Confirm 阶段正确扣减 `locked_quantity` 和 `total_quantity`
- ✅ Cancel 阶段正确释放库存
- ✅ 幂等性保证：重复 Confirm/Cancel 不会重复执行
- ✅ 性能测试：TCC 模式 TPS > AT 模式 50%

---

### 📊 Phase 1 验收总结

**必须达成的指标**:
- [ ] Seata AT 模式分布式事务成功率 100%
- [ ] 订单创建失败时，库存回滚成功率 100%
- [ ] XXL-Job 任务调度稳定运行，无遗漏
- [ ] 订单超时取消任务执行成功率 > 99%
- [ ] TCC 模式库存预留/确认/取消逻辑正确
- [ ] 压测：订单创建 TPS > 1000（AT 模式）
- [ ] 压测：订单创建 TPS > 5000（TCC 模式）

**输出物清单**:
```
scm-platform/
├── scm-order/service/
│   ├── OrderServiceImpl.java (AT 模式)
│   ├── OrderTccServiceImpl.java (TCC 模式)
│   └── OrderTimeoutJob.java (XXL-Job)
├── scm-inventory/service/
│   ├── InventoryServiceImpl.java (AT 模式)
│   └── InventoryTccServiceImpl.java (TCC 模式)
├── docs/
│   ├── seata-integration-guide.md
│   ├── xxl-job-task-list.md
│   └── distributed-transaction-test-report.md
└── scripts/
    ├── seata-server-setup.sh
    └── xxl-job-admin-setup.sh
```

---

## Phase 2: 商品服务 + Elasticsearch 搜索（第 4-5 周）

### 🎯 阶段目标
实现商品服务的完整 CRUD，集成 Elasticsearch 实现高性能商品搜索，集成 Canal 实现 MySQL 到 ES 的实时数据同步。

### 📋 具体实施任务

#### Task 2.1: 商品服务基础 CRUD（2 天）

**实施内容**:

**1. 商品实体类**:
```java
package com.frog.product.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.ASSIGN_UUID)
    private UUID id;

    private String productName;

    private UUID categoryId;

    private UUID brandId;

    private BigDecimal price;

    private Integer stockQuantity;

    /**
     * 商品状态: 1-在售 2-下架 3-售罄
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
```

**2. 商品 Service**:
```java
package com.frog.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product>
    implements IProductService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;

    /**
     * 分页查询商品
     */
    @Override
    public PageResult<ProductDTO> pageQuery(ProductQuery query) {
        Page<Product> page = new Page<>(query.getPageNum(), query.getPageSize());

        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getProductName() != null,
                    Product::getProductName, query.getProductName())
               .eq(query.getCategoryId() != null,
                   Product::getCategoryId, query.getCategoryId())
               .eq(query.getStatus() != null,
                   Product::getStatus, query.getStatus())
               .orderByDesc(Product::getCreateTime);

        Page<Product> resultPage = productMapper.selectPage(page, wrapper);

        return PageResult.of(
            resultPage.getRecords().stream()
                .map(ProductConverter::toDTO)
                .collect(Collectors.toList()),
            resultPage.getTotal()
        );
    }

    /**
     * 根据 ID 查询商品（带缓存）
     */
    @Override
    @Cacheable(value = "product", key = "#id", unless = "#result == null")
    public ProductDTO getById(UUID id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        return ProductConverter.toDTO(product);
    }

    /**
     * 创建商品
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductDTO createProduct(CreateProductRequest request) {
        // 验证分类是否存在
        ProductCategory category = categoryMapper.selectById(request.getCategoryId());
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 创建商品
        Product product = ProductConverter.toEntity(request);
        product.setId(UUIDv7Util.generate());
        product.setStatus(1); // 默认在售
        productMapper.insert(product);

        log.info("商品创建成功: id={}, name={}", product.getId(), product.getProductName());

        return ProductConverter.toDTO(product);
    }

    /**
     * 更新商品（清除缓存）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#id")
    public ProductDTO updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }

        // 更新字段
        if (request.getProductName() != null) {
            product.setProductName(request.getProductName());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }

        productMapper.updateById(product);

        log.info("商品更新成功: id={}", id);

        return ProductConverter.toDTO(product);
    }

    /**
     * 删除商品（逻辑删除 + 清除缓存）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "product", key = "#id")
    public void deleteProduct(UUID id) {
        productMapper.deleteById(id);
        log.info("商品删除成功: id={}", id);
    }
}
```

**3. Controller**:
```java
package com.frog.product.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final IProductService productService;

    @Operation(summary = "分页查询商品")
    @GetMapping
    public ApiResponse<PageResult<ProductDTO>> pageQuery(@Valid ProductQuery query) {
        return ApiResponse.success(productService.pageQuery(query));
    }

    @Operation(summary = "根据ID查询商品")
    @GetMapping("/{id}")
    public ApiResponse<ProductDTO> getById(@PathVariable UUID id) {
        return ApiResponse.success(productService.getById(id));
    }

    @Operation(summary = "创建商品")
    @PostMapping
    @PreAuthorize("hasAuthority('product:create')")
    public ApiResponse<ProductDTO> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success(productService.createProduct(request));
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:update')")
    public ApiResponse<ProductDTO> update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateProductRequest request
    ) {
        return ApiResponse.success(productService.updateProduct(id, request));
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:delete')")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ApiResponse.success();
    }
}
```

**验收标准**:
- ✅ CRUD 接口全部实现并测试通过
- ✅ 分页查询支持多条件筛选
- ✅ 缓存生效，第二次查询命中 Redis
- ✅ Swagger 文档自动生成
- ✅ 单元测试覆盖率 > 80%
- ✅ API 响应时间 < 50ms (p95)

---

#### Task 2.2: Elasticsearch 集成（3 天）

**实施内容**:

**1. POM 依赖**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
<dependency>
    <groupId>co.elastic.clients</groupId>
    <artifactId>elasticsearch-java</artifactId>
</dependency>
```

**2. ES 配置**:
```yaml
# application.yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: changeme
    connection-timeout: 10s
    socket-timeout: 30s
```

**3. ES 文档定义**:
```java
package com.frog.product.search.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Document(indexName = "products")
@Setting(
    shards = 3,
    replicas = 2,
    refreshInterval = "1s"
)
public class ProductDocument {

    @Id
    private UUID id;

    /**
     * 商品名称 - IK 分词器
     */
    @Field(
        type = FieldType.Text,
        analyzer = "ik_max_word",
        searchAnalyzer = "ik_smart"
    )
    private String productName;

    /**
     * 分类名称 - keyword 类型用于聚合
     */
    @Field(type = FieldType.Keyword)
    private String categoryName;

    /**
     * 品牌名称
     */
    @Field(type = FieldType.Keyword)
    private String brandName;

    /**
     * 价格 - 支持范围查询
     */
    @Field(type = FieldType.Double)
    private BigDecimal price;

    /**
     * 库存数量
     */
    @Field(type = FieldType.Integer)
    private Integer stockQuantity;

    /**
     * 商品状态
     */
    @Field(type = FieldType.Integer)
    private Integer status;

    /**
     * 销量 - 用于排序
     */
    @Field(type = FieldType.Long)
    private Long salesCount;

    /**
     * 评分 - 用于排序
     */
    @Field(type = FieldType.Double)
    private Double rating;

    /**
     * 创建时间
     */
    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime createTime;
}
```

**4. ES Repository**:
```java
package com.frog.product.search.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProductSearchRepository
    extends ElasticsearchRepository<ProductDocument, UUID> {

    /**
     * 根据商品名称搜索
     */
    List<ProductDocument> findByProductName(String productName);

    /**
     * 根据分类搜索
     */
    List<ProductDocument> findByCategoryName(String categoryName);

    /**
     * 价格区间搜索
     */
    List<ProductDocument> findByPriceBetween(
        BigDecimal minPrice,
        BigDecimal maxPrice
    );
}
```

**5. 搜索服务实现**:
```java
package com.frog.product.search.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchClient esClient;
    private final ProductSearchRepository searchRepository;

    /**
     * 综合搜索 - 支持关键词、分类、价格区间、排序
     */
    public PageResult<ProductDocument> search(ProductSearchQuery query) {
        try {
            // 构建查询条件
            BoolQuery.Builder boolQuery = QueryBuilders.bool();

            // 1. 关键词搜索（商品名称）
            if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
                boolQuery.must(QueryBuilders.match()
                    .field("productName")
                    .query(query.getKeyword())
                    .build()._toQuery());
            }

            // 2. 分类筛选
            if (query.getCategoryName() != null) {
                boolQuery.filter(QueryBuilders.term()
                    .field("categoryName")
                    .value(query.getCategoryName())
                    .build()._toQuery());
            }

            // 3. 价格区间筛选
            if (query.getMinPrice() != null || query.getMaxPrice() != null) {
                RangeQuery.Builder rangeQuery = QueryBuilders.range().field("price");

                if (query.getMinPrice() != null) {
                    rangeQuery.gte(JsonData.of(query.getMinPrice()));
                }
                if (query.getMaxPrice() != null) {
                    rangeQuery.lte(JsonData.of(query.getMaxPrice()));
                }

                boolQuery.filter(rangeQuery.build()._toQuery());
            }

            // 4. 只搜索在售商品
            boolQuery.filter(QueryBuilders.term()
                .field("status")
                .value(1)
                .build()._toQuery());

            // 构建搜索请求
            SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
                .index("products")
                .query(boolQuery.build()._toQuery())
                .from((query.getPageNum() - 1) * query.getPageSize())
                .size(query.getPageSize());

            // 5. 排序
            if ("price".equals(query.getSortField())) {
                searchBuilder.sort(s -> s
                    .field(f -> f
                        .field("price")
                        .order(query.isAsc() ? SortOrder.Asc : SortOrder.Desc)));
            } else if ("sales".equals(query.getSortField())) {
                searchBuilder.sort(s -> s
                    .field(f -> f
                        .field("salesCount")
                        .order(SortOrder.Desc)));
            } else {
                // 默认相关性排序
                searchBuilder.sort(s -> s.score(sc -> sc.order(SortOrder.Desc)));
            }

            // 执行搜索
            SearchResponse<ProductDocument> response = esClient.search(
                searchBuilder.build(),
                ProductDocument.class
            );

            // 提取结果
            List<ProductDocument> products = response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

            long total = response.hits().total().value();

            log.info("商品搜索完成: keyword={}, 结果数={}, 耗时={}ms",
                query.getKeyword(), total, response.took());

            return PageResult.of(products, total);

        } catch (Exception e) {
            log.error("商品搜索失败", e);
            throw new BusinessException("搜索服务异常");
        }
    }

    /**
     * 热门商品推荐 - 根据销量和评分排序
     */
    public List<ProductDocument> getHotProducts(int size) {
        try {
            FunctionScoreQuery functionScoreQuery = QueryBuilders.functionScore()
                .query(QueryBuilders.term()
                    .field("status")
                    .value(1)
                    .build()._toQuery())
                .functions(
                    // 销量权重 70%
                    FunctionScore.of(fs -> fs
                        .fieldValueFactor(fvf -> fvf
                            .field("salesCount")
                            .factor(0.7)
                            .missing(0.0))),
                    // 评分权重 30%
                    FunctionScore.of(fs -> fs
                        .fieldValueFactor(fvf -> fvf
                            .field("rating")
                            .factor(0.3)
                            .missing(0.0)))
                )
                .scoreMode(FunctionScoreMode.Sum)
                .build();

            SearchResponse<ProductDocument> response = esClient.search(s -> s
                .index("products")
                .query(functionScoreQuery._toQuery())
                .size(size),
                ProductDocument.class
            );

            return response.hits().hits().stream()
                .map(Hit::source)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("获取热门商品失败", e);
            return Collections.emptyList();
        }
    }
}
```

**6. Controller**:
```java
@Tag(name = "商品搜索")
@RestController
@RequestMapping("/api/products/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService searchService;

    @Operation(summary = "搜索商品")
    @GetMapping
    public ApiResponse<PageResult<ProductDocument>> search(
        @Valid ProductSearchQuery query
    ) {
        return ApiResponse.success(searchService.search(query));
    }

    @Operation(summary = "热门商品推荐")
    @GetMapping("/hot")
    public ApiResponse<List<ProductDocument>> getHotProducts(
        @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(searchService.getHotProducts(size));
    }
}
```

**验收标准**:
- ✅ Elasticsearch 索引创建成功
- ✅ IK 分词器安装并生效（中文分词正确）
- ✅ 全文搜索准确率 > 95%
- ✅ 搜索响应时间 < 100ms (p99)
- ✅ 支持多条件组合查询
- ✅ 支持按价格、销量、评分排序
- ✅ 热门商品推荐算法合理

---

#### Task 2.3: Canal 实时数据同步（2 天）

**实施内容**:

**1. Canal Server 部署**:
```yaml
# docker-compose.yml
services:
  canal-server:
    image: canal/canal-server:v1.1.7
    container_name: canal-server
    ports:
      - "11111:11111"
    environment:
      - canal.instance.master.address=mysql:3306
      - canal.instance.dbUsername=canal
      - canal.instance.dbPassword=canal
      - canal.instance.connectionCharset=UTF-8
      - canal.instance.filter.regex=scm_product\\..*
    volumes:
      - ./canal/conf:/home/admin/canal-server/conf
      - ./canal/logs:/home/admin/canal-server/logs
    depends_on:
      - mysql
```

**2. MySQL 配置（启用 binlog）**:
```sql
-- 检查 binlog 是否启用
SHOW VARIABLES LIKE 'log_bin';

-- my.cnf 配置
[mysqld]
log-bin=mysql-bin
binlog-format=ROW
server-id=1

-- 创建 Canal 用户
CREATE USER 'canal'@'%' IDENTIFIED BY 'canal';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;
```

**3. Canal Client 集成**:
```xml
<dependency>
    <groupId>com.alibaba.otter</groupId>
    <artifactId>canal.client</artifactId>
    <version>1.1.7</version>
</dependency>
```

**4. Canal 监听器**:
```java
package com.frog.product.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry.*;
import com.alibaba.otter.canal.protocol.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCanalListener implements CommandLineRunner {

    private final ProductSearchService searchService;
    private final ProductMapper productMapper;

    @Override
    public void run(String... args) {
        // 异步启动 Canal 监听
        new Thread(this::listenCanal, "canal-listener").start();
    }

    private void listenCanal() {
        // 连接 Canal Server
        CanalConnector connector = CanalConnectors.newSingleConnector(
            new InetSocketAddress("localhost", 11111),
            "example", // destination
            "canal",   // username
            "canal"    // password
        );

        try {
            connector.connect();
            connector.subscribe("scm_product\\.product"); // 订阅 product 表
            connector.rollback();

            log.info("Canal 监听启动成功，开始监听 product 表变更");

            while (true) {
                // 获取指定数量的数据
                Message message = connector.getWithoutAck(100);
                long batchId = message.getId();
                int size = message.getEntries().size();

                if (batchId == -1 || size == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    processEntries(message.getEntries());
                    connector.ack(batchId); // 提交确认
                }
            }

        } catch (Exception e) {
            log.error("Canal 监听异常", e);
        } finally {
            connector.disconnect();
        }
    }

    /**
     * 处理 binlog 变更事件
     */
    private void processEntries(List<Entry> entries) {
        for (Entry entry : entries) {
            if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN ||
                entry.getEntryType() == EntryType.TRANSACTIONEND) {
                continue;
            }

            if (entry.getEntryType() == EntryType.ROWDATA) {
                try {
                    RowChange rowChange = RowChange.parseFrom(entry.getStoreValue());
                    EventType eventType = rowChange.getEventType();

                    for (RowData rowData : rowChange.getRowDatasList()) {
                        if (eventType == EventType.INSERT) {
                            handleInsert(rowData);
                        } else if (eventType == EventType.UPDATE) {
                            handleUpdate(rowData);
                        } else if (eventType == EventType.DELETE) {
                            handleDelete(rowData);
                        }
                    }
                } catch (Exception e) {
                    log.error("处理 binlog 事件失败", e);
                }
            }
        }
    }

    /**
     * 处理 INSERT 事件 - 同步到 ES
     */
    private void handleInsert(RowData rowData) {
        UUID productId = extractProductId(rowData.getAfterColumnsList());
        log.info("检测到商品新增: productId={}", productId);

        // 从数据库查询完整数据
        Product product = productMapper.selectById(productId);
        if (product != null) {
            // 转换为 ES 文档并保存
            ProductDocument doc = ProductConverter.toDocument(product);
            searchService.saveDocument(doc);
            log.info("商品已同步到 Elasticsearch: productId={}", productId);
        }
    }

    /**
     * 处理 UPDATE 事件 - 更新 ES
     */
    private void handleUpdate(RowData rowData) {
        UUID productId = extractProductId(rowData.getAfterColumnsList());
        log.info("检测到商品更新: productId={}", productId);

        Product product = productMapper.selectById(productId);
        if (product != null && product.getDeleted() == 0) {
            ProductDocument doc = ProductConverter.toDocument(product);
            searchService.updateDocument(doc);
            log.info("Elasticsearch 文档已更新: productId={}", productId);
        } else {
            // 如果是逻辑删除，从 ES 中删除
            searchService.deleteDocument(productId);
            log.info("商品已从 Elasticsearch 删除: productId={}", productId);
        }
    }

    /**
     * 处理 DELETE 事件 - 从 ES 删除
     */
    private void handleDelete(RowData rowData) {
        UUID productId = extractProductId(rowData.getBeforeColumnsList());
        log.info("检测到商品删除: productId={}", productId);

        searchService.deleteDocument(productId);
        log.info("商品已从 Elasticsearch 删除: productId={}", productId);
    }

    /**
     * 从 Column 列表提取 product_id
     */
    private UUID extractProductId(List<Column> columns) {
        for (Column column : columns) {
            if ("id".equals(column.getName())) {
                return UUID.fromString(column.getValue());
            }
        }
        throw new IllegalStateException("未找到 product_id");
    }
}
```

**验收标准**:
- ✅ Canal Server 成功连接 MySQL
- ✅ Canal Client 成功订阅 product 表
- ✅ 新增商品后，ES 自动创建文档（延迟 < 1 秒）
- ✅ 更新商品后，ES 文档自动更新
- ✅ 删除商品后，ES 文档自动删除
- ✅ 监听稳定性：连续运行 24 小时无异常
- ✅ 数据一致性：MySQL 与 ES 数据一致性 100%

---

### 📊 Phase 2 验收总结

**必须达成的指标**:
- [ ] 商品 CRUD API 全部实现并测试通过
- [ ] ES 搜索准确率 > 95%
- [ ] ES 搜索响应时间 < 100ms (p99)
- [ ] Canal 实时同步延迟 < 1 秒
- [ ] MySQL 与 ES 数据一致性 100%
- [ ] 缓存命中率 > 80%
- [ ] API 单元测试覆盖率 > 80%

**输出物清单**:
```
scm-platform/
├── scm-product/service/
│   ├── ProductServiceImpl.java
│   ├── ProductSearchService.java
│   ├── ProductCanalListener.java
│   └── ProductConverter.java
├── docs/
│   ├── elasticsearch-integration.md
│   ├── canal-setup-guide.md
│   ├── product-api-doc.md
│   └── search-performance-test-report.md
└── scripts/
    ├── elasticsearch-index-create.sh
    └── canal-server-setup.sh
```

---

由于篇幅限制，我将在下一个文档中继续详细拆分 Phase 3-6。

让我先提交这个文档。