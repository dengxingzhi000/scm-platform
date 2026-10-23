# Analytics Platform — Resume Guide (断点续接)

> 更新时间: 2026-10-23 | 分支: `docs/readme-restyle` | 已完成 12/12 Tasks

## 当前状态

- **Task 1–5:** 已完成（见主计划 checkpoint）。
- **Task 6 ODS Consumer:** `OdsIngestConsumer` + `ClickHouseOdsWriter` + 24h Redis 幂等。
- **Task 7 Semantic CRUD:** Metric/Dimension/Dataset/DatasetField 的实体、Mapper、Service、REST。
- **Task 8 Query Engine:** `SqlGenerator` / `PermissionSqlInjector` / `ClickHouseExecutor` / `QueryEngine`，
  白名单字段、租户强制过滤、组织范围注入、LIMIT ≤ 10000。
- **Task 9 Query API:** `POST /api/v1/analytics/query` + `AnalyticsDubboService`。
- **Task 10 Materialization:** `MaterializationService` + `DwdJob` / `DwsJob` / `AdsJob`（按 tenant 分片、先删后插幂等）。
- **Task 11 BI:** `scm-web/src/features/analytics` + `/{locale}/analytics` 页面（数据集/维度/指标选择、图表、表格、CSV 导出）。
- **Task 12 Governance:** `analytics_lineage_edge` / `analytics_quality_rule` / `analytics_quality_result`（Flyway V2），
  `LineageService`、`DataQualityService`、`DataQualityEvaluator`、`DataQualityJob`；保留策略扩展到 ClickHouse。

## 验证命令

```bash
# Windows 需用 build.bat 或显式 JAVA_HOME / Maven 路径
mvn test -pl :scm-analytics-service -am -f com.scm.parent/pom.xml
cd scm-web && npx tsc --noEmit
```

## 部署注意

- `scm-analytics` 已补 Dockerfile 与 `spring-boot-maven-plugin` 的 `<skip>false</skip>`。
- `docker compose --profile ecommerce up -d` 可启动电商域 + analytics 容器（需先 `mvn package`）。
- XXL-Job 执行器名：`scm-analytics-executor`；JobHandler：`analyticsDwdJob`、`analyticsDwsJob`、`analyticsAdsJob`、`analyticsDataQualityJob`。
- ClickHouse 数据库 `analytics`，PostgreSQL 数据库 `db_analytics`（init 脚本已创建）。

## 关联文档

- 主计划: `docs/superpowers/plans/2026-08-31-analytics-platform.md`
- 架构总览: `docs/ARCHITECTURE_AUDIT_2026.md`
- 父POM: `com.scm.parent/pom.xml`
