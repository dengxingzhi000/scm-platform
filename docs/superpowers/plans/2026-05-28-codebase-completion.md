# Codebase Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all compilation errors, missing core components, and configuration gaps so the entire codebase compiles and each service can start. Then implement business logic for stub services.

**Architecture:** Multi-module Maven microservices platform with Spring Boot 3.5.5, Spring Cloud Alibaba, Seata distributed transactions, Dubbo RPC, MyBatis-Plus ORM, PostgreSQL multi-tenant databases.

**Tech Stack:** Java 21, Spring Boot 3.5.5, Spring Cloud 2025.1.0, Spring Cloud Alibaba 2025.0.0.0, Seata 2.0.0, Dubbo 3.3.6, MyBatis-Plus 3.5.15, PostgreSQL 16, Redis, Kafka, RabbitMQ

---

## Phase 0: Fix Compilation Errors (CRITICAL — blocks everything)

### Task 0.1: Fix scm-order malformed imports

**Problem:** 3 service impl files have corrupted package/import statements (path separators in package names).

**Files:**
- Fix: `scm-order/service/src/main/java/scm/order/service/impl/OrdStatusHistoryServiceImpl.java`
- Fix: `scm-order/service/src/main/java/scm/order/service/impl/OrdRefundServiceImpl.java`
- Fix: `scm-order/service/src/main/java/scm/order/service/impl/OrdPaymentServiceImpl.java`

- [ ] **Step 1: Read the corrupted files to understand the pattern**

Run: `Read scm-order/service/src/main/java/scm/order/service/impl/OrdStatusHistoryServiceImpl.java`

Expected: Package declaration has path separators like `scm-order/service.service.impl` instead of `scm.order.service.impl`, and imports have `scm-order/service.domain.entity.X` instead of `scm.order.domain.entity.X`.

- [ ] **Step 2: Fix OrdStatusHistoryServiceImpl.java**

Replace the entire file content with correct package/imports:
```java
package scm.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import scm.order.domain.entity.OrdStatusHistory;
import scm.order.mapper.OrdStatusHistoryMapper;
import scm.order.service.IOrdStatusHistoryService;
import org.springframework.stereotype.Service;

@Service
public class OrdStatusHistoryServiceImpl extends ServiceImpl<OrdStatusHistoryMapper, OrdStatusHistory> implements IOrdStatusHistoryService {
}
```

- [ ] **Step 3: Fix OrdRefundServiceImpl.java**

Same pattern — replace package/imports:
```java
package scm.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import scm.order.domain.entity.OrdRefund;
import scm.order.mapper.OrdRefundMapper;
import scm.order.service.IOrdRefundService;
import org.springframework.stereotype.Service;

@Service
public class OrdRefundServiceImpl extends ServiceImpl<OrdRefundMapper, OrdRefund> implements IOrdRefundService {
}
```

- [ ] **Step 4: Fix OrdPaymentServiceImpl.java**

Same pattern:
```java
package scm.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import scm.order.domain.entity.OrdPayment;
import scm.order.mapper.OrdPaymentMapper;
import scm.order.service.IOrdPaymentService;
import org.springframework.stereotype.Service;

@Service
public class OrdPaymentServiceImpl extends ServiceImpl<OrdPaymentMapper, OrdPayment> implements IOrdPaymentService {
}
```

- [ ] **Step 5: Verify scm-order/service compiles**

Run: `mvn clean compile -pl scm-order/service -am -f com.scm.parent/pom.xml`
Expected: SUCCESS (or only errors from missing OrdOrder entity, fixed in Phase 1)

### Task 0.2: Fix scm-purchase malformed imports

**Problem:** 7 files in scm-purchase have corrupted package/import statements (same pattern as scm-order).

**Files:**
- Fix: `scm-purchase/service/src/main/java/scm/purchase/service/impl/PurReceiptServiceImpl.java`
- Fix: `scm-purchase/service/src/main/java/scm/purchase/service/impl/PurRfqServiceImpl.java`
- Fix: `scm-purchase/service/src/main/java/scm/purchase/service/impl/PurRfqItemServiceImpl.java`
- Fix: `scm-purchase/service/src/main/java/scm/purchase/service/impl/PurRequestItemServiceImpl.java`
- Fix: `scm-purchase/service/src/main/java/scm/purchase/service/impl/PurReceiptItemServiceImpl.java`
- Fix: `scm-purchase/service/src/main/java/scm/purchase/service/impl/PurPlanItemServiceImpl.java`
- Fix: `scm-purchase/service/src/main/java/scm/purchase/controller/PurRequestController.java`

