# 电商域（ecom）按需部署说明

适用主机：`192.168.0.121`（root），部署目录 `/root/scm-deploy`，只读配置目录 `/opt/scm-platform`。
本文件记录 2026-11-03 的实际部署结果与按需启动方式；修改部署前先看第 4、5 节的风险说明。

## 1. 当前状态快照（2026-11-03）

| 项目 | 值 |
|---|---|
| 项目容器总数 | 31（26 个核心/基础设施 + 5 个 ecom） |
| 运行中的 ecom | `scm-member`(8302)、`scm-promotion`(8303)、`scm-payment`(8304)、`scm-order-center`(8305)、`scm-mall`(8301) |
| ecom health | 8301–8305 全部 200 |
| 未启动 ecom | `scm-search`(8307)、`scm-fulfillment`(8306)、`scm-analytics`(8308) |
| 已构建 ecom 镜像 | 仅 member / promotion / payment / order-center / mall（`scm-platform-scm-*:latest`） |
| 内存 | 15G 总，约 14G 已用；可用约 590MB；Swap 已用约 1.7G |
| 磁盘 | `/` 77G，可用约 37G |
| restart 策略 | ecom 容器全部 `no`（不会开机自启） |

ecom 每个 JVM 实际 RSS 约 420–477MiB，启动参数统一为
`JAVA_TOOL_OPTIONS=-Xmx192m -XX:MaxMetaspaceSize=160m`。

## 2. 目录与 compose 调用约定

仓库本地已按 v2 结构分域：平台域 `platform/<name>/`、供应链域 `scm/<name>/`、
电商域 `ecom/<name>/`、物流域 `lms/<name>/`。可部署模块的产物在
`<domain>/<name>/service/target/<artifactId>-<version>.jar`，例如
`ecom/member/service/target/ecom-member-service-2.2.0.jar`。

远程 compose 的 ecom `build.context` 是 **`./scm-member`、`./scm-promotion`** 等，
不是仓库本地文件里的 `./ecom/member`。因此 jar 与 Dockerfile 要放到：

```
/opt/scm-platform/scm-<name>/Dockerfile
/opt/scm-platform/scm-<name>/service/target/<jar>
```

统一命令（所有 ecom 服务都用这一套）：

```bash
cd /root/scm-deploy
docker compose --project-directory /opt/scm-platform --env-file .env \
  -f docker-compose.yml -f docker-compose.remote.yml --profile ecommerce \
  <build|up -d --no-deps> <compose service key>
```

注意：`--project-directory /opt/scm-platform` 让 `./scripts`、`./deploy` 等相对路径解析到
`/opt/scm-platform`；`--env-file .env` 提供数据库、Redis、JWT 等密钥；ecom 服务需要
`--profile ecommerce`。

## 3. 覆盖配置（`/root/scm-deploy/docker-compose.remote.yml`）

文件顶部定义了 `x-ecom-env: &ecom-env`，各 ecom 服务用 `environment: { <<: *ecom-env, ... }`
合并。核心项：

```yaml
x-ecom-env: &ecom-env
  DB_HOST: postgres
  DB_PORT: "5432"
  DB_USERNAME: ${POSTGRES_USER:-admin}
  DB_PASSWORD: ${POSTGRES_PASSWORD:-admin123}
  REDIS_HOST: redis
  REDIS_PORT: "6379"
  REDIS_PASSWORD: ${REDIS_PASSWORD:-admin123}
  SPRING_DATA_REDIS_HOST: redis
  SPRING_DATA_REDIS_PORT: "6379"
  SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD:-admin123}
  NACOS_SERVER: nacos:8848
  SPRING_CLOUD_NACOS_CONFIG_ENABLED: "false"
  DUBBO_REGISTRY_ADDRESS: nacos://nacos:8848
  DUBBO_CONSUMER_CHECK: "false"
  DUBBO_APPLICATION_QOS_ENABLE: "false"
  JWT_SECRET: ${JWT_SECRET:?...}          # 值只在 .env，不写入本文件
  JAVA_TOOL_OPTIONS: "-Xmx192m -XX:MaxMetaspaceSize=160m"
  SPRING_DATASOURCE_DYNAMIC_PRIMARY: master
  SPRING_DATASOURCE_DYNAMIC_STRICT: "false"
  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_DRIVER_CLASS_NAME: org.postgresql.Driver
  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_USERNAME: ${POSTGRES_USER:-admin}
  SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_PASSWORD: ${POSTGRES_PASSWORD:-admin123}
```

每个服务的 dynamic-datasource master URL 与库名：

