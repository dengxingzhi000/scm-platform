# WebAuthnCredentialController 职责拆分设计

**日期**: 2026-09-20
**作者**: opencode (brainstorming session)
**范围**: `scm-auth` 模块的 `WebAuthnCredentialController` + 配套上下文助手
**状态**: 设计草案，等待用户审阅

## 背景

`scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnCredentialController.java`（217 行，8 个端点）承担了三类相互独立的职责：

1. **WebAuthn 注册流程**（2 端点）：`/register/challenge`、`/register/verify`
2. **WebAuthn 认证流程**（2 端点）：`/authenticate/challenge`、`/authenticate/verify`
3. **凭证 CRUD 管理**（4 端点）：`/credentials` 下所有 list/update/delete/deactivate/health

此外还存在以下跨切面问题：

- **重复提取**：每个端点都重复 `UUID userId = user.getUserId(); String ipAddress = IpUtils.getClientIp(request); String deviceId = httpServletRequestUtils.getDeviceId(request);`（4~5 处重复）
- **重复硬编码**：`DEFAULT_RP_ID = "localhost"` 常量在 controller 中硬编码，但 `WebAuthnConfig`（`@ConfigurationProperties(prefix = "webauthn.rp")`）已配置同名字段
- **可读性**：单一类承担注册 + 认证 + 管理三类语义，新增端点时难以定位归属

## 目标

按业务流拆分 Controller，单一职责；抽取公共上下文助手消除重复；移除硬编码常量。

| 目标 | 措施 |
|------|------|
| 职责单一 | 拆分为 3 个 Controller，分别对应注册 / 认证 / 凭证管理 |
| 消除重复 | 引入 `WebAuthnRequestContext` record + 静态工厂方法，封装上下文提取 |
| 配置外移 | 注入 `WebAuthnConfig.getId()` 替代 `DEFAULT_RP_ID` 硬编码常量 |

## 非目标（Out of Scope）

- **不修改** `IWebauthnCredentialService` 接口及 `WebauthnCredentialServiceImpl` 实现。理由：服务签名使用原语参数（UUID, String, ...）合理，传入 record 反而污染服务层；调用方仅 controller，影响面可控
- **不引入** 自定义 `HandlerMethodArgumentResolver`（如 `@CurrentUser`、`@ClientIp`、`@DeviceId`）。理由：scm-auth 模块当前零使用此类基础设施，`SysAuthController` 等其他 controller 仍使用 `@AuthenticationPrincipal SecurityUser user` + 工具方法；引入新模式会与现有 controller 不一致
- **不引入** `ApplicationEventPublisher` / `@EventListener` 鉴权日志事件机制。理由：当前 `credentialService.logAuthenticationAttempt(...)` 已是干净的服务调用，事件机制在 scm-auth 模块零使用，过度设计
- **不新增** controller 单测。理由：当前 scm-auth 模块无 controller 测试（仅 `WebAuthnValidatorTest`），拆分本质是结构调整不引入新业务逻辑；`AGENTS.md` 注明 `jacoco.skip=true` 默认不强制覆盖率
- **不修改** Service 层、DTO 定义、`WebAuthnValidator`、`WebAuthnConfig` 字段、`GlobalExceptionHandler`
- **不修改** 前端 `scm-web`（URL 路径保持不变）

## 设计

### 1. 文件结构

```
scm-auth/src/main/java/com/scmcloud/auth/
├── controller/
│   ├── WebAuthnRegistrationController.java      (NEW, ~70 行)
│   ├── WebAuthnAuthenticationController.java    (NEW, ~75 行)
│   └── WebAuthnCredentialController.java        (MODIFY, ~110 行 — 仅保留凭证 CRUD)
└── webauthn/
    ├── WebAuthnConfig.java                      (existing, 不动)
    ├── WebAuthnValidator.java                   (existing, 不动)
    └── WebAuthnRequestContext.java              (NEW, ~40 行 — Java record + 静态工厂)
```

### 2. URL 映射（路由契约完全保持）

| Controller | HTTP | 路径 |
|---|---|---|
| `WebAuthnRegistrationController` | `POST` | `/api/auth/webauthn/register/challenge` |
| `WebAuthnRegistrationController` | `POST` | `/api/auth/webauthn/register/verify` |
| `WebAuthnAuthenticationController` | `POST` | `/api/auth/webauthn/authenticate/challenge` |
| `WebAuthnAuthenticationController` | `POST` | `/api/auth/webauthn/authenticate/verify` |
| `WebAuthnCredentialController` | `GET` | `/api/auth/webauthn/credentials` |
| `WebAuthnCredentialController` | `PUT` | `/api/auth/webauthn/credentials/{id}/name` |
| `WebAuthnCredentialController` | `PUT` | `/api/auth/webauthn/credentials/{id}/deactivate` |
| `WebAuthnCredentialController` | `DELETE` | `/api/auth/webauthn/credentials/{id}` |
| `WebAuthnCredentialController` | `GET` | `/api/auth/webauthn/credentials/health` |