- [ ] **Step 1: Read each corrupted file to identify the exact pattern**

- [ ] **Step 2: Fix each file — replace corrupted package/imports with correct ones**

Pattern: `scm-purchase/service.service.impl` → `scm.purchase.service.impl`, `scm-purchase/service/src/main/java.domain.entity.X` → `scm.purchase.domain.entity.X`, etc.

- [ ] **Step 3: Fix PurRequestController.java**

Same pattern — fix package declaration and imports.

- [ ] **Step 4: Verify scm-purchase compiles**

Run: `mvn clean compile -pl scm-purchase/service -am -f com.scm.parent/pom.xml`
Expected: SUCCESS

---

## Phase 1: Missing Core Components (CRITICAL — services cannot start without these)

### Task 1.1: Create missing OrdOrder entity and related components

**Problem:** The main `OrdOrder` entity, mapper, service interface, service impl, and controller are completely missing from scm-order. The database table `ord_order` is defined in `012_db_order.sql` but has no Java counterpart. Multiple files reference `OrdOrderMapper` which doesn't exist.

**Files:**
- Create: `scm-order/service/src/main/java/scm/order/domain/entity/OrdOrder.java`
- Create: `scm-order/service/src/main/java/scm/order/mapper/OrdOrderMapper.java`
- Create: `scm-order/service/src/main/java/scm/order/service/IOrdOrderService.java`
- Create: `scm-order/service/src/main/java/scm/order/service/impl/OrdOrderServiceImpl.java`
- Create: `scm-order/service/src/main/java/scm/order/controller/OrdOrderController.java`

- [ ] **Step 1: Read the database schema for ord_order**

Run: `Read scripts/db/microservices/012_db_order.sql`
Find the `CREATE TABLE ord_order` statement to get column definitions.

- [ ] **Step 2: Read existing entity pattern (e.g., OrdOrderItem.java)**

Run: `Read scm-order/service/src/main/java/scm/order/domain/entity/OrdOrderItem.java`
Use this as a template for annotations, imports, and coding style.

- [ ] **Step 3: Create OrdOrder.java entity**

Map all columns from the SQL schema to Java fields. Use `@TableName("ord_order")`, `@TableId(type = IdType.ASSIGN_ID)`, Lombok `@Data`, logical delete field, partition key `create_time`.

- [ ] **Step 4: Create OrdOrderMapper.java**

```java
package scm.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import scm.order.domain.entity.OrdOrder;

@Mapper
public interface OrdOrderMapper extends BaseMapper<OrdOrder> {
}
```

- [ ] **Step 5: Create IOrdOrderService.java**

```java
package scm.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.order.domain.entity.OrdOrder;

public interface IOrdOrderService extends IService<OrdOrder> {
}
```

- [ ] **Step 6: Create OrdOrderServiceImpl.java**

```java
package scm.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.order.domain.entity.OrdOrder;
import scm.order.mapper.OrdOrderMapper;
import scm.order.service.IOrdOrderService;

@Service
public class OrdOrderServiceImpl extends ServiceImpl<OrdOrderMapper, OrdOrder> implements IOrdOrderService {
}
```

- [ ] **Step 7: Create OrdOrderController.java**

Follow the pattern of existing controllers (e.g., `InvInventoryController`). Include basic CRUD endpoints with Swagger annotations.

- [ ] **Step 8: Fix OrderTccServiceImpl.java references**

Run: `Read scm-order/service/src/main/java/scm/order/service/impl/OrderTccServiceImpl.java`
Verify it now compiles with the new `OrdOrderMapper` and `OrdOrder` entity.

- [ ] **Step 9: Fix OrderTimeoutCancelJobHandler references**

Verify it compiles with the new `OrdOrderMapper`.

- [ ] **Step 10: Verify scm-order compiles**

Run: `mvn clean compile -pl scm-order/service -am -f com.scm.parent/pom.xml`
Expected: SUCCESS

### Task 1.2: Create missing sys_audit_log entity for scm-audit

**Problem:** `005_db_audit.sql` defines `sys_audit_log` table but scm-audit only has `SysSensitiveOperationLog` entity. The main audit log table has no Java counterpart.

