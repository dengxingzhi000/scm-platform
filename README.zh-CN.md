<div align="center">

# SCM Platform

**企业级供应链管理系统**

[![Build Status](https://img.shields.io/github/actions/workflow/status/dengxingzhi000/scm-platform/maven-build.yml?branch=master&style=flat-square&logo=github&label=build)](https://github.com/dengxingzhi000/scm-platform/actions)
[![Stars](https://img.shields.io/github/stars/dengxingzhi000/scm-platform?color=ffcb47&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/stargazers)
[![Forks](https://img.shields.io/github/forks/dengxingzhi000/scm-platform?color=8ae8ff&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/network/members)
[![Issues](https://img.shields.io/github/issues/dengxingzhi000/scm-platform?color=ff80eb&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/issues)
[![License](https://img.shields.io/badge/license-Apache%202.0-white?labelColor=black&style=flat-square)](https://github.com/dengxingzhi000/scm-platform/blob/master/LICENSE)

[English](./README.md) | [简体中文](./README.zh-CN.md)

</div>

---

> 基于 Spring Boot 4 + Spring Cloud 2025 构建的云原生微服务架构供应链管理平台，面向秒杀等高并发场景，架构模式借鉴阿里菜鸟、京东物流与美团配送体系。

## 项目亮点

| 图标 | 特性 | 说明 |
|------|------|------|
| 🏗️ | **微服务架构** | 22+ 个可独立部署服务，含 API 网关、服务发现与配置中心 |
| 🔐 | **企业级安全** | OAuth2 + JWT + WebAuthn 无密码登录，RBAC 细粒度数据权限控制 |
| 💰 | **分布式事务** | Seata AT/TCC/Saga 多种模式保障跨服务数据一致性 |
| ⚡ | **高性能** | Redis Lua 原子库存扣减、读写分离、数据库分库分表 |
| 🔍 | **全文搜索** | Elasticsearch 检索能力，通过 Canal 监听 Binlog 实现近实时同步 |
| 🧠 | **决策引擎** | 电商决策矩阵，支持智能定价、库存预测、A/B 测试 |
| 📊 | **可观测性** | Sentinel 熔断、SkyWalking 链路追踪、Prometheus 指标、Grafana 看板 |
| 🏢 | **多租户** | 租户隔离、动态数据源路由、可配置特性开关 |
| 🖥️ | **现代化前端** | Next.js 15 App Router、Ant Design 5 Pro、Zustand 状态管理、TanStack Query |
| 📦 | **领域驱动设计** | 清晰的 CQRS 分离、聚合根、通过 Kafka/RabbitMQ 传递领域事件 |
| ☸️ | **云原生** | 核心服务的 Kubernetes 清单、Helm Chart、ArgoCD GitOps、灰度发布（auth） |

## 技术栈

| 层级 | 组件 |
|-------|-----------|
| **后端** | Java 21（虚拟线程）、Spring Boot 4.0.6、Spring Cloud 2025.1.2 |
| **Alibaba** | Spring Cloud Alibaba 2025.1.0.0（Nacos、Sentinel、Seata） |
| **数据库** | PostgreSQL、MyBatis-Plus 3.5.15、ShardingSphere 5.5.1 |
| **缓存** | Redis（分布式缓存，Lua 脚本实现原子操作） |
| **搜索** | Elasticsearch 8.11.4、Canal 1.1.7（Binlog 同步） |
| **消息** | Kafka（事件流）、RabbitMQ（可靠队列） |
| **RPC** | Apache Dubbo 3.3.6（内部服务调用） |
| **调度** | XXL-Job 3.3.1（分布式任务调度） |
| **监控** | Sentinel、SkyWalking 9.3.0、Micrometer + Prometheus |
| **前端** | Next.js 15（App Router）、React 19、Ant Design 5、Zustand 5、TanStack Query 5、ECharts、next-intl |
| **DevOps** | Docker、Kubernetes、Helm、ArgoCD、GitHub Actions、SonarCloud、OWASP |
| **测试** | JUnit 5、k6 压测、Pact 契约测试、Playwright E2E、PIT 变异测试 |

## 架构

```
   ┌──────────────────┐
   │  前端            │
   │  scm-web :3000   │
   └────────┬─────────┘
            │
   ┌────────▼─────────┐
   │  API 网关        │
   │  scm-gateway     │
   │  :8761           │
   └────────┬─────────┘
            │
   ┌────────▼───────────────────────────────────────────────┐
   │  供应链核心层 — Dubbo RPC 经 Nacos 注册中心              │
   │  auth :8106 · system :8081 · file :8201⚠ · product :8201⚠│
   │  inventory :8202 · order :8203 · warehouse :8204        │
   │  logistics :8205 · supplier :8206 · purchase :8207      │
   │  finance :8208 · message :8209⚠ · approval :8209⚠       │
   │  audit :8210 · notify :8211 · tenant :8212              │
   └────────┬───────────────────────────────────────────────┘
            │
   ┌────────▼───────────────────────────────────────────────┐
   │  电商业务层                                              │
   │  mall :8301 · member :8302 · promotion :8303             │
   │  payment :8304 · order-center :8305 · fulfillment :8306  │
   │  search :8307                                            │
   └─────────────────────────────────────────────────────────┘
```

> **端口冲突（仅本地开发）：** `scm-file` 与 `scm-product` 同为 8201；
> `scm-message` 与 `scm-approval` 同为 8209。同一时间只跑其中一个，或通过
> `server.port` 覆盖。

## 模块

> **8201–8212** 端口区间为供应链核心，**8301–8307** 区间为电商业务层。
> `scm-tenant` 通过动态数据源路由，不绑定公开端口。

| 模块 | 端口 | 描述 |
|--------|------|-------------|
| `scm-web` | 3000 | 前端 — Next.js 15、Ant Design 5、Zustand、TanStack Query |
| `scm-gateway` | 8761 | API 网关 — 路由、限流、横切关注点 |
| `scm-auth` | 8106 | 认证服务 — OAuth2、JWT、WebAuthn 无密码登录 |
| `scm-system` | 8081 | 系统管理 — 用户、角色、权限、部门 |
| `scm-file` | 8201 ⚠ | 文件服务 — 上传、存储抽象（S3/MinIO/本地）、OCR |
| `scm-product` | 8201 ⚠ | 商品中心 — SPU/SKU、分类、品牌、属性 |
| `scm-inventory` | 8202 | 库存 — 实时库存、预占、预警、快照 |
| `scm-order` | 8203 | 订单 — 生命周期状态机、支付、退款 |
| `scm-warehouse` | 8204 | 仓储 — 入出库、波次拣货、库位管理 |
| `scm-logistics` | 8205 | 物流 — 承运商、运单、轨迹、路径优化 |
| `scm-supplier` | 8206 | 供应商 — 入驻、评估、结算 |
| `scm-purchase` | 8207 | 采购 — RFQ、报价、合同、采购单 |
| `scm-finance` | 8208 | 财务 — 结算、发票、运费规则、对账 |
| `scm-message` | 8209 ⚠ | 消息通道 — 短信 / 邮件 / 推送，分发与渠道适配 |
| `scm-approval` | 8209 ⚠ | 审批流 — 可配置的审批流程 |
| `scm-audit` | 8210 | 审计 — 操作日志、敏感操作追踪 |
| `scm-notify` | 8211 | 站内通知 — 模板、多渠道投递 |
| `scm-tenant` | 8212 | 多租户 — 租户生命周期、套餐、特性开关 |
| `scm-mall` | 8301 | 商城 — 电商前台、商品展示、购物车 |
| `scm-member` | 8302 | 会员 — 资料、地址、积分、忠诚度 |
| `scm-promotion` | 8303 | 营销 — 活动、优惠券、折扣、秒杀 |
| `scm-payment` | 8304 | 支付 — 支付处理、退款、对账 |
| `scm-order-center` | 8305 | 订单中心 — 集中式订单编排 |
| `scm-fulfillment` | 8306 | 履约 — 订单履约、出库、配送跟踪 |
| `scm-search` | 8307 | 搜索 — Elasticsearch 全文检索 |
| `scm-common` | — | 公共库 — core、data、data-rw、cache、web、monitoring、integration、decision-matrix、decision-engine、security |

> **端口冲突：** `scm-file` ↔ `scm-product`（8201）与
> `scm-message` ↔ `scm-approval`（8209）。本地同时运行两两之一时需通过
> `server.port` 覆盖。决策引擎（`scm-common/decision-engine`、
> `scm-common/decision-matrix`）是公共库，并非独立部署的服务。

## 快速开始

### 环境依赖

- **JDK 21**（依赖虚拟线程）
- **Maven 3.8+**
- **Node.js 20+**（前端）
- **Docker & Docker Compose**

所有组件都提供本地开发默认值，无需外部账号或云凭证。可选配置见
[环境变量](#环境变量)。

### 环境变量

每个变量都有本地开发默认值，开箱即用。通过 shell 或 `.env` 文件（**勿提交**）覆盖：

| 变量 | 默认值 | 说明 |
|----------|---------|-------------|
| `JWT_SECRET` | dev 占位符 | JWT 签名密钥 — **必须 ≥ 512 位（64 字节）**；生产环境必填 |
| `NACOS_SERVER` | `localhost:8848` | Nacos 配置/注册中心地址 |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | `nacos` / `nacos` | Nacos 认证凭据 |
| `DB_HOST` / `DB_PORT` | `localhost` / `5432` | PostgreSQL 主机 / 端口 |
| `DB_USERNAME` / `DB_PASSWORD` | `admin` / `changeme` | PostgreSQL 凭据 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis 主机 / 端口 |
| `REDIS_PASSWORD` | `changeme` | Redis 密码 |
| `SEATA_SERVER_ADDR` | `127.0.0.1:8091` | Seata 服务地址 |

> **安全提示：** 仓库中签入的 `docker-compose.yml`、Helm `values*.yaml` 与
> `deploy/k8s/secrets.yml` 仅含**占位符**凭据。生产部署前需通过环境变量或密钥管理
> 系统设置真实值。

### 1. 启动基础设施

```bash
docker-compose up -d
```

### 2. 初始化数据库

```bash
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat

# Linux/Mac
export PGPASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh
```

### 3. 构建

```bash
# 全量构建
mvn clean install -DskipTests -f com.scm.parent/pom.xml

# 单模块构建
mvn clean package -pl scm-order/service -am -f com.scm.parent/pom.xml
```

### 4. 启动服务

> **顺序：** 基础设施 → 网关 → 认证 → 系统 → 业务服务

```bash
# 网关
cd scm-gateway && mvn spring-boot:run

# 认证（HTTPS）
cd scm-auth && mvn spring-boot:run

# 系统
cd scm-system/service && mvn spring-boot:run

# 业务服务（任意顺序）
cd scm-product/service && mvn spring-boot:run
cd scm-inventory/service && mvn spring-boot:run
cd scm-order/service && mvn spring-boot:run
```

### 5. 访问入口

| 服务 | URL |
|---------|-----|
| API 网关 | http://localhost:8761 |
| Nacos 控制台 | http://localhost:8848/nacos |
| Sentinel 控制台 | http://localhost:8858 |
| XXL-Job Admin | http://localhost:8088/xxl-job-admin |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

### 6. 前端

```bash
cd scm-web
npm install
npm run dev
```

前端默认运行在 **http://localhost:3000**，内置 zh-CN 与 en-US 国际化。

| 路由 | 页面 |
|-------|------|
| `/login` | 登录与认证 |
| `/dashboard` | 数据看板 |
| `/product` | 商品目录管理 |
| `/order` | 订单生命周期管理 |
| `/inventory` | 库存与预警 |
| `/system/*` | 系统设置（用户、角色、权限） |

## Kubernetes 部署

`deploy/k8s/` 下的清单当前覆盖**基础设施与供应链核心层**——电商业务层服务
（`scm-mall`、`scm-member`、`scm-promotion`、`scm-payment`、`scm-order-center`、
`scm-fulfillment`、`scm-search`）仍以 Docker Compose 本地运行，K8s 清单需
后续补齐。

```bash
# 应用全部 K8s 资源
kubectl apply -f deploy/k8s/namespace.yml
kubectl apply -f deploy/k8s/configmap.yml
kubectl apply -f deploy/k8s/secrets.yml
kubectl apply -f deploy/k8s/scm-*-deployment.yml
kubectl apply -f deploy/k8s/scm-*-service.yml

# 灰度（仅 auth）
kubectl apply -f deploy/k8s/scm-auth-canary-deployment.yml
kubectl apply -f deploy/k8s/scm-auth-canary-service.yml

# 或使用 Helm
helm install scm-platform deploy/helm/scm-platform/

# ArgoCD GitOps
kubectl apply -f deploy/argocd/application.yaml
```

### 已具备 K8s 清单的服务

| 服务 | 端口 | 备注 |
|---------|------|-------|
| scm-gateway | 8761 | 已启用 HPA |
| scm-system | 8081 | |
| scm-auth | 8106 | 另有灰度部署 |
| scm-product | 8201 ⚠ | 与 `scm-file` 共用 8201 |
| scm-inventory | 8202 | |
| scm-warehouse | 8204 | |
| scm-logistics | 8205 | |
| scm-supplier | 8206 | |
| scm-purchase | 8207 | |
| scm-finance | 8208 | |

## 压测

```bash
# 运行 k6 压测脚本
k6 run scripts/loadtest/order-flow.js
k6 run scripts/loadtest/inventory-check.js
```

## 关键架构模式

### 决策引擎（电商决策矩阵）

```java
// 多准则加权融合决策引擎
WeightedFusionEngine engine = new WeightedFusionEngine();
DecisionResult result = engine.evaluate(candidates, criteria, weights);

// A/B 测试支持
if (experiment.isTreatmentGroup(userId)) {
    return treatmentEngine.evaluate(candidates);
}
```

### 多租户数据路由

```java
@DS("user")  // 路由到 db_user
public class UserMapper extends BaseMapper<SysUser> { }
```

### 读写分离

```java
@Slave  // 自动路由到读副本
public List<Order> queryOrders(OrderQuery query) { }

@Master  // 强制写入主库
public void updateOrder(Order order) { }
```

### 原子库存扣减（Redis Lua）

```java
String script = """
    local stock = redis.call('GET', KEYS[1])
    if tonumber(stock) >= tonumber(ARGV[1]) then
        redis.call('DECRBY', KEYS[1], ARGV[1])
        return 1
    end
    return 0
    """;
```

### 分布式事务（Seata AT）

```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public Order createOrder(OrderDTO dto) {
    Order order = orderMapper.insert(new Order(dto));
    inventoryClient.deductStock(dto.getSkuId(), dto.getQuantity());
    paymentClient.createPayment(order.getId(), dto.getAmount());
    return order;
}
```

## 基础设施

| 组件 | 用途 | 配置 |
|-----------|---------|--------|
| **PgBouncer** | 连接池 | `deploy/pgbouncer/` |
| **Redis Sentinel** | 缓存高可用 | `deploy/redis/sentinel.conf` |
| **PostgreSQL HA** | 数据库高可用（Patroni） | `deploy/postgresql/ha/` |
| **Kafka** | 事件流 | Docker Compose |
| **Canal** | Binlog 同步到 Elasticsearch | `docker-compose-debezium.yml` |

## 项目结构

```
scm-platform/
├── com.scm.parent/          # 父 POM（依赖管理）— 构建时始终通过 -f 指定
├── scm-web/                 # 前端（Next.js 15、React 19、Ant Design 5）— 独立 npm 工程
├── scm-common/              # 公共库（无 Spring Boot 启动类）
│   ├── core/                # 工具类、异常、租户上下文
│   ├── data/                # MyBatis-Plus 集成、动态数据源路由
│   ├── data-rw/             # @Master / @Slave 读写分离
│   ├── data-rw-stub/        # 测试用存根实现
│   ├── cache/               # Caffeine + Redis、Lua 脚本中心、幂等、分布式锁
│   ├── web/                 # Web 过滤器、REST 客户端、安全配置
│   ├── monitoring/          # Sentinel 熔断
│   ├── integration/         # Kafka / RabbitMQ 消息（JacksonJsonSerializer）
│   ├── decision-matrix/     # 决策引擎核心类型
│   ├── decision-engine/     # 加权融合引擎、A/B 测试
│   └── security/            # 安全核心 + API
├── scm-gateway/             # API 网关（Spring Cloud Gateway，单模块）
├── scm-auth/                # 认证（OAuth2、JWT、WebAuthn，单模块）
├── scm-system/              # 系统管理（用户、角色、权限）
├── scm-approval/            # 审批流
├── scm-audit/               # 审计日志
├── scm-notify/              # 站内通知
├── scm-tenant/              # 多租户管理
├── scm-product/             # 商品中心（api + service）
├── scm-inventory/           # 库存（api + service，Redis Lua 热路径）
├── scm-order/               # 订单处理（api + service，状态机）
├── scm-warehouse/           # 仓储作业
├── scm-logistics/           # 物流轨迹
├── scm-purchase/            # 采购
├── scm-supplier/            # 供应商管理
├── scm-finance/             # 财务结算
├── scm-file/                # 文件服务（上传、存储抽象、OCR）
├── scm-message/             # 短信 / 邮件 / 推送分发
├── scm-member/              # 会员管理
├── scm-promotion/           # 营销（活动、优惠券、折扣）
├── scm-payment/             # 支付处理
├── scm-search/              # Elasticsearch 全文搜索
├── scm-order-center/        # 集中式订单编排
├── scm-mall/                # 电商前台
├── scm-fulfillment/         # 订单履约与配送
├── deploy/                  # K8s 清单、Helm、ArgoCD、PgBouncer、Redis、Istio、混沌工程
├── scripts/                 # 数据库初始化、分区管理、压测、保留策略、质量检查
└── docs/                    # 架构、运维手册、设计规范、审计文档
```

> 业务服务（`scm-{name}/`）采用 **api / service 拆分**模式 —— `api/` 模块承载
> Dubbo RPC 接口与 DTO，`service/` 模块为可部署的 Spring Boot 工程。
> `scm-gateway`、`scm-auth`、`scm-system` 以及 `scm-common` 下的子模块为单模块结构。

## 数据库管理

```bash
# 初始化全部数据库
export PGPASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh

# 创建未来 3 个月的分区
./scripts/db/partition/create-partitions.sh

# 删除过期分区（默认保留 24 个月）
./scripts/db/partition/drop-old-partitions.sh

# 应用数据保留策略
./scripts/db/retention/apply-retention.sh

# 运行数据质量检查
./scripts/db/quality/check-quality.sh
```

## 测试

```bash
# 单元测试
mvn test -pl scm-order/service -f com.scm.parent/pom.xml

# 单测试方法
mvn test -Dtest=OrderServiceTest -pl scm-order/service -f com.scm.parent/pom.xml

# 完整 CI 检查（测试 + JaCoCo 覆盖率）
mvn verify -f com.scm.parent/pom.xml

# 压测（需安装 k6）
k6 run scripts/loadtest/order-flow.js

# E2E 测试（需安装 Playwright）
cd scm-web && npx playwright test
```

## 贡献指南

欢迎贡献代码！提交 PR 前请阅读 [贡献指南](./CONTRIBUTING.md)。

1. Fork 本仓库
2. 创建特性分支（`git checkout -b feature/amazing-feature`）
3. 提交变更（`git commit -m 'feat: add amazing feature'`）
4. 推送到分支（`git push origin feature/amazing-feature`）
5. 发起 Pull Request

请同时阅读 [行为准则](./CODE_OF_CONDUCT.md)。所有重要变更记录在
[CHANGELOG.md](./CHANGELOG.md) 中。

## 许可证

Apache License 2.0 — 详见 [LICENSE](./LICENSE)。

---

<div align="center">

**为现代企业供应链管理而构建**

</div>