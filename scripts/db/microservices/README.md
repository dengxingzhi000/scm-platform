# 微服务数据库分库架构

## 概述

本目录包含按微服务架构拆分的数据库脚本，将原有的单体数据库拆分为 16 个独立的数据库（6 个基础服务 + 10 个业务/事务服务）。

## 数据库列表

| 数据库 | 服务名称 | 职责描述 | 主要表 |
|--------|----------|----------|--------|
| `db_user` | 用户服务 | 用户账户、身份认证、OAuth、WebAuthn | sys_user, sys_user_oauth, webauthn_credential |
| `db_org` | 组织服务 | 组织架构、部门管理 | sys_dept |
| `db_permission` | 权限服务 | RBAC权限、角色管理、数据权限 | sys_role, sys_permission, sys_user_role, sys_role_permission, sys_role_dept, sys_data_permission_rule, sys_role_data_rule, sys_temp_permission |
| `db_approval` | 审批服务 | 权限申请、审批流程 | sys_permission_approval |
| `db_audit` | 审计服务 | 操作审计、敏感操作日志 | sys_audit_log, sys_sensitive_operation_log |
| `db_notify` | 通知服务 | 消息通知、通知模板 | sys_notification_audit, sys_notification_template, sys_user_notification_preference |
| `db_product` | 商品服务 | 商品分类、品牌、SPU/SKU管理 | prod_category, prod_brand, prod_spu, prod_sku, prod_attribute_template |
| `db_inventory` | 库存服务 | 库存管理、库存预留、库存快照 | inv_inventory, inv_reservation, inv_snapshot, inv_alert, inv_tcc_reservation |
| `db_order` | 订单服务 | 订单管理、支付、退款 | ord_order, ord_order_item, ord_payment, ord_refund, ord_status_history |
| `db_warehouse` | 仓储服务 | 仓库管理、出入库、波次拣货 | wms_warehouse, wms_location, wms_inbound, wms_inbound_item, wms_outbound, wms_outbound_item, wms_wave_picking |
| `db_logistics` | 物流服务 | 承运商、配送区域、运单追踪 | tms_carrier, tms_delivery_area, tms_waybill, tms_tracking, tms_route |
| `db_supplier` | 供应商服务 | 供应商管理、评估、结算、采购 | sup_supplier, sup_supplier_evaluation, sup_settlement, sup_purchase_order, sup_purchase_order_item |
| `db_tenant` | 租户服务 | 多租户管理、套餐、订阅、配额 | sys_tenant, sys_tenant_package, sys_tenant_subscription, sys_tenant_resource_quota, sys_tenant_config, sys_tenant_feature, tenant_operation_log |
| `db_finance` | 财务服务 | 运费规则、结算、发票、对账 | freight_rule, settlement_order, settlement_item, invoice, reconciliation_record, platform_service_fee, freight_calc_record, payment_record |
| `db_purchase` | 采购服务 | 采购流程（15张表） | 采购申请、采购订单、到货、质检等 |
| `db_seata` | Seata服务器 | Seata分布式事务协调器元数据 | 全局事务、分支事务、全局锁 |
| `db_undo_log` | Seata回滚日志 | AT模式所需的undo_log表（每个业务库各一张） | undo_log |
| `db_inventory_tcc` | 库存TCC | TCC模式库存预留表 | inv_tcc_reservation |

## 架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Gateway (8761)                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│  User Service │          │  Org Service  │          │ Perm Service  │
│   (db_user)   │◄────────►│   (db_org)    │◄────────►│(db_permission)│
└───────────────┘          └───────────────┘          └───────────────┘
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐          ┌───────────────┐          ┌───────────────┐
│Approval Serv  │          │ Audit Service │          │ Notify Service│
│ (db_approval) │          │  (db_audit)   │          │  (db_notify)  │
└───────────────┘          └───────────────┘          └───────────────┘

┌───────────────────────────── 业务服务 ──────────────────────────────────────┐

┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│Product Service│  │Inventory Serv │  │ Order Service │  │Warehouse Serv │
│ (db_product)  │  │(db_inventory) │  │  (db_order)   │  │(db_warehouse) │
└───────────────┘  └───────────────┘  └───────────────┘  └───────────────┘

┌───────────────┐  ┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│Logistics Serv │  │Supplier Serv  │  │ Tenant Service│  │Finance Service│
│(db_logistics) │  │(db_supplier)  │  │  (db_tenant)  │  │ (db_finance)  │
└───────────────┘  └───────────────┘  └───────────────┘  └───────────────┘

┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│Purchase Serv  │  │Seata Server   │  │ Inventory TCC │
│(db_purchase)  │  │  (db_seata)   │  │(db_inventory) │
└───────────────┘  └───────────────┘  └───────────────┘
```

## 跨库关联说明

由于微服务架构下数据库独立，原有的外键约束改为应用层维护。以下是跨库关联关系：

### db_user (用户服务)
- `sys_user.dept_id` → `db_org.sys_dept.id`

### db_org (组织服务)
- `sys_dept.leader_id` → `db_user.sys_user.id`

### db_permission (权限服务)
- `sys_user_role.user_id` → `db_user.sys_user.id`
- `sys_role_dept.dept_id` → `db_org.sys_dept.id`
- `sys_temp_permission.user_id` → `db_user.sys_user.id`
- `sys_temp_permission.approval_id` → `db_approval.sys_permission_approval.id`

### db_approval (审批服务)
- `sys_permission_approval.applicant_id` → `db_user.sys_user.id`
- `sys_permission_approval.target_user_id` → `db_user.sys_user.id`
- `sys_permission_approval.current_approver_id` → `db_user.sys_user.id`
- `sys_permission_approval.role_ids[]` → `db_permission.sys_role.id`
- `sys_permission_approval.permission_ids[]` → `db_permission.sys_permission.id`

### db_audit (审计服务)
- `sys_audit_log.user_id` → `db_user.sys_user.id`
- `sys_audit_log.dept_id` → `db_org.sys_dept.id`
- `sys_sensitive_operation_log.user_id` → `db_user.sys_user.id`
- `sys_sensitive_operation_log.approval_id` → `db_approval.sys_permission_approval.id`

### db_notify (通知服务)
- `sys_notification_audit.user_id` → `db_user.sys_user.id`
- `sys_user_notification_preference.user_id` → `db_user.sys_user.id`

### db_product (商品服务)
- `prod_spu.supplier_id` → `db_supplier.sup_supplier.id`
- `prod_sku.warehouse_id` → `db_warehouse.wms_warehouse.id`

### db_inventory (库存服务)
- `inv_inventory.sku_id` → `db_product.prod_sku.id`
- `inv_inventory.warehouse_id` → `db_warehouse.wms_warehouse.id`
- `inv_reservation.order_id` → `db_order.ord_order.id`

### db_order (订单服务)
- `ord_order.user_id` → `db_user.sys_user.id`
- `ord_order_item.sku_id` → `db_product.prod_sku.id`

### db_warehouse (仓储服务)
- `wms_inbound.supplier_id` → `db_supplier.sup_supplier.id`
- `wms_inbound_item.sku_id` → `db_product.prod_sku.id`
- `wms_outbound.order_id` → `db_order.ord_order.id`
- `wms_outbound_item.sku_id` → `db_product.prod_sku.id`

### db_logistics (物流服务)
- `tms_waybill.order_id` → `db_order.ord_order.id`
- `tms_waybill.carrier_id` → `tms_carrier.id`

### db_supplier (供应商服务)
- `sup_purchase_order.supplier_id` → `sup_supplier.id`

### db_purchase (采购服务)
- 采购订单关联 `db_supplier.sup_supplier.id` 和 `db_product.prod_sku.id`

## 部署顺序

由于存在跨库依赖，建议按以下顺序初始化数据库：

```bash
# 1. 创建数据库
psql -c "CREATE DATABASE db_user WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_org WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_permission WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_approval WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_audit WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_notify WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_product WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_inventory WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_order WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_warehouse WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_logistics WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_supplier WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_tenant WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_finance WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_purchase WITH ENCODING = 'UTF8';"
psql -c "CREATE DATABASE db_seata WITH ENCODING = 'UTF8';"

# 2. 初始化表结构（按顺序）
# 基础服务
psql -d db_user -f 001_db_user.sql
psql -d db_org -f 002_db_org.sql
psql -d db_permission -f 003_db_permission.sql
psql -d db_approval -f 004_db_approval.sql
psql -d db_audit -f 005_db_audit.sql
psql -d db_notify -f 006_db_notify.sql
psql -d db_user -f 007_data_redundancy.sql

# 业务服务
psql -d db_product -f 010_db_product.sql
psql -d db_inventory -f 011_db_inventory.sql
psql -d db_order -f 012_db_order.sql
psql -d db_warehouse -f 013_db_warehouse.sql
psql -d db_logistics -f 014_db_logistics.sql
psql -d db_supplier -f 015_db_supplier.sql
psql -d db_tenant -f 016_db_tenant.sql
psql -d db_finance -f 017_db_finance.sql
psql -d db_purchase -f 018_db_purchase.sql

