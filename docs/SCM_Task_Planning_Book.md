# SCM Enterprise Platform
## Task Planning Book & Development Roadmap
### Principal Architect Review — June 2026

---

> **Scope of this document**
> Based on a full codebase audit of the 15-service Spring Cloud SCM platform (Java 21, Spring Boot 4, PostgreSQL, Redis, Kafka + RabbitMQ, Seata, Dubbo 3.x, Nacos, WebAuthn + TOTP + mTLS).
> Incorporates architectural corrections made during the review session:
> — Three thin services (approval / audit / notify) remain independent deployables.
> — RPC chain depth is the primary structural danger, ranked above the anemic domain model.
> — UUID v7 (time-ordered) is the current strategy; migration to BIGINT is data-driven, not preemptive.

---

## 1. System Maturity Assessment

| Dimension | Score | Rationale |
|---|---|---|
| Architecture | 6/10 | Clean business service boundaries; god-module `scm-common`; anemic domain; dead ShardingSphere code |
| Backend | 7/10 | Security layer is exceptional; messaging infra is production-grade; multi-tenant has 4 P0 gaps |
| Frontend | 0/10 | Not implemented |
| Security | 8/10 | mTLS, WebAuthn/FIDO2, TOTP, JWT rotation, API HMAC-SHA256-V2, IP ACL — best-in-class for v1 |
| Scalability | 4/10 | Seata AT global lock ceiling; 6-hop Dubbo chain; anemic domain blocks optimization |
| Observability | 7/10 | SkyWalking + ELK + Prometheus/Grafana + Sentinel; missing tenant-level KPI dashboards |
| User Experience | 1/10 | No UI; score is 1 because API design (ApiResponse, error codes) shows consumer thinking |
| AI Capability | 3/10 | AI tagging mentioned; no vector DB, no semantic search, no RAG pipeline |
| Testing | 2/10 | Isolated unit tests; no integration test framework; no load testing |
| DevOps | 3/10 | Docker Compose for local observability stack; no K8s, no CI/CD pipeline |
| Documentation | 2/10 | JavaDoc comments; no OpenAPI; no ADR; no runbook |

### Architecture — why 6/10

The 15-service decomposition is directionally correct. Business service boundaries
(order, inventory, product, warehouse, logistics, supplier, finance, purchase) are clean.
The cross-cutting layer (auth, system, tenant, audit, notify, approval) is correctly
separated and — after the review session — correctly kept separate.

What prevents a higher score:

- `scm-common` is a god module spanning 7 concern areas. Any change triggers a full
  rebuild of all 15 downstream services. Package boundaries and release cadences
  must be independent.
- The domain layer is completely anemic: 80+ `@Data` POJOs, zero behavior, zero value
  objects, zero domain events. `WebauthnCredential` is the only entity in the codebase
  with methods (`isAvailable()`, `isCounterValid()`, `updateUsage()`). It is the correct
  pattern; it must be the norm, not the exception.
- ShardingSphere dead code: `application-sharding.yaml` and `application-readwrite.yaml`
  reference MySQL driver (`com.mysql.cj.jdbc.Driver`) while the platform runs PostgreSQL.
  This is a maintenance liability and misleads new engineers.
- Multi-tenancy has 4 P0 implementation gaps (detailed in Section 3).

### Security — why 8/10

mTLS between gateway and auth service. WebAuthn/FIDO2 with webauthn4j including
credential counter validation and replay protection. TOTP with Redis-backed nonce
to prevent one-time token reuse. API signature HMAC-SHA256-V2 with timestamp window
and nonce. JWT refresh token rotation. IP access control with Redis-backed ACL.
This is genuinely advanced security for a v1 enterprise product.

What prevents 9/10: no RBAC management UI, no audit log visibility for the tenant
admin, no per-tenant security dashboards.

---

## 2. Gap Analysis

### Critical — Blocking Production Use

| # | Gap | Impact |
|---|---|---|
| G01 | Frontend — zero UI | Users cannot interact with any backend capability |
| G02 | Tenant provisioning API | Onboarding a customer requires manual DB seeding + Nacos config entry |
| G03 | Tenant context lost in async threads | Cross-tenant data leakage in any `@Async` or CompletableFuture path |
| G04 | INSERT does not auto-populate tenant_id | Human error = data leakage guaranteed over time |
| G05 | Cache keys exclude tenantId | Cross-tenant cache poisoning (two tenants with same user ID share a cache entry) |
| G06 | RPC chain depth — no enforcement | 6-hop sync Dubbo chain → cascading timeout under any real order load |
| G07 | API documentation | Frontend engineers and external integrators cannot work without OpenAPI |
| G08 | CI/CD pipeline | Every release is a manual risk event |

### Important — Within 90 Days

| # | Gap | Notes |
|---|---|---|
| G09 | Operation & audit logs (UI) | Required for enterprise compliance and security review |
| G10 | Notification center multi-channel | SMS, Email, DingTalk, WeCom, Webhook, Telegram with template management |
| G11 | Approval workflow (Flowable/BPMN) | Procurement approval, expense claim, quality inspection sign-off |
| G12 | File versioning | Contracts, purchase orders, quality reports require version history |
| G13 | Trash / recycle bin | Soft-delete lifecycle with configurable recovery window |
| G14 | Sharing system | Internal + external share links with expiry and permission scoping |
| G15 | Dictionary management UI | Centralized enum/status management; `sys_dict_type` tables exist but have no UI |
| G16 | State machine UI | Visualize order/purchase/logistics state transitions |
| G17 | Rich domain model (OrdOrder) | Zero business invariant enforcement at the Java layer |

### Optional — Enhancement

| # | Gap | Notes |
|---|---|---|
| G18 | OCR | Extract data from uploaded invoices, contracts, delivery notes |
| G19 | AI semantic search | Vector DB + embedding for "find documents similar to this contract" |
| G20 | Theme system | White-label per-tenant customization |
| G21 | Internationalization (i18n) | Chinese + English minimum for enterprise sales |
| G22 | Full-text search optimization | Elasticsearch synonym management, relevance tuning, field boosting |
| G23 | Data export | Excel/PDF export for orders, financial summaries, inventory reports |
| G24 | SSO / SAML | Enterprise single sign-on integration |
| G25 | Webhook outbound | ERP/CRM/OA integration hooks |

---

## 3. Technical Debt

### 3.1 Package Structure

**Problem:** `scm-common` contains 7 independent concern areas:
security primitives, messaging, caching, data read-write routing, observability
instrumentation, tenant context, and common utilities. Any change to messaging
triggers a full rebuild + redeployment of all 15 business services.

**Resolution:**

```
scm-security-core     ← JWT, signature, tenant context propagation
scm-messaging         ← Kafka producers/consumers, outbox, DLQ handling
scm-cache             ← two-level cache, TenantAwareCacheKeyGenerator
scm-data-rw           ← @DS routing, DataScopeInterceptor, MetaObjectHandler
scm-observability     ← SkyWalking filters, Sentinel adapters, MDC decorators
```

Each module has one concern, one potential team owner, and its own release cadence.
A change to caching does not force rebuilding the order service.

**Estimated effort:** 3 weeks (extraction + dependency update across 15 services).

---

### 3.2 Domain Boundaries

**Problem:** 80+ anemic `@Data` POJOs. Business rules scattered across `*ServiceImpl`
classes. Invariants enforced only at the PostgreSQL layer (CHECK constraints).
The only entity methods in the entire codebase are on `WebauthnCredential`
(`isAvailable()`, `isCounterValid()`, `updateUsage()`) — a correct outlier that
illustrates what the rest of the domain should look like.

**Resolution:** Introduce rich aggregate roots starting with `OrdOrder`.

```java
// Target design — invariants at the domain layer
public class OrdOrder extends TenantAwareEntity {

    private OrderStatus status;
    private List<OrdOrderItem> items;
    private Money totalAmount;

    // Factory
    public static OrdOrder create(OrderContext ctx) {
        OrdOrder order = new OrdOrder();
        order.status = OrderStatus.PENDING_PAYMENT;
        ctx.items().forEach(i -> order.addItem(i.skuId(), i.quantity(), i.unitPrice()));
        order.register(new OrderCreatedEvent(order.getId(), order.getTenantId(), ...));
        return order;
    }

    // Commands — enforce invariants, return domain events
    public void pay(PaymentResult result) {
        if (!this.status.canTransitionTo(OrderStatus.PAID))
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE, this.id);
        this.status = OrderStatus.PAID;
        this.register(new OrderPaidEvent(this.id, result.paymentNo(), result.amount()));
    }

    public void cancel(String reason, UUID operatorId) {
        if (!this.status.canTransitionTo(OrderStatus.CANCELLED))
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE, this.id);
        this.status = OrderStatus.CANCELLED;
        this.register(new OrderCancelledEvent(this.id, reason, operatorId));
    }

    // Aggregate manages its own items — no external service call
    public OrdOrderItem addItem(UUID skuId, Quantity quantity, Money unitPrice) {
        OrdOrderItem item = OrdOrderItem.of(this.id, skuId, quantity, unitPrice);
        this.items.add(item);
        this.totalAmount = this.totalAmount.add(unitPrice.multiply(quantity));
        return item;
    }
}
```

