# SCM Platform 运维手册

> **参考标准**: Google SRE Book + 阿里运维规范
> **运维负责人**: DevOps Team
> **目标**: 确保系统 99.9%+ 可用性

---

## 1. 系统架构概览

### 1.1 服务拓扑

```
Internet
   ↓
[Nginx LB] ×2
   ↓
[API Gateway] ×3 (Port 9095)
   ↓
┌──────────┬──────────┬──────────┬──────────┬──────────┬──────────┐
│   Auth   │ Product  │Inventory │  Order   │Warehouse │Logistics │
│  ×2      │  ×3      │  ×3      │  ×3      │  ×2      │  ×2      │
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘
   ↓           ↓           ↓           ↓           ↓           ↓
┌──────────────────────────────────────────────────────────────────┐
│                       中间件层                                     │
├──────────┬──────────┬──────────┬──────────┬──────────┬──────────┤
│  Nacos   │  Redis   │   ES     │  Seata   │ XXL-Job  │PostgreSQL│
│   ×3     │  ×3      │   ×3     │   ×3     │   ×2     │ 1主2从   │
└──────────┴──────────┴──────────┴──────────┴──────────┴──────────┘
```

### 1.2 端口清单

| 服务 | 端口 | 协议 | 用途 |
|-----|------|------|------|
| Nginx | 80, 443 | HTTP/HTTPS | 负载均衡 |
| API Gateway | 9095 | HTTP | 网关 |
| Auth Service | 8106 | HTTPS | 认证服务 |
| Product Service | 8201 | HTTP | 商品服务 |
| Inventory Service | 8202 | HTTP | 库存服务 |
| Order Service | 8203 | HTTP | 订单服务 |
| Nacos | 8848 | HTTP | 注册中心 |
| Redis | 6379 | TCP | 缓存 |
| Elasticsearch | 9200 | HTTP | 搜索引擎 |
| PostgreSQL | 5432 | TCP | 数据库 |
| Seata | 7091, 8091 | TCP | 分布式事务 |
| XXL-Job Admin | 8088 | HTTP | 任务调度 |
| Prometheus | 9090 | HTTP | 监控 |
| Grafana | 3000 | HTTP | 可视化 |

---

## 2. 部署架构

### 2.1 生产环境规格

**应用服务器**:
```yaml
API Gateway: 3 节点 (8C 16G)
Auth Service: 2 节点 (4C 8G)
Product Service: 3 节点 (8C 16G)
Inventory Service: 3 节点 (8C 16G)
Order Service: 3 节点 (8C 16G)
Warehouse Service: 2 节点 (4C 8G)
Logistics Service: 2 节点 (4C 8G)
```

**中间件服务器**:
```yaml
Nacos 集群: 3 节点 (4C 8G)
Redis 哨兵: 1主2从 (8C 16G)
Elasticsearch: 1 master + 2 data (8C 32G, 500G SSD)
Seata Server: 3 节点 (4C 8G)
XXL-Job Admin: 2 节点 (4C 8G)
PostgreSQL: 1主2从 (16C 64G, 1T SSD)
```

**监控服务器**:
```yaml
Prometheus: 1 节点 (4C 8G)
Grafana: 1 节点 (2C 4G)
ELK Stack: 3 节点 (8C 16G)
```

### 2.2 Docker Compose 部署

```bash
# 部署所有中间件
docker-compose -f docker-compose-prod.yml up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f [service-name]

# 重启服务
docker-compose restart [service-name]
```

---

## 3. 启动与关闭流程

### 3.1 启动顺序

**中间件层** (先启动):
```bash
1. PostgreSQL (1主2从)
2. Redis (哨兵模式)
3. Nacos 集群 (3 节点)
4. Elasticsearch 集群 (3 节点)
5. Seata Server (3 节点)
6. XXL-Job Admin (2 节点)
7. Kafka / RabbitMQ
```

