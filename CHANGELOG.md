# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
