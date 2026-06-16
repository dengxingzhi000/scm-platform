<div align="center">

# SCM Platform

**Enterprise Supply Chain Management System**

[![Build Status](https://img.shields.io/github/actions/workflow/status/dengxingzhi000/scm-platform/maven-build.yml?branch=master&style=flat-square&logo=github&label=build)](https://github.com/dengxingzhi000/scm-platform/actions)
[![Stars](https://img.shields.io/github/stars/dengxingzhi000/scm-platform?color=ffcb47&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/stargazers)
[![Forks](https://img.shields.io/github/forks/dengxingzhi000/scm-platform?color=8ae8ff&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/network/members)
[![Issues](https://img.shields.io/github/issues/dengxingzhi000/scm-platform?color=ff80eb&labelColor=black&style=flat-square&logo=github)](https://github.com/dengxingzhi000/scm-platform/issues)
[![License](https://img.shields.io/badge/license-Apache%202.0-white?labelColor=black&style=flat-square)](https://github.com/dengxingzhi000/scm-platform/blob/master/LICENSE)

[English](./README.md) | [简体中文](./README.zh-CN.md)

</div>

---

> A cloud-native, microservices-based supply chain management platform built on Spring Boot 4 + Spring Cloud 2025. Designed for high-concurrency scenarios like flash sales, with architecture patterns inspired by Alibaba Cainiao, JD Logistics, and Meituan delivery systems.

## Highlights

| Icon | Feature | Description |
|------|---------|-------------|
| 🏗️ | **Microservices Architecture** | 15+ independently deployable services with API Gateway, service discovery, and config management |
| 🔐 | **Enterprise Security** | OAuth2 + JWT + WebAuthn passwordless auth, RBAC with fine-grained data scope control |
| 💰 | **Distributed Transactions** | Seata AT/TCC/Saga modes for cross-service data consistency |
| ⚡ | **High Performance** | Redis Lua atomic stock deduction, read-write separation, database sharding |
| 🔍 | **Full-Text Search** | Elasticsearch-powered product search with real-time sync via Canal binlog |
| 📊 | **Observability** | Sentinel circuit breaking, SkyWalking tracing, Prometheus metrics, Grafana dashboards |
| 🏢 | **Multi-Tenant** | Tenant isolation with dynamic data source routing and configurable feature flags |
| 🖥️ | **Modern Frontend** | Next.js 15 App Router, Ant Design 5 Pro, Zustand state management, TanStack Query |
| 📦 | **Domain-Driven Design** | Clean CQRS separation, aggregate roots, domain events via Kafka/RabbitMQ |
| ☸️ | **Cloud Native** | Kubernetes manifests for all services, Helm charts, ArgoCD GitOps, canary deployments |

## Tech Stack

| Layer | Components |
|-------|-----------|
| **Backend** | Java 21 (Virtual Threads), Spring Boot 4.0.6, Spring Cloud 2025.1.1 |
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
                         ┌──────────────────────┐
                         │   Frontend (scm-web)  │
                         │   Next.js 15 / React  │
                         │   Ant Design 5        │
                         └──────────┬───────────┘
                                    │
                         ┌──────────▼───────────┐
                         │     API Gateway      │ :8761
                         └──────────┬───────────┘
                                    │
     ┌──────────┬──────────┬────────┼────────┬──────────┬──────────┐
     │          │          │        │        │          │          │
  ┌──▼───┐  ┌───▼───┐  ┌──▼───┐ ┌─▼────┐ ┌─▼────┐  ┌──▼───┐  ┌──▼───┐
  │ Auth  │  │System │  │Product│ │Order │ │ WMS  │  │Logist│  │Notify │
  │ :8106 │  │ :8081 │  │:8201  │ │:8203 │ │:8204 │  │:8205 │  │       │
  └───────┘  └───────┘  └───────┘ └──────┘ └──────┘  └──────┘  └──────┘
     │          │          │        │        │          │          │
  ┌──▼───┐  ┌───▼───┐  ┌──▼───┐ ┌─▼────┐ ┌─▼────┐  ┌──▼───┐  ┌──▼───┐
  │Approv│  │ Audit │  │ INV  │ │Finance│ │Suppl │  │Purch │  │ Tenant│
  │      │  │       │  │:8202 │ │       │ │:8206 │  │      │  │       │
  └──────┘  └───────┘  └──────┘ └───────┘ └──────┘  └──────┘  └──────┘
```

## Modules

| Module | Port | Description |
|--------|------|-------------|
| `scm-web` | 3000 | Frontend — Next.js 15, Ant Design 5, Zustand, TanStack Query |
| `scm-gateway` | 8761 | API Gateway — routing, rate limiting, cross-cutting concerns |
| `scm-auth` | 8106 | Authentication — OAuth2, JWT, WebAuthn passwordless login |
| `scm-system` | 8081 | System management — users, roles, permissions, departments |
| `scm-product` | 8201 | Product catalog — SPU/SKU, categories, brands, attributes |
| `scm-inventory` | 8202 | Inventory — real-time stock, reservations, alerts, snapshots |
| `scm-order` | 8203 | Orders — lifecycle, state machine, payments, refunds |
| `scm-warehouse` | 8204 | Warehouse — inbound/outbound, wave picking, location management |
| `scm-logistics` | 8205 | Logistics — carriers, waybills, tracking, route optimization |
| `scm-supplier` | 8206 | Suppliers — onboarding, evaluation, settlements |
| `scm-purchase` | — | Procurement — RFQ, quotations, contracts, purchase orders |
| `scm-finance` | — | Finance — settlements, invoices, freight rules, reconciliation |
| `scm-tenant` | — | Multi-tenant — tenant lifecycle, packages, feature flags |
| `scm-approval` | — | Approval workflows — configurable approval processes |
| `scm-audit` | — | Audit — operation logs, sensitive operation tracking |
| `scm-notify` | — | Notifications — templates, multi-channel delivery, audit |

## Quick Start

### Prerequisites

- **JDK 21** (virtual threads required)
- **Maven 3.8+**
- **Node.js 20+** (for frontend)
- **Docker & Docker Compose**

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
| Sentinel Dashboard | http://localhost:8080 |
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

All 9 services have deployment + service manifests in `deploy/k8s/`. Deploy with:

```bash
# Apply all K8s resources
kubectl apply -f deploy/k8s/namespace.yml
kubectl apply -f deploy/k8s/configmap.yml
kubectl apply -f deploy/k8s/secrets.yml
kubectl apply -f deploy/k8s/scm-*-deployment.yml
kubectl apply -f deploy/k8s/scm-*-service.yml

# Or use Helm
helm install scm-platform deploy/helm/scm-platform/

# ArgoCD GitOps
kubectl apply -f deploy/argocd/application.yaml
```

### Services Port Map

| Service | Port | Replicas |
|---------|------|----------|
| scm-gateway | 8761 | 3 |
| scm-system | 8081 | 3 |
| scm-product | 8201 | 2 |
| scm-inventory | 8202 | 2 |
| scm-order | 8203 | 2 |
| scm-warehouse | 8204 | 2 |
| scm-logistics | 8205 | 2 |
| scm-supplier | 8206 | 2 |
| scm-purchase | 8207 | 2 |
| scm-finance | 8208 | 2 |

## Load Testing

```bash
# Run k6 load tests
k6 run scripts/loadtest/order-flow.js
k6 run scripts/loadtest/inventory-check.js
```

## Key Architecture Patterns

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
├── com.scm.parent/          # Parent POM (dependency management)
├── scm-web/                 # Frontend (Next.js 15, React 19, Ant Design 5)
├── scm-common/              # Shared modules
│   ├── core/                # Utilities, exceptions, tenant context
│   ├── data/                # Data access, read-write separation, multi-tenant routing
│   ├── web/                 # Web filters, REST clients, security config
│   ├── monitoring/          # Sentinel circuit breaker
│   ├── integration/         # Kafka & RabbitMQ messaging
│   └── security/            # Security core & API
├── scm-gateway/             # API Gateway
├── scm-auth/                # Authentication service
├── scm-system/              # System management (users, roles, permissions)
├── scm-product/             # Product catalog
├── scm-inventory/           # Inventory management
├── scm-order/               # Order processing
├── scm-warehouse/           # Warehouse operations
├── scm-logistics/           # Logistics tracking
├── scm-purchase/            # Procurement
├── scm-supplier/            # Supplier management
├── scm-finance/             # Financial settlement
├── scm-tenant/              # Multi-tenant management
├── scm-approval/            # Approval workflows
├── scm-audit/               # Audit logging
├── scm-notify/              # Notification service
├── deploy/                  # Deployment configs (K8s, Helm, ArgoCD, PgBouncer)
├── scripts/                 # Database init scripts, partition mgmt, load tests
└── docs/                    # Documentation, audits, data dictionary
```

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

## License

Apache License 2.0 — see [LICENSE](./LICENSE) for details.

---

<div align="center">

**Built with ❤️ for enterprise supply chain management**

</div>
