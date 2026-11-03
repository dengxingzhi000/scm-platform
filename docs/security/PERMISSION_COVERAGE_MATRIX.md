# API 权限覆盖矩阵与补齐方案

- 核查对象：`d:\ProgramProject\cognora-aura-platform` 当前工作区（不是 HEAD）
- 核查日期：2026-11-02
- 当前分支：`feat/supplier-frontend-completion`（工作区有大量未提交重构，本报告按磁盘上的当前代码统计）
- 本轮只读核查；新增文件仅限权限登记 SQL 与本报告，未修改任何 Java 代码，未执行 git commit / push。

## 0. 结论摘要

| 指标 | 数值 | 说明 |
|---|---:|---|
| Controller 数 | 111 | 仅统计命名以 `Controller.java` 结尾的主代码文件 |
| 路由-方法对 | 713 | 一个方法映射多个路径时分别计数 |
| 方法级 `@PreAuthorize` | 56 | 仅 scm-auth 13 + scm-system 43 |
| URL 权限表精确命中 | 11 | 全部在 scm-system，且都已同时有方法级注解，增量保护为 0 |
| 受保护路由（并集） | 56 | 占 7.9% |
| 公开端点 | 4 | 登录/刷新、支付渠道回调 |
| 内部服务间端点 | 18 | 审计写入、权限查询、库存预占、搜索索引、通知审计写入 |
| 用户自助端点 | 82 | 必须做“当前登录人 == 目标 userId”校验 |
| 混合端点 | 16 | 管理端与自助共用同一路径，需“权限码 或 本人”双分支 |
| 管理端端点 | 537 | 应有权限码，目前 100% 裸奔 |
| 完全无方法级/URL 保护 | 657 | 占 92.1% |

> 另有 `scripts/db/migrations/004_transform_permission_system_multi_tenant.sql` 里的 6 条通配符平台权限（`/api/orders/**` 等）。它们即使执行也不会命中任何当前路由：一是 `findPermissionsByUrl` 是精确匹配，二是该迁移把 `permission_type` 写成字符串 `'API'`，与 `003_db_permission.sql` 里 `SMALLINT` 的定义冲突，需单独核实是否真的执行过。

核心结论：**权限没有真正覆盖到每个服务。** 24 个有 Controller 的服务里，只有 scm-auth 与 scm-system 的方法级注解在起作用；其余 22 个服务的所有路由都只受 “已登录” 约束（背景里“19 个业务服务”是按核心业务口径统计，本次把商城/会员/支付/履约/搜索/分析等电商模块也算进来了，所以是 22 个）。URL 权限表目前只登记了 11 条 scm-system 路由，且 `findPermissionsByUrl` 是 `api_path = #{url}` 精确匹配，路径参数路由和通配符都匹配不上。

## 1. 按服务统计

 | 服务 | 路由数 | 方法级 | 公开 | 内部 | 自助 | 混合 | 管理端 | 无保护 | 
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| scm-analytics | 36 | 0 | 0 | 0 | 0 | 0 | 36 | 36 |
| scm-approval | 10 | 0 | 0 | 0 | 0 | 0 | 10 | 10 |
| scm-audit | 10 | 0 | 0 | 1 | 0 | 4 | 5 | 10 |
| scm-auth | 15 | 13 | 2 | 0 | 0 | 0 | 0 | 2 |
| scm-document | 21 | 0 | 0 | 0 | 12 | 0 | 9 | 21 |
| scm-file | 13 | 0 | 0 | 0 | 7 | 0 | 6 | 13 |
| scm-finance | 40 | 0 | 0 | 0 | 0 | 0 | 40 | 40 |
| scm-fulfillment | 12 | 0 | 0 | 0 | 0 | 1 | 11 | 12 |
| scm-inventory | 23 | 0 | 0 | 6 | 0 | 0 | 17 | 23 |
| scm-logistics | 36 | 0 | 0 | 0 | 0 | 0 | 36 | 36 |
| scm-mall | 10 | 0 | 0 | 0 | 10 | 0 | 0 | 10 |
| scm-member | 14 | 0 | 0 | 0 | 10 | 1 | 3 | 14 |
| scm-notify | 27 | 0 | 0 | 2 | 12 | 2 | 11 | 27 |
| scm-order | 24 | 0 | 0 | 0 | 4 | 4 | 16 | 24 |
| scm-order-center | 8 | 0 | 0 | 0 | 0 | 1 | 7 | 8 |
| scm-payment | 11 | 0 | 2 | 0 | 5 | 2 | 2 | 11 |
| scm-product | 40 | 0 | 0 | 0 | 0 | 0 | 40 | 40 |
| scm-promotion | 12 | 0 | 0 | 0 | 10 | 1 | 1 | 12 |
| scm-purchase | 115 | 0 | 0 | 0 | 0 | 0 | 115 | 115 |
| scm-search | 8 | 0 | 0 | 5 | 3 | 0 | 0 | 8 |
| scm-supplier | 62 | 0 | 0 | 0 | 0 | 0 | 62 | 62 |
| scm-system | 68 | 43 | 0 | 4 | 9 | 0 | 12 | 25 |
| scm-tenant | 52 | 0 | 0 | 0 | 0 | 0 | 52 | 52 |
| scm-warehouse | 46 | 0 | 0 | 0 | 0 | 0 | 46 | 46 |
| **合计** | **713** | **56** | **4** | **18** | **82** | **16** | **537** | **657** |

> scm-message 与 scm-gateway 没有 Controller，未出现在表中。

## 2. 按风险排序的“应该受保护但现在裸奔”清单

排序原则：资金/租户/审计安全 > 数据完整性 > 越权读取 > 运营操作。每条都给出建议保护方式；本轮均未落地。

### P0 — 直接影响资金、租户隔离或审计可信度

| # | 服务 | 端点 | 风险 | 建议保护方式 |
|---:|---|---|---|---|
| 1 | scm-payment | `POST /api/v1/payments/refunds/{refundNo}/approve`、`/reject` | 任意已登录用户可审批/驳回任意退款单，直接触发资金流 | 权限码 `payment:refund:approve-refund` / `payment:refund:reject-refund`，并校验退款单归属商户/租户 |
| 2 | scm-order | `PUT /api/v1/refunds/{id}/approve`、`/reject`、`/complete` | 任意用户可审核通过或完成任意退款 | 权限码 `order:refund:approve` / `order:refund:reject` / `order:refund:complete` |
| 3 | scm-order | `PUT /api/v1/orders/{id}/status` | 任意用户可推进任意订单状态（状态机只校验合法性，不校验操作人） | 权限码 `order:order:update-status`；按目标状态拆细（发货/完成等） |
| 4 | scm-order-center | `POST /api/v1/orders/{orderNo}/pay`、`/ship`、`/deliver`、`/confirm`、`/cancel` | 任意用户可对任意订单支付/发货/签收/取消 | 权限码 `order-center:order:pay-order` 等；同时校验订单归属 |
| 5 | scm-finance | `POST /settlement-order/{id}/pay`、`/invoice/{id}/issue`、`/void`、`/red-flush`、`/platform-service-fee/{id}/pay` | 任意用户可确认结算、开票、作废/红冲发票、支付服务费 | 权限码 `finance:settlement-order:pay`、`finance:invoice:issue/void-invoice/red-flush`、`finance:service-fee:pay` |
| 6 | scm-tenant | `POST /tenant`、`DELETE /tenant/{id}`、`PUT /tenant/{id}/disable`、`POST /tenant/provisioning` 等全部 52 条 | 任意已登录用户可创建/停用/删除租户、开通租户、改套餐/配额/配置，等于跨租户管理权限 | 权限码 `tenant:*`；并且只授予真正的平台管理员角色，不能授予租户管理员 |
| 7 | scm-audit | `GET /sys-audit-log/page`、`/user/{userId}`、`/module/{module}`、`/sys-sensitive-operation-log/page` 等 9 条查询路由（另有 1 条内部写入） | 任意用户可读全租户审计日志与敏感操作日志，可按任意 userId 查询 | 权限码 `audit:log:page/view`、`audit:sensitive:page/view`；限定安全管理员 |
| 8 | scm-system | `GET /api/system/permissions/user/{userId}`、`GET /api/system/users/{userId}/update-login` | 前者泄露任意用户权限集合；后者允许任意用户伪造他人最后登录时间/IP | 改为内部签名通道，或加 `system:user:list` + 本人/管理员判断 |
| 9 | scm-system | `POST/PUT/DELETE /api/v1/decision/rules/**`、`/api/v1/decision/weights/**` | 任意用户可新增/修改/启停决策规则与权重，直接影响风控/定价/路由决策 | 权限码 `system:decision-rule:*`、`system:decision-weight:*` |
| 10 | scm-analytics | `POST/PUT/DELETE /api/v1/analytics/datasets/**`、`/metrics/**`、`/dimensions/**`、`/quality/rules/**`、`POST /query` | 任意用户可改数据集/指标/质量规则；`POST /query` 的 `X-Organization-Ids` 由客户端自报，未校验用户数据范围 | 权限码 `analytics:metadata:*`、`analytics:governance:*`、`analytics:query:query`；组织范围改为从登录态解析 |
| 11 | scm-inventory | `POST /api/v1/inventory/adjust`、`/init`、`/transfer`；`POST /reservation/reserve`、`DELETE /reservation/release/{businessKey}` | 任意用户可调整/初始化/调拨库存，或释放他人的预占 | 权限码 `inventory:inventory:adjust-inventory/init-inventory/transfer-inventory`；预占接口改为内部签名通道 |
| 12 | scm-supplier | `PUT /api/v1/suppliers/{id}/audit`、`/blacklist`、`/disable`；`/suppliers/{id}/bank-accounts/**` | 任意用户可通过供应商审核、拉黑、停用，或改收款账户，存在供应商欺诈风险 | 权限码 `supplier:supplier:audit/blacklist/disable`、`supplier:bank-account:*`；银行账户变更建议双人复核 |

### P1 — 数据完整性、越权读取与批量操作

| # | 服务 | 端点 | 风险 | 建议保护方式 |
|---:|---|---|---|---|
| 13 | scm-purchase | `POST /pur-plan/{id}/approve`、`/pur-request/{id}/approve`、`/pur-contract/{id}/sign`、`/pur-quotation/{id}/select`、`/pur-receipt/{id}/quality-inspect` | 采购审批、合同签署、报价选定、质检全部可被任意用户操作 | 权限码 `purchase:plan:approve`、`purchase:request:approve`、`purchase:contract:sign`、`purchase:quotation:select`、`purchase:receipt:quality-inspect` |
| 14 | scm-file | `POST /api/v1/files/batch-delete`、`DELETE /api/v1/files/{id}`、`GET /{id}/content` | 任意用户可批量删除或下载租户内任意文件 | 权限码 `file:file:batch-delete/delete`；下载/预览增加文件归属或业务 ACL 校验 |
| 15 | scm-warehouse | `PUT /wms-inbound/{id}/receive`、`/cancel`；`PUT /wms-outbound/{id}/ship`、`/cancel`；`PUT /wms-wave-picking/{id}/start`、`/complete` | 任意用户可收货、发货、取消单据、开始/完成波次，直接改库存与履约 | 权限码 `warehouse:inbound:receive/cancel`、`warehouse:outbound:ship/cancel`、`warehouse:wave-picking:start/complete` |
| 16 | scm-logistics | `POST /api/v1/tms-waybill/{id}/cancel`、`PUT /{id}/status`、`POST /api/v1/tms-carrier`、`DELETE /{id}` | 任意用户可取消运单、改运单状态、增删承运商 | 权限码 `logistics:waybill:cancel/update-status`、`logistics:carrier:create/delete` |
| 17 | scm-notify | `POST/PUT/DELETE /sys-notification-template/**`、`DELETE /sys-notification-audit/{id}`、`POST /sys-notification-audit/{id}/send` | 任意用户可改模板、删审计、触发发送，存在通知滥用与审计破坏 | 权限码 `notify:template:*`、`notify:audit:delete/send` |
| 18 | scm-approval | `POST /approval/flow/approve`、`/reject`、`POST /sys-permission-approval/{id}/approve`、`/reject` | 任意用户可审批任意业务键或权限申请 | 权限码 `approval:flow:approve/reject`、`approval:permission:approve/reject`；并校验审批人身份 |
| 19 | scm-product | `POST/PUT/DELETE /api/v1/prod-spu/**`、`prod-sku/**`、`prod-brand/**`、`prod-category/**` | 任意用户可增删改商品、SKU、品牌、分类，破坏目录数据 | 权限码 `product:spu:*`、`product:sku:*`、`product:brand:*`、`product:category:*` |
| 20 | scm-document | `POST /api/v1/documents`、`POST /{id}/status`、`POST /api/v1/templates/{id}/versions/{versionId}/publish` | 任意用户可建文档、改状态、发布模板版本 | 权限码 `document:document:create/change-status`、`document:template:publish-version` |
| 21 | scm-search | `POST /api/v1/search/index/products`、`POST /api/v1/search/index/reindex`、`DELETE /api/v1/search/index/products/{productId}` | 任意用户可重建/清空搜索索引，造成搜索服务不可用或数据缺失 | 内部签名通道；或权限码 `search:index:*` 仅授予运维 |
| 22 | scm-inventory | `POST /api/v1/inventory/alerts/scan`、`POST /api/v1/inventory/snapshots/generate` | 任意用户可触发全量扫描/快照生成，造成数据库与消息压力 | 权限码 `inventory:alert:scan`、`inventory:snapshot:generate`；或改为 XXL-Job 内部触发 |

### P2 — 客户端自报 userId 的越权面（必须做“当前登录人 == 目标 userId”校验，不能只靠权限码）

| 服务 | 端点 | 风险 | 建议 |
|---|---|---|---|
| scm-mall | `GET/POST/DELETE /api/v1/mall/cart?userId=`、`PUT/DELETE /cart/{cartItemId}` | 客户端自报 userId，可读/改/清空他人购物车 | 从登录态取 userId；`cartItemId` 查询后校验归属 |
| scm-mall | `POST /api/v1/mall/reviews`、`DELETE /reviews/{reviewId}?userId=` | 可冒充他人发评/删评 | 从登录态取 userId；删除时校验评价归属 |
| scm-member | `/api/v1/members/{userId}/addresses/**`、`/members/{userId}`、`/members/{userId}/points`、`/points/log` | 路径直接带 userId，可读改他人地址/资料/积分 | 校验 `{userId} == 当前登录人`；管理员查询走独立管理端接口 |
| scm-notify | `/sys-user-notification/page`、`/read-all`、`/unread-count`、`/deleteBatch` | 已改为从登录态取接收人（核对过），但 `deleteBatch` 的 ids 仍需校验归属 | 保持从登录态取 userId；删除时按 id + userId 过滤 |
| scm-notify | `/sys-user-notification-preference/**` | `userId` 可由 query/path/body 自报 | 强制覆盖为当前登录人；管理员维护走权限码 |
| scm-promotion | `GET /api/v1/promotions/coupons/user/{userId}`、`POST /coupons/use`、`/verify` | 可查/核销他人优惠券 | 校验 `{userId}` / body.userId == 当前登录人 |
| scm-promotion | `/api/v1/promotions/groups/{groupBuyId}/join`、`/members`、`/groups` | body 里 userId 可自报，可替他人参团/开团 | 从登录态取 userId |
| scm-search | `GET /api/v1/search/products/personalized/{userId}` | 可读取他人个性化推荐画像 | 校验 `{userId} == 当前登录人` |
| scm-payment | `GET /api/v1/payments/page?userId=`、`POST /payments/create`（body.userId）、`POST /refunds`（body.userId） | 可查他人支付单、替他人创建支付/退款 | 从登录态取 userId；金额与订单归属由服务端校验 |
| scm-order | `POST /api/v1/orders/query`（body.userId）、`POST /api/v1/payments/query`、`/refunds/query` | 管理端与买家共用，可查他人订单/支付/退款 | 有 userId 时校验本人；管理员走权限码 |
| scm-fulfillment | `GET /api/v1/fulfillments/page?userId=` | 可查他人履约单 | 本人校验或管理端权限码 |

