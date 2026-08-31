# SCM Data Platform (Analytics + Semantic Layer + Query Engine) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 SCM 微服务改造为大厂级数据平台：业务PG → Kafka/Outbox → ClickHouse ODS/DWD/DWS/ADS，并以 Metric/Dimension/Dataset 语义层 + Query Engine 统一对外提供 BI/Python/AI 消费入口。

**Architecture:** 新增 `scm-analytics` 域（`api` + `service`），复用 `scm-common/integration` 的 Kafka 与 `scm-common/core` 的 `TenantContextHolder`；业务服务采用 Transactional Outbox 保证 CDC 最终一致性，`scm-analytics/service` 负责 ODS 入库、DWD/DWS聚合、语义解析与 ClickHouse SQL 生成；前端/ Python/ Agent 只调用 `DatasetQuery` 声明式接口，不透传SQL。

**Tech Stack:** Java 21 / Spring Boot 4.1 / Spring Kafka `JacksonJsonSerializer` / ClickHouse 24.x + `clickhouse-java` / PostgreSQL + Flyway / Nacos / Redis 7.2 (幂等 24h + 分布式锁) / MyBatis-Plus / Dubbo 3.3.6

---

## File Structure

```
scm-analytics/                          # NEW — parent pom module
  pom.xml
  api/
    pom.xml
    src/main/java/com/scmcloud/analytics/api/AnalyticsDubboService.java
    src/main/java/com/scmcloud/analytics/api/dto/
      MetricDTO.java / DimensionDTO.java / DatasetDTO.java / DatasetQueryRequest.java / DatasetQueryResult.java
  service/
    pom.xml
    src/main/java/com/scmcloud/analytics/ScmAnalyticsApplication.java
    src/main/java/com/scmcloud/analytics/config/ClickHouseConfig.java, AnalyticsKafkaConfig.java
    src/main/java/com/scmcloud/analytics/domain/entity/MetricDefinition.java, DimensionDefinition.java, DatasetDefinition.java
    src/main/java/com/scmcloud/analytics/mapper/*, service/*, query/QueryEngine.java, SqlGenerator.java
    src/main/java/com/scmcloud/analytics/ingest/OutboxEvent.java, OutboxRelayJob.java, OdsIngestConsumer.java, DwdJob.java, DwsJob.java
    src/main/resources/application.yml (port 8308)
    src/main/resources/db/migration/V1__init_analytics_meta.sql
    src/main/resources/clickhouse/V1__ods_dwd_dws_ads.sql
```

## Progress Checkpoint (2026-08-31 18:00)

| Task | Status | Commit |
|------|--------|--------|
| Task 1 Scaffold | ✅ DONE | 5bc84df3 / b49802bf |
| Task 2 ClickHouse infra | ✅ DONE | a77d9f20 |
| Task 3 PG Meta Schema | ✅ DONE | d9efe429 |
| Task 4 CH DWH Schema | ✅ DONE | aadee78c |
| Task 5 Outbox | ⏳ NEXT | — |
| Task 6 ODS Consumer | ⬜ | — |
| Task 7 Semantic CRUD | ⬜ | — |
| Task 8 Query Engine | ⬜ | — |
| Task 9 Query API | ⬜ | — |
| Task 10 DWD/DWS/ADS Jobs | ⬜ | — |
| Task 11 BI Frontend | ⬜ | — |
| Task 12 Governance | ⬜ | — |

**Branch:** `master` (4 commits ahead of origin/master). To resume: `git checkout master` → continue with Task 5 via subagent-driven-development.

---

### Task 1: Scaffold scm-analytics Modules + Parent POM Registration
*(已完成)* Files: `com.scm.parent/pom.xml:14-58`, `scm-analytics/pom.xml`, `scm-analytics/api/pom.xml`, `scm-analytics/service/pom.xml`, `Placeholder.java`, `ScmAnalyticsApplication.java`
Verify: `mvn validate -f com.scm.parent/pom.xml -o` BUILD SUCCESS

### Task 2: ClickHouse + Kafka Infra
*(已完成)* Files: `docker-compose.yml`, `deploy/clickhouse/users.xml`, `scm-analytics/service/src/main/resources/application.yml`
Verify: yaml parses, mvn validate pass

### Task 3: Analytics Metadata Schema (PG) — Metric/Dimension/Dataset + Flyway
*(已完成)* Files: `scm-analytics/service/src/main/resources/db/migration/V1__init_analytics_meta.sql` (analytics_metric, analytics_dimension, analytics_dataset, analytics_dataset_field, all tenant_id UK), `scripts/db/microservices/040_analytics_meta.sql`
Verify: `MetaSchemaTest` 3 tests PASS

### Task 4: ClickHouse DWH Schema ODS/DWD/DWS/ADS
*(已完成)* Files: `scm-analytics/service/src/main/resources/clickhouse/V1__ods_dwd_dws_ads.sql` (7 tables: ods_order, ods_inventory, dwd_sales_detail, dws_sales_day, dws_inventory_day, ads_sales_dashboard, ads_inventory_health), `deploy/clickhouse/init.sql`, `deploy/clickhouse/config.xml`, `docker-compose.yml` mount
Verify: `ClickHouseSchemaTest` 4 tests PASS (combined 7)

