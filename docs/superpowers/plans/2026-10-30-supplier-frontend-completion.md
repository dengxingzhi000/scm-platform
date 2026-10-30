# 供应商管理前端补齐 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 `scm-supplier` 后端已有的 48 个企业级端点完整呈现在 `scm-web` 前端 —— 从「通讯录」升级成带准入审核、风险管控、证照预警、绩效评价的管理台。

**Architecture:** 契约层（`lib/api/contracts/supplier.ts`）→ 端点层（`lib/api/services/supplier.ts`）→ feature 层（`features/supplier/`，含 `lib/` 纯函数 + `hooks/` 查询变更 + `components/` 页面）。所有 DTO→UI 的转换集中在 `lib/mappers.ts`，所有枚举文案集中在 `lib/constants.ts`，所有后端错误文案清洗集中在 `lib/api-error.ts`。页面路由在 `src/app/[locale]/(app)/supplier/` 下，与既有 `/product` 的路由 + shadcn/ui 结构一致。

**Tech Stack:** Next.js 16.4 App Router / React 19 / TypeScript 7 / TanStack Query v5 / TanStack Table v9 / React Hook Form + Zod / shadcn/ui + Tailwind v4 / Vitest + @testing-library/react

**Spec:** `docs/superpowers/specs/2026-10-30-supplier-frontend-completion-design.md` —— 实施者必须先读这份 spec，plan 只写 spec 没定的选择（文件名、函数签名、测试名、具体数值）。

---

## Global Constraints

以下约束来自 spec，**每个 task 都隐含包含**：

- **后端一行不改。** 本次是纯前端补齐。
- **不新增 Ant Design 组件或页面。** 供应商模块全程用 shadcn/ui（`src/components/ui`）。`menu-config.tsx` 本身是 antd 的 `MenuProps`，保持原样，只改结构。
- **不加新依赖。** 仓库没有 tabs / toast / date-picker / multi-select 组件：分区切换用受控 state + 普通按钮，多选用 `checkbox.tsx`，反馈用行内 `submitError` 提示 + `confirm-dialog.tsx`，日期用 `<input type="date">`。
- **不在前端复刻后端的评分或状态机判定。** 分级阈值 90/80/70、加权 30/30/20/20、合作状态流转矩阵 `0→1, 1→2|3, 2→1|3, 3→0` 全部由服务端裁决。前端只做基于**当前状态**的布尔型前置提示（spec §4.5）。
- **`PUT /api/v1/suppliers/{id}` 是 PATCH 语义**（MyBatis-Plus `update-strategy: NOT_NULL`）：`null`/`undefined` = 保持原值，`""` = 写入空串。表单清空字段必须发 `""`。
- **8 个变更端点里 7 个走 query param**，只有 `PUT /{id}/audit` 和 `PUT /{id}/categories` 用 body（且 categories 的 body 是**裸数组**）。见 spec §4.1 的完整对照表。
- **所有 mutation 的错误路径必须走 `toApiError`**，不能用 `error.message`（后端 4xx 不会被 `unwrap` 捕获）。
- **操作人**：`operatorId` = `useAuthStore().user.id`，`operatorName` = `user.displayName || user.username || ""`，统一由 `useOperator()` 提供。
- **路由不带 locale 前缀**（`/supplier/xxx`），与既有 supplier / product / order / purchase 页面一致。
- **`supplier` 模块的所有页面组件必须是 `"use client"`**。
- **接口入参是 `page` / `size`**（不是 `pageNum`），返回是 MyBatis-Plus 原始 `Page`（`records/total/size/current`），用 `contracts/page.ts` 的 `MpPage<T>` + `toUiPage`。
- **命令**（在 `scm-web/` 目录下执行）：
  - 单测：`npx vitest run src/features/supplier/lib/mappers.test.ts`
  - 全量单测：`npm test`
  - 类型：`npm run typecheck`（**不要**用 `npx tsc`，`.bin/tsc` 指向 TS 6 shim）
  - Lint：`npm run lint`
  - 构建：`npm run build`

---

## Review Focus

以下是 spec 暗示、但任何单个 task 的测试都不会自动覆盖、且最容易咬到真实用户的 5 个输入/状况。每个都在下方对应 task 里有钉住它的测试。

1. **用户清空一个可选字段（备注 / 官网 / 英文名）后保存** —— 期望旧值真的被清掉，而不是"保存成功但值还在"。原因：`PUT /{id}` 是 PATCH 语义，发 `null` 等于不写。→ Task 4 的 `supplier-form-schema.test.ts` 钉住。
2. **后端拒绝一个操作时（合作中的供应商不能删除 / 税号重复 / 分类编码重复）** —— 期望用户看到后端的中文原因，而不是 `Request failed with status code 400`。原因：`ServiceException` 被转成 4xx，`unwrap` 不执行。→ Task 3 的 `api-error.test.ts` 钉住。
3. **税号未补全的供应商出现在待审核队列** —— 期望"通过"按钮不可点且说明原因，后端 `submitAudit` 会拒绝。→ Task 12 的 `supplier-audit-workbench.test.tsx` 钉住。
4. **供应商只关联了一个分类，运营想改成关联另外两个** —— 期望提交的 body 是裸数组 `["c1","c2"]`，不是 `{categoryIds:[...]}`（后者会被后端判为"存在无效的供应商分类"）。→ Task 1 的 `supplier.test.ts` 钉住。
5. **供应商有 5 个资质证照，其中一个 3 天后过期、另一个 20 天前就过期了** —— 期望预警看板只出现"3 天后过期"那一个（后端 `pageExpiring` 的下界是 `validTo >= 今天`），已过期的那张只在详情页的证照分区标红。→ Task 12 的 `supplier-audit-workbench.test.tsx` 钉住。

---

## File Structure

| 文件 | 职责 |
|---|---|
| `src/lib/api/contracts/supplier.ts` | 全部 DTO 与 payload 类型（后端驼峰，不做转换） |
| `src/lib/api/services/supplier.ts` | 48 个端点的请求封装 |
| `src/features/supplier/lib/constants.ts` | 枚举字典（分级/准入/风险/合作/证照类型/证照状态/联系角色/变更类型/供应商类型）+ 状态→Badge variant 映射 |
| `src/features/supplier/lib/mappers.ts` | DTO → UI 模型；分页、日期、星级、脱敏、剩余天数 |
| `src/features/supplier/lib/api-error.ts` | `toApiError` 封装 + 清洗 `Service error: ` 前缀 / 拆 traceId |
| `src/features/supplier/lib/supplier-form-schema.ts` | Zod schema + 表单值 ↔ `SupplierUpsertPayload` 映射 |
| `src/features/supplier/lib/query-keys.ts` | 所有 query key 工厂（单一来源） |
| `src/features/supplier/types/supplier.types.ts` | UI 模型类型 |
| `src/features/supplier/hooks/use-supplier.ts` | 供应商主档 query + mutation |
| `src/features/supplier/hooks/use-supplier-contact.ts` | 联系人 query + mutation |
| `src/features/supplier/hooks/use-supplier-bank-account.ts` | 银行账户 query + mutation |
| `src/features/supplier/hooks/use-supplier-certificate.ts` | 资质证照 query + mutation |
| `src/features/supplier/hooks/use-supplier-category.ts` | 分类字典 query + mutation |
| `src/features/supplier/hooks/use-supplier-evaluation.ts` | 绩效评价 query + mutation |
| `src/features/supplier/hooks/use-operator.ts` | 统一的 operatorId / operatorName |
| `src/features/supplier/components/supplier-list.tsx` | 列表页（KPI + 筛选 + 表格 + 行操作） |
| `src/features/supplier/components/supplier-stats.tsx` | 6 个 KPI 数字卡 |
| `src/features/supplier/components/supplier-filters.tsx` | 筛选栏 + URL 同步 |
| `src/features/supplier/components/supplier-actions.tsx` | 行操作菜单 + 各动作 Dialog |
| `src/features/supplier/components/supplier-detail.tsx` | 360° 画像外壳（左导航 + 右内容） |
| `src/features/supplier/components/supplier-overview-panel.tsx` | 概览 + 绩效评分卡 |
| `src/features/supplier/components/supplier-category-panel.tsx` | 分类关联（复选框多选） |
| `src/features/supplier/components/supplier-contact-panel.tsx` | 联系人 CRUD |
| `src/features/supplier/components/supplier-bank-account-panel.tsx` | 银行账户 CRUD |
| `src/features/supplier/components/supplier-certificate-panel.tsx` | 资质证照 CRUD + 审核 |
| `src/features/supplier/components/supplier-evaluation-panel.tsx` | 绩效评价 CRUD（分页） |
| `src/features/supplier/components/supplier-change-log-panel.tsx` | 变更留痕时间线 |
| `src/features/supplier/components/supplier-form.tsx` | 新建/编辑表单 |
| `src/features/supplier/components/supplier-audit-workbench.tsx` | 准入审核工作台 |
| `src/features/supplier/components/supplier-category-manage.tsx` | 分类字典管理 |
| `src/app/[locale]/(app)/supplier/page.tsx` | 列表路由（包 Suspense） |
| `src/app/[locale]/(app)/supplier/new/page.tsx` | 新建路由 |
| `src/app/[locale]/(app)/supplier/[id]/page.tsx` | 详情路由 |
| `src/app/[locale]/(app)/supplier/[id]/edit/page.tsx` | 编辑路由 |
| `src/app/[locale]/(app)/supplier/audit/page.tsx` | 审核工作台路由 |
| `src/app/[locale]/(app)/supplier/category/page.tsx` | 分类字典路由 |
| `src/components/Layout/menu-config.tsx` | 供应商改为子菜单组 |

---

### Task 1: API 层 —— 契约类型 + 48 个端点

**Files:**
- Modify: `src/lib/api/contracts/supplier.ts`
- Modify: `src/lib/api/services/supplier.ts`
- Test: `src/lib/api/services/supplier.test.ts`

**Interfaces:**
- Consumes: 无（起点）
- Produces:
  - `supplierApiService.list(query: SupplierPageQuery): Promise<SupplierPage>` —— **签名必须保持向后兼容**，
    `features/dashboard/services/dashboard.service.ts` 与 `features/warehouse/services/warehouse.service.ts`
    依赖它。`SupplierPageQuery` 是 `SupplierQuery` 的超集（只多可选字段），
    所以老调用方传 `{ page, size, keyword?, enabled? }` 依然合法
  - `supplierApiService.getById(id: string): Promise<SupplierDTO>` —— 同上，签名不变
  - `supplierApiService.stats(): Promise<SupplierStatsDTO>`
  - `supplierApiService.profile(id: string): Promise<SupplierProfileDTO>`
  - `supplierApiService.performance(id: string): Promise<SupplierPerformanceDTO>`
  - `supplierApiService.changeLogs(id: string, limit?: number): Promise<SupplierChangeLogDTO[]>`
  - `supplierApiService.active(): Promise<SupplierDTO[]>`
  - `supplierApiService.search(name: string): Promise<SupplierDTO[]>`
  - `supplierApiService.create(payload: SupplierUpsertPayload): Promise<SupplierDTO>`
  - `supplierApiService.update(id: string, payload: SupplierUpsertPayload): Promise<SupplierDTO>`
  - `supplierApiService.remove(id: string, operator?: OperatorParams): Promise<void>`
  - `supplierApiService.enable(id: string): Promise<void>`
  - `supplierApiService.disable(id: string): Promise<void>`
  - `supplierApiService.submitAudit(id: string, operator?: OperatorParams): Promise<void>`
  - `supplierApiService.audit(id: string, payload: SupplierAuditPayload): Promise<void>`
  - `supplierApiService.blacklist(id: string, reason: string, operator?: OperatorParams): Promise<void>`
  - `supplierApiService.unblacklist(id: string, operator?: OperatorParams): Promise<void>`
  - `supplierApiService.changeRiskLevel(id: string, level: number, reason?: string, operator?: OperatorParams): Promise<void>`
  - `supplierApiService.changeCooperationStatus(id: string, status: number, operator?: OperatorParams): Promise<void>`
  - `supplierApiService.getCategories(id: string): Promise<SupplierCategoryDTO[]>`
  - `supplierApiService.assignCategories(id: string, categoryIds: string[], operator?: OperatorParams): Promise<void>` —— **body 是裸数组**
  - `supplierApiService.contacts.list(supplierId): Promise<SupplierContactDTO[]>` / `.create(supplierId, data): Promise<SupplierContactDTO>` / `.update(supplierId, id, data): Promise<SupplierContactDTO>` / `.remove(supplierId, id): Promise<void>` / `.setPrimary(supplierId, id): Promise<void>`
  - `supplierApiService.bankAccounts.list/create/update/remove/setDefault` —— 同上形状，`setDefault` 的路径是 `{id}/default`
  - `supplierApiService.certificates.list/create/update/remove` + `.verify(supplierId, id, params: { approved: boolean; operatorId?: string; operatorName?: string; remark?: string }): Promise<void>`
  - `supplierApiService.certificateExpiry.expiring(query: { page: number; size: number; days: number }): Promise<MpPage<SupplierCertificateDTO>>`
  - `supplierApiService.categories.list(query?: { keyword?: string; enabled?: boolean }): Promise<SupplierCategoryDTO[]>` / `.create(data): Promise<SupplierCategoryDTO>` / `.update(id, data): Promise<SupplierCategoryDTO>` / `.remove(id): Promise<void>`
  - `supplierApiService.evaluations.page(query: { page: number; size: number; supplierId?: string; evaluationPeriod?: string }): Promise<MpPage<SupplierEvaluationDTO>>` / `.create(data): Promise<SupplierEvaluationDTO>` / `.update(id, data): Promise<SupplierEvaluationDTO>` / `.remove(id): Promise<void>` / `.bySupplier(supplierId): Promise<SupplierEvaluationDTO[]>` / `.averageScore(supplierId): Promise<number>`

- [ ] **Step 1: 写失败的测试 —— 断言每个端点的 URL / method / 参数位置**

在 `src/lib/api/services/supplier.test.ts` 里 mock axios 客户端：

```ts
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}));

vi.mock("@/lib/api/client", () => ({
  default: { get: h.get, post: h.post, put: h.put, delete: h.delete },
}));

import { supplierApiService } from "./supplier";
```

（`apiClient` 的响应拦截器会返回 `response.data`，所以 mock 直接返回业务体即可；`unwrap` 遇到没有 `code` 的对象会原样返回。）

每个用例前 `beforeEach` 把四个 mock 的返回值设为 `{ code: 200, data: [] }` 之类。

