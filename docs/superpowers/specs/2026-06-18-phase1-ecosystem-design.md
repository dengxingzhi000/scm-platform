# SCM Platform Phase 1 Ecosystem Design

## Overview

This document outlines the design for Phase 1 features of the SCM Platform ecosystem, targeting completion within 6 months with a large development team.

**Goal**: Build foundational infrastructure and platform services that enable all other modules and integrations.

**Architecture Tier Model**:
```
Tier0 (Infrastructure)
├── scm-common
├── scm-cache
├── scm-message
├── scm-file
└── scm-search

Tier1 (Business Core)
├── product, inventory, purchase, order, supplier
├── price (extend scm-product)
└── promotion (new)

Tier2 (Platform Capabilities)
├── openapi
├── workflow, approval
├── notification
└── dict

Tier3 (Data Capabilities)
├── report
├── BI
├── forecast
└── AI

Tier4 (Ecosystem)
├── WMS, TMS
├── ERP, MES, CRM
└── EDI
```

---

## 1. scm-message (Event Bus) — NEW, HIGHEST PRIORITY

### 1.1 Objective

Provide unified event-driven architecture for all domain events, enabling loose coupling between services and supporting future BI, AI, and data lake requirements.

### 1.2 Technical Stack

- Apache Kafka (existing in POM)
- Outbox pattern for transactional events
- Dead letter queue for failed events

### 1.3 Module Structure

```
scm-message/
├── api/                    # Dubbo RPC interfaces
│   ├── EventPublisherApi   # Event publishing
│   └── EventConsumerApi    # Event consumption
├── service/                # Implementation
│   ├── producer/           # Event producers
│   │   ├── KafkaProducer   # Kafka producer
│   │   └── OutboxService   # Outbox pattern
│   ├── consumer/           # Event consumers
│   │   ├── ConsumerRegistry # Consumer registry
│   │   └── RetryService    # Retry mechanism
│   └── deadletter/         # Dead letter queue
│       ├── DlqService      # DLQ management
│       └── DlqRetry        # DLQ retry
```

### 1.4 Event Definition

```java
public interface DomainEvent {
    String getEventId();
    String getEventType();
    String getAggregateType();
    String getAggregateId();
    Long getTenantId();
    Date getTimestamp();
    Map<String, Object> getPayload();
}

// Example events
public class OrderCreatedEvent implements DomainEvent { ... }
public class InventoryChangedEvent implements DomainEvent { ... }
public class PriceChangedEvent implements DomainEvent { ... }
public class PromotionStartedEvent implements DomainEvent { ... }
public class SupplierUpdatedEvent implements DomainEvent { ... }
```

### 1.5 Outbox Pattern

```java
@Data
@TableName("sys_event_outbox")
public class EventOutbox {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String eventType;
    private String aggregateType;
    private String aggregateId;
    private String payload;          // JSON
    private Integer retryCount;
    private OutboxStatus status;
    private Date createdAt;
    private Date publishedAt;
}
```

### 1.6 Event Topics

| Topic | Producer | Consumers |
|-------|----------|-----------|
| scm.order.events | scm-order | search, report, notification |
| scm.inventory.events | scm-inventory | search, report, notification |
| scm.product.events | scm-product | search, report |
| scm.price.events | scm-product | search, promotion |
| scm.supplier.events | scm-supplier | search, report |

---

## 2. scm-file (File Storage Service)

### 2.1 Objective

Provide unified file storage service supporting multiple storage backends for all modules.

### 2.2 Technical Stack

- MinIO (S3 compatible) / Alibaba Cloud OSS / AWS S3
- SPI + Factory pattern for storage engine selection
- Redis metadata cache

### 2.3 Module Structure

```
scm-file/
├── api/                    # Dubbo RPC interfaces
│   ├── FileUploadApi       # File upload
│   ├── FileDownloadApi     # File download
│   └── FileManageApi       # File management
├── service/                # Implementation
│   ├── storage/            # Storage engines (SPI)
│   │   ├── StorageEngine   # SPI interface
│   │   ├── StorageFactory  # Factory
│   │   ├── MinioStorageEngine
│   │   ├── OssStorageEngine
│   │   └── S3StorageEngine
│   ├── upload/             # Upload services
│   │   ├── UploadService   # Upload orchestration
│   │   ├── InstantUpload   # Instant upload (MD5 check)
│   │   └── ResumeUpload    # Resume upload
│   ├── metadata/           # Metadata management
│   │   ├── FileMetadata    # File metadata
│   │   └── FileVersion     # File version
│   └── preview/            # File preview
│       ├── ImagePreview    # Image preview
│       └── DocumentPreview # Document preview
```