# 分布式事务
psql -d db_seata -f 019_db_seata.sql

# 3. 在每个业务库中创建 Seata undo_log 表（AT 模式必需）
for db in db_user db_org db_permission db_approval db_audit db_notify db_product db_inventory db_order db_warehouse db_logistics db_supplier db_tenant db_finance db_purchase; do
  psql -d $db -f 020_undo_log_tables.sql
done

# 4. 库存 TCC 预留表
psql -d db_inventory -f 021_inventory_tcc.sql
```

## 时间戳自动填充策略

### 设计决策

本项目选择使用 **MyBatis-Plus MetaObjectHandler** 而非数据库触发器来自动填充时间戳字段。

### 原因

1. **统一性**：所有数据操作都通过应用层执行，确保一致的行为
2. **可移植性**：不依赖特定数据库的触发器语法，便于数据库迁移
3. **可测试性**：应用层逻辑更容易进行单元测试
4. **性能**：减少数据库端的计算开销
5. **可维护性**：业务逻辑集中在 Java 代码中，便于版本控制和代码审查

### 实现

由 `common/data/.../MyMetaObjectHandler.java` 自动处理以下字段：
- `create_time` - 插入时自动填充当前时间
- `create_by` - 插入时自动填充当前用户ID
- `update_time` - 更新时自动填充当前时间
- `update_by` - 更新时自动填充当前用户ID

### 注意事项

- 审计日志表（`sys_audit_log`, `sys_sensitive_operation_log`）只有 `create_time`，因为审计日志是不可变的（insert-only）
- 直接使用 SQL 工具修改数据时，`update_time` 不会自动更新（需手动设置）

## 数据一致性策略

### 1. 最终一致性
- 使用消息队列（Kafka/RabbitMQ）进行跨服务事件通知
- 例如：用户删除时，通过消息通知其他服务清理关联数据

### 2. Saga 模式
- 对于需要跨多个服务的事务操作，使用 Saga 模式
- 例如：角色授权需要同时更新 db_permission 和发送通知到 db_notify

### 3. 数据冗余
- 在高频查询场景下，允许适度的数据冗余
- 例如：db_audit.sys_audit_log 中冗余存储 username 和 real_name

## 服务间通信

### Dubbo RPC (推荐)
```java
// 权限服务调用用户服务
@DubboReference
private UserDubboService userDubboService;

UserDTO user = userDubboService.getUserById(userId);
```

### Feign REST (降级方案)
```java
// 权限服务调用用户服务
@FeignClient(name = "user-service", fallback = UserFeignFallback.class)
public interface UserFeignClient {
    @GetMapping("/api/users/{id}")
    ApiResponse<UserDTO> getUserById(@PathVariable UUID id);
}
```

## 配置示例

### application.yaml (用户服务)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/db_user
    username: ${DB_USER_USERNAME}
    password: ${DB_USER_PASSWORD}
```

### application.yaml (权限服务)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/db_permission
    username: ${DB_PERMISSION_USERNAME}
    password: ${DB_PERMISSION_PASSWORD}
```

## 监控与运维

### 分区表维护 (db_audit)
```sql
-- 每月执行一次，创建下月分区
SELECT create_audit_log_partition();

