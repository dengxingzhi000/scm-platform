# 供应商管理前端补齐 — 设计文档

日期：2026-10-30
范围：`scm-web`（前端）。后端 `scm-supplier` 不改动。

---

## 1. 意图与背景

`scm-supplier` 后端已经是一套完整的企业级供应商管理实现：主数据 + 准入审核 + 分级分类 +
风险与黑名单 + 多联系人 + 多银行账户 + 资质证照（含到期预警）+ 绩效评价 + 变更留痕，
共 9 个 Controller、**62 个 HTTP 端点**。

前端 `scm-web/src/features/supplier/` 只有两个只读页面（列表 + 详情），
`scm-web/src/lib/api/services/supplier.ts` 只调用了 `GET /suppliers` 与
`GET /suppliers/{id}` 两个端点。**其余 48 个在范围内的端点全部闲置**，
前端 `Supplier` 类型（11 个字段）把 `supplierLevel`、`auditStatus`、`riskLevel`、
`blacklisted`、`creditRating`、`taxNo`、`overallScore` 等后端已经维护的字段全部丢弃。

结果是：后端具备的准入管控、风险管控、证照管控、绩效管控能力，
在业务人员使用的界面上一个都看不到 —— 供应商管理退化成了一个通讯录。

本设计的目标是**把后端已有的企业级能力完整地呈现在前端**，
让采购员能在界面上走完「建档 → 提交审核 → 审核 → 启用 → 挂分类 → 维护联系人/银行账户/
证照 → 评价绩效 → 查留痕 → 拉黑/解禁」这条完整链路，而不是再堆新的后端功能。

### 端点清单核对（62 个）

| Controller | 端点数 | 本次范围 |
|---|---|---|
| `SupSupplierController` | 21 | ✅ 全部 |
| `SupSupplierEvaluationController` | 7 | ✅ 全部 |
| `SupSupplierContactController` | 5 | ✅ 全部 |
| `SupSupplierBankAccountController` | 5 | ✅ 全部 |
| `SupSupplierCertificateController` | 5 | ✅ 全部 |
| `SupSupplierCategoryController` | 4 | ✅ 全部 |
| `SupSupplierCertificateExpiryController` | 1 | ✅ 全部 |
| `SupSettlementController` | 8 | ❌ 属财务域，`scm-finance` 已有结算管理页 |
| `SupPurchaseOrderItemController` | 6 | ❌ 属采购域，`scm-purchase` 已有采购订单页 |

**在范围 = 48 个端点。**

---

## 2. 目标 / 非目标 / 反目标

### 目标

1. **端点覆盖**：前端消费上表 48 个在范围端点。
2. **列表页从通讯录升级为管理台**：统计 KPI 条 + 全维度筛选 + 关键列 + 行内操作。
3. **详情页从静态卡片升级为 360° 画像**：概览 / 分类 / 联系人 / 银行账户 / 资质证照 /
   绩效评价 / 变更留痕 七个分区，全部可增删改。
4. **新增/编辑供应商表单**：覆盖 `SupplierUpsertRequest` 的全部可维护字段，
   Zod 校验，与后端校验规则对齐。
5. **准入审核工作台**：待审核队列 + 证照临期预警，双看板。
6. **分类字典管理页**：分类树维护。

### 非目标

- **不改后端**。后端端点、字段、状态机已足够；本次是纯前端补齐。
- **不做对账单 / 采购明细**（见上表）。在供应商模块里再做一套会造成两处数据源不一致。
- **不做供应商自助门户**。那是新服务（新的后端 + 鉴权模型），不是「补齐」。
- **不做 RFQ / 寻源比价**。`scm-purchase` 已有询价与报价页，属采购域。
- **不做独立的「绩效评价总览页」**。评价数据挂在供应商详情下（见 §5.6）。

### 反目标（做到即视为失败）

- **引入新的 Ant Design 页面**。`AGENTS.md` 明确要求不要新增 Ant Design 屏幕，
  供应商模块已有 shadcn/ui 骨架，必须延续。
- **前端自造业务规则**。分级阈值 90/80/70、加权 30/30/20/20、合作状态流转矩阵
  全部在服务端；前端只做展示与**基于当前状态的布尔型前置校验提示**，
  不在浏览器里复刻一套会漂移的判定。
- **把服务端托管字段开放给用户直接编辑**。`supplierLevel` / `auditStatus` /
  `riskLevel` / `blacklisted` / `overallScore` / `cooperationStartDate` /
  分类 `level` 由流程维护，表单不暴露这些字段。
- **破坏既有调用方**。`supplierApiService.list` / `.getById` 被
  `features/dashboard/services/dashboard.service.ts` 与
  `features/warehouse/services/warehouse.service.ts` 依赖，签名必须保持兼容。

---

## 3. 约束

