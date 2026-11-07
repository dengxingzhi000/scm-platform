# 网关前缀别名迁移（过渡方案）

## 状态与定位

- 别名路由是**过渡层，不是终态**。它让 `cognora-aura-web` 在服务注册名从旧名
  （`scm-*`、`lms-tms`、`ecom-*` 混合）迁移到规范名 `platform-*` 期间，继续通过
  网关直连模式工作。
- 代码位置：路由表在
  `platform/gateway/src/main/java/com/cognora/platform/gateway/config/GatewayPrefixAliasConfig.java`，
  开关在 `GatewayPrefixAliasProperties`。`application.yaml` 只保留
  `platform-auth` / `platform-system` 两条显式路由，**不再声明任何 `alias-*`**。
- 开关：`cognora.gateway.prefix-aliases.enabled`，默认 `false`
  （可用环境变量 `GATEWAY_PREFIX_ALIASES_ENABLED` 覆盖）。
- **关闭时别名路由完全不注册**（不是注册了但不匹配）：
  `GatewayPrefixAliasConfig` 的条件 `RouteDefinitionLocator` bean 根本不存在，
  网关只剩 discovery locator 生成的旧名路由，行为与引入别名之前完全一致。
- 开关只在启动时读取，**改动需要重启网关才生效**（本次变更未重启、未改动任何
  运行中的容器）。
- **本文件只写第二阶段清单，尚未修改前端仓库。**

## 别名路由表

全部别名均为 `order: -1`（低于 discovery locator 的默认 0，确保优先命中），
`StripPrefix=1` 后再转发到目标服务。**仅当
`cognora.gateway.prefix-aliases.enabled=true` 时才注册。**

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

### 硬前提：先确认所有目标服务已按新名注册，再打开开关

别名直接指向规范服务名。**只有当目标服务以该名字注册时，别名才可用。**

- 某服务仍注册旧名（如 `scm-audit`）时，一旦打开开关并重启网关，
  `/scm-audit/**` 会被 `alias-scm-audit`（`order: -1`）抢先匹配并转发到未注册的
  `lb://platform-audit`，返回 **503**；discovery locator 自动生成的
  `/scm-audit/**` 旧名路由不再有机会命中，**把本来能用的接口打挂**。
- 因此顺序固定为：**先确认所有目标服务已按新名注册 → 再打开开关 → 重启网关**。
  **未确认前保持开关关闭**（默认值）；关闭状态下零条别名参与匹配，请求继续走
  locator 的旧名路由。
- 开关是部署闸门，不是运行时热开关：改值必须重启网关才生效。

#### 打开开关前的核对清单

1. **记录现场**：记下当前网关镜像 tag / 提交号、`docker-compose.yml` 快照，
   以及一条当前可用的旧前缀请求（例如 `GET /scm-audit/sys-audit-log/page`）
   的响应码，作为回滚对照。
2. **核对注册名**（网关已开启 `show-details: always` / `show-components: always`）：

   ```bash
   curl -s http://<gateway-host>:8761/actuator/health \
     | jq '.components.discoveryComposite.components.discoveryClient.details.services'
   ```

   输出应为服务注册名数组。逐行对照上面的别名路由表：

   - 别名目标与旧名相同（`lb://scm-order`、`lb://scm-warehouse`、`lb://scm-supplier`、
     `lb://scm-purchase`、`lb://scm-finance`）→ 名称一致，不受影响。
   - 别名目标是规范名（`lb://platform-*`、`lb://ecom-*`、`lb://lms-tms`）→
     该名字**必须出现在上面的 `services` 数组里**；若仍只有旧名
     （`scm-audit`、`scm-member` 等），**保持开关关闭**。
   - 也可在 Nacos 控制台 `http://<nacos-host>:8848/nacos` 的服务列表交叉核对。

3. **判定**：数组中覆盖了全部规范名目标（或对应服务确认不再改名、别名目标沿用旧名）
   才可进入「打开开关」；否则停在关闭状态，先完成服务改名。

#### 打开开关（确认通过后）

1. 设置开关，二选一：

   ```yaml
   cognora:
     gateway:
       prefix-aliases:
         enabled: true
   ```

   或启动时给网关容器加环境变量 `GATEWAY_PREFIX_ALIASES_ENABLED=true`。

2. **只重启网关**，不动业务服务。
3. 核对启动日志（`GatewayPrefixAliasConfig` 的 `ApplicationRunner`）：

   ```text
   Gateway prefix aliases ENABLED: 25 transitional alias routes registered with order -1
   ```

   若是 `DISABLED: 0 of 25 alias routes registered ...`，说明开关没读到，按第 1 步重查。
