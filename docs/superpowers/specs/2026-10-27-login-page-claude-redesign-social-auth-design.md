# 登录页重构与 Google/Apple 社交登录接入设计

日期:2026-10-27
状态:待批准(用户批准后进入 writing-plans)
范围:`scm-web` 登录/找回密码页 + `scm-auth` 社交登录端点 + `scm-system` 社交账号绑定

## 背景与问题

用户提供了 Anthropic Claude 登录页的参考图,要求复刻到本项目的登录页;并在后续决策中
追加了「接入 Google / Apple 社交登录」。

当前登录页(`scm-web/src/app/[locale]/(auth)/login/page.tsx`)是 Ant Design 实现的:

- `AuthBrandCard`(antd `Card`)+ 靛蓝渐变背景(`--auth-bg`),居中单栏卡片
- 表单用 antd `Form`/`Input`/`Checkbox`,硬编码中文文案
- 支持用户名 + 密码 + 记住我 + 忘记密码,以及 TOTP 两步验证分支

参考图是另一种形态:双栏、暖象牙底、衬线大标题、白卡片、细边框浅色按钮、近黑主按钮、
右侧满高图片、第三方登录按钮 + 「OR」分隔线。

社交登录不是「接线」,原因见「已探明的现状」。

## 已探明的现状(决定了设计落点)

### 1. 社交登录的 schema 与实体**已经存在**

| 资产 | 位置 | 状态 |
|------|------|------|
| `sys_user_oauth` 表 | `scripts/db/microservices/001_db_user.sql` | 可直接用,不建新表 |
| `SysUserOauth` 实体 | `scm-system/.../domain/entity/SysUserOauth.java` | 可直接用 |
| `SysUserOauthMapper` | `scm-system/.../mapper/SysUserOauthMapper.java` | 已有 `findByProviderAndOpenid` / `findByUserIdAndProvider` / `existsByUserIdAndProvider` / `updateLastLoginTime` / `unbind`;**但全仓无人引用它**,整条绑定链路是新的 |

`sys_user_oauth` 关键列:

```
provider        VARCHAR(32)  CHECK (provider IN ('google','github','apple','wechat','dingtalk','feishu'))
oauth_openid    VARCHAR(255) NOT NULL   -- provider 的稳定主体标识
oauth_union_id  VARCHAR(255)
oauth_email     VARCHAR(128)
oauth_nickname  VARCHAR(64)
oauth_avatar    VARCHAR(512)
raw_user_info   JSONB
UNIQUE (provider, oauth_openid)          -- 注意:不含 deleted
FK user_id -> sys_user(id) ON DELETE CASCADE
```

`provider` 的 CHECK 约束**已经包含 `google` 与 `apple`**,并预留了 github/wechat/dingtalk/feishu。
身份主键是 provider 的 subject id 而非 email(`oauth_email` 可空且只有普通索引,正符合
「Apple 只在首次授权返回 email」的现实)。

### 2. 平台令牌的签发入口已经支持「认证方式」

`JwtUtils.generateAccessToken(userId, username, roles, permissions, deviceId, ipAddress, amr)`
的第 7 个参数 `amr`(Authentication Methods References)就是为多认证方式设计的。
现状:密码登录 `amr = ["pwd"]`,密码 + TOTP `amr = ["pwd","mfa"]`。
社交登录用 `amr = ["google"]` / `["apple"]`,不需要新造机制。

### 3. 为什么社交登录不能「接线」,必须新写一段流程

| # | 事实 | 后果 |
|---|------|------|
| a | `scm-auth` 的 OAuth2 是 **Spring Authorization Server**(它是签发方),不是社交登录聚合器。全仓没有 google/apple 的 `ClientRegistration`,没有 `oauth2Login()` 配置,没有 `/oauth2/authorization/google` 端点 | 「使用 Google 继续」当前**没有任何可指向的地址** |
| b | 网关与业务服务走 `JwtAuthenticationFilter` → `jwtUtils.validateToken()` 校验自定义 JWT;授权服务器用自己的 RSA 密钥和另一套 claims 签发 | OAuth2 授权服务器的令牌**网关直接校验失败**,必须换成平台 JWT |
| c | `nearsync-web` 是 confidential 客户端(client secret 不能进浏览器);`nearsync-mobile` 虽是 public + PKCE,但回调是 `nearsync://callback` 自定义协议 | 现有客户端**没有一个可用于浏览器 SPA** |
| d | 已注册的回调 `http://localhost:3000/callback`、`/authorized` 两条路由前端**都不存在** | 授权码流程没有落点 |
| e | `RegisteredClientRepository` 与 `OAuth2AuthorizationService` 都是**内存实现** | 客户端与授权码重启即丢 |

补充定位修正:`AuthorizationServerConfig` 位于 `scm-common/web/.../uaa/config/`(不在 `scm-auth`),
由 `scm-auth` 通过 `security.uaa.enabled: true` 启用;网关不跑 `JwtAuthenticationFilter`,
`/auth/**` 在网关的签名白名单里。结论不变,机制描述以本文为准。

### 4. 其它可复用的既有资产

- `PkceAuthorizationCodeTokenResponseClient` + `PkceChallengeStore`(`scm-common/web`),
  `@ConditionalOnProperty("security.oauth2.pkce.enabled")` —— 已有的 PKCE 能力
