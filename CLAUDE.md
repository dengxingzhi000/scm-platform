# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**SCM Platform** is an enterprise-grade Supply Chain Management system built on Spring Cloud microservices architecture. The platform handles product management, inventory control, order processing, warehouse operations, and logistics tracking at scale. It's designed to handle high-concurrency scenarios like flash sales, with patterns inspired by Alibaba Cainiao, JD Logistics, and Meituan.

## Build & Development Commands

### Prerequisites
- JDK 21 (required - uses virtual threads and pattern matching)
- Maven 3.8+
- Docker & Docker Compose (for infrastructure)
- PostgreSQL 14+ (primary database)

### Building the Project

```bash
# Build all modules
mvn clean install

# Build without tests (faster)
mvn clean install -DskipTests

# Build specific module
cd scm-product/service && mvn clean package
```

### Running Services

**IMPORTANT: Start services in this specific order due to dependencies:**

```bash
# 1. Start infrastructure first
docker-compose up -d

# 2. Start gateway service (Port 8761)
cd scm-gateway && mvn spring-boot:run

# 3. Start auth service (Port 8106)
cd scm-auth && mvn spring-boot:run

# 4. Start system service (Port 8081) - user/dept/role/permission management
cd scm-system/service && mvn spring-boot:run

# 5. Start base infrastructure services
cd scm-approval/service && mvn spring-boot:run    # Approval workflow
cd scm-audit/service && mvn spring-boot:run       # Audit logging
cd scm-notify/service && mvn spring-boot:run      # Notification service
cd scm-tenant/service && mvn spring-boot:run      # Multi-tenant management

# 6. Start business services (any order)
cd scm-product/service && mvn spring-boot:run     # Port 8201
cd scm-inventory/service && mvn spring-boot:run   # Port 8202
cd scm-order/service && mvn spring-boot:run       # Port 8203
cd scm-warehouse/service && mvn spring-boot:run   # Port 8204
cd scm-logistics/service && mvn spring-boot:run   # Port 8205
cd scm-supplier/service && mvn spring-boot:run    # Port 8206
cd scm-purchase/service && mvn spring-boot:run    # Purchase management
cd scm-finance/service && mvn spring-boot:run     # Finance management
```

### Testing

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Run integration tests
mvn verify -P integration-test

# Run specific test class
mvn test -Dtest=OrderServiceTest

# Run specific test method
mvn test -Dtest=OrderServiceTest#testCreateOrder
```

### Access Points

- **API Gateway**: http://localhost:8761
- **API Documentation (Knife4j)**: http://localhost:8761/doc.html
- **Nacos Console**: http://localhost:8848/nacos (nacos/nacos)
- **Sentinel Dashboard**: http://localhost:8080 (sentinel/sentinel)
- **XXL-Job Admin**: http://localhost:8088/xxl-job-admin (admin/123456)

## Architecture Overview

### Microservices Structure

This is a **multi-module Maven project** with the following service architecture:

```
scm-platform/
├── scm-common/              # Shared modules (utilities, security, data access)
│   ├── core/                # Core utilities, exceptions, UUID generation
│   ├── security-api/        # Security interfaces and context
│   ├── data/                # Data access layer (MyBatis-Plus, Redis, read-write separation)
│   ├── web/                 # Web security filters, JWT, OAuth2
│   ├── monitoring/          # Circuit breaker integration
│   └── integration/         # Messaging (Kafka, RabbitMQ)
│
├── scm-gateway/             # API Gateway (Spring Cloud Gateway) - Port 8761
├── scm-auth/                # Authentication service (JWT, OAuth2, WebAuthn) - Port 8106
├── scm-system/              # System service (User/Dept/Role/Permission) - Port 8081
│
├── scm-approval/            # Approval workflow service
├── scm-audit/               # Audit logging service
├── scm-notify/              # Notification service (Email/SMS/System)
├── scm-tenant/              # Multi-tenant management service
│
├── scm-product/             # Product catalog service - Port 8201
├── scm-inventory/           # Inventory management service - Port 8202
├── scm-order/               # Order processing service - Port 8203
├── scm-warehouse/           # Warehouse management (WMS) - Port 8204
├── scm-logistics/           # Logistics tracking (TMS) - Port 8205
├── scm-supplier/            # Supplier management service - Port 8206
├── scm-purchase/            # Purchase order management
└── scm-finance/             # Finance and settlement service
```

Each business service typically has two sub-modules:
- `api/`: Dubbo RPC interface definitions
- `service/`: Service implementation

### Multi-Tenant Architecture

The platform supports **multi-tenant isolation** with:
- **Database-per-tenant**: Each tenant has separate databases (db_user, db_org, db_permission, etc.)
- **Tenant Context**: Automatic tenant ID propagation via `TenantContextHolder`
- **Data Isolation**: Row-level security with `tenant_id` column
- **Dynamic DataSource**: Routes queries to correct tenant database using `@DS` annotation

Example tenant configuration in `scm-system/service`:
```yaml
spring:
  datasource:
    dynamic:
      primary: user
      datasource:
        user:
          url: jdbc:postgresql://localhost:5432/db_user
        org:
          url: jdbc:postgresql://localhost:5432/db_org
        permission:
          url: jdbc:postgresql://localhost:5432/db_permission
