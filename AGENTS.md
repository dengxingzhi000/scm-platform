# AGENTS.md

Working notes for AI agents in this repository. Everything below was verified
against the code and config — if it conflicts with the code, trust the code and
fix this file in the same change.

## Build

**There is no root `pom.xml`.** Every Maven command needs
`-f com.scm.parent/pom.xml`; that parent POM declares the modules through
`../scm-*` paths.

```bash
mvn clean install -f com.scm.parent/pom.xml                    # full build
mvn clean install -DskipTests -f com.scm.parent/pom.xml        # fast build
mvn test -pl ../scm-order/service -f com.scm.parent/pom.xml    # one module's tests
mvn test -pl ../scm-order/service -Dtest=OrderServiceTest -f com.scm.parent/pom.xml
mvn verify -Djacoco.skip=false -f com.scm.parent/pom.xml       # coverage gates ON
```

### Maven gotchas

1. **`-pl` paths are relative to `com.scm.parent/`.** `-pl scm-order/service`
   fails with *"Could not find the selected project in the reactor"* — use
   `-pl ../scm-order/service` (this is what CI does), or `cd` into the module
   and run Maven there (its POM resolves the parent via `../pom.xml`).
2. **Fresh clone:** the `scm-common/*` artifacts are not in `~/.m2`, so add
   `-am` (or run a full `install` first) or dependency resolution fails.
3. **`jacoco.skip=true`** is set in the parent POM — plain `mvn verify` does *not*
   enforce the 70% line / 60% branch gates. Pass `-Djacoco.skip=false`.

### `spring-boot-maven-plugin` is skipped by default

The parent POM sets `<skip>true</skip>` on `spring-boot-maven-plugin` (so the
`scm-common/*` libraries are not repackaged into fat jars). Sixteen service
modules override it with `<skip>false</skip>`; **ten do not** and therefore
inherit `skip=true`:

`analytics` · `document` · `file` · `inventory` · `logistics` · `message` ·
`order` · `product` · `supplier` · `warehouse`

For those ten:

- `mvn spring-boot:run` **exits 0 without starting the app** (verified).
- `mvn package` produces a **plain jar, not an executable fat jar**, so
  `java -jar` and their Dockerfiles (`COPY service/target/*.jar app.jar`) fail.

Add `<skip>false</skip>` to that module's `spring-boot-maven-plugin`
configuration before packaging or running it.

### Toolchain

`mvn`/`java` are not on `PATH` on Windows dev machines. Root `build.bat` and
`mvnw.bat` pin `C:\Users\Deng\.jdks\graalvm-jdk-21.0.8` plus a local Maven
3.9.16 — those paths are machine-specific, so `mvnw.bat` only works on the
original dev box.

## Running the stack

```bash
docker-compose up -d                                             # infra
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat
```

Start order matters: **infra → Gateway (8761) → Auth (8106) → System (8081) →
business services.** A service started before Nacos is healthy cannot register;
one started before `scm-system` cannot resolve permission lookups over Dubbo.

## Ports

Read from each module's `application.yml`.

| Service | Port | Service | Port |
|---------|------|---------|------|
| Gateway | 8761 | Finance | 8208 |
| Auth | 8106 | Message | 8209 ⚠ |
| System | 8081 | Approval | 8209 ⚠ |
| Product | 8201 ⚠ | Audit | 8210 |
| File | 8201 ⚠ | Notify | 8211 |
| Inventory | 8202 | Tenant | 8212 |
| Order | 8203 | Document | 8213 |
| Warehouse | 8204 | Mall | 8301 |
| Logistics | 8205 | Member | 8302 |
| Supplier | 8206 | Promotion | 8303 |
| Purchase | 8207 | Payment | 8304 |
| | | Order-Center | 8305 |
| | | Fulfillment | 8306 |
| | | Search | 8307 |
| | | Analytics | 8308 |

Real collisions in the checked-in config — override `server.port` before running
both halves of a pair:

