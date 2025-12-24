# 上线发布计划 (Release Plan)
# SCM Platform - Production Deployment

> **文档类型**: 上线发布计划
> **发布经理**: DevOps Lead
> **发布日期**: 2025-03-15 (周六 02:00-06:00)
> **版本**: v1.0 MVP
> **最后更新**: 2025-12-24

---

## 一、发布概览

### 1.1 发布信息

| 项目 | 内容 |
|-----|------|
| **版本号** | v1.0.0-MVP |
| **发布时间** | 2025-03-15 周六 02:00 AM - 06:00 AM |
| **发布类型** | 首次上线（灰度发布 → 全量发布） |
| **发布窗口** | 4小时 |
| **影响范围** | 全部用户 |
| **是否可回滚** | 是（30分钟内） |
| **预计停机时间** | 0（蓝绿部署，无停机） |

### 1.2 发布目标

**业务目标**:
- ✅ 完成核心功能上线：商品、库存、订单、仓储、物流
- ✅ 支持 1000+ 并发用户
- ✅ 订单履约时长 < 24小时

**技术目标**:
- ✅ 系统可用性 > 99.9%
- ✅ API P99延迟 < 100ms
- ✅ 订单 TPS > 5,000

---

## 二、发布策略

### 2.1 灰度发布（Canary Deployment）

**阶段1: 5% 灰度（1小时）**
```
┌─────────────────────────────────────────────┐
│              负载均衡器（Nginx）              │
└───────────┬─────────────────────────┬───────┘
            │ 5%                      │ 95%
      ┌─────┴──────┐          ┌───────┴────────┐
      │ 灰度集群   │          │  旧版本集群     │
      │ (v1.0)     │          │  (v0.9)        │
      │ 2 Pods     │          │  10 Pods       │
      └────────────┘          └────────────────┘
```

**观察指标**:
- 错误率 < 0.1%
- P99延迟无明显上升
- 数据库慢查询无增加
- 无致命错误日志

**决策点**:
- ✅ 指标正常 → 进入阶段2
- ❌ 指标异常 → 立即回滚

**阶段2: 25% 灰度（2小时）**
```
负载均衡: 25% → v1.0, 75% → v0.9
Pods: 5 (v1.0) + 10 (v0.9)
```

**阶段3: 50% 灰度（1小时）**
```
负载均衡: 50% → v1.0, 50% → v0.9
Pods: 10 (v1.0) + 10 (v0.9)
```

**阶段4: 100% 全量（保持24小时观察）**
```
负载均衡: 100% → v1.0, 0% → v0.9
Pods: 20 (v1.0), 旧版本保留备用
```

### 2.2 蓝绿部署（Blue-Green Deployment）

**部署前状态**:
```
用户流量 → Nginx → 蓝环境（v0.9）
                    绿环境（v1.0）预部署完成，但无流量
```

**切换流量**:
```bash
# 1. 验证绿环境健康
curl http://green.scm.internal/actuator/health

# 2. Nginx 切换上游
# nginx.conf
upstream backend {
    server green.scm.internal:8080;  # v1.0
    # server blue.scm.internal:8080;  # v0.9 注释掉
}

# 3. 重载 Nginx
nginx -s reload

# 4. 观察 5 分钟，无异常则成功
```

**回滚（1分钟内完成）**:
```bash
# Nginx 切回蓝环境
upstream backend {
    # server green.scm.internal:8080;  # v1.0 注释掉
    server blue.scm.internal:8080;      # v0.9 恢复
}

nginx -s reload
```

---

## 三、上线检查清单

### 3.1 代码质量检查

- [ ] 所有 P0/P1 Bug 已修复
- [ ] Code Review 100% 完成
- [ ] SonarQube 质量门禁通过（覆盖率 > 80%）
- [ ] 单元测试通过率 100%
- [ ] 集成测试通过率 > 95%

### 3.2 性能测试检查

- [ ] 压测报告已完成
- [ ] 订单 TPS > 5,000（目标 10,000）
- [ ] 商品搜索 QPS > 20,000（目标 50,000）
- [ ] P99延迟 < 200ms（目标 100ms）
- [ ] 24小时稳定性测试通过
- [ ] 容量规划文档已完成

### 3.3 安全检查