- Next.js 16.4 App Router + React 19 + TypeScript 7，路由带 locale 前缀
  （`/{locale}/...`），由 `src/proxy.ts` 处理。
- UI：shadcn/ui（`src/components/ui`），Tailwind v4，图标 lucide-react。
- 服务端状态：TanStack Query v5，单一 `QueryClient`。
- 表单：React Hook Form + Zod（`zodResolver`），长表单用 `FormField`。
- 表格：TanStack Table v9（`tableFeatures` / `createColumnHelper` / `useTable`）。
- 样式基调：`src/lib/design-tokens.css` 的设计 token（`--color-success`、
  `--color-error`、`--color-warning`、`--color-primary-text` 等）。
- 现有 shadcn 组件：badge / button / card / checkbox / confirm-dialog /
  dialog / dropdown-menu / form / input / label / page-header / popover /
  scroll-area / select / separator / skeleton / table / table-shell / textarea / tooltip。
  **没有** tabs、toast、date-picker、alert-dialog、multi-select 组件。
- 无 `sonner` / `react-hot-toast` 依赖 —— 反馈必须用行内提示
  （`submitError` 模式，见 `features/product/components/product-form.tsx`）
  或 `confirm-dialog.tsx`。
- API base：`/scm-supplier/api/v1/**`（网关 Nacos discovery locator）。
- 响应解包：`unwrap<T>()`（`src/lib/api/envelope.ts`）。`ApiResponse` 是
  `@JsonInclude(NON_NULL)`，`success()` 返回体没有 `data` 键 —— Void 端点
  一律声明为 `Promise<void>` 并忽略返回值。
- 分页：`scm-supplier` 返回 MyBatis-Plus `Page` 原始结构
  （`records/total/size/current`），用 `contracts/page.ts` 的 `MpPage<T>` + `toUiPage`。
  请求参数是 **`page` / `size`**（不是 `pageNum`），并支持
  `keyword / supplierType / cooperationStatus / enabled / auditStatus /
  supplierLevel / riskLevel / blacklisted / categoryId` 共 10 个筛选条件。

---

## 4. 已核实的后端行为（实现时必须遵守）

这一节是构建者最容易踩坑的地方，全部已对照源码核实。

### 4.1 请求形状：query param 还是 JSON body

| 端点 | 参数传递方式 |
|---|---|
| `PUT /{id}/audit` | **JSON body** `SupplierAuditRequest{action, operatorId, operatorName, remark}` |
| `PUT /{id}/categories` | **JSON body**，裸 `List<String>`（**不是** `{categoryIds:[...]}`） |
| `POST /` `PUT /{id}` | **JSON body** `SupplierUpsertRequest` |
| `POST /{id}/submit-audit` | query param `operatorId` `operatorName`（**无 body**） |
| `PUT /{id}/blacklist` | query param `reason`（**必填**）`operatorId` `operatorName` |
| `PUT /{id}/unblacklist` | query param `operatorId` `operatorName` |
| `PUT /{id}/risk-level` | query param `level`（**必填**）`reason`（可选）`operatorId` `operatorName` |
| `PUT /{id}/cooperation-status` | query param `status`（**必填**）`operatorId` `operatorName` |
| `PUT /{id}/certificates/{cid}/verify` | query param `approved`（**必填**）`operatorId` `operatorName` `remark` |
| `DELETE /{id}` | query param `operatorId` `operatorName` |
| `PUT /{id}/enable` `PUT /{id}/disable` | 无参数 |

### 4.2 `PUT /{id}` 实际是 PATCH 语义

`SupSupplierServiceImpl.updateSupplier` 用
`org.springframework.beans.BeanUtils.copyProperties(request, update)` 拷贝**含 null**，
但 `scm-supplier/service/src/main/resources/application.yml` 的
`mybatis-plus.global-config.db-config` **只配了 `logic-delete-*`**，没有覆盖
`update-strategy`。MyBatis-Plus 默认 `FieldStrategy.NOT_NULL` ——
**null 字段在 SET 子句里被跳过**。

实际语义：

- `null` / `undefined` → **该字段保持原值**（不写库）
- `""` → **写入空字符串**（真正的「清空」）

因此：

- 表单**不必**回填并发送所有字段，只发送自己管的字段即可（后端会保留其余字段）。
- **但表单把某个可选字段清空时，payload 必须发 `""`，不能发 `null`/`undefined`** ——
  否则用户清空了备注，保存「成功」，但旧值还在。这是一个静默的数据 bug，
  必须在 payload mapper 里显式处理。
- `cooperationStartDate` **不由表单托管**（服务端在建立合作时自动置 `now()`），
  payload 里直接不出现这个键。

### 4.3 操作人（operatorId / operatorName）

所有变更类端点都接受 `operatorId` / `operatorName`（body 或 query param）。
来源统一为 **`useAuthStore().user`**（`scm-web/src/stores/useAuthStore.ts`）：