- `scm-file` ↔ `scm-product` — both 8201
- `scm-message` ↔ `scm-approval` — both 8209
- Grafana ↔ `scm-web` dev server — both 3000 (`docker-compose.yml` maps `3000:3000`)
- `schema-registry` ↔ `scm-system` — both 8081

Infra: Nacos 8848 (+9848/9849), Redis 6379, PostgreSQL 5432, PgBouncer 6432,
Kafka 9092/9093, RabbitMQ 5672/15672, Elasticsearch 9200, ClickHouse 8123,
Seata 8091, XXL-Job 8088, Sentinel 8858, Prometheus 9090, SkyWalking UI 8090.

## Module layout

- **Service modules** follow an `api/` + `service/` split: `api/` holds the
  Dubbo RPC interfaces and DTOs, `service/` is the deployable Spring Boot app.
- **Flat modules**: `scm-auth`, `scm-gateway` (one Maven module each).
- **`scm-common/`** — `core`, `data`, `data-rw`, `data-rw-stub`, `cache`, `web`,
  `monitoring`, `integration`, `decision-matrix`, `decision-engine`,
  `security/{core,api}`.
- **`scm-web/`** — Next.js 15 frontend, separate npm project, **not** in the
  parent POM.

Package base is `com.scmcloud.*` (e.g. `com.scmcloud.order.controller`,
`com.scmcloud.common.response.ApiResponse` in `scm-common/core`); groupId is
`com.scmcloud`. The Java packages were renamed away from `com.frog`, but the
**Spring config prefixes were not** — `@ConfigurationProperties` still use
`frog.security.headers`, `frog.security.sql-filter`,
`frog.security.https-redirect`, `frog.security.api-filter` and `frog.mybatis`.
New YAML must keep using `frog.*`.

## Patterns that shape the code

### Multi-tenant isolation (four layers, all in `scm-common/core`)

1. `TenantFilter` (`OncePerRequestFilter`, `HIGHEST_PRECEDENCE + 10`) reads the
   tenant UUID from the `X-Tenant-Id` header (or `Tenant-Id`, or the `tenantId`
   query param) and sets it on the context. If it is missing and
   `scm.tenant.required=true` (the default) it throws `TenantParseException`.
   It clears the context in a `finally`.
2. `TenantContextHolder` is a plain `ThreadLocal<UUID>`. Prefer
   `getRequiredTenantId()` — `getTenantId()` returns `null` silently.
3. `TenantInterceptor` (MyBatis) rewrites every SELECT/UPDATE/DELETE/INSERT to
   add `tenant_id = ?`. It skips the `tenant`, `tenant_package`,
   `tenant_subscription`, `tenant_resource_quota`, `tenant_config`,
   `tenant_feature` and `tenant_operation_log` tables. With
   `scm.tenant.fail-on-parse-error=true` (default), SQL it cannot parse throws
   `TenantParseException` instead of silently running unfiltered — `UNION` /
   `INTERSECT` / `EXCEPT` queries are handled, but anything else exotic will
   fail loudly.
4. `AsyncTenantAutoConfiguration` publishes a `tenantAwareTaskExecutor` bean
   that propagates the tenant context into `@Async` threads. Any other executor
   loses it — wrap work in `TenantContextHolder.executeInTenantContext(...)`.

Defaults (`failOnParseError=true`, `required=true`,
`excludePaths=/actuator/**,/v3/api-docs/**`) live in that auto-configuration;
no service overrides them.

### Datasource routing (`@DS`)

`@DS("user"|"org"|"permission"|"approval"|"audit"|"notify")` (baomidou
dynamic-datasource) routes to the matching `db_*` database. Only `scm-system`
uses it, and it is the only service with a `spring.datasource.dynamic` block
(`primary: user`, `strict: true` — an unknown `@DS` value throws). Every other
service has a single plain `spring.datasource.url` pointing at its own
`db_<service>`.

### Read-write separation