- `UserDubboService.getUserByUsername(username)` → `SecurityUser`(含 roles + permissions)
- `useAuthStore.login(user, accessToken, refreshToken)` 写 `access_token` cookie,
  `proxy.ts` 用它做登录态守卫
- `scm-common/core` 已有 `RedisLuaScriptRegistry` 与 Redis 客户端(登录限流/账号锁定都在 Redis)
- `next.config.ts` 已把 `/auth/:path*` 重写到网关(`API_PROXY_TARGET`),浏览器导航可用同源路径

### 5. 三处「安全网」现状 —— 直接决定社交登录能不能跑通

> 这一节是对初稿的**修正**。初稿只看到 MyBatis 层的租户拦截器,漏了 HTTP 层那个活的。

| 组件 | 状态 | 对社交登录的影响 |
|------|------|-----------------|
| `TenantFilter`(`scm-common/core/.../tenant/TenantFilter.java`,`@Component`,`HIGHEST_PRECEDENCE+10`) | **活的**。`scm.tenant.required` 默认 `true`(`AsyncTenantAutoConfiguration` 里 `new TenantProperties(true, true, excludePaths)`),`scm-auth` 的 yaml 没有覆盖 | **缺 `X-Tenant-Id` 的请求直接抛 `TenantParseException`**。而 `/authorize` 是浏览器导航(axios 拦截器不参与),Apple 的 `form_post` 回调是来自 Apple 服务器的跨域 POST —— **两者都带不了这个头** |
| `SecurityConfig`(`scm-common/web/.../security/config/SecurityConfig.java`) | **活的**。permitAll 只有 `/api/auth/login`、`/api/auth/register`、`/api/auth/refresh`、`/api/auth/logout`、`/api/public/**`,其余 `anyRequest().authenticated()` | 新增的 `/api/auth/oauth2/**` 不加进去就是 **401** |
| `TenantInterceptor`(`scm-common/core/.../tenant/TenantInterceptor.java`,MyBatis 层) | **死代码**。`MybatisPlusConfig`(`scm-common/data`)只注册了分页、乐观锁、防全表更新三个拦截器,没有任何配置把它 `addInnerInterceptor` 进链 | 读写 `sys_user` / `sys_user_oauth`(两表**都没有 `tenant_id` 列**)不会被注入租户条件 |

结论:**社交登录端点必须同时做两件事** —— 加进 `scm-auth` 的 `scm.tenant.excludePaths`,
以及加进 `SecurityConfig` 的 permitAll 列表。两处都是必须项,不是可选优化。

### 6. 其它会影响实现的既有约束

| 事实 | 位置 | 影响 |
|------|------|------|
| `StepUpFilter` 只在 `amr.contains("mfa")` 时放行 MFA 步进 | `scm-common/web/.../stepup/StepUpFilter.java` | `amr=["google"]` 的会话**永远无法通过 MFA 步进校验** |
| `SysUser.tenantId` 声明了 `@TableField(value = "tenant_id", fill = FieldFill.INSERT)`,但实际建表脚本 `001_db_user.sql` 里 `sys_user` **没有 `tenant_id` 列** | `scm-system/.../entity/SysUser.java` vs `001_db_user.sql` | 有租户上下文时 MyBatis-Plus 会把 `tenant_id` 写进 INSERT → **列不存在,插入失败**。`AuditMetaObjectHandler` 在无租户上下文时跳过填充(null 字段被 MyBatis-Plus 的 NOT_NULL 策略省略,插入才能过) |
| `sys_user.email` **没有唯一约束**(只有普通索引 `idx_user_email`) | `001_db_user.sql` | 「按已验证 email 找用户」可能匹配多个 → 需要歧义处理规则 |
| `ISysUserCommandService.addUser` 不能直接复用 | `SysUserCommandServiceImpl` | 它会调 `TenantValidationUtil.getRequiredTenantId()`(无租户即抛)、`permissionChecker.requirePermission(operatorId, "user:add")`(社交登录没有 operator),并强制 `forceChangePassword=true` + `passwordExpireTime=now+90d` —— 密码是随机 UUID 的用户会被要求去改一个自己不知道的密码 |
| `SysUserMapper` **没有 `findByEmail`** | `scm-system/.../mapper/SysUserMapper.java` | 需要新增 |
| `uk_provider_openid` 唯一约束**不含 `deleted`**,而 `unbind` 是软删除 | `001_db_user.sql` + `SysUserOauthMapper.unbind` | 解绑后该 `(provider, openid)` 仍占用唯一键 → 重新绑定会冲突 |
| `scm-auth` 跑着 `@Order(1)` 的授权服务器链(`securityMatcher(getEndpointsMatcher())`) | `AuthorizationServerConfig` | 需确认 `/api/auth/oauth2/**` 不会被它捕获 |
| `e2e/login.spec.ts` 断言 `h1` 含 `'Login'`、`input[name="username"]`、`input[name="password"]`、跳 `/dashboard` | `scm-web/e2e/login.spec.ts` | 重做后 H1 文案变了、默认 locale 是 zh-CN → 该 spec 需要同步更新 |
| `apiClient` 响应拦截器对**任何** 401 都尝试刷新 token,失败后 `window.location.href = '/login'` | `scm-web/src/lib/api/client.ts` | 票据无效/过期/重复消费会**静默跳走**,回调页的错误态显示不出来 |
| `completeLogin` 是 `login/page.tsx` 内的闭包,**没有导出** | `scm-web/src/app/[locale]/(auth)/login/page.tsx:44` | 「零改动复用」需要先抽成共享模块 |