- `operatorId` = `user.id`
- `operatorName` = `user.displayName || user.username || ""`

在 `features/supplier/hooks/` 里提供一个 `useOperator()` hook 统一产出这两个值，
所有 mutation 走它，禁止各组件自己取。

**已知后端限制**：`createSupplier` 与 `updateSupplier` 的变更日志
硬编码 `changeLogService.record(id, "CREATE"/"UPDATE", ..., null, null, null)` ——
**操作人永远为 null**，与前端传什么无关。前端把空操作人渲染为「系统」。
（本次不改后端；这是一个后端 bug 苗头，值得单独提一个 issue。）

### 4.4 错误消息：`unwrap` 抓不到 4xx

`ServiceException` 由 `scm-common/core` 的
`GlobalExceptionHandler.handleServiceException` 处理，返回
**HTTP 4xx** + `ApiResponse.fail(code, "Service error: " + msg + " (traceId=...)")`。
axios 因此 reject，`unwrap` 根本不执行。

**所有 mutation 的错误路径必须用 `toApiError(error)`**（`src/lib/api/envelope.ts` 已导出）
取出 `response.data.message`，而不是 `error.message`（那只会得到
「Request failed with status code 400」）。

展示规则：**去掉 `Service error: ` 前缀**，保留尾部 `(traceId=…)` 以灰色小字显示
（排障时要用）。后端的关键业务消息原文如下，UI 必须能让用户看到：

- `合作中的供应商不能删除，请先终止合作`
- `非法合作状态流转: X -> Y`
- `未通过准入审核的供应商不可建立合作`
- `黑名单供应商不可建立合作`
- `列入黑名单必须填写原因`
- `风险等级仅支持 1-低,2-中,3-高`
- `合作状态仅支持 0-3`
- `供应商编码已存在` / `税号已存在`
- `分类编码已存在` / `上级分类不存在`

### 4.5 合作状态流转矩阵不下发到前端

服务端矩阵：`0→1`、`1→2|3`、`2→1|3`、`3→0`。

前端**不复制这张表**。「变更合作状态」下拉给出全部 4 个目标值，
非法流转由后端 400 拒绝，前端显示 §4.4 的消息。

前端**只**做基于当前状态的布尔型前置提示（这些是明确的业务前置条件，不是状态机）：

| 动作 | 前置条件（读当前状态） | 提示文案 |
|---|---|---|
| 启用 | `blacklisted == false` | 黑名单供应商不可启用 |
| 删除 | `cooperationStatus != 1` | 合作中的供应商不能删除，请先终止合作 |
| 建立合作（status=1） | `auditStatus == 2 && !blacklisted` | 未通过准入审核 / 黑名单供应商不可建立合作 |
| 提交审核 | `taxNo` 非空 | 税号未补全，无法提交审核 |

不满足时动作**置灰**并用 tooltip 说明原因。

### 4.6 `overallScore` 的两套口径

- `sup_supplier.overall_score` 列：由 `refreshSupplierScorecard` 用
  **4 维加权**（质量 30 / 交付 30 / 服务 20 / 价格 20，各维取评价均值后加权）回写。
- 只有 `V1__supplier_enterprise_management.sql` 的历史回填用的是
  **3 维简单均值** `ROUND((quality+delivery+service)/3.0, 2)`。

所以**历史遗留数据**的列表得分与详情得分可能不一致，**当前数据一致**。

决策：

- 列表「综合得分」列显示 **0–100 原始分**（`overallScore`）。
- 星级展示：`overallScore / 20` 映射到 0–5 星（`RatingStars` 的量程是 0–5）。
- `toRating` 改为：优先 `overallScore / 20`；`overallScore` 为空时**退回**现有的
  三维度均值 / 20 逻辑（保留旧行为，不回归）。
- 前端**不复算**加权总分，不调和 3 维 / 4 维差异。

### 4.7 证照到期预警只含「未来到期」

`SupSupplierCertificateServiceImpl.pageExpiring` 的过滤条件是
`.ge(validTo, today).le(validTo, today + days).ne(status, 4)` ——
**已过期的证照完全不在结果里**。`countExpiring`（喂 `stats.expiringCertificates`）
下界同样是 `ge(validTo, today)`。

决策：

- 预警看板的文案明确写「**未来 N 天内到期**」，不做「已过期」红色态
  （后端给不出来，做了就是假数据）。
- KPI「资质临期」的文案同样改为「N 天内到期」。
- **已过期证照在供应商详情的证照分区可见**（`listBySupplierId` 不做日期过滤，
  `fillDerivedFields` 会把 `status` 置 3 并给出负的 `daysToExpiry`），
  那里才用红色「已过期」。

### 4.8 分类的 level 与启用开关

- `SupSupplierCategoryServiceImpl.resolveLevel(parentId)` 在创建时、
  以及更新且传了 parentId 时**覆盖** `level`。→ **表单不暴露「层级」字段**。
