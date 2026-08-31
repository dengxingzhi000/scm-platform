# Analytics Platform — Resume Guide (断点续接)

> 创建时间: 2026-08-31 18:05 | 分支: `master` | 已完成 4/12 Tasks

## 一句话恢复指令
下次打开本项目，对 AI 说：
```
继续执行 docs/superpowers/plans/2026-08-31-analytics-platform.md 的 Task 5，使用 subagent-driven-development
```

## 当前状态
- **Git:** `master` 领先 `origin/master` 4 commits (aadee78c 为最新)
- **已落实:**
  - `scm-analytics/pom.xml:1`, `scm-analytics/api/pom.xml:1`, `scm-analytics/service/pom.xml:1` — 模块已注册到 `com.scm.parent/pom.xml:55`
  - `docker-compose.yml:455` — clickhouse + keeper + clickhouse_data volume
  - `deploy/clickhouse/users.xml:1`, `deploy/clickhouse/config.xml:1`, `deploy/clickhouse/init.sql:1`
  - `scm-analytics/service/src/main/resources/application.yml:1` — port 8308, db_analytics, kafka, clickhouse
  - `scm-analytics/service/src/main/resources/db/migration/V1__init_analytics_meta.sql:1` + `scripts/db/microservices/040_analytics_meta.sql:1` — 4张元数据表
  - `scm-analytics/service/src/main/resources/clickhouse/V1__ods_dwd_dws_ads.sql:1` — 7张数仓表
  - Tests: `MetaSchemaTest.java:1` (3 tests), `ClickHouseSchemaTest.java:1` (4 tests) — 均 PASS

## 未完成 (按优先级)
1. **Task 5 Outbox** — 事务发件箱 (db_order/db_inventory 同事务写 outbox_event + 5s Relay Job → Kafka `scm.*`)
2. **Task 6 ODS Consumer** — `analytics-ods` 消费组 → ClickHouse, Redis 24h 幂等
3. **Task 7 Semantic CRUD** — Metric/Dimension/Dataset 的 PG 增删改查
4. **Task 8 Query Engine** — 白名单 SqlGenerator + 权限注入 (tenant_id/organization_id)
5. **Task 9 Query API** — REST + Dubbo 供 BI/Python/AI
6. **Task 10 DWD/DWS/ADS Jobs** — XXL-Job 物化
7. **Task 11 BI 前端** — scm-web analytics 页面
8. **Task 12 Governance** — 血缘/质量/ retention

## 验证命令 (恢复后先跑)
```bash
# Windows 需用 build.bat
.\build.bat mvn validate -f com.scm.parent/pom.xml -o
.\build.bat mvn test -Dtest=MetaSchemaTest,ClickHouseSchemaTest -f com.scm.parent/pom.xml -pl :scm-analytics-service -am -o

# Docker 可用时
docker compose config | grep clickhouse
curl http://localhost:8123/ping  # 启动后应返回 Ok.
```

## 风险与注意
- `scm-analytics/service` 的 ClickHouse 依赖是 `client-v2:0.6.5`，与现有 PG 驱动不冲突，但需验证 `jakarta.*` 兼容
- Outbox 表需在每个业务库各建一份 (040 是 analytics 库，041 是业务库 outbox)，CI 会校验 tenant_id
- Query Engine 必须禁止透传 SQL，所有前端请求走 `DatasetQueryRequest` DTO
- 分区键: PG `UNIQUE(order_no, create_time)` 模式需同步到 ClickHouse `ORDER BY (tenant_id, ...)`
- 若切分支: `git checkout -b feat/analytics-platform && git push -u origin feat/analytics-platform`

## 关联文档
- 主计划: `docs/superpowers/plans/2026-08-31-analytics-platform.md`
- 架构总览: `docs/ARCHITECTURE_AUDIT_2026.md`
- 父POM: `com.scm.parent/pom.xml:14`
- 基础设施: `docker-compose.yml:1`, `scm-common/integration/src/main/java/com/scmcloud/common/integration/config/KafkaIntegrationAutoConfiguration.java:34`, `scm-common/core/src/main/java/com/scmcloud/common/tenant/TenantContextHolder.java:15`