`@Master` / `@Slave` live in `scm-common/data-rw`
(`com.scmcloud.common.data.rw.annotation`); `scm-common/data-rw-stub` is a
no-op variant for editions without replicas. Query services → `@Slave`, command
services → `@Master`.

### CQRS

Cross-database work belongs in `service/query/` and `service/command/`. The
`*CrossDatabaseQueryService` classes exist only in `scm-system`
(`User`/`Role`/`Dept`/`PermissionCrossDatabaseQueryService`). There are no
cross-database joins anywhere in the codebase.

### Distributed transactions

Seata AT mode via `@GlobalTransactional`. Every database needs an `undo_log`
table (`scripts/db/microservices/020_undo_log_tables.sql`).

### Inventory hot path

Redis Lua only — `scm-common/cache/src/main/resources/lua/inventory/`
(`deduct_stock.lua`, `add_stock.lua`, `seckill.lua`). Never query PostgreSQL in
a hot path. Reservation timeout is 900s (auto-release).

### Caching

Two-level: Caffeine (L1) → Redis (L2) in `scm-common/cache`. Inventory stays
Redis-only with a short TTL (~30s).

### Order status transitions

Enum state machine in
`scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OrderStatus.java`
(`canTransitionTo()` / `validNextStatuses()`), enforced inside the aggregate
(`OrdOrder.transitionTo()`). **Not** Spring State Machine — that dependency is
declared in the parent POM but unused.

`PENDING_PAYMENT → PAID → PENDING_SHIP → SHIPPED → IN_TRANSIT → DELIVERED →
COMPLETED`; `CANCELLED` only from `PENDING_PAYMENT`/`PAID`; `REFUNDING` from
`PAID`..`DELIVERED`; `COMPLETED`/`CANCELLED`/`REFUNDED` are terminal.

For other entity types the generic path is `StatusValidator` in
`scm-common/web`, delegating to `StatusMachineDubboService` in `scm-system`.

### Partitioned tables (PostgreSQL)

UNIQUE constraints must include the partition key:
`ord_order(order_no, create_time)`, `inv_reservation(reservation_no, reserved_at)`,
`sup_purchase_order(purchase_no, create_time)`. Cross-partition uniqueness is
enforced in the application via Redis.

### Idempotency

Two aspects exist — use the one that matches your module:
`com.scmcloud.common.lock.IdempotentAspect` (`scm-common/cache`, SpEL key
expression) and `com.scmcloud.common.security.idempotent.IdempotentAspect`
(`scm-common/web`, `Idempotent-Token` header). Both use Redis with a TTL and
delete the key when the business call fails.

### Other config defaults worth knowing

- MyBatis-Plus soft delete is global: `logic-delete-field: deleted`,
  `logic-delete-value: 1`, `logic-not-delete-value: 0`.
- Kafka bootstrap env vars are inconsistent between services —
  `KAFKA_BOOTSTRAP` (default `localhost:9093`), `KAFKA_SERVERS` (default
  `localhost:9092`) and `KAFKA_HOST`/`KAFKA_PORT` all appear. `.env.example`
  only defines `KAFKA_SERVERS`.
- `scripts/rename-packages.ps1` is the one-off script used for the
  `com.frog` → `com.scmcloud` package move; there is no live rename workflow.

## Database

### Initialising

```bash
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat
# Linux/macOS — different env var names!
export PG_ADMIN_USER=admin PG_ADMIN_PASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh
```

The two scripts do **not** use the same variables: the `.bat` reads
`DB_HOST`/`DB_PORT`/`DB_USER` (default `admin`) plus `PGPASSWORD`; the `.sh`
reads `PG_HOST`/`PG_PORT`/`PG_ADMIN_USER` (default `postgres`) plus
`PG_ADMIN_PASSWORD`, which has **no default** — the script fails fast without
it. CI passes `PG_ADMIN_USER=admin` / `PG_ADMIN_PASSWORD=admin123`.

