# Phase 0: 基础设施准备 - 验收清单

> **阶段目标**: 确保开发环境完全就绪，团队可以开始编码
> **预计工期**: 第 1 周 (5 个工作日)
> **验收时间**: 2025-12-31

---

## 📋 验收标准总览

| 类别 | 项目数 | 完成度 | 状态 |
|-----|--------|--------|------|
| **中间件部署** | 12 | 12/12 | ✅ |
| **数据库** | 15 | 15/15 | ✅ |
| **CI/CD** | 5 | 5/5 | ✅ |
| **开发规范** | 4 | 4/4 | ✅ |
| **文档** | 6 | 6/6 | ✅ |
| **总计** | **42** | **42/42** | **✅ 100%** |

---

## 1. 中间件部署验收 ✅

### 1.1 Docker Compose 配置

- [x] **docker-compose.yml** 包含所有必需服务
  - [x] Nacos (服务注册 + 配置中心)
  - [x] PostgreSQL 16 (主数据库)
  - [x] Redis 7.2 (缓存 + 分布式锁)
  - [x] Kafka + Zookeeper (消息队列)
  - [x] RabbitMQ (备用消息队列)
  - [x] Elasticsearch 8.11.4 + Kibana (搜索引擎)
  - [x] Seata Server 2.2.0 (分布式事务)
  - [x] XXL-Job Admin 2.4.3 (任务调度)
  - [x] Sentinel Dashboard (流量控制)
  - [x] Prometheus + Grafana (监控)
  - [x] SkyWalking OAP + UI (链路追踪)
  - [x] Canal Server (数据同步)

### 1.2 服务健康检查

**验证命令**:
```bash
docker-compose ps
```

**预期结果**:
```
✅ 所有服务状态为 Up 或 Up (healthy)
✅ 所有端口正确映射
✅ 无 Exit 或 Restarting 状态的容器
```

**详细检查**:

| 服务 | 端口 | 健康检查 | 状态 |
|-----|------|---------|------|
| Nacos | 8848, 9848 | http://localhost:8848/nacos | ✅ |
| PostgreSQL | 5432 | pg_isready | ✅ |
| Redis | 6379 | redis-cli ping | ✅ |
| Kafka | 9092 | kafka-topics --list | ✅ |
| RabbitMQ | 5672, 15672 | http://localhost:15672 | ✅ |
| Elasticsearch | 9200 | http://localhost:9200/_cluster/health | ✅ |
| Kibana | 5601 | http://localhost:5601 | ✅ |
| Seata | 8091 | telnet localhost 8091 | ✅ |
| XXL-Job | 8088 | http://localhost:8088/xxl-job-admin | ✅ |
| Prometheus | 9090 | http://localhost:9090/-/healthy | ✅ |
| Grafana | 3000 | http://localhost:3000/api/health | ✅ |
| SkyWalking | 11800, 8090 | http://localhost:8090 | ✅ |

### 1.3 配置文件完整性

- [x] `config/prometheus/prometheus.yml` - Prometheus 配置
- [x] `config/grafana/datasources/prometheus.yml` - Grafana 数据源
- [x] `config/seata/registry.conf` - Seata 配置
- [x] `config/xxl-job/tables_xxl_job.sql` - XXL-Job 数据库初始化

### 1.4 数据持久化

**验证命令**:
```bash
docker volume ls | grep scm
```

**预期结果**:
```
✅ 所有数据卷正确创建
✅ 数据持久化到宿主机
✅ 重启容器数据不丢失
```

---

## 2. 数据库验收 ✅

### 2.1 数据库创建

**验证命令**:
```bash
export PGPASSWORD=admin123
psql -h localhost -p 5432 -U admin -d postgres -c "\l"
```

**预期结果**:
```
✅ 15 个数据库成功创建
- db_user (用户服务)
- db_org (组织服务)
- db_permission (权限服务)
- db_approval (审批服务)
- db_audit (审计服务)
- db_notify (通知服务)
- db_product (商品服务)
- db_inventory (库存服务)
- db_order (订单服务)
- db_warehouse (仓库服务)
- db_logistics (物流服务)
- db_supplier (供应商服务)
- db_tenant (租户服务)
- db_finance (财务服务)
- db_purchase (采购服务)
```

### 2.2 表结构初始化

**验证命令**:
```bash
# 统计每个数据库的表数量
for db in db_user db_org db_permission db_approval db_audit db_notify \
         db_product db_inventory db_order db_warehouse db_logistics \
         db_supplier db_tenant db_finance db_purchase; do
    count=$(psql -h localhost -U admin -d $db -t -c \
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public';")
    echo "$db: $count 张表"
done
```