-- 归档并删除旧分区（保留6个月）
DROP TABLE IF EXISTS sys_audit_log_2024_06;
```

### 数据清理建议
| 数据库 | 表 | 保留策略 |
|--------|-----|----------|
| db_audit | sys_audit_log | 保留 12 个月，之后归档到冷存储 |
| db_audit | sys_sensitive_operation_log | 保留 36 个月（合规要求）|
| db_notify | sys_notification_audit | 保留 6 个月 |
| db_order | ord_status_history | 保留 12 个月 |
| db_inventory | inv_snapshot | 保留 6 个月 |
| db_logistics | tms_tracking | 保留 6 个月 |
| db_tenant | tenant_operation_log | 保留 12 个月 |

## 跨库查询解决方案

### 设计原则（参考阿里、美团、字节实践）

由于微服务分库后无法使用 SQL JOIN，采用以下策略：

| 策略 | 适用场景 | 实现方式 |
|------|----------|----------|
| **数据冗余** | 高频查询、变更低频 | 在目标表添加冗余字段 |
| **事件驱动同步** | 数据变更时触发 | Kafka/RabbitMQ 异步同步 |
| **应用层聚合** | 低频复杂查询 | Service 层多次查询聚合 |

### 冗余字段设计

执行 `007_data_redundancy.sql` 添加冗余字段：

| 目标表 | 冗余字段 | 数据来源 | 同步触发 |
|--------|----------|----------|----------|
| `db_permission.sys_user_role` | username, real_name, user_status | db_user.sys_user | 用户变更 |
| `db_org.sys_dept` | leader_name, leader_phone | db_user.sys_user | 用户变更 |
| `db_approval.sys_permission_approval` | applicant_name, role_names | db_user + db_permission | 用户/角色变更 |
| `db_audit.sys_audit_log` | dept_name | db_org.sys_dept | 部门变更 |

### 同步机制（企业级架构）

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           业务操作                                       │
│         (用户/部门/角色 创建/更新/删除)                                   │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    DataSyncEventPublisher                                │
│  • 封装业务实体 → DataSyncEvent                                          │
│  • 注入 TraceId (OpenTelemetry)                                          │
│  • Prometheus 指标埋点                                                   │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    KafkaDataSyncPublisher                                │
│  • 发布到 Kafka (topic: datasync.user/dept/role)                         │
│  • 使用 aggregateId 作为 partition key (保证顺序)                         │
│  • 发送失败 → 死信队列 (datasync.dlq)                                    │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
                    ▼                       ▼
        ┌───────────────────┐    ┌───────────────────┐
        │   正常消费         │    │   死信队列         │
        │   Kafka Consumer  │    │   (人工处理/告警)  │
        └─────────┬─────────┘    └───────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    RetryableEventProcessor                               │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  IdempotentChecker (Redis SETNX)                                 │    │
│  │  • 防止重复消费                                                   │    │
│  │  • 24h 过期                                                       │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│                                │                                         │
│                                ▼                                         │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │  Exponential Backoff Retry                                       │    │
│  │  • 最大重试 3 次                                                  │    │
│  │  • 退避: 1s → 2s → 4s                                            │    │
│  │  • 超限 → 死信队列                                                │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       DataSyncHandler                                    │
│  • UserSyncHandler   → db_permission, db_org                            │
│  • DeptSyncHandler   → db_audit, db_approval                            │
│  • RoleSyncHandler   → db_approval                                      │
└─────────────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    DataReconciliationTask                                │
│  • 定时对账 (cron: 0 0 3 * * ?)                                          │
│  • 比对源库与冗余字段                                                     │
│  • 可选自动修复                                                           │
└─────────────────────────────────────────────────────────────────────────┘
```

### 配置示例

```yaml
# application.yaml
datasync:
  enabled: true
  service-name: system-service
  topic-prefix: datasync
  dead-letter-topic: datasync.dlq
  publish-timeout-ms: 5000

  consumer:
    group-id: datasync-consumer
    concurrency: 3
    batch-size: 100

  retry:
    enabled: true
    max-attempts: 3
    initial-interval-ms: 1000
    max-interval-ms: 30000
    multiplier: 2.0

  idempotent:
    enabled: true
    expire-seconds: 86400
    key-prefix: "datasync:idempotent:"

  reconciliation:
    enabled: true
    cron: "0 0 3 * * ?"
    batch-size: 1000
    auto-fix: false
```

### 监控指标 (Prometheus)

| 指标名 | 类型 | 描述 |
|--------|------|------|
| `datasync.publish.success` | Counter | 发布成功次数 |
| `datasync.publish.failure` | Counter | 发布失败次数 |
| `datasync.publish.duration` | Timer | 发布耗时 |
| `datasync.process.success` | Counter | 消费成功次数 |
| `datasync.process.failure` | Counter | 消费失败次数 |
| `datasync.process.retry` | Counter | 重试次数 |
| `datasync.deadletter.count` | Counter | 进入死信队列次数 |
| `datasync.reconcile.success` | Counter | 对账成功次数 |
| `datasync.reconcile.fix` | Counter | 自动修复次数 |

### 初始化同步

系统升级后，通过对账任务进行全量同步：

```java
@Autowired
private DataReconciliationTask reconciliationTask;

// 手动触发对账（会自动修复不一致数据）
reconciliationTask.reconcile();
```

## 迁移指南

如需从单体数据库迁移到微服务数据库，请参考迁移脚本：
- `migration/001_export_data.sql` - 导出数据
- `migration/002_import_to_microservices.sql` - 导入到各微服务数据库
- `migration/003_verify_data.sql` - 数据校验