## 目标

1. 登录页视觉复刻参考图:双栏构图、暖象牙底、衬线大标题、白卡片、细边框浅色按钮、
   近黑主按钮、右侧满高圆角图片、第三方登录按钮 + OR 分隔线、底部细则
2. 登录页从 Ant Design 迁移到 shadcn/ui + Tailwind v4(遵循 `AGENTS.md` 的「不新增 antd 页面」规矩),
   表单换 React Hook Form + Zod
3. 登录页文案接入 next-intl,zh-CN / en-US 双语,消除硬编码中文
4. 真正可用的 Google / Apple 社交登录:provider 授权码 → 本地账号 → 平台 JWT → 前端会话
5. 账号密码登录与 TOTP 两步验证**行为零回归**

## 非目标(YAGNI)

- 不改造现有 Spring Authorization Server(`AuthorizationServerConfig`),不把它变成社交登录代理
- 不实现 `sys_user_oauth.provider` 里预留的 github / wechat / dingtalk / feishu
- 不做「用社交账号注册」之外的账号合并/解绑/多身份绑定 UI
- 不改租户隔离实现(第 3 层死代码问题另行处理,见「已知遗留」)
- 不引入新的前端依赖(不用 sonner 等 toast 库,复用 antd 的 `App` 弹提示)
- 不做整个平台品牌色改造 —— 仅登录体验用 Claude 橙,其余页面保持靛蓝(见「已知取舍」)

## 反目标(怎样算失败)

- 社交登录按钮点了之后不能真正登入(比没有按钮更糟)
- provider 未配置凭据时仍渲染按钮(点了必然失败)
- 现有用户名密码登录、TOTP、记住我、忘记密码任一功能回归
- 令牌或一次性票据泄漏进 URL / 日志 / referrer
- 社交登录产出的令牌网关不认(等于没登录)
- 被禁用(status=0)或锁定(status=2)的账号通过社交登录进得来
- 新端点因为缺 `X-Tenant-Id` 或缺 permitAll 而 401/500

## 约束

1. **社交流程必须以平台 JWT 收尾**,不能以 provider 令牌或 OAuth2 授权服务器令牌收尾
2. **client secret 不能进浏览器**。授权码交换、userinfo 拉取全部在后端
3. **Apple 的特殊性**:
   - 需要付费 Apple Developer 账号、Services ID、HTTPS 回调地址
   - client secret 是用团队私钥 ES256 签名的 JWT,有效期最长 6 个月,需定期轮换
   - Web 端 Apple 推荐 `response_mode=form_post`,回调是 **POST 表单**,不是 query
   - Apple **仅在首次授权**返回用户邮箱与姓名,之后只返回 `sub` → 必须持久化到
     `sys_user_oauth`(这正是 `oauth_openid` 唯一约束 + `oauth_email`/`oauth_nickname` 存在的理由)
   - Apple 的 Web 授权码流程不支持 PKCE
4. **Google**:`openid email profile` scope;email 需校验 `email_verified == true`
5. provider 凭据(Google client id/secret、Apple service id / ES256 签名 secret)**只有用户能提供**,
   代码无法自行创建控制台应用
6. 前端 `proxy.ts` 无 `access_token` cookie 时会把非公开路径重定向到 `/login`,
   回调路由必须加入 `publicPaths`,否则票据会在重定向中丢失
7. `scm-auth` 的新端点走网关 `/auth/**` 路由(`StripPrefix=1`),
   前端调用路径为 `/auth/api/auth/oauth2/**`
8. 后端要把浏览器送回前端,必须知道前端基址 → 新增配置项

## 设计

### A. 视觉层(纯前端,不依赖社交登录)

#### A1. 设计令牌

`src/lib/design-tokens.css` 是 Ant Design / Tailwind / canvas 的唯一色源,新增 auth 域令牌:

```css
/* Claude 强调色(仅登录体验) */
/* 命名带 brand- 前缀是刻意的:`globals.css` 的 `@theme inline` 里已经有
   `--color-accent: var(--accent)`(shadcn 的 muted-accent,驱动 `bg-accent` 工具类),
   同名会覆盖它。 */
--color-brand-accent:        #D97757;
--color-brand-accent-hover:  #C2603F;
--color-brand-accent-active: #A95134;
--color-brand-accent-bg:     #F5E7E0;

/* 登录页暖中性色 */
--auth-bg:             #F0EEE6;   /* 暖象牙底 */
--auth-surface:        #FFFFFF;   /* 卡片 */
--auth-ink:            #262624;   /* 主文字 / 主按钮底 */
--auth-ink-soft:       #6B6A63;   /* 次级文字 */
--auth-line:           #E5E3DA;   /* 细描边 */
--auth-line-strong:    #D4D1C5;
--auth-btn-soft-bg:    #E9E6DC;   /* 浅色次按钮 */
--auth-btn-soft-hover: #DFDBCE;
--auth-btn-fg:         #FFFFFF;
```