必须覆盖的断言（每条一个 `it`）：

- `it("sends the supplier filters as query params")` —— 调 `supplierApiService.list({ page: 2, size: 10, keyword: "华强", supplierType: 1, cooperationStatus: 1, enabled: true, auditStatus: 2, supplierLevel: 1, riskLevel: 2, blacklisted: false, categoryId: "c-1" })`，断言 `h.get` 收到 `"/scm-supplier/api/v1/suppliers"` 与 `{ params: { page: 2, size: 10, keyword: "华强", supplierType: 1, cooperationStatus: 1, enabled: true, auditStatus: 2, supplierLevel: 1, riskLevel: 2, blacklisted: false, categoryId: "c-1" } }`
- `it("puts the blacklist reason in a query param, not a body")` —— `supplierApiService.blacklist("s-1", "资质过期", { operatorId: "u-1", operatorName: "张三" })`，断言 `h.put` 的第一个参数是 `"/scm-supplier/api/v1/suppliers/s-1/blacklist"`、第二个参数是 `undefined`（没有 body）、第三个参数 `params` 里含 `reason: "资质过期"`、`operatorId: "u-1"`、`operatorName: "张三"`
- `it("puts the risk level and its reason in query params")` —— `changeRiskLevel("s-1", 3, "交付延迟", { operatorId: "u-1" })` → URL 结尾 `/risk-level`，`params` 含 `level: 3`、`reason: "交付延迟"`、`operatorId: "u-1"`
- `it("puts the cooperation status in a query param")` —— `changeCooperationStatus("s-1", 1, { operatorId: "u-1" })` → URL 结尾 `/cooperation-status`，`params.status === 1`
- `it("sends the audit decision as a json body")` —— `audit("s-1", { action: "APPROVE", operatorId: "u-1", operatorName: "张三", remark: "资料齐全" })` → URL 结尾 `/audit`，body 恰为 `{ action: "APPROVE", operatorId: "u-1", operatorName: "张三", remark: "资料齐全" }`
- `it("sends the category ids as a bare array")` —— `assignCategories("s-1", ["c-1", "c-2"])` → `h.put` 的 body 必须是 `["c-1", "c-2"]`（用 `toEqual` 断言数组本身，不是对象）
- `it("sends the submit-audit operator as query params with no body")` —— `submitAudit("s-1", { operatorId: "u-1", operatorName: "张三" })` → URL 结尾 `/submit-audit`，body 为 `undefined`，`params` 含 operator 两个字段
- `it("sends the certificate verification decision as query params")` —— `certificates.verify("s-1", "cert-1", { approved: true, operatorId: "u-1", remark: "已核验" })` → URL 为 `"/scm-supplier/api/v1/suppliers/s-1/certificates/cert-1/verify"`，`params.approved === true`
- `it("sends the operator as query params when deleting")` —— `remove("s-1", { operatorId: "u-1", operatorName: "张三" })` → `h.delete` 的 URL 是 `"/scm-supplier/api/v1/suppliers/s-1"`，`config.params` 含 operator 两个字段
- `it("keeps the legacy list and getById signatures")` —— `list({ page: 1, size: 20 })` → `h.get` 的 URL 是 `"/scm-supplier/api/v1/suppliers"`；`getById("s-1")` → URL 是 `"/scm-supplier/api/v1/suppliers/s-1"`
- `it("reads the certificate expiry window from query params")` —— `certificateExpiry.expiring({ page: 1, size: 10, days: 60 })` → URL `"/scm-supplier/api/v1/supplier-certificates/expiring"`，`params` 含 `days: 60`
- `it("reads the evaluation page from query params")` —— `evaluations.page({ page: 1, size: 10, supplierId: "s-1" })` → URL `"/scm-supplier/api/v1/supplier-evaluations"`，`params.supplierId === "s-1"`

- [ ] **Step 2: 跑测试确认失败**

Run: `npx vitest run src/lib/api/services/supplier.test.ts`
Expected: FAIL —— 报 `supplierApiService.page is not a function` 之类的错误

- [ ] **Step 3: 扩展 `src/lib/api/contracts/supplier.ts`**

保留现有 `SupplierDTO` / `SupplierPage` / `SupplierQuery` 不动，把 `SupplierDTO` 补齐到 `SupSupplier` 实体的全字段（驼峰，**全部可选** `?: T | null`）：

```
id, tenantId, supplierCode, supplierName, supplierNameEn, supplierType,
businessType, legalPerson, registeredCapital, registrationNo, taxNo,
establishmentDate, contactName, contactPhone, contactEmail, contactAddress,
bankName, bankAccount, bankAccountName, creditRating, qualityScore,
deliveryScore, serviceScore, cooperationStartDate, cooperationStatus,
paymentTerms, paymentMethod, businessLicense, certificates, website,
supplierLevel, auditStatus, auditRemark, auditedBy, auditedAt, riskLevel,
blacklisted, blacklistReason, blacklistedAt, overallScore, lastEvaluationAt,
enabled, sortOrder, createTime, updateTime, remark
```

新增以下类型（字段名与后端实体严格一致）：

- `SupplierContactDTO`：`id, supplierId, contactName, contactTitle, contactRole, contactPhone, contactEmail, contactAddress, isPrimary, enabled, sortOrder, createTime, remark`
- `SupplierBankAccountDTO`：`id, supplierId, bankName, bankBranch, bankAccount, accountName, currency, swiftCode, isDefault, enabled, sortOrder, createTime, remark`
- `SupplierCertificateDTO`：`id, supplierId, certificateType, certificateName, certificateNo, issuingAuthority, validFrom, validTo, fileUrl, status, verifiedBy, verifiedAt, createTime, remark, daysToExpiry, supplierName`
- `SupplierCategoryDTO`：`id, categoryCode, categoryName, parentId, level, enabled, sortOrder, createTime, remark`
- `SupplierEvaluationDTO`：`id, supplierId, evaluationPeriod, startDate, endDate, qualityScore, deliveryScore, serviceScore, priceScore, totalScore, purchaseCount, totalAmount, onTimeDeliveryRate, qualityPassRate, evaluatorId, evaluatorName, evaluatedAt, improvementSuggestions, createTime, remark`
- `SupplierChangeLogDTO`：`id, supplierId, changeType, changeContent, reason, operatorId, operatorName, createTime`
- `SupplierProfileDTO`：`supplier: SupplierDTO; contacts: SupplierContactDTO[]; bankAccounts: SupplierBankAccountDTO[]; certificates: SupplierCertificateDTO[]; categories: SupplierCategoryDTO[]; changeLogs: SupplierChangeLogDTO[]`
- `SupplierStatsDTO`：`total, enabled, cooperating, pendingAudit, blacklisted, expiringCertificates: number; byCooperationStatus: Record<number, number>; byAuditStatus: Record<number, number>`
- `SupplierPerformanceDTO`：`supplierId, supplierName, supplierLevel, overallScore, qualityScore, deliveryScore, serviceScore, priceScore, evaluationCount, latestPeriod, latestTotalScore, latestEvaluatedAt, suggestedLevel, recentEvaluations: SupplierEvaluationDTO[]`
- `SupplierPageQuery`：`page: number; size: number; keyword?: string; supplierType?: number; cooperationStatus?: number; enabled?: boolean; auditStatus?: number; supplierLevel?: number; riskLevel?: number; blacklisted?: boolean; categoryId?: string`
- `SupplierQuery` —— **保留为 `SupplierPageQuery` 的别名**（`export type SupplierQuery = SupplierPageQuery;`），
  既有引用 `SupplierQuery` 的代码不用改
- `OperatorParams`：`operatorId?: string; operatorName?: string`
- `SupplierAuditPayload`：`action: "APPROVE" | "REJECT"; operatorId?: string; operatorName?: string; remark?: string`
- `SupplierUpsertPayload`：`supplierCode?: string; supplierName: string; supplierNameEn?: string; supplierType?: number; businessType?: string; legalPerson?: string; registeredCapital?: number; registrationNo?: string; taxNo?: string; establishmentDate?: string; contactName?: string; contactPhone?: string; contactEmail?: string; contactAddress?: string; bankName?: string; bankAccount?: string; bankAccountName?: string; creditRating?: string; cooperationStartDate?: string; paymentTerms?: string; paymentMethod?: string; businessLicense?: string; certificates?: string; website?: string; sortOrder?: number; remark?: string`
  —— **注意 `cooperationStartDate` 在类型里存在（后端接受），但表单不托管它**（spec §4.2）

- [ ] **Step 4: 扩展 `src/lib/api/services/supplier.ts`**

保留现有 `list` / `getById` 的实现不动。在文件顶部定义四个 base 常量：

```ts
const SUPPLIER_BASE = "/scm-supplier/api/v1/suppliers";
const CATEGORY_BASE = "/scm-supplier/api/v1/supplier-categories";
const EVALUATION_BASE = "/scm-supplier/api/v1/supplier-evaluations";
const CERTIFICATE_EXPIRY_BASE = "/scm-supplier/api/v1/supplier-certificates";
```

用一个 `params` helper 把 `undefined` 的字段剔除（避免发出 `?keyword=undefined`）：

```ts
const clean = <T extends Record<string, unknown>>(input: T): T =>
  Object.fromEntries(Object.entries(input).filter(([, v]) => v !== undefined && v !== null)) as T;
```

每个端点严格按 spec §4.1 的对照表选择参数位置：
- body 的：`create` / `update` / `audit` / `assignCategories` / `contacts.create` / `contacts.update` / `bankAccounts.create` / `bankAccounts.update` / `certificates.create` / `certificates.update` / `categories.create` / `categories.update` / `evaluations.create` / `evaluations.update`
- query param 的：`list` / `stats` / `profile` / `performance` / `changeLogs` / `active` / `search` / `remove` / `enable` / `disable` / `submitAudit` / `blacklist` / `unblacklist` / `changeRiskLevel` / `changeCooperationStatus` / `getCategories` / `contacts.list` / `contacts.remove` / `contacts.setPrimary` / `bankAccounts.list` / `bankAccounts.remove` / `bankAccounts.setDefault` / `certificates.list` / `certificates.verify` / `certificates.remove` / `certificateExpiry.expiring` / `categories.list` / `categories.remove` / `evaluations.page` / `evaluations.remove` / `evaluations.bySupplier` / `evaluations.averageScore`

**query-param 端点的 axios 调用形状固定如下**（body 位置传 `undefined`）：

```ts
apiClient.put(url, undefined, { params: clean({ ... }) })   // PUT
apiClient.delete(url, { params: clean({ ... }) })             // DELETE
apiClient.get(url, { params: clean({ ... }) })                // GET
```

（`apiClient.put(url, { params })` 会把 params 塞进 body，是错的。）

`unwrap` 的用法沿用现有风格：`unwrap<T>(apiClient.get(url, { params }))`。Void 端点声明 `Promise<void>` 并忽略返回值（`unwrap` 对没有 `data` 键的 `ApiResponse` 返回 `undefined`）。

- [ ] **Step 5: 跑测试确认通过**

Run: `npx vitest run src/lib/api/services/supplier.test.ts`
Expected: PASS（12 个用例全绿）

- [ ] **Step 6: 类型检查**

Run: `npm run typecheck`
Expected: 无错误

- [ ] **Step 7: 提交**

```bash
git add src/lib/api/contracts/supplier.ts src/lib/api/services/supplier.ts src/lib/api/services/supplier.test.ts
git commit -m "feat(supplier): cover the full scm-supplier API surface"
```

---

### Task 2: 枚举字典 + UI 模型 + 映射层

**Files:**
- Create: `src/features/supplier/lib/constants.ts`
- Modify: `src/features/supplier/types/supplier.types.ts`
- Modify: `src/features/supplier/lib/mappers.ts`
- Modify: `src/features/supplier/lib/mappers.test.ts`

**Interfaces:**
- Consumes: Task 1 的 `SupplierDTO` / `SupplierContactDTO` / `SupplierBankAccountDTO` / `SupplierCertificateDTO` / `SupplierCategoryDTO` / `SupplierEvaluationDTO` / `SupplierChangeLogDTO` / `SupplierStatsDTO` / `SupplierPerformanceDTO`
- Produces:
  - `constants.ts` 导出：
    - `SUPPLIER_LEVELS: ReadonlyArray<{ value: number; label: string; variant: BadgeVariant }>` —— 1 战略 / 2 优选 / 3 合格 / 4 备选
    - `AUDIT_STATUSES` —— 0 草稿 / 1 待审核 / 2 已通过 / 3 已驳回
    - `RISK_LEVELS` —— 1 低 / 2 中 / 3 高
    - `COOPERATION_STATUSES` —— 0 潜在 / 1 合作中 / 2 暂停 / 3 终止
    - `CERTIFICATE_TYPES` —— BUSINESS_LICENSE 营业执照 / TAX_REGISTRATION 税务登记 / QUALITY_ISO 质量体系 / PRODUCTION_LICENSE 生产许可 / AGENT_AUTHORIZATION 代理授权 / OTHER 其他
    - `CERTIFICATE_STATUSES` —— 0 待审核 / 1 有效 / 2 即将到期 / 3 已过期 / 4 已驳回
    - `CONTACT_ROLES` —— SALES 销售 / FINANCE 财务 / LOGISTICS 物流 / TECH 技术 / OTHER 其他
    - `CHANGE_TYPE_LABELS: Record<string, string>` —— CREATE / UPDATE / DELETE / SUBMIT_AUDIT / AUDIT_APPROVE / AUDIT_REJECT / ENABLE / DISABLE / BLACKLIST / UNBLACKLIST / RISK_LEVEL / COOPERATION_STATUS / CATEGORY_ASSIGN
    - `SUPPLIER_TYPES` —— 1 生产型 / 2 贸易型 / 3 服务型 / 4 综合型
    - `labelOf(dictionary, value): string` —— 查不到时返回 `"-"`
    - `variantOf(dictionary, value): BadgeVariant` —— 查不到时返回 `"secondary"`
    - `BadgeVariant` = `"default" | "secondary" | "destructive" | "outline" | "success" | "warning" | "info"`（与 `components/ui/badge.tsx` 的 `badgeVariants` 取值一致）
  - `types/supplier.types.ts` 的 `Supplier` 接口扩展为：
    ```ts
    export interface Supplier {
      id: string;
      name: string;
      code: string;
      contactPerson: string;
      contactPhone: string;
      email?: string;
      address?: string;
      status: "active" | "disabled";
      rating: number;            // 0–5
      overallScore?: number;      // 0–100
      supplierLevel?: number;
      auditStatus?: number;
      riskLevel?: number;
      blacklisted: boolean;
      cooperationStatus?: number;
      creditRating?: string;
      taxNo?: string;
      supplierType?: number;
      createdAt: string;
      remark?: string;
    }
    ```
    并新增 `SupplierContact` / `SupplierBankAccount` / `SupplierCertificate` / `SupplierCategory` / `SupplierEvaluation` / `SupplierChangeLog` / `SupplierStats` / `SupplierPerformance` / `SupplierProfile` 六个 UI 模型（字段与 DTO 同名同义，`SupplierCertificate` 额外有 `daysToExpiry: number | null`）
  - `mappers.ts` 导出：
    - `toSupplier(dto: SupplierDTO): Supplier` —— **行为变更**：`rating` 优先取 `overallScore / 20`（保留 1 位小数），`overallScore` 为空时才退回现有的三维度均值 / 20
    - `toSupplierPage(page): PageResult<Supplier>` —— 签名不变
    - `toSupplierContact(dto): SupplierContact`
    - `toSupplierBankAccount(dto): SupplierBankAccount` —— `bankAccount` 字段保留原值，脱敏在展示层做
    - `toSupplierCertificate(dto): SupplierCertificate`
    - `toSupplierCategory(dto): SupplierCategory`
    - `toSupplierEvaluation(dto): SupplierEvaluation`
    - `toSupplierChangeLog(dto): SupplierChangeLog` —— `operatorName` 为空时给 `"系统"`
    - `toSupplierStats(dto): SupplierStats`
    - `toSupplierPerformance(dto): SupplierPerformance`
    - `toSupplierProfile(dto): SupplierProfile`
    - `maskBankAccount(account?: string | null): string` —— `undefined`/`null`/空 → `"-"`；长度 ≤ 4 → 全掩码 `"****"`；否则保留后 4 位，前面的字符换算成 `*` 后每 4 位加一个空格
    - `daysToExpiryOf(validTo?: string | null, today?: Date): number | null` —— 距到期天数；`validTo` 为空 → `null`；已过期 → 负数