The `.bat` creates 15 databases (including `db_tenant`); the `.sh` creates 14 —
it omits `db_tenant`. Neither creates `db_document` or `db_analytics`; those
need a manual `CREATE DATABASE` plus their Flyway migrations.

### Bootstrap SQL vs Flyway

- `scripts/db/microservices/NNN_*.sql` — one-time bootstrap scripts for the
  older services (order, product, inventory, …). The numbering has gaps and
  duplicates (`022` and `028` each appear twice) and the init scripts list the
  files explicitly, so do not assume lexical order.
- **Per-service Flyway** at `scm-<name>/service/src/main/resources/db/migration/`
  (`V1__*.sql`). Flyway is enabled with `validate-on-migrate: true` and
  `out-of-order: false` — put new schema changes here, not in `scripts/db/`.
  Present for analytics, document, file, fulfillment, mall, member, message,
  order-center, payment and promotion; the older services have empty dirs.
  `scm-analytics` also has `resources/clickhouse/V1__*.sql`.

### Partition / retention / quality

```bash
scripts/db/partition/create-partitions.sh   # next 3 months
scripts/db/partition/drop-old-partitions.sh  # 24-month retention
scripts/db/retention/apply-retention.sh      # audit 2y, login 1y, api logs 90d, notify 6mo, order events 3y
scripts/db/quality/check-quality.sh
```

`inv_reservation` lives in `db_inventory` and `sup_purchase_order` in
`db_supplier` — not `db_order`.

## Frontend (`scm-web`)

Next.js 16.4 App Router / React 19 / TypeScript, `next-intl` for i18n. A
separate npm project — never in the parent POM.

**Target stack (the migration is in progress):**

| Layer | Choice | Rule |
|-------|--------|------|
| Styling | Tailwind CSS v4 | No second CSS-in-JS layer |
| UI | shadcn/ui (`src/components/ui`) | Component code is vendored; edit the source |
| Server state | TanStack Query v5 | One `QueryClient` in the client `QueryProvider`; never wrap query data in `'use cache'` |
| Client state | Zustand | UI state only (sidebar, theme, filter context) — no server data |
| Forms | React Hook Form + Zod | Long forms use `zodResolver` + `FormField` |
| Tables | TanStack Table v9 by default; AG Grid on demand | AG Grid only through the lazy `DataGrid` wrapper |
| Charts | Recharts (KPI), ECharts (complex), G6 (topology) | All lazy; ECharts imports only `echarts/core` |
| Tests | Vitest + Playwright | Unit tests for data transforms; E2E for core ledger paths |

Ant Design 6 is still present on the non-migrated screens (dashboard and
product have been migrated first). Do not add new Ant Design screens; port
them to shadcn/ui incrementally. `src/lib/design-tokens.css` remains the
single palette source for Ant Design, Tailwind and canvas charts.

```bash
cd scm-web
npm install
npm run dev          # http://localhost:3000
npm run lint         # `eslint .` — `next lint` was removed in Next 16
npm run typecheck    # TypeScript 7 (`@typescript/native`)
npm test             # Vitest (unit)
npm run test:coverage
npm run test:e2e     # Playwright
```

- Routes are locale-prefixed — `/{locale}/...` (`zh-CN`, `en-US`) — handled by
  `src/proxy.ts` (next-intl), which also redirects unauthenticated users
  to `/login` unless an `access_token` cookie is present. Next 16 renamed the
  `middleware.ts` convention to `proxy.ts` (the `middleware` export → `proxy`).
  `src/app/layout.tsx` owns `<html>`/`<body>`; `LocaleAttributes` sets
  `lang` inside `[locale]/layout.tsx` (never render a second `<html>`).
- Tailwind v4 is configured CSS-first: `postcss.config.mjs` +
  `src/app/globals.css` (`@theme inline` aliases the design tokens to shadcn
  variables; dark mode keys off `[data-theme="dark"]`). `components.json`
  lets `npx shadcn@latest add <component>` write directly into
  `src/components/ui/`.
