# 应用层配置和使用示例

本文档提供 SCM 平台应用层的配置示例和代码使用示例。

---

## 1. Spring Boot 配置文件

### 1.1 application.yml（主配置文件）

```yaml
spring:
  application:
    name: scm-order-service

  # 数据源配置
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/scm_platform?serverTimezone=UTC&useUnicode=true&characterEncoding=utf8
    username: postgres
    password: your_password

    # HikariCP 连接池配置
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      auto-commit: true
      idle-timeout: 30000
      pool-name: ScmHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1

  # Redis 配置（用于缓存权限、配额等）
  redis:
    host: localhost
    port: 6379
    password:
    database: 0
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms
    timeout: 3000ms

  # Jackson 配置
  jackson:
    time-zone: GMT+8
    date-format: yyyy-MM-dd HH:mm:ss
    serialization:
      write-dates-as-timestamps: false

# MyBatis-Plus 配置
mybatis-plus:
  # Mapper XML 文件位置
  mapper-locations: classpath*:/mapper/**/*.xml

  # 实体类扫描路径
  type-aliases-package: com.frog.*.entity

  # 全局配置
  global-config:
    # 数据库相关配置
    db-config:
      # 主键类型（INPUT表示手动输入，我们通过MetaObjectHandler自动填充UUIDv7）
      id-type: INPUT
      # 逻辑删除字段
      logic-delete-field: deleted
      logic-delete-value: true
      logic-not-delete-value: false

    # Banner 关闭
    banner: false

  # 配置项
  configuration:
    # 驼峰转下划线
    map-underscore-to-camel-case: true
    # 缓存
    cache-enabled: false
    # 日志实现
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

# XXL-Job 配置
xxl:
  job:
    admin:
      # XXL-Job 调度中心地址
      addresses: http://localhost:8080/xxl-job-admin
    executor:
      # 执行器AppName（唯一标识）
      appname: scm-order-executor
      # 执行器注册地址
      address:
      # 执行器IP（为空则自动获取）
      ip:
      # 执行器端口（为0则自动获取）
      port: 9999
      # 执行器日志路径
      logpath: /data/applogs/xxl-job/jobhandler
      # 日志保留天数
      logretentiondays: 30
    # 访问令牌
    accessToken: default_token

# Seata 分布式事务配置
seata:
  enabled: true
  application-id: ${spring.application.name}
  tx-service-group: scm_tx_group
  registry:
    type: nacos
    nacos:
      application: seata-server
      server-addr: localhost:8848
      namespace:
      group: SEATA_GROUP
  config:
    type: nacos
    nacos:
      server-addr: localhost:8848
      namespace:
      group: SEATA_GROUP
      data-id: seataServer.properties

# 日志配置
logging:
  level:
    root: INFO
    com.frog: DEBUG
    com.frog.common.tenant.TenantInterceptor: DEBUG
    com.frog.common.mybatis.AuditMetaObjectHandler: DEBUG
  pattern:
    console: '%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%n'
    file: '%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%n'
  file:
    name: /data/applogs/scm-order-service/application.log
    max-size: 100MB
    max-history: 30

# 服务器配置
server:
  port: 8081
  servlet:
    context-path: /api/order
  tomcat:
    uri-encoding: UTF-8
    max-threads: 200
    min-spare-threads: 10

# 管理端点配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 1.2 application-dev.yml（开发环境）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/scm_platform_dev?serverTimezone=UTC
    username: postgres
    password: dev_password

  redis:
    host: localhost
    port: 6379

logging:
  level:
    com.frog: DEBUG

xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin
```

### 1.3 application-prod.yml（生产环境）

```yaml
spring:
  datasource:
    url: jdbc:postgresql://prod-db.example.com:5432/scm_platform?serverTimezone=UTC
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 50

  redis:
    host: ${REDIS_HOST}
    port: 6379
    password: ${REDIS_PASSWORD}

logging:
  level:
    root: WARN
    com.frog: INFO

xxl:
  job:
    admin:
      addresses: ${XXL_JOB_ADMIN_ADDRESSES}
    accessToken: ${XXL_JOB_ACCESS_TOKEN}
```

---

