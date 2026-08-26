<div align="center">

# SCM Platform

**Enterprise Supply Chain Management System**

[![Build Status](https://img.shields.io/github/actions/workflow/status/dengxingzhi000/scm-platform/maven-build.yml?branch=master&style=flat-square&logo=github&label=build)](https://github.com/dengxingzhi000/scm-platform/actions)
[![Stars](https://img.shields.io/github/stars/dengxingzhi000/scm-platform?color=ffcb47&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/stargazers)
[![Forks](https://img.shields.io/github/forks/dengxingzhi000/scm-platform?color=8ae8ff&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/network/members)
[![Issues](https://img.shields.io/github/issues/dengxingzhi000/scm-platform?color=ff80eb&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/issues)
[![License](https://img.shields.io/badge/license-Apache%202.0-white?labelColor=black&style=flat-square)](https://github.com/dengxingzhi000/scm-platform/blob/master/LICENSE)

[English](./README.md)

</div>

---

> A cloud-native, microservices-based supply chain management platform built on Spring Boot 4 + Spring Cloud 2025. Designed for high-concurrency scenarios like flash sales, with architecture patterns inspired by Alibaba Cainiao, JD Logistics, and Meituan delivery systems.

## Highlights

| Icon | Feature | Description |
|------|---------|-------------|
| 🏗️ | **Microservices Architecture** | 22+ independently deployable services with API Gateway, service discovery, and config management |
| 🔐 | **Enterprise Security** | OAuth2 + JWT + WebAuthn passwordless auth, RBAC with fine-grained data scope control |
| 💰 | **Distributed Transactions** | Seata AT/TCC/Saga modes for cross-service data consistency |
| ⚡ | **High Performance** | Redis Lua atomic stock deduction, read-write separation, database sharding |
| 🔍 | **Full-Text Search** | Elasticsearch-powered product search with real-time sync via Canal binlog |
| 🧠 | **Decision Engine** | E-commerce decision matrix with intelligent pricing, inventory prediction, and A/B testing |
| 📊 | **Observability** | Sentinel circuit breaking, SkyWalking tracing, Prometheus metrics, Grafana dashboards |
| 🏢 | **Multi-Tenant** | Tenant isolation with dynamic data source routing and configurable feature flags |
| 🖥️ | **Modern Frontend** | Next.js 15 App Router, Ant Design 5 Pro, Zustand state management, TanStack Query |
| 📦 | **Domain-Driven Design** | Clean CQRS separation, aggregate roots, domain events via Kafka/RabbitMQ |
| ☸️ | **Cloud Native** | Kubernetes manifests for core services, Helm charts, ArgoCD GitOps, canary deployments (auth) |

## Tech Stack

| Layer | Components |
|-------|-----------|
| **Backend** | Java 21 (Virtual Threads), Spring Boot 4.0.6, Spring Cloud 2025.1.2 |
| **Alibaba** | Spring Cloud Alibaba 2025.1.0.0 (Nacos, Sentinel, Seata) |
| **Database** | PostgreSQL, MyBatis-Plus 3.5.15, ShardingSphere 5.5.1 |
| **Cache** | Redis (distributed cache, Lua scripts for atomic operations) |
| **Search** | Elasticsearch 8.11.4, Canal 1.1.7 (binlog sync) |
| **Messaging** | Kafka (event streaming), RabbitMQ (reliable queue) |
| **RPC** | Apache Dubbo 3.3.6 (internal service calls) |
| **Scheduling** | XXL-Job 3.3.1 (distributed task scheduling) |
| **Monitoring** | Sentinel, SkyWalking 9.3.0, Micrometer + Prometheus |
| **Frontend** | Next.js 15 (App Router), React 19, Ant Design 5, Zustand 5, TanStack Query 5, ECharts, next-intl |
| **DevOps** | Docker, Kubernetes, Helm, ArgoCD, GitHub Actions, SonarCloud, OWASP |
| **Testing** | JUnit 5, k6 load testing, Pact contract testing, Playwright E2E, PIT mutation testing |

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
   │  audit :8210 · notify :8211 · tenant :8212              │
   └────────┬───────────────────────────────────────────────┘
            │
   ┌────────▼───────────────────────────────────────────────┐
   │  E-commerce layer                                        │
   │  mall :8301 · member :8302 · promotion :8303             │
   │  payment :8304 · order-center :8305 · fulfillment :8306  │
   │  search :8307                                            │
   └─────────────────────────────────────────────────────────┘
```

> **Port collisions (local dev only):** `scm-file` ↔ `scm-product` both 8201;
> `scm-message` ↔ `scm-approval` both 8209. Don't run both of a pair without
> overriding `server.port`.

## Modules

> Ports in the **8201–8212** range form the supply-chain core, the **8301–8307**
> range is the e-commerce layer. `scm-tenant` carries a per-DB routing flag
> rather than a public port.

| Module | Port | Description |
|--------|------|-------------|
| `scm-web` | 3000 | Frontend — Next.js 15, Ant Design 5, Zustand, TanStack Query |
| `scm-gateway` | 8761 | API Gateway — routing, rate limiting, cross-cutting concerns |
| `scm-auth` | 8106 | Authentication — OAuth2, JWT, WebAuthn passwordless login |
| `scm-system` | 8081 | System management — users, roles, permissions, departments |
| `scm-file` | 8201 ⚠ | File service — upload, storage abstraction (S3/MinIO/local), OCR |
| `scm-product` | 8201 ⚠ | Product catalog — SPU/SKU, categories, brands, attributes |
| `scm-inventory` | 8202 | Inventory — real-time stock, reservations, alerts, snapshots |
| `scm-order` | 8203 | Orders — lifecycle state machine, payments, refunds |
| `scm-warehouse` | 8204 | Warehouse — inbound/outbound, wave picking, location management |
| `scm-logistics` | 8205 | Logistics — carriers, waybills, tracking, route optimization |
| `scm-supplier` | 8206 | Suppliers — onboarding, evaluation, settlements |
| `scm-purchase` | 8207 | Procurement — RFQ, quotations, contracts, purchase orders |
| `scm-finance` | 8208 | Finance — settlements, invoices, freight rules, reconciliation |
| `scm-message` | 8209 ⚠ | Messaging — SMS / email / push dispatch, channel adapters |
| `scm-approval` | 8209 ⚠ | Approval workflows — configurable approval processes |
| `scm-audit` | 8210 | Audit — operation logs, sensitive operation tracking |
| `scm-notify` | 8211 | In-app notifications — templates, multi-channel delivery |
| `scm-tenant` | 8212 | Multi-tenant — tenant lifecycle, packages, feature flags |
| `scm-mall` | 8301 | Mall — e-commerce storefront, product display, cart |
| `scm-member` | 8302 | Member management — profiles, addresses, points, loyalty programs |
| `scm-promotion` | 8303 | Promotions — campaigns, coupons, discounts, flash sales |
| `scm-payment` | 8304 | Payment — payment processing, refunds, reconciliation |
| `scm-order-center` | 8305 | Order center — centralized order orchestration |
| `scm-fulfillment` | 8306 | Fulfillment — order fulfillment, shipping, delivery tracking |
| `scm-search` | 8307 | Search — Elasticsearch-powered full-text product search |
| `scm-common` | — | Shared libraries — core, data, data-rw, cache, web, monitoring, integration, decision-matrix, decision-engine, security |

> **Port collisions:** `scm-file` ↔ `scm-product` (8201) and
> `scm-message` ↔ `scm-approval` (8209). Override `server.port` when running
> both locally. The decision engine (`scm-common/decision-engine`,
> `scm-common/decision-matrix`) is a library, not a separately deployed
> service.

## Quick Start

### Prerequisites

- **JDK 21** (virtual threads required)
- **Maven 3.8+**
- **Node.js 20+** (for frontend)
- **Docker & Docker Compose**

All components run locally with sensible development defaults; no external
accounts or cloud credentials are required. See
[Environment Variables](#environment-variables) for optional overrides.

### Environment Variables

Everything has a local-development default, so you can start without setting
anything. Override via your shell or a `.env` file (never commit one):

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SECRET` | dev placeholder | JWT signing key — **must be ≥ 512 bits (64 bytes)**; required in production |
| `NACOS_SERVER` | `localhost:8848` | Nacos config/registry address |
| `NACOS_USERNAME` / `NACOS_PASSWORD` | `nacos` / `nacos` | Nacos auth credentials |
| `DB_HOST` / `DB_PORT` | `localhost` / `5432` | PostgreSQL host / port |
| `DB_USERNAME` / `DB_PASSWORD` | `admin` / `changeme` | PostgreSQL credentials |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis host / port |
| `REDIS_PASSWORD` | `changeme` | Redis password |
| `SEATA_SERVER_ADDR` | `127.0.0.1:8091` | Seata server address |

> **Security note:** the checked-in `docker-compose.yml`, Helm `values*.yaml`, and
> `deploy/k8s/secrets.yml` contain **placeholder** credentials only. Set real
> values via the environment / secrets manager before any production deployment.

### 1. Start Infrastructure

```bash
docker-compose up -d
```

### 2. Initialize Databases

```bash
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat

# Linux/Mac
export PGPASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh
```

### 3. Build

```bash
# Build all modules
mvn clean install -DskipTests -f com.scm.parent/pom.xml

# Build a single module
mvn clean package -pl scm-order/service -am -f com.scm.parent/pom.xml
```

### 4. Start Services

> **Order:** Infrastructure → Gateway → Auth → System → Business services

```bash
# Gateway
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

### 5. Access Endpoints

| Service | URL |
|---------|-----|
| API Gateway | http://localhost:8761 |
| Nacos Console | http://localhost:8848/nacos |
| Sentinel Dashboard | http://localhost:8858 |
| XXL-Job Admin | http://localhost:8088/xxl-job-admin |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

### 6. Frontend

```bash
cd scm-web
npm install
npm run dev
```

The frontend runs at **http://localhost:3000** with built-in zh-CN and en-US language support.

| Route | Page |
|-------|------|
| `/login` | Login & authentication |
| `/dashboard` | Analytics dashboard |
| `/product` | Product catalog management |
| `/order` | Order lifecycle management |
| `/inventory` | Inventory & stock alerts |
| `/system/*` | System settings (users, roles, permissions) |

## Kubernetes Deployment

Manifests in `deploy/k8s/` currently cover the **infrastructure and
supply-chain core** services — the e-commerce layer services (`scm-mall`,
`scm-member`, `scm-promotion`, `scm-payment`, `scm-order-center`,
`scm-fulfillment`, `scm-search`) still rely on Docker Compose for local runs
and need manifests added before K8s deployment.

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

### Services With K8s Manifests

| Service | Port | Notes |
|---------|------|-------|
| scm-gateway | 8761 | HPA-enabled |
| scm-system | 8081 | |
| scm-auth | 8106 | Plus canary deployment |
| scm-product | 8201 ⚠ | Shares 8201 with `scm-file` |
| scm-inventory | 8202 | |
| scm-warehouse | 8204 | |
| scm-logistics | 8205 | |
| scm-supplier | 8206 | |
| scm-purchase | 8207 | |
| scm-finance | 8208 | |

## Load Testing

```bash
# Run k6 load tests
k6 run scripts/loadtest/order-flow.js
k6 run scripts/loadtest/inventory-check.js
```

## Key Architecture Patterns

### Decision Engine (E-Commerce Decision Matrix)

```java
// Weighted fusion engine for multi-criteria decision making
WeightedFusionEngine engine = new WeightedFusionEngine();
DecisionResult result = engine.evaluate(candidates, criteria, weights);

// A/B testing support
if (experiment.isTreatmentGroup(userId)) {
    return treatmentEngine.evaluate(candidates);
}
```

### Multi-Tenant Data Routing

```java
@DS("user")  // Routes to db_user
public class UserMapper extends BaseMapper<SysUser> { }
```

### Read-Write Separation

```java
@Slave  // Auto-route to read replica
public List<Order> queryOrders(OrderQuery query) { }

@Master  // Force write to primary
public void updateOrder(Order order) { }
```

### Atomic Stock Deduction (Redis Lua)

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

### Distributed Transaction (Seata AT)

```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public Order createOrder(OrderDTO dto) {
    Order order = orderMapper.insert(new Order(dto));
    inventoryClient.deductStock(dto.getSkuId(), dto.getQuantity());
    paymentClient.createPayment(order.getId(), dto.getAmount());
    return order;
}
```

## Infrastructure

| Component | Purpose | Config |
|-----------|---------|--------|
| **PgBouncer** | Connection pooling | `deploy/pgbouncer/` |
| **Redis Sentinel** | Cache HA | `deploy/redis/sentinel.conf` |
| **PostgreSQL HA** | Database HA with Patroni | `deploy/postgresql/ha/` |
| **Kafka** | Event streaming | Docker Compose |
| **Canal** | Binlog sync to Elasticsearch | `docker-compose-debezium.yml` |

## Project Structure

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
├── scm-message/             # SMS / email / push dispatch
├── scm-member/              # Member management
├── scm-promotion/           # Promotions (campaigns, coupons, discounts)
├── scm-payment/             # Payment processing
├── scm-search/              # Elasticsearch full-text search
├── scm-order-center/        # Centralized order orchestration
├── scm-mall/                # E-commerce storefront
├── scm-fulfillment/         # Order fulfillment & shipping
├── deploy/                  # K8s manifests, Helm, ArgoCD, PgBouncer, Redis, Istio, chaos
├── scripts/                 # DB init, partition mgmt, load tests, retention, quality
└── docs/                    # Architecture, runbooks, design specs, audits
```

> Business services (`scm-{name}/`) follow an **api / service split** — the
> `api/` module holds the Dubbo RPC interfaces and DTOs, the `service/`
> module is the Spring Boot deployable. `scm-gateway`, `scm-auth`,
> `scm-system`, and the modules under `scm-common` are flat (single module).

## Database Management

```bash
# Initialize all databases
export PGPASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh

# Create partitions (next 3 months)
./scripts/db/partition/create-partitions.sh

# Drop old partitions (24-month retention)
./scripts/db/partition/drop-old-partitions.sh

# Apply data retention policies
./scripts/db/retention/apply-retention.sh

# Run data quality checks
./scripts/db/quality/check-quality.sh
```

## Testing

```bash
# Unit tests
mvn test -pl scm-order/service -f com.scm.parent/pom.xml

# Single test
mvn test -Dtest=OrderServiceTest -pl scm-order/service -f com.scm.parent/pom.xml

# Full CI check (tests + JaCoCo coverage)
mvn verify -f com.scm.parent/pom.xml

# Load tests (requires k6)
k6 run scripts/loadtest/order-flow.js

# E2E tests (requires Playwright)
cd scm-web && npx playwright test
```

## Contributing

Contributions are welcome! Please read the [contributing guidelines](./CONTRIBUTING.md) before submitting a PR.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please also review the [code of conduct](./CODE_OF_CONDUCT.md). All notable
changes are tracked in [CHANGELOG.md](./CHANGELOG.md).

## License

Apache License 2.0 — see [LICENSE](./LICENSE) for details.

---

<div align="center">

**Built with ❤️ for enterprise supply chain management**

</div>
