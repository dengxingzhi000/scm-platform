# SCM Platform - Enterprise Supply Chain Management System

## Overview

**SCM Platform** is an enterprise-grade Supply Chain Management system built on top of the **CommonPermissionsFramework** (NewNearSync). It provides comprehensive SCM capabilities including product management, inventory control, order processing, warehouse management, and logistics tracking.

**Built for Scale**: Designed to handle high-concurrency scenarios like flash sales, with architecture patterns inspired by Alibaba Cainiao, JD Logistics, and Meituan delivery systems.

## Tech Stack

### Core Framework
- **Java 21** with virtual threads and pattern matching
- **Spring Boot 4.0.0** - Latest Spring Framework
- **Spring Cloud 2025.1.0** - Microservices infrastructure
- **Spring Cloud Alibaba 2025.0.0.0** - Nacos, Sentinel, Seata integration

### Distributed Components
- **Seata 2.2.0** - Distributed transaction coordination (AT, TCC, Saga modes)
- **XXL-Job 2.4.3** - Distributed task scheduling
- **Nacos** - Service discovery and configuration management
- **Dubbo** - High-performance RPC framework

### Data Layer
- **PostgreSQL/MySQL** - Primary data storage
- **MyBatis-Plus 3.5.15** - Enhanced ORM framework
- **ShardingSphere 5.5.1** - Database sharding and read-write separation
- **Redis** - Distributed cache and session storage
- **Elasticsearch 8.11.4** - Full-text search and analytics
- **Canal 1.1.7** - MySQL binlog-based data synchronization

### Messaging & Events
- **Kafka** - High-throughput event streaming
- **RabbitMQ** - Reliable message queue
- **CloudEvents** - Standardized event format

### Observability
- **Sentinel** - Traffic control and circuit breaking
- **SkyWalking 9.3.0** - APM and distributed tracing
- **Prometheus + Grafana** - Metrics and monitoring
- **Spring Boot Actuator** - Health checks and metrics

### Security
- **Spring Security 6** - Authentication and authorization
- **JWT + OAuth2** - Token-based authentication
- **WebAuthn** - Passwordless authentication
- **RBAC + Data Scope** - Fine-grained access control

## Architecture

### Microservices

```
┌─────────────────┐
│   API Gateway   │ (Port 9095)
└────────┬────────┘
         │
    ┌────┴────┬─────────┬──────────┬──────────┬──────────┬──────────┐
    │         │         │          │          │          │          │
┌───▼───┐ ┌──▼───┐ ┌───▼────┐ ┌───▼────┐ ┌──▼────┐ ┌───▼────┐ ┌───▼────┐
│ Auth  │ │Product│ │Inventory│ │ Order  │ │Warehouse│ │Logistics│ │Supplier│
│Service│ │Service│ │ Service │ │Service │ │Service │ │ Service │ │Service │
└───────┘ └───────┘ └─────────┘ └────────┘ └────────┘ └────────┘ └────────┘
```

### Key Features by Module

#### 1. **scm-product** (Product Service)
- Product catalog management with categories and SKUs
- Multi-attribute product specifications
- Real-time inventory sync
- Elasticsearch integration for product search

#### 2. **scm-inventory** (Inventory Service)
- Real-time inventory tracking
- Atomic stock deduction with Redis Lua scripts
- Stock reservation with timeout auto-release
- Multi-warehouse inventory allocation
- Stock alert and replenishment

#### 3. **scm-order** (Order Service)
- Order lifecycle management with state machine
- Distributed transaction coordination via Seata
- Order timeout auto-cancellation
- Payment integration
- Invoice generation

#### 4. **scm-warehouse** (Warehouse Service)
- Multi-warehouse management
- Inbound/outbound operations
- Inventory transfer between warehouses
- Wave picking and batch operations
- RFID/Barcode support

#### 5. **scm-logistics** (Logistics Service)
- Logistics provider integration
- Real-time shipment tracking
- Route optimization
- Delivery status updates
- Exception handling

#### 6. **scm-supplier** (Supplier Service)
- Supplier onboarding and management
- Purchase order processing
- Supplier performance evaluation
- Payment settlement

## Getting Started

### Prerequisites

- **JDK 21** (required)
- **Maven 3.8+**
- **Docker & Docker Compose** (for infrastructure)
- **Git**

### Infrastructure Setup

Start required infrastructure services:

```bash
# Start Nacos, Redis, PostgreSQL, Kafka, RabbitMQ, Elasticsearch
docker-compose up -d

# Verify services
docker-compose ps
```

### Build

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Build specific module
cd scm-product && mvn clean package
```

### Run Services

**IMPORTANT: Start services in this order:**

```bash
# 1. Start auth service (HTTPS port 8106)
cd scm-auth && mvn spring-boot:run

# 2. Start product service (Port 8201)
cd scm-product/service && mvn spring-boot:run

# 3. Start inventory service (Port 8202)
cd scm-inventory/service && mvn spring-boot:run

# 4. Start order service (Port 8203)
cd scm-order/service && mvn spring-boot:run

# 5. Start warehouse service (Port 8204)
cd scm-warehouse/service && mvn spring-boot:run

# 6. Start logistics service (Port 8205)
cd scm-logistics/service && mvn spring-boot:run

# 7. Start supplier service (Port 8206)
cd scm-supplier/service && mvn spring-boot:run