## 2. MyBatis-Plus 配置类

### 2.1 MybatisPlusConfig.java

```java
package com.frog.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.frog.common.mybatis.AuditMetaObjectHandler;
import com.frog.common.tenant.TenantInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 租户拦截器（自动注入 tenant_id）
        interceptor.addInnerInterceptor(new TenantInterceptor());

        // 2. 分页拦截器
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        paginationInterceptor.setMaxLimit(1000L); // 最大分页限制
        paginationInterceptor.setOverflow(false); // 溢出总页数后是否进行处理
        interceptor.addInnerInterceptor(paginationInterceptor);

        // 3. 乐观锁拦截器
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }

    /**
     * 审计字段自动填充处理器
     */
    @Bean
    public AuditMetaObjectHandler auditMetaObjectHandler() {
        return new AuditMetaObjectHandler();
    }
}
```

---

## 3. 多租户配置

### 3.1 TenantConfig.java

```java
package com.frog.config;

import com.frog.common.tenant.TenantFilter;
import com.frog.common.tenant.QuotaChecker;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 多租户配置类
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Configuration
public class TenantConfig {

    /**
     * 注册租户过滤器
     */
    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration() {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new TenantFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("tenantFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * 配额检查切面
     */
    @Bean
    public QuotaChecker quotaChecker() {
        return new QuotaChecker();
    }
}
```

---

## 4. 实体类示例

### 4.1 BaseEntity.java（基础实体类）

```java
package com.frog.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 基础实体类
 * 所有实体类继承此类，自动拥有审计字段
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Data
public abstract class BaseEntity implements Serializable {

    /**
     * 主键ID（UUIDv7）
     * 由 AuditMetaObjectHandler 自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private UUID id;

    /**
     * 租户ID
     * 由 AuditMetaObjectHandler 自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private UUID tenantId;

    /**
     * 创建时间（UTC时间）
     * 由 AuditMetaObjectHandler 自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createTime;

    /**
     * 创建人ID
     * 由 AuditMetaObjectHandler 自动填充
     */
    @TableField(fill = FieldFill.INSERT)
    private UUID createBy;

    /**
     * 更新时间（UTC时间）
     * 由 AuditMetaObjectHandler 自动填充
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updateTime;

    /**
     * 更新人ID
     * 由 AuditMetaObjectHandler 自动填充
     */
    @TableField(fill = FieldFill.UPDATE)
    private UUID updateBy;

    /**
     * 乐观锁版本号
     */
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;

    /**
     * 逻辑删除标志
     * 由 AuditMetaObjectHandler 自动填充
     */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Boolean deleted;
}
```

### 4.2 Order.java（订单实体类示例）

```java
package com.frog.order.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.frog.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 订单实体类
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ord_order")
public class Order extends BaseEntity {

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单类型
     */
    private String orderType;

    /**
     * 订单状态
     */
    private String status;

    /**
     * 客户ID
     */
    private UUID customerId;

    /**
     * 下单时间
     */
    private OffsetDateTime orderTime;

    /**
     * 订单总金额
     */
    private BigDecimal totalAmount;

    /**
     * 应付金额
     */
    private BigDecimal payableAmount;

    /**
     * 备注
     */
    private String remark;
}
```

---

## 5. Mapper 示例

### 5.1 OrderMapper.java

```java
package com.frog.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frog.order.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

/**
 * 订单 Mapper
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 根据订单编号查询订单
     * TenantInterceptor 会自动添加 tenant_id 条件
     */
    Order selectByOrderNo(@Param("orderNo") String orderNo);

    /**
     * 查询待支付订单列表
     * TenantInterceptor 会自动添加 tenant_id 条件
     */
    List<Order> selectPendingPaymentOrders(@Param("customerId") UUID customerId);
}
```