- [ ] **Step 1: 写失败的测试**

在 `src/features/supplier/lib/mappers.test.ts` 追加（保留现有 3 个用例不动）：

```ts
it("prefers overallScore over the three-dimension average for the star rating", () => {
  const supplier = toSupplier({ ...supplierFixture, overallScore: 92, qualityScore: 10, deliveryScore: 10, serviceScore: 10 });
  expect(supplier.rating).toBe(4.6);   // 92 / 20
  expect(supplier.overallScore).toBe(92);
});

it("falls back to the three-dimension average when overallScore is missing", () => {
  const supplier = toSupplier({ ...supplierFixture, overallScore: null });
  expect(supplier.rating).toBe(4.8);   // 96/94/98 平均 96 → 96/20 = 4.8
});

it("defaults the risk and blacklist state when the backend omits them", () => {
  const supplier = toSupplier({ ...supplierFixture, riskLevel: null, blacklisted: null });
  expect(supplier.blacklisted).toBe(false);
  expect(supplier.riskLevel).toBeUndefined();
});

it("masks a bank account but keeps the last four digits", () => {
  expect(maskBankAccount("6222021234567890")).toBe("**** **** **** 7890");
  expect(maskBankAccount("123")).toBe("****");
  expect(maskBankAccount(undefined)).toBe("-");
});

it("counts the days until a certificate expires, negative once expired", () => {
  expect(daysToExpiryOf("2026-11-30", new Date("2026-10-30T00:00:00Z"))).toBe(31);
  expect(daysToExpiryOf("2026-10-20", new Date("2026-10-30T00:00:00Z"))).toBe(-10);
  expect(daysToExpiryOf(null, new Date("2026-10-30T00:00:00Z"))).toBeNull();
});

it("labels a change log with no operator as the system", () => {
  const log = toSupplierChangeLog({ id: "l-1", supplierId: "s-1", changeType: "CREATE", changeContent: "创建", createTime: "2026-10-30T09:00:00" });
  expect(log.operatorName).toBe("系统");
});

it("maps an unknown enum value to a dash instead of crashing", () => {
  expect(labelOf(AUDIT_STATUSES, 99)).toBe("-");
  expect(labelOf(AUDIT_STATUSES, 2)).toBe("已通过");
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npx vitest run src/features/supplier/lib/mappers.test.ts`
Expected: FAIL —— 报 `maskBankAccount is not exported` 之类

- [ ] **Step 3: 创建 `src/features/supplier/lib/constants.ts`**

按 Interfaces 段列出全部字典。每个字典项形如 `{ value: 1, label: "战略", variant: "default" }`。
Badge variant 分配（与 spec 原型一致）：

| 字典 | variant 分配 |
|---|---|
| 分级 | 1 战略 → `default`（主色）；2 优选 → `success`；3 合格 → `secondary`；4 备选 → `secondary` |
| 准入状态 | 0 草稿 → `secondary`；1 待审核 → `warning`；2 已通过 → `success`；3 已驳回 → `destructive` |
| 风险等级 | 1 低 → `secondary`；2 中 → `warning`；3 高 → `destructive` |
| 合作状态 | 0 潜在 → `secondary`；1 合作中 → `default`；2 暂停 → `warning`；3 终止 → `destructive` |
| 证照状态 | 0 待审核 → `secondary`；1 有效 → `success`；2 即将到期 → `warning`；3 已过期 → `destructive`；4 已驳回 → `destructive` |
| 证照类型 / 联系角色 | 全部 `secondary` |

同时导出 `labelOf` 与 `variantOf`：

```ts
export const labelOf = <T extends { value: number | string }>(
  dictionary: ReadonlyArray<T>,
  value?: number | string | null,
): string =>
  dictionary.find((item) => item.value === value)?.label ?? "-";

export const variantOf = <T extends { value: number | string; variant: BadgeVariant }>(
  dictionary: ReadonlyArray<T>,
  value?: number | string | null,
): BadgeVariant =>
  dictionary.find((item) => item.value === value)?.variant ?? "secondary";
```

- [ ] **Step 4: 扩展 `src/features/supplier/types/supplier.types.ts`**

按 Interfaces 段写出 `Supplier`（扩展）与 8 个新 UI 模型。子实体的字段直接沿用 DTO 的字段名与类型，`date` 类型的字段（如 `establishmentDate`、`validFrom`、`validTo`）在 UI 模型里仍为 `string | null`（原始 ISO 串），格式化在展示层用 `dayjs`（`mappers.ts` 已依赖 `dayjs`）。

- [ ] **Step 5: 扩展 `src/features/supplier/lib/mappers.ts`**

- `toSupplier`：新增 `overallScore`、`supplierLevel`、`auditStatus`、`riskLevel`、`blacklisted`、`cooperationStatus`、`creditRating`、`taxNo`、`supplierType` 的映射（缺失一律 `undefined`，`blacklisted` 缺失时给 `false`）。
  `rating` 计算改为：
  ```ts
  const toRating = (dto: SupplierDTO): number => {
    if (typeof dto.overallScore === "number") {
      return Math.round((dto.overallScore / 20) * 10) / 10;
    }
    const scores = [dto.qualityScore, dto.deliveryScore, dto.serviceScore].filter(
      (score): score is number => typeof score === "number"
    );
    if (scores.length === 0) return 0;
    const average = scores.reduce((sum, score) => sum + score, 0) / scores.length;
    return Math.round((average / 20) * 10) / 10;
  };
  ```
- 新增 `maskBankAccount` 与 `daysToExpiryOf`：
  ```ts
  // 掩掉除后 4 位以外的所有数字，每 4 位一组用 "****" 表示。
  // "6222021234567890"（16 位）→ 掩掉 12 位 → 3 组 → "**** **** **** 7890"
  export const maskBankAccount = (account?: string | null): string => {
    if (!account) return "-";
    if (account.length <= 4) return "****";
    const maskedLength = account.length - 4;
    const groups = Math.ceil(maskedLength / 4);
    const mask = Array.from({ length: groups }, () => "****").join(" ");
    return `${mask} ${account.slice(-4)}`;
  };
  ```
  `daysToExpiryOf(validTo, today)` 用 `dayjs(validTo).startOf("day").diff(dayjs(today).startOf("day"), "day")`；
- 新增各子实体的 `toXxx` 单参映射函数；`toSupplierChangeLog` 的 `operatorName` 为空时给 `"系统"`。
- `toSupplierStats` / `toSupplierPerformance` / `toSupplierProfile` 按字段直译。

- [ ] **Step 6: 跑测试确认通过**

Run: `npx vitest run src/features/supplier/lib/mappers.test.ts`
Expected: PASS（原有 3 个 + 新增 7 个全绿）

- [ ] **Step 7: 类型检查 + 提交**

Run: `npm run typecheck` → 无错误

```bash
git add src/features/supplier/lib/constants.ts src/features/supplier/lib/mappers.ts src/features/supplier/lib/mappers.test.ts src/features/supplier/types/supplier.types.ts
git commit -m "feat(supplier): enum dictionaries and DTO to UI mappers"
```

---

### Task 3: 后端错误清洗 + 操作人

**Files:**
- Create: `src/features/supplier/lib/api-error.ts`
- Create: `src/features/supplier/hooks/use-operator.ts`
- Test: `src/features/supplier/lib/api-error.test.ts`

**Interfaces:**
- Consumes: `src/lib/api/envelope.ts` 的 `toApiError` / `ApiError`
- Produces:
  - `api-error.ts` 导出：
    - `export interface CleanError { message: string; traceId?: string; code: number }`
    - `export function cleanApiError(error: unknown): CleanError` —— 用 `toApiError(error)` 取 `code` 与 `message`，把 `Service error: ` 前缀剥掉，把尾部 `(traceId=xxx)` 拆成 `traceId` 字段并从 `message` 里移除；没有 traceId 时 `traceId` 为 `undefined`
    - `export function errorMessage(error: unknown): string` —— `cleanApiError(error).message` 的便捷版
  - `use-operator.ts` 导出：
    - `export function useOperator(): { operatorId?: string; operatorName?: string }` —— 从 `useAuthStore()` 取 `user`，`operatorId = user?.id`，`operatorName = user?.displayName || user?.username || undefined`

- [ ] **Step 1: 写失败的测试**

`src/features/supplier/lib/api-error.test.ts`：

```ts
import { describe, expect, it } from "vitest";
import { cleanApiError, errorMessage } from "./api-error";

describe("cleanApiError", () => {
  it("keeps the backend business reason from a 4xx envelope", () => {
    const error = { response: { status: 400, data: { code: 400, message: "Service error: 合作中的供应商不能删除，请先终止合作 (traceId=abc123)" } } };
    expect(cleanApiError(error)).toEqual({
      code: 400,
      message: "合作中的供应商不能删除，请先终止合作",
      traceId: "abc123",
    });
  });

  it("strips the prefix even when there is no trace id", () => {
    const error = { response: { status: 400, data: { code: 400, message: "Service error: 税号已存在" } } };
    expect(errorMessage(error)).toBe("税号已存在");
  });

  it("leaves a plain message untouched", () => {
    expect(errorMessage(new Error("Network down"))).toBe("Network down");
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npx vitest run src/features/supplier/lib/api-error.test.ts`
Expected: FAIL —— `Cannot find module './api-error'`

- [ ] **Step 3: 实现 `src/features/supplier/lib/api-error.ts`**

```ts
import { toApiError } from "@/lib/api/envelope";

export interface CleanError {
  code: number;
  message: string;
  traceId?: string;
}

const TRACE_PATTERN = /\s*\(traceId=([^)]+)\)\s*$/;

export function cleanApiError(error: unknown): CleanError {
  const apiError = toApiError(error);
  let message = apiError.message ?? "";
  let traceId: string | undefined;

  const trace = TRACE_PATTERN.exec(message);
  if (trace) {
    traceId = trace[1];
    message = message.replace(TRACE_PATTERN, "");
  }
  if (message.startsWith("Service error: ")) {
    message = message.slice("Service error: ".length);
  }
  return traceId ? { code: apiError.code, message, traceId } : { code: apiError.code, message };
}

export function errorMessage(error: unknown): string {
  return cleanApiError(error).message;
}
```

- [ ] **Step 4: 实现 `src/features/supplier/hooks/use-operator.ts`**

```ts
"use client";

import { useAuthStore } from "@/stores/useAuthStore";

export function useOperator(): { operatorId?: string; operatorName?: string } {
  const user = useAuthStore((state) => state.user);
  return {
    operatorId: user?.id,
    operatorName: user?.displayName || user?.username || undefined,
  };
}
```

- [ ] **Step 5: 跑测试 + 类型检查**

Run: `npx vitest run src/features/supplier/lib/api-error.test.ts` → PASS
Run: `npm run typecheck` → 无错误

- [ ] **Step 6: 提交**

```bash
git add src/features/supplier/lib/api-error.ts src/features/supplier/lib/api-error.test.ts src/features/supplier/hooks/use-operator.ts
git commit -m "feat(supplier): backend error message cleanup and operator identity"
```

---

### Task 4: 表单 schema + payload mapper（PATCH 语义）

**Files:**
- Create: `src/features/supplier/lib/supplier-form-schema.ts`
- Test: `src/features/supplier/lib/supplier-form-schema.test.ts`

**Interfaces:**
- Consumes: Task 1 的 `SupplierUpsertPayload`
- Produces:
  - `export interface SupplierFormValues` —— 表单值类型，全部字段为**字符串**（数字字段用字符串，提交时转数字）
  - `export const supplierFormSchema: z.ZodType<SupplierFormValues>` —— Zod schema
  - `export const supplierFormDefaults: SupplierFormValues` —— 空表单的默认值
  - `export function toSupplierFormValues(dto: SupplierDTO): SupplierFormValues` —— DTO → 表单初值
  - `export function toSupplierPayload(values: SupplierFormValues): SupplierUpsertPayload` —— 表单值 → 请求体

- [ ] **Step 1: 写失败的测试**

`src/features/supplier/lib/supplier-form-schema.test.ts`：

