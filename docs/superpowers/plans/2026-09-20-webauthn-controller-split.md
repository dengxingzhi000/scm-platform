# WebAuthnController Split Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split `scm-auth`'s monolithic `WebAuthnCredentialController` (217 lines, 3 mixed responsibilities) into 3 single-responsibility controllers and add a shared `WebAuthnRequestContext` record to eliminate repeated user/IP/device context extraction.

**Architecture:** Three `@RestController`s grouped by business flow (registration / authentication / credential management), all under the existing `/api/auth/webauthn` URL space. Controllers depend on a thin `WebAuthnRequestContext` Java record built from `SecurityUser` + `HttpServletRequest`. Service layer and DTOs are untouched.

**Tech Stack:** Spring Boot 4.x, Java 21, Lombok, Jakarta validation, existing `WebAuthnConfig` (`@ConfigurationProperties(prefix = "webauthn.rp")`).

**Spec:** `docs/superpowers/specs/2026-09-20-webauthn-controller-split-design.md`

---

## File Structure

| Status | File | Responsibility |
|--------|------|----------------|
| Create | `scm-auth/src/main/java/com/scmcloud/auth/webauthn/WebAuthnRequestContext.java` | Immutable record holding userId/username/deviceId/ipAddress/userAgent + static factory |
| Create | `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnRegistrationController.java` | `POST /register/challenge`, `POST /register/verify` |
| Create | `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnAuthenticationController.java` | `POST /authenticate/challenge`, `POST /authenticate/verify` |
| Modify | `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnCredentialController.java` | Keep only `/credentials/*` CRUD; remove registration + authentication endpoints |

**Untouched (deliberately):** `IWebauthnCredentialService`, `WebauthnCredentialServiceImpl`, all DTOs, `WebAuthnConfig`, `WebAuthnValidator`, `GlobalExceptionHandler`.

**Constraints:**
- URL paths must remain identical to current behavior
- Service method signatures unchanged
- No new tests (per spec non-goals; existing `WebAuthnValidatorTest` must continue to pass)

---

## Task 1: Create WebAuthnRequestContext record

**Files:**
- Create: `scm-auth/src/main/java/com/scmcloud/auth/webauthn/WebAuthnRequestContext.java`

- [ ] **Step 1: Create the record file**

Write `scm-auth/src/main/java/com/scmcloud/auth/webauthn/WebAuthnRequestContext.java` with the following content:

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
 * Encapsulates the userId / username / deviceId / ipAddress / userAgent tuple that every WebAuthn
 * endpoint needs, eliminating the repeated extraction boilerplate that previously lived in each
 * controller method.
 */