### 2.4 Storage SPI

```java
public interface StorageEngine {
    FileMetadata upload(InputStream stream, String fileName, String contentType);
    InputStream download(String fileKey);
    String generatePresignedUrl(String fileKey, Duration expiry);
    void delete(String fileKey);
    boolean exists(String fileKey);
}

public class StorageFactory {
    public static StorageEngine create(StorageType type) {
        return switch (type) {
            case MINIO -> new MinioStorageEngine();
            case OSS -> new OssStorageEngine();
            case S3 -> new S3StorageEngine();
        };
    }
}
```

### 2.5 Instant Upload (MD5 Dedup)

```java
public interface FileUploadApi {
    // Check if file exists (instant upload)
    FileMetadata checkExist(String md5);
    
    // Single file upload
    FileMetadata upload(MultipartFile file, String bizType, String bizId);
    
    // Batch upload
    List<FileMetadata> batchUpload(List<MultipartFile> files, String bizType, String bizId);
    
    // Multipart upload (large files)
    String initMultipartUpload(String fileName, Long fileSize);
    String uploadPart(String uploadId, int partNumber, InputStream data);
    FileMetadata completeMultipartUpload(String uploadId);
}
```

### 2.6 Upload Task Table

```java
@Data
@TableName("sys_upload_task")
public class UploadTask {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fileName;
    private Long fileSize;
    private String md5;
    private String storageKey;
    private Integer totalParts;
    private Integer completedParts;
    private UploadTaskStatus status;  // PENDING, UPLOADING, PAUSED, COMPLETED, FAILED
    private String uploadId;          // Multipart upload ID
    private Long tenantId;
    private Date createTime;
    private Date updateTime;
}
```

### 2.7 File Metadata

```java
@Data
@TableName("sys_file_metadata")
public class FileMetadata {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String originalName;
    private String storageKey;
    private String contentType;
    private Long fileSize;
    private String storageEngine;
    private String bucketName;
    private String md5;
    private Integer version;
    private Long tenantId;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;
    private Integer version;          // Optimistic lock
}
```

---

## 3. scm-search (Elasticsearch Search Service)

### 3.1 Objective

Provide unified full-text search capabilities for products, orders, suppliers, and other core business objects.

### 3.2 Technical Stack

- Elasticsearch 8.11.4 (already in POM)
- Spring Data Elasticsearch
- Debezium + Kafka for data synchronization (NOT Canal - Canal is MySQL only)

### 3.3 Data Sync Architecture

```
PostgreSQL
    ↓
Debezium (CDC)
    ↓
Kafka
    ↓
scm-search (index to ES)
    ↓
Elasticsearch
```

This architecture enables future consumers:
- BI/analytics
- AI/data lake
- Audit logging
- Real-time dashboards

### 3.4 Module Structure

```
scm-search/
├── api/                    # Dubbo RPC interfaces
│   ├── ProductSearchApi
│   ├── OrderSearchApi
│   └── SupplierSearchApi
├── service/                # Implementation
│   ├── index/              # Index management
│   │   ├── IndexManager    # Index lifecycle
│   │   ├── ProductIndex
│   │   ├── OrderIndex
│   │   └── SupplierIndex
│   ├── sync/               # Data sync
│   │   ├── KafkaConsumer   # Kafka consumer
│   │   └── FullSyncService # Full sync
│   └── query/              # Query services
│       ├── SearchService
│       └── AggregationService
```

### 3.5 Enhanced Product Index

```java
@Document(indexName = "product_v1")  // Versioned index name
public class ProductIndex {
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String name;
    
    @Field(type = FieldType.Keyword)
    private String skuCode;
    
    @Field(type = FieldType.Keyword)
    private String categoryCode;
    
    @Field(type = FieldType.Keyword)
    private String categoryPath;      // Full category path for hierarchical search
    
    @Field(type = FieldType.Keyword)
    private String brandCode;
    
    @Field(type = FieldType.Keyword)
    private String tags;              // Comma-separated tags
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    @Field(type = FieldType.Keyword)
    private Long tenantId;
    
    @Field(type = FieldType.Double)
    private BigDecimal price;
    
    @Field(type = FieldType.Integer)
    private Integer stock;
    
    @Field(type = FieldType.Long)
    private Long saleCount;           // Sales count for ranking
    
    @Field(type = FieldType.Double)
    private Double salesVolume;       // Sales volume for ranking
    
    @Field(type = FieldType.Double)
    private Double score;             // Composite score for ranking
    
    @Field(type = FieldType.Completion)
    private CompletionField suggest;  // For auto-complete
    
    @Field(type = FieldType.Date)
    private Date createTime;
    
    @Field(type = FieldType.Date)
    private Date updateTime;
}
```