**应用层** (后启动):
```bash
1. Auth Service (2 节点)
2. Product Service (3 节点)
3. Inventory Service (3 节点)
4. Order Service (3 节点)
5. Warehouse Service (2 节点)
6. Logistics Service (2 节点)
7. API Gateway (3 节点)
```

**负载均衡** (最后启动):
```bash
Nginx (2 节点)
```

### 3.2 关闭顺序

**反向启动顺序**:
```bash
1. Nginx
2. API Gateway
3. 所有业务服务
4. 中间件
```

### 3.3 健康检查

```bash
# 检查所有服务健康状态
./scripts/health-check.sh

# 输出示例
✅ Nacos: Healthy (3/3 nodes)
✅ Redis: Healthy (Master: 192.168.1.101)
✅ Elasticsearch: Green (3 nodes)
✅ PostgreSQL: Healthy (Master: online, 2 slaves)
✅ API Gateway: 3/3 instances
✅ Order Service: 3/3 instances
```

---

## 4. 配置管理

### 4.1 Nacos 配置中心

**配置文件结构**:
```
scm-platform/
├── common.yaml             # 公共配置
├── scm-gateway.yaml        # 网关配置
├── scm-order.yaml          # 订单服务配置
├── scm-order-dev.yaml      # 订单服务开发环境
├── scm-order-prod.yaml     # 订单服务生产环境
└── ...
```

**配置变更流程**:
1. 在 Nacos 控制台修改配置
2. 发布配置（自动推送到客户端）
3. 验证配置生效
4. 记录变更日志

**配置备份**:
```bash
# 每天自动备份 Nacos 配置
0 2 * * * /scripts/backup-nacos-config.sh
```

### 4.2 环境变量

**关键环境变量**:
```bash
# JWT密钥（512位）
export JWT_SECRET="your-512-bit-secret-key"

# 数据库密码
export DB_PASSWORD="your-db-password"

# Redis 密码
export REDIS_PASSWORD="your-redis-password"

# Nacos 地址
export NACOS_SERVER_ADDR="192.168.1.101:8848,192.168.1.102:8848,192.168.1.103:8848"
```

---

## 5. 监控与告警

### 5.1 监控指标

**系统级监控**:
- CPU 使用率
- 内存使用率
- 磁盘使用率
- 网络带宽
- 系统负载

**应用级监控**:
- QPS / TPS
- 响应时间 (P50, P95, P99)
- 错误率
- JVM 堆内存
- GC 时间

**中间件监控**:
- Redis 缓存命中率
- ES 集群状态
- PostgreSQL 连接数
- Nacos 服务注册数
- Seata 事务成功率

### 5.2 Grafana 仪表盘

**预配置仪表盘**:
1. **系统总览**: 所有服务健康状态
2. **API Gateway**: QPS, 延迟, 错误率
3. **Order Service**: TPS, Seata 事务成功率
4. **Inventory Service**: 库存扣减 TPS, Redis 命中率
5. **Database**: QPS, 慢查询, 连接池
6. **JVM**: 堆内存, GC 次数, GC 时间

访问地址: http://grafana.scm-platform.com:3000

### 5.3 告警规则

**Prometheus Alertmanager 配置**:
```yaml
# alertmanager.yml
groups:
  - name: scm-platform-alerts
    interval: 30s
    rules:
      # 服务不可用告警
      - alert: ServiceDown
        expr: up{job=~"scm-.*"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"

      # CPU 使用率告警
      - alert: HighCPU
        expr: cpu_usage > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High CPU usage on {{ $labels.instance }}"

      # 内存使用率告警
      - alert: HighMemory
        expr: memory_usage > 85
        for: 5m
        labels:
          severity: warning

      # 订单创建 TPS 过低
      - alert: LowOrderTPS
        expr: order_create_tps < 1000
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Order creation TPS is low: {{ $value }}"

      # Seata 事务失败率过高
      - alert: HighSeataFailureRate
        expr: seata_transaction_failure_rate > 1
        for: 2m
        labels:
          severity: critical

      # Redis 缓存命中率过低
      - alert: LowRedisCacheHitRate
        expr: redis_cache_hit_rate < 90
        for: 10m
        labels:
          severity: warning

      # Elasticsearch 集群非 green
      - alert: ESClusterNotGreen
        expr: elasticsearch_cluster_status != 0
        for: 1m
        labels:
          severity: critical
```