新增衬线字体栈(中英各落到对应字族):

```css
--font-serif: Georgia, 'Times New Roman', 'Songti SC', 'Noto Serif CJK SC',
              'Source Han Serif SC', serif;
```

在 `src/app/globals.css` 的 `@theme inline` 块里把 `--font-serif` 暴露成 Tailwind 的
`font-serif` 工具类(与现有 shadcn 变量别名同一处)。

**作用域**:登录外壳根元素上覆写 shadcn 变量:

```
style="--primary: var(--color-brand-accent); --ring: var(--color-brand-accent); --accent: var(--color-brand-accent-bg);"
```

这样登录页内的 shadcn `Button`/链接/focus ring 走橙色,而**全局** `--color-primary`(靛蓝)
不动 —— 仪表盘等其它页面保持现状。

#### A2. 双栏外壳

改写 `src/app/[locale]/(auth)/layout.tsx`(替换现有 `src/layouts/auth-layout.tsx`):

```
<main class="min-h-dvh bg-[var(--auth-bg)] lg:grid lg:grid-cols-[1fr_minmax(0,42%)]">
  <section class="flex flex-col px-6 py-6 lg:px-12">
    <header class="flex items-center justify-between">
      <BrandMark />                        {/* 橙色标记 + 字标 */}
      <div class="flex gap-2">
        <Button variant="outline">联系销售</Button>
        <Button variant="solid-ink">试用平台</Button>
      </div>
    </header>
    <div class="flex flex-1 items-center justify-center py-12">
      <div class="w-full max-w-[520px]">{children}</div>
    </div>
  </section>
  <aside class="hidden p-6 lg:block">
    <img src="/auth-hero.svg" class="h-full w-full rounded-2xl object-cover" />
  </aside>
</main>
```

- `lg` 以下隐藏右栏,单栏;`lg` 以上双栏
- 卡片宽 `max-w-[520px]`
- 顶栏两个按钮:`联系销售` 描边(浅底 + 细边),`试用平台` 近黑实心

#### A3. 登录卡片

白底(`--auth-surface`)、1px `--auth-line` 描边、`rounded-2xl`、极轻阴影。自上而下:

1. `使用 Google 继续` —— 浅底按钮(`--auth-btn-soft-bg`),左侧 Google 彩色 G 图标(SVG)
2. `使用 Apple 继续` —— 同上,左侧 Apple 苹果图标(SVG)
3. `或` 分隔线 —— 上下 1px `--auth-line`,中间小字 `--auth-ink-soft`
4. 用户名输入 —— shadcn `Input`,`h-12`,`rounded-lg`,`name="username"`,聚焦环橙色
5. 密码输入 —— shadcn `Input type="password"`,`name="password"`,同尺寸
6. `记住我` 勾选 + `忘记密码?` 链接 一行左右分布
7. `登录` 主按钮 —— 近黑实心,`h-12`,全宽,hover 加深,`type="submit"`
8. 细则 —— 11px `--auth-ink-soft` 居中,含「服务条款」「隐私政策」两个链接

主标题与副标题在卡片**上方**(参考图里在卡片外):

- H1 衬线,桌面 56px / 移动 32px,`--auth-ink`
- 副标题衬线,桌面 20px,`--auth-ink-soft`

#### A4. 表单与交互

- shadcn `Form` + React Hook Form + Zod(`zodResolver`)
- 校验:用户名必填;密码必填
- TOTP 分支保留:服务端返回 `40104` 或消息匹配 `two-factor|verification code|MFA` 时切到验证码步骤
  (`isMfaRequired` 现有逻辑原样保留)
- **把 `completeLogin` 从 `login/page.tsx` 抽到共享模块**(如 `src/lib/auth/complete-login.ts`),
  供登录页与社交回调页共用:
  `useAuthStore.login` → `setPermissions` → `authService.getUserInfo()` 补全资料 →
  `queryClient.clear()` → `router.push('/{locale}/dashboard')`
- 失败提示继续用 antd `App.useApp()` 的 `message`(antd 的 `App` 挂在 `ThemeProvider` 上,全局可用;
  页面本身不再出现 antd 组件)
- 忘记密码链接指向 `/{locale}/forgot-password`(不变)

#### A5. 找回密码页

同样迁移到新外壳 + shadcn(它与登录页共用外壳与卡片,留在 antd 会呈现半迁移状态)。
保留现有三步流程(验证邮箱 → 验证身份 → 重置密码)与全部文案,只换组件与样式。

#### A6. 占位图资源

`public/auth-hero.svg` —— 暖调抽象 SVG(米色渐变 + 柔化的几何块),非照片。
后续换成实拍图只需替换该文件,布局无需改动。

#### A7. i18n

`messages/{zh-CN,en-US}.json` 新增 `login` 命名空间,覆盖全部文案(标题、副标题、按钮、
占位符、校验错误、细则、页脚按钮、社交按钮名)。zh-CN / en-US 各一套。

#### A8. 交付物清单(前端)