### 3.6 Index Alias Strategy

```
product (alias)
    → product_v1 (current index)

Future:
    → product_v2 (new index, zero-downtime migration)
```

### 3.7 Search API

```java
public interface ProductSearchApi {
    SearchResult<ProductIndex> search(SearchRequest request);
    AggregationResult aggregate(AggregationRequest request);
    List<String> suggest(String prefix, int limit);
}
```

---

## 4. scm-cache (Cache Center) — NEW

### 4.1 Objective

Provide unified cache management with standardized key patterns, preventing cache key conflicts and enabling centralized cache monitoring.

### 4.2 Technical Stack

- Redis (existing)
- Caffeine (L1 cache)
- Two-level cache pattern (existing in scm-common)

### 4.3 Module Structure

```
scm-cache/
├── api/                    # Dubbo RPC interfaces
│   ├── CacheApi            # Cache operations
│   └── CacheMonitorApi     # Cache monitoring
├── service/                # Implementation
│   ├── key/                # Key management
│   │   ├── CacheKeyBuilder # Standardized key builder
│   │   └── CacheKeyRegistry # Key registry
│   ├── local/              # Local cache
│   │   └── CaffeineCache   # Caffeine implementation
│   └── remote/             # Remote cache
│       └── RedisCache      # Redis implementation
```

### 4.4 Standardized Key Patterns

```java
public final class CacheKeys {
    // Product
    public static final String PRODUCT = "PRODUCT:";
    public static final String PRODUCT_SKU = "PRODUCT:SKU:";
    public static final String PRODUCT_PRICE = "PRODUCT:PRICE:";
    
    // Order
    public static final String ORDER = "ORDER:";
    public static final String ORDER_NO = "ORDER:NO:";
    
    // Inventory
    public static final String INVENTORY = "INVENTORY:";
    public static final String INVENTORY_SKU = "INVENTORY:SKU:";
    
    // User
    public static final String USER = "USER:";
    public static final String USER_PERMISSION = "USER:PERM:";
    
    // Supplier
    public static final String SUPPLIER = "SUPPLIER:";
    
    // Price
    public static final String PRICE_LIST = "PRICE:LIST:";
    public static final String PRICE_ITEM = "PRICE:ITEM:";
    
    // Promotion
    public static final String PROMOTION = "PROMOTION:";
    public static final String COUPON = "COUPON:";
    
    // Dictionary
    public static final String DICT = "DICT:";
    public static final String DICT_TYPE = "DICT:TYPE:";
}
```

### 4.5 Cache Key Builder

```java
@Component
public class CacheKeyBuilder {
    public String build(String prefix, String... parts) {
        StringBuilder key = new StringBuilder(prefix);
        for (String part : parts) {
            key.append(part).append(":");
        }
        return key.toString();
    }
    
    // Tenant-aware key
    public String buildTenantKey(Long tenantId, String prefix, String... parts) {
        return "T:" + tenantId + ":" + build(prefix, parts);
    }
}
```

---

## 5. scm-openapi (API Open Platform) — NEW MODULE

### 5.1 Objective

Provide unified API management, developer portal, and integration capabilities. **Separate from scm-gateway** to maintain gateway's single responsibility.

### 5.2 Module Structure

```
scm-openapi/
├── api/                    # Dubbo RPC interfaces
│   ├── ApiRegistryApi      # API registry
│   ├── DeveloperApi        # Developer management
│   └── AnalyticsApi        # API analytics
├── service/                # Implementation
│   ├── registry/           # API registry
│   │   ├── ApiRegistry     # API registration
│   │   ├── ApiVersion      # Version management
│   │   └── ApiLifecycle    # Lifecycle management
│   ├── developer/          # Developer portal
│   │   ├── DeveloperApp    # Developer applications
│   │   ├── ApiKeyService   # API key management
│   │   ├── SandboxService  # Sandbox environment
│   │   └── SdkGenerator    # SDK generation
│   ├── security/           # API security
│   │   ├── OAuth2Service   # OAuth 2.0
│   │   ├── SignatureService # Request signature
│   │   └── RateLimitService # Rate limiting
│   └── analytics/          # API analytics
│       ├── UsageService    # Usage statistics
│       └── MonitorService  # Performance monitoring
```