**Estimated effort:** 8 weeks for `OrdOrder` + `InvInventory` aggregates.
Full domain layer: 3–6 months across all bounded contexts.

---

### 3.3 CQRS Implementation

**Current state:** Partial CQRS in `scm-system` (read/write split for cross-DB queries).
Not applied systematically.

**Resolution:** Apply CQRS at the service boundary, not at the entity level.

- **Command service:** handles state-changing operations under Seata transaction scope.
  No complex joins. Returns command result (success/failure + event).
- **Query service:** handles read-only queries. No transaction. Uses denormalized read
  models. Can use Elasticsearch or PostgreSQL read replica.
- **Start with:** order queries — the most read-heavy surface.
  `OrderQueryService.findOrders(OrderQueryFilter filter)` reads from a dedicated
  `v_order_summary` view or Elasticsearch index, not from the write tables.

**Estimated effort:** 4 weeks (order + inventory query services).

---

### 3.4 Aggregate Design

**Problem:** `OrdOrder` and `OrdOrderItem` are saved by independent service calls.
No parent-child encapsulation. The repository can save an `OrdOrderItem` without
a parent `OrdOrder` — a data integrity hole that PostgreSQL FK alone cannot prevent
at the business rule level.

**Resolution:**

```java
// Aggregate root controls persistence of its children
public class OrdOrderRepository {
    public void save(OrdOrder order) {
        ordOrderMapper.insertOrUpdate(order);
        order.getItems().forEach(ordOrderItemMapper::insertOrUpdate);
        // Domain events published here, not in the service layer
        order.getDomainEvents().forEach(eventPublisher::publish);
        order.clearDomainEvents();
    }
}
```

---

### 3.5 Event Design

**Problem:** Only infrastructure events (`DataSyncEvent` for CDC replication).
Zero domain events. Cross-service communication happens exclusively via synchronous
Dubbo calls, enabling the dangerous deep RPC chains identified in Section 3.9.

**Resolution:**

```
Domain Events (transactional outbox pattern):
  OrderCreatedEvent      → consumed by: Finance, Logistics, Notify, Audit
  OrderPaidEvent         → consumed by: Notify, Finance (settlement trigger)
  OrderCancelledEvent    → consumed by: Inventory (release reservation), Notify
  OrderShippedEvent      → consumed by: Notify, Logistics
  StockDeductedEvent     → consumed by: Audit
  StockReplenishedEvent  → consumed by: Audit, Notify (restock alert)
```

Event publishing: same local DB transaction as the entity change (transactional outbox).
`outbox_event` table → `OutboxPoller` → Kafka → consumers.

The outbox table and poller are already drafted in `022_outbox_event.sql` and
the `OutboxService`/`OutboxPoller` classes. These need verification and polish,
not a full rewrite.

---

### 3.6 Cache Strategy

**Problem:** `TwoLevelCacheManager` (Caffeine L1 + Redis L2) is implemented and
working. Cache keys do not include `tenantId`. Two tenants with the same user ID
(`sys_user:userId:{uuid}`) share a single cache entry. Cross-tenant data exposure.

**Resolution:**

```java
@Component
public class TenantAwareCacheKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object target, Method method, Object... params) {
        String tenantId = TenantContextHolder.getTenantId();
        String baseKey = SimpleKeyGenerator.generateKey(params).toString();
        return tenantId + ":" + baseKey;
    }
}
```

Apply to all `@Cacheable`, `@CacheEvict`, `@CachePut` annotations across 15 services.
Add integration test: two tenants with the same user ID must return different cached
permission trees.

---

### 3.7 Distributed Lock

**Problem:** `DistributedLock` with Lua scripts, lease renewal, and metrics is
well-implemented. Developers must manually call `executeWithLock()` — high friction,
easy to forget on new code paths.

**Resolution:** AOP-based declarative annotation:

```java
@DistributedLock(key = "#orderId", ttl = 30, unit = TimeUnit.SECONDS)
public void processPayment(UUID orderId, PaymentResult result) { ... }

@DistributedLock(key = "'sku:' + #skuId", ttl = 10, unit = TimeUnit.SECONDS)
public void deductStock(UUID skuId, int quantity) { ... }
```

SpEL expression support for dynamic key construction. Lock acquisition failure
throws `LockAcquisitionException` with retry hints.

---

### 3.8 Idempotency

**Problem:** Redis `SETNX` idempotency exists in the Kafka consumer layer and TOTP
verification. Not generalized. Payment and stock deduction entry points have no
declarative idempotency protection.

**Resolution:** AOP annotation backed by Redis:

```java
@Idempotent(key = "#request.requestId", ttl = 300, unit = TimeUnit.SECONDS)
public OrderCreateResult createOrder(CreateOrderRequest request) { ... }

@Idempotent(key = "'deduct:' + #skuId + ':' + #requestId", ttl = 60)
public StockDeductResult deductStock(UUID skuId, int qty, String requestId) { ... }
```

Returns the cached result if the same `requestId` has been processed within the TTL.
Logs WARN with the duplicate `requestId` for monitoring.

---

### 3.9 RPC Chain Depth ← Primary Structural Danger

**Problem:** A supply chain order fulfillment can chain:

```
Order → Inventory → Warehouse → Product → Supplier → Finance → Logistics → Notify
```

That is 6–8 synchronous Dubbo hops under a single Seata global transaction.

**Failure math:**
- 99.5% availability per service × 8 hops = **96.1% order success rate**.
- A supply chain platform with 3.9% failure rate is not a product.

**Latency math:**
- 6 hops × 10ms Dubbo p99 = **60ms pure RPC overhead** before any business logic.
- Add Seata coordinator round-trips (register + phase-two) per branch: +40–80ms.
- Total: 100–140ms for a transaction that should complete in 20ms.

**Seata scope problem:**
- AT mode holds row locks in all 6 databases simultaneously until the final commit.
- A GC pause on any downstream service holds locks in every upstream database.
- Lock contention = write throughput ceiling for the entire platform.

**Resolution — 3-hop maximum rule:**

```
Hop 0  Gateway → Order
Hop 1  Order   → Inventory (TCC reserve — must be synchronous)
Hop 2  Order   → Warehouse (locate stock for the reservation)
─────────────────────────────────── commit Seata tx, emit domain event
Async  Kafka → Finance    (freight calculation)
Async  Kafka → Logistics  (waybill creation)
Async  Kafka → Notify     (order confirmation)
Async  Kafka → Audit      (operation log)
```

**Enforcement — Dubbo provider-side Filter:**

```java
@Activate(group = CommonConstants.PROVIDER)
public class HopCountFilter implements Filter {
    private static final String HOP_COUNT_KEY = "rpc-hop-count";
    private static final int MAX_HOPS = 3;

    @Override
    public Result invoke(Invoker<?> invoker, Invocation inv) throws RpcException {
        String raw = RpcContext.getServiceContext().getAttachment(HOP_COUNT_KEY);
        int hops = raw == null ? 1 : Integer.parseInt(raw) + 1;

        if (hops > MAX_HOPS) {
            throw new RpcException(
                "RPC chain depth exceeded: " + hops + " hops. " +
                "Max allowed: " + MAX_HOPS + ". " +
                "Caller: " + inv.getServiceName() + "#" + inv.getMethodName() +
                ". Use domain event (Kafka) for downstream notification.");
        }

        RpcContext.getClientAttachment().setAttachment(HOP_COUNT_KEY,
            String.valueOf(hops));
        return invoker.invoke(inv);
    }
}
```

Register in `META-INF/dubbo/org.apache.dubbo.rpc.Filter`.

**Product snapshot denormalization:** The `ord_order_item` table already has
`sku_name`, `attributes`, `selling_price` columns (snapshot pattern). Order creation
must populate these from the product service during the cart/checkout phase, not
during order commit. No hop to Product or Supplier at order creation time.

---

### 3.10 Exception Hierarchy

**Problem:** `BusinessException`, `ServiceException`, `RateLimitException`,
`UnauthorizedException` exist and are handled by `GlobalExceptionHandler`.
No business error code catalog. Frontend cannot distinguish "order not found"
from "inventory insufficient" without parsing error messages.

**Resolution:**