# 8. Start API Gateway (Port 9095)
cd scm-gateway && mvn spring-boot:run
```

### Access Endpoints

- **API Gateway**: http://localhost:9095
- **Knife4j API Docs**: http://localhost:9095/doc.html
- **Nacos Console**: http://localhost:8848/nacos (nacos/nacos)
- **Sentinel Dashboard**: http://localhost:8080 (sentinel/sentinel)
- **XXL-Job Admin**: http://localhost:8088/xxl-job-admin (admin/123456)

## Development Guide

### Creating a New Feature

See `docs/development-guide.md` for detailed development patterns.

### Common Patterns

#### 1. Distributed Transaction (Seata AT Mode)

```java
@GlobalTransactional(name = "create-order", rollbackFor = Exception.class)
public Order createOrder(OrderDTO dto) {
    // Step 1: Create order (local)
    Order order = orderMapper.insert(new Order(dto));

    // Step 2: Deduct inventory (remote RPC)
    inventoryClient.deductStock(dto.getSkuId(), dto.getQuantity());

    // Step 3: Create payment (remote RPC)
    paymentClient.createPayment(order.getId(), dto.getAmount());

    return order;
}
```

#### 2. Atomic Inventory Deduction (Redis Lua)

```java
@Service
public class InventoryService {

    public boolean deductStock(Long skuId, Integer quantity) {
        String script = """
            local stock = redis.call('GET', KEYS[1])
            if tonumber(stock) >= tonumber(ARGV[1]) then
                redis.call('DECRBY', KEYS[1], ARGV[1])
                return 1
            else
                return 0
            end
            """;

        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            List.of("stock:" + skuId),
            quantity
        );

        return result == 1L;
    }
}
```

#### 3. Order State Machine

```java
@Configuration
@EnableStateMachine
public class OrderStateMachineConfig {

    @Bean
    public StateMachine<OrderState, OrderEvent> buildMachine() {
        StateMachineBuilder.Builder<OrderState, OrderEvent> builder =
            StateMachineBuilder.builder();

        builder.configureStates()
            .withStates()
            .initial(OrderState.PENDING_PAYMENT)
            .states(EnumSet.allOf(OrderState.class));

        builder.configureTransitions()
            .withExternal()
                .source(OrderState.PENDING_PAYMENT)
                .target(OrderState.PAID)
                .event(OrderEvent.PAY_SUCCESS)
            .and()
            .withExternal()
                .source(OrderState.PAID)
                .target(OrderState.SHIPPED)
                .event(OrderEvent.SHIP);

        return builder.build();
    }
}
```

## Testing

```bash
# Run all tests
mvn test

# Run tests with coverage
mvn test jacoco:report

# Run integration tests
mvn verify -P integration-test

# Run specific test
mvn test -Dtest=OrderServiceTest#testCreateOrder
```

## Monitoring

### Health Checks

```bash
# Gateway health
curl http://localhost:9095/actuator/health

# Product service health
curl http://localhost:8201/actuator/health

# Metrics (Prometheus)
curl http://localhost:9095/actuator/prometheus
```

### Tracing

SkyWalking UI: http://localhost:8080

## Configuration

### Environment Variables

```bash
# Nacos
NACOS_SERVER_ADDR=localhost:8848
NACOS_NAMESPACE=scm-dev

# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=scm_platform
DB_USER=postgres
DB_PASSWORD=your-password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Seata
SEATA_SERVER_ADDR=localhost:8091

# Elasticsearch
ES_HOST=localhost
ES_PORT=9200

# Security
JWT_SECRET=your-512-bit-jwt-secret-key-here
AES_KEY=your-256-bit-aes-key-here
```

## Project Structure

```
scm-platform/
├── scm-common/              # Shared modules
│   ├── core/                # Core utilities, exceptions
│   ├── security-api/        # Security interfaces
│   ├── data/                # Data access layer
│   ├── web/                 # Web security filters
│   ├── monitoring/          # Circuit breaker
│   └── integration/         # Messaging
├── scm-gateway/             # API Gateway
├── scm-auth/                # Authentication service
├── scm-product/             # Product service
├── scm-inventory/           # Inventory service
├── scm-order/               # Order service
├── scm-warehouse/           # Warehouse service
├── scm-logistics/           # Logistics service
├── scm-supplier/            # Supplier service
├── docs/                    # Documentation
└── config/                  # Configuration templates
```

## Design Documentation

- **Architecture Design**: See [SCM_DESIGN_PLAN.md](../NewNearSync/SCM_DESIGN_PLAN.md) in the parent project
- **API Documentation**: http://localhost:9095/doc.html (after starting services)
- **Database Schema**: `docs/database-schema.md`
- **Development Guide**: `docs/development-guide.md`

## Performance Benchmarks

Expected performance targets:

- **Order Creation**: 10,000+ TPS
- **Inventory Query**: 50,000+ QPS
- **Product Search**: 100ms p99 latency
- **API Gateway**: 100,000+ connections

See `docs/performance-tuning.md` for optimization strategies.

## Contributing

See `CONTRIBUTING.md` for contribution guidelines.

## License

Apache License 2.0

## Support

- GitHub Issues: https://github.com/your-org/scm-platform/issues
- Documentation: https://scm-platform.readthedocs.io
- Email: support@your-org.com

---

**Built with ❤️ based on CommonPermissionsFramework**