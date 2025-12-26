# SCM Platform 快速启动指南

> **适用人群**: 新加入团队的开发人员
> **预计时间**: 30 分钟

---

## 📋 前置条件

确保您的开发环境已安装以下工具：

| 工具 | 版本要求 | 下载地址 |
|-----|---------|---------|
| JDK | 21+ (推荐 Temurin) | https://adoptium.net/ |
| Maven | 3.8+ | https://maven.apache.org/ |
| Docker | 24+ | https://www.docker.com/ |
| Docker Compose | 2.20+ | (随 Docker Desktop 安装) |
| PostgreSQL Client | 16+ | https://www.postgresql.org/ |
| Git | 2.40+ | https://git-scm.com/ |

**验证安装**:
```bash
java -version      # 应显示 Java 21
mvn -version       # 应显示 Maven 3.8+
docker --version   # 应显示 Docker 24+
psql --version     # 应显示 PostgreSQL 16+
```

---

## 🚀 第一步：获取代码

```bash
# 克隆仓库
git checkout https://github.com/your-org/scm-platform.git
cd scm-platform

# 切换到开发分支
git checkout develop
```

---

## 🐳 第二步：启动基础设施

### 2.1 启动所有中间件

```bash
# 启动 Docker Compose（首次启动需要 5-10 分钟下载镜像）
docker-compose up -d

# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f
```

**预期输出**:
```
NAME                STATUS              PORTS
scm-nacos           Up (healthy)        0.0.0.0:8848->8848/tcp
scm-postgres        Up (healthy)        0.0.0.0:5432->5432/tcp
scm-redis           Up (healthy)        0.0.0.0:6379->6379/tcp
scm-kafka           Up (healthy)        0.0.0.0:9092->9092/tcp
scm-elasticsearch   Up (healthy)        0.0.0.0:9200->9200/tcp
scm-seata           Up                  0.0.0.0:8091->8091/tcp
scm-xxl-job         Up                  0.0.0.0:8088->8080/tcp
scm-prometheus      Up                  0.0.0.0:9090->9090/tcp
scm-grafana         Up                  0.0.0.0:3000->3000/tcp
scm-skywalking-oap  Up (healthy)        0.0.0.0:11800->11800/tcp
scm-skywalking-ui   Up                  0.0.0.0:8090->8080/tcp
```

### 2.2 验证中间件

打开浏览器访问以下地址：

| 服务 | 地址 | 用户名/密码 |
|-----|------|-----------|
| **Nacos** (服务注册) | http://localhost:8848/nacos | nacos / nacos |
| **Kibana** (ES可视化) | http://localhost:5601 | - |
| **Grafana** (监控) | http://localhost:3000 | admin / admin |
| **XXL-Job** (任务调度) | http://localhost:8088/xxl-job-admin | admin / 123456 |
| **SkyWalking** (链路追踪) | http://localhost:8090 | - |
| **Prometheus** | http://localhost:9090 | - |

**故障排查**:
```bash
# 如果某个服务 unhealthy，查看日志
docker logs scm-{service-name}

# 重启服务
docker-compose restart {service-name}

# 完全重新启动
docker-compose down
docker-compose up -d
```

---

## 💾 第三步：初始化数据库

### 3.1 Windows 用户

```cmd
# 设置密码环境变量
set PGPASSWORD=admin123

# 运行初始化脚本
cd scripts\db
init-all-databases.bat
```

### 3.2 Linux/Mac 用户

```bash
# 设置密码环境变量
export PGPASSWORD=admin123

# 运行初始化脚本
cd scripts/db
chmod +x init-all-databases.sh
./init-all-databases.sh
```

**预期输出**:
```
========================================
SCM Platform 数据库初始化
========================================
✓ PostgreSQL 连接成功

第一步: 创建数据库
✓ db_user 创建成功
✓ db_org 创建成功
✓ db_permission 创建成功
...

第二步: 初始化表结构
✓ db_user 初始化成功 (5 张表)
✓ db_org 初始化成功 (1 张表)
✓ db_permission 初始化成功 (8 张表)
...

总计: 120+ 张表

========================================
数据库初始化完成！
========================================
```

### 3.3 验证数据库

```bash
# 连接 PostgreSQL
psql -h localhost -p 5432 -U admin -d db_user

# 查看表
\dt

# 退出
\q
```

---

## 🏗️ 第四步：构建项目

```bash
# 返回项目根目录
cd ../..

# 清理并编译（首次需要 5-10 分钟下载依赖）
mvn clean install -DskipTests

# 如果遇到测试失败，可以跳过测试
mvn clean install -DskipTests
```

