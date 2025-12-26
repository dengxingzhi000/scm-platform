# SCM 平台服务架构概览

## 服务端口映射表

### 基础设施层 (Infrastructure Layer)

| 服务名 | 端口 | 数据库 | 说明 |
|-------|------|--------|------|
| scm-gateway | 9095 | - | API 网关 (Spring Cloud Gateway) |
| scm-auth | 8106 | - | 认证服务 (JWT, OAuth2, WebAuthn) |
| scm-system | - | db_user, db_org, db_permission | 用户/组织/权限服务 (合并) |

### 基础服务层 (Base Services Layer)

| 服务名 | 端口 | 数据库 | 说明 |
|-------|------|--------|------|
| scm-approval | 8209 | db_approval | 审批流程服务 |
| scm-audit | 8210 | db_audit | 审计日志服务 (分区表) |
| scm-notify | 8211 | db_notify | 多渠道通知服务 (邮件/短信/站内信) |
| scm-tenant | 8212 | db_tenant | 租户管理服务 (SaaS 多租户、配额、计费) |

### 供应链核心层 (Supply Chain Core Layer)

| 服务名 | 端口 | 数据库 | 说明 |
|-------|------|--------|------|
| scm-product | 8201 | db_product | 商品服务 (SPU/SKU) |
| scm-inventory | 8202 | db_inventory | 库存服务 (分库分表) |
| scm-order | 8203 | db_order | 订单服务 (分区表) |
| scm-warehouse | 8204 | db_warehouse | 仓储服务 (WMS 波次拣货) |
| scm-logistics | 8205 | db_logistics | 物流服务 (TMS 轨迹追踪) |
| scm-purchase | 8207 | db_purchase | 采购服务 (询价/合同/订单) |

### 供应商与财务层 (Supplier & Finance Layer)

| 服务名 | 端口 | 数据库 | 说明 |
|-------|------|--------|------|
| scm-supplier | 8206 | db_supplier | 供应商管理服务 |
| scm-finance | 8208 | db_finance | 财务服务 (结算/发票/付款) |

---

## 服务模块结构

所有业务服务采用统一的 **API + Service** 结构:

```
scm-<service>/
├── pom.xml                          # 父 POM
├── api/                             # Dubbo RPC API 定义
│   ├── pom.xml
│   └── src/main/java/com/frog/<service>/api/
└── service/                         # 服务实现
    ├── pom.xml
    └── src/
        ├── main/
        │   ├── java/com/frog/<service>/
        │   │   ├── <Service>Application.java    # 启动类
        │   │   ├── controller/                   # REST API
        │   │   ├── service/                      # 业务逻辑
        │   │   ├── mapper/                       # MyBatis Mapper
        │   │   └── domain/entity/                # 实体类
        │   └── resources/
        │       ├── application.yml               # 配置文件
        │       └── mapper/                       # MyBatis XML
        └── test/
```

---

## 技术栈

### 核心框架
- **Java 21** (Virtual Threads + Pattern Matching)
- **Spring Boot 4.0.0**
- **Spring Cloud 2025.1.0**
- **Spring Cloud Alibaba 2025.0.0.0**

### 服务治理
- **Nacos** - 服务注册与配置中心
- **Sentinel** - 流量控制与熔断降级
- **Seata 2.2.0** - 分布式事务 (AT 模式)
- **XXL-Job 2.4.3** - 分布式任务调度

### 数据层
- **PostgreSQL** - 主数据库
- **MyBatis-Plus 3.5.15** - ORM 框架
- **ShardingSphere 5.5.1** - 分库分表
- **Redis** - 缓存与分布式锁

### 消息队列
- **Kafka** - 高吞吐量事件流
- **RabbitMQ** - 可靠消息投递

### 监控与链路追踪
- **SkyWalking** - 分布式链路追踪
- **Prometheus** - 指标监控
- **Micrometer** - 应用指标采集

---

## 服务依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                      API Gateway (9095)                      │
│                     scm-gateway                              │
└────────────────────────┬────────────────────────────────────┘
                         │
        ┌────────────────┴────────────────┬───────────────────┐
        │                                 │                   │
┌───────▼────────┐              ┌────────▼────────┐  ┌──────▼──────┐
│  scm-auth      │              │  scm-system     │  │ scm-approval│
│  (8106)        │              │  (用户/权限)     │  │  (8209)     │
└────────────────┘              └─────────────────┘  └─────────────┘
                                         │
                    ┌────────────────────┼────────────────────┐
                    │                    │                    │
            ┌───────▼────────┐  ┌───────▼────────┐  ┌───────▼────────┐
            │  scm-product   │  │  scm-inventory │  │   scm-order    │
            │    (8201)      │  │     (8202)     │  │    (8203)      │
            └────────────────┘  └────────────────┘  └────────┬───────┘
                                         │                    │
                    ┌────────────────────┼────────────────────┘
                    │                    │
            ┌───────▼────────┐  ┌───────▼────────┐
            │  scm-warehouse │  │  scm-logistics │
            │    (8204)      │  │     (8205)     │
            └────────────────┘  └────────────────┘
