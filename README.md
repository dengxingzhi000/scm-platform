<div align="center">

**English** · [简体中文](./README.zh-CN.md)

# SCM Platform

### A cloud-native supply chain backbone for high-concurrency commerce.

**Twenty-six backend services. One laptop. Real distributed transactions,
Redis-hot inventory, and a Next.js control tower.**

[![Build](https://img.shields.io/github/actions/workflow/status/dengxingzhi000/scm-platform/maven-build.yml?branch=master&style=flat-square&logo=github&label=build)](https://github.com/dengxingzhi000/scm-platform/actions)
[![Stars](https://img.shields.io/github/stars/dengxingzhi000/scm-platform?color=ffcb47&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/stargazers)
[![Forks](https://img.shields.io/github/forks/dengxingzhi000/scm-platform?color=8ae8ff&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/network/members)
[![Issues](https://img.shields.io/github/issues/dengxingzhi000/scm-platform?color=ff80eb&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/issues)
[![Services](https://img.shields.io/badge/services-26-blue?style=flat-square)](#modules)
[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white&style=flat-square)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white&style=flat-square)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue?style=flat-square)](./LICENSE)

**[Docs](#documentation) · [Architecture](#architecture) · [What it covers](#what-it-covers) · [Guides](#documentation)**

[Quick start](#quick-start) · [Highlights](#highlights) · [Tech stack](#tech-stack) · [Architecture](#architecture) · [What it covers](#what-it-covers) · [Modules](#modules) · [Patterns](#key-architecture-patterns) · [Deploy](#kubernetes-deployment) · [Operations](#operations) · [FAQ](#faq) · [Docs](#documentation)

<code>docker-compose up -d && PGPASSWORD=admin123 ./scripts/db/init-all-databases.sh</code>

</div>

> [!IMPORTANT]
> **Three ports collide in a default local run.** `scm-file` ↔ `scm-product` both
> bind 8201, `scm-message` ↔ `scm-approval` both bind 8209, and Grafana in
> `docker-compose.yml` maps `3000:3000` — the same host port the Next.js dev
> server wants. Override `server.port` for whichever service you don't need, and
> remap Grafana if you want both the dashboards and the frontend.

---

Building supply chain software means coordinating inventory, orders, suppliers,
warehouses, carriers, and money across systems that cannot share a transaction.
Getting that right under flash-sale traffic — and then proving it works — is the
expensive part.

SCM Platform is a working reference implementation. Twenty-six deployable backend
services behind an API gateway, with the patterns that actually carry load in
production already wired in: Redis Lua for atomic stock deduction, Seata AT for
cross-service consistency, `@Master`/`@Slave` read-write separation, enum-driven
order state transitions, per-tenant datasource routing, and a CQRS split that
keeps cross-database reads out of the write path.

Everything runs locally. `docker-compose up -d`, initialize the databases, build,
and the frontend is on port 3000 with no cloud account and no paid dependency.

## Quick start

> [!NOTE]
> **Start order matters.** Infrastructure → Gateway → Auth → System → business
> services. The gateway will not route correctly until Nacos is healthy, and
> business services will fail to resolve `scm-system` for permission checks until
> it is up.

### Prerequisites

- **JDK 21** — virtual threads are used on the hot paths
- **Maven 3.8+**
- **Node.js 20+** for `scm-web`
- **Docker & Docker Compose**

Every component has a working local default. No external accounts, cloud
credentials, or paid services are required. See
[Environment variables](#environment-variables) for the optional overrides.

### 1. Start infrastructure

```bash
docker-compose up -d
```

### 2. Initialize databases

```bash
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat

# Linux/Mac
export PGPASSWORD=admin123 && ./scripts/db/init-all-databases.sh
```

### 3. Build

```bash
# Build all modules
mvn clean install -DskipTests -f com.scm.parent/pom.xml

# Build a single module
mvn clean package -pl scm-order/service -am -f com.scm.parent/pom.xml
```

> [!NOTE]
> **There is no root `pom.xml`.** Every Maven command needs
> `-f com.scm.parent/pom.xml`, which references its modules through `../scm-*`
> paths. Dropping the flag is the single most common build error here.

### 4. Start services

```bash
# Gateway (must be first)
cd scm-gateway && mvn spring-boot:run

# Auth (HTTPS)
cd scm-auth && mvn spring-boot:run

# System
cd scm-system/service && mvn spring-boot:run

# Business services (any order)
cd scm-product/service && mvn spring-boot:run
cd scm-inventory/service && mvn spring-boot:run
cd scm-order/service && mvn spring-boot:run
```

### 5. Frontend

```bash
cd scm-web
npm install
npm run dev
```

The frontend runs at **http://localhost:3000** with built-in zh-CN and en-US
language support.

| Route        | Page                       |
| ------------ | -------------------------- |
| `/login`     | Login & authentication     |
| `/dashboard` | Analytics dashboard        |
| `/product`   | Product catalog management |
| `/order`     | Order lifecycle management |
| `/inventory` | Inventory & stock alerts   |
| `/system/*`  | System settings (RBAC)     |

### Access endpoints

| Service            | URL                                                   |
| ------------------ | ----------------------------------------------------- |
| Frontend           | http://localhost:3000                                 |
| API Gateway        | http://localhost:8761                                 |
| Nacos Console      | http://localhost:8848/nacos                           |
| Sentinel Dashboard | http://localhost:8858                                 |
| XXL-Job Admin      | http://localhost:8088/xxl-job-admin                   |
| Prometheus         | http://localhost:9090                                 |
| Grafana            | http://localhost:3000 ⚠ — conflicts with the frontend |

### Environment variables

Everything defaults to a local-development value, so you can start without
setting anything. Override via your shell or a `.env` file — never commit one:

| Variable                            | Default              | Description                                                                 |
| ----------------------------------- | -------------------- | --------------------------------------------------------------------------- |
| `JWT_SECRET`                        | dev placeholder      | JWT signing key — **must be ≥ 512 bits (64 bytes)**; required in production |
| `NACOS_SERVER`                      | `localhost:8848`     | Nacos config/registry address                                               |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | `nacos` / `nacos`    | Nacos auth credentials                                                      |
| `DB_HOST` / `DB_PORT`               | `localhost` / `5432` | PostgreSQL host / port                                                      |
| `DB_USERNAME` / `DB_PASSWORD`       | `admin` / `changeme` | PostgreSQL credentials                                                      |
| `REDIS_HOST` / `REDIS_PORT`         | `localhost` / `6379` | Redis host / port                                                           |
| `REDIS_PASSWORD`                    | `changeme`           | Redis password                                                              |
| `SEATA_SERVER_ADDR`                 | `127.0.0.1:8091`     | Seata server address                                                        |

> [!WARNING]
> **The checked-in credentials are placeholders.** `docker-compose.yml`, the Helm
> `values*.yaml` files, and `deploy/k8s/secrets.yml` all ship `changeme`-style
> defaults. Set real values through the environment or a secrets manager before
> any deployment that isn't a laptop. Note that the database init scripts use a
> different default (`admin` / `admin123`) than `.env.example` — trust the scripts.

## Highlights

| Icon | Feature                 | Description                                                                                   |
| ---- | ----------------------- | --------------------------------------------------------------------------------------------- |
| 🏗️   | **Microservices**       | 26 deployable services with API Gateway, service discovery, and config management             |
| 🔐   | **Enterprise Security** | OAuth2 + JWT + WebAuthn passwordless auth, RBAC with fine-grained data scope control          |
| 💰   | **Distributed Txn**     | Seata AT/TCC/Saga modes for cross-service data consistency                                    |
| ⚡   | **High Performance**    | Redis Lua atomic stock deduction, read-write separation, partitioned tables                   |
| 🔍   | **Full-Text Search**    | Elasticsearch-powered product search with real-time sync via Canal binlog                     |
| 🧠   | **Decision Engine**     | E-commerce decision matrix with intelligent pricing, inventory prediction, and A/B testing    |
| 📊   | **Observability**       | Sentinel circuit breaking, SkyWalking tracing, Prometheus metrics, Grafana dashboards         |
| 🏢   | **Multi-Tenant**        | Tenant isolation with dynamic data source routing and configurable feature flags              |
| 🖥️   | **Modern Frontend**     | Next.js 15 App Router, Ant Design 5 Pro, Zustand state management, TanStack Query             |
| 📦   | **Domain-Driven**       | Clean CQRS separation, aggregate roots, domain events via Kafka/RabbitMQ                      |
| ☸️   | **Cloud Native**        | Kubernetes manifests for core services, Helm charts, ArgoCD GitOps, canary deployments (auth) |

## Tech stack

| Layer          | Components                                                                                       |
| -------------- | ------------------------------------------------------------------------------------------------ |
| **Backend**    | Java 21 (Virtual Threads), Spring Boot 4.0.6, Spring Cloud 2025.1.2                              |
| **Alibaba**    | Spring Cloud Alibaba 2025.1.0.0 (Nacos, Sentinel, Seata)                                         |
| **Database**   | PostgreSQL, MyBatis-Plus 3.5.15, ShardingSphere 5.5.1                                            |
| **Cache**      | Redis (distributed cache, Lua scripts for atomic operations)                                     |
| **Search**     | Elasticsearch 8.11.4, Canal 1.1.7 (binlog sync)                                                  |
| **Messaging**  | Kafka (event streaming), RabbitMQ (reliable queue)                                               |
| **RPC**        | Apache Dubbo 3.3.6 (internal service calls)                                                      |
| **Scheduling** | XXL-Job 3.3.1 (distributed task scheduling)                                                      |
| **Monitoring** | Sentinel, SkyWalking 9.3.0, Micrometer + Prometheus                                              |
| **Frontend**   | Next.js 15 (App Router), React 19, Ant Design 5, Zustand 5, TanStack Query 5, ECharts, next-intl |
| **DevOps**     | Docker, Kubernetes, Helm, ArgoCD, GitHub Actions, SonarCloud, OWASP                              |
| **Testing**    | JUnit 5, k6 load testing, Pact contract testing, Playwright E2E, PIT mutation                    |

## Architecture

```
   ┌──────────────────┐
   │  Frontend        │
   │  scm-web :3000   │
   └────────┬─────────┘
            │
   ┌────────▼─────────┐
   │  API Gateway     │
   │  scm-gateway     │
   │  :8761           │
   └────────┬─────────┘
            │
   ┌────────▼───────────────────────────────────────────────┐
   │  Supply-chain core — Dubbo RPC over Nacos registry      │
   │  auth :8106 · system :8081 · file :8201⚠ · product :8201⚠│
   │  inventory :8202 · order :8203 · warehouse :8204        │
   │  logistics :8205 · supplier :8206 · purchase :8207      │
   │  finance :8208 · message :8209⚠ · approval :8209⚠       │
   │  audit :8210 · notify :8211 · tenant :8212 · document :8213│
   └────────┬───────────────────────────────────────────────┘
             │
   ┌────────▼───────────────────────────────────────────────┐
   │  E-commerce layer                                        │
   │  mall :8301 · member :8302 · promotion :8303             │
   │  payment :8304 · order-center :8305 · fulfillment :8306  │
   │  search :8307 · analytics :8308                          │
   └─────────────────────────────────────────────────────────┘
```

Each service owns its own PostgreSQL database (`db_order`, `db_inventory`,
`db_product`, …). There are no cross-database joins anywhere in the codebase —
cross-database reads go through the per-entity `*CrossDatabaseQueryService` CQRS
classes, and any write spanning two databases is wrapped in `@GlobalTransactional`
so Seata can roll it back.

## What it covers

Every scenario below is implemented by the listed services. The right-hand column
names the constraint that actually shapes the design, not just the config.

| Scenario                  | What the platform does                                                                        | Services and constraints                                                                            |
| ------------------------- | --------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------- |
| Flash-sale ordering       | Campaign pricing, stock reservation with 900s auto-release, atomic deduction under contention | `promotion` 8303, `inventory` 8202, `order` 8203 — Redis Lua only, never PostgreSQL on the hot path |
| Order lifecycle           | Enum-driven state machine, event-sourced status history, idempotent creation by request ID    | `order` 8203 — transitions enforced in `OrderStatus.canTransitionTo()`                              |
| Distributed consistency   | One transaction spanning order → inventory → payment, with compensation on failure            | `@GlobalTransactional`; every database needs an `undo_log` table                                    |
| Multi-tenant SaaS         | Dynamic datasource routing per bounded context, per-tenant feature flags, tenant-scoped rows  | `@DS("user" \| "org" \| …)`, `TenantContextHolder`; `tenant_id` on every table, CI-enforced         |
| Procurement to receiving  | RFQ → quotation → contract → purchase order → goods receipt                                   | `purchase` 8207, `supplier` 8206, `warehouse` 8204                                                  |
| Warehouse execution       | Inbound and outbound, wave picking, bin location management                                   | `warehouse` 8204                                                                                    |
| Shipping and tracking     | Waybill creation, carrier integration, route optimization, delivery tracking                  | `logistics` 8205, `fulfillment` 8306                                                                |
| Payments and settlement   | Payment intents, refunds, reconciliation, invoicing                                           | `payment` 8304, `finance` 8208                                                                      |
| Approval and audit trails | Configurable approval chains, operation logs, sensitive-action tracking                       | `approval` 8209, `audit` 8210                                                                       |
| Product search            | Elasticsearch full-text search kept current by binlog, not by application writes              | `search` 8307, Canal → Elasticsearch                                                                |
| Analytics                 | ClickHouse OLAP with PostgreSQL metadata and Kafka ODS ingestion                              | `analytics` 8308 — ClickHouse 8123                                                                  |
| Identity and access       | OAuth2 + JWT + WebAuthn passwordless, RBAC with row-level data scope                          | `auth` 8106, `system` 8081                                                                          |
| Document lifecycle        | DOCX template rendering with an audit trail, byte storage proxied to the file service         | `document` 8213 → `file` 8201                                                                       |

> [!NOTE]
> The decision engine (`scm-common/decision-engine`, `scm-common/decision-matrix`)
> is a **library**, not a separately deployed service. It's consumed by
> `promotion`, `order`, and `analytics` rather than running on its own port.

## Modules

> [!NOTE]
> Ports in the **8201–8213** range form the supply-chain core; **8301–8308** is
> the e-commerce layer. `scm-tenant` carries a per-database routing flag rather
> than a public port. Business services follow an **api / service split** — `api/`
> holds the Dubbo RPC interfaces and DTOs, `service/` is the deployable Spring
> Boot app. `scm-gateway`, `scm-auth`, `scm-system`, and everything under
> `scm-common` are flat, single modules.

| Module             | Port   | Description                                                                                       |
| ------------------ | ------ | ------------------------------------------------------------------------------------------------- |
| `scm-web`          | 3000 ⚠ | Frontend — Next.js 15, Ant Design 5, Zustand, TanStack Query                                      |
| `scm-gateway`      | 8761   | API Gateway — routing, rate limiting, cross-cutting concerns                                      |
| `scm-auth`         | 8106   | Authentication — OAuth2, JWT, WebAuthn passwordless login                                         |
| `scm-system`       | 8081   | System management — users, roles, permissions, departments                                        |
| `scm-file`         | 8201 ⚠ | File service — upload, storage abstraction (S3/MinIO/local), virus scan                           |
| `scm-product`      | 8201 ⚠ | Product catalog — SPU/SKU, categories, brands, attributes                                         |
| `scm-inventory`    | 8202   | Inventory — real-time stock, reservations, alerts, snapshots                                      |
| `scm-order`        | 8203   | Orders — lifecycle state machine, payments, refunds                                               |
| `scm-warehouse`    | 8204   | Warehouse — inbound/outbound, wave picking, location management                                   |
| `scm-logistics`    | 8205   | Logistics — carriers, waybills, tracking, route optimization                                      |
| `scm-supplier`     | 8206   | Suppliers — onboarding, evaluation, settlements                                                   |
| `scm-purchase`     | 8207   | Procurement — RFQ, quotations, contracts, purchase orders                                         |
| `scm-finance`      | 8208   | Finance — settlements, invoices, freight rules, reconciliation                                    |
| `scm-message`      | 8209 ⚠ | Messaging — SMS / email / push dispatch, channel adapters                                         |
| `scm-approval`     | 8209 ⚠ | Approval workflows — configurable approval processes                                              |
| `scm-audit`        | 8210   | Audit — operation logs, sensitive operation tracking                                              |
| `scm-notify`       | 8211   | In-app notifications — templates, multi-channel delivery                                          |
| `scm-tenant`       | —      | Multi-tenant — tenant lifecycle, packages, feature flags                                          |
| `scm-document`     | 8213   | Document lifecycle — DOCX rendering + audit trail, proxies `scm-file` for byte transfer           |
| `scm-mall`         | 8301   | Mall — e-commerce storefront, product display, cart                                               |
| `scm-member`       | 8302   | Member management — profiles, addresses, points, loyalty programs                                 |
| `scm-promotion`    | 8303   | Promotions — campaigns, coupons, discounts, flash sales                                           |
| `scm-payment`      | 8304   | Payment — payment processing, refunds, reconciliation                                             |
| `scm-order-center` | 8305   | Order center — centralized order orchestration                                                    |
| `scm-fulfillment`  | 8306   | Fulfillment — order fulfillment, shipping, delivery tracking                                      |
| `scm-search`       | 8307   | Search — Elasticsearch-powered full-text product search                                           |
| `scm-analytics`    | 8308   | Analytics — ClickHouse OLAP + PostgreSQL metadata, Kafka ODS ingestion                            |
| `scm-common`       | —      | Shared libraries — core, data, data-rw, cache, web, monitoring, integration, decision-*, security |

### Project structure

```
scm-platform/
├── com.scm.parent/          # Parent POM (dependency management) — always build via -f
├── scm-web/                 # Frontend (Next.js 15, React 19, Ant Design 5) — separate npm project
├── scm-common/              # Shared libraries (no Spring Boot main)
│   ├── core/                # Utilities, exceptions, TenantContextHolder
│   ├── data/                # MyBatis-Plus integration, dynamic-datasource routing
│   ├── data-rw/             # @Master / @Slave read-write separation
│   ├── data-rw-stub/        # Stub variant for tests
│   ├── cache/               # Caffeine + Redis, Lua script center, idempotency, locks
│   ├── web/                 # Web filters, REST clients, security config
│   ├── monitoring/          # Sentinel circuit breaker
│   ├── integration/         # Kafka / RabbitMQ messaging (JacksonJsonSerializer)
│   ├── decision-matrix/     # Decision engine core types
│   ├── decision-engine/     # Weighted-fusion engine, A/B testing
│   └── security/            # Security core + api
├── scm-gateway/             # API Gateway (Spring Cloud Gateway, flat module)
├── scm-auth/                # Authentication (OAuth2, JWT, WebAuthn — flat module)
├── scm-system/              # System management (users, roles, permissions)
├── scm-approval/            # Approval workflows
├── scm-audit/               # Audit logging
├── scm-notify/              # In-app notifications
├── scm-tenant/              # Multi-tenant management
├── scm-product/             # Product catalog (api + service)
├── scm-inventory/           # Inventory (api + service, Redis Lua hot path)
├── scm-order/               # Order processing (api + service, state machine)
├── scm-warehouse/           # Warehouse operations
├── scm-logistics/           # Logistics tracking
├── scm-purchase/            # Procurement
├── scm-supplier/            # Supplier management
├── scm-finance/             # Financial settlement
├── scm-file/                # File service (upload, storage abstraction, OCR)
├── scm-document/            # Document lifecycle & DOCX templates (v1.3.0)
├── scm-message/             # SMS / email / push dispatch
├── scm-member/              # Member management
├── scm-promotion/           # Promotions (campaigns, coupons, discounts)
├── scm-payment/             # Payment processing
├── scm-search/              # Elasticsearch full-text search
├── scm-order-center/        # Centralized order orchestration
├── scm-mall/                # E-commerce storefront
├── scm-fulfillment/         # Order fulfillment & shipping
├── scm-analytics/           # ClickHouse OLAP + PG metadata
├── deploy/                  # K8s manifests, Helm, ArgoCD, PgBouncer, Redis, Istio, chaos
├── scripts/                 # DB init, partition mgmt, load tests, retention, quality
└── docs/                    # Architecture, runbooks, design specs, audits
```

## Key architecture patterns

### Decision engine (e-commerce decision matrix)

```java
// Weighted fusion engine for multi-criteria decision making
WeightedFusionEngine engine = new WeightedFusionEngine();
DecisionResult result = engine.evaluate(candidates, criteria, weights);

// A/B testing support
if (experiment.isTreatmentGroup(userId)) {
    return treatmentEngine.evaluate(candidates);
}
```

### Multi-tenant data routing

```java
@DS("user")  // Routes to db_user
public class UserMapper extends BaseMapper<SysUser> { }
```

### Read-write separation

```java
@Slave  // Auto-route to read replica
public List<Order> queryOrders(OrderQuery query) { }

@Master  // Force write to primary
public void updateOrder(Order order) { }
```

### Atomic stock deduction (Redis Lua)

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

### Distributed transaction (Seata AT)

```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public Order createOrder(OrderDTO dto) {
    Order order = orderMapper.insert(new Order(dto));
    inventoryClient.deductStock(dto.getSkuId(), dto.getQuantity());
    paymentClient.createPayment(order.getId(), dto.getAmount());
    return order;
}
```

### Order state transitions

```java
public enum OrderStatus {
    PENDING_PAYMENT, PAID, PENDING_SHIP, SHIPPED,
    IN_TRANSIT, DELIVERED, COMPLETED, CANCELLED, REFUNDING, REFUNDED;

    public Set<OrderStatus> validNextStatuses() {
        return switch (this) {
            case PENDING_PAYMENT -> Set.of(PAID, CANCELLED);
            case PAID           -> Set.of(PENDING_SHIP, CANCELLED, REFUNDING);
            case COMPLETED      -> Set.of();  // terminal
            // …
        };
    }
}
```

The transition table is the single source of truth for what an order can do next,
and it is enforced in the aggregate rather than in a service method — an illegal
transition throws before a write is attempted.

## Kubernetes deployment

> [!WARNING]
> **The e-commerce layer is Docker Compose only.** Manifests in `deploy/k8s/`
> cover the infrastructure and supply-chain core. `scm-mall`, `scm-member`,
> `scm-promotion`, `scm-payment`, `scm-order-center`, `scm-fulfillment`,
> `scm-search`, `scm-analytics`, and `scm-message` need manifests written before
> they can be deployed to a cluster.

```bash
# Apply all K8s resources
kubectl apply -f deploy/k8s/namespace.yml
kubectl apply -f deploy/k8s/configmap.yml
kubectl apply -f deploy/k8s/secrets.yml
kubectl apply -f deploy/k8s/scm-*-deployment.yml
kubectl apply -f deploy/k8s/scm-*-service.yml

# Canary (auth only)
kubectl apply -f deploy/k8s/scm-auth-canary-deployment.yml
kubectl apply -f deploy/k8s/scm-auth-canary-service.yml

# Or use Helm
helm install scm-platform deploy/helm/scm-platform/

# ArgoCD GitOps
kubectl apply -f deploy/argocd/application.yaml
```

### Services with K8s manifests

| Service         | Port   | Notes                       |
| --------------- | ------ | --------------------------- |
| `scm-gateway`   | 8761   | HPA-enabled                 |
| `scm-system`    | 8081   |                             |
| `scm-auth`      | 8106   | Plus canary deployment      |
| `scm-product`   | 8201 ⚠ | Shares 8201 with `scm-file` |
| `scm-inventory` | 8202   |                             |
| `scm-warehouse` | 8204   |                             |
| `scm-logistics` | 8205   |                             |
| `scm-supplier`  | 8206   |                             |
| `scm-purchase`  | 8207   |                             |
| `scm-finance`   | 8208   |                             |
| `scm-document`  | 8213   | Added in v1.3.0             |

## Operations

### Infrastructure

| Component          | Purpose                      | Config                        |
| ------------------ | ---------------------------- | ----------------------------- |
| **PgBouncer**      | Connection pooling           | `deploy/pgbouncer/`           |
| **Redis Sentinel** | Cache HA                     | `deploy/redis/sentinel.conf`  |
| **PostgreSQL HA**  | Database HA with Patroni     | `deploy/postgresql/ha/`       |
| **Kafka**          | Event streaming              | Docker Compose                |
| **Canal**          | Binlog sync to Elasticsearch | `docker-compose-debezium.yml` |

### Database management

```bash
# Initialize all databases
export PGPASSWORD=admin123 && ./scripts/db/init-all-databases.sh

# Create partitions (next 3 months)
./scripts/db/partition/create-partitions.sh

# Drop old partitions (24-month retention)
./scripts/db/partition/drop-old-partitions.sh

# Apply data retention policies
./scripts/db/retention/apply-retention.sh

# Run data quality checks
./scripts/db/quality/check-quality.sh
```

> [!NOTE]
> **Partitioned tables can't use a plain UNIQUE constraint.** PostgreSQL
> requires the partition key in every unique constraint — `ord_order(order_no,
create_time)`, `inv_reservation(reservation_no, reserved_at)`,
> `sup_purchase_order(purchase_no, create_time)`. Uniqueness that spans partitions
> is enforced at the application layer via Redis. Note that `inv_reservation`
> lives in `db_inventory` and `sup_purchase_order` in `db_supplier`, not `db_order`.

### Testing

```bash
# Unit tests
mvn test -pl scm-order/service -f com.scm.parent/pom.xml

# Single test
mvn test -Dtest=OrderServiceTest -pl scm-order/service -f com.scm.parent/pom.xml

# Full CI check (tests + JaCoCo coverage gates)
mvn verify -Djacoco.skip=false -f com.scm.parent/pom.xml

# Load tests (requires k6)
k6 run scripts/loadtest/order-flow.js
k6 run scripts/loadtest/inventory-check.js

# E2E tests (requires Playwright)
cd scm-web && npx playwright test
```

> [!NOTE]
> **Coverage gates are off by default.** `jacoco.skip=true` is set in the parent
> POM, so a plain `mvn verify` will not enforce the 70% line / 60% branch
> thresholds. Pass `-Djacoco.skip=false` when you want them enforced.

## FAQ

<details>
<summary><strong>Why does every Maven command need <code>-f com.scm.parent/pom.xml</code>?</strong></summary>

There is no root `pom.xml` in this repository. `com.scm.parent/pom.xml` is the
parent POM and it declares its modules with `../scm-*` relative paths. If you run
`mvn` from the repository root without the flag, Maven looks for a POM that isn't
there.

The root `build.bat` pins a GraalVM JDK 21 and a local Maven 3.9.16 path for
this reason — Maven and the JDK are not on `PATH` by default on Windows dev
machines.

</details>

<details>
<summary><strong>Why do <code>scm-file</code> and <code>scm-product</code> both want port 8201?</strong></summary>

This is a real collision in the checked-in configuration, not a doc error.
`scm-message` and `scm-approval` both want 8209, and Grafana maps `3000:3000`
while the Next.js dev server also wants 3000.

All three are safe to work around: override `server.port` for whichever service
you aren't running, or remap the Grafana host port in `docker-compose.yml`.

</details>

<details>
<summary><strong>Is it safe to run with the credentials in the repo?</strong></summary>

For local development, yes. The checked-in `docker-compose.yml`, Helm
`values*.yaml`, and `deploy/k8s/secrets.yml` all contain **placeholder**
credentials (`changeme` and similar).

Set real values through environment variables or a secrets manager before any
deployment that isn't your laptop. Note that the database init scripts use
`admin` / `admin123` as their default, which differs from the `changeme`
placeholders in `.env.example`.

</details>

<details>
<summary><strong>Why are order status transitions hand-rolled instead of using Spring State Machine?</strong></summary>

The Spring State Machine dependency is declared in the parent POM but is not
used. The transitions live in the `OrderStatus` enum as a `validNextStatuses()`
switch, and `OrdOrder.transitionTo()` validates against it inside the aggregate.

A switch expression over ten enum constants is greppable, unit-testable without a
Spring context, and gives the aggregate ownership of the rule. For other entity
types the generic path is `StatusValidator` in `scm-common/web`, which delegates
to `StatusMachineDubboService` in `scm-system`.

</details>

<details>
<summary><strong>Why did my pull request go green even though a test failed?</strong></summary>

By design. `.github/workflows/maven-build.yml` runs pull request builds with
`-Dmaven.test.failure.ignore=true` so that CI stays green while the change is
still in review. Push builds to `master` and `develop` run the full `mvn verify`
plus a schema validation that fails if any listed table is missing a `tenant_id`
column.

Don't rely on the PR check to catch a regression — run the suite locally.

</details>

<details>
<summary><strong>Do I need Elasticsearch, ClickHouse, or Canal running to start the core services?</strong></summary>

No. The core supply-chain services need PostgreSQL, Redis, and Nacos. Elasticsearch
backs `scm-search`, ClickHouse backs `scm-analytics`, and Canal feeds binlog sync
into Elasticsearch — those three are only required if you're running the modules
that depend on them.

</details>

<details>
<summary><strong>Where does each service store its data?</strong></summary>

Each service owns a separate PostgreSQL database — `db_order`, `db_inventory`,
`db_product`, `db_user`, and so on — routed by the `@DS` annotation and resolved
from `TenantContextHolder`. There are no cross-database joins; cross-database
reads go through the per-entity `*CrossDatabaseQueryService` CQRS classes.

Every business table must carry a `tenant_id` column. This is enforced by CI
(`scripts/db/ci_validate_tenant_id.sql`) across all 16 databases, and the build
fails without it.

</details>

<details>
<summary><strong>What should I do if a service won't start?</strong></summary>

Check the start order first — infrastructure, then Gateway, Auth, System, then
business services. A service started before `scm-system` will fail to resolve
permission lookups over Dubbo, and one started before Nacos is healthy will fail
to register.

After that, check that the port is actually free. Given the three known
collisions above, a silent bind failure is the most common cause.

</details>

## Documentation

Start with [docs/README.md](./docs/README.md) for the reading order. For specific
areas:

- [Architecture](./docs/design/ARCHITECTURE.md) — service decomposition, database philosophy, stack decisions
- [CQRS architecture](./docs/design/CQRS_ARCHITECTURE.md) — cross-database query optimization
- [Architecture decision records](./docs/design/ADR.md) — the reasoning behind the major calls
- [Multi-tenant guide](./docs/multi-tenant/MULTI_TENANT_GUIDE.md) — isolation model, permission design
- [Development standards](./docs/development/DEVELOPMENT_STANDARDS.md) — coding, config, and API conventions
- [API design](./docs/technical/API_DESIGN.md) and [database design](./docs/technical/DATABASE_DESIGN.md)
- [Integration guides](./docs/guides/) — Seata AT, Seata TCC, XXL-Job, Elasticsearch
- [Operations manual](./docs/operations/OPERATIONS_MANUAL.md) — deployment, monitoring, incident handling
- [Runbooks](./docs/operations/runbooks/) — deployment rollback, database failover, Redis failover, incident response
- [Performance benchmarks](./docs/project-management/PERFORMANCE_BENCHMARK.md) — load targets and baselines
- [Quick start](./QUICK_START.md) — the short version of the setup above

Report vulnerabilities through [SECURITY.md](./SECURITY.md).

## Star history

<a href="https://www.star-history.com/?repos=dengxingzhi000%2Fscm-platform&amp;type=date">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/chart?repos=dengxingzhi000/scm-platform&amp;type=date&amp;theme=dark&amp;legend=top-left" />
    <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/chart?repos=dengxingzhi000/scm-platform&amp;type=date" />
    <img alt="SCM Platform GitHub star history" src="https://api.star-history.com/chart?repos=dengxingzhi000/scm-platform&amp;type=date" />
  </picture>
</a>

## Related projects

- **[enterprise-agent](https://github.com/dengxingzhi000/enterprise-agent)** — Python AI operations agent built on this platform: a self-built agent runtime (Planner/Executor loop) for order-incident triage, reimbursement compliance review, and sales analysis, with tools/MCP, RAG, memory, policy/HITL, workflows, and eval/observability.

## Contributing

We'd love your help! [Open an issue](https://github.com/dengxingzhi000/scm-platform/issues)
to report a bug or suggest a feature, or send a pull request to improve the code
or docs.

Please read [CONTRIBUTING.md](./CONTRIBUTING.md) first — it covers the build, the
branch conventions, and what CI will check.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a pull request

All notable changes are tracked in [CHANGELOG.md](./CHANGELOG.md), and everyone
is expected to follow the [code of conduct](./CODE_OF_CONDUCT.md).

## License

[Apache License 2.0](./LICENSE)

---

<div align="center">

**Built with ❤️ for enterprise supply chain management**

</div>
