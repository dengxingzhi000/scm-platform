# AGENTS.md

## Build & Run

Parent POM: `com.scm.parent/pom.xml` — never use repo root pom. Always `-f com.scm.parent/pom.xml`.

```bash
mvn clean install -f com.scm.parent/pom.xml                     # full build
mvn clean install -DskipTests -f com.scm.parent/pom.xml          # fast
mvn clean package -pl scm-order/service -am -f com.scm.parent/pom.xml  # single module
mvn test -pl scm-order/service -f com.scm.parent/pom.xml         # single module tests
mvn test -Dtest=OrderServiceTest -pl scm-order/service -f com.scm.parent/pom.xml  # single test
mvn verify -f com.scm.parent/pom.xml                             # full CI check (tests + jacoco)
```

Services start order: `docker-compose up -d` → Gateway (8761) → Auth (8106) → System (8081) → business services (any order).

## Key Ports

| Service | Port |
|---------|------|
| Gateway / Sentinel | 8761 / 8858 |
| Auth | 8106 |
| System | 8081 |
| Product | 8201 |
| Inventory | 8202 |
| Order | 8203 |
| Warehouse | 8204 |
| Logistics | 8205 |
| Supplier | 8206 |
| Purchase | 8207 |
| Finance | 8208 |

## Package Naming

Actual base: `com.scmcloud.{module}` — e.g., `com.scmcloud.order.controller`, `com.scmcloud.common.response.ApiResponse`.
GroupId: `com.scmcloud` (not `com.frog`).

## Module Layout

- **Business services**: `scm-{name}/api/` (Dubbo RPC interfaces) + `scm-{name}/service/` (implementation)
- **Flat modules (no api/service split)**: `scm-auth`, `scm-gateway`
- **Common**: `scm-common/` — `core`, `data`, `web`, `monitoring`, `integration`, `security/core`, `security/api`

## Critical Patterns

### Multi-Tenant Routing
`@DS("user"|"org"|"permission")` (baomidou dynamic-datasource) → routes to `db_user`, `db_org`, `db_permission`. Context in `TenantContextHolder`.

### Read-Write Separation
`@Master` / `@Slave` annotations from `scm-common/data`. Query services → `@Slave`, command services → `@Master`.

### Distributed Transactions
Seata AT mode via `@GlobalTransactional`. Every database needs `undo_log` table (see `scripts/db/microservices/020_undo_log_tables.sql`).

### CQRS in scm-system
Cross-database ops use CQRS services under `service/query/` and `service/command/`. **Do NOT use** the deprecated `CrossDatabaseQueryService`.

### Inventory Hot Path
Redis Lua scripts for atomic stock deduction — never query PostgreSQL in hot path. Stock reservations auto-release after 15 minutes.

### Caching
Two-level: Caffeine (L1, 5-min TTL) → Redis (L2, 30-min TTL). Inventory: Redis only, short TTL (~30s).

### Order State Machine
Spring State Machine: `PENDING_PAYMENT → PAID → PENDING_SHIP → SHIPPED → IN_TRANSIT → DELIVERED → COMPLETED`, with `CANCELLED` from `PENDING_PAYMENT` or `PAID`.

### Partitioned Tables (PostgreSQL)
UNIQUE constraints MUST include the partition key column:
- `ord_order(order_no, create_time)`, `inv_reservation(reservation_no, reserved_at)`, `sup_purchase_order(purchase_no, create_time)`
- App-level uniqueness enforcement via Redis (e.g., order number generation preventing duplicates across partitions).

### Idempotency
Critical ops (inventory deduction, order creation) use request IDs stored in Redis with 24h expiry.

## Database

```bash
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat
# Linux/Mac
export PGPASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh
```

Scripts: `scripts/db/microservices/` (001-021 SQL). Default: `admin` / `admin123`.

### Partition Management
- Auto-create: `scripts/db/partition/create-partitions.sh` (next 3 months)
- Cleanup: `scripts/db/partition/drop-old-partitions.sh` (default 24-month retention)
- Note: `inv_reservation` → `db_inventory`, `sup_purchase_order` → `db_supplier` (not all in `db_order`)

### Data Retention
`scripts/db/retention/apply-retention.sh` — audit 2yr, login 1yr, API logs 90d, notifications 6mo, order events 3yr.

## Environment Variables

All have local-dev defaults. Key vars: `NACOS_SERVER`, `DB_HOST/PORT/USERNAME/PASSWORD`, `REDIS_HOST/PORT/PASSWORD`, `SEATA_SERVER_ADDR`.

## K8s Deployment

All 9 services have deployment + service manifests in `deploy/k8s/`. Deploy with:
```bash
kubectl apply -f deploy/k8s/namespace.yml
kubectl apply -f deploy/k8s/configmap.yml
kubectl apply -f deploy/k8s/secrets.yml
kubectl apply -f deploy/k8s/scm-*-deployment.yml
kubectl apply -f deploy/k8s/scm-*-service.yml
```

Health check path: `/actuator/health` on each service's port.

## CI/CD

`.github/workflows/maven-build.yml`: Build → Test → JaCoCo → SonarCloud → OWASP → Docker build. Triggers on push to `master` or `develop`.
`.github/workflows/backup.yml`: Daily database backup at 2 AM.

## What NOT to Do

- No `javax.servlet` — use `jakarta.servlet` (enforced by maven-enforcer-plugin)
- No deprecated `CrossDatabaseQueryService` — use CQRS services (`service/query/`, `service/command/`)
- No PostgreSQL inventory queries in hot paths — use Redis
- No UNIQUE constraints on partitioned tables without the partition key
- No starting business services before Gateway + Auth + System are running
- No `Wrappers.lambdaQuery()` without type parameter — use `Wrappers.lambdaQuery(Entity.class)` for correct lambda type inference
- No `org.springframework.kafka.support.serializer.JsonSerializer`/`JsonDeserializer`/`JsonSerde` — deprecated in Spring Kafka 4.0, use `JacksonJsonSerializer`/`JacksonJsonDeserializer`