4. 冒烟：逐个抽查 `/scm-*` 前缀接口返回 401/200（不再 503），并确认
   `/actuator/health` 正常。

#### 回滚（任何一步不符合预期）

1. 把 `cognora.gateway.prefix-aliases.enabled` 改回 `false`（或删除该环境变量）。
2. **重启网关**。
3. 启动日志应变为：

   ```text
   Gateway prefix aliases DISABLED: 0 of 25 alias routes registered; /scm-* paths resolve through the discovery locator
   ```

4. 复测第 1 步记录的旧前缀请求，响应码恢复为改动前一致；此时别名 bean 不存在，
   请求全部由 discovery locator 处理，无需回滚代码。

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
| 0 | 记录当前网关提交号与 VM `docker-compose.yml` 快照；确认开关当前为 `false` | 可一键回到此提交 | 无需回滚 |
| 1 | 逐服务修改 `spring.application.name` 为 `platform-*`（`lms-tms`、`ecom-*` 是否改名由各服务 owner 决定，未改名的保持现别名目标）并重启该服务 | 网关 `actuator/health` 中出现新名字；旧名消失 | 改回 `spring.application.name` 并重启该服务 |
| 2 | 按「打开开关前的核对清单」确认全部目标注册名就位后，置 `cognora.gateway.prefix-aliases.enabled=true`，**只重启网关** | 启动日志 `Gateway prefix aliases ENABLED: 25 ...`；`/scm-*` 返回 401/200 而非 503 | 置回 `false` 并重启网关（别名完全不注册，无需回滚代码） |
| 3 | 改 `platform/gateway/src/main/resources/application.yaml`：把 `/platform-*` 前缀加入签名白名单（浏览器组） | 直连网关的 `/platform-*` 请求不再因缺签名 401 | 撤掉白名单新增项 |
| 4 | 在 `cognora-aura-web` 仓库切换 `next.config.ts` rewrites 与 `src/lib/api/services/*` 常量到规范前缀，发独立 PR | Playwright/手工走查登录、订单、库存、审计、租户、产品等页面 | revert 前端 PR；别名仍在，旧前缀继续可用 |
| 5 | 观察至少一个发布周期：网关日志无别名路径 404/503，前端无旧前缀请求 | 旧前缀请求量为 0 | 同步骤 4 |
| 6 | 删除 `GatewayPrefixAliasConfig`（含开关、启动日志与相关 YAML 注释），删除浏览器组签名白名单与 `/auth/**` 认证放行 | 网关测试全绿；前端全功能回归 | revert 网关提交并重启网关 |

注意：

- 步骤 2 的核对不通过（任一目标仍只有旧名）时**停在关闭状态**，先回步骤 1；
  提前打开会让 `/scm-*` 转发到未注册的 `lb://platform-*` 并 503。
- 步骤 3 必须早于步骤 4。前端一旦改到 `/platform-*`，如果白名单没扩容，
  会以 `401 MISSING_PARAMETERS` 失败（签名过滤器对未放行路径仍然生效）。

## 删除别名（终态）判据

同时满足才删：

1. 所有目标服务均以规范名注册，旧名不再出现在 Nacos 服务列表。
2. 前端 `next.config.ts` 与 `src/lib/api/services/*` 已合入规范前缀，且稳定运行一个发布周期。
3. 网关访问日志中 `/auth`、`/system`、`/common`、`/scm-*` 请求量连续一个发布周期为 0
   （内部服务到服务调用不得依赖这些别名）。

删除后本文件应改为「已完成的迁移记录」，并在 `AGENTS.md` 的网关相关段落标注终态。

## 回滚手册

- **仅回滚别名（开关还在时）**：把 `cognora.gateway.prefix-aliases.enabled`
  置回 `false`（或删除 `GATEWAY_PREFIX_ALIASES_ENABLED`），**重启网关**即可；
  别名 bean 不注册，行为立即回到 locator 旧名路由，无需回滚代码。
- **回滚已合并的网关代码**：`git revert <merge commit>`（或恢复
  `GatewaySecurityConfig`/`GatewayCorsConfig`/`GatewayPrefixAliasConfig` 与
  `application.yaml`），重新打包并**重启网关**。
- **回滚前端**：revert 前端切换 PR；别名路由未删除时旧前缀立即恢复。
- **回滚服务改名**：把 `spring.application.name` 改回旧名并重启该服务，然后按步骤 3
  撤回 `/platform-*` 签名白名单；若别名已打开且该服务是规范名目标，先把开关置回
  `false` 再改回旧名，避免 `lb://platform-*` 暂时落空。改名前后的服务只能有一个
  注册名，避免 discovery locator 产生两条同名路由。