- [ ] OWASP ZAP 安全扫描通过（无高危漏洞）
- [ ] SQL注入防护测试通过
- [ ] XSS攻击防护测试通过
- [ ] JWT Token 过期机制验证
- [ ] API 签名验证测试通过
- [ ] 敏感数据加密验证（AES-256）

### 3.4 基础设施检查

- [ ] 生产环境资源准备完成（20台服务器）
- [ ] 数据库主从复制验证（延迟 < 100ms）
- [ ] Redis 集群验证（3主3从）
- [ ] Elasticsearch 集群验证（3节点）
- [ ] Kafka 集群验证（3节点）
- [ ] Nacos 集群验证（3节点）
- [ ] Seata 集群验证（3节点）

### 3.5 监控告警检查

- [ ] Prometheus + Grafana 监控大屏配置完成
- [ ] SkyWalking APM 链路追踪验证
- [ ] 日志采集（ELK）验证
- [ ] 告警规则配置完成（P0/P1/P2 分级）
- [ ] PagerDuty/钉钉告警通道测试
- [ ] On-Call 值班表已确认

### 3.6 数据库检查

- [ ] 数据库备份策略验证（每日全量 + WAL归档）
- [ ] PITR 恢复演练完成
- [ ] 数据库连接池配置验证
- [ ] 慢查询日志启用（> 1秒）
- [ ] 索引优化完成
- [ ] 分库分表策略验证

### 3.7 文档检查

- [ ] API 文档已发布（Swagger）
- [ ] 运维手册已完成
- [ ] 应急预案已演练
- [ ] 用户操作手册已完成
- [ ] 发布公告已准备

---

## 四、发布流程

### 4.1 发布时间线

| 时间 | 阶段 | 操作 | 负责人 | 预计耗时 |
|-----|------|------|--------|---------|
| **02:00** | 准备阶段 | 全员集合，确认分工 | 发布经理 | 10min |
| **02:10** | 数据库 | 数据库全量备份 | DBA | 20min |
| **02:30** | 部署 | 部署 v1.0 到绿环境 | DevOps | 30min |
| **03:00** | 验证 | 冒烟测试（绿环境） | QA | 20min |
| **03:20** | 切换 | 5% 灰度发布 | DevOps | 10min |
| **03:30** | 观察 | 监控观察（5%流量） | 全员 | 30min |
| **04:00** | 切换 | 25% 灰度发布 | DevOps | 10min |
| **04:10** | 观察 | 监控观察（25%流量） | 全员 | 50min |
| **05:00** | 切换 | 100% 全量发布 | DevOps | 10min |
| **05:10** | 验证 | 全量验证测试 | QA | 30min |
| **05:40** | 总结 | 发布复盘会议 | 全员 | 20min |
| **06:00** | 结束 | 发布完成，持续观察 | 全员 | - |

### 4.2 详细步骤

#### Step 1: 数据库备份

```bash
# 1. 全量备份（所有数据库）
for db in scm_product scm_inventory scm_order scm_warehouse scm_logistics; do
    pg_dump -h master -U scm_user -d $db -F c -f /backup/${db}_$(date +%Y%m%d_%H%M%S).dump
done

# 2. 验证备份文件
ls -lh /backup/*.dump

# 3. 测试恢复（从库）
pg_restore -h slave -U scm_user -d scm_order_test /backup/scm_order_20250315_020000.dump
```

#### Step 2: 部署应用

