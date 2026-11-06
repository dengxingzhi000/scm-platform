# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Reserved for `v2.2.0` — every merged commit after `v2.1.0` (`5732ab1b`) through
`a2c584b2` (44 commits). The version number is deliberately not bumped yet.

### Added

- **Tenant** — tenant ownership enforcement and a platform-scope configuration
  query endpoint (#361).
- **Finance** — closed accounting period listing (#359); reconciliation diff
  types with transition rejection (#358); current accounting period plus
  status filtering (#353); payment-term state machine, reconciliation diff
  closure and fund-flow linkage validation (#347); ledger schema, fund-flow
  idempotency key and currency validation (#333); unified money precision,
  audit on money mutations and tenant enforcement on writes (#332).
- **Inventory** — moving weighted-average cost and transfer document pricing
  (#351); stock movement log, permission codes and reservation/movement query
  APIs (#344); Dubbo deduction idempotency and batch atomicity (#338); TCC
  exact deduction with tenant-level idempotency (#336); reservation persistence
  and conditional stock update (#330).
- **LMS/TMS** — OpenAPI snapshot drift CI plus nonce/circuit-breaker Redis
  cross-instance support (#349); logistics frontend integration gaps closed
  (#345); webhook signature verification, SLA/exception closure and carrier
  degradation (#339); waybill state machine and freight reconciliation engine
  (#334); tracking callback idempotency and tenant boundary validation (#331).
- **Auth / System** — session self-service, platform/tenant configuration and
  profile self-service endpoints (#340).
- **Product** — product-change outbox producer, restoring the ES index sync
  chain (#337).

### Fixed

- Audit time-range filters are pushed down and the delivery service id is
  configurable (#360).
- Waybill and tracking numbers are sanitized in the tracking log.
- Entity ids are sanitized in tracking and freight logs.
- Logged request values are sanitized and an internal filter NPE is guarded.
- The logistics callback path is allowed through platform JWT authentication
  (#341).
- Shared XXL-Job executor, member Flyway baseline and partition job corrected
  (#329).

### Changed

- Inventory `inv_log` is partitioned monthly and the OpenAPI drift check was
  extended (#357).
- The extracted `scm-web` frontend was removed from the monorepo (#352).
- The second source of truth for waybill status was removed (#335).

### Documentation

- Finance OpenAPI schemas expose `currency` on settlements and invoices
  (#362).
- The minimatch ReDoS risk in the dev-only lint toolchain is documented and
  accepted (#95).

### Tests

- `StepUpEvaluatorTest` runs in CI again (#313).

### Build & CI

- The daily backup schedule is disabled until real database credentials exist
  (#365).
- GitHub Actions bumped: `actions/setup-java` 4 → 6, `actions/labeler` 6 → 7,
  `actions/stale` 10 → 11, `docker/setup-buildx-action` 3.12.0 → 4.4.1 and
  `aws-actions/configure-aws-credentials` 4.3.1 → 6.3.0.

### Dependencies

- `com.google.guava:guava`, `com.rabbitmq:amqp-client` and
  `maven-enforcer-plugin` bumped in `com.scm.parent`.

## [2.1.0] - 2026-11-04

Covers every merged commit after `v2.0.0` (`8fed1884fd`) through `5732ab1b`
(PR #327). Financial changes are **not** part of this release; they land in
`v2.2.0`.

### Security

- **SQL injection scan no longer backtracks quadratically** — `SqlInjectionFilter`
  is bounded, so a long crafted query cannot pin a request thread.
- **Internal write channels are signed** — `InternalEndpointSignatureFilter`
  verifies the HMAC signature (`X-Timestamp`/`X-Nonce`/`X-Signature`/`X-App-Id`)
  on internal endpoints, and the audit write channel
  (`POST /sys-audit-log`) rejects unsigned writes with 403. The signing secret
  is resolved through one class for both signer and verifier; scm-audit refuses
  to start with a blank or placeholder secret.
- **Unregistered endpoints default to deny** — `ApiAccessControlFilter` returns
  403 with an explicit code, with a report-only fallback for rollout; the
  exemption list is configurable and internal/self-service routes carry
  ownership checks.
- **`search:index:manage` is platform-only** — the permission is granted to the
  platform super admin and no longer to tenant roles.

### Tenant isolation

- **Product search index carries `tenant_id`** — writes, queries and deletes
  are filtered by the authenticated tenant; `scripts/es/tenantize-products.sh`
  bootstraps/backfills the index.
- **Brands are tenant-scoped** — brand writes reject a foreign `tenantId`,
  updates cannot rewrite ownership, `/api/v1/prod-brand/{id}` resolves its
  permission by method + template, and `prod_brand.brand_code` becomes
  `UNIQUE (tenant_id, brand_code)` via an idempotent migration (verified
  against the real database, duplicate data aborts instead of being deleted).
- **Cache keys carry a tenant segment** — `TenantCacheKeys` is the single key
  factory (inventory detail/batch/check, reservation and index keys,
  `apiPermissions`, `permissionTree`, `userDetails`), and user-details keys are
  `<tenantId>:<username>` because usernames are only unique per tenant.
- **Platform console is gated** — the `menu:tenant%` menu permission is
  revoked from every role without `tenant:*` (migration `008`), the seed no
  longer grants it to tenant roles, and CI asserts the gate in PostgreSQL.

### Audit

- **Audit partitions are guaranteed** — `sys_audit_log` partitions are created
  ahead of time and retention is unified at 24 months.
- **Mojibake cleanup** — Chinese text lost to GBK double-encoding was restored,
  and a report-only scanner was added to CI.

### Operations

- **v2 deployment batch 1** — operations notes for the e-commerce, web and
  infrastructure layers, and the internal signature secret is passed to every
  service through `.env` / `scm-secrets`.
- **ES index bootstrap** — the tenant-aware product index can be created and
  backfilled with the checked-in script.

## [2.0.0] - 2026-11-03

Breaking change: the repository is now `cognora-aura-platform`, split into four
domain reactors, and every service identity follows the new artifact semantics.

### Changed

- **Repository layout** — modules moved under `platform/`, `scm/`, `ecom/` and
  `lms/`; the root POM artifactId is `cognora-aura-platform`.
- **Maven coordinates** — groupId `com.cognora`; `platform-common-*`,
  `platform-*` services, `ecom-*` services, `lms-tms*`, SCM stays `scm-*`;
  internal BOM is `com.cognora:cognora-bom:2.0.0`.
- **Java packages** — `com.cognora.platform.*`, `com.cognora.scm.*`,
  `com.cognora.ecom.*` and `com.cognora.lms.*`.
- **Configuration prefixes** — `scm.*` and `frog.*` became `cognora.*`
  (`cognora.tenant`, `cognora.cache`, `cognora.security`, `cognora.mybatis`, …).
- **Runtime identity** — `spring.application.name` / Nacos service IDs use
  `platform-gateway`, `platform-auth`, `platform-system`, `platform-*`,
  `ecom-*`, `lms-tms`; SCM keeps `scm-order` … `scm-finance`.
- **Gateway prefixes** — `/platform-auth/**`, `/platform-system/**`,
  `/platform-common/**`, `/<project>-<domain>/**`; the payment callback
  whitelist is `/ecom-payment/api/v1/payments/callback/**`.
- **Deployment descriptors** — compose service keys / containers, k8s manifests,
  Prometheus jobs, Seata tx-groups, XXL executor names and Kafka topics /
  consumer groups use the new names.
- **Frontend** — npm package `@cognora/aura-web`; added ecommerce payment,
  refund, order-center, fulfillment and tracking screens.

### Migration

- Replace old service IDs (`auth-service`, `gateway-service`,
  `system-service`, and the `scm-*` platform/ecommerce/logistics names) with
  their new names.
- Replace old gateway paths (`/auth/**`, `/system/**`, `/scm-<service>/**`)
  with `/platform-auth/**`, `/platform-system/**` and
  `/<project>-<domain>/**`.
- Rebuild **all** services and recreate containers together. Old and new images
  must not run mixed, because Nacos names, gateway paths, Dubbo application
  names and Kafka topic names all changed.

### Unchanged

- Database schema and table names.
- SCM service artifact IDs (`scm-order`, `scm-warehouse`, `scm-purchase`,
  `scm-supplier`, `scm-finance`).

## [Unreleased] - 2026-10-30

Fixes that make the running stack usable end to end: business APIs are
authorized again, the databases match the entities, notifications are produced
from order events, and audit records finally reach `db_audit`.

### Added

- **Audit delivery** — `@AuditLog` is now handled by a shared aspect in
  `scm-common/web` and reported to scm-audit over HTTP (`POST /sys-audit-log`),
  so `scm-system` and `scm-auth` records are persisted. `scm-audit` keeps the
  local persistence implementation; delivery failures only log a warning.
- **Event-driven notifications** — scm-notify consumes `cognora.ordorder` events
  (outbox → Kafka), maps status changes to in-app notifications and deduplicates
  by the stable outbox id (`business_key` + unique index, Flyway V2).
  `OrderStatusChangedEvent` now carries the buyer id.

  Topic migration: the outbox and message producers publish `cognora.<aggregate>`
  (previously `scm.<aggregate>`, with the message producer briefly adding an
  `.events` suffix). Only scm-notify (`cognora.ordorder`) and scm-analytics
  (`cognora.*`) subscribe, so the switch is not a rolling migration: messages
  left on `scm.*` — or on `cognora.<aggregate>.events` — are not consumed or
  backfilled and are discarded. Deploy notify/analytics together with the
  producers that emit the new topic.
- **Product list SKU counts** — the list loads the SKU catalog and groups counts
  per SPU instead of always rendering 0.
- **Seed suite** — `scripts/db/seed/` ships idempotent, fixed-UUID data for every
  screen (tenant, RBAC, product, inventory, orders, suppliers, warehouses,
  logistics, finance, purchase, notifications), including admin notifications.

### Fixed

- **Business APIs returned 401** — the load-balancer strategy beans were created
  once in the application context under the name "default", so service discovery
  never resolved `system-service`; they are now a per-client configuration and
  Dubbo registrations are filtered out of the instance list. Internal RestClient
  calls now forward the caller's JWT, tenant and device headers.
- **5xx masked as 401** — `/error` is permitted; error dispatches are anonymous
  because the JWT filter skips ERROR dispatch.
- **Tokens rejected by some services** — every service used a different Redis
  database, so the JWT fingerprint written by scm-auth was never found. All
  services now use database 0.
- **`uuid = character varying` failures** — String parameters are bound as
  unspecified types (`stringtype=unspecified`) on every datasource, fixing
  logistics ID lookups, supplier and order writes, and jsonb inserts.
- **`TIMESTAMPTZ` read into `LocalDateTime`** — added a type handler that reads
  both column types; the legacy schema scripts now define `TIMESTAMP` like the
  Flyway-created databases.
- **Soft delete mismatch** — `logic-delete-value`/`logic-not-delete-value` are
  now `true`/`false` (the columns are BOOLEAN), and `SysDeptMapper.xml` no
  longer hardcodes `deleted = 0`.
- **`@DS("org")` ignored** — dept writes ran in a plain Spring transaction that
  had already bound the primary connection; they now use `@DSTransactional`.
- **Outbox never published** — `OutboxPoller` was a component-scanned
  `@ConditionalOnBean`, which evaluates before auto-configuration and was always
  false; it is registered by `OutboxAutoConfiguration` instead.
- **Audit rows silently missing** — the only aspect lived inside scm-audit and
  was never scanned by the audited services; login/security-event writes left
  the primary key null (`save()` without `createLog()`), which also failed.
- **Supplier columns** — `sup_supplier.website` and the eleven audit/risk
  columns the entity expects are added to the schema and seed.

### Changed

- `docker-compose.yml` and every service datasource URL carry
  `stringtype=unspecified`; `scm-system` overrides its six named `@DS`
  datasources explicitly because they are defined in `application.yaml`.
- Notifications API no longer accepts `userId`; recipients come from the session.

### Known issues

- Service addresses are resolved once at startup, so restarting `scm-system`
  requires restarting the services that call it.
- `scm-product` health stays 503 without Elasticsearch and `scm-notify` without
  a real SMTP host; neither blocks the API.
- Audit entries produced before authentication exists (e.g. failed logins) fall
  back to the application log because the record endpoint requires a token.
- **The tenant SQL filter needs a schema migration on the platform tables.**
  `TenantInterceptor` is now registered for every service, so every
  SELECT/UPDATE/DELETE is rewritten with a `tenant_id` predicate. The business
  tables carry the column, but `sys_user`, `sys_user_oauth`, `webauthn_credential`,
  `sys_dept`, `sys_permission` and the RBAC join tables do not — the column is
  added by `scripts/db/migrations/004_transform_permission_system_multi_tenant.sql`,
  which `init-all-databases` never runs. On a database initialised that way the
  management screens fail with *column "tenant_id" does not exist*. Apply the
  migration, or add the columns to the bootstrap scripts, before using the
  management screens. The predicate is also equality-only, so the platform rows
  (`sys_role.tenant_id IS NULL` means "platform role, visible to every tenant")
  are filtered out; those tables need `tenant_id IS NULL OR tenant_id = ?`.
- **`API_SECRET_INTERNAL_SERVICE` is now mandatory.** scm-audit refuses to
  start without it, and every service that reports audit records signs with
  it. Set it to one random value of at least 32 characters on all of them;
  `docker-compose.yml` and the k8s manifests read it from `.env` / `scm-secrets`.

## [Unreleased] - 2026-11-03

API-contract hardening across the business services, plus the fixes a code
review of that work surfaced.

### Added

- **Append-only audit log** — the HTTP delete routes on the audit controllers
  are gone and `POST /sys-audit-log` is guarded by `AuditWriteRequestGuard`,
  which verifies the shared HMAC signature (and normalises the request path
  first, so `/sys-audit-log;x=1` cannot slip through). Rejections are counted
  in `security_audit_write_rejected_total` and logged at ERROR.
- **Tenant-scoped document and file access** — the document, template, file and
  version operations resolve the tenant from the request context instead of
  accepting it from the caller.
- **Order dashboard summary** — `GET /api/v1/orders/dashboard/summary`
  aggregates revenue, order counts, status counts and backlog in PostgreSQL
  instead of paging through orders in the browser.

### Fixed

- **The audit write channel was protected by a committed constant.** The
  signing secret resolved to `your-internal-secret-key`; signer and verifier
  now share one resolver (`InternalSignatureSecretResolver`) and scm-audit
  refuses to start when the secret is blank or still a placeholder.
- **An unauthenticated "mark as paid" RPC.** `PaymentDubboService` exposed
  `handlePaymentCallback`, which reached `applyPaid` with no signature, amount
  or channel check. It is deleted — the verified callback is now the only path
  to a paid payment.
- **Alipay callbacks could be replayed indefinitely.** The 5 minute replay
  window that WeChat had now applies to Alipay as well.
- **Payment amounts were neither normalised nor bounded.** The amount is
  stored at scale 2, an amount with more than two decimals is rejected, and
  the ceiling is a named constant.
- **The reservation routes leaked the exception text to the client.** The
  catch-and-rethrow wrappers are gone; the service exception propagates
  unchanged and the global handler returns a generic message with the trace id.
- **Flyway migrations could never run.** Spring Boot 4 moved the
  auto-configuration into `spring-boot-flyway`, so the eleven modules that
  owned migrations but declared only `flyway-core` silently skipped them. They
  now declare the starter, get an explicit `spring.flyway` block
  (`baseline-on-migrate`, `baseline-version: 0`) and are covered by a build
  guard that fails any module with migrations and no auto-configuration.
- **scm-file could not start on a bootstrap-created schema.** Its migration is
  idempotent now and it has its own Flyway history table (shared with
  scm-message).
- **scm-message stored the tenant as a BIGINT** while the platform writes a
  UUID string, so every message query failed. The column and the Java side
  are now `VARCHAR(36)` / `String`.
- **Three modules were red.** `scm-file`'s test sources did not compile, and
  the new `scm-logistics` and `scm-supplier` tests never reached the code they
  claimed to cover.

### Changed

- The business services moved their business rules out of the controllers and
  into the service layer: the route id is now authoritative, entity-to-DTO
  mapping happens in the query services, and the controllers only translate
  between HTTP and the service.
- `ci_validate_tenant_id.sql` covers the `db_permission` tables that carry a
  `tenant_id` column.

## [1.5.0] - 2026-10-26

Minor release that completes the e-commerce service layer and the analytics BI
path, and moves the frontend to the Next.js 16 / TypeScript 7 toolchain. No
database tables were removed; the new schema changes are additive.

### Added

- **Analytics query engine and governance** — declarative SQL generator with a
  field whitelist, tenant predicate, organisation scope, derived-metric
  substitution and a 10k row cap. Adds `POST /api/v1/analytics/query`,
  `AnalyticsDubboService`, idempotent DWD → DWS → ADS materialisation with
  XXL-Job handlers, lineage edges, quality rules/results and ClickHouse
  retention. Flyway V2 adds the governance schema.
- **Analytics BI dashboard** — dataset/dimension/metric pickers, result chart,
  paginated table, CSV export and typed TanStack Query hooks, with zh-CN and
  en-US copy.
- **Payment** — refund create/query/approve/reject with paid-payment and
  balance checks, Alipay/WeChat callback parsing, `PaymentDubboService`, and
  tenant-scoped Flyway V2 tables.
- **Promotion** — group-buy create/join driven by activity rules, capacity and
  expiry checks, idempotent participant tracking, and REST/Dubbo APIs.
- **Mall** — seller registration/query plus product reviews with rating and
  ownership validation, exposed through `MallDubboService`.
- **Fulfillment** — tracking event recording and queries, initial `SHIPPED`
  events on shipment transitions, and `FulfillmentDubboService`.
- **Search** — bulk/reindex/delete index management, `SearchDubboService`,
  category-aware similar-product lookup, and Elasticsearch mapping/analyzer
  fixes.
- **Order Center** — `OrderCenterDubboService` for status lookup and
  cancellation; shipping is now allowed from `PAID` or `PENDING_SHIP`.
- **Member tenant isolation** — `tenant_id` filling on member, address, level,
  points, tag and binding entities, with V2 columns/indexes.
- **Deployment** — opt-in `ecommerce` Docker Compose profile, `db_promotion`
  and `db_analytics` in both database init scripts, and a corrected PgBouncer
  dependency.
- **Web** — shadcn/ui + Tailwind 4 primitives, lazy ECharts/G6/AG Grid
  wrappers, shared theme tokens, Vitest/Testing Library setup, a redesigned
  dashboard, a product form migrated to React Hook Form + Zod, and a product
  ledger E2E test.
- **WebAuthn** — `WebAuthnRequestContext` record and dedicated registration and
  authentication controllers.

### Changed

- **Frontend toolchain** — Next.js 16.4.3, React 19.3.1, Ant Design 6.6,
  ESLint 10 flat config, `proxy.ts`, Zod 4, React Hook Form 7.91 and
  `@hookform/resolvers` 6. TypeScript 7.0.2 runs through
  `@typescript/native`, while `typescript` aliases the 6.0 API required by
  `typescript-eslint`; `npm run typecheck` is the TypeScript 7 gate.
- **Backend dependencies** — Spring Boot 4.1.2, Spring Cloud 2025.1.4,
  PostgreSQL driver 42.7.14, Testcontainers 2.0.5, XXL-Job 3.5.0 and
  Flyway 13.11.1.
- **CI** — the required **Build and Test** job now runs the `scm-web`
  typecheck, lint, unit tests and production build; Maven unit-test failures
  are no longer ignored on PRs. Adds `actions/setup-java@v6` and
  `actions/upload-artifact@v7`.
- **Documentation** — restyled README (zh-CN/en-US), analytics checkpoints and
  the frontend stack/migration contract.
- **Repository cleanup** — removed the legacy agent framework files and
  obsolete skill bundles.

### Fixed

- The `028` migration guards `CREATE INDEX` when a table has no `deleted`
  column, and CI schema validation now uses `ON_ERROR_STOP=1`.
- Analytics tests now expect the top-level `TenantParseException`.
- Elasticsearch analyzer, date-format and URI handling for product search.
- Dashboard KPI layout and chart theming.

### Upgrade notes

- Create `db_promotion` and `db_analytics`, then run the new Flyway V2
  migrations for analytics, member, order-center, payment, promotion, mall,
  fulfillment and search.
- E-commerce and analytics services are opt-in in Docker Compose through the
  `ecommerce` profile.
- Use `npm run typecheck` rather than `npx tsc`: npm may resolve the TypeScript
  6 shim that `@typescript/typescript6` installs for ESLint.
- Application artifact versions are unchanged; this release is identified by
  the `v1.5.0` tag.

## [1.4.3] - 2026-09-18

Patch release for `scm-common/core`. No API removals, no database changes — only
hardening, performance, and observability. All public methods preserve
backward-compatible signatures.

### Fixed

- **`ServiceException` now carries `HttpStatus`** — the previous
  `GlobalExceptionHandler` mapped the business error code (e.g. `40401`) as if it
  were an HTTP status code, so any non-HTTP-shaped business code was silently
  downgraded to `500`. `ServiceException` now exposes `getHttpStatus()` and the
  handler reads from there. Adds `ServiceException(Integer, HttpStatus, String)`
  and `ServiceException(ErrorCode[, String])` constructors; legacy constructors
  unchanged.
- **`TenantInterceptor` fail-fast on unparseable SQL** — the interceptor used to
  silently skip tenant_id injection when JSQLParser failed (logged a warning,
  then ran the original SQL), which is a data-isolation hole. Now configurable
  via `scm.tenant.fail-on-parse-error` (default `true`): the bad SQL becomes a
  `TenantParseException` that propagates as HTTP 500.
- **`TenantInterceptor` `UNION` / `INTERSECT` / `EXCEPT` support** — the previous
  `handleSelect` cast `select.getSelectBody()` directly to `PlainSelect`, which
  throws `ClassCastException` for `SetOperationList` queries (and falls through
  to the same silent skip as above). Now recurses into every branch.
- **`TenantFilter` honours `scm.tenant.exclude-paths`** — `OncePerRequestFilter`
  with `shouldNotFilter` so `/actuator/**`, `/v3/api-docs/**` and any other
  configured paths bypass tenant resolution. Missing tenant ID now throws
  `TenantParseException` when `scm.tenant.required=true` (default) instead of
  silently proceeding with a null context.
- **`TenantContextHolder.getTenantId()` no longer spams `WARN`** — downgraded to
  `DEBUG`; `getRequiredTenantId()` still throws.
- **`GlobalExceptionHandler` no longer HTML-escapes JSON error messages** —
  `HtmlUtils.htmlEscape` on response bodies was meaningless for JSON consumers
  and damaged i18n keys; removed.

### Performance

- **`TenantInterceptor` Caffeine cache for rewritten SQL** — keyed by
  `MappedStatement.id + tenantId + originalSql`, max 1024 entries, 10 min TTL.
  Avoids re-parsing the same SQL for hot mappers.
- **`PermissionChecker` Caffeine L1 cache** — 60 s TTL, max 10 000 users,
  covers `getUserPermissions` and `getUserRoles`. Eliminates the previous
  per-call remote `PermissionQueryService` round trip on the request hot path.

### Refactored

- **`com.scmcloud.common.tenant.TenantAwareEntity` deprecated** —
  `@Deprecated(since = "1.4.2", forRemoval = true)`; use
  `com.scmcloud.common.entity.TenantAwareEntity` which also carries audit
  fields, optimistic locking, and Snowflake ID.
- **`Idempotent.errorMessage` default now `idempotent.replay`** — aligned with
  `ErrorCode.IDEMPOTENT_REPLAY.i18nKey` so i18n tooling picks it up.
- **`TenantParseException` promoted to top-level class** —
  `TenantContextHolder.TenantNotFoundException` is now `@Deprecated`; the new
  shared exception is used by `TenantInterceptor`, `TenantFilter`, and
  `TenantContextHolder.getRequiredTenantId()`.
- **JSpecify `@NullMarked` package-info** on `com.scmcloud.common` — explicit
  nullness contract for the entire module going forward; existing APIs are
  not yet annotated individually.

### Tests

- **`scm-common/core` unit test count: 8 → 49** (+41). New coverage:
  - `TenantInterceptorTest` (8) — SELECT / UPDATE / DELETE / UNION / exclude
    table / parse error fail-fast / parse error skip / no-context skip
  - `TenantFilterTest` (7) — header / param / required-missing /
    optional-missing / invalid format / clear-after / exclude path
  - `TenantContextHolderTest` (6) — set/get / clear / required-missing /
    required-set / restore-after / clear-when-no-original
  - `GlobalExceptionHandlerTest` (9) — service exception status / default 500 /
    code propagation / 401 / 403 / 429 / business / illegal argument /
    validation field errors
  - `PermissionCheckerTest` (6) — basic permissions / roles / null args /
    cache hit / require permission throws
  - `ServiceExceptionTest` (5) — default / code / status / error-code / error-
    code-with-message

### Verification

- `mvn verify -pl scm-common/core` → **BUILD SUCCESS**, 49/49 tests pass.
- `mvn install -DskipTests -pl scm-common/*` → 13 sub-modules build cleanly.
- Downstream `mvn compile -pl scm-order/service -am` → **BUILD SUCCESS**
  (regression check on `ServiceException` signature addition).

## [1.4.0] - 2026-09-14

### Added

- **New `scm-analytics` module** (modular monolith) registered under `com.scm.parent`,
  port `8308`. ClickHouse-backed OLAP service with PostgreSQL metadata sidecar
  (`db_analytics`, Flyway-managed schema covering `analytics_metric`,
  `analytics_dimension`, `analytics_dataset`, `analytics_dataset_field`).
  Kafka consumer pulls from upstream topics for ODS ingestion.
- **ClickHouse infra in `docker-compose.yml`** plus `deploy/clickhouse/users.xml`
  for local dev alongside the existing PostgreSQL metadata store.

### Infrastructure

- **GitHub workflows** — `codeql.yml` (security scan), `labeler.yml` (auto-label
  PRs), `stale.yml` (stale issue/PR management), `release-docker.yml` (build &
  push GHCR images for every service with a `Dockerfile`, then publish the
  matching GitHub Release — fires on every `v*` tag push).
- **Auto-label rules** (`.github/labeler.yml`) drive per-module and per-area
  PR labels, feeding the auto-generated release notes categorisation in
  `.github/release.yml`.
- **PR template** — explicit Breaking-Change checklist + reorganised module list
  (Platform / Supply-chain core / Approval-Audit / E-commerce / Infra).
- **Issue chooser config** (`.github/ISSUE_TEMPLATE/config.yml`) routes
  Q&A → Discussions and security → private disclosure.

### Notes

- `OutboxRelayJob` and the per-service `OutboxEvent`/`OutboxMapper` were retired
  in favour of the existing `scm-common/integration/.../outbox/{OutboxService,
  OutboxPoller,OutboxEvent}` implementation. Each business service now runs a
  single `OutboxPoller` (`@ConditionalOnBean(KafkaMessagePublisher.class)`),
  publishing to `scm.<aggregateType>` with retry / dead-letter support.

## [1.4.1] - 2026-09-16

Patch release. No new features, no API changes — only dependency bumps, an
internal MyBatis-Plus import migration, and a handful of tenant-service fixes.

### Changed

- **Parent POM dependency bumps** (`com.scm.parent/pom.xml`):
  - `spring-boot` 4.0.6 → **4.1.1**
  - `mybatis-plus` 3.5.15 → **3.5.17** (artifact split now pulls
    `mybatis-plus-spring` for the Spring-aware compat layer)
  - `xxl-job` 3.3.1 → **3.4.2**
  - `org.projectlombok` 1.18.38 → **1.18.48**
  - `fastjson2` 2.0.53 → **2.0.65**
  - `flyway` 10.15.0 → **13.6.0**
  - `elasticsearch` 8.11.4 → **9.5.3**

### Refactored

- **MyBatis-Plus `IService` / `ServiceImpl` import migration** across 22 business
  modules (156 files, +156 / −156 lines):
  `com.baomidou.mybatisplus.extension.service.{IService,ServiceImpl}` →
  `com.baomidou.mybatisplus.spring.service.{IService,ServiceImpl}`.
  Modules: `scm-approval`, `scm-audit`, `scm-auth`, `scm-document`, `scm-file`,
  `scm-finance`, `scm-fulfillment`, `scm-inventory`, `scm-logistics`, `scm-mall`,
  `scm-member`, `scm-message`, `scm-notify`, `scm-order`, `scm-order-center`,
  `scm-payment`, `scm-product`, `scm-promotion`, `scm-purchase`, `scm-supplier`,
  `scm-system`, `scm-tenant`, `scm-warehouse`.

### Fixed

- **`scm-tenant` Mojibake javadoc repair** in `PlatformFeeCalculationJob` and
  `QuotaResetJob` — UTF-8 double-encoded Chinese class-level and method javadoc
  restored to readable text.
- **`TenantCommandService.createTenant`**: `UUID.randomUUID().toString()` →
  `UUIDv7Util.generateString()` to align with platform-wide time-ordered ID
  convention.
- **`TenantCommandService` insert/update**: guard with return-value check and
  downgrade success log to `warn` when no row is affected (previously silent).
- **`scm-tenant` cleanup**: drop unused `java.util.List` import in
  `TenantConfigServiceImpl`; remove redundant blank lines between class
  declaration and first field in `TenantResourceQuotaCommandService`,
  `TenantResourceQuotaController`, `TenantSubscriptionController`.

### Verification

`mvn clean compile -DskipTests -fae -f com.scm.parent/pom.xml` →
**BUILD SUCCESS** for all 88 modules (≈ 4 min).

## [1.3.0] - 2026-08-31

### Added

- **New `scm-document` module** (modular monolith) registered under `com.scm.parent`. Comprises three bounded
  contexts wired through Dubbo RPC and `scm-common-{data,web}` (#266):
  - **file**: `DocumentFileService` proxies byte upload / download / presigned-URL through `scm-file`'s
    `FileManageApi` + `FileQueryApi` (the new `byte[] upload` / `download` overloads added in this release).
  - **template**: `Docx4jTemplateEngine` + `TemplateRenderService` for DOCX template rendering with
    variable-schema validation via `TemplateSchemaService`.
  - **document**: `DocumentService` for document lifecycle, `DocDocumentAudit` for change-trail.
  - DB schema `V1_0_0__create_document_tables.sql` for `db_document` (7 tables, all carrying `tenant_id`).
- **K8s deployment manifests for `scm-document`** (#265): `deploy/k8s/scm-document-deployment.yml` +
  `scm-document-service.yml`, `scm-prod` namespace, port `8213`, replicas `2`, standard
  `scm-config` / `scm-secrets` `envFrom` consumption, `/actuator/health` probes.
- **CI `tenant_id` validation extended to `scm-document` tables** (#264): `doc_file_metadata`,
  `doc_template`, `doc_template_version`, `doc_template_schema`, `doc_document`,
  `doc_document_version`, `doc_document_audit` are now in the `expected_tables` array of
  `scripts/db/ci_validate_tenant_id.sql` so CI fails fast on regression.

### Changed

- **`scm-file` aligned with platform UUID-string convention** (#262):
  - `FileManageApi` / `FileQueryApi` switch `tenantId` parameter from `Long` to `String`. (**BREAKING**)
  - `FileMetadata` / `FileVersion` / `UploadTask` entity fields `tenantId` / `createBy` typed
    `Long` → `String`. (**BREAKING**)
  - DB migration `V1_0_1__tenant_id_uuid.sql` widens `sys_file_metadata`, `sys_file_version`,
    `sys_upload_task` `tenant_id` + `create_by` from `BIGINT` to `VARCHAR(36)` using
    `USING col::TEXT` (lossless for existing rows). (**BREAKING** for any downstream consumer
    that hand-wrote `Long` IDs.)
  - API gains `FileManageApi#upload(byte[], ...)` and `FileQueryApi#download(String, String)`
    for in-process / server-side flows (used by `scm-document` for render artifacts).
  - `UploadService` gains `uploadBytes(...)`, `FileUploadValidator` / `FileValidationException`
    guard rails, `ClamAVVirusScanner` + `NoOpVirusScanner` pluggable via `FileVirusScanner` SPI,
    and orphan-object rollback on metadata-save failure.
  - `StorageEngine` / `MinioStorageEngine`: typed-properties MinIO client, presigned URL expiry
    from `StorageConfig`, `download(InputStream)` signature added.
  - New RPC: `service.rpc.FileManageApiImpl` / `FileQueryApiImpl` expose Dubbo services;
    `service.download.FileDownloadService` centralises resolve / presign / open-stream.
  - New tests: `FileUploadValidatorTest`, `UploadServiceTest`.

### Documentation

- `OrderEventStore#append` javadoc: wrap the `REQUIRES_NEW` advisory onto its own line for
  readability at default javadoc width (#261).

## [1.0.0] - 2026-08-06

### Added

- Cloud-native, microservices-based supply chain management platform (22+ services).
- OAuth2 + JWT + WebAuthn passwordless authentication, RBAC with fine-grained data scope control.
- Distributed transactions via Seata (AT/TCC/Saga).
- Redis Lua atomic stock deduction with 15-minute auto-release reservations.
- Multi-tenant data routing (`@DS`) and read-write separation (`@Master`/`@Slave`).
- Full-text product search backed by Elasticsearch with Canal binlog sync.
- E-commerce decision engine (weighted fusion, intelligent pricing, A/B testing).
- CQRS refactor for `SysUserService` (`service/query/` + `service/command/`).
- Lua script center for atomic Redis operations.
- Helm charts + Kubernetes manifests + ArgoCD GitOps deployment for all services.
- GitHub Actions CI (build, test, JaCoCo, SonarCloud, OWASP, Docker), daily DB backup workflow.
- Apache 2.0 license, contributing guidelines, security policy, and this changelog.

### Changed

- System module service layer simplified and hardened (tenant-scoped caches, `UserCommandTemplate`).
- `PermissionExpiryTask` restructured; Dubbo `HopCountFilter` fixed for both ends.
- JWT secret now injected via `JWT_SECRET` environment variable (no hardcoded default).
- Plaintext dev credentials in Docker Compose, Helm values, K8s secrets, and Seata config
  replaced with overridable placeholders / environment variables.

### Security

- Remove hardcoded JWT signing key default from `scm-common/web` configuration.
- Replace plaintext passwords in `deploy/k8s/secrets.yml`, Helm `values*.yaml`, and
  `docker-compose.yml` with `CHANGE-ME` placeholders or `${VAR:-default}` overrides.