注：原 `WebAuthnCredentialController` 类上的 `@RequestMapping("/api/auth/webauthn")` 将拆为 3 个类上的子路径。`/credentials` 子树集中到一个类，所以 `WebAuthnCredentialController` 改名意义不大，保留原名即可。

### 3. WebAuthnRequestContext（NEW）

```java
package com.scmcloud.auth.webauthn;

import com.scmcloud.common.security.util.HttpServletRequestUtils;
import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.web.domain.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/**
 * Immutable per-request WebAuthn context extracted from {@link SecurityUser} and {@link HttpServletRequest}.
 * <p>
 * Eliminates repeated userId/ipAddress/deviceId/userAgent extraction across WebAuthn controllers.
 */
public record WebAuthnRequestContext(
        UUID userId,
        String username,
        String deviceId,
        String ipAddress,
        String userAgent
) {
    public static WebAuthnRequestContext of(
            SecurityUser user, HttpServletRequest request, HttpServletRequestUtils utils) {
        return new WebAuthnRequestContext(
                user.getUserId(),
                user.getUsername(),
                utils.getDeviceId(request),
                IpUtils.getClientIp(request),
                request.getHeader("User-Agent")
        );
    }
}
```

**职责**：消除 8 处重复的上下文提取。明确一个工厂方法 `of(...)` 接收显式参数（编译期安全，避免反射取 bean）。

### 4. WebAuthnRegistrationController（NEW）

```java
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth/webauthn/register")
@RequiredArgsConstructor
public class WebAuthnRegistrationController {
    private final IWebauthnCredentialService credentialService;
    private final WebAuthnConfig webAuthnConfig;
    private final HttpServletRequestUtils httpServletRequestUtils;

    @PostMapping("/challenge")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebAuthnRegisterChallengeResponse> generateRegistrationChallenge(
            @AuthenticationPrincipal SecurityUser user, HttpServletRequest request) {
        var ctx = WebAuthnRequestContext.of(user, request, httpServletRequestUtils);
        log.info("User {} requesting registration challenge from {}", ctx.userId(), ctx.ipAddress());
        return ApiResponse.success(credentialService.generateRegistrationChallenge(
                ctx.userId(), ctx.username(), ctx.deviceId(), webAuthnConfig.getId()));
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebauthnCredentialDTO> registerCredential(
            @Valid @RequestBody WebauthnRegistrationRequest request,
            @AuthenticationPrincipal SecurityUser user, HttpServletRequest httpRequest) {
        var ctx = WebAuthnRequestContext.of(user, httpRequest, httpServletRequestUtils);
        log.info("User {} registering new WebAuthn credential from {}", ctx.userId(), ctx.ipAddress());
        return ApiResponse.success(credentialService.registerCredential(ctx.userId(), request));
    }
}
```

### 5. WebAuthnAuthenticationController（NEW）

```java
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth/webauthn/authenticate")
@RequiredArgsConstructor
public class WebAuthnAuthenticationController {
    private final IWebauthnCredentialService credentialService;
    private final WebAuthnConfig webAuthnConfig;
    private final HttpServletRequestUtils httpServletRequestUtils;

    @PostMapping("/challenge")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebAuthnChallengeResponse> generateAuthenticationChallenge(
            @AuthenticationPrincipal SecurityUser user, HttpServletRequest request) {
        var ctx = WebAuthnRequestContext.of(user, request, httpServletRequestUtils);
        log.info("User {} requesting authentication challenge from {}", ctx.userId(), ctx.ipAddress());
        return ApiResponse.success(credentialService.generateAuthenticationChallenge(
                ctx.userId(), ctx.username(), ctx.deviceId(), webAuthnConfig.getId()));
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TokenUpgradeResponse> authenticateAndUpgradeToken(
            @Valid @RequestBody WebauthnAuthenticationRequest request,
            @AuthenticationPrincipal SecurityUser user, HttpServletRequest httpRequest) {
        var ctx = WebAuthnRequestContext.of(user, httpRequest, httpServletRequestUtils);
        log.info("User {} authenticating with WebAuthn from {}", ctx.userId(), ctx.ipAddress());

        try {
            TokenUpgradeResponse response = credentialService.authenticateAndUpgradeToken(
                    ctx.userId(), ctx.username(), request, ctx.deviceId(), ctx.ipAddress());
            credentialService.logAuthenticationAttempt(
                    ctx.userId(), request.getCredentialId(), true, ctx.ipAddress(), ctx.userAgent());
            return ApiResponse.success(response);
        } catch (AuthenticationException | BusinessException e) {
            credentialService.logAuthenticationAttempt(
                    ctx.userId(), request.getCredentialId(), false, ctx.ipAddress(), ctx.userAgent());
            throw e;
        }
    }
}
```

### 6. WebAuthnCredentialController（MODIFY — 仅保留凭证 CRUD）