**Files:**
- Create: `scm-audit/service/src/main/java/scm/audit/domain/entity/SysAuditLog.java`
- Create: `scm-audit/service/src/main/java/scm/audit/mapper/SysAuditLogMapper.java`
- Create: `scm-audit/service/src/main/java/scm/audit/service/ISysAuditLogService.java`
- Create: `scm-audit/service/src/main/java/scm/audit/service/impl/SysAuditLogServiceImpl.java`
- Create: `scm-audit/service/src/main/java/scm/audit/controller/SysAuditLogController.java`

- [ ] **Step 1: Read the database schema for sys_audit_log**

Run: `Read scripts/db/microservices/005_db_audit.sql`
Find the `CREATE TABLE sys_audit_log` statement.

- [ ] **Step 2: Read existing entity pattern (SysSensitiveOperationLog.java)**

- [ ] **Step 3: Create entity, mapper, service interface, service impl, controller**

Follow same pattern as Task 1.1. Note: `sys_audit_log` is partitioned by `create_time` — entity must include this field.

- [ ] **Step 4: Verify scm-audit compiles**

Run: `mvn clean compile -pl scm-audit/service -am -f com.scm.parent/pom.xml`

### Task 1.3: Create missing tenant_operation_log entity for scm-tenant

**Problem:** `016_db_tenant.sql` defines `tenant_operation_log` table but scm-tenant has no corresponding Java entity.

**Files:**
- Create: `scm-tenant/service/src/main/java/scm/tenant/domain/entity/TenantOperationLog.java`
- Create: `scm-tenant/service/src/main/java/scm/tenant/mapper/TenantOperationLogMapper.java`
- Create: `scm-tenant/service/src/main/java/scm/tenant/service/ITenantOperationLogService.java`
- Create: `scm-tenant/service/src/main/java/scm/tenant/service/impl/TenantOperationLogServiceImpl.java`
- Create: `scm-tenant/service/src/main/java/scm/tenant/controller/TenantOperationLogController.java`

- [ ] **Step 1: Read `016_db_tenant.sql` for tenant_operation_log schema**
- [ ] **Step 2: Create entity, mapper, service, controller (partitioned by create_time)**
- [ ] **Step 3: Verify scm-tenant compiles**

### Task 1.4: Create missing application.yml for 4 services

**Problem:** `scm-product`, `scm-warehouse`, `scm-logistics`, `scm-supplier` have source code but zero configuration files. They cannot start.

**Files:**
- Create: `scm-product/service/src/main/resources/application.yml`
- Create: `scm-warehouse/service/src/main/resources/application.yml`
- Create: `scm-logistics/service/src/main/resources/application.yml`
- Create: `scm-supplier/service/src/main/resources/application.yml`

- [ ] **Step 1: Read scm-inventory/service/src/main/resources/application.yml as template**

- [ ] **Step 2: Create scm-product application.yml**

Port 8201, datasource `db_product`, Nacos, Seata config. Use consistent env var naming (`NACOS_SERVER`, `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD`).

- [ ] **Step 3: Create scm-warehouse application.yml**

Port 8204, datasource `db_warehouse`.

- [ ] **Step 4: Create scm-logistics application.yml**

Port 8205, datasource `db_logistics`.

- [ ] **Step 5: Create scm-supplier application.yml**

Port 8206, datasource `db_supplier`.

- [ ] **Step 6: Verify all 4 services can at least start Spring Boot context**

### Task 1.5: Create Dockerfiles for all services

**Problem:** CI/CD pipeline references `./${{ matrix.service }}/Dockerfile` but no Dockerfiles exist anywhere in the repo.

**Files:**
- Create: `scm-gateway/Dockerfile`
- Create: `scm-auth/Dockerfile`
- Create: `scm-system/Dockerfile` (note: service is at `scm-system/service/`)
- Create: `scm-product/Dockerfile` (at `scm-product/service/`)
- Create: `scm-inventory/Dockerfile` (at `scm-inventory/service/`)
- Create: `scm-order/Dockerfile` (at `scm-order/service/`)
- Create: `scm-warehouse/Dockerfile` (at `scm-warehouse/service/`)
- Create: `scm-logistics/Dockerfile` (at `scm-logistics/service/`)
- Create: `scm-supplier/Dockerfile` (at `scm-supplier/service/`)
- Create: `scm-purchase/Dockerfile` (at `scm-purchase/service/`)
- Create: `scm-finance/Dockerfile` (at `scm-finance/service/`)
- Create: `scm-approval/Dockerfile` (at `scm-approval/service/`)
- Create: `scm-audit/Dockerfile` (at `scm-audit/service/`)
- Create: `scm-notify/Dockerfile` (at `scm-notify/service/`)
- Create: `scm-tenant/Dockerfile` (at `scm-tenant/service/`)