- Lazy-loading is enforced by ESLint `no-restricted-imports`: bare `echarts`,
  `echarts-for-react`, `@antv/g6`, `ag-grid-community` and `ag-grid-react`
  are errors outside their wrappers (`src/components/charts`,
  `src/components/data-grid`, `src/lib/charts`). AG Grid CSS is imported
  statically in `src/app/layout.tsx`.
- Dashboard is a Server Component doing `prefetchQuery` + `HydrationBoundary`;
  its client half lives in `dashboard-client.tsx`. `dashboardService` is a local
  mock, so prefetch runs on the server today — when it calls the gateway, the
  prefetch must forward the auth/tenant cookies.
- Lint uses the **flat config** `eslint.config.mjs` (ESLint 10 dropped
  `.eslintrc`); `eslint-config-next@16` ships native flat-config arrays.
  `eslint-config-next/typescript` is deliberately not enabled — see the
  comment in `eslint.config.mjs`.
- TypeScript 7 and TypeScript 6 run side-by-side: `@typescript/native` aliases
  `typescript@7` and is the compiler behind `npm run typecheck`; `typescript`
  aliases `@typescript/typescript6` because `typescript-eslint` (bundled with
  `eslint-config-next@16`) still declares `typescript >=4.8.4 <6.1.0` and
  hard-fails on the TS 7 API. ESLint and Next's build-time check use the TS 6
  API; `npm run typecheck` is the TS 7 gate. Do not run `npx tsc` directly —
  npm's `.bin/tsc` may point at the TS 6 shim.
- API base URL: `NEXT_PUBLIC_API_BASE_URL` (`scm-web/.env.local` →
  `http://localhost:8761`), consumed by `src/lib/api/client.ts`. Gateway routes
  strip their first segment: auth is `/auth/api/auth/**` → `/api/auth/**`,
  system is `/system/api/system/**` → `/api/system/**`.
- Feature code lives in `src/features/<domain>/`; pages in
  `src/app/[locale]/(app)/<domain>/`.
- See `scm-web/README.md` for the stack contract and migration checklist.
- API clients: `src/lib/api/contracts/` holds backend DTOs and
  `src/lib/api/services/` holds gateway-aware auth/system calls;
  `src/lib/api/envelope.ts` unwraps `ApiResponse<T>`. System feature services
  map DTOs to UI models in `src/features/system/lib/mappers.ts`. The OpenAPI
  documents at `:8106` / `:8081` are available under `/api/public/v3/api-docs`
  and are not always running; when they are, `npm run generate:api`
  (openapi-generator-cli 7.12, `openapitools.json`) can refresh the generated
  clients. `orval.config.ts` exists but `orval` is not a dependency — that path
  is dead.
- Playwright (`playwright.config.ts`) auto-starts `npm run dev` on :3000 and
  runs Chromium, Firefox and WebKit.

## Testing

- Unit tests are JUnit 5 + Mockito. The parent POM sets surefire
  `<excludedGroups>integration</excludedGroups>`, so every `@Tag("integration")`
  test (Seata AT/TCC, XXL-Job, order lifecycle, product search) is **skipped by
  default**. Those need Docker/Testcontainers; `AbstractIntegrationTest` (in
  `scm-common/core`) starts PostgreSQL, Redis and Kafka containers, but its
  `db/init-test.sql` init script does not exist in the repo.
- Testcontainers is on **2.x**: the module artifacts are
  `testcontainers` / `testcontainers-postgresql` / `testcontainers-kafka`, and
  the container classes moved to `org.testcontainers.postgresql.*` /
  `org.testcontainers.kafka.*` (the old `org.testcontainers.containers.*`
  names are deprecated shims).
- The POM's `<excludedGroups>` wins over `-DexcludedGroups=`, so re-enabling
  integration tests means editing the parent POM or adding a profile.
- CI runs `mvn clean test-compile test` on PRs and `mvn clean verify` on
  pushes.

## Kubernetes

`deploy/k8s/` holds two manifest styles:

