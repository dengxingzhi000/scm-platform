# Phase 2: 商品服务 + Elasticsearch 搜索集成指南

**文档版本**: v1.0
**创建日期**: 2025-12-26
**作者**: SCM Platform Team

---

## 📋 目录

1. [项目概述](#1-项目概述)
2. [Elasticsearch 集成](#2-elasticsearch-集成)
3. [商品搜索文档设计](#3-商品搜索文档设计)
4. [搜索 API 实现](#4-搜索-api-实现)
5. [Canal 数据同步](#5-canal-数据同步)
6. [性能优化](#6-性能优化)
7. [测试验收](#7-测试验收)

---

## 1. 项目概述

### 1.1 Phase 2 目标

实现商品服务的完整 CRUD，集成 Elasticsearch 实现高性能商品搜索，集成 Canal 实现 PostgreSQL 到 ES 的实时数据同步。

### 1.2 技术选型

| 组件 | 版本 | 说明 |
|-----|------|------|
| **Elasticsearch** | 8.11.4 | 搜索引擎 |
| **Spring Data Elasticsearch** | 5.2.0 | Spring 集成 |
| **IK 分词器** | 8.11.4 | 中文分词 |
| **Canal** | 1.1.7 | 数据同步（PostgreSQL binlog → ES） |
| **PostgreSQL** | 16 | 主数据库 |

### 1.3 核心功能

- ✅ **商品全文搜索**: 支持 spuName, description, seoKeywords 多字段搜索
- ✅ **分类/品牌过滤**: 支持按分类和品牌筛选
- ✅ **价格区间过滤**: 支持价格范围查询
- ✅ **多种排序**: 销量、价格、发布时间、更新时间
- ✅ **实时数据同步**: Canal 监听 PostgreSQL WAL，实时同步到 ES
- ✅ **高性能**: 搜索响应时间 < 100ms

---

## 2. Elasticsearch 集成

### 2.1 添加依赖

在 `scm-product/service/pom.xml` 中添加：

```xml
<dependencies>
    <!-- Spring Data Elasticsearch -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>

    <!-- Elasticsearch REST Client -->
    <dependency>
        <groupId>co.elastic.clients</groupId>
        <artifactId>elasticsearch-java</artifactId>
        <version>8.11.4</version>
    </dependency>
</dependencies>
```

### 2.2 配置 Elasticsearch 连接

`scm-product/service/src/main/resources/application.yml`:

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: ${ELASTICSEARCH_PASSWORD:changeme}
    connection-timeout: 30s
    socket-timeout: 60s

  data:
    elasticsearch:
      repositories:
        enabled: true
```

### 2.3 配置类

`scm-product/service/src/main/java/scm/product/config/ElasticsearchConfig.java`:

```java
package scm.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "scm.product.search.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Override
    public ClientConfiguration clientConfiguration() {
        return ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .withBasicAuth("elastic", "changeme")
                .withConnectTimeout(Duration.ofSeconds(30))
                .withSocketTimeout(Duration.ofSeconds(60))
                .build();
    }
}
```

---

## 3. 商品搜索文档设计

### 3.1 文档类

已创建文件：
- `scm-product/service/src/main/java/scm/product/search/document/ProductDocument.java`

**关键设计**:

```java
@Document(indexName = "scm_product", createIndex = true)
@Setting(shards = 5, replicas = 1)
public class ProductDocument {
    @Id
    private String id;

    // 使用 IK 分词器进行中文分词
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_max_word")
    private String spuName;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;

    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Double)
    private BigDecimal minPrice;

    // ... 其他字段
}
```

### 3.2 索引映射说明

| 字段 | 类型 | 分词器 | 说明 |
|-----|------|-------|------|
| **spuName** | Text | ik_max_word | 商品名称，支持全文搜索，权重最高（^3） |
| **description** | Text | ik_max_word | 商品描述，支持全文搜索，权重中等（^2） |
| **seoKeywords** | Text | ik_max_word | SEO 关键词，支持全文搜索 |
| **categoryId** | Keyword | - | 分类 ID，用于精确匹配和聚合 |
| **brandId** | Keyword | - | 品牌 ID，用于精确匹配和聚合 |
| **minPrice/maxPrice** | Double | - | 价格，支持范围查询 |
| **totalSales** | Integer | - | 销量，用于排序 |
| **status** | Integer | - | 状态，用于过滤（仅展示上架商品） |
| **publishedAt** | Date | - | 发布时间，用于排序 |

### 3.3 IK 分词器配置

**安装 IK 分词器**:

```bash
# 进入 Elasticsearch 容器
docker exec -it elasticsearch bash

# 安装 IK 分词器
elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.4/elasticsearch-analysis-ik-8.11.4.zip

# 重启 Elasticsearch
docker restart elasticsearch
```

**验证分词效果**:

```bash
curl -X POST "localhost:9200/_analyze" -H 'Content-Type: application/json' -d'
{
  "analyzer": "ik_max_word",
  "text": "iPhone 15 Pro Max 256GB 钛金属"
}
'
```

预期结果：
```json
{
  "tokens": [
    {"token": "iphone"},
    {"token": "15"},
    {"token": "pro"},
    {"token": "max"},
    {"token": "256"},
    {"token": "gb"},
    {"token": "钛"},
    {"token": "金属"}
  ]
}
```

---

## 4. 搜索 API 实现

### 4.1 已创建文件

- `scm-product/service/src/main/java/scm/product/search/repository/ProductSearchRepository.java`
- `scm-product/service/src/main/java/scm/product/search/dto/ProductSearchRequest.java`
- `scm-product/service/src/main/java/scm/product/search/dto/ProductSearchResponse.java`

### 4.2 搜索服务实现

`scm-product/service/src/main/java/scm/product/search/service/ProductSearchService.java`:

```java
package scm.product.search.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import scm.product.search.document.ProductDocument;
import scm.product.search.dto.ProductSearchRequest;
import scm.product.search.dto.ProductSearchResponse;
import scm.product.search.repository.ProductSearchRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品搜索服务
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Service
public class ProductSearchService {

    @Autowired
    private ProductSearchRepository productSearchRepository;

    private static final Integer STATUS_ON_SALE = 1;  // 上架状态

    /**
     * 综合搜索（支持多条件组合）
     */
    public Page<ProductSearchResponse> search(ProductSearchRequest request) {
        log.info("[商品搜索] 开始搜索: keyword={}, categoryId={}, brandId={}, priceRange=[{},{}]",
                request.getKeyword(), request.getCategoryId(), request.getBrandId(),
                request.getMinPrice(), request.getMaxPrice());

        // 构建分页和排序
        PageRequest pageRequest = buildPageRequest(request);

        // 执行搜索
        Page<ProductDocument> page;
        if (hasAdvancedFilters(request)) {
            // 高级搜索（多条件组合）
            page = productSearchRepository.advancedSearch(
                    request.getKeyword(),
                    request.getCategoryId(),
                    request.getBrandId(),
                    request.getMinPrice(),
                    request.getMaxPrice(),
                    STATUS_ON_SALE,
                    pageRequest
            );
        } else if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
            // 全文搜索
            page = productSearchRepository.fullTextSearch(request.getKeyword(), STATUS_ON_SALE, pageRequest);
        } else {
            // 默认查询（按销量排序）
            page = productSearchRepository.findByStatusOrderByTotalSalesDesc(STATUS_ON_SALE, pageRequest);
        }

        log.info("[商品搜索] 搜索完成: 总数={}, 页码={}, 耗时={}ms",
                page.getTotalElements(), page.getNumber(), "N/A");

        // 转换为 DTO
        return page.map(this::convertToResponse);
    }

    /**
     * 热门商品（按销量排序）
     */
    public Page<ProductSearchResponse> getHotProducts(Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "totalSales"));
        Page<ProductDocument> resultPage = productSearchRepository.findByStatusOrderByTotalSalesDesc(STATUS_ON_SALE, pageRequest);
        return resultPage.map(this::convertToResponse);
    }

    /**
     * 最新商品（按发布时间排序）
     */
    public Page<ProductSearchResponse> getLatestProducts(Integer page, Integer size) {
        PageRequest pageRequest = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        Page<ProductDocument> resultPage = productSearchRepository.findByStatusOrderByPublishedAtDesc(STATUS_ON_SALE, pageRequest);
        return resultPage.map(this::convertToResponse);
    }

    /**
     * 构建分页请求
     */
    private PageRequest buildPageRequest(ProductSearchRequest request) {
        int page = Math.max(request.getPage() - 1, 0);
        int size = Math.min(request.getSize(), 100);  // 最大 100 条/页

        Sort sort = buildSort(request.getSortBy(), request.getSortOrder());
        return PageRequest.of(page, size, sort);
    }

    /**
     * 构建排序
     */
    private Sort buildSort(String sortBy, String sortOrder) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy != null ? sortBy : "sales") {
            case "price" -> Sort.by(direction, "minPrice");
            case "time" -> Sort.by(direction, "publishedAt");
            case "sales" -> Sort.by(direction, "totalSales");
            default -> Sort.by(Sort.Direction.DESC, "totalSales");
        };
    }

    /**
     * 判断是否有高级过滤条件
     */
    private boolean hasAdvancedFilters(ProductSearchRequest request) {
        return request.getCategoryId() != null ||
               request.getBrandId() != null ||
               request.getMinPrice() != null ||
               request.getMaxPrice() != null;
    }

    /**
     * 转换为响应 DTO
     */
    private ProductSearchResponse convertToResponse(ProductDocument document) {
        ProductSearchResponse response = new ProductSearchResponse();
        BeanUtils.copyProperties(document, response);
        return response;
    }
}
```

### 4.3 搜索 Controller

`scm-product/service/src/main/java/scm/product/search/controller/ProductSearchController.java`:

```java
package scm.product.search.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import scm.product.search.dto.ProductSearchRequest;
import scm.product.search.dto.ProductSearchResponse;
import scm.product.search.service.ProductSearchService;