```java
public enum ErrorCode {
    // Order domain
    ORDER_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "order.not_found"),
    ORDER_NOT_PAYABLE(40901, HttpStatus.CONFLICT, "order.not_payable"),
    ORDER_NOT_CANCELLABLE(40902, HttpStatus.CONFLICT, "order.not_cancellable"),

    // Inventory domain
    INSUFFICIENT_STOCK(40910, HttpStatus.CONFLICT, "inventory.insufficient_stock"),
    STOCK_RESERVATION_EXPIRED(40911, HttpStatus.GONE, "inventory.reservation_expired"),

    // Tenant domain
    TENANT_SUSPENDED(40301, HttpStatus.FORBIDDEN, "tenant.suspended"),
    TENANT_QUOTA_EXCEEDED(42901, HttpStatus.TOO_MANY_REQUESTS, "tenant.quota_exceeded");

    ErrorCode(int code, HttpStatus httpStatus, String i18nKey) { ... }
}

// Usage
throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE, orderId);
```

Frontend receives: `{"code": 40902, "message": "Order cannot be cancelled in current state", "data": {"orderId": "..."}}`

---

### 3.11 Dictionary Design

**Current state:** `sys_dict_type` and `sys_dict_item` tables exist in migrations
`023_dict_and_status.sql` and `024_dict_seed_data.sql`. No Java-layer dictionary
service. Status codes are raw strings and integers embedded in Java constants.

**Resolution:**

```java
@Service
public class DictionaryService {
    // Warm on startup, TTL 1 hour, tenant-scoped cache key
    @Cacheable(value = "dict", key = "#tenantId + ':' + #dictCode")
    public List<DictItem> getItems(String tenantId, String dictCode) { ... }
}

// Usage in domain
OrderStatus.fromCode(dictService.getItems(tenantId, "ORDER_STATUS"));
```

Admin UI: dict type list → item management (label, value, sort, enabled/disabled).

---

### 3.12 State Machine Design

**Current state:** `sys_status_dict` and `sys_status_transition` tables exist.
Order state transitions are implicit if-else chains in `OrdOrderServiceImpl`.
No Java state machine engine backed by the database tables.

**Resolution:**

```java
@Component
public class StateEngine {

    public <T extends Enum<T>> void transition(
            UUID entityId, Class<T> domainClass,
            T currentState, T targetState,
            Runnable preAction, Runnable postAction) {

        boolean allowed = statusTransitionMapper.isTransitionAllowed(
            domainClass.getSimpleName(), currentState.name(), targetState.name());

        if (!allowed) throw new BusinessException(
            ErrorCode.INVALID_STATE_TRANSITION,
            currentState.name() + " → " + targetState.name());

        preAction.run();
        entityStatusMapper.updateStatus(entityId, targetState.name());
        postAction.run();
    }
}

// Domain usage
stateEngine.transition(orderId, OrderStatus.class,
    order.getStatus(), OrderStatus.PAID,
    () -> paymentService.confirmPayment(paymentResult),
    () -> eventPublisher.publish(new OrderPaidEvent(orderId, ...)));
```

---

### 3.13 Dubbo Service Granularity

**Problem:** Some Dubbo interfaces have 2–4 methods (too thin, too many round-trips
for related data). Some have 15+ (too fat, breaks ISP, forces mock complexity in tests).

**Principle:** One Dubbo interface per aggregate root.

```
OrderDubboService          ← create, cancel, pay, ship, query
InventoryDubboService      ← deduct, reserve, release, queryStock
ProductDubboService        ← querySpu, querySku, querySnapshot
WarehouseDubboService      ← locateStock, confirmShipment
```

Cross-aggregate queries (e.g., "order detail with product names") go through a
dedicated `OrderQueryDubboService` that returns a denormalized DTO, not through
chaining calls to `OrderDubboService` and then `ProductDubboService`.

---

### 3.14 UUID vs BIGINT (Monitoring Recommendation)

**Current state:** `UUIDv7Util.generate()` is in use. UUID v7 is time-ordered
(Unix timestamp in high bits), which materially reduces B+tree page splits compared
to random UUID v4. This is not the same concern as random UUID.

**Actual risk:**

| Property | UUID v7 | Snowflake BIGINT |
|---|---|---|
| Storage | 16 bytes | 8 bytes |
| Index node density | ~50% of BIGINT | Full |
| Insert order | Time-ordered (sequential within epoch) | Time-ordered |
| VACUUM pressure at 100M rows | Moderate | Low |
| FK join cost | Higher (128-bit compare) | Lower |
| Coordination required | None | Requires node ID assignment |

**The real high-risk index:** `webauthn_credential.credential_id VARCHAR(512)` — up
to 512 bytes as a unique index entry. More dangerous than any UUID primary key at scale.

**Recommendation:** Do not migrate proactively. Monitor and act on data:

```sql
-- Run weekly once row count > 10M on any table
SELECT relname, n_dead_tup, n_live_tup,
       round(n_dead_tup::numeric / nullif(n_live_tup,0) * 100, 2) AS dead_pct
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC
LIMIT 20;
```

If `ord_order`, `inv_log`, or `sys_audit_log` show `dead_pct > 5` despite tuned
autovacuum, migrate those specific tables to `BIGINT GENERATED BY DEFAULT AS IDENTITY`.

**Set immediately** (regardless of ID type):

```sql
ALTER TABLE ord_order
    SET (autovacuum_vacuum_scale_factor = 0.01,
         autovacuum_analyze_scale_factor = 0.005);
-- Apply same to: inv_log, sys_audit_log, outbox_event
```

---

### 3.15 Configuration Management

**Current state:** Nacos config center integrated. Sensitive values use environment
variable placeholders (`${DB_PASSWORD}`). Sentinel rules pushed via Nacos. No secret
rotation. No config change audit. No config schema validation.

**Resolution (phased):**

- **Phase 1 (now):** Nacos config change history — enable Nacos `config.history.count=50`.
  Log config push events to `sys_audit_log`.
- **Phase 2 (Q3):** HashiCorp Vault for secrets. Nacos references Vault-sourced values.
  30-day automatic rotation for DB credentials.
- **Phase 3 (Year 2):** Config schema validation on startup using JSON Schema.
  Fail-fast if required keys are missing or malformed before the service starts.

---

## 4. Frontend Priority Analysis

### Option A — Build Frontend First

**Advantages:**
- Demonstrates product value to stakeholders and customers immediately
- Surfaces API design problems (missing endpoints, inconsistent responses) while
  the backend is still easy to change
- Enables user testing and real feedback collection before investing further in backend
- Required for any commercial adoption or open-source community growth
- Resume value: full-stack product demonstration

**Disadvantages:**
- Four P0 multi-tenant gaps will produce **visible data leakage** under real user load.
  Building a UI on top of a leaky data isolation layer is not safe.
- Frontend built against an unstable API contract will require constant rework as
  the domain model is enriched (OrdOrder aggregate changes the API shape).
- Every new frontend screen surfaces a new API call that may fail due to missing
  tenant_id propagation.

### Option B — Continue Backend Capability Expansion

**Advantages:**
- Closes P0 safety gaps before real users interact with data
- Establishes stable API contracts (OpenAPI) for the frontend to build against reliably
- Tenant provisioning, rich domain events, and transactional outbox make the
  frontend experience deterministic rather than fragile
- Technical debt fixed now is cheaper than technical debt fixed under user pressure

**Disadvantages:**
- No user-visible progress for 2–3 months
- Harder to demonstrate value to non-technical stakeholders
- Harder to recruit frontend engineers without a UI to show

### Recommendation — Parallel Track, Phased

**This is not an either/or decision. The answer is both, at the right surfaces:**

| Weeks 1–4 | Backend P0 fixes (T001–T019) | Non-negotiable. Tenant safety before users. |
|---|---|---|
| Weeks 2–8 | Frontend admin shell (T066–T077) | Safe to build now: auth + RBAC are stable APIs. |
| Weeks 5–12 | Backend API surface (T045–T060) | OpenAPI, error codes, @Idempotent, state machine. |
| Weeks 9–16 | Backend domain enrichment (T029–T044) | OrdOrder aggregate, domain events, outbox. |
| Weeks 13–20 | Frontend business screens (T078–T087) | Built on now-stable domain model and API. |
| Weeks 17–24 | DevOps + testing (T051–T065, T088–T100) | CI/CD, K8s, Grafana, integration tests. |

**Rule:** do not build order/inventory/purchase UI until the OrdOrder aggregate and
domain events are stable. Do build login, user management, role/permission, and
tenant dashboard immediately — these surfaces are backed by the security layer,
which is the most stable and complete part of the backend.

---

## 5. Architecture Evolution

### Stage 1 — Distributed Monolith (Current)

The system has microservice deployment units but monolithic coupling:
`scm-common` god module, synchronous Dubbo chains up to 6 hops, Seata AT global scope
spanning multiple databases, no domain events decoupling downstream reactions.

It has the **operational complexity** of microservices without the **isolation benefits**.

**Action:** Decompose `scm-common`, enforce 3-hop rule, implement outbox pattern.
This work brings the system to the next stage without any infrastructure changes.

---