### 5.2 OrderMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.frog.order.mapper.OrderMapper">

    <!-- 基础ResultMap -->
    <resultMap id="BaseResultMap" type="com.frog.order.entity.Order">
        <id column="id" property="id"/>
        <result column="tenant_id" property="tenantId"/>
        <result column="order_no" property="orderNo"/>
        <result column="order_type" property="orderType"/>
        <result column="status" property="status"/>
        <result column="customer_id" property="customerId"/>
        <result column="order_time" property="orderTime"/>
        <result column="total_amount" property="totalAmount"/>
        <result column="payable_amount" property="payableAmount"/>
        <result column="remark" property="remark"/>
        <result column="create_time" property="createTime"/>
        <result column="create_by" property="createBy"/>
        <result column="update_time" property="updateTime"/>
        <result column="update_by" property="updateBy"/>
        <result column="version" property="version"/>
        <result column="deleted" property="deleted"/>
    </resultMap>

    <!-- 根据订单编号查询订单 -->
    <!-- 注意：不需要手动添加 tenant_id 条件，TenantInterceptor 会自动注入 -->
    <select id="selectByOrderNo" resultMap="BaseResultMap">
        SELECT *
        FROM ord_order
        WHERE order_no = #{orderNo}
          AND NOT deleted
    </select>

    <!-- 查询待支付订单列表 -->
    <select id="selectPendingPaymentOrders" resultMap="BaseResultMap">
        SELECT *
        FROM ord_order
        WHERE customer_id = #{customerId}
          AND status = 'PENDING_PAYMENT'
          AND NOT deleted
        ORDER BY order_time DESC
    </select>

</mapper>
```

---

## 6. Service 示例

### 6.1 OrderService.java

```java
package com.frog.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.frog.common.tenant.quota.QuotaType;
import com.frog.common.tenant.quota.RequireQuotaCheck;
import com.frog.order.dto.OrderCreateDTO;
import com.frog.order.entity.Order;

import java.util.List;
import java.util.UUID;

/**
 * 订单服务接口
 *
 * @author Claude Code
 * @since 2025-01-24
 */
public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     * 使用 @RequireQuotaCheck 自动检查租户的订单配额
     *
     * @param dto 订单创建DTO
     * @return 订单ID
     */
    @RequireQuotaCheck(quotaType = QuotaType.ORDERS, increment = 1)
    UUID createOrder(OrderCreateDTO dto);

    /**
     * 根据订单编号查询订单
     */
    Order getByOrderNo(String orderNo);

    /**
     * 查询客户的待支付订单
     */
    List<Order> getPendingPaymentOrders(UUID customerId);

    /**
     * 取消订单
     */
    boolean cancelOrder(UUID orderId);
}
```

### 6.2 OrderServiceImpl.java

```java
package com.frog.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frog.common.util.UUIDv7Util;
import com.frog.order.dto.OrderCreateDTO;
import com.frog.order.entity.Order;
import com.frog.order.mapper.OrderMapper;
import com.frog.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * 订单服务实现类
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final OrderMapper orderMapper;

    /**
     * 创建订单
     * 注意：
     * 1. @RequireQuotaCheck 会在方法执行前自动检查配额
     * 2. 无需手动设置 id, tenant_id, create_time 等字段，AuditMetaObjectHandler 会自动填充
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UUID createOrder(OrderCreateDTO dto) {
        // 1. 构建订单实体
        Order order = new Order();
        // id, tenant_id, create_time 等字段会被自动填充
        order.setOrderNo(generateOrderNo());
        order.setOrderType(dto.getOrderType());
        order.setStatus("PENDING_PAYMENT");
        order.setCustomerId(dto.getCustomerId());
        order.setOrderTime(OffsetDateTime.now(ZoneOffset.UTC));
        order.setTotalAmount(dto.getTotalAmount());
        order.setPayableAmount(dto.getTotalAmount());
        order.setRemark(dto.getRemark());

        // 2. 保存订单
        this.save(order);

        log.info("订单创建成功，订单ID: {}, 订单编号: {}", order.getId(), order.getOrderNo());

        return order.getId();
    }

    @Override
    public Order getByOrderNo(String orderNo) {
        return orderMapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<Order> getPendingPaymentOrders(UUID customerId) {
        return orderMapper.selectPendingPaymentOrders(customerId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(UUID orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            log.warn("订单不存在，订单ID: {}", orderId);
            return false;
        }

        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            log.warn("订单状态不允许取消，订单ID: {}, 当前状态: {}", orderId, order.getStatus());
            return false;
        }

        order.setStatus("CANCELLED");
        // update_time, update_by 会被自动填充
        return this.updateById(order);
    }

    /**
     * 生成订单编号
     */
    private String generateOrderNo() {
        // 使用 UUIDv7 的时间戳部分作为订单号的一部分
        UUID uuid = UUIDv7Util.generate();
        long timestamp = UUIDv7Util.extractTimestamp(uuid);
        return "ORD" + timestamp + String.format("%06d", (int) (Math.random() * 1000000));
    }
}
```

---

## 7. Controller 示例

### 7.1 OrderController.java

```java
package com.frog.order.controller;