| compose key | 库名 | `SPRING_DATASOURCE_DYNAMIC_DATASOURCE_MASTER_URL` |
|---|---|---|
| scm-member | db_user | `jdbc:postgresql://postgres:5432/db_user?stringtype=unspecified` |
| scm-promotion | db_promotion | `jdbc:postgresql://postgres:5432/db_promotion?stringtype=unspecified` |
| scm-payment | db_finance | `jdbc:postgresql://postgres:5432/db_finance?stringtype=unspecified` |
| scm-order-center | db_order | `jdbc:postgresql://postgres:5432/db_order?stringtype=unspecified` |
| scm-mall | db_order | `jdbc:postgresql://postgres:5432/db_order?stringtype=unspecified` |
| scm-fulfillment | db_warehouse | `jdbc:postgresql://postgres:5432/db_warehouse?stringtype=unspecified` |
| scm-analytics | db_analytics | `jdbc:postgresql://postgres:5432/db_analytics?stringtype=unspecified` |
| scm-search | 无数据库 | 该服务单独设置 `SPRING_DATASOURCE_DYNAMIC_ENABLED: "false"` |

为什么需要这些：ecom 模块的 `application.yml` 用普通 `spring.datasource`，但 classpath 带
`dynamic-datasource` starter；不配置 master 会报 `dynamic-datasource can not find primary datasource`。
搜索服务没有 MyBatis 数据源，直接关闭 dynamic。

同一覆盖文件里还有两条与本主题无关、但不要删除的旧配置：

- `scm-file: SERVER_PORT: 8214`（模块默认 8201 与 product 冲突）
- `scm-gateway: SPRING_AUTOCONFIGURE_EXCLUDE=...`（排除 DataSource/AuditLog/PermissionAccess 三个自动配置，
  gateway 是 WebFlux，没有 ObjectMapper / SysPermissionServiceClient）

备份：`docker-compose.remote.yml.bak-ecom`、`.bak-ecom2`、`.bak-ecom3`、`.bak-ecom4`；
当前生效文件是加入 dynamic master + Dubbo 配置后的版本。

## 4. 数据库迁移与 Flyway 风险（重要）

**事实**：ecom 模块只声明了 `flyway-core`，Spring Boot 4 的 Flyway 自动配置已拆到
`spring-boot-starter-flyway`，因此 **Flyway 没有运行**（日志无 Flyway 记录），业务表不存在，
接口最初全部 500（`relation "mem_member" does not exist` 等）。

本次在**未改源码**的前提下，从各 jar 的 `BOOT-INF/classes/db/migration` 提取 SQL 并手工执行：

| 服务 | 目标库 | 已手工应用 | Flyway 历史表 |
|---|---|---|---|
| member | db_user | V1、V2 | 无记录 |
| promotion | db_promotion | V1、V2 | 无记录 |
| payment | db_finance | V1、V2、V3 | 无记录 |
| order-center | db_order | V1、V2 | 无记录 |
| mall | db_order | V1、V2 | 无记录 |
| fulfillment | db_warehouse | **未应用** | 无 |
| analytics | db_analytics | **未应用** | 无 |
| search | 无 | — | — |

手工应用方式（可复用）：

```bash
# 1) 从 jar 提取 SQL 到临时目录
docker run --rm -v /opt/scm-platform/scm-member/service/target:/w \
  -v /tmp/ecom-sql/member:/out eclipse-temurin:21-jdk-alpine \
  sh -c 'cd /out; for j in /w/*.jar; do jar xf "$j" BOOT-INF/classes/db/migration; done'

# 2) 按 V1 -> V2 -> V3 顺序执行
docker exec -i scm-postgres psql -U admin -d db_user \
  < /tmp/ecom-sql/member/BOOT-INF/classes/db/migration/V1__create_member_tables.sql
docker exec -i scm-postgres psql -U admin -d db_user \
  < /tmp/ecom-sql/member/BOOT-INF/classes/db/migration/V2__add_member_tenant_id.sql
```

**启用 Flyway 前必须处理**：数据库已有表但 `flyway_history_*` 没有记录。直接加
`spring-boot-starter-flyway` 会让 V1 从头执行并因表已存在而失败。正确做法是二选一：

1. 设置 `spring.flyway.baseline-on-migrate=true` 且 `baseline-version` >= 已应用的最高版本，
   让 Flyway 先 baseline；或
2. 用 Flyway CLI 对每个库执行 baseline / 手工补写 history 行（checksum 必须与实际 SQL 一致）。

`spring.flyway.table` 各服务不同：`flyway_history_member`、`flyway_history_promotion`、
`flyway_history_payment`、`flyway_history_order_center`、`flyway_history_mall`、
`flyway_history_fulfillment` 等，baseline 时注意对应表名。

