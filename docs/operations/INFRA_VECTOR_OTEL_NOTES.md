# pgvector 与 OTel Collector 部署说明

适用主机：`192.168.0.121`（root）。部署日期：2026-11-03。
本轮只新增两个独立容器，**未 stop/restart 任何现有容器**；`scm-platform` 的 31 个容器与 `scm-web` 保持运行。

## 1. 当前状态

| 容器 | 镜像 | 端口 | restart | 内存（docker stats） |
|---|---|---|---|---|
| `pgvector-agent` | `pgvector/pgvector:pg18`（Postgres 18.6 + pgvector 0.8.7） | **5434 → 5432** | `no` | 约 42–79MiB |
| `otel-collector` | `otel/opentelemetry-collector:latest`（0.164.0，core 版） | 4317/4318/8889 | `no` | 约 51–84MiB |

> **端口冲突说明**：按要求希望映射 5433，但宿主机 `5433` 已被另一个项目的
> `petcare-postgres`（`0.0.0.0:5433->5432`）占用。本轮未停任何现有容器，pgvector 实际使用
> **5434**。若必须使用 5433，需要先与 petcare 项目协调迁移/停止其端口（需另行确认），
> 不能在不影响现有容器的前提下完成。

## 2. pgvector 实例

- 容器名：`pgvector-agent`
- 卷：`pgvector-data` → `/var/lib/postgresql`（PGDATA 为 `/var/lib/postgresql/18/docker`）
- 数据库：`db_agent_vector`（由 `POSTGRES_DB` 初始化创建）
- 账号：沿用 `/root/scm-deploy/.env` 的 `POSTGRES_USER` / `POSTGRES_PASSWORD`，**未写入任何文件**
- 启动命令：

```bash
set -a; . /root/scm-deploy/.env; set +a
docker run -d --name pgvector-agent --restart no \
  -p 5434:5432 \
  -e POSTGRES_USER="$POSTGRES_USER" \
  -e POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  -e POSTGRES_DB=db_agent_vector \
  -v pgvector-data:/var/lib/postgresql \
  pgvector/pgvector:pg18
```

### 扩展与表验证

```bash
docker exec pgvector-agent psql -U "$POSTGRES_USER" -d db_agent_vector \
  -c "CREATE EXTENSION IF NOT EXISTS vector;"
docker exec pgvector-agent psql -U "$POSTGRES_USER" -d db_agent_vector \
  -Atc "SELECT extversion FROM pg_extension WHERE extname='vector';"
# 输出：0.8.7
```

已执行 `agent_schema.sql`（来源 `enterprise-agent/scripts/db/agent_schema.sql`），创建表：

`agent_approval`、`agent_paused_task`、`memory_user`、`memory_org`、`memory_conv`、
`memory_task`、`memory_episodic`（各表索引均已创建）。

外部连通性已用 `postgres:18` 容器实测：
`psql -h 192.168.0.121 -p 5434 -U <user> -d db_agent_vector` → `SELECT extversion` 返回 `0.8.7`。

### DSN 模板（不含真实密码）

```text
postgresql://<user>:<password>@192.168.0.121:5434/db_agent_vector
```

> 实际端口是 **5434**，不是 5433；原因见上方端口冲突说明。

## 3. OTel Collector

- 容器名：`otel-collector`
- 镜像：`otel/opentelemetry-collector:latest`（core 发行版，0.164.0；体积 184MB，满足
  `otlp` receiver + `debug` + `prometheus` exporter 的最小需求，未使用更大的 contrib 镜像）
- 配置文件：`/root/otel/collector.yaml`，挂载为 `/etc/otelcol/config.yaml`
- 启动命令：

```bash
docker run -d --name otel-collector --restart no \
  -p 4317:4317 -p 4318:4318 -p 8889:8889 \
  -v /root/otel/collector.yaml:/etc/otelcol/config.yaml:ro \
  otel/opentelemetry-collector:latest
```

配置内容：OTLP gRPC(4317)/HTTP(4318) receiver；`debug` exporter；`prometheus` exporter(8889)；
traces/metrics/logs 三条 pipeline；**没有配置任何需要外网的 backend**。

### 验证结果

| 检查 | 结果 |
|---|---|
| `docker logs otel-collector` | `Everything is ready...`，无报错 |
| 4317 TCP | **open**（gRPC server 已启动） |
| `HEAD /v1/traces`（4318） | **405**（端口在监听） |
| `GET :8889/metrics` | **200**；发送合成 OTLP metric 后可见 `scm_test_metric` |
| 本机 Windows → 4317/4318/8889 | 全部可达 |

说明：`8889/metrics` 在没有任何指标流入时返回 200 + 空 body；发送一条测试 OTLP metric 后
已验证 prometheus exporter 正常输出。若需要长期看到 collector 自身内部指标，可另行开启
`service.telemetry.metrics`，本轮未改。

### OTLP endpoint

```text
http://192.168.0.121:4317        # gRPC
http://192.168.0.121:4318        # HTTP
http://192.168.0.121:8889/metrics # Prometheus 拉取
```

## 4. 资源与影响

| 项目 | 新增前 | 新增后 |
|---|---|---|
| 运行容器总数 | 35 | **37**（+pgvector-agent +otel-collector） |
| `scm-platform` 容器数 | 31 | **31（未变）** |
| `scm-web` | Up | **Up** |
| 可用内存 | 约 580MB | 约 **654MB**（缓存波动） |
| 新容器内存 | — | pgvector 约 42–79MiB；otel 约 51–84MiB |
| 磁盘 `/` | 可用约 36G | 可用约 36G（新镜像共约 629MB，仍够用） |

后端健康抽查（新增后）：`8761 / 8081 / 8106 / 8203 / 8201` 全部 **200**；`scm-web` 登录页 **200**。

## 5. 停止 / 重启 / 回滚

```bash
# 只操作本轮新增的两个容器，不影响后端与 scm-web
docker stop pgvector-agent
docker start pgvector-agent
docker stop otel-collector
docker start otel-collector

# 回滚（谨慎；卷 pgvector-data 按需保留或另行确认后删除）
docker rm -f pgvector-agent otel-collector
```

- 两者 `restart=no`，主机重启后需人工 `docker start`。
- `pgvector-data` 是独立命名卷，未执行 `volume prune`/删除；删除该卷会丢失 `db_agent_vector` 数据。
- OTel 为内存队列，无持久化 backend；重启会丢失未导出的 telemetry。

## 6. 风险提示

1. **5433 被 petcare-postgres 占用**，当前 pgvector 在 5434；若代码侧写死 5433，需要先协调
   petcare 端口或改用 5434。本轮未停任何现有容器。
2. 可用内存约 654MB，仍偏紧；OTel `debug` exporter 在高流量下会增加日志/CPU，pgvector
   建 HNSW/IVFFlat 索引时也会上涨。建议联调时观察 `docker stats` 与 `free -m`。
3. pgvector 使用 Postgres 18.6（pg18 镜像），与 pg17 存在版本差异；扩展版本 0.8.7。
4. 两个新容器均未配置开机自启；主机重启后需手动启动。
5. 未执行 `down`、`prune`、删卷；未修改现有 Compose、镜像、卷或任何业务容器。