- [ ] **Step 1: Create a base Dockerfile template**

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE {PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Create Dockerfile for each service with correct port**

For flat modules (gateway, auth): context is `scm-gateway/`, `scm-auth/`
For api/service modules: context is `scm-{name}/service/`

- [ ] **Step 3: Verify CI/CD workflow references match Dockerfile locations**

The workflow at `.github/workflows/maven-build.yml` line 164-168 has logic to handle both flat and api/service structures. Verify the `context` and `file` paths match.

### Task 1.6: Fix CI/CD workflow

**Problem:** Multiple issues in `.github/workflows/maven-build.yml`.

**Files:**
- Fix: `.github/workflows/maven-build.yml`

- [ ] **Step 1: Add scm-auth to docker build matrix**

Add `- scm-auth` to the matrix at line 136.

- [ ] **Step 2: Fix build commands to use correct parent POM path**

Line 38: `mvn clean install -DskipTests -B -V` → `mvn clean install -DskipTests -B -V -f com.scm.parent/pom.xml`
Line 41: `mvn test -B` → `mvn test -B -f com.scm.parent/pom.xml`

- [ ] **Step 3: Fix JaCoCo report path for multi-module**

Line 49: `./target/site/jacoco/jacoco.xml` → `**/target/site/jacoco/jacoco.xml`

- [ ] **Step 4: Verify workflow YAML syntax**

---

## Phase 2: Configuration Standardization (HIGH — prevents runtime errors)

### Task 2.1: Standardize environment variable naming

**Problem:** Nacos env var is `NACOS_SERVER` in some services and `NACOS_SERVER_ADDR` in others. DB credentials default to different values.

**Files:**
- Fix: `scm-approval/service/src/main/resources/application.yml`
- Fix: `scm-audit/service/src/main/resources/application.yml`
- Fix: `scm-notify/service/src/main/resources/application.yml`
- Fix: `scm-finance/service/src/main/resources/application.yml`
- Fix: `scm-purchase/service/src/main/resources/application.yml`
- Fix: `scm-tenant/service/src/main/resources/application.yml`

- [ ] **Step 1: Standardize Nacos env var to `NACOS_SERVER` (default: `localhost:8848`)**

In each file, replace `NACOS_SERVER_ADDR` with `NACOS_SERVER`.

- [ ] **Step 2: Standardize DB credentials to `DB_USERNAME`/`DB_PASSWORD` (default: `admin`/`admin123`)**

- [ ] **Step 3: Standardize namespace default to empty string**

- [ ] **Step 4: Verify consistency across all services**

### Task 2.2: Remove hardcoded IPs from scm-auth and scm-system

**Problem:** `scm-auth` has hardcoded `192.168.18.133`, `scm-system` has `192.168.18.134/135/136`.

**Files:**
- Fix: `scm-auth/src/main/resources/application.yaml`
- Fix: `scm-system/service/src/main/resources/application.yaml`

- [ ] **Step 1: Replace hardcoded IPs with env vars**

`192.168.18.133` → `${DB_HOST:localhost}` (and similar for Redis, Sentinel)
`192.168.18.134` → `${DB_MASTER_HOST:localhost}`
`192.168.18.135` → `${DB_SLAVE1_HOST:localhost}`
`192.168.18.136` → `${DB_SLAVE2_HOST:localhost}`

- [ ] **Step 2: Verify configs still parse correctly**

### Task 2.3: Standardize Seata configuration

**Problem:** Some services have full Seata config, some minimal, some none.

**Files:**
- Fix: `scm-purchase/service/src/main/resources/application.yml` (minimal → full)
- Fix: `scm-finance/service/src/main/resources/application.yml` (minimal → full)
- Add to: `scm-approval/service/src/main/resources/application.yml`
- Add to: `scm-audit/service/src/main/resources/application.yml`
- Add to: `scm-tenant/service/src/main/resources/application.yml`

- [ ] **Step 1: Use scm-order's Seata config as reference (full config)**
- [ ] **Step 2: Add full Seata config to services that participate in distributed transactions**
- [ ] **Step 3: Standardize tx-service-group to `${spring.application.name}-tx-group`**

---

## Phase 3: Dubbo API Layer (HIGH — cross-service communication)

### Task 3.1: Create Dubbo API interfaces for 10 modules

**Problem:** Only scm-inventory and scm-order have Dubbo API interfaces. The other 10 modules have empty api/ modules.

**Files to create (one interface per module):**

- `scm-product/api/src/main/java/com/frog/product/api/ProductDubboService.java`
- `scm-warehouse/api/src/main/java/com/frog/warehouse/api/WarehouseDubboService.java`
- `scm-logistics/api/src/main/java/com/frog/logistics/api/LogisticsDubboService.java`
- `scm-supplier/api/src/main/java/com/frog/supplier/api/SupplierDubboService.java`
- `scm-purchase/api/src/main/java/com/frog/purchase/api/PurchaseDubboService.java`
- `scm-finance/api/src/main/java/com/frog/finance/api/FinanceDubboService.java`
- `scm-approval/api/src/main/java/com/frog/approval/api/ApprovalDubboService.java`
- `scm-audit/api/src/main/java/com/frog/audit/api/AuditDubboService.java`
- `scm-notify/api/src/main/java/com/frog/notify/api/NotifyDubboService.java`
- `scm-tenant/api/src/main/java/com/frog/tenant/api/TenantDubboService.java`

- [ ] **Step 1: Read InventoryDubboService.java as reference pattern**
- [ ] **Step 2: Design interfaces for each module based on business requirements**
- [ ] **Step 3: Create interfaces with proper DTOs and exception classes**
- [ ] **Step 4: Verify each api module compiles**

### Task 3.2: Implement Dubbo service providers

**Problem:** Even scm-inventory's `InventoryDubboService` and scm-order's `OrderDubboService` have no `@DubboService` implementation.

**Files to create:**
- `scm-inventory/service/src/main/java/com/frog/inventory/service/dubbo/InventoryDubboServiceImpl.java`
- `scm-order/service/src/main/java/scm/order/service/dubbo/OrderDubboServiceImpl.java`
- Plus implementations for all interfaces created in Task 3.1

- [ ] **Step 1: Create InventoryDubboServiceImpl with @DubboService annotation**
- [ ] **Step 2: Create OrderDubboServiceImpl with @DubboService annotation**
- [ ] **Step 3: Create implementations for remaining modules**
- [ ] **Step 4: Verify Dubbo services register with Nacos on startup**

---

## Phase 4: Business Logic Implementation (MEDIUM — the actual feature work)

This phase has the most work. Each task follows the same pattern: read the database schema, understand the entity relationships, implement CRUD + business logic.

**Reference implementation:** `scm-inventory` module — use `InvInventoryServiceImpl` (385 lines), `InvInventoryController` (248 lines) as the gold standard.

### Task 4.1: scm-product — Implement 5 services

**Entities:** ProdSpu, ProdSku, ProdCategory, ProdBrand, ProdAttributeTemplate

- [ ] **Step 1: Implement ProdCategoryService** — CRUD + tree structure, parent-child relationships
- [ ] **Step 2: Implement ProdBrandService** — CRUD + search by name
- [ ] **Step 3: Implement ProdAttributeTemplateService** — CRUD + attribute management
- [ ] **Step 4: Implement ProdSpuService** — CRUD + category/brand association, SKU management
- [ ] **Step 5: Implement ProdSkuService** — CRUD + attribute value binding, price/stock management
- [ ] **Step 6: Implement all 5 controllers with Swagger annotations**
- [ ] **Step 7: Write unit tests**

### Task 4.2: scm-order — Implement 5 services

**Entities:** OrdOrder (new), OrdOrderItem, OrdPayment, OrdRefund, OrdStatusHistory

- [ ] **Step 1: Implement OrdOrderService** — create order, state machine transitions, timeout cancellation
- [ ] **Step 2: Implement OrdOrderItemService** — line items management
- [ ] **Step 3: Implement OrdPaymentService** — payment integration
- [ ] **Step 4: Implement OrdRefundService** — refund workflow
- [ ] **Step 5: Implement OrdStatusHistoryService** — audit trail
- [ ] **Step 6: Implement OrderController with full CRUD + state transitions**
- [ ] **Step 7: Wire up OrderDubboService implementation**
- [ ] **Step 8: Write unit tests**

### Task 4.3: scm-warehouse — Implement 7 services

**Entities:** WmsWarehouse, WmsLocation, WmsInbound, WmsInboundItem, WmsOutbound, WmsOutboundItem, WmsWavePicking

- [ ] **Step 1: Implement WmsWarehouseService** — multi-warehouse management
- [ ] **Step 2: Implement WmsLocationService** — bin/location management
- [ ] **Step 3: Implement WmsInboundService + WmsInboundItemService** — receiving workflow
- [ ] **Step 4: Implement WmsOutboundService + WmsOutboundItemService** — shipping workflow
- [ ] **Step 5: Implement WmsWavePickingService** — wave picking batch operations
- [ ] **Step 6: Implement all 7 controllers**
- [ ] **Step 7: Write unit tests**

### Task 4.4: scm-logistics — Implement 5 services

**Entities:** TmsWaybill, TmsTracking, TmsRoute, TmsCarrier, TmsDeliveryArea

- [ ] **Step 1: Implement TmsCarrierService** — carrier management
- [ ] **Step 2: Implement TmsDeliveryAreaService** — delivery area/zone management
- [ ] **Step 3: Implement TmsWaybillService** — waybill creation, status tracking
- [ ] **Step 4: Implement TmsTrackingService** — real-time tracking events
- [ ] **Step 5: Implement TmsRouteService** — route management
- [ ] **Step 6: Implement all 5 controllers**
- [ ] **Step 7: Write unit tests**

### Task 4.5: scm-supplier — Implement 4 services

**Entities:** SupSupplier, SupSupplierEvaluation, SupSettlement, SupPurchaseOrderItem

- [ ] **Step 1: Implement SupSupplierService** — supplier CRUD, onboarding workflow
- [ ] **Step 2: Implement SupSupplierEvaluationService** — performance scoring
- [ ] **Step 3: Implement SupSettlementService** — payment settlement
- [ ] **Step 4: Implement SupPurchaseOrderItemService** — purchase order items
- [ ] **Step 5: Implement all 4 controllers**
- [ ] **Step 6: Write unit tests**

### Task 4.6: scm-purchase — Implement 13 services

**Entities:** PurRequest, PurRequestItem, PurPlan, PurPlanItem, PurRfq, PurRfqItem, PurQuotation, PurQuotationItem, PurPriceComparison, PurPriceComparisonItem, PurContract, PurOrderItem, PurReceipt, PurReceiptItem

This is the largest module. Implement in order of business workflow:

- [ ] **Step 1: Implement PurRequestService + PurRequestItemService** — purchase request workflow
- [ ] **Step 2: Implement PurPlanService + PurPlanItemService** — purchase planning
- [ ] **Step 3: Implement PurRfqService + PurRfqItemService** — request for quotation
- [ ] **Step 4: Implement PurQuotationService + PurQuotationItemService** — quotation management
- [ ] **Step 5: Implement PurPriceComparisonService + PurPriceComparisonItemService** — price comparison
- [ ] **Step 6: Implement PurContractService** — contract management
- [ ] **Step 7: Implement PurOrderItemService** — order items
- [ ] **Step 8: Implement PurReceiptService + PurReceiptItemService** — goods receipt
- [ ] **Step 9: Implement all 13 controllers**
- [ ] **Step 10: Write unit tests**

### Task 4.7: scm-finance — Implement 6 services

**Entities:** FreightRule, SettlementOrder, SettlementItem, Invoice, ReconciliationRecord, PlatformServiceFee

- [ ] **Step 1: Implement FreightRuleService** — freight calculation rules
- [ ] **Step 2: Implement SettlementOrderService + SettlementItemService** — settlement workflow
- [ ] **Step 3: Implement InvoiceService** — invoice generation
- [ ] **Step 4: Implement ReconciliationRecordService** — reconciliation
- [ ] **Step 5: Implement PlatformServiceFeeService** — platform fees
- [ ] **Step 6: Implement all 6 controllers**
- [ ] **Step 7: Write unit tests**

### Task 4.8: scm-approval — Implement 1 service

**Entities:** SysPermissionApproval

- [ ] **Step 1: Implement SysPermissionApprovalService** — approval workflow (pending → approved/rejected)
- [ ] **Step 2: Implement controller with approval endpoints**
- [ ] **Step 3: Write unit tests**

### Task 4.9: scm-audit — Implement 2 services

**Entities:** SysSensitiveOperationLog, SysAuditLog (new)

- [ ] **Step 1: Implement SysAuditLogService** — query/filter audit logs
- [ ] **Step 2: Implement SysSensitiveOperationLogService** — sensitive operation tracking
- [ ] **Step 3: Implement both controllers**
- [ ] **Step 4: Write unit tests**

### Task 4.10: scm-notify — Implement 3 services

**Entities:** SysNotificationTemplate, SysNotificationAudit, SysUserNotificationPreference

- [ ] **Step 1: Implement SysNotificationTemplateService** — template CRUD
- [ ] **Step 2: Implement SysNotificationAuditService** — notification sending + audit
- [ ] **Step 3: Implement SysUserNotificationPreferenceService** — user preferences
- [ ] **Step 4: Implement all 3 controllers**
- [ ] **Step 5: Write unit tests**

### Task 4.11: scm-tenant — Implement 7 services

**Entities:** Tenant, TenantPackage, TenantSubscription, TenantResourceQuota, TenantConfig, TenantFeature, TenantOperationLog (new)

- [ ] **Step 1: Implement TenantService** — tenant CRUD, lifecycle management
- [ ] **Step 2: Implement TenantPackageService** — package/plan management
- [ ] **Step 3: Implement TenantSubscriptionService** — subscription management
- [ ] **Step 4: Implement TenantResourceQuotaService** — quota management
- [ ] **Step 5: Implement TenantConfigService** — tenant-specific configuration
- [ ] **Step 6: Implement TenantFeatureService** — feature flags per tenant
- [ ] **Step 7: Implement TenantOperationLogService** — operation audit trail
- [ ] **Step 8: Implement all 7 controllers**
- [ ] **Step 9: Write unit tests**

---

## Phase 5: Fix TODO/FIXME Items (MEDIUM)

### Task 5.1: Fix critical TODOs in scm-common

- [ ] **Step 1: Fix AuditMetaObjectHandler.getCurrentUser()** — integrate with SecurityContextHolder to return actual user ID instead of null
- [ ] **Step 2: Fix TenantFilter JWT extraction** — extract tenant ID from JWT token
- [ ] **Step 3: Fix TenantInterceptor for UPDATE/DELETE/INSERT** — extend tenant filtering beyond SELECT
- [ ] **Step 4: Fix PermissionCheckUtil data scope** — implement data scope permission checks

### Task 5.2: Fix TODOs in scm-system

- [ ] **Step 1: Implement direct permission grant logic** in SysPermissionApprovalServiceImpl
- [ ] **Step 2: Implement notification integration** in PermissionExpiryTask and ApprovalCleanupTask
- [ ] **Step 3: Implement custom data permission rules** in PermissionQueryServiceImpl

---

## Phase 6: Documentation & Cleanup (LOW)

### Task 6.1: Update database README

- [ ] **Step 1: Extend `scripts/db/microservices/README.md` to document databases 010-021**

### Task 6.2: Fix Canal configuration

- [ ] **Step 1: Remove or fix Canal config in docker-compose.yml** — Canal only works with MySQL, not PostgreSQL. Either remove it or replace with Debezium/PostgreSQL WAL-based CDC.

### Task 6.3: Add owasp-suppressions.xml

- [ ] **Step 1: Create `owasp-suppressions.xml`** referenced by CI/CD workflow, or remove the reference.

---

## Execution Order

Execute phases in order 0 → 1 → 2 → 3 → 4 → 5 → 6. Within each phase, tasks can be parallelized.

**Estimated effort:**
- Phase 0: ~30 minutes (fix compilation errors)
- Phase 1: ~2 hours (missing core components)
- Phase 2: ~1 hour (config standardization)
- Phase 3: ~3 hours (Dubbo API layer)
- Phase 4: ~40 hours (business logic — largest phase)
- Phase 5: ~4 hours (TODO fixes)
- Phase 6: ~1 hour (documentation)

**Total: ~51 hours of work**
