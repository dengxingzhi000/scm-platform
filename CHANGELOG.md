# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.2] - 2026-09-18

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