### Stage 2 — Well-bounded Microservices (Q1–Q2 2027)

- `scm-common` decomposed into 5 focused libraries
- 3-hop Dubbo chain enforced by `HopCountFilter`
- Domain events via transactional outbox replace Dubbo calls beyond hop 3
- Tenant context propagated via Dubbo attachment filter
- Each service owns its full domain: schema, cache namespace, event topic prefix

**Trigger:** When tenant provisioning is live and real tenants are being onboarded.

**New stack components:** None. This is architectural discipline, not infrastructure.

---

### Stage 3 — Event-driven Architecture (Q3–Q4 2027)

- Order-inventory hot path: Kafka saga replaces Seata AT global locks
- All post-transaction side effects are async Kafka consumers
- CQRS read models for order queries (Elasticsearch) and inventory summaries
- Dead Letter Queue monitoring with automated replay and alerting
- Finance/Logistics/Notify services become pure event consumers — no Dubbo exposure

**Trigger:** When order volume exceeds 5,000/hour and Seata AT shows measurable
lock contention (visible via `pg_locks` or Seata monitor).

**New stack components:** Kafka Streams (for aggregation), DLQ replay tooling.

---

### Stage 4 — Cloud Native (Year 2)

- Kubernetes with Helm charts for all 15 services
- Horizontal pod autoscaling on order and inventory services (CPU + custom metrics)
- PostgreSQL partitioning extended; Citus evaluated for per-tenant sharding if needed
- Redis Cluster mode (sentinel → cluster)
- Istio service mesh: mTLS at infrastructure level, traffic management, canary deployment
- GitOps with ArgoCD: every merge to `main` triggers a Helm upgrade
- Prometheus Operator + Grafana as code (dashboards in Git)

**Trigger:** When operational team exceeds 5 engineers and deployment frequency
exceeds weekly releases.

**New stack components:** Kubernetes, Helm 3, ArgoCD, Istio, Prometheus Operator,
Loki (log aggregation), Tempo (distributed tracing alternative to SkyWalking).

---

### Stage 5 — Serverless + AI-native (Year 3)

- Event-driven functions for notification routing, OCR, AI tagging
- Vector database (Milvus or Weaviate) for semantic document search
- LLM-assisted supply chain decision support (demand forecasting, anomaly detection)
- Multi-region active-active for global enterprise customers
- Spring AI / LangChain4j for RAG pipeline over document corpus

**Trigger:** When AI features become a primary commercial differentiation and a
significant portion of customer documents are stored (sufficient corpus for meaningful
embeddings).

**New stack components:** Milvus, Spring AI, LangChain4j, PostgreSQL + pgvector,
Python ML microservices for time-series forecasting.

---

## 6. Module Roadmap

### P0 — Must Have (Months 1–3)

| Module | Description | Value | Complexity | Effort |
|---|---|---|---|---|
| Multi-tenant P0 fixes | tenant_id propagation, auto-populate INSERT, cache isolation | Blocks all SaaS revenue | Medium | 3 weeks |
| Tenant provisioning API | Create tenant with DB schema, roles, Nacos config | Enables customer onboarding | High | 4 weeks |
| Frontend admin shell | Login, user management, role/permission, tenant dashboard | Visible product | Medium | 6 weeks |
| API documentation | OpenAPI 3.0, Swagger UI, TypeScript client generation | Developer onboarding | Low | 1 week |
| CI/CD pipeline | GitHub Actions → Docker → Kubernetes deploy | Release safety | Medium | 2 weeks |
| RPC hop-count enforcement | Dubbo HopCountFilter (provider + client side) | Platform reliability | Low | 1 week |

### P1 — Important (Months 3–6)

| Module | Description | Value | Complexity | Effort |
|---|---|---|---|---|
| Operation & audit log | Full API request/response audit with user attribution | Enterprise compliance | Medium | 3 weeks |
| Notification center | SMS, Email, DingTalk, WeCom, Webhook, template management | User engagement | High | 6 weeks |
| Dictionary management | Centralized enum/status management with admin UI | Development velocity | Low | 2 weeks |
| State machine engine | Java engine backed by `sys_status_transition` tables | Domain correctness | High | 4 weeks |
| Rich order aggregate | OrdOrder with `pay()`, `cancel()`, `ship()`, domain events | Architecture quality | High | 8 weeks |
| File versioning | Version history for contracts and purchase orders | Enterprise feature | Medium | 4 weeks |
| Trash / recycle bin | Soft-delete with configurable recovery window | User experience | Low | 2 weeks |
| @DistributedLock annotation | AOP declarative lock, SpEL key expression | Developer safety | Low | 1 week |
| @Idempotent annotation | AOP declarative idempotency, Redis backend | Data integrity | Low | 1 week |

### P2 — Enhancement (Months 6–12)

| Module | Description | Value | Complexity | Effort |
|---|---|---|---|---|
| Flowable BPMN workflow | Procurement approval, quality inspection, HR flows | Enterprise sales | Very High | 12 weeks |
| OCR integration | Invoice/contract data extraction | AI differentiation | High | 6 weeks |
| Sharing system | Internal/external links with expiry and permission scoping | Collaboration | Medium | 4 weeks |
| SSO / SAML 2.0 | Enterprise identity integration (Okta, Azure AD) | Enterprise sales | High | 6 weeks |
| Webhook outbound | ERP/CRM/OA integration hooks | Ecosystem | Medium | 3 weeks |
| Kafka saga migration | Replace Seata AT on order-inventory hot path | Scale ceiling | Very High | 10 weeks |
| i18n | Chinese + English switchable | Market reach | Medium | 3 weeks |

### P3 — Future (12+ Months)

| Module | Description | Value | Complexity | Effort |
|---|---|---|---|---|
| Semantic search | Milvus + embeddings for document similarity search | AI differentiation | Very High | 12 weeks |
| AI demand forecasting | Time-series models on order history | Market position | Very High | 16 weeks |
| Conversational BI | LLM query interface over supply chain data | Differentiation | Very High | 12 weeks |
| Multi-region active-active | Global enterprise customer support | Enterprise Tier | Very High | 20 weeks |
| White-label SaaS | Per-tenant theming, custom domain, branded portal | Commercial | High | 8 weeks |
| ISO 27001 / SOC2 | Compliance certification | Enterprise sales | Very High | 24 weeks |

---

## 7. Detailed Task Breakdown

### Epic 1: Multi-tenant Safety

#### Feature 1.1 — Tenant Context Propagation

*Story: Tenant context is automatically available in all execution contexts —
synchronous, asynchronous, and across Dubbo RPC boundaries.*

| Task | Est. |
|---|---|
| `TenantAwareMDCTaskDecorator` wrapping all `@Async` thread pools | 2d |
| `TaskDecorator` registration in `AsyncConfigurer` across all services | 1d |
| Dubbo server `TenantIdFilter`: reads `tenant-id` attachment, sets `TenantContextHolder` | 2d |
| Dubbo client `TenantIdFilter`: reads `TenantContextHolder`, writes attachment | 1d |
| Unit tests: filter with/without attachment, null tenant ID handling | 1d |
| Integration test: `@Async` method receives correct tenant ID | 1d |
| Integration test: Dubbo call propagates tenant ID to provider | 1d |

*Total: 9 person-days*

---

#### Feature 1.2 — Auto-populate tenant_id on INSERT

*Story: Every database INSERT automatically carries the correct tenant_id
without developer intervention or code review catching it.*

| Task | Est. |
|---|---|
| `TenantAwareEntity` base class with `@TableField(fill = INSERT)` `tenantId` | 1d |
| `AuditMetaObjectHandler`: reads `TenantContextHolder`, populates `tenantId`, `createdBy`, `createdTime` | 1d |
| Migrate `OrdOrder`, `OrdOrderItem`, `InvInventory`, `ProdSpu`, `ProdSku` to extend `TenantAwareEntity` | 2d |
| Migrate `WmsInbound`, `WmsOutbound`, `SupPurchaseOrder`, `FinSettlement` to `TenantAwareEntity` | 2d |
| CI SQL lint rule: fail if business table DDL lacks `tenant_id` column | 2d |
| Integration tests: verify `tenant_id` populated correctly on INSERT for 5 entities | 2d |

*Total: 10 person-days*

---

#### Feature 1.3 — Tenant-scoped Cache

*Story: Cache entries are isolated per tenant. Two tenants with the same user ID
receive different cached permission trees.*

| Task | Est. |
|---|---|
| `TenantAwareCacheKeyGenerator` prepending `{tenantId}:` to all Spring cache keys | 1d |
| Apply generator to `TwoLevelCacheManager` configuration | 0.5d |
| Apply generator to all `@Cacheable` annotations across all services | 2d |
| Cache eviction propagation respects tenant scope (pub/sub channel per tenant) | 1d |
| Integration test: tenant A and tenant B with same user ID receive different cached data | 1d |

