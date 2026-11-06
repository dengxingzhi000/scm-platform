# SCM Platform 文档

欢迎查阅SCM平台文档。本文档提供完整的架构设计、开发规范和技术指南。

## 相关仓库

| 仓库 | 范围 | 边界 |
|------|------|------|
| [cognora-aura-platform](https://github.com/dengxingzhi000/cognora-aura-platform) | Java 后端（本仓库） | 网关、认证、系统与全部业务服务、基础设施脚本、K8s/Helm |
| [cognora-aura-web](https://github.com/dengxingzhi000/cognora-aura-web) | Next.js 16 前端 | 前端页面、UI、API 客户端；通过网关路由与 OpenAPI 文档对接 |
| [cognora-enterprise-agent](https://github.com/dengxingzhi000/cognora-enterprise-agent) | 企业 Agent 运行时 | 独立服务，仅通过 HTTP/Dubbo 契约与平台交互 |
| [cognora-browser-agent](https://github.com/dengxingzhi000/cognora-browser-agent) | 浏览器自动化 | 独立运行时，与本仓库无构建期依赖 |

前端原位于本仓库 `scm-web/`，在 `cognora-aura-web` v0.3.0（CI 绿 + 镜像发布）后已删除。

---

## 📚 核心文档

### 🏗️ 设计与架构
- [微服务架构文档](design/ARCHITECTURE.md) - **推荐阅读** 完整的微服务架构设计、数据库思想、技术栈选型
- [SCM设计方案](design/SCM_DESIGN_PLAN.md) - 平台整体业务设计方案
- [架构决策记录](design/ADR.md) - 重要架构决策（ADR）
- [CQRS架构设计](design/CQRS_ARCHITECTURE.md) - **推荐阅读** 系统服务CQRS重构设计、跨库查询优化

### 💻 开发规范
- [开发规范](development/DEVELOPMENT_STANDARDS.md) - **必读** 完整的编码规范、配置管理、API设计标准

### 🔧 技术详细文档
- [API设计规范](technical/API_DESIGN.md) - RESTful API详细设计标准
- [数据库设计](technical/DATABASE_DESIGN.md) - 表设计、索引、分区、性能优化

### 🏢 多租户架构
- [多租户完整指南](multi-tenant/MULTI_TENANT_GUIDE.md) - **核心文档** 多租户SaaS架构的完整设计与实施

### 📖 集成指南
- [Seata集成指南](guides/SEATA_INTEGRATION_GUIDE.md) - 分布式事务AT模式集成
- [Seata TCC模式](guides/SEATA_TCC_MODE_GUIDE.md) - TCC模式实现
- [XXL-Job集成](guides/XXL_JOB_INTEGRATION_GUIDE.md) - 分布式任务调度
- [Elasticsearch集成](guides/ELASTICSEARCH_INTEGRATION_GUIDE.md) - 商品搜索集成
- [分布式事务示例](guides/DISTRIBUTED_TRANSACTION_EXAMPLE.md) - 实战代码示例

### 📦 产品与运维
- [产品需求文档](product/PRD.md) - 产品功能规划与业务流程
- [运维手册](operations/OPERATIONS_MANUAL.md) - 部署、监控、故障处理
- [性能基准](project-management/PERFORMANCE_BENCHMARK.md) - 性能测试目标与基准

---

## 🆕 新人快速上手

**推荐阅读路径**：

1. **了解架构** - [微服务架构文档](design/ARCHITECTURE.md)
   - 理解平台的微服务划分、技术栈、数据库设计思想

2. **环境搭建** - [快速开始](../QUICK_START.md)
   - 配置开发环境、启动服务、验证功能

3. **开发规范** - [开发规范](development/DEVELOPMENT_STANDARDS.md)
   - 学习编码规范、命名规范、Git工作流

4. **多租户理解** - [多租户完整指南](multi-tenant/MULTI_TENANT_GUIDE.md)
   - 理解平台的多租户隔离机制、权限设计

5. **动手实践** - [分布式事务示例](guides/DISTRIBUTED_TRANSACTION_EXAMPLE.md)
   - 通过实际代码理解Seata分布式事务

---

## 📂 文档结构

```
docs/
├── README.md                                    # 本文档
│
├── design/                                      # 架构设计 (4个)
│   ├── ARCHITECTURE.md                          # 微服务架构总览
│   ├── ADR.md                                   # 架构决策记录
│   ├── SCM_DESIGN_PLAN.md                       # 完整设计方案
│   └── CQRS_ARCHITECTURE.md                     # CQRS架构设计
│
├── development/                                 # 开发规范 (1个)
│   └── DEVELOPMENT_STANDARDS.md                 # 开发规范与配置指南
│
├── technical/                                   # 技术详细文档 (2个)
│   ├── API_DESIGN.md                            # API设计详细规范
│   └── DATABASE_DESIGN.md                       # 数据库设计详细规范
│
├── multi-tenant/                                # 多租户 (1个)
│   └── MULTI_TENANT_GUIDE.md                    # 多租户完整指南
│
├── guides/                                      # 集成指南 (5个)
│   ├── DISTRIBUTED_TRANSACTION_EXAMPLE.md
│   ├── ELASTICSEARCH_INTEGRATION_GUIDE.md
│   ├── SEATA_INTEGRATION_GUIDE.md
│   ├── SEATA_TCC_MODE_GUIDE.md
│   └── XXL_JOB_INTEGRATION_GUIDE.md
│
├── operations/                                  # 运维 (1个)
│   └── OPERATIONS_MANUAL.md
│
├── product/                                     # 产品 (1个)
│   └── PRD.md
│
├── project-management/                          # 项目管理 (1个)
│   └── PERFORMANCE_BENCHMARK.md
│
└── archived/                                    # 已归档文档
    └── CODE_NORMALIZATION_PLAN.md
```

**文档总数**: 16个核心文档 + 1个归档文档

---

## 🔍 文档索引

### 按技术主题分类

**微服务架构**:
- [ARCHITECTURE.md](design/ARCHITECTURE.md) - 架构总览
- [ADR.md](design/ADR.md) - 架构决策
- [CQRS_ARCHITECTURE.md](design/CQRS_ARCHITECTURE.md) - CQRS架构与跨库查询优化

**数据库**:
- [DATABASE_DESIGN.md](technical/DATABASE_DESIGN.md) - 表设计规范
- [ARCHITECTURE.md#数据库设计思想](design/ARCHITECTURE.md#5-数据库设计思想) - 分区、分表、UUIDv7

**API开发**:
- [DEVELOPMENT_STANDARDS.md#API设计规范](development/DEVELOPMENT_STANDARDS.md#3-api设计规范) - RESTful标准
- [API_DESIGN.md](technical/API_DESIGN.md) - 详细API设计

**分布式事务**:
- [SEATA_INTEGRATION_GUIDE.md](guides/SEATA_INTEGRATION_GUIDE.md) - Seata集成
- [DISTRIBUTED_TRANSACTION_EXAMPLE.md](guides/DISTRIBUTED_TRANSACTION_EXAMPLE.md) - 代码示例

**多租户**:
- [MULTI_TENANT_GUIDE.md](multi-tenant/MULTI_TENANT_GUIDE.md) - 完整指南

**测试**:
- [DEVELOPMENT_STANDARDS.md#测试规范](development/DEVELOPMENT_STANDARDS.md#5-测试规范) - 单元测试、集成测试

---

## 📝 文档贡献

如需更新或补充文档，请遵循以下规范：

1. **Markdown格式**: 使用标准Markdown语法
2. **清晰的目录**: 复杂文档必须包含目录导航
3. **代码示例**: 提供完整的可运行代码示例
4. **交叉引用**: 使用相对路径链接到其他相关文档
5. **更新日期**: 修改文档时更新"最后更新"时间

---

**最后更新**: 2025-01-16
**维护团队**: SCM Platform Team