**告警通道**:
- 钉钉群机器人（实时）
- 企业微信（实时）
- 短信（P0告警）
- 电话（P0告警超过5分钟）

---

## 6. 日志管理

### 6.1 日志收集架构

```
应用服务器
   ↓ Filebeat
ELK Stack
   ├── Elasticsearch (存储)
   ├── Logstash (处理)
   └── Kibana (查询)
```

### 6.2 日志级别

| 级别 | 使用场景 | 示例 |
|-----|---------|------|
| **ERROR** | 系统错误 | 数据库连接失败 |
| **WARN** | 警告信息 | 缓存未命中 |
| **INFO** | 重要业务日志 | 订单创建成功 |
| **DEBUG** | 调试信息 | 仅开发环境 |

**生产环境日志级别**: INFO

### 6.3 日志查询

**Kibana 查询示例**:
```bash
# 查询订单创建失败日志
level:ERROR AND service:scm-order AND message:"创建订单失败"

# 查询慢查询日志
level:WARN AND message:"slow query" AND duration:>1000

# 查询某个用户的所有操作
userId:"123e4567-e89b-12d3-a456-426614174000"
```

### 6.4 日志轮转

```bash
# logrotate 配置
/var/log/scm-platform/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 scm scm
}
```

---

## 7. 备份与恢复

### 7.1 数据库备份

**PostgreSQL 全量备份**:
```bash
# 每天凌晨 2 点全量备份
0 2 * * * /scripts/pg-backup-full.sh

# pg-backup-full.sh
#!/bin/bash
DATE=$(date +%Y%m%d)
pg_dump -U postgres -h 192.168.1.101 scm_platform | gzip > /backup/scm_platform_$DATE.sql.gz

# 保留 30 天备份
find /backup -name "scm_platform_*.sql.gz" -mtime +30 -delete
```

**PostgreSQL 增量备份** (WAL归档):
```bash
# postgresql.conf
archive_mode = on
archive_command = 'cp %p /archive/%f'
```

### 7.2 Redis 备份

**RDB + AOF 双重保障**:
```bash
# redis.conf
save 900 1       # 15分钟内至少1次写操作
save 300 10      # 5分钟内至少10次写操作
save 60 10000    # 1分钟内至少10000次写操作

appendonly yes
appendfsync everysec
```

### 7.3 Elasticsearch 快照

```bash
# 创建快照仓库
PUT /_snapshot/backup_repo
{
  "type": "fs",
  "settings": {
    "location": "/mount/backups/elasticsearch"
  }
}

# 创建快照（每天凌晨3点）
PUT /_snapshot/backup_repo/snapshot_20251224
{
  "indices": "products",
  "ignore_unavailable": true
}
```

### 7.4 恢复流程

**PostgreSQL 恢复**:
```bash
# 从备份恢复
gunzip < /backup/scm_platform_20251224.sql.gz | psql -U postgres scm_platform
```

**Elasticsearch 恢复**:
```bash
# 从快照恢复
POST /_snapshot/backup_repo/snapshot_20251224/_restore
{
  "indices": "products"
}
```

---

## 8. 应急预案

### 8.1 服务宕机应急

**API Gateway 宕机**:
```bash
1. Nginx 自动摘除故障节点
2. 告警通知运维
3. 查看日志分析原因
4. 重启服务或切换节点
5. 验证服务恢复
```

**数据库主库宕机**:
```bash
1. 哨兵自动选举新主库（< 30秒）
2. 更新 Nacos 配置（新主库地址）
3. 应用自动切换到新主库
4. 修复故障主库
5. 将故障主库降级为从库
```