*Total: 5.5 person-days*

---

#### Feature 1.4 — Tenant Provisioning API

*Story: Platform administrators can create a new tenant via API, which
automatically provisions the database schema, default roles, and Nacos config.*

| Task | Est. |
|---|---|
| `TenantProvisioningService.createTenant(TenantCreateRequest)` orchestration | 3d |
| Schema initialization: execute DDL migration for new tenant's logical namespace | 2d |
| Default role seeding: `TENANT_ADMIN`, `OPERATOR`, `VIEWER` with correct permissions | 2d |
| Nacos config registration: push tenant-specific config namespace | 1d |
| `TenantSuspensionService.suspend(tenantId)`: revoke JWT, close sessions | 2d |
| `TenantActivationService.activate(tenantId)`: restore access | 1d |
| REST endpoints: `POST /admin/tenants`, `PATCH /admin/tenants/{id}/status` | 1d |
| Integration tests: full provisioning → login → access → suspension cycle | 3d |

*Total: 15 person-days*

---

### Epic 2: RPC Reliability

#### Feature 2.1 — Hop-count Enforcement

*Story: No Dubbo call chain exceeds 3 hops from the gateway entry point.
Violations fail fast with a clear error message naming the offending service.*

| Task | Est. |
|---|---|
| `HopCountFilter` provider-side: read attachment, throw if `> MAX_HOPS` | 2d |
| `HopCountClientFilter` consumer-side: increment and forward | 1d |
| SPI registration: `META-INF/dubbo/org.apache.dubbo.rpc.Filter` | 0.5d |
| WARN log when hop count reaches 2 (early warning before breach) | 0.5d |
| Unit tests: 1 hop (pass), 2 hops (pass), 3 hops (pass), 4 hops (fail) | 1d |
| Document: service call graph annotated with hop depths | 2d |

*Total: 7 person-days*

---

#### Feature 2.2 — Transactional Outbox

*Story: Domain events are published exactly once, reliably, even if the Kafka
broker is temporarily unavailable.*

| Task | Est. |
|---|---|
| Verify and finalize `outbox_event` DDL (`022_outbox_event.sql`) | 0.5d |
| Verify `OutboxService.saveEvent()`: writes to outbox in same local transaction | 1d |
| Verify `OutboxPoller`: `SELECT FOR UPDATE SKIP LOCKED` → publish → mark PUBLISHED | 1d |
| Exponential backoff for FAILED events (2ˢ delay, max 32s, max 5 retries) | 1d |
| Cleanup job: delete PUBLISHED events older than 7 days | 0.5d |
| Alert: DLQ depth > 100 → DingTalk/WeCom notification | 1d |
| Integration test: entity save + event publish + Kafka consumer receipt | 3d |
| Load test: 10,000 events published, zero loss, correct order | 2d |

*Total: 10 person-days*

---

### Epic 3: Order Domain Enrichment

#### Feature 3.1 — Rich Order Aggregate Root

*Story: The OrdOrder aggregate enforces all order lifecycle invariants.
No order can transition to an invalid state. No item can be added to a
confirmed or cancelled order.*

| Task | Est. |
|---|---|
| `OrderStatus` enum with valid transitions (replaces raw String status) | 1d |
| `OrdOrder.create(OrderContext)`: factory method, initial state PENDING_PAYMENT | 2d |
| `OrdOrder.pay(PaymentResult)`: validate state, update, register domain event | 1.5d |
| `OrdOrder.cancel(String reason, UUID operatorId)`: validate, update, event | 1.5d |
| `OrdOrder.ship(WaybillInfo)`: validate PENDING_SHIP state | 1d |
| `OrdOrder.complete()`: validate IN_TRANSIT state | 1d |
| `OrdOrder.addItem(UUID skuId, Quantity qty, Money unitPrice)` | 1d |
| `OrdOrder.canTransitionTo(OrderStatus)` backed by `sys_status_transition` | 2d |
| Migrate `OrdOrderServiceImpl.createOrder()` to use aggregate | 3d |
| Migrate `OrdOrderServiceImpl.cancelOrder()` to use aggregate | 2d |
| `OrdOrderRepository.save(OrdOrder)`: saves aggregate + items + publishes events | 2d |
| Unit tests: invariants (cannot pay a cancelled order, cannot add item to paid order) | 3d |
| Integration test: full lifecycle create → pay → ship → complete | 2d |

*Total: 24 person-days*

---

#### Feature 3.2 — Order Domain Events

*Story: Every order state change publishes a domain event.
Downstream services (Finance, Logistics, Notify, Audit) react asynchronously
without any direct Dubbo dependency on the order service.*

| Task | Est. |
|---|---|
| `OrderCreatedEvent` (tenantId, orderId, customerId, items snapshot, totalAmount) | 1d |
| `OrderPaidEvent` (orderId, paymentNo, paidAmount, paidAt) | 0.5d |
| `OrderCancelledEvent` (orderId, reason, operatorId, cancelledAt) | 0.5d |
| `OrderShippedEvent` (orderId, waybillNo, carrier, estimatedDelivery) | 0.5d |
| Finance service: Kafka consumer → `OrderCreatedEvent` → freight calculation | 2d |
| Logistics service: Kafka consumer → `OrderCreatedEvent` → waybill creation | 2d |
| Notify service: Kafka consumer → `OrderPaidEvent` → email + DingTalk confirmation | 2d |
| Notify service: Kafka consumer → `OrderCancelledEvent` → cancellation notice | 1d |
| Audit service: Kafka consumer → all order events → operation log entry | 1d |
| Integration tests: each consumer receives correct event payload | 3d |

*Total: 13.5 person-days*

---

### Epic 4: Frontend Admin Shell

#### Feature 4.1 — Project Scaffold and Layout

| Task | Est. |
|---|---|
| React 18 + TypeScript + Vite project initialization | 0.5d |
| Ant Design Pro layout: sidebar, top header, breadcrumb, content area | 2d |
| OpenAPI client generation (TypeScript, auto-generated from Springdoc spec) | 1d |
| Axios interceptors: attach JWT Bearer, handle 401 (refresh or redirect to login) | 1d |
| Route configuration: public (login) vs. protected (requires auth) | 0.5d |

*Total: 5 person-days*

---

#### Feature 4.2 — Authentication UI

| Task | Est. |
|---|---|
| Login page: username + password form, validation, error states | 2d |
| MFA step: TOTP 6-digit code entry after password success | 1d |
| WebAuthn passkey registration flow (Credential Management API) | 3d |
| WebAuthn passkey authentication flow | 2d |
| JWT storage (memory + httpOnly cookie pattern), refresh on expiry | 2d |
| Logout: revoke refresh token, clear state | 0.5d |
| Unit tests: form validation, error message display | 2d |

*Total: 12.5 person-days*

---

#### Feature 4.3 — User & Permission Management

| Task | Est. |
|---|---|
| User list: paginated table with search (name, email, department, status) | 2d |
| User create/edit form: name, email, phone, department, role assignment | 2d |
| User enable/disable/delete actions with confirmation | 1d |
| Role list: create/edit/clone/delete | 2d |
| Permission tree: checkbox tree component with select-all per category | 3d |
| Data scope configuration per role (ALL / DEPARTMENT / OWN) | 2d |
| Department tree: CRUD, drag-to-reorder | 2d |
| User-role assignment: multi-select with search | 1d |

*Total: 15 person-days*

---

#### Feature 4.4 — Tenant Dashboard

| Task | Est. |
|---|---|
| Tenant list: status badge, plan, resource usage summary | 2d |
| Tenant detail: quota gauges (API calls, storage, users, orders) | 2d |
| Tenant provisioning form: name, plan, admin email, resource limits | 2d |
| Feature flag toggles per tenant (WebAuthn, AI tagging, SSO, etc.) | 2d |
| Suspension/activation actions with audit reason field | 1d |
| Tenant subscription timeline | 1d |

*Total: 10 person-days*

---

### Epic 5: Observability Enhancement

#### Feature 5.1 — Business KPI Dashboards

| Task | Est. |
|---|---|
| Grafana dashboard: orders/hour, revenue/day, cancellation rate (line + bar charts) | 2d |
| Grafana dashboard: inventory health — low stock count, stockout alerts, turnover rate | 2d |
| Grafana dashboard: per-tenant API usage vs. quota (gauge + trend) | 2d |
| Grafana dashboard: notification delivery rate per channel | 1d |
| Alert rules: order failure rate > 2%, DLQ depth > 100, P99 latency > 500ms | 2d |
| DingTalk/WeCom alert webhook integration | 1d |

*Total: 10 person-days*

---

## 8. Three-Year Roadmap

### Phase 1 — MVP (Months 1–6)

**Goal:** Production-safe, demo-able SCM platform with multi-tenant admin console.