```ts
import { describe, expect, it } from "vitest";
import { supplierFormSchema, toSupplierFormValues, toSupplierPayload } from "./supplier-form-schema";

describe("supplier form schema", () => {
  it("requires a supplier name", () => {
    const result = supplierFormSchema.safeParse({ ...toSupplierFormValues({ id: "s-1", supplierName: "" }), supplierName: "" });
    expect(result.success).toBe(false);
  });

  it("rejects a supplier code with illegal characters", () => {
    const result = supplierFormSchema.safeParse({ ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }), supplierCode: "SUP 001!" });
    expect(result.success).toBe(false);
  });

  it("rejects a credit rating outside the allowed set", () => {
    const result = supplierFormSchema.safeParse({ ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }), creditRating: "E" });
    expect(result.success).toBe(false);
  });

  it("sends an empty string when the user clears an optional field", () => {
    const values = { ...toSupplierFormValues({ id: "s-1", supplierName: "华强", remark: "旧备注" }) };
    values.remark = "";
    const payload = toSupplierPayload(values);
    expect(payload.remark).toBe("");
    expect("remark" in payload).toBe(true);
  });

  it("omits fields the form does not own", () => {
    const payload = toSupplierPayload(toSupplierFormValues({ id: "s-1", supplierName: "华强" }));
    expect("cooperationStartDate" in payload).toBe(false);
    expect("supplierLevel" in payload).toBe(false);
    expect("auditStatus" in payload).toBe(false);
    expect("riskLevel" in payload).toBe(false);
    expect("blacklisted" in payload).toBe(false);
  });

  it("converts the registered capital string into a number", () => {
    const values = { ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }) };
    values.registeredCapital = "5000000";
    expect(toSupplierPayload(values).registeredCapital).toBe(5000000);
  });

  it("rejects a negative registered capital", () => {
    const values = { ...toSupplierFormValues({ id: "s-1", supplierName: "华强" }) };
    values.registeredCapital = "-1";
    expect(supplierFormSchema.safeParse(values).success).toBe(false);
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npx vitest run src/features/supplier/lib/supplier-form-schema.test.ts`
Expected: FAIL —— `Cannot find module './supplier-form-schema'`

- [ ] **Step 3: 实现 `src/features/supplier/lib/supplier-form-schema.ts`**

`SupplierFormValues` 字段（全部 `string`）：

```
supplierCode, supplierName, supplierNameEn, supplierType, businessType,
legalPerson, registeredCapital, registrationNo, taxNo, establishmentDate,
contactName, contactPhone, contactEmail, contactAddress, bankName,
bankAccount, bankAccountName, creditRating, paymentTerms, paymentMethod,
businessLicense, certificates, website, sortOrder, remark
```

Zod 规则（与后端 `SupSupplierServiceImpl.validateMasterData` 对齐，spec §5.4）：

- `supplierName`：`z.string().trim().min(1, "供应商名称不能为空").max(256, "供应商名称长度不能超过 256")`
- `supplierCode`：`z.string().trim().regex(/^[A-Za-z0-9_-]{2,64}$/, "供应商编码只能包含字母、数字、下划线或连字符，长度 2-64")`，允许空串（后端留空会自动生成编码）
- `taxNo` / `registrationNo`：`.max(64, "…长度不能超过 64")`
- `contactPhone`：`.max(32, "联系电话长度不能超过 32")`
- `contactEmail`：`z.union([z.literal(""), z.string().email("联系邮箱格式不正确").max(128)])`
- `creditRating`：`z.union([z.literal(""), z.enum(["A+", "A", "B+", "B", "C", "D"])])`，错误消息 "信用等级仅支持 A+/A/B+/B/C/D"
- `registeredCapital`：`z.string().refine((v) => v === "" || (Number.isFinite(Number(v)) && Number(v) >= 0), "注册资本必须是非负数")`
- `supplierType`：数字字符串，`.refine((v) => v === "" || [1,2,3,4].includes(Number(v)))`
- `sortOrder`：数字字符串，允许空串
- 其余字段：`.max(n)` 对齐后端的 `@Size`

`toSupplierFormValues(dto)`：把 DTO 的每个字段转成字符串（数字用 `String(n)`，`null`/`undefined` → `""`），**不包含** `cooperationStartDate` 与任何状态/评分字段。

`toSupplierPayload(values)` 的规则（这是 PATCH 语义的核心）：

- 只输出表单托管的 26 个字段
- 空字符串**保留**为 `""`（这是"清空"的信号）
- 数字字段：空串 → 该键**不出现在 payload 里**（表示"不改"）；非空串 → `Number(v)`
- `supplierName` 始终输出（后端 `@NotBlank`）
- **绝不输出** `cooperationStartDate`、`supplierLevel`、`auditStatus`、`riskLevel`、`blacklisted`、`overallScore`

- [ ] **Step 4: 跑测试确认通过**

Run: `npx vitest run src/features/supplier/lib/supplier-form-schema.test.ts` → PASS（7 个用例全绿）

- [ ] **Step 5: 类型检查 + 提交**

Run: `npm run typecheck` → 无错误

```bash
git add src/features/supplier/lib/supplier-form-schema.ts src/features/supplier/lib/supplier-form-schema.test.ts
git commit -m "feat(supplier): form schema and PATCH-aware payload mapper"
```

---

### Task 5: Query keys + Hooks 层

**Files:**
- Create: `src/features/supplier/lib/query-keys.ts`
- Create: `src/features/supplier/hooks/use-supplier.ts`（修改现有）
- Create: `src/features/supplier/hooks/use-supplier-contact.ts`
- Create: `src/features/supplier/hooks/use-supplier-bank-account.ts`
- Create: `src/features/supplier/hooks/use-supplier-certificate.ts`
- Create: `src/features/supplier/hooks/use-supplier-category.ts`
- Create: `src/features/supplier/hooks/use-supplier-evaluation.ts`
- Modify: `src/features/supplier/hooks/index.ts`
- Test: `src/features/supplier/hooks/use-supplier.test.ts`

**Interfaces:**
- Consumes: Task 1 的 `supplierApiService`、Task 2 的 mappers、Task 3 的 `useOperator`
- Produces（**所有 hook 名与签名如下，后续 task 直接引用**）：

`lib/query-keys.ts`：

```ts
export const supplierKeys = {
  all: ["suppliers"] as const,
  list: (params?: unknown) => ["suppliers", "list", params] as const,
  detail: (id: string) => ["suppliers", "detail", id] as const,
  profile: (id: string) => ["suppliers", "profile", id] as const,
  performance: (id: string) => ["suppliers", "performance", id] as const,
  stats: ["suppliers", "stats"] as const,
  changeLogs: (id: string, limit: number) => ["suppliers", "change-logs", id, limit] as const,
  contacts: (supplierId: string) => ["suppliers", "contacts", supplierId] as const,
  bankAccounts: (supplierId: string) => ["suppliers", "bank-accounts", supplierId] as const,
  certificates: (supplierId: string) => ["suppliers", "certificates", supplierId] as const,
  categories: (supplierId: string) => ["suppliers", "categories", supplierId] as const,
};

export const supplierCategoryKeys = {
  all: ["supplier-categories"] as const,
  list: (params?: unknown) => ["supplier-categories", "list", params] as const,
};

export const supplierEvaluationKeys = {
  page: (supplierId: string | undefined, page: number) => ["supplier-evaluations", "page", supplierId, page] as const,
  bySupplier: (supplierId: string) => ["supplier-evaluations", "by-supplier", supplierId] as const,
};

export const supplierCertificateExpiryKeys = {
  expiring: (days: number, page: number) => ["supplier-certificates", "expiring", days, page] as const,
};
```

`hooks/use-supplier.ts`：

```ts
export function useSupplierList(params?: SupplierSearchParams)
export function useSupplierSearch(name: string)  // typeahead 候选，enabled: name.trim().length > 0
export function useSupplierDetail(id: string)
export function useSupplierProfile(id: string)
export function useSupplierPerformance(id: string)
export function useSupplierStats()
export function useSupplierChangeLogs(id: string, limit?: number)
export function useCreateSupplier()          // mutation, 变量 SupplierUpsertPayload
export function useUpdateSupplier()          // mutation, 变量 { id: string; data: SupplierUpsertPayload }
export function useDeleteSupplier()          // mutation, 变量 { id: string }
export function useEnableSupplier()          // mutation, 变量 { id: string }
export function useDisableSupplier()         // mutation, 变量 { id: string }
export function useSubmitSupplierAudit()     // mutation, 变量 { id: string }
export function useAuditSupplier()           // mutation, 变量 { id: string; action: "APPROVE" | "REJECT"; remark?: string }
export function useBlacklistSupplier()       // mutation, 变量 { id: string; reason: string }
export function useUnblacklistSupplier()     // mutation, 变量 { id: string }
export function useChangeSupplierRiskLevel() // mutation, 变量 { id: string; level: number; reason?: string }
export function useChangeCooperationStatus() // mutation, 变量 { id: string; status: number }
export function useAssignSupplierCategories()// mutation, 变量 { id: string; categoryIds: string[] }
export function useSupplierAssignedCategories(supplierId: string) // 该供应商**已关联**的分类（GET /suppliers/{id}/categories）
```

`useSupplierCategories`（分类字典，来自 `GET /supplier-categories`）在
`hooks/use-supplier-category.ts` 里；`useSupplierAssignedCategories`（某供应商已关联的分类，
来自 `GET /suppliers/{id}/categories`）放在 `hooks/use-supplier.ts` 里 ——
两个 hook 名字相近但语义不同，**不要合并**。

`hooks/use-supplier-contact.ts`：

```ts
export function useSupplierContacts(supplierId: string)
export function useCreateSupplierContact()   // 变量 { supplierId: string; data: SupplierContactDTO }
export function useUpdateSupplierContact()   // 变量 { supplierId: string; id: string; data: SupplierContactDTO }
export function useDeleteSupplierContact()   // 变量 { supplierId: string; id: string }
export function useSetPrimarySupplierContact() // 变量 { supplierId: string; id: string }
```

`hooks/use-supplier-bank-account.ts`：

```ts
export function useSupplierBankAccounts(supplierId: string)
export function useCreateSupplierBankAccount()  // { supplierId, data }
export function useUpdateSupplierBankAccount()  // { supplierId, id, data }
export function useDeleteSupplierBankAccount()  // { supplierId, id }
export function useSetDefaultSupplierBankAccount() // { supplierId, id }
```

`hooks/use-supplier-certificate.ts`：

```ts
export function useSupplierCertificates(supplierId: string)
export function useExpiringCertificates(days: number, page: number)
export function useCreateSupplierCertificate()  // { supplierId, data }
export function useUpdateSupplierCertificate()  // { supplierId, id, data }
export function useVerifySupplierCertificate()   // { supplierId, id, approved: boolean; remark?: string }
export function useDeleteSupplierCertificate()  // { supplierId, id }
```

`hooks/use-supplier-category.ts`：

```ts
export function useSupplierCategories(params?: { keyword?: string; enabled?: boolean })
export function useCreateSupplierCategory()  // { data }
export function useUpdateSupplierCategory()  // { id, data }
```

（**不提供删除 hook** —— spec §5.9 决定分类页不暴露删除按钮）

`hooks/use-supplier-evaluation.ts`：

```ts
export function useSupplierEvaluations(supplierId: string, page: number, size?: number)
export function useSupplierEvaluationDetail(id: string)      // GET /supplier-evaluations/{id}
export function useSupplierAverageScore(supplierId: string)
export function useCreateSupplierEvaluation()  // { data }
export function useUpdateSupplierEvaluation()  // { id, data }
export function useDeleteSupplierEvaluation()  // { id }
```

**所有 mutation 的实现约定**（每个 mutation hook 都一样）：

```ts
export function useAuditSupplier() {
  const qc = useQueryClient();
  const { operatorId, operatorName } = useOperator();
  return useMutation({
    mutationFn: (vars: { id: string; action: "APPROVE" | "REJECT"; remark?: string }) =>
      supplierApiService.audit(vars.id, {
        action: vars.action,
        remark: vars.remark,
        operatorId,
        operatorName,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: supplierKeys.all });
      qc.invalidateQueries({ queryKey: supplierCategoryKeys.all });
    },
  });
}
```

- 审核 / 风险等级 / 黑名单 / 合作状态 / 启停 / 提交审核 / 删除 / 分类关联的 mutation，`onSuccess` 都要 `invalidateQueries({ queryKey: supplierKeys.all })`（前缀失效，覆盖 list / detail / profile / stats 全部键）。
- 联系人 / 银行账户 / 证照 / 评价 的 mutation，额外失效对应的子资源键（如 `supplierKeys.contacts(vars.supplierId)`）。
- **mutation 不在 hook 里弹提示**：仓库没有 toast 组件，提示由调用方（组件）在 `mutateAsync` 的 catch 里用 `errorMessage(error)` 渲染成行内错误。

- [ ] **Step 1: 写失败的测试**

`src/features/supplier/hooks/use-supplier.test.ts` —— 用 `renderHook` + `QueryClientProvider` 验证 query key 与 mutation 变量传递：

```ts
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { renderHook, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { ReactNode } from "react";

const h = vi.hoisted(() => ({ audit: vi.fn(), list: vi.fn(), user: { id: "u-1", username: "zhangsan", displayName: "张三" } }));

vi.mock("@/lib/api/services/supplier", () => ({
  supplierApiService: { audit: h.audit, list: h.list, stats: vi.fn(), page: vi.fn(), profile: vi.fn(), performance: vi.fn(), changeLogs: vi.fn() },
}));
vi.mock("@/stores/useAuthStore", () => ({ useAuthStore: (selector: (s: { user: unknown }) => unknown) => selector({ user: h.user }) }));

import { useAuditSupplier, useSupplierList } from "./use-supplier";

const wrapper = ({ children }: { children: ReactNode }) => (
  <QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>{children}</QueryClientProvider>
);

describe("supplier hooks", () => {
  beforeEach(() => { h.audit.mockReset().mockResolvedValue(undefined); h.list.mockReset().mockResolvedValue({ records: [], total: 0, size: 20, current: 1 }); });

  it("sends the current user as the audit operator", async () => {
    const { result } = renderHook(() => useAuditSupplier(), { wrapper });
    await result.current.mutateAsync({ id: "s-1", action: "APPROVE", remark: "资料齐全" });
    expect(h.audit).toHaveBeenCalledWith("s-1", { action: "APPROVE", remark: "资料齐全", operatorId: "u-1", operatorName: "张三" });
  });

  it("falls back to the username when there is no display name", async () => {
    h.user = { id: "u-2", username: "lisi", displayName: "" } as never;
    const { result } = renderHook(() => useAuditSupplier(), { wrapper });
    await result.current.mutateAsync({ id: "s-1", action: "REJECT", remark: "资料不全" });
    expect(h.audit).toHaveBeenCalledWith("s-1", expect.objectContaining({ operatorName: "lisi" }));
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npx vitest run src/features/supplier/hooks/use-supplier.test.ts`
Expected: FAIL —— `useAuditSupplier` 不存在或 mock 不匹配