```bash
# 1. 拉取镜像
docker pull scm/product-service:v1.0.0
docker pull scm/inventory-service:v1.0.0
docker pull scm/order-service:v1.0.0
docker pull scm/warehouse-service:v1.0.0
docker pull scm/logistics-service:v1.0.0
docker pull scm/gateway:v1.0.0

# 2. Kubernetes 部署（绿环境）
kubectl apply -f k8s/green/product-service.yaml
kubectl apply -f k8s/green/inventory-service.yaml
kubectl apply -f k8s/green/order-service.yaml
kubectl apply -f k8s/green/warehouse-service.yaml
kubectl apply -f k8s/green/logistics-service.yaml
kubectl apply -f k8s/green/gateway.yaml

# 3. 等待 Pods Ready
kubectl wait --for=condition=Ready pod -l app=product-service -n scm-green --timeout=300s
kubectl wait --for=condition=Ready pod -l app=inventory-service -n scm-green --timeout=300s
kubectl wait --for=condition=Ready pod -l app=order-service -n scm-green --timeout=300s
kubectl wait --for=condition=Ready pod -l app=warehouse-service -n scm-green --timeout=300s
kubectl wait --for=condition=Ready pod -l app=logistics-service -n scm-green --timeout=300s
kubectl wait --for=condition=Ready pod -l app=gateway -n scm-green --timeout=300s

# 4. 验证服务健康
for service in product inventory order warehouse logistics gateway; do
    curl -f http://${service}-service.scm-green.svc.cluster.local:8080/actuator/health
    if [ $? -ne 0 ]; then
        echo "Service $service health check failed!"
        exit 1
    fi
done
```

#### Step 3: 冒烟测试

```bash
# 自动化冒烟测试脚本
python smoke_test.py --env=green

# 测试用例：
# 1. 创建商品
# 2. 搜索商品
# 3. 查询库存
# 4. 创建订单（Seata事务验证）
# 5. 查询订单
```

#### Step 4: 灰度发布（5%）

```bash
# Nginx 配置（权重）
upstream backend {
    server green.scm.internal:8080 weight=5;   # v1.0
    server blue.scm.internal:8080 weight=95;   # v0.9
}

# 重载 Nginx
nginx -t && nginx -s reload

# 验证流量分配
for i in {1..100}; do
    curl -I http://api.scm.com | grep X-App-Version
done | sort | uniq -c
# 预期输出:
#   5 X-App-Version: v1.0.0
#  95 X-App-Version: v0.9.0
```

#### Step 5: 监控观察

```bash
# Grafana Dashboard
open http://grafana.scm.com/d/release-monitor

# 关键指标：
# - 错误率（v1.0 vs v0.9 对比）
# - P99延迟（v1.0 vs v0.9 对比）
# - JVM GC时间
# - 数据库慢查询
# - Redis命中率

# 实时日志查看
kubectl logs -f -l app=order-service -n scm-green | grep ERROR

# 告警检查
curl http://prometheus.scm.com/api/v1/alerts | jq '.data.alerts[] | select(.state=="firing")'
```

#### Step 6: 全量发布

```bash
# Nginx 配置（全量）
upstream backend {
    server green.scm.internal:8080 weight=100;  # v1.0
    # server blue.scm.internal:8080 weight=0;   # v0.9 保留但不分配流量
}

nginx -t && nginx -s reload

# 验证
curl -I http://api.scm.com | grep X-App-Version
# X-App-Version: v1.0.0
```

---

## 五、回滚方案

### 5.1 回滚决策标准

**立即回滚（P0）**:
- 错误率 > 1%
- 核心功能不可用（订单创建、支付）
- 数据不一致（库存超卖）
- 数据库死锁

**计划回滚（P1）**:
- 错误率 0.1% - 1%
- 性能下降 > 30%
- 非核心功能不可用

### 5.2 回滚流程（< 5分钟）

```bash
# 1. 发布经理宣布回滚决策
echo "开始回滚到 v0.9"

# 2. Nginx 切换流量到蓝环境
upstream backend {
    # server green.scm.internal:8080 weight=100;  # v1.0 注释
    server blue.scm.internal:8080 weight=100;     # v0.9 恢复
}

nginx -t && nginx -s reload

# 3. 验证流量已切回
for i in {1..10}; do
    curl -I http://api.scm.com | grep X-App-Version
done
# 预期全部输出: X-App-Version: v0.9.0

# 4. 数据库回滚（如有DDL变更）
psql -h master -U scm_user -d scm_order -f rollback/v1.0_to_v0.9.sql

# 5. 通知全员
echo "回滚完成，当前版本: v0.9"
```

### 5.3 回滚后处理

1. **根因分析**（24小时内）
   - 收集错误日志、监控数据
   - 定位问题根因
   - 制定修复方案

2. **修复验证**（48小时内）
   - 修复 Bug
   - 回归测试
   - 性能测试

3. **二次发布**（1周内）
   - 重新评审发布计划
   - 重新执行上线检查清单

---

## 六、应急预案