| Milestone | Month | Deliverable |
|---|---|---|
| M1 | Month 1 | All 4 P0 multi-tenant gaps closed. RPC hop filter live. CI/CD pipeline running. |
| M2 | Month 2 | Tenant provisioning API. First real tenant onboarded without manual DB work. |
| M3 | Month 3 | Admin frontend: login, user management, role/permission, tenant dashboard. |
| M4 | Month 4 | Order management UI. Inventory dashboard. Product catalog browser. |
| M5 | Month 5 | Notification center (email + DingTalk). Operation audit logs with UI. |
| M6 | Month 6 | File management UI. Supplier management. First external beta customer. |

**New tech added:** React 18 + TypeScript, Ant Design Pro, Vite, Springdoc OpenAPI 3.0.

---

### Phase 2 — Enterprise Edition (Months 7–18)

**Goal:** Feature-complete enterprise SCM with workflow, compliance, and ERP integration.

| Milestone | Month | Deliverable |
|---|---|---|
| M7–9 | Months 7–9 | Flowable BPMN workflow engine. Procurement approval, quality inspection flows. |
| M9–12 | Months 9–12 | OCR invoice processing. SSO/SAML (Okta, Azure AD). |
| M12–15 | Months 12–15 | Webhook outbound. ERP adapters (SAP B1, Kingdee, Yonyou). |
| M15–18 | Months 15–18 | File versioning. Sharing system. White-label theming. i18n (EN + ZH). |

**New tech added:** Flowable 7.x, PaddleOCR, Spring Security SAML2, Apache POI (Excel export).

---

### Phase 3 — AI Enhancement (Months 18–30)

**Goal:** AI-assisted supply chain intelligence as primary differentiation.

| Milestone | Month | Deliverable |
|---|---|---|
| M18–21 | Months 18–21 | Milvus vector DB. Semantic document search ("find contracts similar to this one"). |
| M21–24 | Months 21–24 | LLM contract analysis (risk clause detection, anomaly flagging). |
| M24–27 | Months 24–27 | AI demand forecasting on order history (LSTM / Prophet). |
| M27–30 | Months 27–30 | Conversational BI: natural language queries over supply chain data. |

**New tech added:** Milvus / Weaviate, Spring AI, LangChain4j, PostgreSQL + pgvector,
Python ML microservices, Apache Spark (batch feature engineering).

---

### Phase 4 — Cloud Native (Months 24–36, parallel with Phase 3)

**Goal:** Kubernetes-native with horizontal autoscaling and zero-downtime deployment.

| Milestone | Month | Deliverable |
|---|---|---|
| M24–27 | Months 24–27 | Full K8s deployment. Helm chart for all 15 services. ArgoCD GitOps. |
| M27–30 | Months 27–30 | HPA on order/inventory services. Redis Cluster mode. Istio service mesh. |
| M30–33 | Months 30–33 | Canary deployment strategy. Chaos engineering (LitmusChaos). |
| M33–36 | Months 33–36 | Multi-region active-active. Global load balancing. Cross-region Kafka replication. |

**New tech added:** Kubernetes, Helm 3, ArgoCD, Istio, Prometheus Operator,
Loki, Tempo, LitmusChaos.

---

### Phase 5 — Commercial Product (Year 3+)

**Goal:** SaaS product with usage-based pricing, marketplace, and partner ecosystem.

| Area | Deliverable |
|---|---|
| Billing | Stripe/Paddle integration. Usage metering against `TenantResourceQuota`. Invoicing. |
| Marketplace | Partner integration app store (ERP connectors, logistics APIs). |
| Compliance | ISO 27001 audit. SOC2 Type II attestation. GDPR data residency options. |
| Support tiers | Community / Professional / Enterprise with SLA dashboards per tier. |
| Developer platform | Public API with rate-limit tiers. Webhook sandbox. Developer portal. |

---

## 9. Open Source Competitiveness

### vs. NextCloud

| | SCM Platform | NextCloud |
|---|---|---|
| **Primary domain** | Supply chain management | General file storage and groupware |
| **Multi-tenancy** | Native, granular quota management | Multi-instance required per tenant |
| **Security posture** | mTLS, WebAuthn, TOTP, HMAC signature | Basic + 2FA plugin, no mTLS standard |
| **Supply chain workflows** | Native (procurement, inventory, logistics) | None |
| **File collaboration** | Basic (upload, versioning, sharing) | Real-time co-editing (Collabora/OnlyOffice) |
| **Mobile / desktop sync** | Not implemented | Native iOS, Android, desktop sync clients |
| **Plugin ecosystem** | None yet | 250+ apps in marketplace |

**SCM Platform's unique angle:** The only open-source platform combining enterprise
file management with native supply chain lifecycle management (PO → inventory →
order → logistics → settlement) in a single multi-tenant SaaS architecture.

---

### vs. Immich

Not a direct competitor. Immich is a self-hosted photo/media management platform.
SCM Platform does not target media management.

The comparison is instructive for one specific design lesson: Immich has mobile-first
UX and exceptionally fast onboarding (< 5 minutes to first photo). SCM Platform's
onboarding is complex (15 services, Nacos, Seata, Kafka). The gap in deployment
simplicity is significant and must be addressed for open-source adoption.

**Action:** Provide a one-command Docker Compose startup that brings up a functional
demo with seeded test data.

---

### vs. PicList / Cloudreve

Both target individual users and SMBs, not enterprise. Neither has multi-tenant
isolation, supply chain workflows, distributed transactions, or enterprise security.

SCM Platform is 5–10× more complex to operate, which is both its strength
(enterprise-grade) and its weakness (high barrier to adoption for smaller teams).

**Positioning clarity:** Do not try to compete on simplicity. Compete on:
- Vertical integration (full supply chain lifecycle in one platform)
- Security compliance (enterprise audit and access control)
- Multi-tenant SaaS (managed service model for ISVs)

---

### Unique Selling Points

1. **Vertical supply chain integration** — from procurement to settlement in one platform.
   No competitor in the open-source space combines inventory management, order lifecycle,
   logistics tracking, supplier scoring, and financial settlement.

2. **Enterprise-grade security as standard** — mTLS, WebAuthn, TOTP, API signature,
   IP ACL, RBAC with data scope. These are paid add-ons in commercial products.

3. **Multi-tenant SaaS architecture** — designed for managed service deployment.
   Competitors require separate installations per customer.

4. **Java/Spring ecosystem** — enterprise Java teams can read, customize, and
   extend the codebase without learning a new language or framework.
   Go and Node.js alternatives are often opaque to enterprise Java teams.

---

## 10. Resume Competitiveness

**Overall score: 78 / 100**

### Technical Depth — 85/100

The security implementation (WebAuthn + TOTP + mTLS + JWT rotation + API HMAC-SHA256-V2
with replay protection) is genuinely advanced and rare in portfolio projects.
A full-stack security review interviewer will find multiple substantive topics here.

Read-write separation with 5 load balancer strategies, circuit breaker integration,
and health-check-aware routing demonstrates infrastructure sophistication beyond
most senior engineer portfolios.

Two-level caching (Caffeine L1 + Redis L2) with pub/sub invalidation across services
demonstrates distributed systems consistency understanding.

**What prevents 90+:** The domain layer. FAANG and Alibaba/ByteDance interviewers
will probe DDD depth and find 80+ anemic POJOs. This is a material gap for
principal architect roles where domain design is evaluated heavily.

### System Design — 80/100

15-service decomposition with clean business boundaries, PostgreSQL range partitioning,
TCC distributed transaction design, Kafka CDC pipeline, and transactional outbox
pattern — these are strong system design demonstrations covering breadth.

**What prevents 90+:** The absence of a Kafka saga implementation (Seata AT is the
standard but safe choice). Showing the migration plan from Seata AT to Kafka saga
on the order-inventory hot path, and explaining the tradeoffs at scale, demonstrates
the depth that distinguishes a principal architect from a senior engineer.

### Interview Value — 75/100

**Breadth is strong.** Any of these topics sustains a 30-minute deep dive:
multi-tenancy isolation design, Lua-based distributed lock with lease renewal,
WebAuthn credential management, TCC state machine, read-write separation strategies,
two-level cache invalidation across services, or Seata AT vs. TCC vs. Saga tradeoffs.

**Risk:** Without a frontend, there is no product story. Interviewers at
product-focused companies will ask "what does a user actually do?" and the answer
cannot be "they make API calls." The admin frontend is the minimum for interview
narrative completeness.

### Enterprise Applicability — 80/100

Operation audit logs, RBAC with data scope, multi-tenancy, quota management,
approval workflow planning, API rate limiting, IP access control — these are exactly
what enterprise software procurement checklist requires.

The security posture exceeds most commercial products at the same price point.

**Improvements to reach 90+:**