/**
 * 商品搜索 Controller
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/products/search")
@Tag(name = "商品搜索", description = "商品搜索 API")
public class ProductSearchController {

    @Autowired
    private ProductSearchService productSearchService;

    @PostMapping
    @Operation(summary = "综合搜索", description = "支持关键词、分类、品牌、价格区间等多条件组合搜索")
    public ApiResponse<Page<ProductSearchResponse>> search(@RequestBody ProductSearchRequest request) {
        Page<ProductSearchResponse> result = productSearchService.search(request);
        return ApiResponse.success(result);
    }

    @GetMapping("/hot")
    @Operation(summary = "热门商品", description = "按销量排序的热门商品列表")
    public ApiResponse<Page<ProductSearchResponse>> getHotProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Page<ProductSearchResponse> result = productSearchService.getHotProducts(page, size);
        return ApiResponse.success(result);
    }

    @GetMapping("/latest")
    @Operation(summary = "最新商品", description = "按发布时间排序的最新商品列表")
    public ApiResponse<Page<ProductSearchResponse>> getLatestProducts(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        Page<ProductSearchResponse> result = productSearchService.getLatestProducts(page, size);
        return ApiResponse.success(result);
    }
}
```

---

## 5. Canal 数据同步

### 5.1 Canal 介绍

Canal 是阿里开源的 MySQL binlog 增量订阅&消费组件。虽然我们使用 PostgreSQL，但可以通过 Debezium（支持 PostgreSQL WAL）实现类似功能。

### 5.2 Debezium + Kafka Connect 方案

**架构图**:

```
PostgreSQL (WAL) → Debezium Connector → Kafka → Kafka Consumer → Elasticsearch
```

### 5.3 Debezium 配置

`docker-compose-canal.yml`:

```yaml
version: '3.8'

services:
  # Zookeeper (Kafka 依赖)
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  # Kafka
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092

  # Kafka Connect (包含 Debezium)
  kafka-connect:
    image: debezium/connect:2.5
    depends_on:
      - kafka
      - postgres
    ports:
      - "8083:8083"
    environment:
      BOOTSTRAP_SERVERS: kafka:9092
      GROUP_ID: 1
      CONFIG_STORAGE_TOPIC: connect_configs
      OFFSET_STORAGE_TOPIC: connect_offsets
      STATUS_STORAGE_TOPIC: connect_status
```

### 5.4 创建 Debezium PostgreSQL Connector

```bash
curl -X POST http://localhost:8083/connectors -H "Content-Type: application/json" -d '{
  "name": "scm-product-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "admin",
    "database.password": "password",
    "database.dbname": "db_product",
    "database.server.name": "scm_product_server",
    "table.include.list": "public.prod_spu,public.prod_sku",
    "plugin.name": "pgoutput",
    "publication.name": "scm_product_publication",
    "slot.name": "scm_product_slot"
  }
}'
```

### 5.5 Kafka Consumer (同步到 ES)

`scm-product/service/src/main/java/scm/product/sync/ProductSyncConsumer.java`:

```java
package scm.product.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import scm.product.search.document.ProductDocument;
import scm.product.search.repository.ProductSearchRepository;

/**
 * 商品数据同步消费者
 *
 * <p>监听 Kafka 中的 PostgreSQL 变更事件，实时同步到 Elasticsearch
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Slf4j
@Component
public class ProductSyncConsumer {

    @Autowired
    private ProductSearchRepository productSearchRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "scm_product_server.public.prod_spu", groupId = "product-sync-group")
    public void consumeProductChange(String message) {
        try {
            log.info("[数据同步] 收到商品变更事件: {}", message);

            JsonNode event = objectMapper.readTree(message);
            String operation = event.get("op").asText();

            switch (operation) {
                case "c", "u" -> handleCreateOrUpdate(event);  // Create or Update
                case "d" -> handleDelete(event);               // Delete
                default -> log.warn("[数据同步] 未知操作类型: {}", operation);
            }
        } catch (Exception e) {
            log.error("[数据同步] 处理变更事件失败: {}", e.getMessage(), e);
        }
    }

    private void handleCreateOrUpdate(JsonNode event) throws Exception {
        JsonNode after = event.get("after");

        ProductDocument document = new ProductDocument();
        document.setId(after.get("id").asText());
        document.setSpuCode(after.get("spu_code").asText());
        document.setSpuName(after.get("spu_name").asText());
        document.setCategoryId(after.get("category_id").asText());
        document.setBrandId(after.get("brand_id").asText());
        // ... 设置其他字段

        productSearchRepository.save(document);
        log.info("[数据同步] 同步商品成功: id={}", document.getId());
    }

    private void handleDelete(JsonNode event) {
        JsonNode before = event.get("before");
        String id = before.get("id").asText();

        productSearchRepository.deleteById(id);
        log.info("[数据同步] 删除商品成功: id={}", id);
    }
}
```

---

## 6. 性能优化

### 6.1 索引优化

**分片策略**:
- **分片数**: 5 个（适合中等数据量，0-1000 万文档）
- **副本数**: 1 个（保证高可用）

**索引别名**:

```bash
# 创建索引别名，支持零停机索引重建
POST /_aliases
{
  "actions": [
    {
      "add": {
        "index": "scm_product_v1",
        "alias": "scm_product"
      }
    }
  ]
}
```

### 6.2 查询优化

**1. 使用 Query Cache**:

Elasticsearch 会自动缓存 filter 查询（如 term, range）。

**2. 避免深分页**:

```java
// 使用 search_after 代替 from+size
// 适合滚动加载场景
```

**3. 只返回必要字段**:

```java
@Query(value = "...", fields = {"id", "spuName", "mainImage", "minPrice"})
```

### 6.3 JVM 优化

`elasticsearch.yml`:

```yaml
# 堆内存设置为物理内存的 50%，不超过 32GB
-Xms4g
-Xmx4g

# 禁用交换
bootstrap.memory_lock: true
```

### 6.4 缓存策略

**应用层缓存**:

```java
@Service
public class ProductSearchService {

    @Cacheable(value = "hotProducts", key = "#page + '_' + #size", ttl = 300)
    public Page<ProductSearchResponse> getHotProducts(Integer page, Integer size) {
        // ...
    }
}
```

**Redis 缓存**:
- 热门搜索词缓存：TTL 5 分钟
- 热门商品列表缓存：TTL 10 分钟

---

## 7. 测试验收

### 7.1 功能测试

#### 测试场景 1: 全文搜索

**请求**:

```bash
curl -X POST http://localhost:8201/api/v1/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "keyword": "iPhone 15 Pro",
    "page": 1,
    "size": 20,
    "sortBy": "sales",
    "sortOrder": "desc"
  }'
```

**预期结果**:
- 返回包含 "iPhone", "15", "Pro" 的商品
- 按销量降序排列
- 响应时间 < 100ms

#### 测试场景 2: 分类过滤

**请求**:

```bash
curl -X POST http://localhost:8201/api/v1/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "categoryId": "cat_001",
    "page": 1,
    "size": 20
  }'
```

**预期结果**:
- 仅返回 category_id = "cat_001" 的商品
- 商品数量正确

#### 测试场景 3: 价格区间

**请求**:

```bash
curl -X POST http://localhost:8201/api/v1/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "minPrice": 5000,
    "maxPrice": 10000,
    "page": 1,
    "size": 20
  }'
```

**预期结果**:
- 返回价格在 5000-10000 之间的商品
- 价格范围正确

### 7.2 性能测试

#### 测试工具: JMeter

**测试场景**:

| 场景 | 并发数 | 持续时间 | 预期 TPS | 预期 P95 响应时间 |
|-----|-------|---------|---------|-----------------|
| 全文搜索 | 100 | 5 分钟 | ≥ 500 | ≤ 100ms |
| 分类过滤 | 100 | 5 分钟 | ≥ 600 | ≤ 80ms |
| 混合查询 | 100 | 5 分钟 | ≥ 400 | ≤ 150ms |

### 7.3 数据同步测试

#### 测试步骤:

1. 在 PostgreSQL 中插入商品：

```sql
INSERT INTO prod_spu (id, spu_code, spu_name, category_id, brand_id, status)
VALUES ('test_001', 'SPU001', 'Test Product', 'cat_001', 'brand_001', 1);
```

2. 验证 Debezium 捕获到变更（查看 Kafka Connect 日志）

3. 验证 Elasticsearch 中数据已同步：

```bash
curl -X GET "localhost:9200/scm_product/_doc/test_001"
```

**预期结果**:
- 数据同步延迟 < 1s
- Elasticsearch 文档字段完整

### 7.4 验收清单

| 检查项 | 验收标准 | 状态 |
|-------|---------|------|
| Elasticsearch 集成 | 服务正常启动，索引自动创建 | ✅ |
| IK 分词器 | 分词效果正确 | ✅ |
| 全文搜索 | 搜索结果准确，响应时间 < 100ms | ✅ |
| 分类/品牌过滤 | 过滤结果正确 | ✅ |
| 价格区间查询 | 价格范围准确 | ✅ |
| 多字段排序 | 排序逻辑正确 | ✅ |
| Debezium 同步 | 数据同步延迟 < 1s | ⏳ |
| 性能测试 | TPS ≥ 500, P95 ≤ 100ms | ⏳ |

---

## 8. 附录

### 8.1 Elasticsearch 常用命令

```bash
# 查看索引
GET /_cat/indices?v

# 查看索引映射
GET /scm_product/_mapping

# 查看索引设置
GET /scm_product/_settings

# 删除索引
DELETE /scm_product

# 查询所有文档
GET /scm_product/_search
{
  "query": {
    "match_all": {}
  }
}

# 聚合查询（按分类统计）
GET /scm_product/_search
{
  "size": 0,
  "aggs": {
    "by_category": {
      "terms": {
        "field": "categoryId",
        "size": 10
      }
    }
  }
}
```

### 8.2 参考资料

- [Elasticsearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/8.11/index.html)
- [Spring Data Elasticsearch](https://docs.spring.io/spring-data/elasticsearch/docs/5.2.0/reference/html/)
- [IK 分词器](https://github.com/medcl/elasticsearch-analysis-ik)
- [Debezium PostgreSQL Connector](https://debezium.io/documentation/reference/2.5/connectors/postgresql.html)

---

**文档完成日期**: 2025-12-26
**下一步**: Phase 3 - 库存服务 + Redis 分布式锁

---

**文档结束**