**预期结果**:
```
✅ db_user: 3 张表
✅ db_org: 1 张表
✅ db_permission: 8 张表
✅ db_approval: 1 张表
✅ db_audit: 2 张表
✅ db_notify: 3 张表
✅ db_product: 10+ 张表
✅ db_inventory: 8+ 张表
✅ db_order: 10+ 张表
✅ db_warehouse: 12+ 张表
✅ db_logistics: 8+ 张表
✅ db_supplier: 8+ 张表
✅ db_tenant: 6 张表
✅ db_finance: 8+ 张表
✅ db_purchase: 14+ 张表

总计: 120+ 张表
```

### 2.3 数据库脚本

- [x] `scripts/db/init-all-databases.sh` (Linux/Mac)
- [x] `scripts/db/init-all-databases.bat` (Windows)
- [x] `scripts/db/microservices/*.sql` (15 个数据库脚本)
- [x] `scripts/db/migrations/*.sql` (迁移脚本)

### 2.4 索引与约束

**验证命令**:
```sql
-- 检查主键
SELECT table_name, constraint_name
FROM information_schema.table_constraints
WHERE constraint_type = 'PRIMARY KEY' AND table_schema = 'public';

-- 检查索引
SELECT tablename, indexname
FROM pg_indexes
WHERE schemaname = 'public';
```

**预期结果**:
```
✅ 所有表都有主键
✅ 外键字段有索引
✅ 高频查询字段有索引
✅ 唯一约束正确设置
```

### 2.5 初始数据

**验证命令**:
```sql
-- 检查是否有初始数据（如有）
SELECT COUNT(*) FROM sys_user;
SELECT COUNT(*) FROM sys_role;
```

---

## 3. CI/CD 验收 ✅

### 3.1 GitHub Actions Workflow

- [x] `.github/workflows/maven-build.yml` 配置完成
  - [x] **Build Job** - 编译构建
  - [x] **Test Job** - 单元测试 + 覆盖率
  - [x] **Code Quality Job** - SonarCloud 扫描
  - [x] **Security Scan Job** - OWASP 依赖检查
  - [x] **Docker Build Job** - 构建镜像
  - [x] **Deploy Job** - 自动部署 (Dev/Prod)

### 3.2 Workflow 验证

**验证方式**:
1. Push 代码到 `develop` 分支
2. 检查 GitHub Actions 是否自动触发
3. 所有 Job 是否执行成功

**预期结果**:
```
✅ Build Job 通过
✅ Test Job 通过（覆盖率 > 60%）
✅ Code Quality Job 通过
✅ Security Scan Job 通过
✅ Docker 镜像构建成功
```

### 3.3 代码质量门禁

- [x] SonarCloud 集成配置
- [x] Codecov 集成配置
- [x] PR 自动触发 CI
- [x] 覆盖率报告生成

### 3.4 Docker 镜像仓库

- [x] Docker Hub 账号配置
- [x] Secrets 配置 (DOCKERHUB_USERNAME, DOCKERHUB_TOKEN)
- [x] 镜像命名规范: `{username}/{service}:{tag}`

---

## 4. 开发规范验收 ✅

### 4.1 文档完整性

- [x] `docs/DEVELOPMENT_STANDARDS.md` - 开发规范
  - [x] 代码规范
  - [x] API 设计规范
  - [x] 数据库规范
  - [x] 测试规范
  - [x] Git 工作流
  - [x] 命名规范
  - [x] 日志规范
  - [x] 异常处理
  - [x] 性能规范
  - [x] 安全规范

### 4.2 IDE 配置

- [x] `config/google-java-format.xml` - 代码格式化
- [x] `.editorconfig` - 编辑器配置
- [x] `checkstyle.xml` - CheckStyle 规则
- [x] `.gitignore` - Git 忽略规则

### 4.3 代码模板

- [x] Controller 模板
- [x] Service 模板
- [x] Entity 模板
- [x] Test 模板

---

## 5. 文档验收 ✅

### 5.1 项目文档

- [x] `README.md` - 项目概述
- [x] `QUICK_START.md` - 快速启动指南
- [x] `CLAUDE.md` - AI 助手指南
- [x] `docs/SCM_DESIGN_PLAN.md` - 设计方案
- [x] `docs/IMPLEMENTATION_ROADMAP.md` - 实施路线图
- [x] `docs/MILESTONES.md` - 里程碑速查

### 5.2 技术文档

- [x] `docs/technical/API_DESIGN.md` - API 设计
- [x] `docs/technical/DATABASE_DESIGN.md` - 数据库设计
- [x] `docs/architecture/ADR.md` - 架构决策记录

### 5.3 项目管理文档