### 5.3 API Registry

```java
@Data
@TableName("sys_api_definition")
public class ApiDefinition {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String serviceId;
    private String path;
    private String method;
    private String name;
    private String description;
    private String version;
    private ApiStatus status;
    private String category;
    private String tags;
    private Boolean isPublic;
    private Integer rateLimit;
    private String authType;
    private Long tenantId;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;
    private Integer version;          // Optimistic lock
}
```

### 5.4 Developer Application

```java
@Data
@TableName("sys_developer_app")
public class DeveloperApp {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String appName;
    private String appKey;
    private String appSecret;
    private String developerId;
    private List<String> scopes;
    private AppStatus status;
    private Long tenantId;
    private Date createTime;
    private Date updateTime;
}
```

### 5.5 Gateway Remains Pure

scm-gateway responsibilities (unchanged):
- Route
- Authentication
- Rate limiting
- Request filtering

scm-openapi handles:
- API registry
- Developer portal
- SDK generation
- API analytics

---

## 6. scm-dict (Dictionary Center) — NEW

### 6.1 Objective

Provide centralized dictionary and enum management, avoiding frequent releases for status code changes.

### 6.2 Module Structure

```
scm-dict/
├── api/                    # Dubbo RPC interfaces
│   ├── DictApi             # Dictionary operations
│   └── DictTypeApi         # Dictionary type management
├── service/                # Implementation
│   ├── DictService         # Dictionary service
│   ├── DictTypeService     # Dictionary type service
│   └── DictCacheService    # Dictionary cache
```

### 6.3 Dictionary Type

```java
@Data
@TableName("sys_dict_type")
public class DictType {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String typeCode;          // e.g., "order_status", "payment_status"
    private String typeName;
    private String description;
    private Boolean isSystem;         // System preset, cannot delete
    private Long tenantId;
    private Date createTime;
    private Date updateTime;
}
```

### 6.4 Dictionary Data

```java
@Data
@TableName("sys_dict_data")
public class DictData {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String typeCode;
    private String label;
    private String value;
    private Integer sortOrder;
    private String cssClass;
    private String remark;
    private Boolean isDefault;
    private Long tenantId;
    private Date createTime;
    private Date updateTime;
}
```

### 6.5 Pre-defined Dictionary Types

| Type Code | Description | Examples |
|-----------|-------------|----------|
| order_status | Order status | PENDING_PAYMENT, PAID, SHIPPED, COMPLETED |
| payment_status | Payment status | UNPAID, PAID, REFUNDED |
| supplier_level | Supplier level | A, B, C, D |
| inventory_status | Inventory status | AVAILABLE, RESERVED, LOCKED |
| price_list_type | Price list type | RETAIL, WHOLESALE, AGREEMENT |
| promotion_type | Promotion type | DISCOUNT, FULL_REDUCE, COUPON |

---

## 7. Price Management (Extend scm-product)

### 7.1 Objective

Provide flexible price management system supporting multiple pricing strategies with B2B dimension support.

### 7.2 Extended Structure

```
scm-product extensions:
├── price/
│   ├── PriceList
│   ├── PriceItem
│   ├── PriceDimension       # NEW: B2B dimensions
│   ├── PriceHistory
│   └── PriceApproval
├── pricing-strategy/
│   ├── CostPlusPricing
│   ├── MarketPricing
│   ├── DynamicPricing
│   └── TierPricing
└── price-query/
    ├── PriceCalculator
    └── PriceComparator
```

### 7.3 Price Dimension (B2B Support)

```java
@Data
@TableName("prod_price_dimension")
public class PriceDimension {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String priceListId;
    private String productId;
    private String skuId;
    
    // B2B dimensions
    private String customerId;        // Specific customer
    private String customerGroupId;   // Customer group
    private String regionCode;        // Region
    private String channelCode;       // Sales channel
    private String currency;          // Currency
    private String warehouseId;       // Warehouse
    
    private BigDecimal basePrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String tierPricing;       // JSON: [{"minQty":1,"maxQty":99,"price":100}]
    
    private Long tenantId;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;
    private Integer version;          // Optimistic lock
}
```