### 6.1 场景1: 数据库主库宕机

**现象**: 应用报错 "Connection refused"

**处理步骤**:
```bash
# 1. 验证主库状态
pg_isready -h master -p 5432

# 2. 如果主库不可用，提升从库为主库
# 使用 Patroni 自动 Failover
patronictl failover scm-cluster --master slave1

# 3. 修改应用配置（或通过 DNS 自动切换）
# spring.datasource.url=jdbc:postgresql://slave1:5432/scm_order

# 4. 重启应用（如果不使用 DNS）
kubectl rollout restart deployment/order-service -n scm-green
```

**预计恢复时间**: 5分钟

### 6.2 场景2: Redis 集群脑裂

**现象**: 部分请求报错 "READONLY You can't write against a read only replica"

**处理步骤**:
```bash
# 1. 检查 Sentinel 状态
redis-cli -h sentinel1 -p 26379 SENTINEL masters
redis-cli -h sentinel1 -p 26379 SENTINEL get-master-addr-by-name mymaster

# 2. 手动指定 Master
redis-cli -h master -p 6379 SLAVEOF NO ONE

# 3. 重启 Sentinel
docker restart sentinel1 sentinel2 sentinel3

# 4. 验证
redis-cli -h master -p 6379 INFO replication
```

**预计恢复时间**: 10分钟

### 6.3 场景3: Elasticsearch 集群Yellow

**现象**: 搜索变慢，部分分片未分配

**处理步骤**:
```bash
# 1. 查看集群健康
curl http://es-node1:9200/_cluster/health?pretty

# 2. 查看未分配分片
curl http://es-node1:9200/_cat/shards?v | grep UNASSIGNED

# 3. 手动分配分片
curl -X POST "http://es-node1:9200/_cluster/reroute" -H 'Content-Type: application/json' -d'
{
  "commands": [
    {
      "allocate_replica": {
        "index": "products",
        "shard": 0,
        "node": "es-node2"
      }
    }
  ]
}
'

# 4. 等待集群恢复 Green
watch -n 5 'curl -s http://es-node1:9200/_cluster/health?pretty | grep status'
```

**预计恢复时间**: 20分钟

---

## 七、发布后观察

### 7.1 观察周期

| 时间 | 重点观察 | 负责人 |
|-----|---------|--------|
| **0-2小时** | 错误率、P99延迟、告警 | 全员 On-Call |
| **2-6小时** | 系统稳定性、资源使用率 | DevOps 值班 |
| **6-24小时** | 业务指标、用户反馈 | PM + 客服 |
| **1-7天** | 长期稳定性、成本 | Tech Lead |

### 7.2 成功标准

**技术指标**:
- [x] 系统可用性 > 99.9%（24小时内）
- [x] 错误率 < 0.1%
- [x] P99延迟 < 100ms
- [x] 无P0/P1告警

**业务指标**:
- [x] 订单创建成功率 > 99.9%
- [x] 订单履约时长 < 24小时
- [x] 用户投诉 < 5个

### 7.3 发布复盘

**时间**: 发布后 2 个工作日

**参与人**: 全员

**议程**:
1. 发布过程回顾（30分钟）
   - 时间线回顾
   - 问题记录

2. 问题分析（30分钟）
   - 出现了哪些问题？
   - 根因是什么？

3. 改进行动（30分钟）
   - 下次发布如何改进？
   - Action Items 分配

**输出**: 发布复盘报告（Confluence）

---

## 八、联系方式

| 角色 | 姓名 | 手机 | 邮箱 |
|-----|------|------|------|
| 发布经理 | 张三 | 138-0000-0001 | zhangsan@scm.com |
| 技术总监 | 李四 | 138-0000-0002 | lisi@scm.com |
| DBA | 王五 | 138-0000-0003 | wangwu@scm.com |
| DevOps | 赵六 | 138-0000-0004 | zhaoliu@scm.com |
| QA Lead | 孙七 | 138-0000-0005 | sunqi@scm.com |

**紧急联系**:
- 钉钉群: SCM平台发布群
- 电话会议: Zoom Meeting ID: 123-456-789

---

**文档维护**: 发布团队
**审批**: CTO + CEO
**版本**: v1.0
**最后更新**: 2025-12-24