**Redis 宕机**:
```bash
1. 哨兵自动故障转移
2. 应用降级：查询直接打数据库（性能下降）
3. 修复 Redis 节点
4. 验证缓存恢复
```

### 8.2 性能下降应急

**订单创建 TPS 下降**:
```bash
1. 查看 Grafana 仪表盘定位瓶颈
2. 检查 Seata Server 负载
3. 检查数据库连接池
4. 检查 Redis 缓存命中率
5. 临时扩容服务节点
6. 分析根因，制定优化方案
```

### 8.3 数据不一致应急

**MySQL 与 ES 数据不一致**:
```bash
1. 暂停 Canal 同步
2. 运行数据对账脚本
3. 生成差异报告
4. 手动修复差异数据
5. 恢复 Canal 同步
6. 验证数据一致性
```

---

## 9. 容量规划

### 9.1 当前容量

| 服务 | 当前 QPS/TPS | 单机能力 | 节点数 | 总容量 | 使用率 |
|-----|-------------|---------|--------|--------|--------|
| API Gateway | 100,000 QPS | 50,000 QPS | 3 | 150,000 QPS | 67% |
| Order Service | 10,000 TPS | 5,000 TPS | 3 | 15,000 TPS | 67% |
| Inventory Service | 20,000 TPS | 10,000 TPS | 3 | 30,000 TPS | 67% |
| Product Search | 50,000 QPS | 20,000 QPS | 3 | 60,000 QPS | 83% |

### 9.2 扩容计划

**触发条件**:
- CPU 使用率 > 70% 持续 1 小时
- 内存使用率 > 80% 持续 1 小时
- QPS 达到容量的 80%

**扩容流程**:
1. 申请新服务器
2. 部署应用服务
3. 注册到 Nacos
4. Nginx 添加新节点
5. 灰度验证
6. 全量流量

---

## 10. 值班 (On-Call)

### 10.1 值班安排

| 班次 | 时间 | 值班人 |
|-----|------|--------|
| 白班 | 9:00-21:00 | 轮值 |
| 夜班 | 21:00-9:00 | 轮值 |

### 10.2 值班职责

- 监控告警响应（< 5分钟）
- 处理生产故障
- 处理变更发布
- 记录值班日志

### 10.3 升级流程

```
P3告警 → 值班工程师处理
   ↓ 无法解决
P2告警 → 升级到 Tech Lead
   ↓ 无法解决
P1告警 → 升级到架构师
   ↓ 无法解决
P0告警 → 拉会议室全员解决
```

---

## 11. 常用运维命令

### 11.1 服务管理

```bash
# 查看服务状态
systemctl status scm-order

# 启动服务
systemctl start scm-order

# 重启服务
systemctl restart scm-order

# 查看服务日志
journalctl -u scm-order -f
```

### 11.2 Docker 管理

```bash
# 查看容器状态
docker ps -a

# 查看容器日志
docker logs -f scm-order

# 进入容器
docker exec -it scm-order /bin/bash

# 重启容器
docker restart scm-order
```

### 11.3 数据库管理

```bash
# 连接数据库
psql -U postgres -d scm_platform

# 查看慢查询
SELECT * FROM pg_stat_statements ORDER BY mean_time DESC LIMIT 10;

# 查看连接数
SELECT count(*) FROM pg_stat_activity;

# 杀死慢查询
SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE ...;
```

---

## 12. 附录

### 12.1 故障处理流程

```
故障发现 → 告警通知 → 定位问题 → 应急处理 → 恢复服务 → 根因分析 → 编写复盘报告
```

### 12.2 联系方式

| 角色 | 姓名 | 电话 | 微信 |
|-----|------|------|------|
| 架构师 | 张三 | 138xxxx1234 | zhangsan |
| Tech Lead | 李四 | 139xxxx5678 | lisi |
| DevOps | 吴九 | 136xxxx9012 | wujiu |

---

**文档维护**: DevOps Team
**版本**: v1.0
**最后更新**: 2025-12-24
