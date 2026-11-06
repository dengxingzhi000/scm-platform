# 后端接口文档 (OpenAPI)

由运行中的服务实时抓取。

抓取方式：请求 `/api/public/v3/api-docs` 并附带 `X-Tenant-Id` 头
（多租户过滤器要求租户上下文，否则返回 401/500）。

```bash
node scripts/api/fetch-openapi.mjs <host> --out docs/api
```

| 服务 | 端口 | 路径数 | Schema 数 | 摘要 | 原始 JSON |
|------|------|--------|-----------|------|-----------|
| scm-system | 8081 | 54 | 49 | [scm-system.md](scm-system.md) | [openapi-scm-system.json](openapi-scm-system.json) |
| scm-auth | 8106 | 15 | 20 | [scm-auth.md](scm-auth.md) | [openapi-scm-auth.json](openapi-scm-auth.json) |
| scm-product | 8201 | 24 | 33 | [scm-product.md](scm-product.md) | [openapi-scm-product.json](openapi-scm-product.json) |
| scm-inventory | 8202 | 23 | 32 | [scm-inventory.md](scm-inventory.md) | [openapi-scm-inventory.json](openapi-scm-inventory.json) |
| scm-order | 8203 | 24 | 46 | [scm-order.md](scm-order.md) | [openapi-scm-order.json](openapi-scm-order.json) |
| scm-warehouse | 8204 | 32 | 31 | [scm-warehouse.md](scm-warehouse.md) | [openapi-scm-warehouse.json](openapi-scm-warehouse.json) |
| scm-logistics | 8205 | 29 | 29 | [scm-logistics.md](scm-logistics.md) | [openapi-scm-logistics.json](openapi-scm-logistics.json) |
| scm-supplier | 8206 | 39 | 48 | [scm-supplier.md](scm-supplier.md) | [openapi-scm-supplier.json](openapi-scm-supplier.json) |
| scm-purchase | 8207 | 85 | 29 | [scm-purchase.md](scm-purchase.md) | [openapi-scm-purchase.json](openapi-scm-purchase.json) |
| scm-finance | 8208 | 28 | 27 | [scm-finance.md](scm-finance.md) | [openapi-scm-finance.json](openapi-scm-finance.json) |
| scm-approval | 8209 | 10 | 11 | [scm-approval.md](scm-approval.md) | [openapi-scm-approval.json](openapi-scm-approval.json) |
| scm-audit | 8210 | 11 | 7 | [scm-audit.md](scm-audit.md) | [openapi-scm-audit.json](openapi-scm-audit.json) |
| scm-notify | 8211 | 20 | 14 | [scm-notify.md](scm-notify.md) | [openapi-scm-notify.json](openapi-scm-notify.json) |
| scm-tenant | 8212 | 39 | 25 | [scm-tenant.md](scm-tenant.md) | [openapi-scm-tenant.json](openapi-scm-tenant.json) |
| scm-document | 8213 | 13 | 22 | [scm-document.md](scm-document.md) | [openapi-scm-document.json](openapi-scm-document.json) |
| scm-file | 8214 | 8 | 12 | [scm-file.md](scm-file.md) | [openapi-scm-file.json](openapi-scm-file.json) |

## 网关路由

网关 (`:8761`) 只为 `auth` / `system` / `common` 配置了显式路由 (`StripPrefix=1`)，
其余服务通过 Nacos 服务发现定位器以 `/scm-<service>/**` 访问：

```
/platform-auth/api/auth/**     -> platform-auth      (:8106)
/platform-system/api/system/** -> platform-system   (:8081)
/platform-product/api/v1/**    -> platform-product  (:8201)
/platform-inventory/api/v1/**  -> platform-inventory(:8202)
/scm-order/api/v1/**         -> scm-order    (:8203)
/scm-warehouse/wms-*/**      -> scm-warehouse(:8204)
/lms-tms/api/v1/**            -> lms-tms(:8205)
/scm-supplier/**             -> scm-supplier (:8206)
/scm-purchase/pur-*/**       -> scm-purchase (:8207)
/scm-finance/**              -> scm-finance  (:8208)
/platform-approval/**         -> platform-approval (:8209)
/platform-audit/**            -> platform-audit    (:8210)
/platform-notify/**           -> platform-notify   (:8211)
/scm-tenant/**               -> scm-tenant   (:8212)
/scm-document/api/v1/**      -> scm-document (:8213)
/scm-file/**                 -> scm-file     (:8214)
```

## 生成 TypeScript 客户端

`cognora-aura-web/openapitools.json` 读取本目录的 JSON，生成 `src/lib/api/generated/`（前端已拆分到独立仓库）：

```bash
cd cognora-aura-web && npm run generate:api
```

生成结果按功能域拆分为独立 API 类（`OrderApi`、`UserApi`、`SpuApi` …），
代码只读，请勿手工修改——重新执行上面的命令即可刷新。

## 已知问题（抓取脚本已做归一化）

部署中的服务有两处不符合 OpenAPI 规范，`fetch-openapi.mjs` 在写盘前修正：

| 问题 | 原因 | 归一化 |
|------|------|--------|
| `components.securitySchemes` 的键为 `"Bearer Authentication"` | 键名必须匹配 `^[a-zA-Z0-9.\-_]+$`，openapi-generator 直接拒绝该文档 | 改为 `BearerAuthentication` |
| 每个操作的 `tags` 是中文（"订单管理"、"用户管理" …） | 生成器用 tag 推导 API 类名，无法把 CJK 字符串转成 ASCII 标识符，于是整个服务的操作被合并进一个 `DefaultApi`，方法名退化成 `getById1`/`getById2` | 映射为英文别名（`Order`、`User` …） |

`scm-common/web` 的 `OpenApiConfig` 已把 security scheme 名改成
`BearerAuthentication`，重新部署后不再需要脚本兜底。
