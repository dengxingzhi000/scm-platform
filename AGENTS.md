# AGENTS.md

## Build & Run

Parent POM: `com.scm.parent/pom.xml` — there is no root pom; always pass `-f com.scm.parent/pom.xml`. Modules are referenced from it via `../scm-*` paths.

```bash
mvn clean install -f com.scm.parent/pom.xml                     # full build
mvn clean install -DskipTests -f com.scm.parent/pom.xml          # fast
mvn clean package -pl scm-order/service -am -f com.scm.parent/pom.xml  # single module
mvn test -pl scm-order/service -f com.scm.parent/pom.xml         # single module tests
mvn test -Dtest=OrderServiceTest -pl scm-order/service -f com.scm.parent/pom.xml  # single test
mvn verify -Djacoco.skip=false -f com.scm.parent/pom.xml         # full check incl. coverage gates
```

- `jacoco.skip=true` is set in the parent POM, so plain `mvn verify` does NOT enforce coverage (gates: 70% line / 60% branch). Pass `-Djacoco.skip=false` when coverage matters.
- Maven/JDK are not on PATH by default on Windows dev machines — root `build.bat` pins GraalVM JDK 21 and a local Maven 3.9.12 path.
- Java 21, Spring Boot 4.x, Spring Cloud 2025, Jakarta namespace throughout.

Start order: `docker-compose up -d` → Gateway (8761) → Auth (8106) → System (8081) → business services (any order).

## Key Ports

Verified from each service's `application.yml`:

| Service | Port | Service | Port |
|---------|------|---------|------|
| Gateway | 8761 | Finance | 8208 |
| Auth | 8106 | Approval | 8209 |
| System | 8081 | Message | 8209 ⚠ |
| Product | 8201 ⚠ | Audit | 8210 |
| File | 8201 ⚠ | Notify | 8211 |
| Inventory | 8202 | Tenant | 8212 |
| Order | 8203 | Mall | 8301 |
| Warehouse | 8204 | Member | 8302 |
| Logistics | 8205 | Promotion | 8303 |
| Supplier | 8206 | Payment | 8304 |
| Purchase | 8207 | Order-Center | 8305 |
| | | Fulfillment | 8306 |
| | | Search | 8307 |

⚠ Real config collisions: `scm-file` vs `scm-product` both 8201; `scm-message` vs `scm-approval` both 8209. Don't run both of a pair locally without changing ports. Infra: Nacos 8848, Redis 6379, PostgreSQL 5432, XXL-Job admin 8088, Sentinel dashboard 8858, frontend 3000.

## Module Layout

- **Business services** (all have it): `scm-{name}/api/` (Dubbo RPC interfaces) + `scm-{name}/service/` (implementation). Includes approval, audit, file, finance, fulfillment, inventory, logistics, mall, member, message, notify, order, order-center, payment, product, promotion, purchase, search, supplier, system, tenant, warehouse.
- **Flat modules (no api/service split)**: `scm-auth`, `scm-gateway`.
- **Common**: `scm-common/` — `core`, `data`, `data-rw`, `data-rw-stub`, `cache` (Redis + Lua + locks + idempotency), `web`, `monitoring`, `integration` (Kafka/RabbitMQ), `decision-matrix`, `decision-engine`, `security/{core,api}`.
- **Frontend**: `scm-web/` (Next.js 15, separate npm project, not in parent POM).

## Package Naming

Actual base: `com.scmcloud.{module}` — e.g., `com.scmcloud.order.controller`, `com.scmcloud.common.response.ApiResponse`. GroupId: `com.scmcloud` (not `com.frog`).

## Critical Patterns

### Multi-Tenant Routing
`@DS("user"|"org"|"permission"|"approval"|"audit"|"notify")` (baomidou dynamic-datasource) → routes to `db_{name}` databases. Context in `TenantContextHolder` (`scm-common/core`). Every business table must have a `tenant_id` column — CI validates this on push (`scripts/db/ci_validate_tenant_id.sql`) and fails the build otherwise.

### Read-Write Separation
`@Master` / `@Slave` annotations live in `scm-common/data-rw` (stub variant in `data-rw-stub`). Query services → `@Slave`, command services → `@Master`.

### CQRS
Cross-database ops go through CQRS services under `service/query/` and `service/command/`. Cross-DB reads use the per-entity `*CrossDatabaseQueryService` classes (e.g., `UserCrossDatabaseQueryService`).