**预期输出**:
```
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary:
[INFO] ------------------------------------------------------------------------
[INFO] SCM Platform ...................................... SUCCESS [  1.234 s]
[INFO] SCM Common ........................................ SUCCESS [  2.345 s]
[INFO] SCM Gateway ....................................... SUCCESS [  3.456 s]
[INFO] SCM System ........................................ SUCCESS [  2.789 s]
[INFO] ...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

---

## 🎯 第五步：启动服务

**推荐启动顺序**:

### 5.1 启动 Gateway (必需)

```bash
cd scm-gateway
mvn spring-boot:run
```

等待看到日志：
```
2025-12-26 10:00:00.123  INFO --- [           main] s.g.GatewayApplication: Started GatewayApplication in 8.123 seconds
```

浏览器访问：http://localhost:8761
应看到 "SCM Platform Gateway" 页面。

### 5.2 启动 System Service (必需)

**打开新的终端窗口**:
```bash
cd scm-system/service
mvn spring-boot:run
```

### 5.3 启动业务服务（可选）

根据您的开发任务选择性启动：

```bash
# 商品服务
cd scm-product/service && mvn spring-boot:run

# 库存服务
cd scm-inventory/service && mvn spring-boot:run

# 订单服务
cd scm-order/service && mvn spring-boot:run

# 仓库服务
cd scm-warehouse/service && mvn spring-boot:run

# 物流服务
cd scm-logistics/service && mvn spring-boot:run
```

---

## ✅ 第六步：验证环境

### 6.1 检查服务注册

访问 Nacos: http://localhost:8848/nacos

登录后，点击 "服务管理" → "服务列表"，应看到：
- ✅ scm-gateway
- ✅ scm-system
- ✅ scm-product (如果已启动)
- ✅ scm-inventory (如果已启动)
- ...

### 6.2 测试 API

访问 Swagger UI: http://localhost:8761/doc.html

尝试调用一个简单的接口：

```bash
# 健康检查
curl http://localhost:8761/actuator/health

# 预期响应
{
  "status": "UP",
  "components": {
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### 6.3 查看监控

- **Prometheus**: http://localhost:9090
  - 查询示例: `up{job="scm-gateway"}`

- **Grafana**: http://localhost:3000
  - 登录: admin / admin
  - 查看 "SCM Platform Dashboard"

- **SkyWalking**: http://localhost:8090
  - 查看服务拓扑图和调用链

---

## 🧪 第七步：运行测试

```bash
# 运行所有单元测试
mvn test

# 运行特定模块的测试
cd scm-order/service
mvn test

# 运行集成测试
mvn verify -P integration-test

# 查看测试覆盖率报告
mvn jacoco:report
# 报告位置: target/site/jacoco/index.html
```

---

## 🔧 常见问题

### Q1: Docker 启动失败

**问题**: `Error starting userland proxy: listen tcp4 0.0.0.0:8848: bind: address already in use`

**解决**:
```bash
# 查找占用端口的进程
# Windows
netstat -ano | findstr 8848

# Linux/Mac
lsof -i :8848

# 杀掉进程或修改 docker-compose.yml 中的端口
```

### Q2: Maven 构建失败

**问题**: `Could not resolve dependencies`

**解决**:
```bash
# 清理 Maven 缓存
mvn clean install -U

# 或删除本地仓库
rm -rf ~/.m2/repository/*
mvn clean install
```

### Q3: 数据库连接失败

**问题**: `Connection to localhost:5432 refused`

**解决**:
```bash
# 检查 PostgreSQL 是否运行
docker ps | grep postgres

# 查看日志
docker logs scm-postgres

# 重启 PostgreSQL
docker-compose restart postgres
```

### Q4: 服务无法注册到 Nacos

**问题**: 服务启动成功但在 Nacos 看不到

**解决**:
1. 检查 `application.yml` 中的 Nacos 配置
2. 确认 Nacos 服务正常运行
3. 查看服务日志中的错误信息

---

## 📚 下一步

环境搭建完成后，建议阅读：

1. **开发规范**: `docs/DEVELOPMENT_STANDARDS.md`
2. **架构设计**: `docs/architecture/ADR.md`
3. **API 文档**: `docs/technical/API_DESIGN.md`
4. **数据库设计**: `docs/technical/DATABASE_DESIGN.md`

---

## 🆘 获取帮助

如果遇到问题：

1. 查看 **常见问题** 章节
2. 搜索 GitHub Issues
3. 联系团队 Tech Lead
4. 在团队群里提问

---

**祝您开发愉快！** 🎉