### 7.4 Price Calculation with Dimensions

```java
public interface PriceQueryApi {
    // Query with dimensions
    PriceInfo getProductPrice(String productId, String skuId, PriceDimension dimension);
    
    // Batch query
    List<PriceInfo> batchGetPrice(List<PriceQuery> queries);
    
    // Price calculation (considering promotions)
    PriceCalculation calculatePrice(PriceCalculationRequest request);
}
```

### 7.5 Price-Promotion Dependency (No Circular)

```
Order Service
    ↓
Price Service (calculate base price)
    ↓
Promotion Service (apply discounts)
    ↓
Final Price
```

---

## 8. scm-promotion (Promotion Management)

### 8.1 Objective

Provide flexible promotion and discount management capabilities using lightweight rule engine.

### 8.2 Technical Stack

- Aviator or QLExpress (lightweight, NOT Drools)
- Redis cache for promotion rules
- Kafka for promotion events

### 8.3 Module Structure

```
scm-promotion/
├── api/                    # Dubbo RPC interfaces
│   ├── PromotionApi
│   ├── CouponApi
│   └── DiscountApi
├── service/                # Implementation
│   ├── promotion/
│   │   ├── PromotionEngine
│   │   ├── PromotionRule
│   │   └── PromotionCalc
│   ├── coupon/
│   │   ├── CouponGenerator
│   │   ├── CouponValidator
│   │   └── CouponExecutor
│   └── campaign/
│       ├── CampaignManager
│       ├── CampaignBudget
│       └── CampaignAnalytics
```

### 8.4 Promotion Model (unchanged, already good)

```java
@Data
@TableName("prm_promotion")
public class Promotion {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private PromotionType type;
    private Date startDate;
    private Date endDate;
    private PromotionStatus status;
    private String rules;
    private Integer priority;
    private Boolean stackable;
    private BigDecimal budget;
    private BigDecimal usedBudget;
    private Long tenantId;
    private Long createBy;
    private Long updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer deleted;
    private Integer version;          // Optimistic lock
}
```

### 8.5 Lightweight Rule Engine (Aviator)

```java
@Component
public class PromotionRuleEngine {
    
    public DiscountResult evaluate(Promotion promotion, OrderContext context) {
        String ruleExpression = promotion.getRules();
        
        // Aviator expression evaluation
        Expression expression = AviatorEvaluator.compile(ruleExpression);
        Map<String, Object> env = buildEnvironment(context);
        
        Object result = expression.execute(env);
        return (DiscountResult) result;
    }
    
    private Map<String, Object> buildEnvironment(OrderContext context) {
        Map<String, Object> env = new HashMap<>();
        env.put("totalAmount", context.getTotalAmount());
        env.put("quantity", context.getQuantity());
        env.put("categoryCode", context.getCategoryCode());
        env.put("customerId", context.getCustomerId());
        return env;
    }
}
```

---

## 9. scm-report (Simplified BI)

### 9.1 Objective

Provide basic reporting capabilities using PostgreSQL directly, without heavy infrastructure.

### 9.2 Technical Stack

- PostgreSQL (existing, no Spark/Presto)
- EasyExcel for export
- ECharts for visualization
- Redis for report caching

### 9.3 Module Structure

```
scm-report/
├── api/                    # Dubbo RPC interfaces
│   ├── ReportApi           # Report query
│   └── ExportApi           # Data export
├── service/                # Implementation
│   ├── report/
│   │   ├── ReportService   # Report service
│   │   ├── ReportTemplate  # Report templates
│   │   └── ReportCache     # Report caching
│   └── export/
│       ├── ExcelExporter   # EasyExcel export
│       ├── PdfExporter     # PDF export
│       └── CsvExporter     # CSV export
```

### 9.4 Pre-built Reports (Phase 1)

| Report | Data Source | Description |
|--------|------------|-------------|
| Sales Daily | ord_order | Daily sales summary |
| Sales Monthly | ord_order | Monthly sales summary |
| Inventory Status | inv_inventory | Current inventory levels |
| Inventory Turnover | inv_inventory | Turnover rate calculation |
| Purchase Summary | sup_purchase_order | Purchase order summary |
| Supplier Performance | sup_supplier | Supplier delivery metrics |

### 9.5 Report Definition