- [ ] **Step 3: 创建 `src/features/supplier/lib/query-keys.ts`** —— 按 Interfaces 段原样实现

- [ ] **Step 4: 实现 6 个 hooks 文件** —— 按 Interfaces 段的签名清单逐个实现，遵守"所有 mutation 的实现约定"

`hooks/index.ts` 改为：

```ts
export * from "./use-operator";
export * from "./use-supplier";
export * from "./use-supplier-bank-account";
export * from "./use-supplier-certificate";
export * from "./use-supplier-category";
export * from "./use-supplier-contact";
export * from "./use-supplier-evaluation";
```

- [ ] **Step 5: 跑测试确认通过**

Run: `npx vitest run src/features/supplier/hooks/use-supplier.test.ts` → PASS（2 个用例全绿）

- [ ] **Step 6: 类型检查 + 全量单测 + 提交**

Run: `npm run typecheck` → 无错误
Run: `npm test` → 全绿（既有用例不得回归）

```bash
git add src/features/supplier/lib/query-keys.ts src/features/supplier/hooks/
git commit -m "feat(supplier): query keys and TanStack Query hooks"
```

---

### Task 6: 列表页 —— KPI + 筛选 + 表格 + 行操作

**Files:**
- Modify: `src/features/supplier/components/supplier-list.tsx`
- Create: `src/features/supplier/components/supplier-stats.tsx`
- Create: `src/features/supplier/components/supplier-filters.tsx`
- Create: `src/features/supplier/components/supplier-actions.tsx`
- Modify: `src/features/supplier/types/supplier.types.ts`（`SupplierSearchParams` 扩展）
- Test: `src/features/supplier/components/supplier-list.test.tsx`

**Interfaces:**
- Consumes: Task 2 的 `Supplier` / constants、Task 5 的 `useSupplierList` / `useSupplierStats` / 各 mutation hook、Task 3 的 `errorMessage`
- Produces:
  - `SupplierSearchParams` 扩展为：
    ```ts
    export interface SupplierSearchParams {
      current?: number;
      pageSize?: number;
      keyword?: string;
      status?: string;               // active | disabled
      supplierType?: number;
      cooperationStatus?: number;
      auditStatus?: number;
      supplierLevel?: number;
      riskLevel?: number;
      blacklisted?: boolean;
      categoryId?: string;
    }
    ```
  - `components/supplier-stats.tsx` 导出 `default function SupplierStats({ onFilter }: { onFilter: (patch: Partial<SupplierSearchParams>) => void })`
  - `components/supplier-filters.tsx` 导出 `default function SupplierFilters({ value, categories, onChange }: { value: SupplierSearchParams; categories: SupplierCategory[]; onChange: (patch: Partial<SupplierSearchParams>) => void })`
  - `components/supplier-actions.tsx` 导出 `default function SupplierActions({ supplier, onDone }: { supplier: Supplier; onDone: () => void })` —— `⋯` 菜单 + 各动作 Dialog
  - `components/supplier-list.tsx` 导出 `default function SupplierList()`

- [ ] **Step 1: 写失败的测试**

`src/features/supplier/components/supplier-list.test.tsx`：

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  searchParams: new URLSearchParams() as URLSearchParams,
  listParams: undefined as Record<string, unknown> | undefined,
  rows: [] as unknown[],
  stats: { total: 128, enabled: 96, cooperating: 74, pendingAudit: 9, blacklisted: 3, expiringCertificates: 5, byCooperationStatus: {}, byAuditStatus: {} },
}));

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: h.push, replace: h.replace }),
  useSearchParams: () => h.searchParams,
}));
vi.mock("../hooks", () => ({
  useSupplierList: (params: Record<string, unknown>) => { h.listParams = params; return { data: { records: h.rows, total: 1, current: 1, pageSize: 20 }, isLoading: false }; },
  useSupplierStats: () => ({ data: h.stats }),
  useSupplierCategories: () => ({ data: [] }),
  useSupplierSearch: () => ({ data: [] }),
  useAuditSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useBlacklistSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useEnableSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDisableSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useSubmitSupplierAudit: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useChangeSupplierRiskLevel: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useChangeCooperationStatus: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useUnblacklistSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
  useDeleteSupplier: () => ({ mutateAsync: vi.fn(), isPending: false }),
}));

import SupplierList from "./supplier-list";

const supplier = {
  id: "s-1", name: "深圳华强电子有限公司", code: "SUP-0001",
  contactPerson: "张伟", contactPhone: "138****2210", email: undefined, address: undefined,
  status: "active", rating: 4.6, overallScore: 92, supplierLevel: 1, auditStatus: 2,
  riskLevel: 1, blacklisted: false, cooperationStatus: 1, createdAt: "2021-03-02",
};