- `validateCategory` 要求 `categoryCode` 与 `categoryName` **均非空**。
  → **启用/禁用开关必须同时回传这两个字段**，不能只发 `enabled`。
- `ensureCodeUnique` 按租户判重（`categoryCode` 唯一）。

### 4.9 URL 筛选同步需要 Suspense 边界

`next build` 在客户端组件里用 `useSearchParams()` 而没有 Suspense 边界会失败
（`missing-suspense-with-csr-bailout`）。
参照 `src/app/[locale]/(app)/order/page.tsx`：
**`supplier/page.tsx` 必须改成 `<Suspense fallback={<Skeleton/>}>` 包住列表组件。**

### 4.10 链接的 locale 前缀

既有 feature（supplier / product / order / purchase）的 `router.push` **不带 locale 前缀**
（`/supplier/xxx`），只有 dashboard 用了 `/${locale}/...`。
`src/proxy.ts`（next-intl）会把无前缀路径重定向到默认 locale。

决策：**沿用多数派写法，不带 locale 前缀**，并保持现有 supplier 页面的写法不变。

### 4.11 多选没有现成组件

`src/components/ui/select.tsx` 是单选 Radix Select，没有 multi-select。
供应商分类关联（`PUT /{id}/categories` 是全量替换）需要多选 ——
用现成的 `src/components/ui/checkbox.tsx` 在 Dialog 里做一组复选框实现。

---

## 5. 设计

### 5.1 路由与菜单

新增 6 条路由（`src/app/[locale]/(app)/supplier/...`）：

| 路由 | 页面 | 说明 |
|---|---|---|
| `/supplier` | 供应商列表 | 已有，改造（加 Suspense 边界） |
| `/supplier/new` | 新建供应商 | 新增，静态段优先于 `[id]` |
| `/supplier/[id]` | 供应商 360° 画像 | 已有，改造 |
| `/supplier/[id]/edit` | 编辑供应商 | 新增 |
| `/supplier/audit` | 准入审核工作台 | 新增 |
| `/supplier/category` | 分类字典 | 新增 |

Next.js App Router 中静态段优先于动态段，`/supplier/new` 不会被
`/supplier/[id]` 捕获，无需额外处理。

**菜单**（`src/components/Layout/menu-config.tsx`）：把单叶子
`getItem('供应商管理', '/supplier', <TeamOutlined />)` 改成子菜单组，
与「商品管理」「仓库管理」「采购管理」的既有模式一致：

```
供应商管理 (group-supplier, TeamOutlined)
  - 供应商列表 /supplier
  - 准入审核   /supplier/audit
  - 分类字典   /supplier/category
```

`menu-config.tsx` 目前是 antd 的 `MenuProps`，**保持现状不动** ——
改菜单结构不等于改菜单实现。

### 5.2 契约层 `src/lib/api/contracts/supplier.ts`

把 `SupplierDTO` 补齐到 `SupSupplier` 实体的全字段，并新增：

- `SupplierContactDTO`、`SupplierBankAccountDTO`、`SupplierCertificateDTO`、
  `SupplierCategoryDTO`、`SupplierEvaluationDTO`、`SupplierChangeLogDTO`
- `SupplierProfileDTO`（`GET /{id}/profile` 的信封）
- `SupplierStatsDTO`（`GET /stats`）
- `SupplierPerformanceDTO`（`GET /{id}/performance`）
- `SupplierUpsertPayload`（对应后端 `SupplierUpsertRequest`）
- `SupplierAuditPayload`（`action` / `operatorId` / `operatorName` / `remark`）

字段命名一律保持后端驼峰（`supplierName`、`auditStatus`…），不预先转 snake_case；
DTO → UI 模型的转换集中在 feature 层的 `lib/mappers.ts`。

### 5.3 API 服务层 `src/lib/api/services/supplier.ts`

保持 `supplierApiService.list(query)` 与 `supplierApiService.getById(id)`
签名不变（dashboard / warehouse 依赖），在其上扩展为分组结构：

```ts
export const supplierApiService = {
  // —— 已有，保持兼容 ——
  list, getById,
  // —— 新增 ——
  page, stats, profile, performance, changeLogs, active, search,
  create, update, remove,
  enable, disable, submitAudit, audit, blacklist, unblacklist,
  changeRiskLevel, changeCooperationStatus,
  getCategories, assignCategories,
  contacts:     { list, create, update, remove, setPrimary },
  bankAccounts: { list, create, update, remove, setDefault },
  certificates: { list, create, update, verify, remove },
  certificateExpiry: { expiring },
  categories:   { list, create, update, remove },
  evaluations:  { list, page, create, update, remove, bySupplier, averageScore },
}
```

参数形状严格按 §4.1 写：query-param 的用 `params: {...}`，body 的用 `data: {...}`。
`assignCategories` 的 body 是**裸数组**。