- `scm-<name>-deployment.yml` + `scm-<name>-service.yml` for 11 services
  (auth, document, finance, gateway, inventory, logistics, product, purchase,
  supplier, system, warehouse) plus an `scm-auth-canary` pair.
- Single-file `scm-<name>.yaml` for approval, audit, file, message, notify,
  order and tenant — these are **not** matched by a `scm-*-deployment.yml` glob.

No manifests exist for the e-commerce layer (member, promotion, payment,
order-center, fulfillment, search, analytics). Helm charts live in
`deploy/helm/`, ArgoCD in `deploy/argocd/`. Health endpoint:
`/actuator/health` on each service's port.

## CI

`.github/workflows/maven-build.yml` — on push/PR to `master`/`develop`:

- **PR**: `mvn clean test-compile test` — unit-test failures fail the job. The
  same job then runs the `scm-web` gates (`npm ci`, `npm run typecheck`,
  `npm run lint`, `npm test`, `npm run build`), so frontend and backend
  changes share the required **Build and Test** check.
- **Push**: `mvn clean verify` + Codecov upload. `verify` still skips JaCoCo
  (the parent sets `jacoco.skip=true`), so the coverage gates never run in CI.
- **Push only**: the databases are initialised and
  `scripts/db/ci_validate_tenant_id.sql` runs against **14** databases
  (`db_user`, `db_org`, `db_permission`, `db_product`, `db_inventory`,
  `db_order`, `db_warehouse`, `db_logistics`, `db_supplier`, `db_purchase`,
  `db_finance`, `db_audit`, `db_notify`, `db_approval`). The SQL file expects
  41 tables to carry `tenant_id`; `db_document` and `db_analytics` are **not**
  in the CI loop even though the SQL lists `doc_*` tables.
- Docker images are built for 15 services only (not member/promotion/payment/
  search/mall/…); deploy jobs roll only auth, gateway and system images (dev
  from `develop`, prod from `master`).

Other workflows: `codeql.yml`, `backup.yml` (daily 02:00 DB backup),
`release-docker.yml`, `deploy-gh-pages.yml`, `stale.yml`.

## What not to do

- No `javax.servlet` — use `jakarta.servlet` (enforced by maven-enforcer).
- No cross-database logic outside `service/query/` + `service/command/`.
- No PostgreSQL inventory queries in hot paths — Redis + Lua only.
- No UNIQUE constraint on a partitioned table without the partition key.
- Do not start business services before Gateway + Auth + System.
- Do not use `org.springframework.kafka.support.serializer.JsonSerializer` /
  `JsonDeserializer` / `JsonSerde` — removed in Spring Kafka 4.0; use
  `JacksonJsonSerializer` / `JacksonJsonDeserializer` (`scm-common/integration`).
- Do not add schema changes to `scripts/db/microservices/` for a service that
  has Flyway migrations — use `db/migration/V<n>__*.sql`.
- Do not rename `com.scmcloud.*` packages or the `frog.*` config prefixes
  without a repo-wide sweep.

## Where to read more

- `docs/README.md` — reading order (Chinese).
- `docs/design/ARCHITECTURE.md`, `docs/design/CQRS_ARCHITECTURE.md`,
  `docs/design/ADR.md`
- `docs/development/DEVELOPMENT_STANDARDS.md` — coding, config, API conventions
- `docs/multi-tenant/MULTI_TENANT_GUIDE.md`
- `docs/technical/API_DESIGN.md`, `docs/technical/DATABASE_DESIGN.md`
- `docs/operations/OPERATIONS_MANUAL.md` + `docs/operations/runbooks/`
- `README.md` — setup and architecture (accurate);
  `QUICK_START.md` is Chinese and partly stale (it omits
  `-f com.scm.parent/pom.xml` and links two doc paths that no longer exist).

Repo-local agent skills live under `.opencode/skills/` and are invoked with the
`skill` tool; `docs/opencode-setup.md` explains the intended workflow.