| 动作 | 文件 |
|------|------|
| 改写 | `src/app/[locale]/(auth)/layout.tsx` |
| 改写 | `src/app/[locale]/(auth)/login/page.tsx` |
| 改写 | `src/app/[locale]/(auth)/forgot-password/page.tsx` |
| 新增 | `src/components/auth/auth-shell.tsx`、`login-form.tsx`、`totp-form.tsx`、`social-auth-buttons.tsx`、`brand-mark.tsx` |
| 新增 | `src/lib/auth/complete-login.ts`(从 login 页抽出的共享会话建立逻辑) |
| 删除 | `src/components/ui/auth-brand-card.tsx`(antd,两处引用一并移除)、`src/layouts/auth-layout.tsx`(antd) |
| 修改 | `src/lib/design-tokens.css`、`src/app/globals.css`、`messages/zh-CN.json`、`messages/en-US.json`、`src/proxy.ts`(加 `/auth/callback` 到 publicPaths) |
| 修改 | `e2e/login.spec.ts`(H1 文案与 locale 前缀断言需同步) |
| 新增资源 | `public/auth-hero.svg` |

### B. 社交登录(Google / Apple)

#### B1. 总体流程

```
浏览器                     scm-auth                    provider
  |  点「使用 Google 继续」    |                          |
  |-------------------------->|                          |
  |  302 → provider 授权页     |                          |
  |--------------------------------------------------->|
  |                            |   302 ← code + state    |
  |                            |<------------------------|
  |                            | 校验 state / 换 token / 拉 profile |
  |                            | 绑定或查找本地账号        |
  |                            | 签发平台 access/refresh token |
  |                            | 生成一次性票据            |
  |  302 ← {前端基址}/{locale}/auth/callback?ticket=xxx  |
  |<---------------------------|                          |
  |  POST /auth/api/auth/oauth2/exchange { ticket }       |
  |-------------------------------------------------------->|
  |  LoginResponse(平台 JWT)  |                          |
  |<---------------------------|
  |  写 access_token cookie,跳 dashboard                    |
```

**为什么用一次性票据而不是把 token 放 URL**:避免令牌进浏览器历史、referrer、网关访问日志。
前端回调页拿票据换一次 `LoginResponse`,**与密码登录返回结构完全一致**,抽出的
`completeLogin()` 零改动复用。

#### B2. 后端端点契约