public record WebAuthnRequestContext(
        UUID userId,
        String username,
        String deviceId,
        String ipAddress,
        String userAgent
) {

    /**
     * Build a context from the authenticated user and HTTP request.
     *
     * @param user    current authenticated principal (never null)
     * @param request current HTTP request (never null)
     * @param utils   request utilities used to resolve the device id
     * @return immutable request context
     */
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

- [ ] **Step 2: Compile to verify**

Run (from repo root):

```bash
mvn clean compile -pl scm-auth/service -am -f com.scm.parent/pom.xml -DskipTests
```

Expected: `BUILD SUCCESS`. The new record file is in the source tree, the rest of `scm-auth` still compiles unchanged.

- [ ] **Step 3: Commit**

```bash
git add scm-auth/src/main/java/com/scmcloud/auth/webauthn/WebAuthnRequestContext.java
git commit -m "feat(auth): add WebAuthnRequestContext record"
```

---

## Task 2: Extract registration endpoints to WebAuthnRegistrationController

**Files:**
- Create: `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnRegistrationController.java`
- Modify: `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnCredentialController.java` (remove `generateRegistrationChallenge`, `registerCredential`, drop `WebAuthnConfig` and `HttpServletRequestUtils` dependencies, drop `DEFAULT_RP_ID` constant)

- [ ] **Step 1: Create WebAuthnRegistrationController**

Write `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnRegistrationController.java`:

```java
package com.scmcloud.auth.controller;

import com.scmcloud.auth.domain.dto.WebauthnCredentialDTO;
import com.scmcloud.auth.domain.dto.WebauthnRegistrationRequest;
import com.scmcloud.auth.service.IWebauthnCredentialService;
import com.scmcloud.auth.webauthn.WebAuthnConfig;
import com.scmcloud.auth.webauthn.WebAuthnRequestContext;
import com.scmcloud.common.dto.auth.WebAuthnRegisterChallengeResponse;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.security.util.HttpServletRequestUtils;
import com.scmcloud.common.web.domain.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebAuthn credential registration flow.
 * <p>
 * Hosts the two endpoints that together register a new passkey for an authenticated user:
 * challenge generation and credential verification.
 *
 * @author system
 * @since 2026-09-20
 */
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
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest request) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, request, httpServletRequestUtils);
        log.info("User {} requesting registration challenge from {}", ctx.userId(), ctx.ipAddress());

        WebAuthnRegisterChallengeResponse challenge = credentialService.generateRegistrationChallenge(
                ctx.userId(), ctx.username(), ctx.deviceId(), webAuthnConfig.getId());

        return ApiResponse.success(challenge);
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<WebauthnCredentialDTO> registerCredential(
            @Valid @RequestBody WebauthnRegistrationRequest request,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest httpRequest) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, httpRequest, httpServletRequestUtils);
        log.info("User {} registering new WebAuthn credential from {}", ctx.userId(), ctx.ipAddress());

        WebauthnCredentialDTO credential = credentialService.registerCredential(ctx.userId(), request);

        return ApiResponse.success(credential);
    }
}
```

- [ ] **Step 2: Remove the registration methods from WebAuthnCredentialController**

Open `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnCredentialController.java` and apply the following edits:

1. Update the class-level `@RequestMapping("/api/auth/webauthn")` to `@RequestMapping("/api/auth/webauthn/credentials")`.

2. Delete the `DEFAULT_RP_ID` constant block (lines 44–47 in current file):

```java
    /**
     * 默认 RP ID,生产环境应通过配置文件设置
     */
    private static final String DEFAULT_RP_ID = "localhost";
```

3. Delete the imports that are no longer used by the trimmed-down class (keep the rest):

```java
import com.scmcloud.auth.domain.dto.WebauthnAuthenticationRequest;
import com.scmcloud.auth.domain.dto.WebauthnRegistrationRequest;
import com.scmcloud.auth.webauthn.WebAuthnConfig;
import com.scmcloud.common.dto.auth.WebAuthnChallengeResponse;
import com.scmcloud.common.dto.auth.WebAuthnRegisterChallengeResponse;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.security.util.HttpServletRequestUtils;
import com.scmcloud.common.security.util.IpUtils;
import com.scmcloud.common.dto.auth.TokenUpgradeResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.AuthenticationException;
import java.util.UUID;
```

Note: `UUID` is still needed because each CRUD handler resolves `userId` via `user.getUserId()`. Keep `import java.util.UUID;`.

4. Remove the fields that are no longer needed. The trimmed class should keep only:

```java
    private final IWebauthnCredentialService credentialService;
```

Drop `HttpServletRequestUtils httpServletRequestUtils` and `WebAuthnConfig webAuthnConfig` (both no longer used after removing the auth/register endpoints).

5. Delete the four methods that moved to the new controllers:
   - `generateRegistrationChallenge`
   - `registerCredential`
   - `generateAuthenticationChallenge`
   - `authenticateAndUpgradeToken`

After all edits, the file should match the design spec section 6 (the `WebAuthnCredentialController` example). The five remaining methods are `listCredentials`, `updateDeviceName`, `deleteCredential`, `deactivateCredential`, `checkCredentialHealth`.

- [ ] **Step 3: Compile to verify**

```bash
mvn clean compile -pl scm-auth/service -am -f com.scm.parent/pom.xml -DskipTests
```

Expected: `BUILD SUCCESS`. Both the new `WebAuthnRegistrationController` and the trimmed `WebAuthnCredentialController` compile, and there is no path collision on `/api/auth/webauthn/register/*`.

- [ ] **Step 4: Commit**

```bash
git add scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnRegistrationController.java
git add scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnCredentialController.java
git commit -m "refactor(auth): extract WebAuthn registration endpoints to dedicated controller"
```

---

## Task 3: Extract authentication endpoints to WebAuthnAuthenticationController

**Files:**
- Create: `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnAuthenticationController.java`
- Modify: `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnCredentialController.java` — `authenticateAndUpgradeToken` was already removed in Task 2 step 2.5, so this task is *creation only* if Task 2 was done correctly. Re-open the file to confirm.

- [ ] **Step 1: Verify the old controller no longer defines the authentication endpoints**

Run:

```bash
rg -n "authenticateAndUpgradeToken|generateAuthenticationChallenge" scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnCredentialController.java
```

Expected: no matches. If matches exist, remove the methods and the imports they need (`WebauthnAuthenticationRequest`, `TokenUpgradeResponse`, `BusinessException`, `AuthenticationException`, `IpUtils`, `HttpServletRequest`, `HttpServletRequestUtils`) before continuing.

- [ ] **Step 2: Create WebAuthnAuthenticationController**

Write `scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnAuthenticationController.java`:

```java
package com.scmcloud.auth.controller;

import com.scmcloud.auth.domain.dto.WebauthnAuthenticationRequest;
import com.scmcloud.auth.service.IWebauthnCredentialService;
import com.scmcloud.auth.webauthn.WebAuthnConfig;
import com.scmcloud.auth.webauthn.WebAuthnRequestContext;
import com.scmcloud.common.dto.auth.TokenUpgradeResponse;
import com.scmcloud.common.dto.auth.WebAuthnChallengeResponse;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.security.util.HttpServletRequestUtils;
import com.scmcloud.common.web.domain.SecurityUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * WebAuthn assertion (authentication) flow.
 * <p>
 * Hosts the two endpoints that together authenticate an already-registered passkey and upgrade
 * the user's session token. The {@code /verify} handler records both successful and failed
 * authentication attempts via {@link IWebauthnCredentialService#logAuthenticationAttempt}.
 *
 * @author system
 * @since 2026-09-20
 */
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
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest request) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, request, httpServletRequestUtils);
        log.info("User {} requesting authentication challenge from {}", ctx.userId(), ctx.ipAddress());

        WebAuthnChallengeResponse challenge = credentialService.generateAuthenticationChallenge(
                ctx.userId(), ctx.username(), ctx.deviceId(), webAuthnConfig.getId());

        return ApiResponse.success(challenge);
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<TokenUpgradeResponse> authenticateAndUpgradeToken(
            @Valid @RequestBody WebauthnAuthenticationRequest request,
            @AuthenticationPrincipal SecurityUser user,
            HttpServletRequest httpRequest) {

        WebAuthnRequestContext ctx = WebAuthnRequestContext.of(user, httpRequest, httpServletRequestUtils);
        log.info("User {} authenticating with WebAuthn from {}", ctx.userId(), ctx.ipAddress());

        try {
            TokenUpgradeResponse response = credentialService.authenticateAndUpgradeToken(
                    ctx.userId(), ctx.username(), request, ctx.deviceId(), ctx.ipAddress());

            // 记录成功的认证尝试
            credentialService.logAuthenticationAttempt(
                    ctx.userId(), request.getCredentialId(), true, ctx.ipAddress(), ctx.userAgent());

            return ApiResponse.success(response);

        } catch (AuthenticationException | BusinessException e) {
            // 记录失败的认证尝试
            credentialService.logAuthenticationAttempt(
                    ctx.userId(), request.getCredentialId(), false, ctx.ipAddress(), ctx.userAgent());
            throw e;
        }
    }
}
```

- [ ] **Step 3: Compile to verify**

```bash
mvn clean compile -pl scm-auth/service -am -f com.scm.parent/pom.xml -DskipTests
```

Expected: `BUILD SUCCESS`. There must be no path collision on `/api/auth/webauthn/authenticate/*`.

- [ ] **Step 4: Commit**

```bash
git add scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnAuthenticationController.java
git commit -m "refactor(auth): extract WebAuthn authentication endpoints to dedicated controller"
```

---

## Task 4: Final verification

**Files:** none modified

- [ ] **Step 1: Build the full scm-auth module with tests**

Run:

```bash
mvn clean package -pl scm-auth/service -am -f com.scm.parent/pom.xml
```

Expected: `BUILD SUCCESS`. The only existing test, `WebAuthnValidatorTest`, must still pass — confirm by inspecting the surefire output for `Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`.

- [ ] **Step 2: Sanity check the URL space**

Confirm there are exactly 9 endpoint mappings across the three controllers, all under `/api/auth/webauthn`:

```bash
rg -n "@(GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping)" scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthn*Controller.java
```

Expected count:
- `WebAuthnRegistrationController` — 2 mappings (`/register/challenge`, `/register/verify`)
- `WebAuthnAuthenticationController` — 2 mappings (`/authenticate/challenge`, `/authenticate/verify`)
- `WebAuthnCredentialController` — 1 class-level `@RequestMapping("/api/auth/webauthn/credentials")` plus 5 method-level mappings (list, name, deactivate, delete, health)

- [ ] **Step 3: Confirm no leftover references to removed code**

```bash
rg -n "DEFAULT_RP_ID" scm-auth/src/main/java
```

Expected: no matches.

```bash
rg -n "generateRegistrationChallenge|registerCredential|generateAuthenticationChallenge|authenticateAndUpgradeToken" scm-auth/src/main/java/com/scmcloud/auth/controller/WebAuthnCredentialController.java
```

Expected: no matches. All four methods must live in their respective new controllers.

- [ ] **Step 4: Final commit if any leftover edits**

If steps 1–3 surfaced any small touch-ups (e.g., stray import, formatting), commit them:

```bash
git add -A scm-auth/src/main/java/com/scmcloud/auth
git commit -m "refactor(auth): final cleanup of WebAuthn controller split"
```

Skip this step if there were no edits.

---

## Self-Review Notes (for the planner, not the executor)

- **Spec coverage:** Task 1 → §3 record. Task 2 → §4 registration controller. Task 3 → §5 authentication controller. Task 4 → §6 trimmed CRUD controller + §10 verification.
- **Type/name consistency:** `WebAuthnRequestContext.of(user, request, httpServletRequestUtils)` is the same factory signature used in all three tasks; `WebAuthnConfig.getId()` replaces `DEFAULT_RP_ID` consistently; method names on the service (`generateRegistrationChallenge`, `registerCredential`, `generateAuthenticationChallenge`, `authenticateAndUpgradeToken`, `listActiveCredentials`, `updateDeviceName`, `deleteCredential`, `deactivateCredential`, `checkCredentialHealth`, `logAuthenticationAttempt`) match `IWebauthnCredentialService` exactly.
- **No placeholders:** every step either shows complete code or names an exact command with expected output.
- **Sequencing rationale:** Task 2 step 2 removes ALL four methods that move out (registration + authentication), so Task 3 only needs to create the authentication controller. Task 1's `WebAuthnRequestContext` is a leaf with no dependencies on the controllers, so it can ship first.