### 5.4 Feature 层 `src/features/supplier/`

```
types/supplier.types.ts        UI 模型（扩展现有 Supplier + 新增各子实体模型）
types/index.ts
services/supplier.service.ts   feature 级编排（DTO → UI）
services/index.ts
hooks/use-supplier.ts          供应商主档的 query + mutation
hooks/use-supplier-contact.ts
hooks/use-supplier-bank-account.ts
hooks/use-supplier-certificate.ts
hooks/use-supplier-category.ts
hooks/use-supplier-evaluation.ts
hooks/use-operator.ts           统一产出 operatorId / operatorName（见 §4.3）
hooks/index.ts
lib/mappers.ts                 DTO → UI
lib/mappers.test.ts            映射单测（现有文件扩展）
lib/constants.ts               枚举字典（见下）
lib/supplier-form-schema.ts    Zod schema + 表单值 ↔ payload 映射
lib/api-error.ts               toApiError 包装 + 消息清洗（见 §4.4）
components/*.tsx               见下
components/index.ts
```

**`lib/constants.ts`** 是枚举字典的唯一来源，列表徽标、筛选下拉、表单下拉共用：

| 字典 | 取值 |
|---|---|
| `SUPPLIER_LEVELS` | 1 战略 / 2 优选 / 3 合格 / 4 备选 |
| `AUDIT_STATUSES` | 0 草稿 / 1 待审核 / 2 已通过 / 3 已驳回 |
| `RISK_LEVELS` | 1 低 / 2 中 / 3 高 |
| `COOPERATION_STATUSES` | 0 潜在 / 1 合作中 / 2 暂停 / 3 终止 |
| `CERTIFICATE_TYPES` | BUSINESS_LICENSE / TAX_REGISTRATION / QUALITY_ISO / PRODUCTION_LICENSE / AGENT_AUTHORIZATION / OTHER |
| `CERTIFICATE_STATUSES` | 0 待审核 / 1 有效 / 2 即将到期 / 3 已过期 / 4 已驳回 |
| `CONTACT_ROLES` | SALES / FINANCE / LOGISTICS / TECH / OTHER |
| `CHANGE_TYPES` | CREATE / UPDATE / DELETE / SUBMIT_AUDIT / AUDIT_APPROVE / AUDIT_REJECT / ENABLE / DISABLE / BLACKLIST / UNBLACKLIST / RISK_LEVEL / COOPERATION_STATUS / CATEGORY_ASSIGN |

徽标配色用设计 token：分级用主色系、风险用 error/warning、准入用 success/neutral。
`Badge` 组件的 `variant` 取值以 `src/components/ui/badge.tsx` 实际支持的为准，
在 `constants.ts` 里集中做「状态 → variant + 颜色类名」映射，组件不散落硬编码。

**`lib/supplier-form-schema.ts`**：Zod schema 覆盖 `SupplierUpsertRequest`
的全部**可维护**字段。校验规则与后端 `SupSupplierServiceImpl.validateMasterData`
对齐（编码 `^[A-Za-z0-9_-]{2,64}$`、名称必填 ≤256、税号 ≤64、
信用等级枚举 `A+/A/B+/B/C/D`、邮箱格式、注册资本 ≥ 0）。

**payload mapper 的关键约定**（见 §4.2）：

- 表单未托管的字段 → **不出现在 payload 里**（而不是发 `null`）。
- 用户清空了一个可选字段 → payload 里发 **`""`**，不是 `null` / `undefined`。
- `cooperationStartDate` **不由表单托管**，不出现。
- 服务端托管字段（`supplierLevel` / `auditStatus` / `riskLevel` / `blacklisted` /
  `overallScore`）**不出现在 payload 里**。

### 5.5 列表页 `/supplier`

自上而下：

1. **统计 KPI 条** — `GET /stats` 的 6 个数字，做成 6 个数字卡：
   总数 / 启用 / 合作中 / 待审核 / 黑名单 / **30 天内到期证照**。
   数字可点击，点击即写入对应筛选：
   待审核 → `auditStatus=1`；黑名单 → `blacklisted=true`；合作中 → `cooperationStatus=1`；
   资质到期 → 跳 `/supplier/audit`。
2. **筛选栏** — 关键字输入（带 **typeahead 快速跳转**：调用 `GET /suppliers/search?name=`
   给出的候选项，点击直接跳详情）+ 下拉：供应商类型、合作状态、准入状态、分级、
   风险等级、黑名单、分类（分类下拉来自 `GET /supplier-categories`，按
   `sortOrder` 排序，`parentId` 构成缩进层级）。
   筛选状态同步到 URL query（与 order 列表页已有的做法一致），
   保证刷新与分享可还原。**页面需包 Suspense 边界**（§4.9）。
