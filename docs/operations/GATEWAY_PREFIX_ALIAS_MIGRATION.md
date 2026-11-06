# 网关前缀别名迁移（过渡方案）

## 状态与定位

- 别名路由是**过渡层，不是终态**。它让 `cognora-aura-web` 在服务注册名从旧名
  （`scm-*`、`lms-tms`、`ecom-*` 混合）迁移到规范名 `platform-*` 期间，继续通过
  网关直连模式工作。
- 代码位置：`platform/gateway/src/main/resources/application.yaml` 中
  `spring.cloud.gateway.server.webflux.routes` 下的 `alias-*` 路由。
- **本文件只写第二阶段清单，尚未修改前端仓库。**
- 别名在网关重启后才会生效（本次变更未重启、未改动任何运行中的容器）。

## 别名路由表

全部别名均为 `order: -1`（低于 discovery locator 的默认 0，确保优先命中），
`StripPrefix=1` 后再转发到目标服务。

| Path | 目标服务（lb URI） | route id |
|------|--------------------|----------|
| `/auth/**` | `lb://platform-auth` | `alias-auth` |
| `/system/**` | `lb://platform-system` | `alias-system` |
| `/common/**` | `lb://platform-system` | `alias-common` |
| `/scm-product/**` | `lb://platform-product` | `alias-scm-product` |
| `/scm-inventory/**` | `lb://platform-inventory` | `alias-scm-inventory` |
| `/scm-order/**` | `lb://scm-order` | `alias-scm-order` |
| `/scm-warehouse/**` | `lb://scm-warehouse` | `alias-scm-warehouse` |
| `/scm-logistics/**` | `lb://lms-tms` | `alias-scm-logistics` |
| `/scm-supplier/**` | `lb://scm-supplier` | `alias-scm-supplier` |
| `/scm-purchase/**` | `lb://scm-purchase` | `alias-scm-purchase` |
| `/scm-finance/**` | `lb://scm-finance` | `alias-scm-finance` |
| `/scm-approval/**` | `lb://platform-approval` | `alias-scm-approval` |
| `/scm-audit/**` | `lb://platform-audit` | `alias-scm-audit` |
| `/scm-notify/**` | `lb://platform-notify` | `alias-scm-notify` |
| `/scm-tenant/**` | `lb://platform-tenant` | `alias-scm-tenant` |
| `/scm-document/**` | `lb://platform-document` | `alias-scm-document` |
| `/scm-file/**` | `lb://platform-file` | `alias-scm-file` |
| `/scm-analytics/**` | `lb://ecom-analytics` | `alias-scm-analytics` |
| `/scm-mall/**` | `lb://ecom-mall` | `alias-scm-mall` |
| `/scm-member/**` | `lb://ecom-member` | `alias-scm-member` |
| `/scm-promotion/**` | `lb://ecom-promotion` | `alias-scm-promotion` |
| `/scm-payment/**` | `lb://ecom-payment` | `alias-scm-payment` |
| `/scm-order-center/**` | `lb://ecom-order-center` | `alias-scm-order-center` |
| `/scm-fulfillment/**` | `lb://ecom-fulfillment` | `alias-scm-fulfillment` |
| `/scm-search/**` | `lb://ecom-search` | `alias-scm-search` |

同时保留 `platform-auth` / `platform-system` 两条原有显式路由（`order: -1`），
并删除指向不存在服务的 `/platform-common/**` 死路由。

### 硬前提：先改名，再重启网关

别名直接指向规范服务名。**只有当目标服务以该名字注册时，别名才可用。**

- 某服务仍注册旧名（如 `scm-audit`）时，重启网关后 `/scm-audit/**` 会被
  `alias-scm-audit`（`order: -1`）抢先匹配并 `503`，discovery locator 自动生成的
  `/scm-audit/**` 路由不再生效。
- 因此第二阶段必须**先把服务改名并重启服务，最后再重启网关**。
- 注册名核对：`GET http://<gateway>:8761/actuator/health` →
  `components.discoveryComposite.components.discoveryClient.details.services`。

## 与本变更同时打开的三道门

### 1. CORS（`cognora.gateway.cors.allowed-origins`）

- 由 `GatewayCorsConfig` 注册的响应式 `CorsConfigurationSource` 供
  `GatewaySecurityConfig` 的 `.cors(...)` 使用，preflight 在 JWT 认证之前应答。
- 默认值：`http://localhost:*`（本地任意端口）与 `http://192.168.0.121:3000`（VM 前端）。
- 允许方法 `GET/POST/PUT/PATCH/DELETE/OPTIONS/HEAD`，`allow-credentials=true`，`max-age=3600`。
- 调整来源只改配置，不改代码：

  ```yaml
  cognora:
    gateway:
      cors:
        allowed-origins:
          - http://localhost:*
          - http://192.168.0.121:3000
  ```