全部挂在 `scm-auth` 的 `/api/auth/oauth2` 下(网关路径 `/auth/api/auth/oauth2/**`):

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/oauth2/providers` | 返回已配置的 provider 列表,例如 `["google","apple"]`。未配置凭据的不出现在列表里。**沿用仓库的 `ApiResponse<List<String>>` 信封**(scm-auth 其它端点都是这个形状) |
| GET | `/api/auth/oauth2/{provider}/authorize` | 生成 `state`(存 Redis,TTL 5 分钟,值含 `state` + `locale` + `returnTo`),302 到 provider 授权页。Google:`response_type=code&scope=openid email profile`;Apple:`response_type=code&response_mode=form_post&scope=name email` |
| GET/POST | `/api/auth/oauth2/{provider}/callback` | 接收 provider 回跳。**同时接受 GET query 与 POST form**(Apple 是 form_post)。校验 `state` → 用 client secret 换 token → 拉 profile → 绑定/查找本地账号 → 签发平台令牌 → 生成一次性票据 → 302 到 `{前端基址}/{locale}/auth/callback?ticket=...` |
| POST | `/api/auth/oauth2/exchange` | 请求体 `{ "ticket": "..." }`,消费票据(单次,Redis TTL 60 秒),返回与 `POST /api/auth/login` **相同结构**的 `LoginResponse` |

**必须配套的两处放行**(否则端点在进控制器前就挂掉):

| 位置 | 改法 |
|------|------|
| `scm-auth/src/main/resources/application.yaml` | `scm.tenant.excludePaths` 增加 `/api/auth/oauth2/**`(默认值只有 `/actuator/**,/v3/api-docs/**`)。**必需** —— `/authorize` 是浏览器导航、Apple 回调是跨域 POST,两者都带不了 `X-Tenant-Id` |
| `scm-common/web/.../security/config/SecurityConfig.java` | permitAll 列表增加 `/api/auth/oauth2/**`。这是共享模块,改动影响所有服务 —— 但其它服务不存在该路径,无实际影响 |

另需确认:`AuthorizationServerConfig` 的 `@Order(1)` 链用
`securityMatcher(getEndpointsMatcher())` 匹配的是 `/oauth2/authorize`、`/oauth2/token`、
`/userinfo`、`/connect/register`、`/.well-known/**` 等精确路径,不应捕获
`/api/auth/oauth2/**`。实现时需写一条测试锁死这个结论。

#### B3. provider 身份与账号绑定

从 provider profile 提取:

| provider | subject | email | 姓名 | 头像 |
|----------|---------|-------|------|------|
| google | OIDC `sub` | `email`(须校验 `email_verified == true`) | `name` | `picture` |
| apple | `sub` | 首次授权才有 | 首次授权才有 | 无 |

查找顺序:

1. `sys_user_oauth` 命中 `(provider, oauth_openid)` → 取 `user_id` → 加载用户
2. 未命中 → 走绑定策略(见下)→ 成功后 upsert `sys_user_oauth`
   (新增一个 upsert 能力:`SysUserOauthMapper` 现有方法都是查询/更新,没有插入)
3. 已命中时刷新 `oauth_email` / `oauth_nickname` / `oauth_avatar` / `raw_user_info` / `last_login_time`

**绑定策略**已确认(见「已确认决策」第 2 条)。代码按「默认仅绑定,可配置开启自动开通」实现:

- 默认(`auto-provision = false`):按 provider 已验证 email 查 `sys_user.email`;
  查到且未绑定过该 provider → 绑定并登录;查不到 → 拒绝,返回可读错误
  「该邮箱尚未开通账号,请联系管理员」
- **email 命中多个用户时拒绝**(提示联系管理员),不取第一条
- 开启(`auto-provision = true`):查不到 `sys_user` 时新建 —— `username` 取 email `@` 前缀并去重
  (`sys_user.username` 唯一约束,冲突加数字后缀)、`email` = provider email、`real_name` = provider 姓名、
  `avatar` = provider 头像、`password` = BCrypt(随机 UUID)(使密码登录不可能成功)、
  `status = 1`、`account_type = 1`、`user_level = 1`
- **自动开通不能复用 `ISysUserCommandService.addUser`**(见「已探明的现状」第 6 节),
  需要一条自己的命令路径:不要求租户、不要求 operator 权限、不设置
  `forceChangePassword` / `passwordExpireTime`(用户本来就没有密码)
- 授予默认角色码由配置项决定,默认**空**(能登录但看不到任何功能,避免越权)

#### B4. 账号状态与审计(密码路径有的,社交路径也必须有)

密码路径走 `authenticationManager.authenticate()`,会经过 `SecurityUser.isEnabled()`
(`status == 1`)与 `isAccountNonLocked()`(`status != 2`),并写
`auditLogService.recordLogin` / `businessMetrics.recordLogin`。
**直接 `getUserByUsername` → 签发令牌会绕过这些**。社交路径必须显式补齐:

- `status != 1` → 拒绝(账号停用)
- `status == 2` → 拒绝(账号锁定)
- `locked_until` 未到期 → 拒绝
- 写 `auditLogService.recordLogin(userId, username, ipAddress, true, "Social login: google")`
- 写 `businessMetrics.recordLogin(true, deviceId)`

#### B5. 平台令牌签发

复用密码登录同一条路径,保证社交登录用户在网关侧与密码登录用户**完全等价**:

- `userDubboService.getUserByUsername(username)` → `SecurityUser`(含 roles + permissions)
- `jwtUtils.generateAccessToken(userId, username, roles, permissions, deviceId, ipAddress, amr)`
  - `amr = ["google"]` / `["apple"]`
  - `deviceId` 取请求头(与密码登录同一来源)
- `jwtUtils.generateRefreshToken(userId, username, deviceId)`
- 更新 `sys_user.last_login_time` / `last_login_ip`(`UserDubboService.updateLastLogin`)

**关于 TOTP 与 `amr`**:平台已经用 `amr` 表达「这次认证用了哪些方法」
(`StepUpFilter` 据此放行 MFA 步进)。provider 账号本身是强认证,**社交登录不再叠加平台 TOTP**,
`amr` 如实记录 `["google"]` / `["apple"]`。
**副作用**:这类会话无法通过要求 `amr` 含 `mfa` 的步进校验(MFA 步进场景会要求用户改用密码登录)。
该副作用已被接受(见「已确认决策」第 4 条),票据流程保持单阶段,不引入「待验证」中间态。

#### B6. 一次性票据

- Redis key:`auth:oauth2:ticket:{ticket}`,值 = `LoginResponse` 序列化,TTL 60 秒
- 读取即删除(单次消费);重复使用返回失败
- 票据用 `UUIDv7Util.generateString()`(仓库既有工具)生成

#### B7. 前端改动

| 改动 | 说明 |
|------|------|
| `social-auth-buttons.tsx` | 登录时拉 `GET /auth/api/auth/oauth2/providers`,**只为返回列表里的 provider 渲染按钮**。未配置凭据的 provider 按钮不出现 —— 避免点了必然失败的入口。请求失败时静默隐藏(不阻塞登录表单) |
| 点击行为 | 浏览器导航到 `${API_BASE}/auth/api/auth/oauth2/${provider}/authorize?locale={locale}&returnTo={path}`。`locale` 与 `returnTo` 存进 `state`,回调时用 |
| 新路由 `src/app/[locale]/auth/callback/page.tsx` | 读 URL 上的 `ticket` → `POST /auth/api/auth/oauth2/exchange` → 调用共享的 `completeLogin()` → 跳 `returnTo`(默认 dashboard);失败则展示错误并给回登录页的链接 |
| `exchange` 的 401 处理 | **不能用现有 `apiClient`** —— 它的响应拦截器对任何 401 都会尝试刷新 token,失败后 `window.location.href = '/login'`,会把票据错误变成静默跳转且丢掉 locale。需要一个独立 axios 实例(或给 `apiClient` 加 `skipAuthRefresh` 标志),让回调页能显示错误态 |
| `src/proxy.ts` | `publicPaths` 增加 `/auth/callback`,否则未持有 `access_token` cookie 时会被守卫重定向到 `/login`,票据丢失 |
| `src/lib/api/services/auth.ts` | 新增 `oauth2.providers()` 与 `oauth2.exchange(ticket)`;`contracts/auth.ts` 补对应类型 |

**注意**:`apiClient` 的请求拦截器会自动附带 `X-Tenant-Id`(来自 tenant store 的持久化状态,
`.env.local` 里 `NEXT_PUBLIC_DEFAULT_TENANT_ID` 有值)。`exchange` 是认证前的调用,
这个头带不带都无害(租户拦截器未注册),但实现时不要把它当作认证依据。

#### B8. 配置

`scm-auth` 的 `application.yaml`:

```yaml
scm:
  tenant:
    exclude-paths:            # 覆盖默认值,加入社交登录路径
      - /actuator/**
      - /v3/api-docs/**
      - /api/auth/oauth2/**

security:
  oauth2:
    social:
      enabled: ${SOCIAL_AUTH_ENABLED:false}
      auto-provision: ${SOCIAL_AUTH_AUTO_PROVISION:false}
      default-role-codes: []          # 自动开通时授予的角色码,默认空
      ticket-ttl-seconds: 60
      state-ttl-seconds: 300
      frontend-base-url: ${SOCIAL_AUTH_FRONTEND_BASE_URL:http://localhost:3000}
      providers:
        google:
          client-id: ${GOOGLE_CLIENT_ID:}
          client-secret: ${GOOGLE_CLIENT_SECRET:}
          redirect-uri: ${GOOGLE_REDIRECT_URI:http://localhost:8761/auth/api/auth/oauth2/google/callback}
        apple:
          # Services ID,形如 teamid.bundleid
          client-id: ${APPLE_SERVICE_ID:}
          # ES256 签名、有效期 <= 6 个月的 JWT,由外部预先生成后注入
          client-secret: ${APPLE_CLIENT_SECRET:}
          redirect-uri: ${APPLE_REDIRECT_URI:http://localhost:8761/auth/api/auth/oauth2/apple/callback}
```

- `client-id` / `client-secret` / `redirect-uri` 任一为空 → 该 provider 视为未配置,
  不出现在 `/providers` 列表,前端不渲染其按钮
- `frontend-base-url` 是后端把浏览器送回前端所需的新配置项
- Apple 的 client secret 由外部预先用团队私钥生成后以环境变量注入(不在服务内做 ES256 签名,
  避免在仓库里引入私钥处理逻辑)
- `redirect-uri` 必须与 Google / Apple 控制台登记的完全一致,否则 provider 拒绝
- 回调地址部署形态:`https://<网关域名>/auth/api/auth/oauth2/{provider}/callback`
  (网关 `/auth/**` → `StripPrefix=1` → 后端实际路径 `/api/auth/oauth2/{provider}/callback`)

#### B9. 交付物清单(后端)

| 位置 | 内容 |
|------|------|
| `scm-auth/.../config/SocialAuthProperties.java` | 配置绑定 |
| `scm-auth/.../service/SocialAuthService.java` | 授权码交换、profile 拉取、账号绑定、令牌签发、票据生成 |
| `scm-auth/.../controller/SocialAuthController.java` | `/providers`、`/authorize`、`/callback`、`/exchange` 四个端点 |
| `scm-auth/.../service/SocialIdentityProvider.java` + `GoogleIdentityProvider` / `AppleIdentityProvider` | provider 差异(端点、scope、form_post、profile 提取)收敛在接口后 |
| `scm-auth/src/main/resources/application.yaml` | `scm.tenant.exclude-paths` + `security.oauth2.social.*` |
| `scm-common/web/.../security/config/SecurityConfig.java` | permitAll 增加 `/api/auth/oauth2/**`(共享模块) |
| `scm-system/api` | `UserDubboService` 增加社交账号相关方法(按 email 查用户、按 `(provider, openid)` 查绑定、创建用户) |
| `scm-system/service` | `SysUserOauthMapper` 增加 upsert;`SysUserMapper` 增加 `findByEmail`;新增社交账号绑定/自动开通的命令实现 |

**不新建表**:`sys_user_oauth` 已就位。若采用「自动开通」策略,`sys_user` 已有的
`email` / `username` / `password` / `status` 等列足够 —— 但 `tenant_id` 列的问题按
「已确认决策」第 3 条处理(本次不补列,走不含该列的插入路径)。

## 测试策略

### 前端

- Vitest 单测覆盖:Zod schema 校验、`isMfaRequired` 分支判定、票据消费成功/失败/重复消费
- `npm run typecheck`(TS7)/ `npm run lint` / `npm test` / `npm run build` 全绿
- `e2e/login.spec.ts` 同步更新(H1 文案、locale 前缀、`input[name=...]` 断言)

### 后端

- Mockito 单测(仓库默认不启 Spring 上下文,见 `AGENTS.md`):
  - provider profile 提取(Google `email_verified` 为 false 时拒绝)
  - 绑定策略:命中既有绑定 / 按 email 绑定 / email 歧义 / 无匹配拒绝 / 自动开通(含 username 去重)
  - **账号状态**:`status != 1` 拒绝、`status == 2` 拒绝、`locked_until` 未到期拒绝
  - 审计与指标写入被调用
  - 票据:生成、单次消费、过期、重复消费失败
  - `amr` 值正确写入平台令牌
- 一条测试锁死:`/api/auth/oauth2/**` 不被 `AuthorizationServerConfig` 的 `@Order(1)` 链捕获
- 并发首次登录时 `uk_provider_openid` 唯一约束冲突的处理
- 若要端到端验证,需要真实 provider 凭据 —— 见「已确认决策」第 1 条

## 验证命令

```bash
# 前端
cd scm-web
npm run typecheck
npm run lint
npm test
npm run build

# 后端(单模块)
mvn test -pl ../scm-auth -f com.scm.parent/pom.xml
mvn test -pl ../scm-system/service -f com.scm.parent/pom.xml
mvn clean install -DskipTests -f com.scm.parent/pom.xml
```

## 影响面

**改动**:`scm-web` 登录/找回密码页与外壳、新增 `components/auth/*`、删 2 个 antd 组件、
design-tokens/globals/messages/proxy.ts/e2e spec、`scm-auth` 新增社交登录端点与配置、
`scm-common/web` 的 `SecurityConfig` permitAll、`scm-system` 新增社交账号绑定与 Dubbo 方法。

**不动**:`AuthorizationServerConfig`(Spring Authorization Server)、现有 `POST /api/auth/login`、
`JwtAuthenticationFilter`、`StepUpFilter`、网关路由表、`sys_user_oauth` 表结构、其它页面。

## 已知取舍

1. **登录页用 Claude 橙,其余页面保持靛蓝。** 这是用户明确选定的取舍(选项描述里已写明
   「代价是登录页跟其他页面的品牌色不一致」)。全平台品牌色改造不在本次范围。
2. **登录页浅色专用。** 应用有暗色主题(`data-theme="dark"`),但参考图是浅色单风格,
   登录外壳本次只做浅色。暗色下的登录页留作后续。
3. **社交登录不叠加 TOTP**(理由见 B5),用 `amr` 如实记录认证方式。
4. **不使用 scm-auth 现有的 OAuth2 授权服务器。** 它与平台令牌体系不通用,强行复用会让
   「社交登录」变成「换令牌体系」,风险远大于收益。
5. **`/providers` 用 `ApiResponse` 信封**,与 scm-auth 其它端点保持一致(而不是裸数组)。

## 已知遗留(不在本次范围)

1. `TenantInterceptor` 未注册进 MyBatis 插件链 —— 租户隔离第 3 层当前是死代码。
   `AGENTS.md` 的描述与实际不符,建议单独开一条修复。
2. `AuthorizationServerConfig` 的 `RegisteredClientRepository` / `OAuth2AuthorizationService`
   是内存实现,重启即丢;`nearsync-*` 客户端 id 是历史遗留命名。
3. `sys_user_oauth` 的 `access_token` / `refresh_token` / `token_expire_time` 列用于存 provider
   令牌。本设计**不存**(provider 令牌用完即弃,只留 `raw_user_info`),以免长期保存第三方凭证。
4. `uk_provider_openid` 不含 `deleted`,软删除的绑定会占用唯一键 —— 建议改成
   `UNIQUE (provider, oauth_openid) WHERE NOT deleted` 的部分唯一索引。
   本次不处理(见「已知遗留」),解绑后重新绑定该 provider 需人工清理。
5. 仓库 247 个文件存在不可自动恢复的中文乱码(见 `docs/mojibake-report.md`)。

## 已确认决策(2026-10-27 用户确认,按设计方默认值)

以下 4 项在设计评审中确认,实现时按此执行,不再回头讨论。

1. **落地顺序:先 Google,Apple 随后。**
   `SocialIdentityProvider` 接口按多 provider 设计,`GoogleIdentityProvider` 先行落地;
   `AppleIdentityProvider` 作为同一接口的第二个实现随后补。配置里的 `apple` 段保留,
   但在 `service-id` / `client-secret` 配齐之前它视为未配置 —— 不出现在 `/providers` 列表,
   前端不渲染其按钮。
   *前置条件(仍需用户提供,属于部署前置而非实现阻塞):Google 的 client id / client secret
   与控制台登记的回调地址。没有凭据,功能可写完并通过单测,但无法端到端验证。*

2. **账号绑定策略:仅绑定 + `auto-provision` 开关默认关。**
   默认行为:按 provider 已验证 email 查 `sys_user.email`,查到且未绑定过该 provider → 绑定并登录;
   查不到 → 拒绝,提示「该邮箱尚未开通账号,请联系管理员」。
   `auto-provision = true` 时才自动开通新账号(且默认不授予任何角色)。
   **email 歧义规则:`sys_user.email` 没有唯一约束,按 email 匹配命中多个用户时拒绝并提示
   联系管理员 —— 不猜测、不取第一条。**

3. **`sys_user.tenant_id`:本次不动 schema。**
   实体声明了 `tenant_id` 且 `AuditMetaObjectHandler` 会填充它,但建表脚本里没有这一列,
   有租户上下文时 INSERT 会报错。本次**不补列**,社交自动开通走一条显式不含 `tenant_id` 的
   插入路径(绕开自动填充)。补列留给既有的多租户改造
   (未执行的 `scripts/db/migrations/004_transform_permission_system_multi_tenant.sql`)。

4. **社交登录不叠加 TOTP。**
   `amr` 如实记录 `["google"]` / `["apple"]`。已接受的副作用:这类会话无法通过要求 `amr`
   含 `mfa` 的步进校验(MFA 步进场景要求用户改用密码登录)。
   票据流程保持单阶段,不引入「待验证」中间态,回调页不需要验证码表单。
