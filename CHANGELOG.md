# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