```java
@Data
@TableName("rpt_report_definition")
public class ReportDefinition {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private ReportType type;
    private String sqlTemplate;
    private String chartConfig;       // ECharts config (JSON)
    private String params;            // Parameters (JSON)
    private Boolean isSystem;
    private Long tenantId;
    private Date createTime;
    private Date updateTime;
}
```

### 9.6 Export API

```java
public interface ExportApi {
    // Async export (large data)
    String exportAsync(String reportId, Map<String, Object> params, ExportFormat format);
    
    // Check export status
    ExportStatus getExportStatus(String exportId);
    
    // Download
    String getDownloadUrl(String exportId);
}
```

---

## 10. scm-notification (Notification Center) — EXPAND scm-notify

### 10.1 Objective

Expand existing scm-notify module to support multi-channel notifications.

### 10.2 Channels

| Channel | Implementation |
|---------|----------------|
| In-app | Database + WebSocket |
| SMS | Alibaba Cloud SMS / Twilio |
| Email | Spring Mail |
| Webhook | HTTP client |
| WeCom | WeCom API |
| DingTalk | DingTalk API |

### 10.3 Notification Template

```java
@Data
@TableName("sys_notification_template")
public class NotificationTemplate {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String code;
    private String name;
    private String channel;           // sms, email, webhook, etc.
    private String titleTemplate;
    private String contentTemplate;
    private Map<String, String> params;
    private Long tenantId;
}
```

---

## Database Design Standards

All new tables MUST include:

```sql
CREATE TABLE table_name (
    id          VARCHAR(36) PRIMARY KEY,
    -- business columns --
    tenant_id   BIGINT NOT NULL,
    create_by   BIGINT,
    update_by   BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted     INTEGER DEFAULT 0,
    version     INTEGER DEFAULT 0  -- Optimistic lock
);
```

### Optimistic Lock Usage

Required for:
- `prod_price_item.version`
- `prod_price_dimension.version`
- `prm_promotion.version` (budget updates)
- `prm_coupon.version` (usage count)
- `inv_inventory.version` (stock updates)

---

## Implementation Order (Revised)

```
Month 1:
├── scm-file (foundation for all modules)
└── scm-message (event bus, foundation for sync)

Month 2:
└── scm-search (depends on message for sync)

Month 3:
├── scm-openapi (API platform)
└── scm-cache (cache center)

Month 4:
├── Price Management (extend scm-product)
└── scm-dict (dictionary center)

Month 5:
└── Promotion (depends on price)

Month 6:
└── scm-notification (expand scm-notify)
    scm-report (simplified BI)

Phase 2:
├── Workflow/Approval (Flowable)
├── Advanced BI (Superset integration)
└── Rich domain model

Phase 3:
├── AI/ML (demand forecasting)
├── Advanced analytics
└── Data warehouse

Phase 4:
├── WMS advanced
├── TMS advanced
├── ERP/MES/CRM integration
└── EDI
```

---

## Module Dependency Graph

```
scm-file ──────────────────────────────────┐
scm-message ───────────────────────────────┤
    ↓                                      │
scm-search ────────────────────────────────┤
    ↓                                      │
scm-openapi ───────────────────────────────┤
scm-cache ─────────────────────────────────┤
    ↓                                      │
Price Management ──────────────────────────┤
    ↓                                      │
Promotion ─────────────────────────────────┤
    ↓                                      │
scm-notification ──────────────────────────┤
scm-dict ──────────────────────────────────┤
    ↓                                      │
scm-report ────────────────────────────────┘
```

---

## Summary of Changes from Original Design

| Change | Rationale |
|--------|-----------|
| Added scm-message (event bus) | Foundation for all async operations |
| Added scm-cache | Centralized cache management |
| Added scm-dict | Avoid enum releases |
| Replaced Canal with Debezium+Kafka | Canal is MySQL only, PostgreSQL needs Debezium |
| Simplified scm-report | Removed Spark/Presto/Superset, use PostgreSQL directly |
| Created scm-openapi (new module) | Keep gateway pure, single responsibility |
| Added PriceDimension | B2B support (customer, region, channel) |
| Used Aviator instead of Drools | Lightweight, sufficient for 90% rules |
| Enhanced ProductIndex | More fields, versioned index names |
| Added instant upload, resume upload | Better UX for file uploads |
| Added optimistic locking | Critical for price, coupon, promotion |
| Adjusted implementation order | Infrastructure first, then business |