```java
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth/webauthn/credentials")
@RequiredArgsConstructor
public class WebAuthnCredentialController {
    private final IWebauthnCredentialService credentialService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WebauthnCredentialDTO>> listCredentials(
            @AuthenticationPrincipal SecurityUser user) {
        UUID userId = user.getUserId();
        log.debug("User {} listing credentials", userId);
        return ApiResponse.success(credentialService.listActiveCredentials(userId));
    }

    @PutMapping("/{credentialId}/name")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebauthnCredentialDTO> updateDeviceName(
            @PathVariable String credentialId,
            @NotBlank(message = "Device name cannot be empty") @RequestParam String deviceName,
            @AuthenticationPrincipal SecurityUser user) {
        UUID userId = user.getUserId();
        log.info("User {} updating device name for credential {}", userId, credentialId);
        return ApiResponse.success(credentialService.updateDeviceName(userId, credentialId, deviceName));
    }

    @DeleteMapping("/{credentialId}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deleteCredential(
            @PathVariable String credentialId, @AuthenticationPrincipal SecurityUser user) {
        UUID userId = user.getUserId();
        log.info("User {} deleting credential {}", userId, credentialId);
        credentialService.deleteCredential(userId, credentialId);
        return ApiResponse.success();
    }

    @PutMapping("/{credentialId}/deactivate")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> deactivateCredential(
            @PathVariable String credentialId, @AuthenticationPrincipal SecurityUser user) {
        UUID userId = user.getUserId();
        log.info("User {} deactivating credential {}", userId, credentialId);
        credentialService.deactivateCredential(userId, credentialId);
        return ApiResponse.success();
    }

    @GetMapping("/health")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<WebauthnCredentialDTO>> checkCredentialHealth(
            @AuthenticationPrincipal SecurityUser user) {
        UUID userId = user.getUserId();
        log.debug("User {} checking credential health", userId);
        return ApiResponse.success(credentialService.checkCredentialHealth(userId));
    }
}
```

注：此 controller 不再依赖 `HttpServletRequestUtils` 和 `WebAuthnConfig`（CRUD 类操作不需要 IP/deviceId/rpId）。

### 7. 数据流

**注册流程**：
```
Client → WebAuthnRegistrationController
  ↓ WebAuthnRequestContext.of(user, request, utils)
  ↓ credentialService.generateRegistrationChallenge / registerCredential (签名不变)
  ↓ Redis (challenge 临时存储) + DB (webauthn_credential 写入)
```

**认证流程**：
```
Client → WebAuthnAuthenticationController
  ↓ WebAuthnRequestContext.of(...)
  ↓ credentialService.authenticateAndUpgradeToken
  ↓ try 成功 → credentialService.logAuthenticationAttempt(success=true)
  ↓ catch AuthenticationException | BusinessException → logAuthenticationAttempt(success=false); rethrow
  ↓ Redis (challenge 校验) + DB (webauthn_credential 读 + 更新) + audit 表
```

**凭证管理**：
```
Client → WebAuthnCredentialController
  ↓ credentialService.{listActiveCredentials, updateDeviceName, ...} (签名不变)
  ↓ DB (webauthn_credential CRUD + 健康检查)
```

### 8. 错误处理（沿用现有行为，不引入新机制）

| 异常类型 | 处理方 |
|---|---|
| `BusinessException` | 全局 `@RestControllerAdvice`（scm-common 通用机制） |
| `AuthenticationException` | 同上 |
| `MethodArgumentNotValidException` | Spring `@Valid` 默认机制 |
| `ConstraintViolationException` | `@Validated` + 约束注解，全局异常处理 |

`logAuthenticationAttempt` 的 try/catch 结构保持原样，**不引入事件机制**。

### 9. 配置注入

`WebAuthnConfig` 已以 `@Configuration + @ConfigurationProperties(prefix = "webauthn.rp")` 形式存在（双重注解），Spring 自动扫描为 Bean。两个新 controller 通过 `@RequiredArgsConstructor` 直接注入即可，无需新增配置类。

`WebAuthnConfig.id` 默认值 `"localhost"`（`scm-auth/.../webauthn/WebAuthnConfig.java:21`）将作为 RP ID 的默认来源，覆盖原 controller 中的 `DEFAULT_RP_ID` 常量。

### 10. 测试与验证

- **构建验证**：`mvn clean package -pl scm-auth/service -am -f com.scm.parent/pom.xml` 必须编译通过
- **现有测试**：`mvn test -pl scm-auth/service -f com.scm.parent/pom.xml` 必须保留 `WebAuthnValidatorTest` 通过
- **不新增** controller 单测（理由见"非目标"）
- **手动冒烟**（用户本地执行，可选）：启动 auth 服务后用 curl 调用 9 个端点（拆分后数量不变），确认 200/401/403/400 行为与拆分前一致

## 风险与回滚

- **风险等级**：低。本变更仅涉及 4 个文件（2 新建 + 1 修改 + 1 新建 record），无数据库 schema 变更，无接口签名变更（URL 路径保持不变），Service 层零修改。
- **回滚方案**：`git revert` 即可，无数据迁移成本。

## 实施顺序

1. 新建 `WebAuthnRequestContext.java`
2. 新建 `WebAuthnRegistrationController.java`（含 2 端点）
3. 新建 `WebAuthnAuthenticationController.java`（含 2 端点）
4. 修改 `WebAuthnCredentialController.java`（删除注册/认证相关端点，改为 `/credentials` 子路径）
5. 编译并运行现有测试
