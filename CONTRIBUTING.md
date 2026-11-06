# Contributing to SCM Platform

Thank you for your interest in contributing to SCM Platform! This guide will help you get started.

## How to Contribute

There are many ways to contribute:

- **Report bugs** — Found something broken? [Open an issue](https://github.com/dengxingzhi000/cognora-aura-platform/issues/new?template=bug_report.md)
- **Suggest features** — Have an idea? [Request a feature](https://github.com/dengxingzhi000/cognora-aura-platform/issues/new?template=feature_request.md)
- **Submit code** — Fix bugs, add features, or improve documentation via pull requests
- **Improve docs** — Fix typos, add examples, or clarify existing documentation
- **Review PRs** — Help review open pull requests

## Development Setup

### Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 21+ | Virtual threads required |
| Maven | 3.8+ | |
| Node.js | 20+ | For the separate frontend repo (`cognora-aura-web`) |
| Docker | Latest | For infrastructure services |
| Docker Compose | Latest | Included with Docker Desktop |
| PostgreSQL | 16+ | Or use Docker Compose |
| Redis | 7+ | Or use Docker Compose |

### Clone & Setup

```bash
# 1. Fork the repository on GitHub, then clone your fork
git clone https://github.com/<your-username>/cognora-aura-platform.git
cd cognora-aura-platform

# 2. Add upstream remote
git remote add upstream https://github.com/dengxingzhi000/cognora-aura-platform.git

# 3. Start infrastructure services
docker-compose up -d

# 4. Initialize databases
# Windows
set PGPASSWORD=admin123 && cd scripts\db && init-all-databases.bat
# Linux/Mac
export PGPASSWORD=admin123 && cd scripts/db && ./init-all-databases.sh

# 5. Build the project
mvn clean install -DskipTests -f com.scm.parent/pom.xml

# 6. Start services (in order)
# Gateway → Auth → System → Business services
cd scm-gateway && mvn spring-boot:run
cd scm-auth && mvn spring-boot:run
cd scm-system/service && mvn spring-boot:run
cd scm-product/service && mvn spring-boot:run   # any business service
```

### Frontend Setup

The frontend is not part of this repository. It lives in
[`cognora-aura-web`](https://github.com/dengxingzhi000/cognora-aura-web):

```bash
git clone https://github.com/dengxingzhi000/cognora-aura-web.git
cd cognora-aura-web
npm install
npm run dev
```

The frontend runs at `http://localhost:3000` and talks to this repo's gateway
or its direct service ports. Backend contract changes must stay compatible with
the OpenAPI documents in `docs/api/`.

### Build Commands Reference

```bash
# Full build
mvn clean install -f com.scm.parent/pom.xml

# Fast build (skip tests)
mvn clean install -DskipTests -f com.scm.parent/pom.xml

# Single module
mvn clean package -pl scm-order/service -am -f com.scm.parent/pom.xml

# Run tests for a module
mvn test -pl scm-order/service -f com.scm.parent/pom.xml

# Run a single test class
mvn test -Dtest=OrderServiceTest -pl scm-order/service -f com.scm.parent/pom.xml

# Full CI verification (tests + JaCoCo coverage)
mvn verify -f com.scm.parent/pom.xml
```

> **Important:** Always use `-f com.scm.parent/pom.xml`. The repo root `pom.xml` is not the parent POM.

### Service Ports

| Service | Port |
|---------|------|
| API Gateway / Sentinel | 8761 / 8858 |
| Auth | 8106 |
| System | 8081 |
| Product | 8201 |
| Inventory | 8202 |
| Order | 8203 |
| Warehouse | 8204 |
| Logistics | 8205 |
| Supplier | 8206 |

## Coding Standards

### Language & Framework

- **Java 21** — Use virtual threads, records, pattern matching, sealed classes where appropriate
- **Spring Boot 4** + **Spring Cloud 2025**
- **Jakarta EE** — Use `jakarta.servlet`, never `javax.servlet` (enforced by maven-enforcer-plugin)

### Package Structure

```
com.cognora.{module}.{layer}
```

Examples:
- `com.cognora.scm.order.controller`
- `com.cognora.scm.order.service.impl`
- `com.cognora.scm.order.mapper`
- `com.cognora.platform.common.response.ApiResponse`

GroupId is `com.cognora` (not `com.frog`).

### Module Layout

- **Business services**: `scm-{name}/api/` (Dubbo RPC interfaces) + `scm-{name}/service/` (implementation)
- **Flat modules**: `scm-auth`, `scm-gateway` (no api/service split)
- **Common**: `scm-common/` — `core`, `data`, `web`, `monitoring`, `integration`, `security/core`, `security/api`

### Naming Conventions

| Type | Convention | Example |
|------|-----------|---------|
| Classes | PascalCase | `OrderService`, `InventoryMapper` |
| Methods | camelCase | `createOrder`, `deductStock` |
| Constants | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| Packages | lowercase | `com.cognora.scm.order.service` |
| Tables | snake_case with prefix | `ord_order`, `inv_stock` |
| Config files | kebab-case | `application-dev.yml` |

### Architecture Patterns to Follow

**Multi-Tenant Routing:**
```java
@DS("user")  // Routes to db_user
public class UserMapper extends BaseMapper<SysUser> { }
```

**Read-Write Separation:**
```java
@Slave  // Query → read replica
public List<Order> queryOrders(OrderQuery query) { }

@Master  // Command → primary
public void updateOrder(Order order) { }
```

**Distributed Transactions:**
```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public Order createOrder(OrderDTO dto) { ... }
```

**CQRS (in scm-system):**
Use services under `service/query/` and `service/command/` for cross-database operations. Do not use the deprecated `CrossDatabaseQueryService`.

**Inventory Hot Path:**
Use Redis Lua scripts for atomic stock deduction. Never query PostgreSQL in hot paths.

**Caching:**
Two-level: Caffeine (L1, 5-min TTL) → Redis (L2, 30-min TTL). Inventory uses Redis only with short TTL (~30s).

**Idempotency:**
Critical operations (inventory deduction, order creation) must use request IDs stored in Redis with 24h expiry.

**Partitioned Tables:**
UNIQUE constraints must include the partition key column. Use app-level uniqueness enforcement via Redis.

### What NOT to Do

- No `javax.servlet` — use `jakarta.servlet`
- No deprecated `CrossDatabaseQueryService` — use CQRS services
- No PostgreSQL inventory queries in hot paths — use Redis
- No UNIQUE constraints on partitioned tables without the partition key
- No starting business services before Gateway + Auth + System are running

## Commit Convention

This project follows [Conventional Commits](https://www.conventionalcommits.org/). All commit messages must match this format:

```
<type>(<scope>): <description>

[optional body]

[optional footer(s)]
```

### Types

| Type | Description |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `docs` | Documentation only changes |
| `style` | Code style (formatting, semicolons, etc.) — no logic change |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `perf` | Performance improvement |
| `test` | Adding or updating tests |
| `chore` | Build process, tooling, dependencies |
| `ci` | CI/CD configuration changes |
| `revert` | Reverts a previous commit |

### Scopes

Use the module name as scope:

- `order`, `inventory`, `product`, `warehouse`, `logistics`, `supplier`, `purchase`, `finance`
- `auth`, `gateway`, `system`, `tenant`, `approval`, `audit`, `notify`
- `common`, `web`, `deps` (for dependency updates)

### Examples

```
feat(order): add order cancellation with state machine validation
fix(inventory): prevent negative stock on concurrent deductions
docs(readme): update architecture diagram
refactor(common): extract tenant context into scm-common/core
test(order): add integration tests for order creation flow
chore(deps): bump spring-boot to 4.0.7
```

### Rules

- Description must be lowercase and imperative mood ("add" not "added")
- No period at the end of the description
- Body should explain **what** and **why**, not **how**
- Reference issues in the footer: `Closes #123`, `Fixes #456`

## Pull Request Process

### 1. Fork & Branch

```bash
# Sync with upstream
git fetch upstream
git checkout master
git merge upstream/master

# Create a feature branch
git checkout -b feature/amazing-feature
```

### Branch Naming

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feature/<description>` | `feature/order-cancellation` |
| Bug fix | `fix/<description>` | `fix/inventory-negative-stock` |
| Docs | `docs/<description>` | `docs/api-annotations` |
| Refactor | `refactor/<description>` | `refactor/tenant-context` |
| Test | `test/<description>` | `test/order-service` |

### 2. Make Changes & Test

```bash
# Make your changes, then run tests
mvn test -pl scm-order/service -f com.scm.parent/pom.xml

# Run full verification before pushing
mvn verify -f com.scm.parent/pom.xml
```

### 3. Commit & Push

```bash
git add .
git commit -m 'feat(order): add order cancellation with state machine validation'
git push origin feature/order-cancellation
```

### 4. Open a Pull Request

- Fill in the PR template completely
- Link related issues
- Describe **what** changed and **why**
- Include screenshots for UI changes
- Ensure CI passes (build, test, coverage, SonarCloud, OWASP)

### 5. Code Review

- Address all review comments
- Keep commits focused — squash if needed
- Maintainers may request changes before merging

### PR Requirements

- [ ] All tests pass
- [ ] Code coverage does not decrease
- [ ] No SonarCloud issues (critical/blocker)
- [ ] No OWASP dependency vulnerabilities
- [ ] Conventional commit messages
- [ ] Documentation updated (if applicable)

## Issue Guidelines

### Bug Report

When reporting a bug, include:

1. **Environment**: OS, JDK version, Maven version, Docker version
2. **Steps to reproduce**: Minimal steps to trigger the bug
3. **Expected behavior**: What should happen
4. **Actual behavior**: What actually happens
5. **Logs**: Relevant stack traces or error messages
6. **Screenshots**: If applicable

### Feature Request

When requesting a feature, include:

1. **Problem**: What problem does this solve?
2. **Proposed solution**: How should it work?
3. **Alternatives considered**: What other approaches did you consider?
4. **Additional context**: Mockups, examples, references

## Good First Issues

Looking for ways to contribute? These categories are great for first-time contributors:

| Category | Description | Difficulty |
|----------|-------------|------------|
| **Unit tests** | Add test coverage for existing services | Easy |
| **Documentation** | Improve Javadoc, add API annotations, fix typos | Easy |
| **Encoding fixes** | Fix character encoding issues in source files | Easy |
| **API annotations** | Add OpenAPI/Swagger annotations to controllers | Easy |
| **Code style** | Fix SonarCloud issues, improve readability | Easy |
| **Integration tests** | Write integration tests for API endpoints | Medium |
| **Performance** | Optimize queries, add caching where missing | Medium |

Look for issues labeled [`good first issue`](https://github.com/dengxingzhi000/cognora-aura-platform/labels/good%20first%20issue) on GitHub.

## Code Review Checklist

Reviewers will check:

### Correctness
- [ ] Does the code do what it claims?
- [ ] Are edge cases handled?
- [ ] Are error paths handled gracefully?

### Architecture
- [ ] Follows existing patterns (CQRS, read-write separation, multi-tenant routing)?
- [ ] Uses appropriate annotations (`@Master`, `@Slave`, `@DS`, `@GlobalTransactional`)?
- [ ] No hot-path PostgreSQL queries for inventory?

### Security
- [ ] No hardcoded credentials or secrets?
- [ ] Input validation present?
- [ ] SQL injection prevention (parameterized queries)?
- [ ] Proper authorization checks?

### Performance
- [ ] No N+1 queries?
- [ ] Appropriate caching used?
- [ ] Redis used for hot-path operations?

### Code Quality
- [ ] Readable and well-structured?
- [ ] No unnecessary comments (code should be self-documenting)?
- [ ] Follows naming conventions?
- [ ] No `javax.servlet` imports?

### Testing
- [ ] Unit tests for new logic?
- [ ] Integration tests for API changes?
- [ ] Tests cover happy path and error cases?

## License

SCM Platform is licensed under the [Apache License 2.0](./LICENSE).

### Contributor License Agreement (CLA)

By submitting a pull request, you agree that your contributions will be licensed under the Apache License 2.0.

For enterprise contributions (from companies or organizations), a signed CLA may be required. Contact the maintainers for details.

---

Questions? Open a [discussion](https://github.com/dengxingzhi000/cognora-aura-platform/discussions) on GitHub or reach out to the maintainers.