import com.frog.common.result.Result;
import com.frog.order.dto.OrderCreateDTO;
import com.frog.order.entity.Order;
import com.frog.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * 订单控制器
 *
 * @author Claude Code
 * @since 2025-01-24
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<UUID> createOrder(@Valid @RequestBody OrderCreateDTO dto) {
        UUID orderId = orderService.createOrder(dto);
        return Result.success(orderId);
    }

    @Operation(summary = "根据订单编号查询订单")
    @GetMapping("/no/{orderNo}")
    public Result<Order> getByOrderNo(@PathVariable String orderNo) {
        Order order = orderService.getByOrderNo(orderNo);
        return Result.success(order);
    }

    @Operation(summary = "查询待支付订单")
    @GetMapping("/pending/{customerId}")
    public Result<List<Order>> getPendingPaymentOrders(@PathVariable UUID customerId) {
        List<Order> orders = orderService.getPendingPaymentOrders(customerId);
        return Result.success(orders);
    }

    @Operation(summary = "取消订单")
    @PutMapping("/{orderId}/cancel")
    public Result<Boolean> cancelOrder(@PathVariable UUID orderId) {
        boolean success = orderService.cancelOrder(orderId);
        return Result.success(success);
    }
}
```

---

## 8. HTTP 请求示例

### 8.1 创建订单

```http
POST http://localhost:8081/api/order/orders
Content-Type: application/json
X-Tenant-Id: 123e4567-e89b-12d3-a456-426614174000

{
  "orderType": "SALES",
  "customerId": "223e4567-e89b-12d3-a456-426614174000",
  "totalAmount": 1299.00,
  "remark": "测试订单"
}
```

### 8.2 查询订单

```http
GET http://localhost:8081/api/order/orders/no/ORD202501240001
X-Tenant-Id: 123e4567-e89b-12d3-a456-426614174000
```

---

## 9. 使用要点总结

### 9.1 实体类

- 继承 `BaseEntity`，自动拥有审计字段
- 无需手动设置 `id`, `tenant_id`, `create_time` 等字段
- 使用 `@TableName` 指定表名
- 使用 `@TableField` 自定义字段映射

### 9.2 Mapper

- 继承 `BaseMapper<T>`，拥有基础CRUD方法
- **重要**：查询时不需要手动添加 `tenant_id` 条件，`TenantInterceptor` 会自动注入

### 9.3 Service

- 继承 `IService<T>` 接口，实现类继承 `ServiceImpl<M, T>`
- 使用 `@RequireQuotaCheck` 注解进行配额检查
- 使用 `@Transactional` 注解声明事务

### 9.4 Controller

- 使用 `@RestController` 和 `@RequestMapping`
- HTTP 请求头必须携带 `X-Tenant-Id`，否则会报错
- 返回统一的 `Result` 对象

### 9.5 配额检查

```java
@RequireQuotaCheck(quotaType = QuotaType.ORDERS, increment = 1)
public UUID createOrder(OrderCreateDTO dto) {
    // 方法执行前会自动检查租户的订单配额
    // 如果配额不足，抛出 QuotaExceededException
}
```

### 9.6 租户上下文

```java
// 获取当前租户ID
UUID tenantId = TenantContextHolder.getTenantId();

// 在指定租户上下文中执行代码
TenantContextHolder.executeInTenantContext(tenantId, () -> {
    // 这里的代码会在指定租户的上下文中执行
    return orderService.createOrder(dto);
});
```

---

**文档版本**：v1.0
**最后更新**：2025-01-24
**作者**：Claude Code