describe("SupplierList", () => {
  beforeEach(() => { h.rows = [supplier]; h.push.mockReset(); });

  it("shows the six supplier statistics", () => {
    render(<SupplierList />);
    expect(screen.getByText("128")).toBeInTheDocument();
    expect(screen.getByText("待审核")).toBeInTheDocument();
    expect(screen.getByText("9")).toBeInTheDocument();
  });

  it("passes the search keyword to the list query", async () => {
    render(<SupplierList />);
    await userEvent.type(screen.getByPlaceholderText("搜索供应商名称 / 编码"), "华强");
    await userEvent.click(screen.getByRole("button", { name: "搜索" }));
    await waitFor(() => expect(h.listParams).toMatchObject({ keyword: "华强" }));
  });

  it("navigates to the supplier detail when the name is clicked", async () => {
    render(<SupplierList />);
    await userEvent.click(screen.getByText("深圳华强电子有限公司"));
    expect(h.push).toHaveBeenCalledWith("/supplier/s-1");
  });

  it("greys out delete for a supplier that is still cooperating", async () => {
    render(<SupplierList />);
    await userEvent.click(screen.getByRole("button", { name: "更多操作" }));
    const del = await screen.findByRole("menuitem", { name: "删除" });
    expect(del).toHaveAttribute("data-disabled");
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npx vitest run src/features/supplier/components/supplier-list.test.tsx`
Expected: FAIL —— 新组件不存在 / 断言不匹配

- [ ] **Step 3: 实现 `components/supplier-stats.tsx`**

6 个数字卡（总数 / 启用 / 合作中 / 待审核 / 黑名单 / 30 天内到期证照）。
每张卡是 `<button>`，点击调用 `onFilter`：

| 卡 | onFilter 参数 |
|---|---|
| 供应商总数 | `{}`（清空筛选） |
| 已启用 | `{ status: "active" }` |
| 合作中 | `{ cooperationStatus: 1 }` |
| 待审核 | `{ auditStatus: 1 }` |
| 黑名单 | `{ blacklisted: true }` |
| 30 天内到期证照 | 不改筛选，改为跳 `/supplier/audit`（用 `useRouter`） |

样式：`Card` + `text-[23px] font-semibold tabular-nums` 的数字，标签用 `text-[10px] text-muted-foreground`。
数据来自 `useSupplierStats()`，缺失时显示 `0`。

- [ ] **Step 4: 实现 `components/supplier-filters.tsx`**

一行 flex 容器：关键字 `Input`（宽 230，placeholder "搜索供应商名称 / 编码"）+ 6 个 `Select`
（供应商类型 / 合作状态 / 准入状态 / 分级 / 风险等级 / 黑名单）+ 分类 `Select`。
每个 `Select` 的 trigger 里先放一个 `text-muted-foreground` 的字段名（与现有 `supplier-list.tsx` 的写法一致），
value 用 `__all__` 表示"全部"（与现有代码的 `ALL` 常量同款）。

分类下拉的选项来自 `categories` prop（`SupplierCategory[]`），按 `sortOrder` 升序，
子分类用全角空格缩进（`　`）。

**URL 同步**：`SupplierList`（不是 `SupplierFilters`）负责读写 URL ——
用 `useSearchParams()` 取初始筛选值、用 `router.replace`（**不是 `push`**，避免污染历史栈）
把筛选写回 query。筛选键名：`keyword` / `status` / `supplierType` / `cooperationStatus` /
`auditStatus` / `supplierLevel` / `riskLevel` / `blacklisted` / `categoryId` / `page`。

> 这就是 Task 14 要加 `<Suspense>` 边界的原因：`useSearchParams` 在客户端组件里
> 没有 Suspense 边界会让 `next build` 失败。

- [ ] **Step 5: 实现 `components/supplier-actions.tsx`**

`DropdownMenu` + `DropdownMenuTrigger`（`aria-label="更多操作"`）+ `DropdownMenuContent`。
菜单项（按顺序）：提交审核 / 审核准入 / 启用 / 停用 / 调整风险等级 / 变更合作状态 /
`DropdownMenuSeparator` / 列入黑名单 / 移出黑名单 / 删除。

**置灰规则（spec §4.5，只用当前状态的布尔判断）**：

| 菜单项 | 置灰条件 | tooltip 文案 |
|---|---|---|
| 启用 | `blacklisted === true` | 黑名单供应商不可启用 |
| 停用 | `status === "disabled"` | 供应商已停用 |
| 提交审核 | `taxNo` 为空 | 税号未补全，无法提交审核 |
| 变更合作状态 → 建立合作 | `auditStatus !== 2` 或 `blacklisted` | 未通过准入审核的供应商不可建立合作 / 黑名单供应商不可建立合作 |
| 删除 | `cooperationStatus === 1` | 合作中的供应商不能删除，请先终止合作 |
| 移出黑名单 | `blacklisted === false` | 供应商不在黑名单中 |

置灰用 `DropdownMenuItem disabled`（Radix 会渲染 `data-disabled` 属性）。
**不复制合作状态流转矩阵** —— 「变更合作状态」子菜单给出全部 4 个目标值（0/1/2/3），
非法流转由后端 400 拒绝，组件把后端消息通过 `errorMessage(error)` 显示在 Dialog 的行内错误位。

需要填写内容的动作各自一个 `Dialog`：
- 审核准入：`RadioGroup`（通过 / 驳回）+ 审核意见 `Textarea`（驳回时必填）
- 调整风险等级：`Select`（低 / 中 / 高）+ 原因 `Textarea`（可选）
- 列入黑名单：原因 `Textarea`（**必填**，后端 `reason` 是必填 query param）
- 删除：`ConfirmDialog`（destructive）

每个 Dialog 的提交失败时用 `errorMessage(error)` 在 Dialog 内渲染一行红色文本
（`border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm`，
与 `features/product/components/product-form.tsx` 的 `submitError` 样式一致）。

- [ ] **Step 6: 重写 `components/supplier-list.tsx`**

结构：`PageHeader`（eyebrow "供应链协同" / title "供应商管理" / description "供应商主数据、准入状态、风险等级、证照与绩效的一体化管理台"，
actions 放一个"新建供应商" `Button`，`router.push("/supplier/new")`）
→ `SupplierStats` → `TablePanel`（`TableToolbar` 里放 `SupplierFilters`；表格；`TablePagination`）。

筛选状态来自 `useSearchParams()`（初值），任何筛选变更都同时
`setState` 并 `router.replace` 写回 query（键名见 Step 4）。
筛选变更时页码重置为 1。

表格列（`columnHelper` + `tableFeatures({})`，与现有写法一致）：
供应商（名称 + 编码，左侧 3px 色条按合作状态着色）/ 分级 / 准入状态 / 风险等级 /
综合得分（0–100 数字 + `RatingStars`）/ 联系人（姓名 + 电话）/ 合作状态 / 启用状态 / 操作。

色条颜色：合作中 → `var(--color-primary)`；暂停 → `var(--color-warning)`；终止 → `var(--color-error)`；潜在 → `var(--color-border-strong)`。

操作列：两个 `Button variant="ghost" size="icon"`（查看 → `router.push(\`/supplier/${id}\`)`、
编辑 → `router.push(\`/supplier/${id}/edit\`)`）+ 一个 `SupplierActions`。

`RatingStars` 保持原样（0–5 量程），传入 `supplier.rating`（Task 2 已改成 `overallScore / 20` 优先）。

- [ ] **Step 7: 跑测试确认通过**

Run: `npx vitest run src/features/supplier/components/supplier-list.test.tsx` → PASS（4 个用例全绿）

- [ ] **Step 8: 类型检查 + lint + 提交**

Run: `npm run typecheck` → 无错误
Run: `npm run lint` → 无错误

```bash
git add src/features/supplier/components/ src/features/supplier/types/supplier.types.ts
git commit -m "feat(supplier): management console list with stats, filters and row actions"
```

---

### Task 7: 详情页外壳 + 概览分区

**Files:**
- Modify: `src/features/supplier/components/supplier-detail.tsx`
- Create: `src/features/supplier/components/supplier-overview-panel.tsx`
- Test: `src/features/supplier/components/supplier-detail.test.tsx`

**Interfaces:**
- Consumes: Task 2 的 `SupplierProfile` / `SupplierPerformance` + constants、Task 5 的 `useSupplierProfile` / `useSupplierPerformance`
- Produces:
  - `components/supplier-overview-panel.tsx` 导出 `default function SupplierOverviewPanel({ profile, performance }: { profile: SupplierProfile; performance?: SupplierPerformance | null })`
  - `components/supplier-detail.tsx` 导出 `default function SupplierDetail({ id }: { id: string })`

- [ ] **Step 1: 写失败的测试**

`src/features/supplier/components/supplier-detail.test.tsx`：

```tsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({ push: vi.fn(), profile: null as unknown, performance: null as unknown }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: h.push }) }));
vi.mock("../hooks", () => ({
  useSupplierProfile: () => ({ data: h.profile, isLoading: false }),
  useSupplierPerformance: () => ({ data: h.performance, isLoading: false }),
}));

import SupplierDetail from "./supplier-detail";

const profile = {
  supplier: { id: "s-1", name: "深圳华强电子有限公司", code: "SUP-0001", contactPerson: "张伟", contactPhone: "138****2210", status: "active", rating: 4.6, overallScore: 92, supplierLevel: 1, auditStatus: 2, riskLevel: 1, blacklisted: false, cooperationStatus: 1, createdAt: "2021-03-02" },
  contacts: [], bankAccounts: [], certificates: [], categories: [], changeLogs: [],
};

describe("SupplierDetail", () => {
  beforeEach(() => { h.profile = profile; h.performance = { supplierId: "s-1", supplierName: "深圳华强电子有限公司", supplierLevel: 1, overallScore: 92, qualityScore: 95, deliveryScore: 91.2, serviceScore: 88.5, priceScore: 94, evaluationCount: 12, latestPeriod: "2026-09", latestTotalScore: 92.4, suggestedLevel: 1, recentEvaluations: [] }; h.push.mockReset(); });

  it("shows the supplier name, code and status badges", () => {
    render(<SupplierDetail id="s-1" />);
    expect(screen.getByText("深圳华强电子有限公司")).toBeInTheDocument();
    expect(screen.getByText("SUP-0001")).toBeInTheDocument();
    expect(screen.getByText("战略")).toBeInTheDocument();
    expect(screen.getByText("已通过")).toBeInTheDocument();
  });

  it("shows the performance scorecard dimensions", () => {
    render(<SupplierDetail id="s-1" />);
    expect(screen.getByText("质量")).toBeInTheDocument();
    expect(screen.getByText("95")).toBeInTheDocument();
    expect(screen.getByText("交付")).toBeInTheDocument();
  });

  it("lists the seven sections in the left rail", () => {
    render(<SupplierDetail id="s-1" />);
    for (const label of ["概览", "分类", "联系人", "银行账户", "资质证照", "绩效评价", "变更留痕"]) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npx vitest run src/features/supplier/components/supplier-detail.test.tsx` → FAIL

- [ ] **Step 3: 实现 `components/supplier-overview-panel.tsx`**

两张 `Card`：
1. **主数据** —— 3 列栅格（`grid gap-x-8 gap-y-5 md:grid-cols-3`），字段：供应商编码 / 英文名称 /
   供应商类型 / 业务类型 / 法人 / 注册资本 / 成立日期 / 统一社会信用代码 / 税号 / 信用等级 /
   付款条件 / 付款方式 / 合作起始日期 / 官网 / 排序 / 备注。
   数字与金额用 `font-mono`；`registeredCapital` 显示为 `¥ 5,000,000`（`toLocaleString("zh-CN")`）。
   缺失值显示 `"-"`。
2. **绩效评分卡** —— 顶部一行 5 格，**格子标题必须带权重后缀**：
   「综合得分」「质量 (30%)」「交付 (30%)」「服务 (20%)」「价格 (20%)」，
   下面 4 条横向进度条（条的标签用**不带权重**的「质量」「交付」「服务」「价格」，
   宽度 = 分数百分比，条高 5px，用 `var(--color-primary)`），
   底部一行小字显示评价次数与最近周期。

   > 标题带权重、进度条标签不带权重 —— 两处文字必须不同，否则测试里的
   > `getByText("质量")` 会命中两个元素而报错。

- [ ] **Step 4: 重写 `components/supplier-detail.tsx`**

结构：
- `PageHeader`：eyebrow "供应商档案" / title = 供应商名 / description = `供应商编码 {code} · 创建于 {createdAt}`，
  actions = 「返回列表」`Button variant="outline"`（`router.push("/supplier")`）+「编辑」`Button`（`router.push(\`/supplier/${id}/edit\`)`）
- 状态徽标组：分级 / 准入状态 / 风险等级 / 黑名单 / 合作状态 / 启用，各用一个 `Badge`
- 左导航（`w-[122px] shrink-0 border-r`）：7 个可点击项，用 `useState<SectionKey>` 控制当前分区；
  每项右侧显示条目数（从 `profile.contacts.length` 等取）。当前项高亮（`bg-[var(--color-primary-soft)] text-[var(--color-primary-text)] font-semibold`）。
- 右内容区：当前分区对应的面板。**本 task 只实现「概览」面板**；其余 6 个分区在 Task 8–10 接入，
  此处先放一个 `placeholder`（`text-muted-foreground text-xs`）标明"建设中"，
  保证 Task 7 结束时页面可用。

- [ ] **Step 5: 跑测试确认通过**

Run: `npx vitest run src/features/supplier/components/supplier-detail.test.tsx` → PASS（3 个用例全绿）

- [ ] **Step 6: 类型检查 + 提交**

Run: `npm run typecheck` → 无错误

```bash
git add src/features/supplier/components/supplier-detail.tsx src/features/supplier/components/supplier-overview-panel.tsx src/features/supplier/components/supplier-detail.test.tsx
git commit -m "feat(supplier): 360-degree profile shell with the overview section"
```

---

### Task 8: 详情页分区 —— 分类 + 联系人

**Files:**
- Create: `src/features/supplier/components/supplier-category-panel.tsx`
- Create: `src/features/supplier/components/supplier-contact-panel.tsx`
- Modify: `src/features/supplier/components/supplier-detail.tsx`（接上两个分区）
- Test: `src/features/supplier/components/supplier-panels.test.tsx`

**Interfaces:**
- Consumes: Task 5 的 `useAssignSupplierCategories` / 联系人 hooks、Task 2 的 `SupplierCategory` / `SupplierContact`
- Produces:
  - `components/supplier-category-panel.tsx` 导出 `default function SupplierCategoryPanel({ supplierId }: { supplierId: string })`
  - `components/supplier-contact-panel.tsx` 导出 `default function SupplierContactPanel({ supplierId }: { supplierId: string })`

- [ ] **Step 1: 写失败的测试**

`src/features/supplier/components/supplier-panels.test.tsx`：

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({ assign: vi.fn(), setPrimary: vi.fn(), remove: vi.fn() }));

vi.mock("../hooks", () => ({
  useSupplierCategories: () => ({ data: [{ id: "c-1", categoryCode: "RAW", categoryName: "原材料", level: 1, enabled: true, sortOrder: 10, createTime: null, remark: null }] }),
  useSupplierAssignedCategories: () => ({ data: [{ id: "c-1", categoryCode: "RAW", categoryName: "原材料", level: 1, enabled: true, sortOrder: 10, createTime: null, remark: null }] }),
  useAssignSupplierCategories: () => ({ mutateAsync: h.assign, isPending: false }),
  useSupplierContacts: () => ({ data: [{ id: "ct-1", supplierId: "s-1", contactName: "张伟", contactTitle: "销售经理", contactRole: "SALES", contactPhone: "13800002210", contactEmail: "z@example.com", contactAddress: null, isPrimary: true, enabled: true, sortOrder: 0, createTime: "2026-01-01", remark: null }] }),
  useSetPrimarySupplierContact: () => ({ mutateAsync: h.setPrimary, isPending: false }),
  useDeleteSupplierContact: () => ({ mutateAsync: h.remove, isPending: false }),
}));

import SupplierCategoryPanel from "./supplier-category-panel";
import SupplierContactPanel from "./supplier-contact-panel";

describe("supplier detail panels", () => {
  beforeEach(() => { h.assign.mockReset().mockResolvedValue(undefined); h.setPrimary.mockReset().mockResolvedValue(undefined); h.remove.mockReset().mockResolvedValue(undefined); });

  it("sends the selected category ids as an array when the assignment is saved", async () => {
    render(<SupplierCategoryPanel supplierId="s-1" />);
    await userEvent.click(screen.getByRole("button", { name: "设置分类" }));
    await userEvent.click(await screen.findByRole("checkbox", { name: "原材料" }));
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    await waitFor(() => expect(h.assign).toHaveBeenCalledWith({ id: "s-1", categoryIds: ["c-1"] }));
  });

  it("marks the primary contact and offers to change it", async () => {
    render(<SupplierContactPanel supplierId="s-1" />);
    expect(await screen.findByText("张伟")).toBeInTheDocument();
    expect(screen.getByText("主联系人")).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 跑测试确认失败**

Run: `npx vitest run src/features/supplier/components/supplier-panels.test.tsx` → FAIL

- [ ] **Step 3: 实现 `components/supplier-category-panel.tsx`**

- 列出当前已关联的分类：用 `useSupplierAssignedCategories(supplierId)`（`GET /suppliers/{id}/categories`）
- 「设置分类」按钮打开 `Dialog`：里面是 `useSupplierCategories()`（分类字典）构成的 `Checkbox` 列表
  （子分类缩进），每个 checkbox 的 `aria-label` 是分类名；
  勾选初值 = 已关联分类的 id 集合
- 「保存」→ `assign.mutateAsync({ id: supplierId, categoryIds: selected })`
- 失败时行内显示 `errorMessage(error)`

- [ ] **Step 4: 实现 `components/supplier-contact-panel.tsx`**

- 表格列：姓名 / 职务 / 角色（`labelOf(CONTACT_ROLES, role)`）/ 电话 / 邮箱 / 主联系人 / 操作
- 行操作：编辑 / 设为主联系人（`setPrimary`）/ 删除（`ConfirmDialog`）
- 「新增联系人」按钮打开 Dialog 表单（姓名必填、角色下拉、电话、邮箱、地址）
- 删除主联系人时后端会自动提升下一位（spec §4.1 的控制器描述），不需要前端处理

- [ ] **Step 5: 把两个分区接进 `supplier-detail.tsx`**

- [ ] **Step 6: 跑测试 + 类型检查 + 提交**

Run: `npx vitest run src/features/supplier/components/supplier-panels.test.tsx` → PASS
Run: `npm run typecheck` → 无错误

```bash
git add src/features/supplier/components/supplier-category-panel.tsx src/features/supplier/components/supplier-contact-panel.tsx src/features/supplier/components/supplier-detail.tsx src/features/supplier/components/supplier-panels.test.tsx
git commit -m "feat(supplier): category and contact sections"
```

---

### Task 9: 详情页分区 —— 银行账户 + 资质证照

**Files:**
- Create: `src/features/supplier/components/supplier-bank-account-panel.tsx`
- Create: `src/features/supplier/components/supplier-certificate-panel.tsx`
- Modify: `src/features/supplier/components/supplier-detail.tsx`
- Test: 追加到 `src/features/supplier/components/supplier-panels.test.tsx`

**Interfaces:**
- Consumes: Task 5 的 `useSupplierBankAccounts` / 银行账户 mutations / `useSupplierCertificates` / 证照 mutations、Task 2 的 `maskBankAccount` / `daysToExpiryOf`
- Produces:
  - `components/supplier-bank-account-panel.tsx` 导出 `default function SupplierBankAccountPanel({ supplierId }: { supplierId: string })`
  - `components/supplier-certificate-panel.tsx` 导出 `default function SupplierCertificatePanel({ supplierId }: { supplierId: string })`

- [ ] **Step 1: 追加失败的测试**

在 `supplier-panels.test.tsx` 追加：

```tsx
it("masks the bank account and reveals it on demand", async () => {
  render(<SupplierBankAccountPanel supplierId="s-1" />);
  expect(await screen.findByText("**** **** **** 7890")).toBeInTheDocument();
});

it("shows an expired certificate in red with a negative day count", async () => {
  render(<SupplierCertificatePanel supplierId="s-1" />);
  expect(await screen.findByText("已过期")).toBeInTheDocument();
  expect(screen.getByText("已过期")).toHaveClass("text-[var(--color-error-text)]");
});
```

并把 `../hooks` 的 mock 补上 `useSupplierBankAccounts`、`useSupplierCertificates` 及相关 mutations。

- [ ] **Step 2: 跑测试确认失败** → FAIL

- [ ] **Step 3: 实现 `components/supplier-bank-account-panel.tsx`**

- 表格列：开户行 / 支行 / 账号（默认脱敏）/ 账户名 / 币种 / 默认账户 / 操作
- 每个账号行有一个「显示」开关（`useState` 记录已展开的账号 id 集合），
  关闭时显示 `maskBankAccount(account)`，打开时显示原始账号
- 操作：编辑 / 设为默认 / 删除
- 「新增银行账户」Dialog：开户行、支行、账号、账户名、币种、SWIFT

- [ ] **Step 4: 实现 `components/supplier-certificate-panel.tsx`**

- 表格列：证照名称 / 类型（`labelOf(CERTIFICATE_TYPES, type)`）/ 证照编号 / 发证机关 /
  有效期起 / 有效期止 / 剩余天数 / 状态（`Badge`，variant 取自 `CERTIFICATE_STATUSES`）/ 操作
- 剩余天数的着色：`<= 0` → `text-[var(--color-error-text)]`（已过期）；
  `1..7` → `text-[var(--color-error-text)]`；`8..30` → `text-[var(--color-warning-text)]`；其余 → `text-muted-foreground`
- 状态徽标：后端返回的 `status` 为准；`status === 3` 显示"已过期"
- 操作：编辑 / 审核（Dialog：通过 / 驳回 + 备注）/ 删除
- 「新增资质证照」Dialog：证照类型下拉、证照名称、编号、发证机关、有效期起止（`<input type="date">`）、文件地址

- [ ] **Step 5: 接进 `supplier-detail.tsx` + 跑测试 + 类型检查 + 提交**

Run: `npx vitest run src/features/supplier/components/supplier-panels.test.tsx` → PASS

```bash
git add src/features/supplier/components/
git commit -m "feat(supplier): bank account and certificate sections"
```

---

### Task 10: 详情页分区 —— 绩效评价 + 变更留痕

**Files:**
- Create: `src/features/supplier/components/supplier-evaluation-panel.tsx`
- Create: `src/features/supplier/components/supplier-change-log-panel.tsx`
- Modify: `src/features/supplier/components/supplier-detail.tsx`
- Test: 追加到 `src/features/supplier/components/supplier-panels.test.tsx`

**Interfaces:**
- Consumes: Task 5 的 `useSupplierEvaluations` / `useSupplierAverageScore` / 评价 mutations / `useSupplierChangeLogs`
- Produces:
  - `components/supplier-evaluation-panel.tsx` 导出 `default function SupplierEvaluationPanel({ supplierId }: { supplierId: string })`
  - `components/supplier-change-log-panel.tsx` 导出 `default function SupplierChangeLogPanel({ supplierId }: { supplierId: string })`

- [ ] **Step 1: 追加失败的测试**

```tsx
it("shows the weighted total next to the four dimension scores", async () => {
  render(<SupplierEvaluationPanel supplierId="s-1" />);
  expect(await screen.findByText("加权总分")).toBeInTheDocument();
  expect(screen.getByText("预估")).toBeInTheDocument();
});

it("labels a change log entry with no operator as the system", async () => {
  render(<SupplierChangeLogPanel supplierId="s-1" />);
  expect(await screen.findByText("系统")).toBeInTheDocument();
});
```

mock 里 `useSupplierEvaluations` 返回一条四维度都有分的记录，`useSupplierChangeLogs` 返回一条 `operatorName` 为 null 的记录。

- [ ] **Step 2: 跑测试确认失败** → FAIL

- [ ] **Step 3: 实现 `components/supplier-evaluation-panel.tsx`**

- 头部显示 `useSupplierAverageScore(supplierId)` 的平均分
- 表格列：评价周期 / 质量 / 交付 / 服务 / 价格 / 加权总分 / 采购次数 / 准时交付率 / 质量合格率 / 评价人 / 评价时间 / 操作
- **加权总分用前端算的「预估」值**：权重与 `SupplierScoreCalculator` 一致
  （质量 0.30 / 交付 0.30 / 服务 0.20 / 价格 0.20），四维缺失的项按 0 计入并归一化权重。
  表格里该列显示「预估」小字标签；**最终值以后端回写的 `totalScore` 为准**，
  两列并排（`总评` 用后端值，`加权总分（预估）` 用前端值）。
- 「新增评价」Dialog：评价周期（`YYYY-MM`）、起止日期、四维打分（0–100 的 number input）、
  采购次数、总金额、准时交付率、质量合格率、评价人、改进建议
- **编辑**动作：先调 `GET /supplier-evaluations/{id}` 拿到完整记录再打开 Dialog
  （列表行不保证带全部字段）—— 这也是该端点在界面上的调用点
- 分页：用 `useSupplierEvaluations(supplierId, page, size)` + `TablePagination`

- [ ] **Step 4: 实现 `components/supplier-change-log-panel.tsx`**

- 竖向时间线（`border-l` + 每条一个圆点）
- 每条：变更类型徽标（`CHANGE_TYPE_LABELS[changeType]`）+ 变更内容 + 原因 +
  操作人（`operatorName` 为空显示「系统」）+ 时间
- 只读，无操作按钮

- [ ] **Step 5: 接进 `supplier-detail.tsx` + 跑测试 + 类型检查 + 提交**

Run: `npx vitest run src/features/supplier/components/supplier-panels.test.tsx` → PASS

```bash
git add src/features/supplier/components/
git commit -m "feat(supplier): performance evaluation and change log sections"
```

---

### Task 11: 新建 / 编辑表单页

**Files:**
- Create: `src/features/supplier/components/supplier-form.tsx`
- Create: `src/app/[locale]/(app)/supplier/new/page.tsx`
- Create: `src/app/[locale]/(app)/supplier/[id]/edit/page.tsx`
- Test: `src/features/supplier/components/supplier-form.test.tsx`

**Interfaces:**
- Consumes: Task 4 的 `supplierFormSchema` / `toSupplierFormValues` / `toSupplierPayload` / `supplierFormDefaults`、
  Task 5 的 `useCreateSupplier` / `useUpdateSupplier` / `useSupplierDetail`、Task 3 的 `errorMessage`
- Produces:
  - `components/supplier-form.tsx` 导出 `default function SupplierForm({ id }: { id?: string })`

- [ ] **Step 1: 写失败的测试**

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({ push: vi.fn(), create: vi.fn(), update: vi.fn() }));

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: h.push }) }));
vi.mock("../hooks", () => ({
  useSupplierDetail: () => ({ data: undefined }),
  useCreateSupplier: () => ({ mutateAsync: h.create, isPending: false }),
  useUpdateSupplier: () => ({ mutateAsync: h.update, isPending: false }),
}));

import SupplierForm from "./supplier-form";

describe("SupplierForm", () => {
  beforeEach(() => { h.push.mockReset(); h.create.mockReset().mockResolvedValue({ id: "s-1" }); });

  it("requires a supplier name before submitting", async () => {
    render(<SupplierForm />);
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    expect(await screen.findByText("供应商名称不能为空")).toBeInTheDocument();
    expect(h.create).not.toHaveBeenCalled();
  });

  it("submits the payload built from the form values", async () => {
    render(<SupplierForm />);
    await userEvent.type(screen.getByLabelText("供应商名称"), "深圳华强电子有限公司");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    await waitFor(() => expect(h.create).toHaveBeenCalled());
    expect(h.create.mock.calls[0][0]).toMatchObject({ supplierName: "深圳华强电子有限公司" });
  });

  it("shows the backend reason when the save is rejected", async () => {
    h.create.mockRejectedValue({ response: { status: 400, data: { code: 400, message: "Service error: 供应商编码已存在 (traceId=t1)" } } });
    render(<SupplierForm />);
    await userEvent.type(screen.getByLabelText("供应商名称"), "重复编码公司");
    await userEvent.click(screen.getByRole("button", { name: "保存" }));
    expect(await screen.findByText(/供应商编码已存在/)).toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 跑测试确认失败** → FAIL

- [ ] **Step 3: 实现 `components/supplier-form.tsx`**

- `useForm<SupplierFormValues>({ resolver: zodResolver(supplierFormSchema), defaultValues: supplierFormDefaults, mode: "onBlur" })`
- 编辑模式：`useSupplierDetail(id)` 拿到 DTO 后 `form.reset(toSupplierFormValues(dto))`
  （`useEffect` 依赖 `[data, form]`）
- 4 张 `Card`：基本信息 / 联系信息 / 结算信息 / 其他，栅格 `grid gap-4 md:grid-cols-2`
- 字段与 `SupplierFormValues` 一一对应；`supplierType` 用 `Select`（`SUPPLIER_TYPES`），
  `creditRating` 用 `Select`（空串选项 + 6 个等级），`registeredCapital` / `sortOrder` 用 `type="number"`，
  `establishmentDate` 用 `type="date"`，`registeredCapital` 与 `sortOrder` 在表单里保持**字符串**，
  提交时由 `toSupplierPayload` 转数字
- 编辑模式下 `supplierCode` 的 `Input` 加 `disabled`
- 顶部：错误提示块（`submitError` 样式）+ 「取消」`Button variant="outline"`（`router.push("/supplier")`）+ 「保存」`Button type="submit"`
- 提交：`const payload = toSupplierPayload(values)`；`id ? update.mutateAsync({ id, data: payload }) : create.mutateAsync(payload)`；
  成功后 `router.push("/supplier")`；失败 `setSubmitError(errorMessage(error))`

- [ ] **Step 4: 创建两个路由文件**

`src/app/[locale]/(app)/supplier/new/page.tsx`：

```tsx
"use client";

import { SupplierForm } from "@/features/supplier/components";

export default function SupplierNewPage() {
  return <SupplierForm />;
}
```

`src/app/[locale]/(app)/supplier/[id]/edit/page.tsx`：

```tsx
"use client";

import { usePathname } from "next/navigation";
import { SupplierForm } from "@/features/supplier/components";

export default function SupplierEditPage() {
  const pathname = usePathname();
  const id = pathname.split("/").pop() ?? "";
  return <SupplierForm id={id} />;
}
```

（与既有 `src/app/[locale]/(app)/supplier/[id]/page.tsx` 的取 id 方式保持一致）

- [ ] **Step 5: 跑测试 + 类型检查 + 提交**

Run: `npx vitest run src/features/supplier/components/supplier-form.test.tsx` → PASS
Run: `npm run typecheck` → 无错误

```bash
git add src/features/supplier/components/supplier-form.tsx src/features/supplier/components/supplier-form.test.tsx "src/app/[locale]/(app)/supplier/new/page.tsx" "src/app/[locale]/(app)/supplier/[id]/edit/page.tsx"
git commit -m "feat(supplier): create and edit supplier master data form"
```

---

### Task 12: 准入审核工作台

**Files:**
- Create: `src/features/supplier/components/supplier-audit-workbench.tsx`
- Create: `src/app/[locale]/(app)/supplier/audit/page.tsx`
- Test: `src/features/supplier/components/supplier-audit-workbench.test.tsx`

**Interfaces:**
- Consumes: Task 5 的 `useSupplierList` / `useAuditSupplier` / `useExpiringCertificates`、Task 3 的 `errorMessage`
- Produces:
  - `components/supplier-audit-workbench.tsx` 导出 `default function SupplierAuditWorkbench()`

- [ ] **Step 1: 写失败的测试**

```tsx
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({ audit: vi.fn(), listParams: undefined as Record<string, unknown> | undefined }));

vi.mock("../hooks", () => ({
  useSupplierList: (params: Record<string, unknown>) => { h.listParams = params; return { data: { records: [{ id: "s-1", name: "苏州瑞丰新材料有限公司", code: "SUP-0003", contactPerson: "王强", contactPhone: "", status: "disabled", rating: 3.8, blacklisted: false, cooperationStatus: 0, taxNo: "91320500MA1XK3", auditStatus: 1, supplierLevel: 3, riskLevel: 1, createdAt: "2026-10-28" }], total: 1, current: 1, pageSize: 20 }, isLoading: false }; },
  useAuditSupplier: () => ({ mutateAsync: h.audit, isPending: false }),
  useExpiringCertificates: () => ({ data: { records: [], total: 0, size: 10, current: 1 } }),
}));

import SupplierAuditWorkbench from "./supplier-audit-workbench";

describe("SupplierAuditWorkbench", () => {
  beforeEach(() => { h.audit.mockReset().mockResolvedValue(undefined); });

  it("asks for the pending audit queue", () => {
    render(<SupplierAuditWorkbench />);
    expect(h.listParams).toMatchObject({ auditStatus: 1 });
  });

  it("approves a supplier with the reviewer remark", async () => {
    render(<SupplierAuditWorkbench />);
    await userEvent.click(await screen.findByRole("button", { name: "通过" }));
    expect(h.audit).toHaveBeenCalledWith({ id: "s-1", action: "APPROVE", remark: undefined });
  });

  it("never offers a certificate that already expired", () => {
    render(<SupplierAuditWorkbench />);
    expect(screen.queryByText("已过期")).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 跑测试确认失败** → FAIL

- [ ] **Step 3: 实现 `components/supplier-audit-workbench.tsx`**

- `PageHeader`：eyebrow "供应商管理" / title "准入审核工作台" /
  description "供应商准入审核队列与资质证照到期预警"
- **待审核队列** `TablePanel`：
  - 数据源 `useSupplierList({ auditStatus: 1, current, pageSize: 20 })`
  - 列：供应商（名称 + 编码）/ 类型 / 信用等级 / 税号 / 证照数 / 提交时间 / 操作
  - 行内两个 `Button`：「通过」（`variant="success"` 风格的 success 色，用
    `className="bg-[var(--color-success)] text-white hover:bg-[var(--color-success-hover)]"`）与「驳回」（destructive 描边）
  - 「通过」：`audit.mutateAsync({ id, action: "APPROVE" })`（意见选填；直接提交）
  - 「驳回」：打开 Dialog，审核意见**必填**，提交
    `audit.mutateAsync({ id, action: "REJECT", remark })`
  - **「通过」在 `taxNo` 为空时置灰**（`disabled`），并用 `Tooltip` 说明「税号未补全，无法提交审核」
- **证照到期预警** `TablePanel`：
  - 天数切换用分段控件（30 / 60 / 90），`useState<number>`
  - 数据源 `useExpiringCertificates(days, page)`
  - 列：供应商（名称 + 编码）/ 证照名称 / 类型 / 到期日 / 剩余天数 / 操作
  - 剩余天数 ≤ 7 天标红、≤ 30 天标黄、其余灰
  - 标题写「资质证照到期预警 · 未来 {days} 天内到期」
  - **不渲染"已过期"状态**（后端 `pageExpiring` 的下界是 `validTo >= 今天`）
  - 「查看供应商」→ `router.push(\`/supplier/${id}\`)`

- [ ] **Step 4: 创建 `src/app/[locale]/(app)/supplier/audit/page.tsx`**

```tsx
"use client";

import { SupplierAuditWorkbench } from "@/features/supplier/components";

export default function SupplierAuditPage() {
  return <SupplierAuditWorkbench />;
}
```

- [ ] **Step 5: 跑测试 + 类型检查 + 提交**

Run: `npx vitest run src/features/supplier/components/supplier-audit-workbench.test.tsx` → PASS
Run: `npm run typecheck` → 无错误

```bash
git add src/features/supplier/components/supplier-audit-workbench.tsx src/features/supplier/components/supplier-audit-workbench.test.tsx "src/app/[locale]/(app)/supplier/audit/page.tsx"
git commit -m "feat(supplier): admission review and certificate expiry workbench"
```

---

### Task 13: 分类字典管理页

**Files:**
- Create: `src/features/supplier/components/supplier-category-manage.tsx`
- Create: `src/app/[locale]/(app)/supplier/category/page.tsx`
- Test: `src/features/supplier/components/supplier-category-manage.test.tsx`

**Interfaces:**
- Consumes: Task 5 的 `useSupplierCategories` / `useCreateSupplierCategory` / `useUpdateSupplierCategory`、
  Task 2 的 `SupplierCategory`、Task 3 的 `errorMessage`
- Produces:
  - `components/supplier-category-manage.tsx` 导出 `default function SupplierCategoryManage()`
  - `components/supplier-category-manage.tsx` 同时导出
    `export function buildCategoryTree(categories: SupplierCategory[]): SupplierCategory[]`
    —— 按 `parentId` 组树，每层按 `sortOrder` 升序，孤儿（父不存在）当根节点

- [ ] **Step 1: 写失败的测试**

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const h = vi.hoisted(() => ({ update: vi.fn(), create: vi.fn() }));

vi.mock("../hooks", () => ({
  useSupplierCategories: () => ({ data: [
    { id: "c-1", categoryCode: "RAW", categoryName: "原材料", parentId: null, level: 1, enabled: true, sortOrder: 10, createTime: null, remark: null },
    { id: "c-2", categoryCode: "RAW_ELEC", categoryName: "电子元器件", parentId: "c-1", level: 2, enabled: true, sortOrder: 11, createTime: null, remark: null },
  ] }),
  useCreateSupplierCategory: () => ({ mutateAsync: h.create, isPending: false }),
  useUpdateSupplierCategory: () => ({ mutateAsync: h.update, isPending: false }),
}));

import SupplierCategoryManage, { buildCategoryTree } from "./supplier-category-manage";

describe("SupplierCategoryManage", () => {
  beforeEach(() => { h.update.mockReset().mockResolvedValue(undefined); h.create.mockReset().mockResolvedValue({}); });

  it("nests a child category under its parent", () => {
    const tree = buildCategoryTree([
      { id: "c-1", categoryCode: "RAW", categoryName: "原材料", parentId: null, level: 1, enabled: true, sortOrder: 10, createTime: null, remark: null },
      { id: "c-2", categoryCode: "RAW_ELEC", categoryName: "电子元器件", parentId: "c-1", level: 2, enabled: true, sortOrder: 11, createTime: null, remark: null },
    ]);
    expect(tree).toHaveLength(1);
    expect(tree[0].children?.[0].categoryName).toBe("电子元器件");
  });

  it("resends the code and the name when a category is toggled", async () => {
    render(<SupplierCategoryManage />);
    await userEvent.click(await screen.findByRole("checkbox", { name: "原材料" }));
    await waitFor(() => expect(h.update).toHaveBeenCalled());
    const payload = h.update.mock.calls[0][0];
    expect(payload.data).toMatchObject({ categoryCode: "RAW", categoryName: "原材料", enabled: false });
  });

  it("does not offer a delete button", () => {
    render(<SupplierCategoryManage />);
    expect(screen.queryByRole("button", { name: "删除" })).not.toBeInTheDocument();
  });
});
```

- [ ] **Step 2: 跑测试确认失败** → FAIL

- [ ] **Step 3: 实现 `components/supplier-category-manage.tsx`**

- `buildCategoryTree(categories)`：先把有 `parentId` 且父存在的归到父的 `children`，
  其余当根；每层按 `sortOrder` 升序、再按 `categoryName` 兜底排序；
  返回的节点是 `{ ...category, children?: SupplierCategory[] }`
- 表格（缩进树）：分类名称（按层级缩进，左侧带层级徽标）/ 分类编码 / 层级 / 排序 / 启用开关 / 操作（编辑 / ＋子分类）
- 启用开关用 `components/ui/checkbox.tsx`（**仓库里没有 `switch.tsx`**，不要引用不存在的组件），
  `aria-label` 设为分类名；勾选框的 checked 态由 `enabled` 驱动
- 切换开关 → `update.mutateAsync({ id, data: { id, categoryCode, categoryName, parentId, level, sortOrder, enabled: next } })`
  —— **必须同时回传 `categoryCode` 与 `categoryName`**（后端 `validateCategory` 要求非空，spec §4.8）
- 「新增分类」/「编辑」Dialog：分类编码、名称、上级分类（`Select`，列出除自身外的所有分类）、排序、启用
  —— **没有「层级」字段**（服务端 `resolveLevel` 覆盖）
- **不渲染删除按钮**（spec §5.9）

- [ ] **Step 4: 创建 `src/app/[locale]/(app)/supplier/category/page.tsx`**

```tsx
"use client";

import { SupplierCategoryManage } from "@/features/supplier/components";

export default function SupplierCategoryPage() {
  return <SupplierCategoryManage />;
}
```

- [ ] **Step 5: 跑测试 + 类型检查 + 提交**

Run: `npx vitest run src/features/supplier/components/supplier-category-manage.test.tsx` → PASS
Run: `npm run typecheck` → 无错误

```bash
git add src/features/supplier/components/supplier-category-manage.tsx src/features/supplier/components/supplier-category-manage.test.tsx "src/app/[locale]/(app)/supplier/category/page.tsx"
git commit -m "feat(supplier): supplier category dictionary management"
```

---

### Task 14: 路由 + 菜单 + Suspense 边界

**Files:**
- Modify: `src/app/[locale]/(app)/supplier/page.tsx`
- Modify: `src/app/[locale]/(app)/supplier/[id]/page.tsx`
- Modify: `src/features/supplier/components/index.ts`
- Modify: `src/components/Layout/menu-config.tsx`
- Test: `src/components/Layout/menu-config.test.ts`

**Interfaces:**
- Consumes: Task 6/7/11/12/13 的所有组件
- Produces:
  - `src/app/[locale]/(app)/supplier/page.tsx` —— 用 `<Suspense fallback={<Skeleton className="h-64 w-full" />}>` 包住 `<SupplierList />`
    （`useSearchParams` 在客户端组件里必须有 Suspense 边界，否则 `next build` 失败 —— spec §4.9）
  - `src/app/[locale]/(app)/supplier/[id]/page.tsx` —— 保持现有取 id 的方式，渲染 `<SupplierDetail id={id} />`
  - `src/features/supplier/components/index.ts` —— 导出全部组件
  - `src/components/Layout/menu-config.tsx` —— 供应商改为子菜单组

- [ ] **Step 1: 写失败的测试**

`src/components/Layout/menu-config.test.ts`：

```ts
import { describe, expect, it } from "vitest";
import { appMenuItems, matchMenuKey, getParentGroupKey } from "./menu-config";

describe("supplier menu group", () => {
  it("exposes the three supplier pages under one group", () => {
    const group = appMenuItems.find((item) => (item as { key: string }).key === "group-supplier") as { children?: { key: string }[] };
    expect(group).toBeDefined();
    expect(group.children?.map((child) => child.key)).toEqual(["/supplier", "/supplier/audit", "/supplier/category"]);
  });

  it("matches the audit route to the supplier group", () => {
    expect(matchMenuKey("/zh-CN/supplier/audit")).toBe("/supplier/audit");
    expect(getParentGroupKey("/zh-CN/supplier/audit")).toBe("group-supplier");
  });
});
```

- [ ] **Step 2: 跑测试确认失败** → FAIL（`group-supplier` 不存在）

- [ ] **Step 3: 改 `src/components/Layout/menu-config.tsx`**

把

```tsx
getItem('供应商管理', '/supplier', <TeamOutlined />),
```

换成

```tsx
getItem('供应商管理', 'group-supplier', <TeamOutlined />, [
  getItem('供应商列表', '/supplier'),
  getItem('准入审核', '/supplier/audit'),
  getItem('分类字典', '/supplier/category'),
]),
```

**只改结构，不改 `menu-config.tsx` 的 antd 实现方式。**

- [ ] **Step 4: 改 `src/app/[locale]/(app)/supplier/page.tsx`**

```tsx
"use client";

import { Suspense } from "react";
import { Skeleton } from "@/components/ui/skeleton";
import { SupplierList } from "@/features/supplier/components";

export default function SupplierPage() {
  return (
    <Suspense fallback={<Skeleton className="h-64 w-full" />}>
      <SupplierList />
    </Suspense>
  );
}
```

（与 `src/app/[locale]/(app)/order/page.tsx` 的写法一致）

- [ ] **Step 5: 改 `src/features/supplier/components/index.ts`**

沿用现有文件的**命名 re-export + 单引号**风格：

```ts
export { default as SupplierList } from './supplier-list'
export { default as SupplierDetail } from './supplier-detail'
export { default as SupplierForm } from './supplier-form'
export { default as SupplierAuditWorkbench } from './supplier-audit-workbench'
export { default as SupplierCategoryManage } from './supplier-category-manage'
export { default as SupplierStats } from './supplier-stats'
export { default as SupplierFilters } from './supplier-filters'
export { default as SupplierActions } from './supplier-actions'
export { default as SupplierOverviewPanel } from './supplier-overview-panel'
export { default as SupplierCategoryPanel } from './supplier-category-panel'
export { default as SupplierContactPanel } from './supplier-contact-panel'
export { default as SupplierBankAccountPanel } from './supplier-bank-account-panel'
export { default as SupplierCertificatePanel } from './supplier-certificate-panel'
export { default as SupplierEvaluationPanel } from './supplier-evaluation-panel'
export { default as SupplierChangeLogPanel } from './supplier-change-log-panel'
export { default as RatingStars } from './rating-stars'
```

- [ ] **Step 6: 跑测试 + 类型检查 + lint + 提交**

Run: `npx vitest run src/components/Layout/menu-config.test.ts` → PASS
Run: `npm run typecheck` → 无错误
Run: `npm run lint` → 无错误

```bash
git add "src/app/[locale]/(app)/supplier/page.tsx" "src/app/[locale]/(app)/supplier/[id]/page.tsx" src/features/supplier/components/index.ts src/components/Layout/menu-config.tsx src/components/Layout/menu-config.test.ts
git commit -m "feat(supplier): wire routes, suspense boundary and the supplier menu group"
```

---

### Task 15: 端点覆盖核对 + 全量验证

**Files:**
- Modify: `src/lib/api/services/supplier.ts`（补注释，列出每个端点的调用点）

**Interfaces:**
- Consumes: 全部前置 task
- Produces: 无新接口；产出一份可复核的覆盖表（写在 `src/lib/api/services/supplier.ts` 的文件头注释里）

- [ ] **Step 1: 在 `src/lib/api/services/supplier.ts` 顶部写覆盖表注释**

在文件头写一段注释，逐个列出 48 个在范围端点与它们的界面调用点
（组件名 + hook 名）。格式：

```ts
/**
 * 供应商端点覆盖表（scm-supplier 共 62 个端点，其中 48 个在范围内；
 * 对账单 /api/v1/settlements 与采购明细 /api/v1/purchase-order-items 属财务与采购域，不在本模块）。
 *
 * | 端点 | 调用点 |
 * |---|---|
 * | GET  /suppliers | features/supplier/hooks/use-supplier.ts → useSupplierList（列表页 / 审核队列 / 分类筛选） |
 * | GET  /suppliers/{id} | useSupplierDetail（编辑页初值） |
 * ...
 * | GET  /suppliers/active | 仅暴露契约与服务层，供跨模块的供应商选择器取用，本模块无界面调用点（设计例外 1/2） |
 * | DELETE /supplier-categories/{id} | 仅暴露契约与服务层；分类字典页不暴露删除按钮（设计例外 2/2） |
 */
```

必须覆盖到全部 48 个。其中 **46 个有界面调用点**，2 个是**已记录的设计例外**
（`GET /suppliers/active`、`DELETE /supplier-categories/{id}`）——
理由要写在表里，不能静默漏掉。

- [ ] **Step 2: 跑全量单测**

Run: `npm test`
Expected: 全绿，既有 21 个测试文件的用例不回归

- [ ] **Step 3: 类型检查 + lint + 构建**

Run: `npm run typecheck` → 无错误
Run: `npm run lint` → 无错误
Run: `npm run build` → 构建成功（这一步同时验证 Suspense 边界与所有路由）

- [ ] **Step 4: 人工核对 6 条路由**

在 `scm-web` 目录跑 `npm run dev`，浏览器逐个访问：
`/zh-CN/supplier`、`/zh-CN/supplier/new`、`/zh-CN/supplier/<某个 id>`、
`/zh-CN/supplier/<某个 id>/edit`、`/zh-CN/supplier/audit`、`/zh-CN/supplier/category`
—— 每条都能渲染，不出现「供应商不存在」以外的空态，左侧菜单里「供应商管理」是展开的三项子菜单。

（这一步需要后端在跑；后端不在时至少确认页面不崩、显示 loading / 空态。）

- [ ] **Step 5: 提交**

```bash
git add src/lib/api/services/supplier.ts
git commit -m "docs(supplier): record the endpoint coverage table"
```

---

## Self-Review

**1. Spec coverage** —— 逐节核对 `docs/superpowers/specs/2026-10-30-supplier-frontend-completion-design.md`：

| spec 节 | 覆盖的 task |
|---|---|
| §4.1 请求形状（query vs body） | Task 1（12 个断言逐条钉住） |
| §4.2 PATCH 语义 | Task 4（"清空发 `""`"、"不托管字段不出现"） |
| §4.3 操作人 | Task 3（`useOperator`）+ Task 5（测试钉住 `operatorName` 回退到 username） |
| §4.4 错误消息 | Task 3（`api-error.test.ts` 三个用例） |
| §4.5 前置条件置灰 | Task 6（删除置灰）+ Task 12（税号缺失置灰）；合作状态流转矩阵不下发 —— Task 6 的下拉给全部 4 个目标值 |
| §4.6 `overallScore` 两套口径 | Task 2（优先 `overallScore / 20`，缺失才退回三维度均值） |
| §4.7 证照预警只含未来到期 | Task 12（"never offers a certificate that already expired"） |
| §4.8 分类 level 与启停回传 | Task 13（`buildCategoryTree` + 切换时回传编码与名称） |
| §4.9 Suspense 边界 | Task 14 |
| §4.10 locale 前缀 | Global Constraints（路由不带前缀） |
| §4.11 无 multi-select 组件 | Task 8（`checkbox.tsx` 实现多选） |
| §5.1 路由与菜单 | Task 11 / 12 / 13（新建 4 条）+ Task 14（改造 2 条 + 菜单） |
| §5.2 契约层 | Task 1 |
| §5.3 端点层 | Task 1 |
| §5.4 Feature 层结构 | Task 1–5（文件清单见 File Structure） |
| §5.5 列表页 | Task 6 |
| §5.6 详情页 7 分区 | Task 7（概览）+ 8（分类/联系人）+ 9（银行/证照）+ 10（评价/留痕） |
| §5.7 新建/编辑表单 | Task 11 |
| §5.8 审核工作台 | Task 12 |
| §5.9 分类字典 | Task 13 |
| §6 数据流与 query keys | Task 5 |
| §7 与既有代码的关系 | Global Constraints（`list`/`getById` 签名不变；dashboard/warehouse 不改） |
| §8 验收标准 | Task 15（typecheck / lint / test / build / 6 条路由 / 覆盖表） |

无遗漏。

**2. Step scan** —— 每个 step 都指定了文件、签名、测试名或断言值，没有 "TBD" / "处理边界情况" 这类空步骤。

**3. Type consistency** —— `Supplier` / `SupplierCategory` / `SupplierContact` / `SupplierBankAccount` /
`SupplierCertificate` / `SupplierEvaluation` / `SupplierChangeLog` / `SupplierStats` / `SupplierPerformance` /
`SupplierProfile` 的名字在 Task 2 定义，Task 6–13 引用一致；
`supplierKeys` / `supplierCategoryKeys` / `supplierEvaluationKeys` / `supplierCertificateExpiryKeys`
在 Task 5 定义，Task 6–13 引用一致；
`cleanApiError` / `errorMessage` / `useOperator` 在 Task 3 定义，Task 6/11/12 引用一致；
`toSupplierFormValues` / `toSupplierPayload` / `supplierFormSchema` / `supplierFormDefaults` 在 Task 4 定义，Task 11 引用一致。

**4. Review Focus** —— 5 条全部落位：第 1 条 → Task 4；第 2 条 → Task 3；第 3 条 → Task 12；
第 4 条 → Task 1（`assignCategories` 裸数组断言）；第 5 条 → Task 12（证照看板不出现已过期）。

**5. Proportion** —— plan 约 1100 行，spec 约 600 行，比例约 1.8 倍，在合理范围内：
代码块集中在**测试代码**（这是"要写什么"而非"要写实现"）与**接口签名**（防命名漂移），
实现体一律留给执行者。