### Distributed Transactions
Seata AT mode via `@GlobalTransactional`. Every database needs an `undo_log` table (`scripts/db/microservices/020_undo_log_tables.sql`).

### Inventory Hot Path
Redis Lua scripts for atomic stock deduction (`scm-common/cache/src/main/resources/lua/inventory/deduct_stock.lua` etc.) — never query PostgreSQL in hot paths. Reservation timeout: 900s (15 min auto-release).

### Caching
Two-level: Caffeine (L1) → Redis (L2), implemented in `scm-common/cache`. Inventory stays Redis-only with short TTL (~30s).

### Order Status Transitions
Enum-driven state machine in `scm-order/.../domain/entity/OrderStatus.java` (`canTransitionTo()` / `validNextStatuses()`) — NOT Spring State Machine (that dependency is declared but unused). Flow: `PENDING_PAYMENT → PAID → PENDING_SHIP → SHIPPED → IN_TRANSIT → DELIVERED → COMPLETED`; `CANCELLED` only from `PENDING_PAYMENT`/`PAID`; `REFUNDING → REFUNDED` reachable from most paid states.

### Partitioned Tables (PostgreSQL)
UNIQUE constraints MUST include the partition key column:
- `ord_order(order_no, create_time)`, `inv_reservation(reservation_no, reserved_at)`, `sup_purchase_order(purchase_no, create_time)`
- App-level uniqueness enforced via Redis (order number generation prevents duplicates across partitions).

### Idempotency
Critical ops (inventory deduction, order creation) use request IDs stored in Redis with 24h expiry (`IdempotentAspect` in `scm-common/cache`).

## Database

```bash
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat
# Linux/Mac
export PGPASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh
```

Scripts: `scripts/db/microservices/` (001–027 SQL). Default credentials for init/CI: `admin` / `admin123` (note: `.env.example` ships `changeme` placeholders instead).

### Partition Management
- Auto-create: `scripts/db/partition/create-partitions.sh` (next 3 months)
- Cleanup: `scripts/db/partition/drop-old-partitions.sh` (default 24-month retention)
- Note: `inv_reservation` → `db_inventory`, `sup_purchase_order` → `db_supplier` (not all in `db_order`)

### Data Retention
`scripts/db/retention/apply-retention.sh` — audit 2yr, login 1yr, API logs 90d, notifications 6mo, order events 3yr.

## Frontend (`scm-web`)

```bash
cd scm-web && npm install && npm run dev   # http://localhost:3000, zh-CN/en-US
npm run lint                               # next lint
npm run test:e2e                           # Playwright
npm run generate:api                       # regenerate typed API clients (openapi-generator-cli, openapitools.json)
```

## K8s Deployment

Only 10 services have deployment manifests in `deploy/k8s/`: auth (+ canary), finance, gateway, inventory, logistics, product, purchase, supplier, system, warehouse. The e-commerce layer has none yet.

```bash
kubectl apply -f deploy/k8s/namespace.yml
kubectl apply -f deploy/k8s/configmap.yml
kubectl apply -f deploy/k8s/secrets.yml
kubectl apply -f deploy/k8s/scm-*-deployment.yml
kubectl apply -f deploy/k8s/scm-*-service.yml
```

Health check path: `/actuator/health` on each service's port.

## CI/CD

`.github/workflows/maven-build.yml` — triggers on push/PR to `master` or `develop`:
- PR: runs tests but with `-Dmaven.test.failure.ignore=true` (CI stays green on test failures).
- Push: full `mvn verify` + Codecov upload, plus schema validation requiring `tenant_id` on all listed tables across 14 DBs.
- Docker build matrix covers only 15 services (infra + supply-chain core, not member/promotion/payment/search/mall/etc.). Deploy jobs only roll auth/gateway/system images (dev on `develop`, prod on `master`).

`.github/workflows/backup.yml`: daily database backup at 2 AM.

## What NOT to Do

- No `javax.servlet` dependencies — use `jakarta.servlet` (enforced by maven-enforcer-plugin)
- No cross-DB logic outside `service/query/` + `service/command/` CQRS services
- No PostgreSQL inventory queries in hot paths — use Redis
- No UNIQUE constraints on partitioned tables without the partition key
- Don't start business services before Gateway + Auth + System are running
- No `org.springframework.kafka.support.serializer.JsonSerializer`/`JsonDeserializer`/`JsonSerde` — deprecated in Spring Kafka 4.0; use `JacksonJsonSerializer`/`JacksonJsonDeserializer` (see `scm-common/integration`)