3. **表格** — 列：供应商（名称 + 编码，左侧色条按合作状态）/ 分级 / 准入状态 /
   风险等级 / 综合得分（0–100 + 星级）/ 联系人 / 合作状态 / 启用状态 / 操作。
   `RatingStars` 保留，按 §4.6 改为 `overallScore` 优先。
4. **行操作** — 每行两个图标快捷按钮（查看 / 编辑）+ 一个 `⋯` 下拉菜单
   （`DropdownMenu`），菜单项：提交审核 / 审核准入 / 启用 / 停用 /
   调整风险等级 / 变更合作状态 / 列入黑名单 / 移出黑名单 / 删除。
   不可用的动作按 §4.5 置灰并给 tooltip 说明。
   需要填理由 / 备注的动作（审核、风险等级、黑名单）打开 Dialog 表单。

### 5.6 详情页 `/supplier/[id]` — 360° 画像

单页 + 七个分区。**不引入 tabs 组件**（仓库里没有）—— **左侧分区导航 + 右侧内容**
（2026-10-30 评审选定方案 A：左侧导航带每个分区的条目数，切换不丢上下文，
改动某个分区不会撑爆整页高度）。

左栏宽 ~120px，7 个分区项依次是：概览 / 分类 / 联系人 / 银行账户 / 资质证照 /
绩效评价 / 变更留痕，每项右侧带条目数徽标。右栏放当前分区的内容。

| 分区 | 数据来源 | 可写操作 |
|---|---|---|
| 概览 | `GET /{id}/profile` 的 `supplier` + `GET /{id}/performance` | — （只读） |
| 分类 | `GET /{id}/profile` 的 `categories` / `GET /{id}/categories` | `PUT /{id}/categories`（全量替换，复选框多选） |
| 联系人 | `GET /suppliers/{id}/contacts` | 新增 / 编辑 / 删除 / 设为主联系人 |
| 银行账户 | `GET /suppliers/{id}/bank-accounts` | 新增 / 编辑 / 删除 / 设为默认 |
| 资质证照 | `GET /suppliers/{id}/certificates` | 新增 / 编辑 / 审核 / 删除 |
| 绩效评价 | `GET /supplier-evaluations?page&size&supplierId`（分页） + `GET /supplier-evaluations/supplier/{id}/average-score` | 新增 / 编辑 / 删除 |
| 变更留痕 | `GET /suppliers/{id}/change-logs?limit=50` | — （只读） |

页面顶部固定一条：供应商名称 + 编码 + 状态徽标组（分级 / 准入 / 风险 / 黑名单 /
合作状态 / 启用）+ 操作按钮组（编辑 / 提交审核 / 审核 / 更多）。

说明：

- **概览分区**展示主数据全字段（编码、名称/英文名、类型、业务类型、法人、
  注册资本、统一社会信用代码、税号、成立日期、信用等级、付款条件、付款方式、
  合作起始日期、官网、排序、备注）+ 状态徽标组（分级/准入/风险/黑名单/合作/启用）
  + 绩效评分卡（`performance`：总分、四维度、评价次数、最近周期与得分、建议分级）。
- **绩效评价分区**用分页端点而不是 `bySupplier` 全量拉取，避免评价多时一次拉爆；
  `bySupplier` 用于绩效卡片的「最近评价」摘要，`averageScore` 用于分区头部显示平均得分。
- **变更留痕**时间线展示：类型、原因/内容、操作人、时间。操作人为空时显示「系统」（§4.3）。
- 分区内的增删改全部走 Dialog + RHF 表单，操作成功后 `invalidateQueries`
  刷新对应 key。

### 5.7 新建 / 编辑 `/supplier/new`、`/supplier/[id]/edit`

同一个 `SupplierForm` 组件，`id` 存在即编辑。分区：

- **基本信息** — 编码（编辑时只读）、名称、英文名、供应商类型、业务类型、
  法人、注册资本、成立日期、统一社会信用代码、税号、信用等级
- **联系信息** — 联系人、联系电话、联系邮箱、联系地址
- **结算信息** — 开户行、银行账号、账户名称、付款条件、付款方式
- **其他** — 官网、证照摘要（`certificates` 文本）、营业执照（`businessLicense`
  文本）、排序、备注

**不包含** `cooperationStartDate`（服务端托管）与任何状态/评分字段（§4.2、§5.4）。

### 5.8 准入审核工作台 `/supplier/audit`

页面顶部一条页头，下面**上下两个看板**（不是左右）：

- **待审核队列** — `GET /suppliers?auditStatus=1` 分页列表。列：
  供应商（名称 + 编码）/ 供应商类型 / 信用等级 / 税号 / 证照数 / 提交时间 / 审核操作。
  行内「通过 / 驳回」两个按钮，点「通过」直接提交（审核意见选填），
  点「驳回」打开 Dialog（**审核意见必填**）。通过后后端自动启用供应商。
  **税号未补全的行，「通过」置灰**（后端 `submitAudit` 要求税号已补全），
  tooltip 写明原因。
  页头右侧提供「已驳回 (4)」入口，查看被驳回的供应商（`auditStatus=3`）。