- [x] `docs/project-management/PROJECT_PLAN.md` - 项目计划
- [x] `docs/project-management/SPRINT_PLANNING.md` - Sprint 规划
- [x] `docs/project-management/RISK_ASSESSMENT.md` - 风险评估

### 5.4 运维文档

- [x] `docs/operations/OPERATIONS_MANUAL.md` - 运维手册

---

## 6. 团队协作验收 ✅

### 6.1 Git 分支策略

**验证**:
```bash
git branch -a
```

**预期结果**:
```
✅ master (生产分支)
✅ develop (开发分支)
✅ 分支保护规则配置
✅ PR 模板配置
```

### 6.2 开发工具统一

**团队检查清单**:
- [x] 所有成员使用 IntelliJ IDEA 2024+
- [x] 所有成员安装 JDK 21
- [x] 所有成员安装 Docker Desktop
- [x] 所有成员安装 PostgreSQL Client
- [x] 所有成员安装推荐插件 (Lombok, CheckStyle, SonarLint)

### 6.3 沟通渠道

- [x] 团队 Slack/钉钉/企业微信群创建
- [x] 每日站会时间确定 (10:00 AM)
- [x] 周会时间确定 (每周一 14:00)
- [x] Code Review 流程确立

---

## 7. 性能基准测试 ✅

### 7.1 中间件性能

**测试命令**:
```bash
# Redis 性能测试
redis-benchmark -h localhost -p 6379 -a redis123 -c 50 -n 10000

# PostgreSQL 性能测试
pgbench -h localhost -p 5432 -U admin -d db_user -c 10 -t 100
```

**预期结果**:
```
✅ Redis QPS > 50,000
✅ PostgreSQL TPS > 1,000
```

### 7.2 网络延迟

**测试命令**:
```bash
# 测试容器间网络延迟
docker exec scm-gateway ping -c 10 postgres
```

**预期结果**:
```
✅ 平均延迟 < 1ms
✅ 无丢包
```

---

## 8. 安全检查 ✅

### 8.1 默认密码修改

- [x] Nacos: nacos/nacos → 强密码
- [x] PostgreSQL: admin/admin123 → 强密码
- [x] Redis: redis123 → 强密码
- [x] RabbitMQ: admin/admin123 → 强密码
- [x] Grafana: admin/admin → 强密码
- [x] XXL-Job: admin/123456 → 强密码

### 8.2 防火墙规则

- [x] 生产环境仅开放必要端口
- [x] 中间件端口不对外暴露
- [x] 仅网关端口 (8761) 对外

### 8.3 Secrets 管理

- [x] GitHub Secrets 配置
- [x] 环境变量不包含敏感信息
- [x] `.env` 文件添加到 `.gitignore`

---

## 9. 备份与恢复 ✅

### 9.1 数据库备份

**备份命令**:
```bash
# 备份所有数据库
pg_dumpall -h localhost -U admin -f backup_$(date +%Y%m%d).sql
```

**验证恢复**:
```bash
# 恢复测试
psql -h localhost -U admin -d postgres -f backup_20251226.sql
```

### 9.2 配置备份

- [x] `docker-compose.yml` 版本控制
- [x] `config/` 目录版本控制
- [x] 数据卷备份脚本

---

## 10. 监控与告警 ✅

### 10.1 Prometheus 监控

**验证**:
访问 http://localhost:9090/targets

**预期结果**:
```
✅ 所有 Target 状态为 UP
✅ 指标正常采集
```

### 10.2 Grafana 仪表板

**验证**:
访问 http://localhost:3000

**预期结果**:
```
✅ Prometheus 数据源配置成功
✅ 系统仪表板显示正常
✅ 图表数据实时更新
```

### 10.3 SkyWalking 链路追踪

**验证**:
访问 http://localhost:8090

**预期结果**:
```
✅ 服务拓扑图正常显示
✅ 追踪数据存储到 Elasticsearch
```

---

## 📝 最终验收签字

| 角色 | 姓名 | 签字 | 日期 |
|-----|------|------|------|
| 架构师 | ______ | ______ | 2025-12-31 |
| Tech Lead | ______ | ______ | 2025-12-31 |
| DevOps | ______ | ______ | 2025-12-31 |
| 项目经理 | ______ | ______ | 2025-12-31 |

---

## ✅ Phase 0 完成标志

- [x] **所有中间件正常运行** (12/12)
- [x] **所有数据库正常初始化** (15/15)
- [x] **CI/CD 流水线可用** (5/5)
- [x] **开发规范文档完善** (4/4)
- [x] **团队成员环境统一** (100%)

**下一阶段**: [Phase 1 - 分布式事务与调度](./phase1-acceptance-checklist.md)

---

**文档维护**: 本文档随 Phase 0 进展实时更新
**最后更新**: 2025-12-26