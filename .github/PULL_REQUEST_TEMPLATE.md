## Description

Brief description of the changes.

## Type of Change

- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update
- [ ] Refactoring (no functional changes)

## Related Issues

Fixes #(issue number)

## Breaking Change Checklist (fill in if marked above)

- [ ] DB migration added (`V*__*.sql` under `scm-*/service/src/main/resources/db/migration/`)
- [ ] Migration listed in `CHANGELOG.md` with exact command to apply
- [ ] Dubbo `*Api` method signature changed → `service/rpc/*Impl` updated
- [ ] `@DS` value added/renamed → `scripts/db/ci_validate_tenant_id.sql` and Nacos config updated
- [ ] DTO/entity field type changed (e.g. `Long → String` tenantId) → downstream consumers notified
- [ ] Helm / K8s `env` / secret key changed → `deploy/k8s/configmap.yml` and `secrets.yml` updated
- [ ] Frontend typed client regenerated (`npm run generate:api`)
- [ ] Release notes / `CHANGELOG.md` `[BREAKING]` section updated

## Checklist

- [ ] My code follows the project's coding standards
- [ ] I have performed a self-review of my own code
- [ ] I have commented my code, particularly in hard-to-understand areas
- [ ] I have made corresponding changes to the documentation
- [ ] My changes generate no new warnings
- [ ] I have added tests that prove my fix is effective or that my feature works
- [ ] New and existing unit tests pass locally with my changes
- [ ] Any dependent changes have been merged and published
- [ ] Tenant isolation verified: every new/changed table has `tenant_id`; every new mapper uses correct `@DS` (or `@Master`/`@Slave`)

## Module(s) Affected

### Platform / Cross-cutting
- [ ] `scm-common` (shared libraries)
- [ ] `scm-gateway`
- [ ] `scm-auth`

### Supply-chain core
- [ ] `scm-system`
- [ ] `scm-product`
- [ ] `scm-inventory`
- [ ] `scm-order`
- [ ] `scm-warehouse`
- [ ] `scm-logistics`
- [ ] `scm-purchase`
- [ ] `scm-supplier`
- [ ] `scm-finance`
- [ ] `scm-file`
- [ ] `scm-document`

### Approval / Audit / Notify / Tenant
- [ ] `scm-approval`
- [ ] `scm-audit`
- [ ] `scm-notify`
- [ ] `scm-message`
- [ ] `scm-tenant`

### E-commerce layer
- [ ] `scm-mall`
- [ ] `scm-member`
- [ ] `scm-promotion`
- [ ] `scm-payment`
- [ ] `scm-order-center`
- [ ] `scm-fulfillment`
- [ ] `scm-search`
- [ ] `scm-analytics`

### Infra / Tooling / Docs
- [ ] `deploy/` (K8s / Helm / ArgoCD)
- [ ] `scripts/db/` (Flyway / init / partition / retention)
- [ ] `.github/` (workflows / templates)
- [ ] `docs/` (architecture / runbooks / specs)
- [ ] `scm-web` (frontend)

## Testing

Describe the tests that you ran to verify your changes.

## Screenshots (if applicable)

Add screenshots to show the changes.