- **证照到期预警** — `GET /supplier-certificates/expiring?days=30`，天数可切
  （30 / 60 / 90 分段控件）。列：供应商（名称 + 编码）/ 证照名称 / 证照类型 /
  到期日 / 剩余天数 / 操作。剩余天数 ≤ 7 天标红、≤ 30 天标黄、其余灰。
  行内「查看供应商」跳详情。标题明确写「未来 N 天内到期」（§4.7）。

### 5.9 分类字典 `/supplier/category`

**缩进树表格**（一行一个分类，按 `parentId` 缩进 + 层级徽标），
不是可折叠的树组件 —— 分类层级浅（≤3 层），表格更好排序和批量看。
列：分类名称（缩进 + 层级徽标）/ 分类编码 / 层级 / 排序 / 启用开关 / 操作
（编辑 / ＋子分类）。

新增/编辑 Dialog 表单字段：分类编码（租户内唯一）、名称、上级分类、排序、启用状态。
**不含「层级」字段**（服务端 `resolveLevel` 覆盖，§4.8）。
启用/禁用开关必须同时回传 `categoryCode` 与 `categoryName`（§4.8）。

**不做「关联供应商」列** —— 后端没有按分类统计供应商数量的接口，
前端要算就得拉全量供应商再聚合（一页 20 条，得翻页拉完），
成本与收益不匹配。等后端补了 `GET /supplier-categories/{id}/supplier-count` 再加。

**不画「删除」按钮** —— 后端删除被引用的分类会拒绝，前端无法预判哪些被引用；
删除暂不在界面暴露，等后端给出可预判的接口（如删除返回引用数量）再补。

---

## 6. 数据流与状态

- Query keys（统一在 `hooks/` 里定义）：
  - `['suppliers', 'list', params]`
  - `['suppliers', 'detail', id]`
  - `['suppliers', 'profile', id]`
  - `['suppliers', 'performance', id]`
  - `['suppliers', 'stats']`
  - `['suppliers', 'change-logs', id, limit]`
  - `['suppliers', 'contacts', supplierId]`
  - `['suppliers', 'bank-accounts', supplierId]`
  - `['suppliers', 'certificates', supplierId]`
  - `['suppliers', 'categories', supplierId]`
  - `['supplier-categories', 'list', params]`
  - `['supplier-evaluations', 'page', supplierId, page]`
  - `['supplier-evaluations', 'by-supplier', supplierId]`
  - `['supplier-certificates', 'expiring', days, page]`
- mutation 成功后 `invalidateQueries` 掉受影响的 key
  （例如审核通过 → `['suppliers']` 前缀整体失效 + `['suppliers','stats']`）。
- 反馈：仓库无 toast 组件，成功/失败用行内 `submitError` 提示 +
  Dialog 关闭前的局部错误条；破坏性动作（删除、拉黑）走 `confirm-dialog.tsx`。
- 所有 mutation 的 catch 分支走 `toApiError`（§4.4），统一在 `lib/api-error.ts`
  里做「去掉 `Service error: ` 前缀、拆出 traceId」的清洗。

---

## 7. 与既有代码的关系

| 文件 | 改动 |
|---|---|
| `src/lib/api/contracts/supplier.ts` | 扩展 DTO + 新增子实体契约 |
| `src/lib/api/services/supplier.ts` | 扩展端点，`list`/`getById` 保持兼容 |
| `src/features/supplier/**` | 主要改动面（见 §5.4） |
| `src/app/[locale]/(app)/supplier/page.tsx` | 包 Suspense 边界（§4.9） |
| `src/app/[locale]/(app)/supplier/new/page.tsx` | 新增 |
| `src/app/[locale]/(app)/supplier/[id]/page.tsx` | 改造（分区切换、URL 参数解析） |
| `src/app/[locale]/(app)/supplier/[id]/edit/page.tsx` | 新增 |
| `src/app/[locale]/(app)/supplier/audit/page.tsx` | 新增 |
| `src/app/[locale]/(app)/supplier/category/page.tsx` | 新增 |
| `src/components/Layout/menu-config.tsx` | 供应商改为子菜单组 |
| `src/features/dashboard/services/dashboard.service.ts` | **不改**（签名兼容） |
| `src/features/warehouse/services/warehouse.service.ts` | **不改**（签名兼容） |
| 后端任何文件 | **不改** |

---

## 8. 验收标准

1. `npm run typecheck` 通过（TypeScript 7）。
2. `npm run lint` 通过（flat config，ESLint 10）。
3. `npm test` 通过；`lib/mappers.test.ts` 至少覆盖：
   `overallScore` 优先 / 缺失时退回三维度均值、
   状态字典回落、
   证照剩余天数计算、
   银行账号脱敏、
   payload mapper 的「清空字段发 `""`、未托管字段不出现」。