1. Implement 3–5 proper aggregate roots (OrdOrder, InvInventory minimum)
2. Add OpenAPI documentation with working Swagger UI
3. Add Kubernetes deployment manifests with Helm
4. Add 5–10 integration tests using Testcontainers
5. Deploy a public demo instance with realistic seed data
6. Write one architecture decision record (ADR) for each major technology choice

---

## 11. Next 100 Tasks

| Task ID | Task Name | Priority | Est. Days | Dependencies | Status |
|---|---|---|---|---|---|
| T001 | Fix `inv_tcc_reservation` DDL: replace MySQL `COMMENT` column syntax with PostgreSQL equivalent | P0 | 0.5 | — | TODO |
| T002 | Implement `TenantAwareMDCTaskDecorator` for all `@Async` thread pools | P0 | 2 | — | TODO |
| T003 | Register `TenantAwareMDCTaskDecorator` in `AsyncConfigurer` across all services | P0 | 1 | T002 | TODO |
| T004 | Dubbo server `TenantIdFilter`: read `tenant-id` attachment, set `TenantContextHolder` | P0 | 2 | T002 | TODO |
| T005 | Dubbo client `TenantIdFilter`: read `TenantContextHolder`, write attachment | P0 | 1 | T004 | TODO |
| T006 | Dubbo server `HopCountFilter`: read `rpc-hop-count`, throw if `> 3` | P0 | 2 | — | TODO |
| T007 | Dubbo client `HopCountFilter`: increment and forward `rpc-hop-count` | P0 | 1 | T006 | TODO |
| T008 | SPI registration: both filters in `META-INF/dubbo/org.apache.dubbo.rpc.Filter` | P0 | 0.5 | T006 | TODO |
| T009 | `TenantAwareEntity` base class with `@TableField(fill = INSERT)` `tenantId` | P0 | 1 | — | TODO |
| T010 | `AuditMetaObjectHandler`: read `TenantContextHolder`, populate `tenantId` + `createdBy` + `createdTime` | P0 | 1 | T009 | TODO |
| T011 | Migrate `OrdOrder` and `OrdOrderItem` to extend `TenantAwareEntity` | P0 | 1 | T009 | TODO |
| T012 | Migrate `InvInventory` and `InvLog` to extend `TenantAwareEntity` | P0 | 1 | T009 | TODO |
| T013 | Migrate `ProdSpu` and `ProdSku` to extend `TenantAwareEntity` | P0 | 1 | T009 | TODO |
| T014 | Migrate `WmsInbound` and `WmsOutbound` to extend `TenantAwareEntity` | P0 | 1 | T009 | TODO |
| T015 | Migrate `SupPurchaseOrder` to extend `TenantAwareEntity` | P0 | 1 | T009 | TODO |
| T016 | `TenantAwareCacheKeyGenerator`: prepend `{tenantId}:` to all Spring cache keys | P0 | 1 | — | TODO |
| T017 | Apply `TenantAwareCacheKeyGenerator` to `TwoLevelCacheManager` | P0 | 0.5 | T016 | TODO |
| T018 | Apply `TenantAwareCacheKeyGenerator` to all `@Cacheable` annotations across all services | P0 | 2 | T016 | TODO |
| T019 | CI SQL lint: fail build if business table DDL lacks `tenant_id` column | P0 | 2 | — | TODO |
| T020 | Unit tests: `HopCountFilter` at 1, 2, 3 (pass), 4 (throw) hops | P0 | 1 | T006 | TODO |
| T021 | Integration test: `@Async` method receives correct `tenantId` | P0 | 1 | T003 | TODO |
| T022 | Integration test: Dubbo call propagates `tenantId` to provider | P0 | 1 | T004 | TODO |
| T023 | Integration test: two tenants with same userId receive different cached permission trees | P0 | 2 | T018 | TODO |
| T024 | Tenant provisioning: `TenantProvisioningService.createTenant()` orchestration | P1 | 3 | T010 | TODO |
| T025 | Tenant provisioning: execute DDL schema initialization for new tenant | P1 | 2 | T024 | TODO |
| T026 | Tenant provisioning: seed default roles (`TENANT_ADMIN`, `OPERATOR`, `VIEWER`) | P1 | 2 | T024 | TODO |
| T027 | Tenant provisioning: push tenant-specific Nacos config namespace | P1 | 1 | T024 | TODO |
| T028 | Tenant suspension: revoke all JWT tokens, close WebSocket sessions | P1 | 2 | T024 | TODO |
| T029 | Verify and finalize `outbox_event` DDL (`022_outbox_event.sql`) | P1 | 0.5 | — | TODO |
| T030 | Verify `OutboxService.saveEvent()`: atomicity with entity write in same transaction | P1 | 1 | T029 | TODO |
| T031 | Verify `OutboxPoller`: `SELECT FOR UPDATE SKIP LOCKED` → publish → mark PUBLISHED | P1 | 1 | T030 | TODO |
| T032 | Outbox exponential backoff: FAILED events, 2ˢ delay, max 5 retries | P1 | 1 | T031 | TODO |
| T033 | Outbox cleanup job: delete PUBLISHED events older than 7 days | P1 | 0.5 | T031 | TODO |
| T034 | Integration test: entity save + event publish + Kafka consumer receipt (exactly once) | P1 | 3 | T031 | TODO |
| T035 | `OrderStatus` enum with valid transition declarations | P1 | 1 | — | TODO |
| T036 | `OrdOrder.create(OrderContext)`: factory, initial state PENDING_PAYMENT | P1 | 2 | T035 | TODO |
| T037 | `OrdOrder.pay(PaymentResult)`: state guard + domain event registration | P1 | 1.5 | T035 | TODO |
| T038 | `OrdOrder.cancel(String reason, UUID operatorId)`: state guard + event | P1 | 1.5 | T035 | TODO |
| T039 | `OrdOrder.ship(WaybillInfo)`: state guard + event | P1 | 1 | T035 | TODO |
| T040 | `OrdOrder.complete()`: state guard + event | P1 | 1 | T035 | TODO |
| T041 | `OrdOrder.addItem(UUID skuId, Quantity qty, Money unitPrice)` | P1 | 1 | T035 | TODO |
| T042 | `OrdOrder.canTransitionTo(OrderStatus)`: backed by `sys_status_transition` lookup | P1 | 2 | T035 | TODO |
| T043 | `OrdOrderRepository.save(OrdOrder)`: saves aggregate + items + publishes events | P1 | 2 | T036 | TODO |
| T044 | Migrate `OrdOrderServiceImpl.createOrder()` to use aggregate | P1 | 3 | T043 | TODO |
| T045 | Migrate `OrdOrderServiceImpl.cancelOrder()` to use aggregate | P1 | 2 | T038 | TODO |
| T046 | `OrderCreatedEvent` class (tenantId, orderId, customerId, items snapshot, totalAmount) | P1 | 1 | T036 | TODO |
| T047 | `OrderPaidEvent`, `OrderCancelledEvent`, `OrderShippedEvent` classes | P1 | 1 | T037 | TODO |
| T048 | Finance service: consume `OrderCreatedEvent` → freight calculation | P1 | 2 | T046,T031 | TODO |
| T049 | Logistics service: consume `OrderCreatedEvent` → waybill creation | P1 | 2 | T046,T031 | TODO |
| T050 | Notify service: consume `OrderPaidEvent` → email + DingTalk confirmation | P1 | 2 | T047,T031 | TODO |
| T051 | Audit service: consume all order events → operation log entry | P1 | 1 | T047,T031 | TODO |
| T052 | Springdoc OpenAPI 3.0 dependency + base config across all services | P1 | 1 | — | TODO |
| T053 | Annotate all controllers in `scm-auth` with `@Operation`, `@ApiResponse` | P1 | 1 | T052 | TODO |
| T054 | Annotate all controllers in `scm-order` with `@Operation`, `@ApiResponse` | P1 | 1 | T052 | TODO |
| T055 | Annotate all controllers in `scm-inventory`, `scm-product` | P1 | 1 | T052 | TODO |
| T056 | Generate TypeScript client from OpenAPI spec (openapi-generator-cli) | P1 | 1 | T053 | TODO |
| T057 | `ErrorCode` enum: order domain (ORDER_NOT_FOUND, ORDER_NOT_PAYABLE, ORDER_NOT_CANCELLABLE) | P1 | 1 | — | TODO |
| T058 | `ErrorCode` enum: inventory domain (INSUFFICIENT_STOCK, RESERVATION_EXPIRED) | P1 | 1 | — | TODO |
| T059 | `ErrorCode` enum: tenant domain (TENANT_SUSPENDED, TENANT_QUOTA_EXCEEDED) | P1 | 0.5 | — | TODO |
| T060 | GitHub Actions CI: compile, test, Docker build on every PR | P1 | 2 | — | TODO |
| T061 | GitHub Actions CD: deploy to staging on merge to `main` | P1 | 2 | T060 | TODO |
| T062 | `@DistributedLock` AOP annotation (SpEL key, TTL, unit, fail strategy) | P1 | 2 | — | TODO |
| T063 | `@Idempotent` AOP annotation (SpEL key, TTL, return cached result on duplicate) | P1 | 2 | — | TODO |
| T064 | Apply `@Idempotent` to order creation REST endpoint | P1 | 0.5 | T063 | TODO |
| T065 | Apply `@Idempotent` to stock deduction Dubbo service | P1 | 0.5 | T063 | TODO |
| T066 | State machine engine: `StateEngine.transition()` backed by `sys_status_transition` | P1 | 4 | — | TODO |
| T067 | `StateEngine` pre/post action hooks + domain event emission | P1 | 2 | T066 | TODO |
| T068 | Dictionary API: `GET /api/v1/dict/{dictCode}/items` with tenant-scoped cache | P1 | 2 | T018 | TODO |
| T069 | Frontend: React 18 + TypeScript + Vite scaffold | P1 | 0.5 | — | TODO |
| T070 | Frontend: Ant Design Pro layout (sidebar, header, breadcrumb, content) | P1 | 2 | T069 | TODO |
| T071 | Frontend: OpenAPI TypeScript client integration | P1 | 1 | T056,T070 | TODO |
| T072 | Frontend: Axios interceptors — JWT Bearer attach, 401 → token refresh | P1 | 1.5 | T071 | TODO |
| T073 | Frontend: Route guard (unauthenticated → /login redirect) | P1 | 0.5 | T072 | TODO |
| T074 | Frontend: Login page — username/password form, validation, error states | P1 | 2 | T073 | TODO |
| T075 | Frontend: TOTP MFA step (6-digit code entry after password success) | P1 | 1 | T074 | TODO |
| T076 | Frontend: User list — paginated table with search, status badge, actions | P1 | 2 | T074 | TODO |
| T077 | Frontend: User create/edit form — name, email, department, role assignment | P1 | 2 | T076 | TODO |
| T078 | Frontend: Role management — list, create, edit, clone, delete | P1 | 2 | T074 | TODO |
| T079 | Frontend: Permission tree — checkbox tree, select-all per category | P1 | 3 | T078 | TODO |
| T080 | Frontend: Department tree — CRUD, expand/collapse | P1 | 2 | T074 | TODO |
| T081 | Frontend: Tenant dashboard — quota gauges, subscription info, feature flags | P1 | 3 | T074 | TODO |
| T082 | Kubernetes Deployment manifest: `scm-auth`, `scm-gateway` | P1 | 2 | — | TODO |
| T083 | Kubernetes Deployment manifest: `scm-order`, `scm-inventory` | P1 | 1 | T082 | TODO |
| T084 | Kubernetes Deployment manifests: remaining 11 services | P1 | 3 | T082 | TODO |
| T085 | Helm chart: `cognora-aura-platform` umbrella chart with sub-charts per service | P1 | 4 | T082 | TODO |
| T086 | Helm values: `values-dev.yaml`, `values-staging.yaml`, `values-prod.yaml` | P1 | 2 | T085 | TODO |
| T087 | Grafana dashboard: orders/hour, revenue/day, cancellation rate | P2 | 2 | — | TODO |
| T088 | Grafana dashboard: inventory health — low stock, stockout count, turnover | P2 | 2 | — | TODO |
| T089 | Grafana dashboard: per-tenant API usage vs. quota | P2 | 2 | — | TODO |
| T090 | Alert rule: DLQ depth > 100 → DingTalk notification | P2 | 1 | — | TODO |
| T091 | Alert rule: tenant API quota > 90% → email + DingTalk warning | P2 | 1 | — | TODO |
| T092 | Testcontainers: PostgreSQL + Redis + Kafka base test configuration | P2 | 3 | — | TODO |
| T093 | Integration test: full order lifecycle — create → pay → ship → complete | P2 | 3 | T092,T044 | TODO |
| T094 | Integration test: TCC reserve → confirm → verify inventory deducted | P2 | 3 | T092 | TODO |
| T095 | Integration test: Kafka event published and consumed end-to-end | P2 | 3 | T092,T034 | TODO |
| T096 | Frontend: WebAuthn passkey registration flow | P2 | 3 | T074 | TODO |
| T097 | Frontend: WebAuthn passkey authentication flow | P2 | 2 | T096 | TODO |
| T098 | Notification center: email channel — Mailgun/SendGrid integration | P2 | 3 | — | TODO |
| T099 | Notification center: DingTalk webhook channel | P2 | 2 | T098 | TODO |
| T100 | Notification center: template management API (create, preview, versioning) | P2 | 3 | T098 | TODO |