### Task 5: Transactional Outbox Pattern (Order + Inventory as template)
**Files:**
- Create: `scm-order/service/src/main/java/com/scmcloud/order/domain/entity/OutboxEvent.java`
- Create: `scm-order/service/src/main/java/com/scmcloud/order/mapper/OutboxMapper.java`
- Modify: `scm-order/service/src/main/java/com/scmcloud/order/service/impl/OrderServiceImpl.java`
- Create: `scm-analytics/service/src/main/java/com/scmcloud/analytics/ingest/OutboxRelayJob.java`
- Create: `scripts/db/microservices/041_outbox.sql`
Steps:
- [ ] Step 1: Write failing test `OutboxPublishTest.java` - createOrder inserts Outbox atomically
- [ ] Step 2: Create outbox_event table per DB (db_order, db_inventory) with published index
- [ ] Step 3: Modify business service same TX inserts outbox via TenantContextHolder.getRequiredTenantId()
- [ ] Step 4: Create OutboxRelayJob @Scheduled(5s) + @DistributedLock + kafkaTemplate.send("scm."+aggregateType)
- [ ] Step 5: Verify mvn test OutboxPublishTest PASS then commit

### Task 6: ODS Ingest Consumer (Kafka → ClickHouse)
**Files:** `OdsIngestConsumer.java`, `AnalyticsKafkaConfig.java`, `ClickHouseOdsWriter.java`
- [ ] EmbeddedKafka test consumeOrderCreated inserts ClickHouse ods
- [ ] @KafkaListener topics scm.order/scm.inventory/... group analytics-ods + IdempotencyChecker 24h
- [ ] Batch insert via client-v2 1000 rows/5s flush
- [ ] mvn test OdsIngestConsumerTest PASS

### Task 7: Semantic Layer CRUD — Metric/Dimension/Dataset
**Files:** `api/dto/*`, `domain/entity/*`, `mapper/*`, `MetricService.java`, `AnalyticsMetaController.java`
- [ ] MetricServiceTest createAtomicMetric + derivedMetric
- [ ] Entities @TableName(analytics_metric) with tenant_id, @Master/@Slave
- [ ] REST /api/v1/analytics/metrics + list
- [ ] mvn test MetricServiceTest PASS

### Task 8: Query Engine — SqlGenerator + Permission Injector
**Files:** `SqlGenerator.java`, `PermissionSqlInjector.java`, `QueryEngine.java`, `ClickHouseExecutor.java`, `DatasetQueryRequest.java`
- [ ] SqlGeneratorTest generate simpleQuery with WHERE tenant_id + GROUP BY, reject forbidden field
- [ ] DTO record DatasetQueryRequest(datasetCode, dimensions, metrics, filters, orderBy, limit)
- [ ] SqlGenerator whitelist-only (no raw SQL), cap LIMIT 10000
- [ ] PermissionSqlInjector inject organization_id IN (...)

### Task 9: Analytics Query API (REST + Dubbo for Python/AI)
**Files:** `AnalyticsDubboService.java`, `AnalyticsDubboServiceImpl.java`, `AnalyticsQueryController.java`
- [ ] AnalyticsQueryControllerTest @WebMvcTest query returns rows
- [ ] Dubbo interface query(DatasetQueryRequest, UUID tenantId)
- [ ] REST POST /api/v1/analytics/query delegates to QueryEngine

### Task 10: DWD/DWS/ADS Materialization Jobs (XXL-Job)
**Files:** `DwdJob.java`, `DwsJob.java`, `AdsJob.java`
- [ ] DwsAggregationTest with Testcontainers ClickHouse
- [ ] SQL: INSERT INTO dwd/dws/ads SELECT ... WHERE date=:date AND tenant_id=:tenantId GROUP BY ...
- [ ] @XxlJob handlers sharding by tenant_id

### Task 11: BI Integration — Frontend Dashboard + OpenAPI Gen
**Files:** `scm-web/src/app/(admin)/analytics/page.tsx`, `ChartBuilder.tsx`, `useAnalyticsQuery.ts`
- [ ] npm run generate:api generates src/api/analytics/
- [ ] useAnalyticsQuery hook
- [ ] Dashboard: dataset selector + metric/dimension picker + chart/table + CSV export

### Task 12: Data Governance — Lineage / Quality / Retention
**Files:** `governance/LineageService.java`, `DataQualityRule.java`, `DataQualityJob.java`, `scripts/db/retention/apply-retention.sh`
- [ ] lineage table + API (ods->dwd->dws->ads->gmv)
- [ ] Quality rules sales_amount>=0, tenant_id not null
- [ ] Retention extends existing script (audit 2yr, API 90d, raw 1yr)

---

## Execution Handoff
To resume: invoke `subagent-driven-development` skill, dispatch Task 5 implementer subagent with full Task 5 text above.