## 5. 按需启动清单

| compose key | 端口 | 依赖 | 当前状态 | 单实例内存余量建议 |
|---|---|---|---|---|
| scm-member | 8302 | Nacos/Postgres(redis) | 运行 200 | — |
| scm-promotion | 8303 | Nacos/Postgres/Redis | 运行 200 | — |
| scm-payment | 8304 | Nacos/Postgres(db_finance)/Redis | 运行 200 | — |
| scm-order-center | 8305 | Nacos/Postgres(db_order)/Redis | 运行 200 | — |
| scm-mall | 8301 | Nacos/Postgres(db_order)/Redis | 运行 200 | — |
| **scm-search** | 8307 | Nacos/Redis/**Elasticsearch** | **未启动，无镜像** | ≥ 700MB 可用 |
| **scm-fulfillment** | 8306 | Nacos/Postgres(db_warehouse)/Redis | **未启动，无镜像** | ≥ 700MB 可用 |
| **scm-analytics** | 8308 | Nacos/Postgres(db_analytics)/Redis/Kafka/**ClickHouse**/XXL-Job | **未启动，无镜像；ClickHouse 未部署** | ≥ 1.5GB 可用（含 ClickHouse） |

当前可用内存约 590MB，而单个 ecom JVM 约 420–477MB、启动峰值更高，因此
**search / fulfillment 现在不具备启动余量**；不要为了它们去停 notify 或改核心服务堆参数。

### search 按需启动（镜像尚未构建）

```bash
# 前置：本机构建 `ecom/search` 后，把 Dockerfile 与 jar 放到
#   /opt/scm-platform/scm-search/Dockerfile
#   /opt/scm-platform/scm-search/service/target/ecom-search-service-2.2.0.jar
cd /root/scm-deploy
docker compose --project-directory /opt/scm-platform --env-file .env \
  -f docker-compose.yml -f docker-compose.remote.yml --profile ecommerce \
  build scm-search
docker compose --project-directory /opt/scm-platform --env-file .env \
  -f docker-compose.yml -f docker-compose.remote.yml --profile ecommerce \
  up -d --no-deps scm-search
```

说明：ES 已 healthy；索引为空时服务可启动，商品搜索返回空态。需要先有索引数据才能搜出商品。

### fulfillment 按需启动（镜像尚未构建，且迁移未跑）

```bash
# 前置：本机构建 `ecom/fulfillment` 后，把 jar + Dockerfile 放到 /opt/scm-platform/scm-fulfillment/...
# 启动前先把 db_warehouse 的 V1/V2 迁移按第 4 节手工应用
cd /root/scm-deploy
docker compose --project-directory /opt/scm-platform --env-file .env \
  -f docker-compose.yml -f docker-compose.remote.yml --profile ecommerce \
  build scm-fulfillment
docker compose --project-directory /opt/scm-platform --env-file .env \
  -f docker-compose.yml -f docker-compose.remote.yml --profile ecommerce \
  up -d --no-deps scm-fulfillment
```

### analytics 按需再做（标记冻结）

缺 ClickHouse（`scm-clickhouse` / `scm-clickhouse-keeper` 未运行，镜像也未拉取），
同时需要 Kafka、XXL-Job、db_analytics 迁移。当前不构建、不启动；等 ClickHouse 方案确认后再做。

## 6. 运维注意

- 所有 scm 容器 `restart=no`，主机重启后需人工 `up -d`；不要 disable `docker.service`。
- 不要执行 `down`、`down --volumes`、`prune`、`docker volume prune`。
- `/opt/scm-platform` 只放 Dockerfile 与 jar，不要改源码。
- 常用检查：
  ```bash
  curl -s http://127.0.0.1:<port>/actuator/health
  docker logs <container> --tail 100
  free -h
  ```
- ecom 接口验证（token 从 auth 8106 获取）：
  `GET /api/v1/members/page`（8302）、`GET /api/v1/orders/page`（8305）、
  `GET /api/v1/payments/page` 与 `/api/v1/payments/refunds/page`（8304）。
  目前表为空，接口返回 200 + 空分页属正常空态。

## 7. 本次验证记录

- ecom 接口最初 500，根因缺表；手工迁移 member/promotion/payment/order-center/mall 后，
  member page、order-center page、payment page、refund page 均返回 **200**（空分页）。
- 5 个 ecom 容器启动后 health 200，restart=no。
- 搜索、履约、分析未启动；搜索/履约无镜像，分析缺 ClickHouse。