---

## 12. Final Recommendation

### Optimal Development Sequence

```
Weeks 1–4   ████████████  Backend P0 (T001–T023)
Weeks 2–8   ░░████████████████████  Frontend admin shell (T069–T081)  ← parallel
Weeks 5–12  ░░░░████████████████  API + annotations + state machine (T052–T068)
Weeks 9–16  ░░░░░░░░████████████████  Order aggregate + domain events (T035–T051)
Weeks 13–20 ░░░░░░░░░░░░████████████████  Frontend business screens
Weeks 17–24 ░░░░░░░░░░░░░░░░████████████  K8s + CI/CD + tests (T060–T061, T082–T095)
```

---

### Answer to Each Strategic Question

**1. Build frontend first?**

Yes — specifically the **admin shell only**. Login, user management, role/permission
assignment, tenant dashboard. These surfaces are backed by the most stable part of
the codebase (the security and RBAC layer). Build them now. Start in Week 2,
in parallel with P0 backend fixes.

Do **not** build order/inventory/purchase UI yet. Those surfaces will change
significantly once the OrdOrder aggregate and domain events are implemented.
Building them now means rebuilding them in 3 months.

**2. Expand backend capabilities?**

Yes — two specific capabilities: **(a) P0 multi-tenant fixes** (tenant_id
propagation, auto-populate, cache isolation, hop-count filter). These are not
features; they are correctness requirements. Any customer data in the system
before these are fixed is at leakage risk. **(b) Transactional outbox**. This
enables decoupling Finance/Logistics/Notify from the order commit path and is
the precondition for the Kafka saga migration later.

Do not expand features (file versioning, sharing, OCR) until the platform is correct.

**3. Introduce AI modules?**

**Not yet.** The minimum viable foundation for AI is:
- Stable multi-tenant isolation (AI on leaky tenancy = AI serving wrong data)
- Sufficient domain data (100K+ orders for forecasting, 50K+ documents for semantic search)
- A working product surface for users to interact with AI features

Revisit at **Month 18**. By then the platform will be stable, the data corpus
will be meaningful, and you will know which AI capabilities customers actually
want versus which ones sound good in a roadmap.

**4. Refactor architecture?**

**Yes, surgically.** Three targeted refactors:
1. Decompose `scm-common` (4 weeks) — reduces rebuild blast radius immediately
2. Introduce `OrdOrder` aggregate root (4 weeks) — the foundation for everything else
3. Implement transactional outbox (2 weeks, largely already drafted)

Do **not** do a full rewrite. Refactor domain-by-domain, in production-compatible
increments, over 6–12 months. Each refactor must ship working code, not theoretical improvements.

**5. Focus on observability and DevOps?**

**Observability:** Already strong (SkyWalking, ELK, Prometheus, Grafana). Add the
three business KPI dashboards (T087–T089) and the two alert rules (T090–T091).
That is 8 person-days and closes the remaining gap. Do not invest more.

**DevOps:** CI/CD (T060–T061) is P1 because manual deployment is a safety risk.
Kubernetes manifests (T082–T086) are P1 because they are required before adding
engineers and before offering the platform as a managed service.
Execute both in the Weeks 17–24 window.

**6. Move toward microservices?**

The system is already deployed as 15 microservices. The architectural work is not
moving toward microservices — it is making the **existing** microservices
architecturally correct. That means:
- Each service owns its domain (no cross-service entity access)
- 3-hop rule enforced (operational independence under load)
- Domain events replace direct calls beyond hop 3 (decoupled failure domains)
- Tenant context propagated correctly (behavioral isolation)

The goal is **operational independence and domain correctness**, not the word "microservices."

---

### Single Sentence Priority

> Close the four P0 multi-tenant gaps and the RPC chain depth risk in weeks 1–4;
> build the admin frontend in parallel starting week 2;
> then enrich the order domain with a rich aggregate and domain events;
> and defer AI, full K8s migration, and workflow engine until the platform is correct and has real users.

---

*This document covers: 15 microservices, 16 databases, Java 21, Spring Boot 4,
Spring Cloud 2025.1.1, Dubbo 3.x, Seata 2.x, PostgreSQL, Redis, Kafka, RabbitMQ,
SkyWalking, Prometheus/Grafana, WebAuthn/TOTP/mTLS.*

*Generated: June 2026. Review cycle: quarterly.*