```

---

## 数据库设计亮点

### 1. 分区表设计
- **订单表** (`ord_order`): 按月分区 (2025-01 ~ 2025-12)
- **采购订单** (`pur_order`): 按月分区
- **审计日志** (`aud_operation_log`): 按月分区,便于归档

### 2. 分库分表
- **库存表** (`inv_stock`): 按 `sku_id` 取模分表 (8张表)
- **订单表** (`ord_order`): 按 `user_id` 分库分表 (支持亿级数据)

### 3. 多租户隔离
- 所有业务表包含 `tenant_id` 字段
- 行级数据隔离
- 平台资源 vs 租户资源区分

### 4. 软删除
- 所有表支持逻辑删除 (`deleted` 字段)
- 索引自动排除已删除数据

### 5. UUIDv7 主键
- 时间有序 UUID (前48位时间戳)
- 分布式友好,无需中心化 ID 生成器
- B+树索引性能优异

---

## 启动顺序

```bash
# 1. 启动基础设施
docker-compose up -d              # Nacos, Redis, PostgreSQL, Kafka, RabbitMQ

# 2. 启动认证服务
cd scm-auth/auth && mvn spring-boot:run

# 3. 启动基础服务 (任意顺序)
cd scm-system/service && mvn spring-boot:run
cd scm-tenant/service && mvn spring-boot:run
cd scm-approval/service && mvn spring-boot:run
cd scm-audit/service && mvn spring-boot:run
cd scm-notify/service && mvn spring-boot:run

# 4. 启动业务服务 (任意顺序)
cd scm-product/service && mvn spring-boot:run
cd scm-inventory/service && mvn spring-boot:run
cd scm-order/service && mvn spring-boot:run
cd scm-warehouse/service && mvn spring-boot:run
cd scm-logistics/service && mvn spring-boot:run
cd scm-purchase/service && mvn spring-boot:run
cd scm-supplier/service && mvn spring-boot:run
cd scm-finance/service && mvn spring-boot:run

# 5. 最后启动网关
cd scm-gateway/gateway && mvn spring-boot:run
```

---

## 服务健康检查

```bash
# Gateway 健康检查
curl http://localhost:9095/actuator/health

# 各服务健康检查
curl http://localhost:8201/actuator/health  # Product
curl http://localhost:8202/actuator/health  # Inventory
curl http://localhost:8203/actuator/health  # Order
curl http://localhost:8207/actuator/health  # Purchase
curl http://localhost:8208/actuator/health  # Finance
curl http://localhost:8209/actuator/health  # Approval
curl http://localhost:8210/actuator/health  # Audit
curl http://localhost:8211/actuator/health  # Notify
curl http://localhost:8212/actuator/health  # Tenant
```

---

## API 文档访问

- **Knife4j 文档**: http://localhost:9095/doc.html
- **各服务 Swagger**: http://localhost:<port>/swagger-ui/index.html

---

## Nacos 配置

所有服务在 Nacos 注册,命名规则:
```
${spring.application.name}-${spring.profiles.active}.yaml
```

示例:
- `scm-purchase-dev.yaml`
- `scm-finance-dev.yaml`
- `scm-approval-dev.yaml`

---

## 下一步工作

### 已完成 ✅
- [x] 创建 scm-purchase (采购服务)
- [x] 创建 scm-finance (财务服务)
- [x] 创建 scm-approval (审批服务)
- [x] 创建 scm-audit (审计服务)
- [x] 创建 scm-notify (通知服务)
- [x] 创建 scm-tenant (租户服务)
- [x] 迁移 XXL-Job 定时任务到对应服务
- [x] 更新父 pom.xml
- [x] 清理冗余目录 (scm-services)

### 待实现 🚧
- [ ] 实现各服务的 Domain Entity (实体类)
- [ ] 实现各服务的 Mapper 接口
- [ ] 实现各服务的 Service 业务逻辑
- [ ] 实现各服务的 Controller REST API
- [ ] 实现 Dubbo RPC 接口
- [ ] 添加单元测试和集成测试
- [ ] 配置 Seata 分布式事务
- [ ] 配置 Sentinel 流量控制规则
- [ ] 实现 Canal 数据同步 (MySQL → Elasticsearch)

---

**创建时间**: 2025-12-25
**版本**: v1.0
**作者**: SCM Platform Team