4. `npm run build` 通过（含 Suspense 边界）。
5. 6 条路由均可渲染。
6. **端点覆盖核对表** —— 48 个在范围端点全部在 `src/lib/api/services/supplier.ts`
   里有类型化函数；其中除下面两个外，全部有界面调用点：
   - `GET /suppliers/active` —— 跨模块的「可合作供应商」选择器数据源，
     本次只暴露契约与服务层（采购/仓储模块自行取用），**不新增界面**。
   - `GET /supplier-evaluations`（分页）—— **有界面调用点**
     （详情页绩效评价分区），见 §5.6。

   其余 46 个端点必须有明确的调用点，写在 `services/supplier.ts` 的注释里，
   便于核对。

---

## 9. 留给构建者（可自行决定的小事）

以下几项已在 2026-10-30 的评审中定稿，此处只留真正自由的部分：

- ~~列表页统计 KPI 条的具体视觉形态~~ → **已定：6 个数字卡**（§5.5）
- ~~详情页 7 个分区的布局~~ → **已定：左侧分区导航 + 右侧内容**（§5.6）
- ~~分类字典的「关联供应商」列 / 删除按钮~~ → **已定：都不做**（§5.9）
- 银行账号脱敏的默认掩码规则（建议 `**** **** **** 1234`）。
- 供应商类型（`supplierType` 1..N）的具体枚举文案 —— 后端只存整数，
  没有字典表；先按「生产型 / 贸易型 / 服务型 / 综合型」给出默认，
  后续可迁到 `scm-system` 的字典表。
- 证照「审核」按钮的交互：通过 / 驳回二选一 + 备注。
- 新建/编辑表单各分区的具体栅格列数（建议 2 列，窄屏塌成 1 列）。

---

## 10. 明确不做的事（再次强调）

- 不改后端任何一行 Java / SQL。
- 不新增 Ant Design 组件或页面。
- 不在浏览器里复刻后端的评分 / 状态机判定逻辑（仅做展示与提示）。
- 不把 `/api/v1/settlements`、`/api/v1/purchase-order-items` 接进供应商模块。
- 不做供应商自助注册门户、不做 RFQ 寻源、不做独立的绩效评价总览页。
- 不做分类的「关联供应商」统计、不在分类页暴露删除按钮。

---

## 11. 评审过程中发现的后端问题（本次不改，建议单独开 issue）

1. **变更日志的操作人永远是空的** ——
   `SupSupplierServiceImpl.createSupplier` / `updateSupplier` 硬编码
   `changeLogService.record(id, "CREATE"/"UPDATE", ..., null, null, null)`。
   建档和编辑这两类变更的 `operatorId` / `operatorName` 不管前端传什么都是 null。
   前端本次按「显示为『系统』」处理。
2. **证照预警接口看不到已过期证照** ——
   `pageExpiring` / `countExpiring` 的下界是 `validTo >= today`，
   所以 `stats.expiringCertificates` 统计的是「未来到期数」而不是「到期 + 过期数」。
   前端文案已按「30 天内到期」措辞，但指标名 `expiringCertificates` 有歧义。
3. **`supplier.overall_score` 有两套口径** ——
   迁移脚本 `V1__supplier_enterprise_management.sql` 的历史回填用 3 维简单均值，
   `refreshSupplierScorecard` 用 4 维加权。历史数据两种口径混存。
4. **分类的「删除」接口没有可预判的引用检查** ——
   后端在删除被引用的分类时抛错，前端无法提前置灰。
5. **`SupSettlementController`（8 个端点）与 `scm-finance` 的结算页职责重叠** ——
   两处都提供对账单能力，存在两套数据源的风险，需要产品层面确认归属。

---

## 12. 评审记录

2026-10-30，与需求方逐项过完，原型在 `.superpowers/brainstorm/`（已 gitignore）。

| 决策点 | 结论 |
|---|---|
| 详情页 7 个分区的布局 | **方案 A** —— 左侧分区导航（带条目数）+ 右侧内容 |
| 列表页 KPI 条形态 | 6 个数字卡，可点击直达筛选 |
| 列表页行操作形态 | 2 个图标快捷键（查看/编辑）+ `⋯` 下拉菜单 |
| 分类字典的「关联供应商」列 | **不做** —— 后端无该统计接口，前端聚合成本过高 |
| 分类字典的「删除」按钮 | **不做** —— 后端无法预判引用，点了才报错 |
| 证照预警看板文案 | 「未来 N 天内到期」；已过期只在详情页标红 |
| 供应商类型枚举文案 | 先用「生产型/贸易型/服务型/综合型」，后续迁 `scm-system` 字典表 |

设计文档已定稿。下一步：`superpowers:writing-plans` 出实施计划。