### 2. 签名白名单（按路径分组，不关闭过滤器）

`security.signature.enabled` 仍为 `true`，`ApiSignatureFilter` 照常执行。只在
`security.signature.whitelist` 新增**浏览器侧前缀组**：

| 已放宽（浏览器 JWT） | 仍未放宽（必须 `X-Timestamp`/`X-Nonce`/`X-Signature`/`X-App-Id`） |
|----------------------|-------------------------------------------------------------------|
| `/auth/**` | `/platform-system/**` |
| `/system/**` | `/platform-product/**`、`/platform-inventory/**` 等其余 `/platform-*` |
| `/common/**` | `/ecom-*/**`（渠道回调除外） |
| `/scm-*/**` | `/lms-*/**`（物流回调除外） |

既有白名单保持不变：`/platform-auth/**`（历史遗留的登录/授权前缀）、`/public/**`、
`/actuator/**`、`/ecom-payment/api/v1/payments/callback/**`、
`/lms-tms/api/v1/tms-tracking/callback/**`。后两者由渠道自身验签。

`application-local.yaml` 的白名单同步新增同一组浏览器前缀（该 profile 独立覆盖
`application.yaml` 的同名列表）。

### 3. 认证放行（`GatewaySecurityConfig`）

在原有 `/platform-auth/api/auth/login`、`/refresh`、`/oauth2/**`、`/api/public/**`
之外，新增别名前缀的同名端点：

- `/auth/api/auth/login`
- `/auth/api/auth/refresh`
- `/auth/oauth2/**`
- `/auth/api/public/**`

## 第二阶段：切换清单（本轮不执行）

顺序固定：**服务改名 → 前端切前缀 → 观察 → 删除别名**。

| 步骤 | 操作 | 验收 | 回滚点（失败时） |
|------|------|------|------------------|
| 0 | 记录当前网关提交号与 VM `docker-compose.yml` 快照 | 可一键回到此提交 | 无需回滚 |
| 1 | 逐服务修改 `spring.application.name` 为 `platform-*`（`lms-tms`、`ecom-*` 是否改名由各服务 owner 决定，未改名的保持现别名目标）并重启该服务 | 网关 `actuator/health` 中出现新名字；旧名消失 | 改回 `spring.application.name` 并重启该服务 |
| 2 | 改 `platform/gateway/src/main/resources/application.yaml`：把 `/platform-*` 前缀加入签名白名单（浏览器组） | 直连网关的 `/platform-*` 请求不再因缺签名 401 | 撤掉白名单新增项 |
| 3 | 在 `cognora-aura-web` 仓库切换 `next.config.ts` rewrites 与 `src/lib/api/services/*` 常量到规范前缀，发独立 PR | Playwright/手工走查登录、订单、库存、审计、租户、产品等页面 | revert 前端 PR；别名仍在，旧前缀继续可用 |
| 4 | 观察至少一个发布周期：网关日志无别名路径 404/503，前端无旧前缀请求 | 旧前缀请求量为 0 | 同步骤 3 |
| 5 | 删除全部 `alias-*` 路由，删除浏览器组签名白名单与 `/auth/**` 认证放行 | 网关测试全绿；前端全功能回归 | revert 网关提交并重启网关 |

注意：步骤 2 必须早于步骤 3。前端一旦改到 `/platform-*`，如果白名单没扩容，
会以 `401 MISSING_PARAMETERS` 失败（签名过滤器对未放行路径仍然生效）。

## 删除别名（终态）判据

同时满足才删：

1. 所有目标服务均以规范名注册，旧名不再出现在 Nacos 服务列表。
2. 前端 `next.config.ts` 与 `src/lib/api/services/*` 已合入规范前缀，且稳定运行一个发布周期。
3. 网关访问日志中 `/auth`、`/system`、`/common`、`/scm-*` 请求量连续一个发布周期为 0
   （内部服务到服务调用不得依赖这些别名）。

删除后本文件应改为「已完成的迁移记录」，并在 `AGENTS.md` 的网关相关段落标注终态。

## 回滚手册

- **仅回滚网关**：`git revert <merge commit>`（或恢复 `application.yaml` 与
  `GatewaySecurityConfig`/`GatewayCorsConfig`），重新打包并**重启网关**。
- **回滚前端**：revert 前端切换 PR；别名路由未删除时旧前缀立即恢复。
- **回滚服务改名**：把 `spring.application.name` 改回旧名并重启该服务，然后按步骤 2
  撤回 `/platform-*` 签名白名单。改名前后的服务只能有一个注册名，避免 discovery
  locator 产生两条同名路由。
