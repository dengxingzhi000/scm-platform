<div align="center">

**English** · [简体中文](./README.zh-CN.md)

# SCM Platform

### 面向高并发电商场景的云原生供应链中台。

**26 个后端服务。一台笔记本。真实的分布式事务、Redis 热路径库存，
以及一套 Next.js 控制台。**

[![Build](https://img.shields.io/github/actions/workflow/status/dengxingzhi000/scm-platform/maven-build.yml?branch=master&style=flat-square&logo=github&label=build)](https://github.com/dengxingzhi000/scm-platform/actions)
[![Stars](https://img.shields.io/github/stars/dengxingzhi000/scm-platform?color=ffcb47&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/stargazers)
[![Forks](https://img.shields.io/github/forks/dengxingzhi000/scm-platform?color=8ae8ff&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/network/members)
[![Issues](https://img.shields.io/github/issues/dengxingzhi000/scm-platform?color=ff80eb&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/issues)
[![Services](https://img.shields.io/badge/services-26-blue?style=flat-square)](#模块)
[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white&style=flat-square)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white&style=flat-square)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)](./LICENSE)

**[文档](#文档) · [架构](#架构) · [覆盖场景](#平台覆盖场景) · [指南](#文档)**

[快速开始](#快速开始) · [项目亮点](#项目亮点) · [技术栈](#技术栈) · [架构](#架构) · [覆盖场景](#平台覆盖场景) · [模块](#模块) · [架构模式](#关键架构模式) · [部署](#kubernetes-部署) · [运维](#运维) · [FAQ](#faq) · [文档](#文档)

<code>docker-compose up -d && PGPASSWORD=admin123 ./scripts/db/init-all-databases.sh</code>

</div>

> [!IMPORTANT]
> **默认本地启动存在三处端口冲突。** `scm-file` ↔ `scm-product` 都占用 8201，
> `scm-message` ↔ `scm-approval` 都占用 8209，`docker-compose.yml` 中 Grafana
> 映射 `3000:3000`，与 Next.js 开发服务器端口相同。对不需要的服务通过
> `server.port` 覆盖即可；若想同时使用看板与前端，请重新映射 Grafana 端口。

---

做供应链软件意味着要在无法共享事务的系统之间协调库存、订单、供应商、仓储、
承运商与资金。而在秒杀流量下把这些做对、并证明它是对的，是其中最贵的部分。

SCM Platform 是一份可运行的参考实现。API 网关背后是 26 个可独立部署的后端服务，
真正扛得住生产流量的模式已经接好：Redis Lua 原子扣减库存、Seata AT 保证跨服务一致性、
`@Master`/`@Slave` 读写分离、枚举驱动的订单状态流转、按租户路由的数据源，
以及把跨库读从写路径上剥离出去的 CQRS 拆分。

全部可在本地跑起来。`docker-compose up -d`、初始化数据库、构建，前端即可在 3000 端口访问，
无需任何云账号或付费依赖。

## 快速开始

> [!NOTE]
> **启动顺序有讲究。** 基础设施 → 网关 → 认证 → 系统 → 业务服务。Nacos 未就绪前
> 网关无法正确路由；`scm-system` 未启动前，业务服务解析不到权限相关的 Dubbo 调用。

### 环境依赖

- **JDK 21** —— 热路径依赖虚拟线程
- **Maven 3.8+**
- **Node.js 20+**（前端）
- **Docker & Docker Compose**

每个组件都提供可用的本地默认值，无需外部账号、云凭证或付费服务。可选覆盖项见
[环境变量](#环境变量)。

### 1. 启动基础设施

```bash
docker-compose up -d
```

### 2. 初始化数据库

```bash
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat

# Linux/Mac
export PGPASSWORD=admin123 && ./scripts/db/init-all-databases.sh
```

### 3. 构建

```bash
# 全量构建
mvn clean install -DskipTests -f com.scm.parent/pom.xml

# 单模块构建
mvn clean package -pl scm-order/service -am -f com.scm.parent/pom.xml
```

> [!NOTE]
> **仓库根目录没有 `pom.xml`。** 每条 Maven 命令都需要 `-f com.scm.parent/pom.xml`，
> 父 POM 通过 `../scm-*` 相对路径引用各模块。漏掉这个参数是这里最常见的构建错误。

### 4. 启动服务

```bash
# 网关（必须最先启动）
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

### 5. 前端

```bash
cd scm-web
npm install
npm run dev
```

前端默认运行在 **http://localhost:3000**，内置 zh-CN 与 en-US 国际化。

| 路由         | 页面                         |
| ------------ | ---------------------------- |
| `/login`     | 登录与认证                   |
| `/dashboard` | 数据看板                     |
| `/product`   | 商品目录管理                 |
| `/order`     | 订单生命周期管理             |
| `/inventory` | 库存与预警                   |
| `/system/*`  | 系统设置（用户、角色、权限） |

### 访问入口

| 服务            | URL                                 |
| --------------- | ----------------------------------- |
| 前端            | http://localhost:3000               |
| API 网关        | http://localhost:8761               |
| Nacos 控制台    | http://localhost:8848/nacos         |
| Sentinel 控制台 | http://localhost:8858               |
| XXL-Job Admin   | http://localhost:8088/xxl-job-admin |
| Prometheus      | http://localhost:9090               |
| Grafana         | http://localhost:3000 ⚠ 与前端冲突  |

### 环境变量

每个变量都有本地开发默认值，开箱即用。通过 shell 或 `.env` 文件（**勿提交**）覆盖：

| 变量                                | 默认值               | 说明                                                       |
| ----------------------------------- | -------------------- | ---------------------------------------------------------- |
| `JWT_SECRET`                        | dev 占位符           | JWT 签名密钥 —— **必须 ≥ 512 位（64 字节）**；生产环境必填 |
| `NACOS_SERVER`                      | `localhost:8848`     | Nacos 配置/注册中心地址                                    |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | `nacos` / `nacos`    | Nacos 认证凭据                                             |
| `DB_HOST` / `DB_PORT`               | `localhost` / `5432` | PostgreSQL 主机 / 端口                                     |
| `DB_USERNAME` / `DB_PASSWORD`       | `admin` / `changeme` | PostgreSQL 凭据                                            |
| `REDIS_HOST` / `REDIS_PORT`         | `localhost` / `6379` | Redis 主机 / 端口                                          |
| `REDIS_PASSWORD`                    | `changeme`           | Redis 密码                                                 |
| `SEATA_SERVER_ADDR`                 | `127.0.0.1:8091`     | Seata 服务地址                                             |

> [!WARNING]
> **签入仓库的凭据均为占位符。** `docker-compose.yml`、Helm `values*.yaml`、
> `deploy/k8s/secrets.yml` 中都是 `changeme` 一类的默认值。除笔记本之外的任何部署，
> 都必须先通过环境变量或密钥管理系统设置真实值。另请注意，数据库初始化脚本使用的默认
> 凭据（`admin` / `admin123`）与 `.env.example` 中的 `changeme` 占位符并不一致 —— 以脚本为准。

## 项目亮点

| 图标 | 特性             | 说明                                                                      |
| ---- | ---------------- | ------------------------------------------------------------------------- |
| 🏗️   | **微服务架构**   | 26 个可独立部署服务，含 API 网关、服务发现与配置中心                      |
| 🔐   | **企业级安全**   | OAuth2 + JWT + WebAuthn 无密码登录，RBAC 细粒度数据权限控制               |
| 💰   | **分布式事务**   | Seata AT/TCC/Saga 多种模式保障跨服务数据一致性                            |
| ⚡   | **高性能**       | Redis Lua 原子库存扣减、读写分离、分区表                                  |
| 🔍   | **全文搜索**     | Elasticsearch 检索能力，通过 Canal 监听 Binlog 实现近实时同步             |
| 🧠   | **决策引擎**     | 电商决策矩阵，支持智能定价、库存预测、A/B 测试                            |
| 📊   | **可观测性**     | Sentinel 熔断、SkyWalking 链路追踪、Prometheus 指标、Grafana 看板         |
| 🏢   | **多租户**       | 租户隔离、动态数据源路由、可配置特性开关                                  |
| 🖥️   | **现代化前端**   | Next.js 15 App Router、Ant Design 5 Pro、Zustand 状态管理、TanStack Query |
| 📦   | **领域驱动设计** | 清晰的 CQRS 分离、聚合根、通过 Kafka/RabbitMQ 传递领域事件                |
| ☸️   | **云原生**       | 核心服务的 Kubernetes 清单、Helm Chart、ArgoCD GitOps、灰度发布（auth）   |

## 技术栈

| 层级        | 组件                                                                                              |
| ----------- | ------------------------------------------------------------------------------------------------- |
| **后端**    | Java 21（虚拟线程）、Spring Boot 4.0.6、Spring Cloud 2025.1.2                                     |
| **Alibaba** | Spring Cloud Alibaba 2025.1.0.0（Nacos、Sentinel、Seata）                                         |
| **数据库**  | PostgreSQL、MyBatis-Plus 3.5.15、ShardingSphere 5.5.1                                             |
| **缓存**    | Redis（分布式缓存，Lua 脚本实现原子操作）                                                         |
| **搜索**    | Elasticsearch 8.11.4、Canal 1.1.7（Binlog 同步）                                                  |
| **消息**    | Kafka（事件流）、RabbitMQ（可靠队列）                                                             |
| **RPC**     | Apache Dubbo 3.3.6（内部服务调用）                                                                |
| **调度**    | XXL-Job 3.3.1（分布式任务调度）                                                                   |
| **监控**    | Sentinel、SkyWalking 9.3.0、Micrometer + Prometheus                                               |
| **前端**    | Next.js 15（App Router）、React 19、Ant Design 5、Zustand 5、TanStack Query 5、ECharts、next-intl |
| **DevOps**  | Docker、Kubernetes、Helm、ArgoCD、GitHub Actions、SonarCloud、OWASP                               |
| **测试**    | JUnit 5、k6 压测、Pact 契约测试、Playwright E2E、PIT 变异测试                                     |

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
   │  audit :8210 · notify :8211 · tenant :8212 · document :8213│
   └────────┬───────────────────────────────────────────────┘
             │
   ┌────────▼───────────────────────────────────────────────┐
   │  电商业务层                                              │
   │  mall :8301 · member :8302 · promotion :8303             │
   │  payment :8304 · order-center :8305 · fulfillment :8306  │
   │  search :8307 · analytics :8308                          │
   └─────────────────────────────────────────────────────────┘
```

每个服务独占自己的 PostgreSQL 数据库（`db_order`、`db_inventory`、`db_product` 等）。
代码库中不存在任何跨库 JOIN —— 跨库读取一律走各实体的
`*CrossDatabaseQueryService` CQRS 类，跨库写入则由 `@GlobalTransactional` 包裹，
交由 Seata 回滚。

## 平台覆盖场景

下表所列场景均由对应服务实现。右列给出的是真正约束设计的因素，而非仅仅列出配置。

| 场景         | 平台提供的能力                                            | 涉及服务与约束                                                                            |
| ------------ | --------------------------------------------------------- | ----------------------------------------------------------------------------------------- |
| 秒杀下单     | 活动定价、库存预占与 900 秒自动释放、高并发下的原子扣减   | `promotion` 8303、`inventory` 8202、`order` 8203 —— 热路径只走 Redis Lua，不查 PostgreSQL |
| 订单生命周期 | 枚举驱动的状态机、事件溯源的状态历史、按请求 ID 幂等创建  | `order` 8203 —— 流转由 `OrderStatus.canTransitionTo()` 强制校验                           |
| 分布式一致性 | 订单 → 库存 → 支付跨库单事务，失败时补偿回滚              | `@GlobalTransactional`；每个数据库都需要 `undo_log` 表                                    |
| 多租户 SaaS  | 按上下文动态路由数据源、租户级特性开关、数据行按租户隔离  | `@DS("user" \| "org" \| …)`、`TenantContextHolder`；所有表必须有 `tenant_id`，由 CI 强制  |
| 采购到收货   | RFQ → 报价 → 合同 → 采购单 → 到货接收                     | `purchase` 8207、`supplier` 8206、`warehouse` 8204                                        |
| 仓储作业     | 出入库、波次拣货、库位管理                                | `warehouse` 8204                                                                          |
| 物流跟踪     | 运单创建、承运商接入、路径优化、配送跟踪                  | `logistics` 8205、`fulfillment` 8306                                                      |
| 支付与结算   | 支付单、退款、对账、开票                                  | `payment` 8304、`finance` 8208                                                            |
| 审批与审计   | 可配置审批链、操作日志、敏感操作追踪                      | `approval` 8209、`audit` 8210                                                             |
| 商品检索     | 由 Binlog 而非应用写入保持同步的 Elasticsearch 全文检索   | `search` 8307、Canal → Elasticsearch                                                      |
| 数据分析     | ClickHouse OLAP + PostgreSQL 元数据 + Kafka ODS 接入      | `analytics` 8308 —— ClickHouse 8123                                                       |
| 身份与权限   | OAuth2 + JWT + WebAuthn 无密码登录、带行级数据范围的 RBAC | `auth` 8106、`system` 8081                                                                |
| 文档生命周期 | DOCX 模板渲染并留审计痕迹，字节存储代理至文件服务         | `document` 8213 → `file` 8201                                                             |

> [!NOTE]
> 决策引擎（`scm-common/decision-engine`、`scm-common/decision-matrix`）是一个
> **公共库**，不是独立部署的服务。它由 `promotion`、`order`、`analytics` 引用，
> 自身不监听端口。

## 模块

> [!NOTE]
> **8201–8213** 端口区间为供应链核心，**8301–8308** 区间为电商业务层。
> `scm-tenant` 通过动态数据源路由，不绑定公开端口。业务服务采用 **api / service 拆分**
> —— `api/` 承载 Dubbo RPC 接口与 DTO，`service/` 为可部署的 Spring Boot 工程。
> `scm-gateway`、`scm-auth`、`scm-system` 以及 `scm-common` 下的子模块为单模块结构。

| 模块               | 端口   | 描述                                                                                     |
| ------------------ | ------ | ---------------------------------------------------------------------------------------- |
| `scm-web`          | 3000 ⚠ | 前端 —— Next.js 15、Ant Design 5、Zustand、TanStack Query                                |
| `scm-gateway`      | 8761   | API 网关 —— 路由、限流、横切关注点                                                       |
| `scm-auth`         | 8106   | 认证服务 —— OAuth2、JWT、WebAuthn 无密码登录                                             |
| `scm-system`       | 8081   | 系统管理 —— 用户、角色、权限、部门                                                       |
| `scm-file`         | 8201 ⚠ | 文件服务 —— 上传、存储抽象（S3/MinIO/本地）、病毒扫描                                    |
| `scm-product`      | 8201 ⚠ | 商品中心 —— SPU/SKU、分类、品牌、属性                                                    |
| `scm-inventory`    | 8202   | 库存 —— 实时库存、预占、预警、快照                                                       |
| `scm-order`        | 8203   | 订单 —— 生命周期状态机、支付、退款                                                       |
| `scm-warehouse`    | 8204   | 仓储 —— 出入库、波次拣货、库位管理                                                       |
| `scm-logistics`    | 8205   | 物流 —— 承运商、运单、轨迹、路径优化                                                     |
| `scm-supplier`     | 8206   | 供应商 —— 入驻、评估、结算                                                               |
| `scm-purchase`     | 8207   | 采购 —— RFQ、报价、合同、采购单                                                          |
| `scm-finance`      | 8208   | 财务 —— 结算、发票、运费规则、对账                                                       |
| `scm-message`      | 8209 ⚠ | 消息通道 —— 短信 / 邮件 / 推送，分发与渠道适配                                           |
| `scm-approval`     | 8209 ⚠ | 审批流 —— 可配置的审批流程                                                               |
| `scm-audit`        | 8210   | 审计 —— 操作日志、敏感操作追踪                                                           |
| `scm-notify`       | 8211   | 站内通知 —— 模板、多渠道投递                                                             |
| `scm-tenant`       | —      | 多租户 —— 租户生命周期、套餐、特性开关                                                   |
| `scm-document`     | 8213   | 文档生命周期 —— DOCX 渲染 + 审计追踪，代理 `scm-file` 提供字节传输                       |
| `scm-mall`         | 8301   | 商城 —— 电商前台、商品展示、购物车                                                       |
| `scm-member`       | 8302   | 会员 —— 资料、地址、积分、忠诚度                                                         |
| `scm-promotion`    | 8303   | 营销 —— 活动、优惠券、折扣、秒杀                                                         |
| `scm-payment`      | 8304   | 支付 —— 支付处理、退款、对账                                                             |
| `scm-order-center` | 8305   | 订单中心 —— 集中式订单编排                                                               |
| `scm-fulfillment`  | 8306   | 履约 —— 订单履约、出库、配送跟踪                                                         |
| `scm-search`       | 8307   | 搜索 —— Elasticsearch 全文检索                                                           |
| `scm-analytics`    | 8308   | 数据分析 —— ClickHouse OLAP + PostgreSQL 元数据、Kafka ODS 接入                          |
| `scm-common`       | —      | 公共库 —— core、data、data-rw、cache、web、monitoring、integration、decision-*、security |

### 项目结构

```
scm-platform/
├── com.scm.parent/          # 父 POM（依赖管理）—— 构建时始终通过 -f 指定
├── scm-web/                 # 前端（Next.js 15、React 19、Ant Design 5）—— 独立 npm 工程
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
├── scm-document/            # 文档生命周期与 DOCX 模板（v1.3.0）
├── scm-message/             # 短信 / 邮件 / 推送分发
├── scm-member/              # 会员管理
├── scm-promotion/           # 营销（活动、优惠券、折扣）
├── scm-payment/             # 支付处理
├── scm-search/              # Elasticsearch 全文搜索
├── scm-order-center/        # 集中式订单编排
├── scm-mall/                # 电商前台
├── scm-fulfillment/         # 订单履约与配送
├── scm-analytics/           # ClickHouse OLAP + PG 元数据
├── deploy/                  # K8s 清单、Helm、ArgoCD、PgBouncer、Redis、Istio、混沌工程
├── scripts/                 # 数据库初始化、分区管理、压测、保留策略、质量检查
└── docs/                    # 架构、运维手册、设计规范、审计文档
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

### 订单状态流转

```java
public enum OrderStatus {
    PENDING_PAYMENT, PAID, PENDING_SHIP, SHIPPED,
    IN_TRANSIT, DELIVERED, COMPLETED, CANCELLED, REFUNDING, REFUNDED;

    public Set<OrderStatus> validNextStatuses() {
        return switch (this) {
            case PENDING_PAYMENT -> Set.of(PAID, CANCELLED);
            case PAID           -> Set.of(PENDING_SHIP, CANCELLED, REFUNDING);
            case COMPLETED      -> Set.of();  // 终态
            // …
        };
    }
}
```

流转表是「订单下一步能做什么」的唯一事实来源，且在聚合内部而非 service 方法中校验 ——
非法流转会在写入发生之前直接抛异常。

## Kubernetes 部署

> [!WARNING]
> **电商业务层目前仅有 Docker Compose。** `deploy/k8s/` 下的清单覆盖基础设施与供应链
> 核心层。`scm-mall`、`scm-member`、`scm-promotion`、`scm-payment`、
> `scm-order-center`、`scm-fulfillment`、`scm-search`、`scm-analytics` 与
> `scm-message` 需要补齐清单后才能部署到集群。

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

| 服务            | 端口   | 备注                    |
| --------------- | ------ | ----------------------- |
| `scm-gateway`   | 8761   | 已启用 HPA              |
| `scm-system`    | 8081   |                         |
| `scm-auth`      | 8106   | 另有灰度部署            |
| `scm-product`   | 8201 ⚠ | 与 `scm-file` 共用 8201 |
| `scm-inventory` | 8202   |                         |
| `scm-warehouse` | 8204   |                         |
| `scm-logistics` | 8205   |                         |
| `scm-supplier`  | 8206   |                         |
| `scm-purchase`  | 8207   |                         |
| `scm-finance`   | 8208   |                         |
| `scm-document`  | 8213   | v1.3.0 新增             |

## 运维

### 基础设施

| 组件               | 用途                        | 配置                          |
| ------------------ | --------------------------- | ----------------------------- |
| **PgBouncer**      | 连接池                      | `deploy/pgbouncer/`           |
| **Redis Sentinel** | 缓存高可用                  | `deploy/redis/sentinel.conf`  |
| **PostgreSQL HA**  | 数据库高可用（Patroni）     | `deploy/postgresql/ha/`       |
| **Kafka**          | 事件流                      | Docker Compose                |
| **Canal**          | Binlog 同步到 Elasticsearch | `docker-compose-debezium.yml` |

### 数据库管理

```bash
# 初始化全部数据库
export PGPASSWORD=admin123 && ./scripts/db/init-all-databases.sh

# 创建未来 3 个月的分区
./scripts/db/partition/create-partitions.sh

# 删除过期分区（默认保留 24 个月）
./scripts/db/partition/drop-old-partitions.sh

# 应用数据保留策略
./scripts/db/retention/apply-retention.sh

# 运行数据质量检查
./scripts/db/quality/check-quality.sh
```

> [!NOTE]
> **分区表不能使用普通的 UNIQUE 约束。** PostgreSQL 要求每个唯一约束都包含分区键 ——
> `ord_order(order_no, create_time)`、`inv_reservation(reservation_no, reserved_at)`、
> `sup_purchase_order(purchase_no, create_time)`。跨分区的唯一性由应用层借助 Redis 保证。
> 另注意 `inv_reservation` 位于 `db_inventory`，`sup_purchase_order` 位于 `db_supplier`，
> 并非都在 `db_order`。

### 测试

```bash
# 单元测试
mvn test -pl scm-order/service -f com.scm.parent/pom.xml

# 单测试方法
mvn test -Dtest=OrderServiceTest -pl scm-order/service -f com.scm.parent/pom.xml

# 完整 CI 检查（测试 + JaCoCo 覆盖率门槛）
mvn verify -Djacoco.skip=false -f com.scm.parent/pom.xml

# 压测（需安装 k6）
k6 run scripts/loadtest/order-flow.js
k6 run scripts/loadtest/inventory-check.js

# E2E 测试（需安装 Playwright）
cd scm-web && npx playwright test
```

> [!NOTE]
> **覆盖率门槛默认关闭。** 父 POM 中设置了 `jacoco.skip=true`，因此单纯的
> `mvn verify` 并不会强制 70% 行覆盖 / 60% 分支覆盖的阈值。需要强制执行时，
> 请传入 `-Djacoco.skip=false`。

## FAQ

<details>
<summary><strong>为什么每条 Maven 命令都要带 <code>-f com.scm.parent/pom.xml</code>？</strong></summary>

因为本仓库根目录没有 `pom.xml`。`com.scm.parent/pom.xml` 才是父 POM，它通过
`../scm-*` 相对路径声明各模块。在仓库根目录执行 `mvn` 而不带该参数时，
Maven 会去找一个并不存在的 POM。

根目录的 `build.bat` 正是为此固定了 GraalVM JDK 21 与本地 Maven 3.9.16 路径 ——
在 Windows 开发机上，Maven 与 JDK 默认并不在 `PATH` 中。

</details>

<details>
<summary><strong>为什么 <code>scm-file</code> 和 <code>scm-product</code> 都占用 8201？</strong></summary>

这是签入配置中真实存在的冲突，不是文档错误。`scm-message` 与 `scm-approval` 同为 8209，
Grafana 映射 `3000:3000` 而 Next.js 开发服务器也需要 3000。

三处都有办法绕开：对不运行的服务覆盖 `server.port`，或修改 `docker-compose.yml`
中 Grafana 的宿主机端口映射。

</details>

<details>
<summary><strong>直接使用仓库里的凭据跑，安全吗？</strong></summary>

本地开发可以。签入的 `docker-compose.yml`、Helm `values*.yaml` 与
`deploy/k8s/secrets.yml` 中都是**占位符**凭据（`changeme` 之类）。

任何不属于本机的部署，都必须先通过环境变量或密钥管理系统设置真实值。另请注意，
数据库初始化脚本使用的默认凭据是 `admin` / `admin123`，与 `.env.example` 中的
`changeme` 占位符并不相同。

</details>

<details>
<summary><strong>为什么订单状态流转是手写的，而不是用 Spring State Machine？</strong></summary>

Spring State Machine 依赖在父 POM 中已声明但并未使用。流转规则以
`validNextStatuses()` 的形式写在 `OrderStatus` 枚举中，由聚合内部的
`OrdOrder.transitionTo()` 负责校验。

对十个枚举常量使用 switch 表达式，可被检索、无需 Spring 上下文即可单元测试，
并让规则归属于聚合。对其他实体类型，通用路径是 `scm-common/web` 中的
`StatusValidator`，它委托给 `scm-system` 的 `StatusMachineDubboService`。

</details>

<details>
<summary><strong>为什么我的 PR 在测试失败的情况下仍然是绿的？</strong></summary>

这是有意为之。`.github/workflows/maven-build.yml` 在 PR 构建中使用
`-Dmaven.test.failure.ignore=true`，以便变更仍在评审阶段时 CI 保持绿色。
推送到 `master` 与 `develop` 的构建会执行完整的 `mvn verify`，外加一项 schema
校验：任何列出的表若缺少 `tenant_id` 列即失败。

不要依赖 PR 检查来拦截回归 —— 请在本地跑测试。

</details>

<details>
<summary><strong>启动核心服务需要 Elasticsearch、ClickHouse 或 Canal 吗？</strong></summary>

不需要。核心供应链服务依赖 PostgreSQL、Redis 与 Nacos。Elasticsearch 支撑
`scm-search`，ClickHouse 支撑 `scm-analytics`，Canal 负责向 Elasticsearch 投递
Binlog —— 只有运行依赖它们的服务时才需要。

</details>

<details>
<summary><strong>每个服务的数据存在哪里？</strong></summary>

每个服务独占一个独立的 PostgreSQL 数据库 —— `db_order`、`db_inventory`、
`db_product`、`db_user` 等 —— 通过 `@DS` 注解路由，并从
`TenantContextHolder` 解析租户上下文。不存在跨库 JOIN；跨库读取走各实体的
`*CrossDatabaseQueryService` CQRS 类。

所有业务表都必须带 `tenant_id` 列。这一点由 CI
（`scripts/db/ci_validate_tenant_id.sql`）在全部 16 个数据库上强制校验，缺失则构建失败。

</details>

<details>
<summary><strong>服务起不来该怎么排查？</strong></summary>

先确认启动顺序 —— 基础设施、网关、认证、系统，然后才是业务服务。先于 `scm-system`
启动的服务会因 Dubbo 调用无法解析而失败，先于 Nacos 就绪启动的服务则无法注册。

之后确认端口是否真的空闲。考虑到上面三处已知冲突，端口绑定失败是最常见的诱因。

</details>

## 文档

阅读顺序参见 [docs/README.md](./docs/README.md)。按领域查阅：

- [微服务架构](./docs/design/ARCHITECTURE.md) —— 服务拆分、数据库设计思想、技术栈选型
- [CQRS 架构](./docs/design/CQRS_ARCHITECTURE.md) —— 跨库查询优化
- [架构决策记录](./docs/design/ADR.md) —— 重大取舍背后的理由
- [多租户完整指南](./docs/multi-tenant/MULTI_TENANT_GUIDE.md) —— 隔离模型与权限设计
- [开发规范](./docs/development/DEVELOPMENT_STANDARDS.md) —— 编码、配置与 API 约定
- [API 设计](./docs/technical/API_DESIGN.md) 与 [数据库设计](./docs/technical/DATABASE_DESIGN.md)
- [集成指南](./docs/guides/) —— Seata AT、Seata TCC、XXL-Job、Elasticsearch
- [运维手册](./docs/operations/OPERATIONS_MANUAL.md) —— 部署、监控、故障处理
- [应急手册](./docs/operations/runbooks/) —— 发布回滚、数据库故障切换、Redis 故障切换、事件响应
- [性能基准](./docs/project-management/PERFORMANCE_BENCHMARK.md) —— 压测目标与基线
- [快速开始](./QUICK_START.md) —— 上述环境搭建的精简版

安全问题请通过 [SECURITY.md](./SECURITY.md) 反馈。

## Star 历程

<a href="https://www.star-history.com/?repos=dengxingzhi000%2Fscm-platform&amp;type=date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=dengxingzhi000/scm-platform&amp;type=date&amp;theme=dark&amp;legend=top-left" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=dengxingzhi000/scm-platform&amp;type=date" />
    <img alt="SCM Platform GitHub star 历程" src="https://api.star-history.com/chart?repos=dengxingzhi000/scm-platform&amp;type=date" />
  </picture>
</a>

## 关联项目

- **[enterprise-agent](https://github.com/dengxingzhi000/enterprise-agent)** —— 基于本平台的 Python AI 运维 Agent：自研 Agent Runtime（Planner/Executor 循环），覆盖订单故障排查、报销合规审查、销售数据分析，含 Tools/MCP、RAG、Memory、Policy/HITL、Workflow 与 Eval/可观测。

## 贡献指南

欢迎参与！[提交 Issue](https://github.com/dengxingzhi000/scm-platform/issues)
反馈缺陷或提出新功能，或发送 Pull Request 改进代码与文档。

请先阅读 [CONTRIBUTING.md](./CONTRIBUTING.md) —— 其中涵盖构建方式、分支约定，
以及 CI 会检查哪些内容。

1. Fork 本仓库
2. 创建特性分支（`git checkout -b feature/amazing-feature`）
3. 提交变更（`git commit -m 'feat: add amazing feature'`）
4. 推送到分支（`git push origin feature/amazing-feature`）
5. 发起 Pull Request

所有重要变更记录在 [CHANGELOG.md](./CHANGELOG.md) 中，并请遵循
[行为准则](./CODE_OF_CONDUCT.md)。

## 许可证

[Apache License 2.0](./LICENSE)

---

<div align="center">

**为现代企业供应链管理而构建**

</div>