### 内部服务间端点（当前既无权限码，也没有统一签名）

| 服务 | 端点 | 现状 | 建议 |
|---|---|---|---|
| scm-audit | `POST /sys-audit-log` | 已有 `AuditWriteRequestGuard` HMAC 校验（核对过） | 保持；fail-closed 时加入白名单 |
| scm-system | `GET /api/system/permissions/find-by-url`、`/api`、`/user/{userId}` | 仅要求登录，任意用户可调用；`find-by-url` 是服务间权限查询入口 | 加内部签名或独立内网端口；fail-closed 时必须白名单，否则所有服务权限查询会自锁 |
| scm-system | `GET /api/system/users/{userId}/update-login` | 仅要求登录 | 内部签名或删除该 HTTP 入口，改 Dubbo |
| scm-inventory | `/api/v1/inventory/reservation/**` | 订单服务实际走 Dubbo，REST 入口无签名 | 内部签名或下线 REST 入口 |
| scm-notify | `POST /sys-notification-audit`、`/{id}/send` | 无签名 | 内部签名通道 |
| scm-search | `POST /api/v1/search/index/**`、`DELETE /index/**` | 无签名 | 内部签名通道，或仅运维权限码 |

## 3. 权限登记补齐产出

新增脚本：[019_api_permission_coverage.sql](file:///d:/ProgramProject/cognora-aura-platform/scripts/db/seed/019_api_permission_coverage.sql)（目标库 `db_permission`，依赖 `002_rbac_permission.sql`）。

- 权限码总数：**553** 条（管理端 537 + 混合 16），命名沿用 `domain:resource:action`，例如 `order:order:update-status`、`tenant:tenant:delete`。
- 可直接生效：**203** 条 literal 路径（`api_path` 是字面量，当前精确匹配即可命中）。
- 需匹配器升级后生效：**350** 条 templated 路径（`api_path` 含 `{id}` / `{orderNo}` 等占位符；当前 `findPermissionsByUrl` 精确匹配不会命中）。
- 角色绑定：`ROLE_SUPER_ADMIN` 全量；`ROLE_TENANT_ADMIN` 除 `tenant:*` 外的全量（避免租户管理员管理其他租户）；`ROLE_DEPARTMENT_MANAGER` / `ROLE_OPERATOR` 的只读授权块默认注释，待产品确认角色矩阵后启用。
- 幂等：`uuid_generate_v5` 固定 ID + `ON CONFLICT (permission_code) DO UPDATE`；角色绑定 `ON CONFLICT (role_id, permission_id) DO NOTHING`。
- 未覆盖：公开端点 4 条、内部端点 18 条、自助端点 82 条（这些不应靠权限码，按上面的方式处理）。

执行方式（Windows）：

```bat
set PGPASSWORD=admin123
psql -h localhost -p 5432 -U admin -d db_permission -v ON_ERROR_STOP=1 -f scripts\db\seed\019_api_permission_coverage.sql
```

> 注意：`seed-all.bat` / `seed-all.sh` 没有把这个新脚本加入执行列表，本轮按“只改 SQL 与文档”的约束未修改它们；需要手工执行一次，或后续单独把 019 加入种子列表。

## 4. fail-closed 实施方案（本轮不执行）

### 4.1 先决条件

1. **匹配器升级**：把 `SysPermissionServiceImpl.findPermissionsByUrl` / `SysPermissionMapper.findPermissionsByUrl` 改为占位符或 Ant 匹配（或先按 `{var}` 模板归一化 URI）。否则 350 条路径参数权限永远不会命中，fail-closed 后大量正常请求会被 403。
2. **登记覆盖率**：至少保证管理端 553 条中可生效的部分先跑通；建议先完成 literal 203 条 + 匹配器升级后的 350 条，再做强制。
3. **公共白名单**：登录、刷新、支付回调、健康检查、错误页、OpenAPI 文档、内部签名通道。
4. **内部调用豁免**：`/api/system/permissions/find-by-url`、`/api`、`/user/{userId}`、`/api/system/users/{userId}/update-login`、库存预占 REST、搜索索引、通知审计写入必须走签名或白名单，否则 fail-closed 会自锁权限查询链路。
5. **自助端点归属校验**：82 条自助端点必须先补“当前登录人 == 目标 userId”，否则 fail-closed 后要么全 403，要么被迫给所有用户发权限码。
6. **租户上下文**：`X-Tenant-Id` 目前完全由客户端提供（`TenantFilter` 只解析不校验，网关也不覆盖）。fail-closed 前必须把 tenant 从 JWT claim 或网关注入的可信 identity 中取，否则改一个 header 就能跨租户。
7. **权限可用性**：每个请求会调 `findPermissionsByUrl` + `findAllPermissionsByUserId` 两次 scm-system；`DynamicPermissionLoader` 默认关闭且无消费者。建议先启用本地缓存/动态加载，避免 fail-closed 把 scm-system 变成单点。

### 4.2 建议白名单（按服务实际路径）

| 类型 | 路径 | 说明 |
|---|---|---|
| 登录前 | `/api/auth/login`、`/api/auth/refresh` | `SecurityConfig` 已 permitAll；`ApiAccessControlFilter` 默认白名单已含 |
| 支付回调 | `/api/v1/payments/callback/**` | 渠道验签由 `PaymentCallbackVerifier` 完成，不能要求 JWT |
| 健康检查 | `/actuator/health`、`/actuator/info` | 已有 |
| 错误页 | `/error` | 避免 5xx 被改写成 401 |
| 文档 | `/v3/api-docs/**`、`/swagger-ui/**` | 仅在需要的环境开放；生产建议关闭 |
| 内部签名 | `/sys-audit-log`（POST）、`/api/system/permissions/find-by-url`、`/api`、`/user/{userId}`、`/api/system/users/{userId}/update-login` | 必须带 `X-Timestamp`/`X-Nonce`/`X-Signature`/`X-App-Id`，不能只按路径放行 |
| 内部签名 | `/api/v1/inventory/reservation/**`、`/api/v1/search/index/**`、`/sys-notification-audit/**` | 同上；或在网关只暴露用户路径，内部走独立端口 |

### 4.3 灰度与回滚

1. **影子模式（1 周）**：新增 `deny-on-unregistered=false` 的观测分支，对“未登记”请求只打点/日志（metric `security.access.unregistered`），不拒绝；按服务统计命中率。
2. **管理端强制**：先对 scm-tenant、scm-audit、scm-system 决策规则、scm-analytics 元数据、scm-finance、scm-payment 退款审批开启拒绝；这些服务的管理端用户少，回滚成本低。
3. **业务写操作**：再对 order/order-center/fulfillment/inventory/warehouse/purchase/supplier 的写接口开启；保留 `bypassRoles` 给运维/实施账号。
4. **全面强制**：最后覆盖读接口与混合接口；自助端点必须先完成归属校验。
5. **回滚开关**：保留 `frog.security.api-filter.enabled=false`（整服务关闭）与按服务的 Nacos 配置覆盖；出现 403 激增时先关过滤器，再回滚 SQL（SQL 只新增，回滚只需把新增权限 `deleted=true` 或删除）。

### 4.4 `bypassOnOpen` 是否改 false

**当前 `ApiAccessControlProperties.circuitBreaker.bypassOnOpen` 是死配置**：`ApiAccessControlFilter` 没有读取它，也没有使用 `SimpleCircuitBreaker`；只有 `StepUpProperties` 有同名但独立的配置。实际放行发生在两处：

1. `requiredPermissions.isEmpty()` 直接 `doFilter`（未登记 = 放行）；
2. 未登录时直接 `doFilter`，交给 Spring Security 的 `anyRequest().authenticated()`。

因此：把 `bypassOnOpen` 改成 false **不会改变任何行为**，除非先把熔断器接回过滤器。真正要改的是第 1 条。建议在实施方案中把它拆成两个开关：

- `deny-on-unregistered`（默认 false，灰度后逐服务打开）：空权限列表是否拒绝；
- `bypass-on-permission-service-failure`（默认 false）：权限服务不可用时是否放行。当前 `FeignPermissionAccess` 已经是 fail-closed（抛 `PermissionServiceException`），保持 false 更安全。

## 5. 本轮没做但必须做的事（建议顺序）

1. **先把 tenant 可信化**：JWT 增加 tenant claim 或网关注入并覆盖 `X-Tenant-Id`；否则任何权限码都挡不住跨租户访问。
2. **升级 URL 匹配器**：支持 `{var}` / Ant 通配；这是 350 条路径参数权限生效的前提。
3. **补自助端点归属校验**：82 条（购物车、地址、积分、通知、优惠券、个性化推荐、支付/退款/订单/履约按 userId 查询）。
4. **补内部签名通道**：权限查询、库存预占、搜索索引、通知审计写入；审计写入已有实现可复用。
5. **补方法级注解**：对 P0 清单中的 12 组端点优先加 `@PreAuthorize`（本轮按约束未加），作为 URL 权限之外的兜底。
6. **角色矩阵**：明确平台管理员、租户管理员、部门主管、业务专员的权限码集合；当前 SQL 只绑定前两者。
7. **观测与告警**：为 `security.access.denied`、`security.access.unregistered`、权限服务延迟/失败建立看板。
8. **测试**：为 fail-closed 增加集成测试（未登记→403、白名单→200、内部签名→200、无签名→403），并覆盖路径参数匹配。

## 6. 核对边界

### 已核对（读当前工作区代码/脚本）

- `ApiAccessControlFilter` 的空权限放行分支、未登录放行分支、白名单实现；
- `ApiAccessControlProperties` 的 `bypassOnOpen` 未被过滤器使用；
- `SysPermissionMapper.findPermissionsByUrl` 的 `api_path = #{url}` 精确匹配、`http_method` 支持 `*`；
- `findAllPermissionsByUserId` 返回所有类型权限码（含 type 3），角色绑定可以生效；
- 111 个 Controller 的 713 条路由（脚本解析注解，已抽查 auth/system/order/payment 等文件）；
- `scripts/db/seed/002_rbac_permission.sql` 的 11 条精确 URL 登记与 6 条通配符平台权限；
- `AuditWriteRequestGuard` 对 `POST /sys-audit-log` 的 HMAC 校验；
- `PaymentCallbackSecurityConfig` 对支付回调的 permitAll；
- `TenantFilter` 只从 header/query 解析 tenant，`JwtAuthenticationFilter` 不校验 tenant，`SecurityUser` 也没有 tenantId 字段；网关无 tenant 覆盖逻辑；
- `DynamicPermissionLoader` 默认关闭且没有消费者；
- 当前分支与工作区未提交状态（`git status`）。

### 推断/需要环境验证

- 713 条是注解解析结果；如果存在运行时动态注册的 `RouterFunction`、`@RequestMapping` 常量拼接或过滤器改写路径，实际路由可能略有出入；
- 各服务是否都成功注册 `ApiAccessControlFilter`：代码上所有服务 `scanBasePackages` 都含 `com.cognora.platform.common`，推断已注册；未逐个启动验证；
- 生产 Nacos 配置是否覆盖 `frog.security.api-filter.*`：仓库内没有覆盖，推断使用默认值；
- 现有数据库是否已执行 `002_rbac_permission.sql`、是否已有其他租户权限数据：没有连接数据库验证；
- 角色绑定后各租户用户的实际权限集合：没有运行环境验证；
- 网关 discovery locator 是否给业务服务剥离 `/scm-<service>` 前缀：仓库配置没有显式 StripPrefix，按 Spring Cloud Gateway 默认行为推断会剥离；生产行为需实测。

## 附录：完整覆盖矩阵

说明：`保护` 列中“方法级”表示有 `@PreAuthorize`；“方法级+URL”表示同时命中 `002_rbac_permission.sql` 的 11 条精确 URL 登记；“无”表示既无方法级也未命中现有 URL 权限表。`分类` 按代码语义判定。

### scm-analytics（36 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| DELETE | `/api/v1/analytics/lineage/{id}` | AnalyticsGovernanceController.deleteLineage | 无 | 管理端 | analytics:governance:delete-lineage |
| GET | `/api/v1/analytics/lineage/downstream/{node}` | AnalyticsGovernanceController.downstream | 无 | 管理端 | analytics:governance:downstream |
| GET | `/api/v1/analytics/lineage/upstream/{node}` | AnalyticsGovernanceController.upstream | 无 | 管理端 | analytics:governance:upstream |
| GET | `/api/v1/analytics/lineage` | AnalyticsGovernanceController.listLineage | 无 | 管理端 | analytics:governance:list-lineage |
| POST | `/api/v1/analytics/lineage` | AnalyticsGovernanceController.recordLineage | 无 | 管理端 | analytics:governance:record-lineage |
| GET | `/api/v1/analytics/quality/rules/{id}/results` | AnalyticsGovernanceController.listResults | 无 | 管理端 | analytics:governance:list-results |
| POST | `/api/v1/analytics/quality/rules/{id}/run` | AnalyticsGovernanceController.runRule | 无 | 管理端 | analytics:governance:run-rule |
| DELETE | `/api/v1/analytics/quality/rules/{id}` | AnalyticsGovernanceController.deleteRule | 无 | 管理端 | analytics:governance:delete-rule |
| GET | `/api/v1/analytics/quality/rules/{id}` | AnalyticsGovernanceController.getRule | 无 | 管理端 | analytics:governance:get-rule |
| PUT | `/api/v1/analytics/quality/rules/{id}` | AnalyticsGovernanceController.updateRule | 无 | 管理端 | analytics:governance:update-rule |
| GET | `/api/v1/analytics/quality/rules` | AnalyticsGovernanceController.listRules | 无 | 管理端 | analytics:governance:list-rules |
| POST | `/api/v1/analytics/quality/rules` | AnalyticsGovernanceController.createRule | 无 | 管理端 | analytics:governance:create-rule |
| DELETE | `/api/v1/analytics/datasets/{datasetId}/fields/{id}` | AnalyticsMetaController.deleteField | 无 | 管理端 | analytics:metadata:delete-field |
| GET | `/api/v1/analytics/datasets/{datasetId}/fields/{id}` | AnalyticsMetaController.getField | 无 | 管理端 | analytics:metadata:get-field |
| PUT | `/api/v1/analytics/datasets/{datasetId}/fields/{id}` | AnalyticsMetaController.updateField | 无 | 管理端 | analytics:metadata:update-field |
| GET | `/api/v1/analytics/datasets/{datasetId}/fields` | AnalyticsMetaController.listFields | 无 | 管理端 | analytics:metadata:list-fields |
| POST | `/api/v1/analytics/datasets/{datasetId}/fields` | AnalyticsMetaController.createField | 无 | 管理端 | analytics:metadata:create-field |
| DELETE | `/api/v1/analytics/datasets/{id}` | AnalyticsMetaController.deleteDataset | 无 | 管理端 | analytics:metadata:delete-dataset |
| GET | `/api/v1/analytics/datasets/{id}` | AnalyticsMetaController.getDataset | 无 | 管理端 | analytics:metadata:get-dataset |
| PUT | `/api/v1/analytics/datasets/{id}` | AnalyticsMetaController.updateDataset | 无 | 管理端 | analytics:metadata:update-dataset |
| GET | `/api/v1/analytics/datasets/code/{code}` | AnalyticsMetaController.getDatasetByCode | 无 | 管理端 | analytics:metadata:get-dataset-by-code |
| GET | `/api/v1/analytics/datasets` | AnalyticsMetaController.listDatasets | 无 | 管理端 | analytics:metadata:list-datasets |
| POST | `/api/v1/analytics/datasets` | AnalyticsMetaController.createDataset | 无 | 管理端 | analytics:metadata:create-dataset |
| DELETE | `/api/v1/analytics/dimensions/{id}` | AnalyticsMetaController.deleteDimension | 无 | 管理端 | analytics:metadata:delete-dimension |
| GET | `/api/v1/analytics/dimensions/{id}` | AnalyticsMetaController.getDimension | 无 | 管理端 | analytics:metadata:get-dimension |
| PUT | `/api/v1/analytics/dimensions/{id}` | AnalyticsMetaController.updateDimension | 无 | 管理端 | analytics:metadata:update-dimension |
| GET | `/api/v1/analytics/dimensions/code/{code}` | AnalyticsMetaController.getDimensionByCode | 无 | 管理端 | analytics:metadata:get-dimension-by-code |
| GET | `/api/v1/analytics/dimensions` | AnalyticsMetaController.listDimensions | 无 | 管理端 | analytics:metadata:list-dimensions |
| POST | `/api/v1/analytics/dimensions` | AnalyticsMetaController.createDimension | 无 | 管理端 | analytics:metadata:create-dimension |
| DELETE | `/api/v1/analytics/metrics/{id}` | AnalyticsMetaController.deleteMetric | 无 | 管理端 | analytics:metadata:delete-metric |
| GET | `/api/v1/analytics/metrics/{id}` | AnalyticsMetaController.getMetric | 无 | 管理端 | analytics:metadata:get-metric |
| PUT | `/api/v1/analytics/metrics/{id}` | AnalyticsMetaController.updateMetric | 无 | 管理端 | analytics:metadata:update-metric |
| GET | `/api/v1/analytics/metrics/code/{code}` | AnalyticsMetaController.getMetricByCode | 无 | 管理端 | analytics:metadata:get-metric-by-code |
| GET | `/api/v1/analytics/metrics` | AnalyticsMetaController.listMetrics | 无 | 管理端 | analytics:metadata:list-metrics |
| POST | `/api/v1/analytics/metrics` | AnalyticsMetaController.createMetric | 无 | 管理端 | analytics:metadata:create-metric |
| POST | `/api/v1/analytics/query` | AnalyticsQueryController.query | 无 | 管理端 | analytics:query:query |

### scm-approval（10 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| POST | `/approval/flow/approve` | ApprovalFlowController.approve | 无 | 管理端 | approval:flow:approve |
| POST | `/approval/flow/reject` | ApprovalFlowController.reject | 无 | 管理端 | approval:flow:reject |
| POST | `/approval/flow/start` | ApprovalFlowController.startProcess | 无 | 管理端 | approval:flow:start-process |
| GET | `/approval/flow/status/{businessKey}` | ApprovalFlowController.getProcessStatus | 无 | 管理端 | approval:flow:get-process-status |
| GET | `/approval/flow/todo` | ApprovalFlowController.findTodoTasks | 无 | 管理端 | approval:flow:find-todo-tasks |
| POST | `/sys-permission-approval/{id}/approve` | SysPermissionApprovalController.approve | 无 | 管理端 | approval:permission:approve |
| POST | `/sys-permission-approval/{id}/reject` | SysPermissionApprovalController.reject | 无 | 管理端 | approval:permission:reject |
| GET | `/sys-permission-approval/applicant/{applicantId}` | SysPermissionApprovalController.listByApplicant | 无 | 管理端 | approval:permission:list-by-applicant |
| GET | `/sys-permission-approval/pending` | SysPermissionApprovalController.listPending | 无 | 管理端 | approval:permission:list-pending |
| POST | `/sys-permission-approval/submit` | SysPermissionApprovalController.submitApproval | 无 | 管理端 | approval:permission:submit-approval |

### scm-audit（10 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| GET | `/sys-audit-log/{id}` | SysAuditLogController.getById | 无 | 管理端 | audit:log:view |
| GET | `/sys-audit-log/business-type/{businessType}` | SysAuditLogController.listByBusinessType | 无 | 管理端 | audit:log:list-by-business-type |
| GET | `/sys-audit-log/module/{module}` | SysAuditLogController.listByModule | 无 | 管理端 | audit:log:list-by-module |
| GET | `/sys-audit-log/page` | SysAuditLogController.pageQuery | 无 | 混合（管理端+自助） | audit:log:page；有 userId 时校验本人 |
| GET | `/sys-audit-log/user/{userId}` | SysAuditLogController.listByUserId | 无 | 混合（管理端+自助） | audit:log:list-by-user-id；有 userId 时校验本人 |
| POST | `/sys-audit-log` | SysAuditLogController.create | 无 | 内部调用 | 内部签名/白名单 |
| GET | `/sys-sensitive-operation-log/{id}` | SysSensitiveOperationLogController.getById | 无 | 管理端 | audit:sensitive:view |
| GET | `/sys-sensitive-operation-log/page` | SysSensitiveOperationLogController.pageQuery | 无 | 混合（管理端+自助） | audit:sensitive:page；有 userId 时校验本人 |
| GET | `/sys-sensitive-operation-log/risk-level/{riskScore}` | SysSensitiveOperationLogController.listByRiskLevel | 无 | 管理端 | audit:sensitive:list-by-risk-level |
| GET | `/sys-sensitive-operation-log/user/{userId}` | SysSensitiveOperationLogController.listByUserId | 无 | 混合（管理端+自助） | audit:sensitive:list-by-user-id；有 userId 时校验本人 |

### scm-auth（15 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
 | POST | `/oauth2/logout` | OAuth2LogoutController.logout | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/auth/force-logout/{userId}` | SysAuthController.forceLogout | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
| POST | `/api/auth/login` | SysAuthController.login | 无 | 公开 | 公开白名单 |
 | POST | `/api/auth/logout` | SysAuthController.logout | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
| POST | `/api/auth/refresh` | SysAuthController.refreshToken | 无 | 公开 | 公开白名单 |
 | GET | `/api/auth/userinfo` | SysAuthController.getUserInfo | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/auth/webauthn/authenticate/challenge` | WebAuthnAuthenticationController.generateAuthenticationChallenge | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/auth/webauthn/authenticate/verify` | WebAuthnAuthenticationController.authenticateAndUpgradeToken | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/auth/webauthn/credentials/{credentialId}/deactivate` | WebAuthnCredentialController.deactivateCredential | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/auth/webauthn/credentials/{credentialId}/name` | WebAuthnCredentialController.updateDeviceName | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | DELETE | `/api/auth/webauthn/credentials/{credentialId}` | WebAuthnCredentialController.deleteCredential | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/auth/webauthn/credentials/health` | WebAuthnCredentialController.checkCredentialHealth | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/auth/webauthn/credentials` | WebAuthnCredentialController.listCredentials | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/auth/webauthn/register/challenge` | WebAuthnRegistrationController.generateRegistrationChallenge | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/auth/webauthn/register/verify` | WebAuthnRegistrationController.registerCredential | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 

### scm-document（21 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
 | GET | `/api/v1/documents/{id}/audits` | DocumentController.listAudits | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
| POST | `/api/v1/documents/{id}/status` | DocumentController.changeStatus | 无 | 管理端 | document:document:change-status |
 | GET | `/api/v1/documents/{id}/versions` | DocumentController.listVersions | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
 | GET | `/api/v1/documents/{id}` | DocumentController.getById | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
 | GET | `/api/v1/documents/page` | DocumentController.page | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
| POST | `/api/v1/documents` | DocumentController.create | 无 | 管理端 | document:document:create |
 | GET | `/api/v1/document-files/{id}/url` | DocumentFileController.presignedUrl | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
| DELETE | `/api/v1/document-files/{id}` | DocumentFileController.delete | 无 | 管理端 | document:file:delete |
 | GET | `/api/v1/document-files/{id}` | DocumentFileController.getById | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
 | GET | `/api/v1/document-files/page` | DocumentFileController.page | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
| POST | `/api/v1/document-files` | DocumentFileController.create | 无 | 管理端 | document:file:create |
| POST | `/api/v1/templates/{id}/versions/{versionId}/publish` | TemplateController.publishVersion | 无 | 管理端 | document:template:publish-version |
 | GET | `/api/v1/templates/{id}/versions` | TemplateController.listVersions | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
| POST | `/api/v1/templates/{id}/versions` | TemplateController.createVersion | 无 | 管理端 | document:template:create-version |
 | GET | `/api/v1/templates/{id}` | TemplateController.getById | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
| PUT | `/api/v1/templates/{id}` | TemplateController.update | 无 | 管理端 | document:template:update |
 | GET | `/api/v1/templates/{templateId}/schema` | TemplateController.listSchema | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
| POST | `/api/v1/templates/{templateId}/schema` | TemplateController.saveSchema | 无 | 管理端 | document:template:save-schema |
 | GET | `/api/v1/templates/page` | TemplateController.page | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
 | POST | `/api/v1/templates/render` | TemplateController.render | 无 | 用户自助 | 校验文档归属/可见范围或业务 ACL | 
| POST | `/api/v1/templates` | TemplateController.create | 无 | 管理端 | document:template:create |

### scm-file（13 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
 | GET | `/api/v1/files/{id}/content` | FileController.download | 无 | 用户自助 | 校验文件归属/业务 ACL | 
 | GET | `/api/v1/files/{id}/preview` | FileController.preview | 无 | 用户自助 | 校验文件归属/业务 ACL | 
 | GET | `/api/v1/files/{id}/url` | FileController.presignedUrl | 无 | 用户自助 | 校验文件归属/业务 ACL | 
 | DELETE | `/api/v1/files/{id}` | FileController.delete | 无 | 用户自助 | 校验文件归属/业务 ACL | 
 | PUT | `/api/v1/files/{id}` | FileController.update | 无 | 用户自助 | 校验文件归属/业务 ACL | 
| POST | `/api/v1/files/batch-delete` | FileController.batchDelete | 无 | 管理端 | file:file:batch-delete |
 | GET | `/api/v1/files/check/{md5}` | FileController.checkExist | 无 | 用户自助 | 校验文件归属/业务 ACL | 
| GET | `/api/v1/files/page` | FileController.page | 无 | 管理端 | file:file:page |
| GET | `/api/v1/files/stats` | FileController.stats | 无 | 管理端 | file:file:stats |
 | POST | `/api/v1/files/upload` | FileController.upload | 无 | 用户自助 | 校验文件归属/业务 ACL | 
| POST | `/file/ocr/contract` | OcrController.extractContract | 无 | 管理端 | file:ocr:extract-contract |
| POST | `/file/ocr/invoice` | OcrController.extractInvoice | 无 | 管理端 | file:ocr:extract-invoice |
| POST | `/file/ocr/recognize` | OcrController.recognizeText | 无 | 管理端 | file:ocr:recognize-text |

### scm-finance（40 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| DELETE | `/freight-rule/{id}` | FreightRuleController.delete | 无 | 管理端 | finance:freight-rule:delete |
| GET | `/freight-rule/{id}` | FreightRuleController.getById | 无 | 管理端 | finance:freight-rule:view |
| PUT | `/freight-rule/{id}` | FreightRuleController.update | 无 | 管理端 | finance:freight-rule:update |
| GET | `/freight-rule/active` | FreightRuleController.listActiveRules | 无 | 管理端 | finance:freight-rule:list-active-rules |
| GET | `/freight-rule/calculate` | FreightRuleController.calculateFreight | 无 | 管理端 | finance:freight-rule:calculate-freight |
| POST | `/freight-rule` | FreightRuleController.create | 无 | 管理端 | finance:freight-rule:create |
| POST | `/invoice/{id}/issue` | InvoiceController.issue | 无 | 管理端 | finance:invoice:issue |
| POST | `/invoice/{id}/red-flush` | InvoiceController.redFlush | 无 | 管理端 | finance:invoice:red-flush |
| POST | `/invoice/{id}/void` | InvoiceController.voidInvoice | 无 | 管理端 | finance:invoice:void-invoice |
| DELETE | `/invoice/{id}` | InvoiceController.delete | 无 | 管理端 | finance:invoice:delete |
| GET | `/invoice/{id}` | InvoiceController.getById | 无 | 管理端 | finance:invoice:view |
| PUT | `/invoice/{id}` | InvoiceController.update | 无 | 管理端 | finance:invoice:update |
| GET | `/invoice/page` | InvoiceController.pageByPartyId | 无 | 管理端 | finance:invoice:page-by-party-id |
| POST | `/invoice` | InvoiceController.create | 无 | 管理端 | finance:invoice:create |
| POST | `/platform-service-fee/{id}/pay` | PlatformServiceFeeController.pay | 无 | 管理端 | finance:service-fee:pay |
| DELETE | `/platform-service-fee/{id}` | PlatformServiceFeeController.delete | 无 | 管理端 | finance:service-fee:delete |
| GET | `/platform-service-fee/{id}` | PlatformServiceFeeController.getById | 无 | 管理端 | finance:service-fee:view |
| PUT | `/platform-service-fee/{id}` | PlatformServiceFeeController.update | 无 | 管理端 | finance:service-fee:update |
| GET | `/platform-service-fee/pending` | PlatformServiceFeeController.listPendingFees | 无 | 管理端 | finance:service-fee:list-pending-fees |
| POST | `/platform-service-fee` | PlatformServiceFeeController.create | 无 | 管理端 | finance:service-fee:create |
| POST | `/reconciliation-record/{id}/confirm` | ReconciliationRecordController.confirm | 无 | 管理端 | finance:reconciliation:confirm |
| POST | `/reconciliation-record/{id}/mark-diff` | ReconciliationRecordController.markAsDiff | 无 | 管理端 | finance:reconciliation:mark-as-diff |
| POST | `/reconciliation-record/{id}/reconcile` | ReconciliationRecordController.reconcile | 无 | 管理端 | finance:reconciliation:reconcile |
| DELETE | `/reconciliation-record/{id}` | ReconciliationRecordController.delete | 无 | 管理端 | finance:reconciliation:delete |
| GET | `/reconciliation-record/{id}` | ReconciliationRecordController.getById | 无 | 管理端 | finance:reconciliation:view |
| PUT | `/reconciliation-record/{id}` | ReconciliationRecordController.update | 无 | 管理端 | finance:reconciliation:update |
| GET | `/reconciliation-record/page` | ReconciliationRecordController.page | 无 | 管理端 | finance:reconciliation:page |
| POST | `/reconciliation-record` | ReconciliationRecordController.create | 无 | 管理端 | finance:reconciliation:create |
| DELETE | `/settlement-item/{id}` | SettlementItemController.delete | 无 | 管理端 | finance:settlement-item:delete |
| GET | `/settlement-item/{id}` | SettlementItemController.getById | 无 | 管理端 | finance:settlement-item:view |
| PUT | `/settlement-item/{id}` | SettlementItemController.update | 无 | 管理端 | finance:settlement-item:update |
| GET | `/settlement-item/by-settlement/{settlementId}` | SettlementItemController.listBySettlementId | 无 | 管理端 | finance:settlement-item:list-by-settlement-id |
| POST | `/settlement-item` | SettlementItemController.create | 无 | 管理端 | finance:settlement-item:create |
| POST | `/settlement-order/{id}/confirm` | SettlementOrderController.confirm | 无 | 管理端 | finance:settlement-order:confirm |
| POST | `/settlement-order/{id}/pay` | SettlementOrderController.pay | 无 | 管理端 | finance:settlement-order:pay |
| DELETE | `/settlement-order/{id}` | SettlementOrderController.delete | 无 | 管理端 | finance:settlement-order:delete |
| GET | `/settlement-order/{id}` | SettlementOrderController.getById | 无 | 管理端 | finance:settlement-order:view |
| PUT | `/settlement-order/{id}` | SettlementOrderController.update | 无 | 管理端 | finance:settlement-order:update |
| GET | `/settlement-order/list` | SettlementOrderController.listByStatus | 无 | 管理端 | finance:settlement-order:list-by-status |
| POST | `/settlement-order` | SettlementOrderController.create | 无 | 管理端 | finance:settlement-order:create |

### scm-fulfillment（12 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| POST | `/api/v1/fulfillments/{fulfillmentNo}/cancel` | FulfillmentController.cancelFulfillment | 无 | 管理端 | fulfillment:fulfillment:cancel-fulfillment |
| POST | `/api/v1/fulfillments/{fulfillmentNo}/deliver` | FulfillmentController.confirmDelivery | 无 | 管理端 | fulfillment:fulfillment:confirm-delivery |
| POST | `/api/v1/fulfillments/{fulfillmentNo}/pack` | FulfillmentController.packItems | 无 | 管理端 | fulfillment:fulfillment:pack-items |
| POST | `/api/v1/fulfillments/{fulfillmentNo}/pick` | FulfillmentController.pickItems | 无 | 管理端 | fulfillment:fulfillment:pick-items |
| POST | `/api/v1/fulfillments/{fulfillmentNo}/ship` | FulfillmentController.shipItems | 无 | 管理端 | fulfillment:fulfillment:ship-items |
| GET | `/api/v1/fulfillments/{fulfillmentNo}` | FulfillmentController.getFulfillment | 无 | 管理端 | fulfillment:fulfillment:get-fulfillment |
| GET | `/api/v1/fulfillments/page` | FulfillmentController.page | 无 | 混合（管理端+自助） | fulfillment:fulfillment:page；有 userId 时校验本人 |
| POST | `/api/v1/fulfillments` | FulfillmentController.createFulfillment | 无 | 管理端 | fulfillment:fulfillment:create-fulfillment |
| GET | `/api/v1/fulfillments/{fulfillmentNo}/tracking/latest` | TrackingController.getLatest | 无 | 管理端 | fulfillment:tracking:get-latest |
| GET | `/api/v1/fulfillments/{fulfillmentNo}/tracking` | TrackingController.getByFulfillmentNo | 无 | 管理端 | fulfillment:tracking:get-by-fulfillment-no |
| GET | `/api/v1/fulfillments/tracking/{trackingNo}` | TrackingController.getByTrackingNo | 无 | 管理端 | fulfillment:tracking:get-by-tracking-no |
| POST | `/api/v1/fulfillments/tracking` | TrackingController.recordEvent | 无 | 管理端 | fulfillment:tracking:record-event |

### scm-inventory（23 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| PUT | `/api/v1/inventory/alerts/{id}/resolve` | InvAlertController.resolve | 无 | 管理端 | inventory:alert:resolve |
| GET | `/api/v1/inventory/alerts/{id}` | InvAlertController.get | 无 | 管理端 | inventory:alert:view |
| POST | `/api/v1/inventory/alerts/query` | InvAlertController.query | 无 | 管理端 | inventory:alert:query |
| POST | `/api/v1/inventory/alerts/scan` | InvAlertController.scan | 无 | 管理端 | inventory:alert:scan |
| GET | `/api/v1/inventory/alerts/stats` | InvAlertController.stats | 无 | 管理端 | inventory:alert:stats |
| POST | `/api/v1/inventory/adjust` | InvInventoryController.adjustInventory | 无 | 管理端 | inventory:inventory:adjust-inventory |
| POST | `/api/v1/inventory/batch` | InvInventoryController.batchGetInventory | 无 | 管理端 | inventory:inventory:batch-get-inventory |
| GET | `/api/v1/inventory/check` | InvInventoryController.checkStockAvailable | 无 | 管理端 | inventory:inventory:check-stock-available |
| POST | `/api/v1/inventory/init` | InvInventoryController.initInventory | 无 | 管理端 | inventory:inventory:init-inventory |
| POST | `/api/v1/inventory/query` | InvInventoryController.queryInventory | 无 | 管理端 | inventory:inventory:query-inventory |
| GET | `/api/v1/inventory/stats` | InvInventoryController.getInventoryStats | 无 | 管理端 | inventory:inventory:get-inventory-stats |
| POST | `/api/v1/inventory/transfer` | InvInventoryController.transferInventory | 无 | 管理端 | inventory:inventory:transfer-inventory |
| GET | `/api/v1/inventory` | InvInventoryController.getInventory | 无 | 管理端 | inventory:inventory:get-inventory |
| GET | `/api/v1/inventory/reservation/check/{businessKey}` | InvReservationController.checkReservationExists | 无 | 内部调用 | 内部签名/白名单 |
| PUT | `/api/v1/inventory/reservation/confirm/{businessKey}` | InvReservationController.confirmReservation | 无 | 内部调用 | 内部签名/白名单 |
| GET | `/api/v1/inventory/reservation/quantity/{businessKey}` | InvReservationController.getReservedQuantity | 无 | 内部调用 | 内部签名/白名单 |
| POST | `/api/v1/inventory/reservation/release-expired` | InvReservationController.releaseExpiredReservations | 无 | 内部调用 | 内部签名/白名单 |
| DELETE | `/api/v1/inventory/reservation/release/{businessKey}` | InvReservationController.releaseReservation | 无 | 内部调用 | 内部签名/白名单 |
| POST | `/api/v1/inventory/reservation/reserve` | InvReservationController.reserveInventory | 无 | 内部调用 | 内部签名/白名单 |
| GET | `/api/v1/inventory/snapshots/{id}` | InvSnapshotController.get | 无 | 管理端 | inventory:snapshot:view |
| POST | `/api/v1/inventory/snapshots/generate` | InvSnapshotController.generate | 无 | 管理端 | inventory:snapshot:generate |
| POST | `/api/v1/inventory/snapshots/query` | InvSnapshotController.query | 无 | 管理端 | inventory:snapshot:query |
| GET | `/api/v1/inventory/snapshots/trend` | InvSnapshotController.trend | 无 | 管理端 | inventory:snapshot:trend |

### scm-logistics（36 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| PUT | `/api/v1/tms-carrier/{id}/enable` | TmsCarrierController.toggleEnabled | 无 | 管理端 | logistics:carrier:toggle-enabled |
| DELETE | `/api/v1/tms-carrier/{id}` | TmsCarrierController.delete | 无 | 管理端 | logistics:carrier:delete |
| GET | `/api/v1/tms-carrier/{id}` | TmsCarrierController.getById | 无 | 管理端 | logistics:carrier:view |
| PUT | `/api/v1/tms-carrier/{id}` | TmsCarrierController.update | 无 | 管理端 | logistics:carrier:update |
| GET | `/api/v1/tms-carrier/code/{carrierCode}` | TmsCarrierController.getByCarrierCode | 无 | 管理端 | logistics:carrier:get-by-carrier-code |
| GET | `/api/v1/tms-carrier/enabled` | TmsCarrierController.listEnabled | 无 | 管理端 | logistics:carrier:list-enabled |
| GET | `/api/v1/tms-carrier/list` | TmsCarrierController.pageList | 无 | 管理端 | logistics:carrier:page |
| POST | `/api/v1/tms-carrier` | TmsCarrierController.create | 无 | 管理端 | logistics:carrier:create |
| DELETE | `/api/v1/tms-delivery-area/{id}` | TmsDeliveryAreaController.delete | 无 | 管理端 | logistics:area:delete |
| GET | `/api/v1/tms-delivery-area/{id}` | TmsDeliveryAreaController.getById | 无 | 管理端 | logistics:area:view |
| PUT | `/api/v1/tms-delivery-area/{id}` | TmsDeliveryAreaController.update | 无 | 管理端 | logistics:area:update |
| GET | `/api/v1/tms-delivery-area/carrier/{carrierId}` | TmsDeliveryAreaController.listByCarrier | 无 | 管理端 | logistics:area:list-by-carrier |
| GET | `/api/v1/tms-delivery-area/check-coverage` | TmsDeliveryAreaController.checkCoverage | 无 | 管理端 | logistics:area:check-coverage |
| GET | `/api/v1/tms-delivery-area/list` | TmsDeliveryAreaController.pageList | 无 | 管理端 | logistics:area:page |
| POST | `/api/v1/tms-delivery-area` | TmsDeliveryAreaController.create | 无 | 管理端 | logistics:area:create |
| DELETE | `/api/v1/tms-route/{id}` | TmsRouteController.delete | 无 | 管理端 | logistics:route:delete |
| GET | `/api/v1/tms-route/{id}` | TmsRouteController.getById | 无 | 管理端 | logistics:route:view |
| PUT | `/api/v1/tms-route/{id}` | TmsRouteController.update | 无 | 管理端 | logistics:route:update |
| GET | `/api/v1/tms-route/courier/{courierId}` | TmsRouteController.listByCourierId | 无 | 管理端 | logistics:route:list-by-courier-id |
| GET | `/api/v1/tms-route/list` | TmsRouteController.pageList | 无 | 管理端 | logistics:route:page |
| GET | `/api/v1/tms-route/no/{routeNo}` | TmsRouteController.getByRouteNo | 无 | 管理端 | logistics:route:get-by-route-no |
| POST | `/api/v1/tms-route` | TmsRouteController.create | 无 | 管理端 | logistics:route:create |
| GET | `/api/v1/tms-tracking/{id}` | TmsTrackingController.getById | 无 | 管理端 | logistics:tracking:view |
| GET | `/api/v1/tms-tracking/list` | TmsTrackingController.pageList | 无 | 管理端 | logistics:tracking:page |
| GET | `/api/v1/tms-tracking/waybill-no/{waybillNo}` | TmsTrackingController.listByWaybillNo | 无 | 管理端 | logistics:tracking:list-by-waybill-no |
| GET | `/api/v1/tms-tracking/waybill/{waybillId}` | TmsTrackingController.listByWaybillId | 无 | 管理端 | logistics:tracking:list-by-waybill-id |
| POST | `/api/v1/tms-tracking` | TmsTrackingController.addTracking | 无 | 管理端 | logistics:tracking:add-tracking |
| POST | `/api/v1/tms-waybill/{id}/cancel` | TmsWaybillController.cancel | 无 | 管理端 | logistics:waybill:cancel |
| PUT | `/api/v1/tms-waybill/{id}/status` | TmsWaybillController.updateStatus | 无 | 管理端 | logistics:waybill:update-status |
| DELETE | `/api/v1/tms-waybill/{id}` | TmsWaybillController.delete | 无 | 管理端 | logistics:waybill:delete |
| GET | `/api/v1/tms-waybill/{id}` | TmsWaybillController.getById | 无 | 管理端 | logistics:waybill:view |
| GET | `/api/v1/tms-waybill/list` | TmsWaybillController.pageList | 无 | 管理端 | logistics:waybill:page |
| GET | `/api/v1/tms-waybill/no/{waybillNo}` | TmsWaybillController.getByWaybillNo | 无 | 管理端 | logistics:waybill:get-by-waybill-no |
| GET | `/api/v1/tms-waybill/order-no/{orderNo}` | TmsWaybillController.listByOrderNo | 无 | 管理端 | logistics:waybill:list-by-order-no |
| GET | `/api/v1/tms-waybill/order/{orderId}` | TmsWaybillController.listByOrderId | 无 | 管理端 | logistics:waybill:list-by-order-id |
| POST | `/api/v1/tms-waybill` | TmsWaybillController.create | 无 | 管理端 | logistics:waybill:create |

### scm-mall（10 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
 | DELETE | `/api/v1/mall/cart/{cartItemId}` | CartController.removeFromCart | 无 | 用户自助 | 校验当前登录人 == userId | 
 | PUT | `/api/v1/mall/cart/{cartItemId}` | CartController.updateQuantity | 无 | 用户自助 | 校验当前登录人 == userId | 
 | DELETE | `/api/v1/mall/cart/clear` | CartController.clearCart | 无 | 用户自助 | 校验当前登录人 == userId | 
 | GET | `/api/v1/mall/cart` | CartController.getCart | 无 | 用户自助 | 校验当前登录人 == userId | 
 | POST | `/api/v1/mall/cart` | CartController.addToCart | 无 | 用户自助 | 校验当前登录人 == userId | 
 | GET | `/api/v1/mall/products/{productId}/reviews` | ReviewController.listReviews | 无 | 用户自助 | 校验当前登录人 == userId | 
 | DELETE | `/api/v1/mall/reviews/{reviewId}` | ReviewController.deleteReview | 无 | 用户自助 | 校验当前登录人 == userId | 
 | POST | `/api/v1/mall/reviews` | ReviewController.createReview | 无 | 用户自助 | 校验当前登录人 == userId | 
 | GET | `/api/v1/mall/sellers/{sellerId}` | SellerController.getSeller | 无 | 用户自助 | 校验当前登录人 == userId | 
 | POST | `/api/v1/mall/sellers` | SellerController.registerSeller | 无 | 用户自助 | 校验当前登录人 == userId | 

### scm-member（14 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
 | PUT | `/api/v1/members/{userId}/addresses/{addressId}/default` | MemberAddressController.setDefault | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
 | DELETE | `/api/v1/members/{userId}/addresses/{addressId}` | MemberAddressController.delete | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
 | PUT | `/api/v1/members/{userId}/addresses/{addressId}` | MemberAddressController.update | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
 | GET | `/api/v1/members/{userId}/addresses` | MemberAddressController.list | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
 | POST | `/api/v1/members/{userId}/addresses` | MemberAddressController.create | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
| PUT | `/api/v1/members/{userId}/level` | MemberController.updateLevel | 无 | 管理端 | member:member:update-level |
 | GET | `/api/v1/members/{userId}` | MemberController.getByUserId | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
 | PUT | `/api/v1/members/{userId}` | MemberController.update | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
| GET | `/api/v1/members/page` | MemberController.page | 无 | 混合（管理端+自助） | member:member:page；有 userId 时校验本人 |
 | POST | `/api/v1/members/register` | MemberController.register | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
| POST | `/api/v1/members/{userId}/points/add` | MemberPointsController.addPoints | 无 | 管理端 | member:points:add-points |
| POST | `/api/v1/members/{userId}/points/deduct` | MemberPointsController.deductPoints | 无 | 管理端 | member:points:deduct-points |
 | GET | `/api/v1/members/{userId}/points/log` | MemberPointsController.getLog | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
 | GET | `/api/v1/members/{userId}/points` | MemberPointsController.getBalance | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 

### scm-notify（27 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| POST | `/sys-notification-audit/{id}/send` | SysNotificationAuditController.sendNotification | 无 | 内部调用 | 内部签名/白名单 |
| DELETE | `/sys-notification-audit/{id}` | SysNotificationAuditController.deleteById | 无 | 管理端 | notify:audit:delete |
| GET | `/sys-notification-audit/{id}` | SysNotificationAuditController.getById | 无 | 管理端 | notify:audit:view |
| GET | `/sys-notification-audit/page` | SysNotificationAuditController.pageQuery | 无 | 混合（管理端+自助） | notify:audit:page；有 userId 时校验本人 |
| GET | `/sys-notification-audit/status/{status}` | SysNotificationAuditController.listByStatus | 无 | 管理端 | notify:audit:list-by-status |
| GET | `/sys-notification-audit/user/{userId}` | SysNotificationAuditController.listByUserId | 无 | 混合（管理端+自助） | notify:audit:list-by-user-id；有 userId 时校验本人 |
| POST | `/sys-notification-audit` | SysNotificationAuditController.create | 无 | 内部调用 | 内部签名/白名单 |
| PUT | `/sys-notification-template/{id}/disable` | SysNotificationTemplateController.disable | 无 | 管理端 | notify:template:disable |
| PUT | `/sys-notification-template/{id}/enable` | SysNotificationTemplateController.enable | 无 | 管理端 | notify:template:enable |
| DELETE | `/sys-notification-template/{id}` | SysNotificationTemplateController.deleteById | 无 | 管理端 | notify:template:delete |
| GET | `/sys-notification-template/{id}` | SysNotificationTemplateController.getById | 无 | 管理端 | notify:template:view |
| GET | `/sys-notification-template/channel/{channel}` | SysNotificationTemplateController.listByChannel | 无 | 管理端 | notify:template:list-by-channel |
| GET | `/sys-notification-template/page` | SysNotificationTemplateController.pageQuery | 无 | 管理端 | notify:template:page |
| POST | `/sys-notification-template` | SysNotificationTemplateController.create | 无 | 管理端 | notify:template:create |
| PUT | `/sys-notification-template` | SysNotificationTemplateController.update | 无 | 管理端 | notify:template:update |
 | PUT | `/sys-user-notification/{id}/read` | SysUserNotificationController.markAsRead | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | DELETE | `/sys-user-notification/{id}` | SysUserNotificationController.delete | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | GET | `/sys-user-notification/page` | SysUserNotificationController.page | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | PUT | `/sys-user-notification/read-all` | SysUserNotificationController.markAllAsRead | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | GET | `/sys-user-notification/unread-count` | SysUserNotificationController.unreadCount | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | DELETE | `/sys-user-notification` | SysUserNotificationController.deleteBatch | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | DELETE | `/sys-user-notification-preference/{id}` | SysUserNotificationPreferenceController.deleteById | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | GET | `/sys-user-notification-preference/{id}` | SysUserNotificationPreferenceController.getById | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | GET | `/sys-user-notification-preference/page` | SysUserNotificationPreferenceController.pageQuery | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | GET | `/sys-user-notification-preference/user/{userId}` | SysUserNotificationPreferenceController.getByUserId | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | POST | `/sys-user-notification-preference` | SysUserNotificationPreferenceController.create | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 
 | PUT | `/sys-user-notification-preference` | SysUserNotificationPreferenceController.update | 无 | 用户自助 | 从登录态取当前用户并校验归属 | 

### scm-order（24 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
 | PUT | `/api/v1/orders/{id}/cancel` | OrdOrderController.cancel | 无 | 用户自助 | 校验订单/支付/退款归属 == 当前登录人 | 
| GET | `/api/v1/orders/{id}/items` | OrdOrderController.getItems | 无 | 管理端 | order:order:get-items |
| PUT | `/api/v1/orders/{id}/status` | OrdOrderController.updateStatus | 无 | 管理端 | order:order:update-status |
| GET | `/api/v1/orders/{id}` | OrdOrderController.getById | 无 | 管理端 | order:order:view |
| GET | `/api/v1/orders/dashboard/summary` | OrdOrderController.dashboardSummary | 无 | 混合（管理端+自助） | order:order:dashboard-summary；有 userId 时校验本人 |
| POST | `/api/v1/orders/query` | OrdOrderController.query | 无 | 混合（管理端+自助） | order:order:query；有 userId 时校验本人 |
 | POST | `/api/v1/orders` | OrdOrderController.createOrder | 无 | 用户自助 | 校验订单/支付/退款归属 == 当前登录人 | 
| GET | `/api/v1/order-items/{id}` | OrdOrderItemController.getById | 无 | 管理端 | order:order-item:view |
| POST | `/api/v1/order-items/query` | OrdOrderItemController.queryByOrderId | 无 | 管理端 | order:order-item:query |
| PUT | `/api/v1/payments/{id}/status` | OrdPaymentController.updateStatus | 无 | 管理端 | order:payment:update-status |
| GET | `/api/v1/payments/{id}` | OrdPaymentController.getById | 无 | 管理端 | order:payment:view |
| GET | `/api/v1/payments/order/{orderId}` | OrdPaymentController.listByOrderId | 无 | 管理端 | order:payment:list-by-order-id |
| POST | `/api/v1/payments/query` | OrdPaymentController.query | 无 | 混合（管理端+自助） | order:payment:query；有 userId 时校验本人 |
 | POST | `/api/v1/payments` | OrdPaymentController.createPayment | 无 | 用户自助 | 校验订单/支付/退款归属 == 当前登录人 | 
| PUT | `/api/v1/refunds/{id}/approve` | OrdRefundController.approve | 无 | 管理端 | order:refund:approve |
| PUT | `/api/v1/refunds/{id}/complete` | OrdRefundController.complete | 无 | 管理端 | order:refund:complete |
| GET | `/api/v1/refunds/{id}/items` | OrdRefundController.listItems | 无 | 管理端 | order:refund:list-items |
| PUT | `/api/v1/refunds/{id}/reject` | OrdRefundController.reject | 无 | 管理端 | order:refund:reject |
| GET | `/api/v1/refunds/{id}` | OrdRefundController.getById | 无 | 管理端 | order:refund:view |
| GET | `/api/v1/refunds/order/{orderId}` | OrdRefundController.listByOrderId | 无 | 管理端 | order:refund:list-by-order-id |
| POST | `/api/v1/refunds/query` | OrdRefundController.query | 无 | 混合（管理端+自助） | order:refund:query；有 userId 时校验本人 |
 | POST | `/api/v1/refunds` | OrdRefundController.createRefund | 无 | 用户自助 | 校验订单/支付/退款归属 == 当前登录人 | 
| GET | `/api/v1/status-history/{id}` | OrdStatusHistoryController.getById | 无 | 管理端 | order:status-history:view |
| GET | `/api/v1/status-history/order/{orderId}` | OrdStatusHistoryController.listByOrderId | 无 | 管理端 | order:status-history:list-by-order-id |

### scm-order-center（8 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| POST | `/api/v1/orders/{orderNo}/cancel` | OrderCenterController.cancelOrder | 无 | 管理端 | order-center:order:cancel-order |
| POST | `/api/v1/orders/{orderNo}/confirm` | OrderCenterController.confirmOrder | 无 | 管理端 | order-center:order:confirm-order |
| POST | `/api/v1/orders/{orderNo}/deliver` | OrderCenterController.deliverOrder | 无 | 管理端 | order-center:order:deliver-order |
| POST | `/api/v1/orders/{orderNo}/pay` | OrderCenterController.payOrder | 无 | 管理端 | order-center:order:pay-order |
| POST | `/api/v1/orders/{orderNo}/ship` | OrderCenterController.shipOrder | 无 | 管理端 | order-center:order:ship-order |
| GET | `/api/v1/orders/{orderNo}` | OrderCenterController.getOrder | 无 | 管理端 | order-center:order:get-order |
| GET | `/api/v1/orders/page` | OrderCenterController.page | 无 | 混合（管理端+自助） | order-center:order:page；有 userId 时校验本人 |
| POST | `/api/v1/orders` | OrderCenterController.createOrder | 无 | 管理端 | order-center:order:create-order |

### scm-payment（11 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
 | POST | `/api/v1/payments/{paymentNo}/cancel` | PaymentController.cancelPayment | 无 | 用户自助 | 校验订单/支付/退款归属 == 当前登录人 | 
 | GET | `/api/v1/payments/{paymentNo}` | PaymentController.queryPayment | 无 | 用户自助 | 校验订单/支付/退款归属 == 当前登录人 | 
| POST | `/api/v1/payments/callback/alipay` | PaymentController.alipayCallback | 无 | 公开 | 公开白名单 |
| POST | `/api/v1/payments/callback/wechat` | PaymentController.wechatCallback | 无 | 公开 | 公开白名单 |
 | POST | `/api/v1/payments/create` | PaymentController.createPayment | 无 | 用户自助 | 校验订单/支付/退款归属 == 当前登录人 | 
| GET | `/api/v1/payments/page` | PaymentController.page | 无 | 混合（管理端+自助） | payment:payment:page；有 userId 时校验本人 |
| POST | `/api/v1/payments/refunds/{refundNo}/approve` | RefundController.approveRefund | 无 | 管理端 | payment:refund:approve-refund |
| POST | `/api/v1/payments/refunds/{refundNo}/reject` | RefundController.rejectRefund | 无 | 管理端 | payment:refund:reject-refund |
 | GET | `/api/v1/payments/refunds/{refundNo}` | RefundController.queryRefund | 无 | 用户自助 | 校验订单/支付/退款归属 == 当前登录人 | 
| GET | `/api/v1/payments/refunds/page` | RefundController.page | 无 | 混合（管理端+自助） | payment:refund:page；有 userId 时校验本人 |
 | POST | `/api/v1/payments/refunds` | RefundController.createRefund | 无 | 用户自助 | 校验订单/支付/退款归属 == 当前登录人 | 

### scm-product（40 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| DELETE | `/api/v1/prod-attribute-template/{id}` | ProdAttributeTemplateController.delete | 无 | 管理端 | product:attribute:delete |
| GET | `/api/v1/prod-attribute-template/{id}` | ProdAttributeTemplateController.getById | 无 | 管理端 | product:attribute:view |
| PUT | `/api/v1/prod-attribute-template/{id}` | ProdAttributeTemplateController.update | 无 | 管理端 | product:attribute:update |
| GET | `/api/v1/prod-attribute-template/category/{categoryId}` | ProdAttributeTemplateController.listByCategoryId | 无 | 管理端 | product:attribute:list-by-category-id |
| GET | `/api/v1/prod-attribute-template` | ProdAttributeTemplateController.list | 无 | 管理端 | product:attribute:list |
| POST | `/api/v1/prod-attribute-template` | ProdAttributeTemplateController.create | 无 | 管理端 | product:attribute:create |
| DELETE | `/api/v1/prod-brand/{id}` | ProdBrandController.delete | 无 | 管理端 | product:brand:delete |
| GET | `/api/v1/prod-brand/{id}` | ProdBrandController.getById | 无 | 管理端 | product:brand:view |
| PUT | `/api/v1/prod-brand/{id}` | ProdBrandController.update | 无 | 管理端 | product:brand:update |
| GET | `/api/v1/prod-brand/featured` | ProdBrandController.listFeatured | 无 | 管理端 | product:brand:list-featured |
| GET | `/api/v1/prod-brand/search` | ProdBrandController.search | 无 | 管理端 | product:brand:search |
| GET | `/api/v1/prod-brand` | ProdBrandController.list | 无 | 管理端 | product:brand:list |
| POST | `/api/v1/prod-brand` | ProdBrandController.create | 无 | 管理端 | product:brand:create |
| DELETE | `/api/v1/prod-category/{id}` | ProdCategoryController.delete | 无 | 管理端 | product:category:delete |
| GET | `/api/v1/prod-category/{id}` | ProdCategoryController.getById | 无 | 管理端 | product:category:view |
| PUT | `/api/v1/prod-category/{id}` | ProdCategoryController.update | 无 | 管理端 | product:category:update |
| GET | `/api/v1/prod-category/children/{parentId}` | ProdCategoryController.listByParentId | 无 | 管理端 | product:category:list-by-parent-id |
| GET | `/api/v1/prod-category/search` | ProdCategoryController.search | 无 | 管理端 | product:category:search |
| GET | `/api/v1/prod-category/tree` | ProdCategoryController.getCategoryTree | 无 | 管理端 | product:category:get-category-tree |
| GET | `/api/v1/prod-category` | ProdCategoryController.list | 无 | 管理端 | product:category:list |
| POST | `/api/v1/prod-category` | ProdCategoryController.create | 无 | 管理端 | product:category:create |
| DELETE | `/api/v1/prod-sku/{id}` | ProdSkuController.delete | 无 | 管理端 | product:sku:delete |
| GET | `/api/v1/prod-sku/{id}` | ProdSkuController.getById | 无 | 管理端 | product:sku:view |
| PUT | `/api/v1/prod-sku/{id}` | ProdSkuController.update | 无 | 管理端 | product:sku:update |
| GET | `/api/v1/prod-sku/spu/{spuId}` | ProdSkuController.listBySpuId | 无 | 管理端 | product:sku:list-by-spu-id |
| GET | `/api/v1/prod-sku` | ProdSkuController.list | 无 | 管理端 | product:sku:list |
| POST | `/api/v1/prod-sku` | ProdSkuController.create | 无 | 管理端 | product:sku:create |
| DELETE | `/api/v1/prod-spu/{id}` | ProdSpuController.delete | 无 | 管理端 | product:spu:delete |
| GET | `/api/v1/prod-spu/{id}` | ProdSpuController.getById | 无 | 管理端 | product:spu:view |
| PUT | `/api/v1/prod-spu/{id}` | ProdSpuController.update | 无 | 管理端 | product:spu:update |
| GET | `/api/v1/prod-spu/brand/{brandId}` | ProdSpuController.listByBrandId | 无 | 管理端 | product:spu:list-by-brand-id |
| GET | `/api/v1/prod-spu/category/{categoryId}` | ProdSpuController.listByCategoryId | 无 | 管理端 | product:spu:list-by-category-id |
| GET | `/api/v1/prod-spu` | ProdSpuController.list | 无 | 管理端 | product:spu:list |
| POST | `/api/v1/prod-spu` | ProdSpuController.create | 无 | 管理端 | product:spu:create |
| GET | `/api/v1/products/search/brand/{brandId}` | ProductSearchController.findByBrand | 无 | 管理端 | product:search:find-by-brand |
| GET | `/api/v1/products/search/category/{categoryId}` | ProductSearchController.findByCategory | 无 | 管理端 | product:search:find-by-category |
| GET | `/api/v1/products/search/hot` | ProductSearchController.getHotProducts | 无 | 管理端 | product:search:get-hot-products |
| GET | `/api/v1/products/search/latest` | ProductSearchController.getLatestProducts | 无 | 管理端 | product:search:get-latest-products |
| GET | `/api/v1/products/search` | ProductSearchController.quickSearch | 无 | 管理端 | product:search:quick-search |
| POST | `/api/v1/products/search` | ProductSearchController.search | 无 | 管理端 | product:search:search |

### scm-promotion（12 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| POST | `/api/v1/promotions/coupons/issue` | CouponController.issueCoupon | 无 | 管理端 | promotion:coupon:issue-coupon |
| GET | `/api/v1/promotions/coupons/page` | CouponController.page | 无 | 混合（管理端+自助） | promotion:coupon:page；有 userId 时校验本人 |
 | POST | `/api/v1/promotions/coupons/use` | CouponController.useCoupon | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 
 | GET | `/api/v1/promotions/coupons/user/{userId}` | CouponController.getByUserId | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 
 | POST | `/api/v1/promotions/coupons/verify` | CouponController.verifyCoupon | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 
 | GET | `/api/v1/promotions/flash-sales/active` | FlashSaleController.getActiveFlashSales | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 
 | POST | `/api/v1/promotions/flash-sales/check-stock` | FlashSaleController.checkStock | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 
 | POST | `/api/v1/promotions/flash-sales/purchase` | FlashSaleController.purchase | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 
 | POST | `/api/v1/promotions/groups/{groupBuyId}/join` | GroupBuyController.joinGroup | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 
 | GET | `/api/v1/promotions/groups/{groupBuyId}/members` | GroupBuyController.listMembers | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 
 | GET | `/api/v1/promotions/groups/{groupBuyId}` | GroupBuyController.getGroup | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 
 | POST | `/api/v1/promotions/groups` | GroupBuyController.createGroup | 无 | 用户自助 | 校验 userId / body.userId == 当前登录人 | 

### scm-purchase（115 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| POST | `/pur-contract/{id}/sign` | PurContractController.sign | 无 | 管理端 | purchase:contract:sign |
| POST | `/pur-contract/{id}/terminate` | PurContractController.terminate | 无 | 管理端 | purchase:contract:terminate |
| DELETE | `/pur-contract/{id}` | PurContractController.delete | 无 | 管理端 | purchase:contract:delete |
| GET | `/pur-contract/{id}` | PurContractController.getById | 无 | 管理端 | purchase:contract:view |
| GET | `/pur-contract/list` | PurContractController.listByStatus | 无 | 管理端 | purchase:contract:list-by-status |
| GET | `/pur-contract/no/{contractNo}` | PurContractController.getByContractNo | 无 | 管理端 | purchase:contract:get-by-contract-no |
| GET | `/pur-contract/page` | PurContractController.page | 无 | 管理端 | purchase:contract:page |
| GET | `/pur-contract/supplier/{supplierId}` | PurContractController.listBySupplierId | 无 | 管理端 | purchase:contract:list-by-supplier-id |
| POST | `/pur-contract` | PurContractController.save | 无 | 管理端 | purchase:contract:create |
| PUT | `/pur-contract` | PurContractController.update | 无 | 管理端 | purchase:contract:update |
| GET | `/api/v1/pur-order/{id}/items` | PurOrderController.listItems | 无 | 管理端 | purchase:order:list-items |
| GET | `/api/v1/pur-order/{id}` | PurOrderController.getById | 无 | 管理端 | purchase:order:view |
| GET | `/api/v1/pur-order/page` | PurOrderController.page | 无 | 管理端 | purchase:order:page |
| DELETE | `/pur-order-item/{id}` | PurOrderItemController.delete | 无 | 管理端 | purchase:order-item:delete |
| GET | `/pur-order-item/{id}` | PurOrderItemController.getById | 无 | 管理端 | purchase:order-item:view |
| GET | `/pur-order-item/list/{orderId}` | PurOrderItemController.listByOrderId | 无 | 管理端 | purchase:order-item:list-by-order-id |
| DELETE | `/pur-order-item/order/{orderId}` | PurOrderItemController.deleteByOrderId | 无 | 管理端 | purchase:order-item:delete-by-order-id |
| POST | `/pur-order-item` | PurOrderItemController.save | 无 | 管理端 | purchase:order-item:create |
| PUT | `/pur-order-item` | PurOrderItemController.update | 无 | 管理端 | purchase:order-item:update |
| POST | `/pur-plan/{id}/approve` | PurPlanController.approve | 无 | 管理端 | purchase:plan:approve |
| POST | `/pur-plan/{id}/complete` | PurPlanController.complete | 无 | 管理端 | purchase:plan:complete |
| POST | `/pur-plan/{id}/submit` | PurPlanController.submit | 无 | 管理端 | purchase:plan:submit |
| DELETE | `/pur-plan/{id}` | PurPlanController.delete | 无 | 管理端 | purchase:plan:delete |
| GET | `/pur-plan/{id}` | PurPlanController.getById | 无 | 管理端 | purchase:plan:view |
| GET | `/pur-plan/list` | PurPlanController.listByStatus | 无 | 管理端 | purchase:plan:list-by-status |
| GET | `/pur-plan/no/{planNo}` | PurPlanController.getByPlanNo | 无 | 管理端 | purchase:plan:get-by-plan-no |
| GET | `/pur-plan/page` | PurPlanController.page | 无 | 管理端 | purchase:plan:page |
| POST | `/pur-plan` | PurPlanController.save | 无 | 管理端 | purchase:plan:create |
| PUT | `/pur-plan` | PurPlanController.update | 无 | 管理端 | purchase:plan:update |
| DELETE | `/pur-plan-item/{id}` | PurPlanItemController.delete | 无 | 管理端 | purchase:plan-item:delete |
| GET | `/pur-plan-item/{id}` | PurPlanItemController.getById | 无 | 管理端 | purchase:plan-item:view |
| GET | `/pur-plan-item/list/{planId}` | PurPlanItemController.listByPlanId | 无 | 管理端 | purchase:plan-item:list-by-plan-id |
| DELETE | `/pur-plan-item/plan/{planId}` | PurPlanItemController.deleteByPlanId | 无 | 管理端 | purchase:plan-item:delete-by-plan-id |
| POST | `/pur-plan-item` | PurPlanItemController.save | 无 | 管理端 | purchase:plan-item:create |
| PUT | `/pur-plan-item` | PurPlanItemController.update | 无 | 管理端 | purchase:plan-item:update |
| POST | `/pur-price-comparison/{id}/approve` | PurPriceComparisonController.approve | 无 | 管理端 | purchase:comparison:approve |
| DELETE | `/pur-price-comparison/{id}` | PurPriceComparisonController.delete | 无 | 管理端 | purchase:comparison:delete |
| GET | `/pur-price-comparison/{id}` | PurPriceComparisonController.getById | 无 | 管理端 | purchase:comparison:view |
| GET | `/pur-price-comparison/no/{comparisonNo}` | PurPriceComparisonController.getByComparisonNo | 无 | 管理端 | purchase:comparison:get-by-comparison-no |
| GET | `/pur-price-comparison/page` | PurPriceComparisonController.page | 无 | 管理端 | purchase:comparison:page |
| GET | `/pur-price-comparison/rfq/{rfqId}` | PurPriceComparisonController.listByRfqId | 无 | 管理端 | purchase:comparison:list-by-rfq-id |
| POST | `/pur-price-comparison` | PurPriceComparisonController.save | 无 | 管理端 | purchase:comparison:create |
| PUT | `/pur-price-comparison` | PurPriceComparisonController.update | 无 | 管理端 | purchase:comparison:update |
| DELETE | `/pur-price-comparison-item/{id}` | PurPriceComparisonItemController.delete | 无 | 管理端 | purchase:comparison-item:delete |
| GET | `/pur-price-comparison-item/{id}` | PurPriceComparisonItemController.getById | 无 | 管理端 | purchase:comparison-item:view |
| DELETE | `/pur-price-comparison-item/comparison/{comparisonId}` | PurPriceComparisonItemController.deleteByComparisonId | 无 | 管理端 | purchase:comparison-item:delete-by-comparison-id |
| GET | `/pur-price-comparison-item/list/{comparisonId}` | PurPriceComparisonItemController.listByComparisonId | 无 | 管理端 | purchase:comparison-item:list-by-comparison-id |
| POST | `/pur-price-comparison-item` | PurPriceComparisonItemController.save | 无 | 管理端 | purchase:comparison-item:create |
| PUT | `/pur-price-comparison-item` | PurPriceComparisonItemController.update | 无 | 管理端 | purchase:comparison-item:update |
| POST | `/pur-quotation/{id}/select` | PurQuotationController.select | 无 | 管理端 | purchase:quotation:select |
| POST | `/pur-quotation/{id}/submit` | PurQuotationController.submit | 无 | 管理端 | purchase:quotation:submit |
| DELETE | `/pur-quotation/{id}` | PurQuotationController.delete | 无 | 管理端 | purchase:quotation:delete |
| GET | `/pur-quotation/{id}` | PurQuotationController.getById | 无 | 管理端 | purchase:quotation:view |
| GET | `/pur-quotation/no/{quotationNo}` | PurQuotationController.getByQuotationNo | 无 | 管理端 | purchase:quotation:get-by-quotation-no |
| GET | `/pur-quotation/page` | PurQuotationController.page | 无 | 管理端 | purchase:quotation:page |
| GET | `/pur-quotation/rfq/{rfqId}` | PurQuotationController.listByRfqId | 无 | 管理端 | purchase:quotation:list-by-rfq-id |
| GET | `/pur-quotation/supplier/{supplierId}` | PurQuotationController.listBySupplierId | 无 | 管理端 | purchase:quotation:list-by-supplier-id |
| POST | `/pur-quotation` | PurQuotationController.save | 无 | 管理端 | purchase:quotation:create |
| PUT | `/pur-quotation` | PurQuotationController.update | 无 | 管理端 | purchase:quotation:update |
| DELETE | `/pur-quotation-item/{id}` | PurQuotationItemController.delete | 无 | 管理端 | purchase:quotation-item:delete |
| GET | `/pur-quotation-item/{id}` | PurQuotationItemController.getById | 无 | 管理端 | purchase:quotation-item:view |
| GET | `/pur-quotation-item/list/{quotationId}` | PurQuotationItemController.listByQuotationId | 无 | 管理端 | purchase:quotation-item:list-by-quotation-id |
| DELETE | `/pur-quotation-item/quotation/{quotationId}` | PurQuotationItemController.deleteByQuotationId | 无 | 管理端 | purchase:quotation-item:delete-by-quotation-id |
| POST | `/pur-quotation-item` | PurQuotationItemController.save | 无 | 管理端 | purchase:quotation-item:create |
| PUT | `/pur-quotation-item` | PurQuotationItemController.update | 无 | 管理端 | purchase:quotation-item:update |
| POST | `/pur-receipt/{id}/quality-inspect` | PurReceiptController.qualityInspect | 无 | 管理端 | purchase:receipt:quality-inspect |
| POST | `/pur-receipt/{id}/receive` | PurReceiptController.receive | 无 | 管理端 | purchase:receipt:receive |
| POST | `/pur-receipt/{id}/shelve` | PurReceiptController.shelve | 无 | 管理端 | purchase:receipt:shelve |
| DELETE | `/pur-receipt/{id}` | PurReceiptController.delete | 无 | 管理端 | purchase:receipt:delete |
| GET | `/pur-receipt/{id}` | PurReceiptController.getById | 无 | 管理端 | purchase:receipt:view |
| GET | `/pur-receipt/no/{receiptNo}` | PurReceiptController.getByReceiptNo | 无 | 管理端 | purchase:receipt:get-by-receipt-no |
| GET | `/pur-receipt/order/{orderId}` | PurReceiptController.listByOrderId | 无 | 管理端 | purchase:receipt:list-by-order-id |
| GET | `/pur-receipt/page` | PurReceiptController.page | 无 | 管理端 | purchase:receipt:page |
| POST | `/pur-receipt` | PurReceiptController.save | 无 | 管理端 | purchase:receipt:create |
| PUT | `/pur-receipt` | PurReceiptController.update | 无 | 管理端 | purchase:receipt:update |
| DELETE | `/pur-receipt-item/{id}` | PurReceiptItemController.delete | 无 | 管理端 | purchase:receipt-item:delete |
| GET | `/pur-receipt-item/{id}` | PurReceiptItemController.getById | 无 | 管理端 | purchase:receipt-item:view |
| GET | `/pur-receipt-item/list/{receiptId}` | PurReceiptItemController.listByReceiptId | 无 | 管理端 | purchase:receipt-item:list-by-receipt-id |
| DELETE | `/pur-receipt-item/receipt/{receiptId}` | PurReceiptItemController.deleteByReceiptId | 无 | 管理端 | purchase:receipt-item:delete-by-receipt-id |
| POST | `/pur-receipt-item` | PurReceiptItemController.save | 无 | 管理端 | purchase:receipt-item:create |
| PUT | `/pur-receipt-item` | PurReceiptItemController.update | 无 | 管理端 | purchase:receipt-item:update |
| POST | `/pur-request/{id}/approve` | PurRequestController.approve | 无 | 管理端 | purchase:request:approve |
| POST | `/pur-request/{id}/close` | PurRequestController.close | 无 | 管理端 | purchase:request:close |
| POST | `/pur-request/{id}/convert` | PurRequestController.convert | 无 | 管理端 | purchase:request:convert |
| GET | `/pur-request/{id}/detail` | PurRequestController.detail | 无 | 管理端 | purchase:request:view-detail |
| PUT | `/pur-request/{id}/full` | PurRequestController.update | 无 | 管理端 | purchase:request:update |
| POST | `/pur-request/{id}/reject` | PurRequestController.reject | 无 | 管理端 | purchase:request:reject |
| POST | `/pur-request/{id}/submit` | PurRequestController.submit | 无 | 管理端 | purchase:request:submit |
| DELETE | `/pur-request/{id}` | PurRequestController.delete | 无 | 管理端 | purchase:request:delete |
| GET | `/pur-request/{id}` | PurRequestController.getById | 无 | 管理端 | purchase:request:view |
| POST | `/pur-request/full` | PurRequestController.create | 无 | 管理端 | purchase:request:create |
| GET | `/pur-request/list` | PurRequestController.listByStatus | 无 | 管理端 | purchase:request:list-by-status |
| GET | `/pur-request/no/{requestNo}` | PurRequestController.getByRequestNo | 无 | 管理端 | purchase:request:get-by-request-no |
| GET | `/pur-request/page` | PurRequestController.page | 无 | 管理端 | purchase:request:page |
| DELETE | `/pur-request-item/{id}` | PurRequestItemController.delete | 无 | 管理端 | purchase:request-item:delete |
| GET | `/pur-request-item/{id}` | PurRequestItemController.getById | 无 | 管理端 | purchase:request-item:view |
| GET | `/pur-request-item/list/{requestId}` | PurRequestItemController.listByRequestId | 无 | 管理端 | purchase:request-item:list-by-request-id |
| DELETE | `/pur-request-item/request/{requestId}` | PurRequestItemController.deleteByRequestId | 无 | 管理端 | purchase:request-item:delete-by-request-id |
| POST | `/pur-request-item` | PurRequestItemController.save | 无 | 管理端 | purchase:request-item:create |
| PUT | `/pur-request-item` | PurRequestItemController.update | 无 | 管理端 | purchase:request-item:update |
| POST | `/pur-rfq/{id}/close` | PurRfqController.close | 无 | 管理端 | purchase:rfq:close |
| POST | `/pur-rfq/{id}/publish` | PurRfqController.publish | 无 | 管理端 | purchase:rfq:publish |
| DELETE | `/pur-rfq/{id}` | PurRfqController.delete | 无 | 管理端 | purchase:rfq:delete |
| GET | `/pur-rfq/{id}` | PurRfqController.getById | 无 | 管理端 | purchase:rfq:view |
| GET | `/pur-rfq/list` | PurRfqController.listByStatus | 无 | 管理端 | purchase:rfq:list-by-status |
| GET | `/pur-rfq/no/{rfqNo}` | PurRfqController.getByRfqNo | 无 | 管理端 | purchase:rfq:get-by-rfq-no |
| GET | `/pur-rfq/page` | PurRfqController.page | 无 | 管理端 | purchase:rfq:page |
| POST | `/pur-rfq` | PurRfqController.save | 无 | 管理端 | purchase:rfq:create |
| PUT | `/pur-rfq` | PurRfqController.update | 无 | 管理端 | purchase:rfq:update |
| DELETE | `/pur-rfq-item/{id}` | PurRfqItemController.delete | 无 | 管理端 | purchase:rfq-item:delete |
| GET | `/pur-rfq-item/{id}` | PurRfqItemController.getById | 无 | 管理端 | purchase:rfq-item:view |
| GET | `/pur-rfq-item/list/{rfqId}` | PurRfqItemController.listByRfqId | 无 | 管理端 | purchase:rfq-item:list-by-rfq-id |
| DELETE | `/pur-rfq-item/rfq/{rfqId}` | PurRfqItemController.deleteByRfqId | 无 | 管理端 | purchase:rfq-item:delete-by-rfq-id |
| POST | `/pur-rfq-item` | PurRfqItemController.save | 无 | 管理端 | purchase:rfq-item:create |
| PUT | `/pur-rfq-item` | PurRfqItemController.update | 无 | 管理端 | purchase:rfq-item:update |

### scm-search（8 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| DELETE | `/api/v1/search/index/products/{productId}` | IndexController.deleteProduct | 无 | 内部调用 | 内部签名/白名单 |
| POST | `/api/v1/search/index/products` | IndexController.bulkIndex | 无 | 内部调用 | 内部签名/白名单 |
| POST | `/api/v1/search/index/reindex` | IndexController.reindexAll | 无 | 内部调用 | 内部签名/白名单 |
| DELETE | `/api/v1/search/index/product/{productId}` | SearchController.removeProduct | 无 | 内部调用 | 内部签名/白名单 |
| POST | `/api/v1/search/index/product` | SearchController.indexProduct | 无 | 内部调用 | 内部签名/白名单 |
 | GET | `/api/v1/search/products/{productId}/similar` | SearchController.getSimilarProducts | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
 | GET | `/api/v1/search/products/personalized/{userId}` | SearchController.getPersonalizedResults | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 
 | GET | `/api/v1/search/products` | SearchController.searchProducts | 无 | 用户自助 | 校验路径 userId == 当前登录人 | 

### scm-supplier（62 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| DELETE | `/api/v1/purchase-order-items/{id}` | SupPurchaseOrderItemController.delete | 无 | 管理端 | supplier:purchase-order-item:delete |
| GET | `/api/v1/purchase-order-items/{id}` | SupPurchaseOrderItemController.getById | 无 | 管理端 | supplier:purchase-order-item:view |
| PUT | `/api/v1/purchase-order-items/{id}` | SupPurchaseOrderItemController.update | 无 | 管理端 | supplier:purchase-order-item:update |
| GET | `/api/v1/purchase-order-items/purchase/{purchaseId}` | SupPurchaseOrderItemController.listByPurchaseId | 无 | 管理端 | supplier:purchase-order-item:list-by-purchase-id |
| GET | `/api/v1/purchase-order-items` | SupPurchaseOrderItemController.pageList | 无 | 管理端 | supplier:purchase-order-item:page |
| POST | `/api/v1/purchase-order-items` | SupPurchaseOrderItemController.create | 无 | 管理端 | supplier:purchase-order-item:create |
| PUT | `/api/v1/settlements/{id}/confirm` | SupSettlementController.confirm | 无 | 管理端 | supplier:settlement:confirm |
| PUT | `/api/v1/settlements/{id}/pay` | SupSettlementController.markAsPaid | 无 | 管理端 | supplier:settlement:mark-as-paid |
| DELETE | `/api/v1/settlements/{id}` | SupSettlementController.delete | 无 | 管理端 | supplier:settlement:delete |
| GET | `/api/v1/settlements/{id}` | SupSettlementController.getById | 无 | 管理端 | supplier:settlement:view |
| PUT | `/api/v1/settlements/{id}` | SupSettlementController.update | 无 | 管理端 | supplier:settlement:update |
| GET | `/api/v1/settlements/supplier/{supplierId}` | SupSettlementController.listBySupplierId | 无 | 管理端 | supplier:settlement:list-by-supplier-id |
| GET | `/api/v1/settlements` | SupSettlementController.pageList | 无 | 管理端 | supplier:settlement:page |
| POST | `/api/v1/settlements` | SupSettlementController.create | 无 | 管理端 | supplier:settlement:create |
| PUT | `/api/v1/suppliers/{supplierId}/bank-accounts/{id}/default` | SupSupplierBankAccountController.setDefault | 无 | 管理端 | supplier:bank-account:set-default |
| DELETE | `/api/v1/suppliers/{supplierId}/bank-accounts/{id}` | SupSupplierBankAccountController.delete | 无 | 管理端 | supplier:bank-account:delete |
| PUT | `/api/v1/suppliers/{supplierId}/bank-accounts/{id}` | SupSupplierBankAccountController.update | 无 | 管理端 | supplier:bank-account:update |
| GET | `/api/v1/suppliers/{supplierId}/bank-accounts` | SupSupplierBankAccountController.list | 无 | 管理端 | supplier:bank-account:list |
| POST | `/api/v1/suppliers/{supplierId}/bank-accounts` | SupSupplierBankAccountController.create | 无 | 管理端 | supplier:bank-account:create |
| DELETE | `/api/v1/supplier-categories/{id}` | SupSupplierCategoryController.delete | 无 | 管理端 | supplier:category:delete |
| PUT | `/api/v1/supplier-categories/{id}` | SupSupplierCategoryController.update | 无 | 管理端 | supplier:category:update |
| GET | `/api/v1/supplier-categories` | SupSupplierCategoryController.list | 无 | 管理端 | supplier:category:list |
| POST | `/api/v1/supplier-categories` | SupSupplierCategoryController.create | 无 | 管理端 | supplier:category:create |
| PUT | `/api/v1/suppliers/{supplierId}/certificates/{id}/verify` | SupSupplierCertificateController.verify | 无 | 管理端 | supplier:certificate:verify |
| DELETE | `/api/v1/suppliers/{supplierId}/certificates/{id}` | SupSupplierCertificateController.delete | 无 | 管理端 | supplier:certificate:delete |
| PUT | `/api/v1/suppliers/{supplierId}/certificates/{id}` | SupSupplierCertificateController.update | 无 | 管理端 | supplier:certificate:update |
| GET | `/api/v1/suppliers/{supplierId}/certificates` | SupSupplierCertificateController.list | 无 | 管理端 | supplier:certificate:list |
| POST | `/api/v1/suppliers/{supplierId}/certificates` | SupSupplierCertificateController.create | 无 | 管理端 | supplier:certificate:create |
| GET | `/api/v1/supplier-certificates/expiring` | SupSupplierCertificateExpiryController.expiring | 无 | 管理端 | supplier:certificate:expiring |
| PUT | `/api/v1/suppliers/{supplierId}/contacts/{id}/primary` | SupSupplierContactController.setPrimary | 无 | 管理端 | supplier:contact:set-primary |
| DELETE | `/api/v1/suppliers/{supplierId}/contacts/{id}` | SupSupplierContactController.delete | 无 | 管理端 | supplier:contact:delete |
| PUT | `/api/v1/suppliers/{supplierId}/contacts/{id}` | SupSupplierContactController.update | 无 | 管理端 | supplier:contact:update |
| GET | `/api/v1/suppliers/{supplierId}/contacts` | SupSupplierContactController.list | 无 | 管理端 | supplier:contact:list |
| POST | `/api/v1/suppliers/{supplierId}/contacts` | SupSupplierContactController.create | 无 | 管理端 | supplier:contact:create |
| PUT | `/api/v1/suppliers/{id}/audit` | SupSupplierController.audit | 无 | 管理端 | supplier:supplier:audit |
| PUT | `/api/v1/suppliers/{id}/blacklist` | SupSupplierController.blacklist | 无 | 管理端 | supplier:supplier:blacklist |
| GET | `/api/v1/suppliers/{id}/categories` | SupSupplierController.getCategories | 无 | 管理端 | supplier:supplier:get-categories |
| PUT | `/api/v1/suppliers/{id}/categories` | SupSupplierController.assignCategories | 无 | 管理端 | supplier:supplier:assign-categories |
| GET | `/api/v1/suppliers/{id}/change-logs` | SupSupplierController.listChangeLogs | 无 | 管理端 | supplier:supplier:list-change-logs |
| PUT | `/api/v1/suppliers/{id}/cooperation-status` | SupSupplierController.changeCooperationStatus | 无 | 管理端 | supplier:supplier:change-cooperation-status |
| PUT | `/api/v1/suppliers/{id}/disable` | SupSupplierController.disable | 无 | 管理端 | supplier:supplier:disable |
| PUT | `/api/v1/suppliers/{id}/enable` | SupSupplierController.enable | 无 | 管理端 | supplier:supplier:enable |
| GET | `/api/v1/suppliers/{id}/performance` | SupSupplierController.getPerformance | 无 | 管理端 | supplier:supplier:get-performance |
| GET | `/api/v1/suppliers/{id}/profile` | SupSupplierController.getProfile | 无 | 管理端 | supplier:supplier:get-profile |
| PUT | `/api/v1/suppliers/{id}/risk-level` | SupSupplierController.changeRiskLevel | 无 | 管理端 | supplier:supplier:change-risk-level |
| POST | `/api/v1/suppliers/{id}/submit-audit` | SupSupplierController.submitAudit | 无 | 管理端 | supplier:supplier:submit-audit |
| PUT | `/api/v1/suppliers/{id}/unblacklist` | SupSupplierController.unblacklist | 无 | 管理端 | supplier:supplier:unblacklist |
| DELETE | `/api/v1/suppliers/{id}` | SupSupplierController.delete | 无 | 管理端 | supplier:supplier:delete |
| GET | `/api/v1/suppliers/{id}` | SupSupplierController.getById | 无 | 管理端 | supplier:supplier:view |
| PUT | `/api/v1/suppliers/{id}` | SupSupplierController.update | 无 | 管理端 | supplier:supplier:update |
| GET | `/api/v1/suppliers/active` | SupSupplierController.listActive | 无 | 管理端 | supplier:supplier:list-active |
| GET | `/api/v1/suppliers/search` | SupSupplierController.searchByName | 无 | 管理端 | supplier:supplier:search-by-name |
| GET | `/api/v1/suppliers/stats` | SupSupplierController.stats | 无 | 管理端 | supplier:supplier:stats |
| GET | `/api/v1/suppliers` | SupSupplierController.pageList | 无 | 管理端 | supplier:supplier:page |
| POST | `/api/v1/suppliers` | SupSupplierController.create | 无 | 管理端 | supplier:supplier:create |
| DELETE | `/api/v1/supplier-evaluations/{id}` | SupSupplierEvaluationController.delete | 无 | 管理端 | supplier:evaluation:delete |
| GET | `/api/v1/supplier-evaluations/{id}` | SupSupplierEvaluationController.getById | 无 | 管理端 | supplier:evaluation:view |
| PUT | `/api/v1/supplier-evaluations/{id}` | SupSupplierEvaluationController.update | 无 | 管理端 | supplier:evaluation:update |
| GET | `/api/v1/supplier-evaluations/supplier/{supplierId}/average-score` | SupSupplierEvaluationController.getAverageScore | 无 | 管理端 | supplier:evaluation:get-average-score |
| GET | `/api/v1/supplier-evaluations/supplier/{supplierId}` | SupSupplierEvaluationController.listBySupplierId | 无 | 管理端 | supplier:evaluation:list-by-supplier-id |
| GET | `/api/v1/supplier-evaluations` | SupSupplierEvaluationController.pageList | 无 | 管理端 | supplier:evaluation:page |
| POST | `/api/v1/supplier-evaluations` | SupSupplierEvaluationController.create | 无 | 管理端 | supplier:evaluation:create |

### scm-system（68 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| GET | `/api/v1/decision/rules/{engineType}` | DecisionRuleController.list | 无 | 管理端 | system:decision-rule:list |
| PUT | `/api/v1/decision/rules/{id}/toggle` | DecisionRuleController.toggle | 无 | 管理端 | system:decision-rule:toggle |
| DELETE | `/api/v1/decision/rules/{id}` | DecisionRuleController.delete | 无 | 管理端 | system:decision-rule:delete |
| PUT | `/api/v1/decision/rules/{id}` | DecisionRuleController.update | 无 | 管理端 | system:decision-rule:update |
| POST | `/api/v1/decision/rules/conflicts/{engineType}` | DecisionRuleController.detectConflicts | 无 | 管理端 | system:decision-rule:detect-conflicts |
| POST | `/api/v1/decision/rules` | DecisionRuleController.create | 无 | 管理端 | system:decision-rule:create |
| GET | `/api/v1/decision/weights/{engineType}/versions` | DecisionWeightController.getVersions | 无 | 管理端 | system:decision-weight:get-versions |
| GET | `/api/v1/decision/weights/{engineType}` | DecisionWeightController.getActive | 无 | 管理端 | system:decision-weight:get-active |
| PUT | `/api/v1/decision/weights/{id}/activate` | DecisionWeightController.activate | 无 | 管理端 | system:decision-weight:activate |
| DELETE | `/api/v1/decision/weights/{id}` | DecisionWeightController.delete | 无 | 管理端 | system:decision-weight:delete |
| POST | `/api/v1/decision/weights` | DecisionWeightController.create | 无 | 管理端 | system:decision-weight:create |
 | DELETE | `/api/system/depts/{id}` | SysDeptController.delete | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/system/depts/{id}` | SysDeptController.update | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/depts/tree` | SysDeptController.tree | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/depts` | SysDeptController.add | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/dicts/items/{dictCode}` | SysDictController.listItems | 无 | 用户自助 | 校验当前登录人 == 目标 userId | 
 | DELETE | `/api/system/dicts/items/{id}` | SysDictController.deleteItem | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/dicts/items` | SysDictController.createItem | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/system/dicts/items` | SysDictController.updateItem | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | DELETE | `/api/system/dicts/types/{id}` | SysDictController.deleteType | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/dicts/types/code/{dictCode}` | SysDictController.getTypeByCode | 无 | 用户自助 | 校验当前登录人 == 目标 userId | 
 | GET | `/api/system/dicts/types` | SysDictController.listTypes | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/dicts/types` | SysDictController.createType | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/system/dicts/types` | SysDictController.updateType | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | DELETE | `/api/system/permissions/{id}` | SysPermissionController.delete | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/permissions/{id}` | SysPermissionController.getById | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/system/permissions/{id}` | SysPermissionController.update | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
| GET | `/api/system/permissions/api` | SysPermissionController.findApiPermissions | 无 | 内部调用 | 内部签名/白名单 |
| GET | `/api/system/permissions/find-by-url` | SysPermissionController.findPermissionsByUrl | 无 | 内部调用 | 内部签名/白名单 |
 | GET | `/api/system/permissions/tree` | SysPermissionController.tree | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
| GET | `/api/system/permissions/user/{userId}` | SysPermissionController.getUserPermissions | 无 | 内部调用 | 内部签名/白名单 |
 | POST | `/api/system/permissions` | SysPermissionController.add | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/roles/{id}/grant-permissions` | SysRoleController.grantPermissions | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/roles/{id}/permissions` | SysRoleController.getRolePermissions | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | DELETE | `/api/system/roles/{id}` | SysRoleController.delete | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/system/roles/{id}` | SysRoleController.update | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/roles/all` | SysRoleController.listAll | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/roles` | SysRoleController.list | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/roles` | SysRoleController.add | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/status/actions` | SysStatusDictController.getAvailableActions | 无 | 用户自助 | 校验当前登录人 == 目标 userId | 
 | GET | `/api/system/status/check` | SysStatusDictController.checkTransition | 无 | 用户自助 | 校验当前登录人 == 目标 userId | 
 | GET | `/api/system/status/dict/{bizType}/{statusCode}` | SysStatusDictController.getStatusByCode | 无 | 用户自助 | 校验当前登录人 == 目标 userId | 
 | GET | `/api/system/status/dict/{bizType}` | SysStatusDictController.listStatusDict | 无 | 用户自助 | 校验当前登录人 == 目标 userId | 
 | DELETE | `/api/system/status/dict/{id}` | SysStatusDictController.deleteStatusDict | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/status/dict` | SysStatusDictController.createStatusDict | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/system/status/dict` | SysStatusDictController.updateStatusDict | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/status/next-statuses` | SysStatusDictController.getValidNextStatuses | 无 | 用户自助 | 校验当前登录人 == 目标 userId | 
| POST | `/api/system/status/transition` | SysStatusDictController.executeTransition | 无 | 管理端 | system:status:execute-transition |
 | GET | `/api/system/status/transitions/{bizType}` | SysStatusDictController.listTransitions | 无 | 用户自助 | 校验当前登录人 == 目标 userId | 
 | DELETE | `/api/system/status/transitions/{id}` | SysStatusDictController.deleteTransition | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/status/transitions` | SysStatusDictController.createTransition | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/system/status/transitions` | SysStatusDictController.updateTransition | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/users/{id}/force-logout` | SysUserController.forceLogout | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/users/{id}/grant-roles` | SysUserController.grantRoles | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/users/{id}/grant-temporary-roles` | SysUserController.grantTemporaryRoles | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/users/{id}/lock` | SysUserController.lockUser | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/users/{id}/reset-password` | SysUserController.resetPassword | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/users/{id}/statistics` | SysUserController.getUserStatistics | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/users/{id}/temporary-roles` | SysUserController.getUserTemporaryRoles | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | DELETE | `/api/system/users/{id}` | SysUserController.delete | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | GET | `/api/system/users/{id}` | SysUserController.getById | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | PUT | `/api/system/users/{id}` | SysUserController.update | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/users/{userId}/extend-temporary-role/{roleId}` | SysUserController.extendTemporaryRole | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/users/{userId}/terminate-temporary-role/{roleId}` | SysUserController.terminateTemporaryRole | 方法级 | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
| GET | `/api/system/users/{userId}/update-login` | SysUserController.updateLastLogin | 无 | 内部调用 | 内部签名/白名单 |
 | POST | `/api/system/users/change-password` | SysUserController.changePassword | 无 | 用户自助 | 校验当前登录人 == 目标 userId | 
 | GET | `/api/system/users` | SysUserController.list | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 
 | POST | `/api/system/users` | SysUserController.add | 方法级+URL | 方法级 @PreAuthorize | 已有 @PreAuthorize | 

### scm-tenant（52 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| DELETE | `/tenant-config/{id}` | TenantConfigController.deleteById | 无 | 管理端 | tenant:config:delete |
| GET | `/tenant-config/{id}` | TenantConfigController.getById | 无 | 管理端 | tenant:config:view |
| GET | `/tenant-config/page` | TenantConfigController.pageQuery | 无 | 管理端 | tenant:config:page |
| GET | `/tenant-config/tenant/{tenantId}/key/{configKey}` | TenantConfigController.getConfigByTenantAndKey | 无 | 管理端 | tenant:config:get-config-by-tenant-and-key |
| POST | `/tenant-config` | TenantConfigController.create | 无 | 管理端 | tenant:config:create |
| PUT | `/tenant-config` | TenantConfigController.update | 无 | 管理端 | tenant:config:update |
| PUT | `/tenant/{id}/disable` | TenantController.disable | 无 | 管理端 | tenant:tenant:disable |
| PUT | `/tenant/{id}/enable` | TenantController.enable | 无 | 管理端 | tenant:tenant:enable |
| DELETE | `/tenant/{id}` | TenantController.deleteById | 无 | 管理端 | tenant:tenant:delete |
| GET | `/tenant/{id}` | TenantController.getById | 无 | 管理端 | tenant:tenant:view |
| GET | `/tenant/active` | TenantController.listActive | 无 | 管理端 | tenant:tenant:list-active |
| GET | `/tenant/page` | TenantController.pageQuery | 无 | 管理端 | tenant:tenant:page |
| POST | `/tenant` | TenantController.create | 无 | 管理端 | tenant:tenant:create |
| PUT | `/tenant` | TenantController.update | 无 | 管理端 | tenant:tenant:update |
| DELETE | `/tenant-feature/{id}` | TenantFeatureController.deleteById | 无 | 管理端 | tenant:feature:delete |
| GET | `/tenant-feature/{id}` | TenantFeatureController.getById | 无 | 管理端 | tenant:feature:view |
| GET | `/tenant-feature/check` | TenantFeatureController.isFeatureEnabled | 无 | 管理端 | tenant:feature:is-feature-enabled |
| GET | `/tenant-feature/page` | TenantFeatureController.pageQuery | 无 | 管理端 | tenant:feature:page |
| GET | `/tenant-feature/tenant/{tenantId}` | TenantFeatureController.listByTenantId | 无 | 管理端 | tenant:feature:list-by-tenant-id |
| POST | `/tenant-feature` | TenantFeatureController.create | 无 | 管理端 | tenant:feature:create |
| PUT | `/tenant-feature` | TenantFeatureController.update | 无 | 管理端 | tenant:feature:update |
| DELETE | `/tenant-operation-log/{id}` | TenantOperationLogController.deleteById | 无 | 管理端 | tenant:operation-log:delete |
| GET | `/tenant-operation-log/{id}` | TenantOperationLogController.getById | 无 | 管理端 | tenant:operation-log:view |
| GET | `/tenant-operation-log/operation-type/{operationType}` | TenantOperationLogController.listByOperationType | 无 | 管理端 | tenant:operation-log:list-by-operation-type |
| GET | `/tenant-operation-log/page` | TenantOperationLogController.pageQuery | 无 | 管理端 | tenant:operation-log:page |
| GET | `/tenant-operation-log/tenant/{tenantId}` | TenantOperationLogController.listByTenantId | 无 | 管理端 | tenant:operation-log:list-by-tenant-id |
| POST | `/tenant-operation-log` | TenantOperationLogController.create | 无 | 管理端 | tenant:operation-log:create |
| DELETE | `/tenant-package/{id}` | TenantPackageController.deleteById | 无 | 管理端 | tenant:package:delete |
| GET | `/tenant-package/{id}` | TenantPackageController.getById | 无 | 管理端 | tenant:package:view |
| GET | `/tenant-package/active` | TenantPackageController.listActive | 无 | 管理端 | tenant:package:list-active |
| GET | `/tenant-package/page` | TenantPackageController.pageQuery | 无 | 管理端 | tenant:package:page |
| POST | `/tenant-package` | TenantPackageController.create | 无 | 管理端 | tenant:package:create |
| PUT | `/tenant-package` | TenantPackageController.update | 无 | 管理端 | tenant:package:update |
| POST | `/tenant-platform-fee/calculate` | TenantPlatformFeeController.calculate | 无 | 管理端 | tenant:platform-fee:calculate |
| GET | `/tenant-platform-fee/page` | TenantPlatformFeeController.page | 无 | 管理端 | tenant:platform-fee:page |
| GET | `/tenant-platform-fee/tenant/{tenantId}` | TenantPlatformFeeController.listByTenant | 无 | 管理端 | tenant:platform-fee:list-by-tenant |
| POST | `/tenant/provisioning` | TenantProvisioningController.provisionTenant | 无 | 管理端 | tenant:provisioning:provision-tenant |
| DELETE | `/tenant-resource-quota/{id}` | TenantResourceQuotaController.deleteById | 无 | 管理端 | tenant:quota:delete |
| GET | `/tenant-resource-quota/{id}` | TenantResourceQuotaController.getById | 无 | 管理端 | tenant:quota:view |
| GET | `/tenant-resource-quota/check` | TenantResourceQuotaController.checkQuota | 无 | 管理端 | tenant:quota:check-quota |
| GET | `/tenant-resource-quota/page` | TenantResourceQuotaController.pageQuery | 无 | 管理端 | tenant:quota:page |
| GET | `/tenant-resource-quota/tenant/{tenantId}` | TenantResourceQuotaController.listByTenantId | 无 | 管理端 | tenant:quota:list-by-tenant-id |
| POST | `/tenant-resource-quota` | TenantResourceQuotaController.create | 无 | 管理端 | tenant:quota:create |
| PUT | `/tenant-resource-quota` | TenantResourceQuotaController.update | 无 | 管理端 | tenant:quota:update |
| POST | `/tenant-subscription/{id}/unsubscribe` | TenantSubscriptionController.unsubscribe | 无 | 管理端 | tenant:subscription:unsubscribe |
| DELETE | `/tenant-subscription/{id}` | TenantSubscriptionController.deleteById | 无 | 管理端 | tenant:subscription:delete |
| GET | `/tenant-subscription/{id}` | TenantSubscriptionController.getById | 无 | 管理端 | tenant:subscription:view |
| GET | `/tenant-subscription/page` | TenantSubscriptionController.pageQuery | 无 | 管理端 | tenant:subscription:page |
| POST | `/tenant-subscription/subscribe` | TenantSubscriptionController.subscribe | 无 | 管理端 | tenant:subscription:subscribe |
| GET | `/tenant-subscription/tenant/{tenantId}` | TenantSubscriptionController.listByTenantId | 无 | 管理端 | tenant:subscription:list-by-tenant-id |
| POST | `/tenant-subscription` | TenantSubscriptionController.create | 无 | 管理端 | tenant:subscription:create |
| PUT | `/tenant-subscription` | TenantSubscriptionController.update | 无 | 管理端 | tenant:subscription:update |

### scm-warehouse（46 条）

| HTTP | 路径 | Controller 方法 | 保护 | 分类 | 建议权限码 / 处理 |
|---|---|---|---|---|---|
| PUT | `/wms-inbound/{id}/cancel` | WmsInboundController.cancel | 无 | 管理端 | warehouse:inbound:cancel |
| PUT | `/wms-inbound/{id}/receive` | WmsInboundController.receive | 无 | 管理端 | warehouse:inbound:receive |
| DELETE | `/wms-inbound/{id}` | WmsInboundController.delete | 无 | 管理端 | warehouse:inbound:delete |
| GET | `/wms-inbound/{id}` | WmsInboundController.getById | 无 | 管理端 | warehouse:inbound:view |
| PUT | `/wms-inbound/{id}` | WmsInboundController.update | 无 | 管理端 | warehouse:inbound:update |
| GET | `/wms-inbound/page` | WmsInboundController.page | 无 | 管理端 | warehouse:inbound:page |
| POST | `/wms-inbound` | WmsInboundController.create | 无 | 管理端 | warehouse:inbound:create |
| DELETE | `/wms-inbound-item/{id}` | WmsInboundItemController.delete | 无 | 管理端 | warehouse:inbound-item:delete |
| GET | `/wms-inbound-item/{id}` | WmsInboundItemController.getById | 无 | 管理端 | warehouse:inbound-item:view |
| PUT | `/wms-inbound-item/{id}` | WmsInboundItemController.update | 无 | 管理端 | warehouse:inbound-item:update |
| GET | `/wms-inbound-item/list` | WmsInboundItemController.listByInboundId | 无 | 管理端 | warehouse:inbound-item:list-by-inbound-id |
| POST | `/wms-inbound-item` | WmsInboundItemController.create | 无 | 管理端 | warehouse:inbound-item:create |
| DELETE | `/wms-location/{id}` | WmsLocationController.delete | 无 | 管理端 | warehouse:location:delete |
| GET | `/wms-location/{id}` | WmsLocationController.getById | 无 | 管理端 | warehouse:location:view |
| PUT | `/wms-location/{id}` | WmsLocationController.update | 无 | 管理端 | warehouse:location:update |
| GET | `/wms-location/list` | WmsLocationController.listByWarehouseId | 无 | 管理端 | warehouse:location:list-by-warehouse-id |
| GET | `/wms-location/page` | WmsLocationController.page | 无 | 管理端 | warehouse:location:page |
| POST | `/wms-location` | WmsLocationController.create | 无 | 管理端 | warehouse:location:create |
| PUT | `/wms-outbound/{id}/cancel` | WmsOutboundController.cancel | 无 | 管理端 | warehouse:outbound:cancel |
| PUT | `/wms-outbound/{id}/ship` | WmsOutboundController.ship | 无 | 管理端 | warehouse:outbound:ship |
| DELETE | `/wms-outbound/{id}` | WmsOutboundController.delete | 无 | 管理端 | warehouse:outbound:delete |
| GET | `/wms-outbound/{id}` | WmsOutboundController.getById | 无 | 管理端 | warehouse:outbound:view |
| PUT | `/wms-outbound/{id}` | WmsOutboundController.update | 无 | 管理端 | warehouse:outbound:update |
| GET | `/wms-outbound/page` | WmsOutboundController.page | 无 | 管理端 | warehouse:outbound:page |
| POST | `/wms-outbound` | WmsOutboundController.create | 无 | 管理端 | warehouse:outbound:create |
| DELETE | `/wms-outbound-item/{id}` | WmsOutboundItemController.delete | 无 | 管理端 | warehouse:outbound-item:delete |
| GET | `/wms-outbound-item/{id}` | WmsOutboundItemController.getById | 无 | 管理端 | warehouse:outbound-item:view |
| PUT | `/wms-outbound-item/{id}` | WmsOutboundItemController.update | 无 | 管理端 | warehouse:outbound-item:update |
| GET | `/wms-outbound-item/list` | WmsOutboundItemController.listByOutboundId | 无 | 管理端 | warehouse:outbound-item:list-by-outbound-id |
| POST | `/wms-outbound-item` | WmsOutboundItemController.create | 无 | 管理端 | warehouse:outbound-item:create |
| PUT | `/wms-warehouse/{id}/disable` | WmsWarehouseController.disable | 无 | 管理端 | warehouse:warehouse:disable |
| PUT | `/wms-warehouse/{id}/enable` | WmsWarehouseController.enable | 无 | 管理端 | warehouse:warehouse:enable |
| DELETE | `/wms-warehouse/{id}` | WmsWarehouseController.delete | 无 | 管理端 | warehouse:warehouse:delete |
| GET | `/wms-warehouse/{id}` | WmsWarehouseController.getById | 无 | 管理端 | warehouse:warehouse:view |
| PUT | `/wms-warehouse/{id}` | WmsWarehouseController.update | 无 | 管理端 | warehouse:warehouse:update |
| GET | `/wms-warehouse/list` | WmsWarehouseController.listEnabled | 无 | 管理端 | warehouse:warehouse:list-enabled |
| GET | `/wms-warehouse/page` | WmsWarehouseController.page | 无 | 管理端 | warehouse:warehouse:page |
| POST | `/wms-warehouse` | WmsWarehouseController.create | 无 | 管理端 | warehouse:warehouse:create |
| PUT | `/wms-wave-picking/{id}/cancel` | WmsWavePickingController.cancel | 无 | 管理端 | warehouse:wave-picking:cancel |
| PUT | `/wms-wave-picking/{id}/complete` | WmsWavePickingController.complete | 无 | 管理端 | warehouse:wave-picking:complete |
| PUT | `/wms-wave-picking/{id}/start` | WmsWavePickingController.start | 无 | 管理端 | warehouse:wave-picking:start |
| DELETE | `/wms-wave-picking/{id}` | WmsWavePickingController.delete | 无 | 管理端 | warehouse:wave-picking:delete |
| GET | `/wms-wave-picking/{id}` | WmsWavePickingController.getById | 无 | 管理端 | warehouse:wave-picking:view |
| PUT | `/wms-wave-picking/{id}` | WmsWavePickingController.update | 无 | 管理端 | warehouse:wave-picking:update |
| GET | `/wms-wave-picking/page` | WmsWavePickingController.page | 无 | 管理端 | warehouse:wave-picking:page |
| POST | `/wms-wave-picking` | WmsWavePickingController.create | 无 | 管理端 | warehouse:wave-picking:create |