```

### Key Architectural Patterns

#### 1. Distributed Transactions (Seata)

The platform uses **Seata AT mode** for distributed transaction coordination. When creating orders, multiple services participate in a global transaction:

```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public Order createOrder(OrderDTO dto) {
    // 1. Create order (local transaction)
    Order order = orderMapper.insert(new Order(dto));

    // 2. Deduct inventory (remote RPC - participates in global transaction)
    inventoryClient.deductStock(dto.getSkuId(), dto.getQuantity());

    // 3. Create payment (remote RPC - participates in global transaction)
    paymentClient.createPayment(order.getId(), dto.getAmount());

    return order; // If any step fails, all are rolled back
}
```

**Critical**: Each database must have an `undo_log` table for Seata rollback. See `scripts/db/microservices/*.sql` for database initialization.

#### 2. High-Concurrency Inventory Management

Inventory deduction uses **Redis Lua scripts** for atomic operations to prevent overselling:

```java
// Redis Lua script ensures atomicity
String script = """
    local stock = redis.call('GET', KEYS[1])
    if tonumber(stock) >= tonumber(ARGV[1]) then
        redis.call('DECRBY', KEYS[1], ARGV[1])
        return 1
    else
        return 0
    end
    """;
```

**Stock Reservation Pattern**: Orders reserve inventory for 15 minutes with automatic release if payment fails.

#### 3. Order State Machine (Spring State Machine)

Orders follow a strict state flow managed by Spring State Machine:

```
PENDING_PAYMENT → PAID → PENDING_SHIP → SHIPPED → IN_TRANSIT → DELIVERED → COMPLETED
                    ↓
                CANCELLED
```

State transitions trigger actions (e.g., confirm inventory reservation on payment, notify logistics on shipment).

#### 4. Read-Write Separation

The data layer (`scm-common/data`) implements automatic read-write separation with:
- **5 load balancing strategies**: Round Robin, Weighted Round Robin, Random, Weighted Random, Least Connections
- **Automatic failover**: Circuit breaker for slave databases
- **Annotations**: `@Master` and `@Slave` for explicit routing

#### 5. Two-Level Caching

- **L1 Cache**: Caffeine (JVM local cache, 10,000 items, 5-minute TTL)
- **L2 Cache**: Redis (distributed cache, 30-minute TTL)
- **Pattern**: Check L1 → Check L2 → Query DB → Populate caches

### Technology Stack Details

**Core Framework**:
- Java 21 with virtual threads (use for I/O-bound operations)
- Spring Boot 4.0.0 / Spring Cloud 2025.1.0 / Spring Cloud Alibaba 2025.0.0.0

**Distributed Components**:
- **Seata 2.2.0**: AT mode for distributed transactions
- **Sentinel**: Traffic control, circuit breaking (configuration in Nacos)
- **XXL-Job 2.4.3**: Distributed task scheduling (e.g., order timeout cancellation)
- **Nacos**: Service discovery + configuration center

**Data Layer**:
- **PostgreSQL**: Primary storage (multi-database per tenant)
- **MyBatis-Plus 3.5.15**: ORM with automatic CRUD
- **ShardingSphere 5.5.1**: Database sharding (ready, not yet configured)
- **Redis**: L2 cache + distributed locks + inventory counters
- **Elasticsearch 8.11.4**: Product search with IK analyzer
- **Canal 1.1.7**: MySQL binlog → Elasticsearch sync

**Messaging**:
- **Kafka**: High-throughput event streaming (order events, inventory changes)
- **RabbitMQ**: Reliable message delivery with DLQ retry

## Development Patterns

### Creating a New Microservice

1. **Add module to parent POM** (`pom.xml`):
   ```xml
   <modules>
       <module>scm-new-service</module>
   </modules>
   ```

2. **Create API module** (Dubbo interfaces):
   ```
   scm-new-service/
   ├── api/
   │   ├── pom.xml
   │   └── src/main/java/com/frog/newservice/api/
   │       └── NewServiceDubboApi.java
   └── pom.xml (parent)
   ```

3. **Create Service module** (implementation):
   ```
   scm-new-service/
   └── service/
       ├── pom.xml
       ├── src/main/java/com/frog/newservice/
       │   ├── NewServiceApplication.java
       │   ├── controller/
       │   ├── service/
       │   ├── mapper/
       │   └── domain/
       └── src/main/resources/
           └── application.yml
   ```

4. **Register with Nacos** (application.yml):
   ```yaml
   spring:
     application:
       name: scm-new-service
     cloud:
       nacos:
         discovery:
           server-addr: ${NACOS_SERVER:localhost:8848}
   ```

### Distributed Transaction Pattern

When implementing cross-service operations:

1. **Mark entry point** with `@GlobalTransactional`:
   ```java
   @GlobalTransactional(name = "operation-name", rollbackFor = Exception.class)
   public void doOperation() { ... }
   ```

2. **Remote RPC calls** automatically participate if they modify data
3. **Database operations** use MyBatis-Plus (automatically tracked)
4. **Exceptions** trigger automatic rollback across all services

### Idempotency Pattern

For critical operations (inventory deduction, order creation), use request IDs:

```java
public boolean deductStock(Long skuId, Integer quantity, String requestId) {
    // Check if already processed
    if (redisTemplate.hasKey("deduct:" + requestId)) {
        return true; // Already processed
    }

    // Process operation
    boolean success = doDeduct(skuId, quantity);

    // Mark as processed (24-hour expiry)
    if (success) {
        redisTemplate.opsForValue().set("deduct:" + requestId, "1", 24, TimeUnit.HOURS);
    }

    return success;
}
```

### Multi-Tenant Data Access

Use `@DS` annotation to route to the correct database:

```java
@Service
public class UserService {

    @DS("user")  // Route to user database
    public SysUser getUserById(Long userId) {
        return userMapper.selectById(userId);
    }

    @DS("org")  // Route to org database
    public SysDept getDeptById(Long deptId) {
        return deptMapper.selectById(deptId);
    }

    @DS("permission")  // Route to permission database
    public SysRole getRoleById(Long roleId) {
        return roleMapper.selectById(roleId);
    }
}
```

### Search Integration

Product search uses Elasticsearch:

1. **Document definition** (annotated entity):
   ```java
   @Document(indexName = "products")
   public class ProductDocument {
       @Id
       private Long id;

       @Field(type = FieldType.Text, analyzer = "ik_max_word")
       private String name;
   }
   ```

2. **Data sync**: Canal monitors PostgreSQL WAL → automatically updates ES
3. **Search queries**: Use `ElasticsearchRepository` or `ElasticsearchRestTemplate`

### Common Utilities

Located in `scm-common/core`:

- **UUIDv7Util**: Time-ordered UUID generation (use for order IDs, primary keys)
- **ApiResponse<T>**: Standard API response wrapper
- **PageResult<T>**: Pagination wrapper
- **GlobalExceptionHandler**: Centralized exception handling

Security utilities in `scm-common/web`:

- **JwtUtils**: JWT token generation/validation
- **PasswordUtils**: Password hashing (Argon2)
- **IpUtils**: IP address extraction and validation

## Configuration Management

### Environment Variables

Key environment variables (set in Docker Compose or shell):

```bash
# Nacos
NACOS_SERVER=localhost:8848
NACOS_NAMESPACE=scm-dev

# Database (multi-tenant)
DB_HOST=localhost
DB_USERNAME=admin
DB_PASSWORD=your-password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Seata
SEATA_SERVER_ADDR=localhost:8091

# Security (MUST be set in production)
JWT_SECRET=your-512-bit-jwt-secret-key-here
AES_KEY=your-256-bit-aes-key-here
IDENTITY_SIGNATURE_SECRET=your-identity-secret
API_SECRET_WEB_APP=your-web-app-secret
API_SECRET_INTERNAL_SERVICE=your-internal-service-secret
KEYSTORE_PASSWORD=your-keystore-password
TRUSTSTORE_PASSWORD=your-truststore-password
```

### Nacos Configuration

Services read configuration from Nacos with fallback to local YAML. Configuration naming pattern:

```
${spring.application.name}-${spring.profiles.active}.yaml
```

Example: `scm-order-dev.yaml`

## Data Access Patterns

### Using MyBatis-Plus

```java
// 1. Define entity
@Data
@TableName("t_product")
public class Product {
    @TableId(type = IdType.ASSIGN_ID)  // Use UUIDv7
    private Long id;

    private String productName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

// 2. Define mapper (no XML needed for basic CRUD)
public interface ProductMapper extends BaseMapper<Product> {
    // Custom queries only
    @Select("SELECT * FROM t_product WHERE category_id = #{categoryId}")
    List<Product> selectByCategory(Long categoryId);
}

// 3. Use in service
@Service
public class ProductService extends ServiceImpl<ProductMapper, Product> {
    public List<Product> search(String keyword) {
        return lambdaQuery()
            .like(Product::getProductName, keyword)
            .list();
    }
}
```

### Data Scope & Permissions

Use `@DataScope` annotation for row-level permission filtering:

```java
@DataScope(deptAlias = "d")
public List<Order> getUserOrders(Long userId) {
    // Automatically filters by user's department scope
}
```

### Distributed Locks

Use `DistributedLock` from `scm-common/data`:

```java
@Autowired
private DistributedLock distributedLock;

public void processOrder(Long orderId) {
    String lockKey = "order:" + orderId;
    LockHandle lock = distributedLock.acquire(lockKey, 30, TimeUnit.SECONDS);

    try {
        // Critical section
    } finally {
        lock.release();
    }
}
```

## API Design Standards

All REST APIs follow these conventions:

### URL Structure
```
/api/v1/{resource}          # List/Create
/api/v1/{resource}/{id}     # Get/Update/Delete
/api/v1/{resource}/{id}/{sub-resource}  # Nested resources
```

### HTTP Methods
- `GET`: Query (idempotent)
- `POST`: Create (non-idempotent)
- `PUT`: Full update (idempotent)
- `PATCH`: Partial update (idempotent)
- `DELETE`: Delete (idempotent)

### Response Format

All APIs return `ApiResponse<T>`:

```json
{
  "code": 200,
  "message": "Success",
  "data": { ... },
  "traceId": "550e8400-...",
  "timestamp": "2025-12-24T10:30:00Z"
}
```

### Pagination

Use `PageResult<T>` for paginated queries:

```
GET /api/v1/products?page=1&size=20&sort=createdAt,desc
```

Response includes: `total`, `page`, `size`, `pages`, `records`

## Monitoring & Observability

### Health Checks

```bash
# Gateway health
curl http://localhost:8761/actuator/health

# Individual service health
curl http://localhost:8201/actuator/health  # Product service
```

### Metrics (Prometheus)

```bash
# Scrape endpoint
curl http://localhost:8761/actuator/prometheus
```

### Distributed Tracing

- **SkyWalking**: Traces requests across services
- **TraceId**: Automatically propagated in `ApiResponse`

## Critical Performance Considerations

### 1. Inventory Operations

- **NEVER** query inventory from database in hot path
- **ALWAYS** use Redis for inventory checks and deductions
- **USE** Lua scripts for atomic operations

### 2. Database Queries

- **PREFER** read-write separation (`@Slave` for queries)
- **USE** pagination for large result sets
- **AVOID** N+1 queries (use `@BatchSize` or joins)
- **USE** `@DS` annotation for multi-tenant database routing

### 3. Caching Strategy

- **Hot data** (product details, user info): Cache in Caffeine + Redis
- **Frequently changing** (inventory): Redis only, short TTL (30s)
- **Static data** (categories, brands): Cache permanently with manual invalidation

### 4. Messaging

- **Use Kafka** for high-throughput, order-insensitive events
- **Use RabbitMQ** for guaranteed delivery, order-sensitive operations
- **ALWAYS** implement idempotency in message consumers

## Security Notes

### Authentication Flow

1. User authenticates via `/auth/login` → receives JWT
2. JWT included in `Authorization: Bearer <token>` header
3. Gateway validates JWT before routing to services
4. Services extract user context from `SecurityContextHolder`

### Gateway Security Features

The API Gateway (`scm-gateway`) implements multiple security layers:

1. **API Signature Verification**: HMAC-SHA256 request signing
2. **IP Access Control**: Whitelist/blacklist support
3. **Identity Propagation**: Automatic user context forwarding via headers
4. **mTLS Support**: Certificate-based authentication for service-to-service calls

All security secrets MUST be provided via environment variables in production (no defaults).

### WebAuthn Support

The auth service supports passwordless authentication:
- Registration: `/auth/webauthn/register`
- Authentication: `/auth/webauthn/authenticate`

### Data Desensitization

Sensitive fields are automatically masked in responses (see `DesensitizeUtils`):
- Phone: `138****8000`
- ID Card: `110101****1234`
- Bank Card: `6222****5678`

## Database Management

### Multi-Database Structure

Each tenant has separate databases located in `scripts/db/microservices/`:

- **001_db_user.sql**: User accounts and OAuth data
- **002_db_org.sql**: Department and organizational structure
- **003_db_permission.sql**: Roles and permissions
- **004_db_approval.sql**: Approval workflows
- **005_db_audit.sql**: Audit logs
- **006_db_notify.sql**: Notification templates and logs
- **007_data_redundancy.sql**: Cross-database data synchronization
- **010-018**: Business domain databases (product, inventory, order, etc.)

### Initializing Databases

```bash
# Initialize all databases
cd scripts/db
./init-all-databases.sh

# Or initialize individually
psql -U admin -d postgres -f microservices/001_db_user.sql
```

### PostgreSQL Partitioning Constraints

**CRITICAL**: Several tables use PostgreSQL range partitioning. When working with partitioned tables, be aware of this constraint:

**PostgreSQL Requirement**: UNIQUE constraints on partitioned tables MUST include ALL partition key columns.

#### Tables with Partitioned Design

The following tables are partitioned by time (create_time or reserved_at):

| Database | Table | Partition Key | UNIQUE Constraint Pattern |
|----------|-------|--------------|---------------------------|
| db_inventory | inv_reservation | reserved_at | (reservation_no, reserved_at) |
| db_order | ord_order | create_time | (order_no, create_time) |
| db_supplier | sup_purchase_order | create_time | (purchase_no, create_time) |
| db_purchase | pur_order | create_time | (order_no, create_time) |
| db_finance | freight_calc_record | create_time | No UNIQUE constraint (log table) |
| db_finance | payment_record | create_time | No UNIQUE constraint (log table) |
| db_tenant | tenant_operation_log | create_time | No UNIQUE constraint (log table) |

#### Pattern for Partitioned Tables

```sql
-- CORRECT: UNIQUE constraint includes partition key
CREATE TABLE ord_order (
    id UUID DEFAULT gen_random_uuid(),
    order_no VARCHAR(128) NOT NULL,
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- ... other columns
    CONSTRAINT uk_order_no_create_time UNIQUE (order_no, create_time)
) PARTITION BY RANGE (create_time);

-- WRONG: Will fail with error
CREATE TABLE ord_order (
    id UUID DEFAULT gen_random_uuid(),
    order_no VARCHAR(128) NOT NULL UNIQUE,  -- ❌ Missing partition key
    create_time TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- ...
) PARTITION BY RANGE (create_time);
```

#### Business Implication

Composite UNIQUE constraints (business_key, partition_key) allow the same business key in different partitions. To enforce global uniqueness:

1. **Use application-level enforcement** with Redis:
   ```java
   public String generateOrderNo() {
       String orderNo = "ORD" + UUIDv7Util.generate();
       Boolean success = redisTemplate.opsForValue()
           .setIfAbsent("order_no:" + orderNo, "1", 7, TimeUnit.DAYS);
       if (Boolean.TRUE.equals(success)) {
           return orderNo;
       }
       throw new BusinessException("Failed to generate unique order number");
   }
   ```

2. **Use PostgreSQL sequences** for guaranteed uniqueness:
   ```sql
   CREATE SEQUENCE seq_order_no;
   -- Generates: ORD20250125 0000000001
   SELECT 'ORD' || TO_CHAR(NOW(), 'YYYYMMDD') ||
          LPAD(NEXTVAL('seq_order_no')::TEXT, 10, '0');
   ```

## Troubleshooting

### Common Issues

1. **Service won't start**: Check Nacos is running and accessible at localhost:8848
2. **Seata transaction fails**: Verify `undo_log` table exists in all databases
3. **Redis connection errors**: Ensure Redis is running on expected host:port
4. **Search not working**: Check Canal is syncing data to Elasticsearch
5. **Multi-tenant routing fails**: Verify `@DS` annotation and datasource configuration
6. **Gateway security blocks requests**: Check environment variables for secrets are set

### Logging

Services use SLF4J with Logback. Log files are in:
- Console: Colorized output
- File: `logs/{service-name}.log` (if file appender configured)

## Additional Documentation

- **Detailed Design**: `docs/SCM_DESIGN_PLAN.md` (Chinese)
- **Database Scripts**: `scripts/db/microservices/README.md`
- **Multi-Tenant Migration**: `docs/SCM_SYSTEM_MULTI_TENANT_MIGRATION.md`
- **Permission Design**: `docs/PERMISSION_MULTI_TENANT_DESIGN.md`
- **Service Architecture**: `docs/SERVICE_ARCHITECTURE_OVERVIEW.md`