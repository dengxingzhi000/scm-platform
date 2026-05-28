# AGENTS.md

## Build & Run

Parent POM is at `com.scm.parent/pom.xml` — NOT at the repo root. Always use `-f com.scm.parent/pom.xml` when building from the project root.

```bash
# Build all modules
mvn clean install -f com.scm.parent/pom.xml

# Build without tests (faster, use for verification)
mvn clean install -DskipTests -f com.scm.parent/pom.xml

# Build a single module (example: order service)
mvn clean package -pl scm-order/service -am -f com.scm.parent/pom.xml

# Run tests for a single module
mvn test -pl scm-order/service -f com.scm.parent/pom.xml

# Run a single test class
mvn test -Dtest=OrderServiceTest -pl scm-order/service -f com.scm.parent/pom.xml
```

## Service Start Order

Infrastructure (docker-compose) → Gateway → Auth → System → business services (any order).

```bash
docker-compose up -d
# Gateway (8761), Auth (8106), System (8081), then business services
```

## Architecture Quick Reference

### Module Layout

- **Parent POM**: `com.scm.parent/pom.xml` (groupId: `com.frog`, artifactId: `scm-platform`)
- **Common modules**: `scm-common/` — `core`, `data`, `web`, `monitoring`, `integration`, `security/core`, `security/api`
- **Business services** follow `scm-{name}/api/` + `scm-{name}/service/` pattern
- **Auth** and **Gateway** are flat modules (no api/service split)

### Key Ports

| Service | Port |
|---------|------|
| Gateway | 8761 |
| Auth | 8106 |
| System | 8081 |
| Product | 8201 |
| Inventory | 8202 |
| Order | 8203 |
| Warehouse | 8204 |
| Logistics | 8205 |
| Supplier | 8206 |

### Package Naming

Base package: `com.frog.{module}` — e.g., `com.frog.order`, `com.frog.system`, `com.frog.inventory`
Common code: `com.frog.common` — e.g., `com.frog.common.response.ApiResponse`, `com.frog.common.util.UUIDv7Util`

## Critical Patterns

### Multi-Tenant Data Routing

Uses `@DS` annotation from `baomidou dynamic-datasource` to route to tenant-specific databases:
- `@DS("user")` → db_user, `@DS("org")` → db_org, `@DS("permission")` → db_permission
- Tenant context is in `TenantContextHolder`

### Read-Write Separation

`scm-common/data` provides automatic routing via `@Master` / `@Slave` annotations. Query services use `@Slave`, command services use `@Master`.

### Distributed Transactions

Seata AT mode. Entry points use `@GlobalTransactional`. Each database must have an `undo_log` table (see `scripts/db/microservices/020_undo_log_tables.sql`).

### CQRS in scm-system

Cross-database operations are split into:
- **Query services** (`service/query/`): `UserCrossDatabaseQueryService`, `RoleCrossDatabaseQueryService`, `DeptCrossDatabaseQueryService`, `PermissionCrossDatabaseQueryService`
- **Command services** (`service/command/`): `UserRoleCrossDatabaseCommandService`, `DeptRoleCrossDatabaseCommandService`

Do NOT use the deprecated `CrossDatabaseQueryService`.

### Inventory Operations

- Use Redis Lua scripts for atomic stock deduction — never query DB in hot path
- Stock reservations use 15-minute timeout with auto-release

### Partitioned Tables

Several tables use PostgreSQL range partitioning. UNIQUE constraints MUST include the partition key column:
- `ord_order` → `(order_no, create_time)`
- `inv_reservation` → `(reservation_no, reserved_at)`
- `sup_purchase_order` → `(purchase_no, create_time)`

## Database Setup

```bash
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat

# Linux/Mac
export PGPASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh
```

Scripts are in `scripts/db/microservices/` (001-021 SQL files). Default DB credentials: `admin` / `admin123`.

## Environment Variables

All services use env vars with defaults for local dev. Key ones:
- `NACOS_SERVER` (default: `localhost:8848`), `NACOS_NAMESPACE`
- `DB_HOST`, `DB_PORT`, `DB_USERNAME`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`
- `SEATA_SERVER_ADDR`

## CI/CD

GitHub Actions workflow at `.github/workflows/maven-build.yml`:
- Build → Test → Coverage (JaCoCo) → SonarCloud → OWASP dependency check → Docker build
- Docker images built on push to `master` or `develop`

## What NOT to Do

- Do not use `javax.servlet` — project uses Jakarta EE (`jakarta.servlet`)
- Do not use the deprecated `CrossDatabaseQueryService` — use CQRS services instead
- Do not query inventory from PostgreSQL in hot paths — use Redis
- Do not create UNIQUE constraints on partitioned tables without including the partition key
- Do not start business services before Gateway + Auth + System are running
