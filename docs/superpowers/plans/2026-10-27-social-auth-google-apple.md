# Google/Apple 社交登录接入 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 scm-auth 实现「provider 授权码 → 本地账号绑定 → 平台 JWT → 一次性票据」的服务端流程,并在前端登录卡片上提供「使用 Google 继续 / 使用 Apple 继续」按钮与回调页,让社交登录产出的会话与密码登录完全等价。

**Architecture:** 后端持有 client secret,浏览器只做一次导航到 provider 授权页;provider 回跳到后端回调端点后,后端换 token、拉 profile、绑定或查找本地账号、签发**平台 JWT**(与密码登录同一个 `JwtUtils` 路径),再把结果放进一个 60 秒有效的一次性票据,重定向回前端回调页;前端用票据换回与 `POST /api/auth/login` **结构完全相同**的 `LoginResponse`,复用共享的 `useCompleteLogin()` 建立会话 —— 令牌永不进 URL。数据复用既有的 `sys_user_oauth` 表与 `SysUserOauth` 实体,不新建表。

**Tech Stack:** Spring Boot 4.1 / Spring Security 7 / Spring Authorization Server 不复用 / `RestClient` 调 provider / Redis(票据 + state)/ Dubbo(`UserDubboService`)/ JUnit 5 + Mockito / MyBatis-Plus / Next.js 16.4 / React 19 / TanStack Query v5 / axios / Vitest

**Spec:** `docs/superpowers/specs/2026-10-27-login-page-claude-redesign-social-auth-design.md`(B 部分:B1–B9 + 「已确认决策」)

**前置计划:** `docs/superpowers/plans/2026-10-27-login-page-claude-redesign.md` —— 登录卡片结构由它产出,本计划的 Task 6 往里加按钮。

## Global Constraints

- **社交登录必须以平台 JWT 收尾**。`JwtAuthenticationFilter` 只认 `JwtUtils` 签发的 JWT;provider 令牌与 OAuth2 授权服务器令牌都不能直接用。
- **client secret 不能进浏览器**。授权码交换、profile 拉取全部在后端完成。
- **`amr` 必须如实记录**:Google → `["google"]`,Apple → `["apple"]`。**不得**写成 `["pwd"]` 或伪造 `["pwd","mfa"]`。
- **不叠加 TOTP**。社交登录不要求平台验证码(已确认决策 4)。
- **复用 `sys_user_oauth`,不新建表**。身份主键是 `(provider, oauth_openid)`,不是 email。
- **默认仅绑定**:`auto-provision` 默认 `false`;`sys_user.email` 无唯一约束,**命中多个用户时拒绝**,不取第一条。
- **本次不动 schema**:`sys_user` 实体声明了 `tenant_id` 但建表脚本里没这列;自动开通走**显式不含 `tenant_id`** 的插入路径。
- **`/api/auth/oauth2/**` 必须同时**加进 `scm-auth` 的 `scm.tenant.exclude-paths`(否则缺 `X-Tenant-Id` 的浏览器导航与 Apple form_post 回调会被 `TenantFilter` 拒绝)**和 `SecurityConfig` 的 permitAll 列表(否则 401)**。
- **不得改动** `AuthorizationServerConfig`(Spring Authorization Server)、`POST /api/auth/login`、`JwtAuthenticationFilter`、`StepUpFilter`、网关路由表。
- **不得新增 npm 依赖**;`RestClient` 用 Spring 自带的,测试用 `MockRestServiceServer`。
- 提交粒度 = 一个 Task 一次提交,提交信息用中文 + 约定式前缀。

## Review Focus

以下 5 类失败模式在规格里被提及,但没有哪个 Task 的测试真正覆盖。实现时,每一条都要在**拥有该代码的 Task** 里补上对应测试。

1. **浏览器导航与 Apple 的 form_post 回调不带 `X-Tenant-Id`** —— `TenantFilter` 默认 `required=true`,缺头直接抛异常;`/authorize` 是 `window.location.href` 导航(axios 拦截器不参与),Apple 回调是跨域 POST。期望:这两个端点在没有租户头时也能正常走通。
2. **停用或锁定账号通过社交登录登入** —— 密码路径靠 `SecurityUser.isEnabled()` / `isAccountNonLocked()` 拦截,社交路径若直接 `getUserByUsername` 签发令牌就会绕过。期望:`status != 1` 或 `status == 2` 或 `locked_until` 未到期时拒绝登录。
3. **`sys_user.email` 命中多个用户** —— 该列没有唯一约束。期望:命中多条时拒绝登录并提示联系管理员,**不能**随便绑定其中一个(否则等于把账号给了别人)。
4. **票据被重复消费** —— 票据是登录凭证。期望:同一票据第二次兑换失败;过期(超过 TTL)也失败。
5. **provider 未配置凭据时按钮仍渲染** —— 用户点进去必然失败。期望:`/providers` 只返回配置完整的 provider,前端只渲染这些按钮;provider 列表请求失败时登录表单照常可用(社交按钮静默隐藏)。

---

### Task 1: 配置绑定与两处放行

**Files:**
- Create: `scm-auth/src/main/java/com/scmcloud/auth/config/SocialAuthProperties.java`
- Create: `scm-auth/src/test/java/com/scmcloud/auth/config/SocialAuthPropertiesTest.java`
- Modify: `scm-auth/src/main/resources/application.yaml`
- Modify: `scm-common/web/src/main/java/com/scmcloud/common/security/config/SecurityConfig.java`
- Modify: `scm-common/web/src/test/java/com/scmcloud/common/security/config/SecurityConfigPublicPathsTest.java`(新建)

**Interfaces:**
- Consumes: 无
- Produces:
  - `SocialAuthProperties` —— `@ConfigurationProperties("security.oauth2.social")`,含 `enabled`、`autoProvision`、`defaultRoleCodes`、`ticketTtlSeconds`、`stateTtlSeconds`、`frontendBaseUrl`、`providers: Map<String, ProviderProperties>`
  - `SocialAuthProperties.ProviderProperties` —— `clientId`、`clientSecret`、`redirectUri`
  - `SecurityConfig.PUBLIC_PATHS` —— `public static final List<String>`,permitAll 使用的路径清单

- [ ] **Step 1: 写失败的测试**

创建 `scm-common/web/src/test/java/com/scmcloud/common/security/config/SecurityConfigPublicPathsTest.java`:

```java
package com.scmcloud.common.security.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigPublicPathsTest {

    @Test
    void socialAuthEndpointsArePublic() {
        assertTrue(
                SecurityConfig.PUBLIC_PATHS.contains("/api/auth/oauth2/**"),
                "the social login endpoints must be reachable without a bearer token"
        );
    }

    @Test
    void existingPublicPathsAreKept() {
        assertTrue(SecurityConfig.PUBLIC_PATHS.contains("/api/auth/login"));
        assertTrue(SecurityConfig.PUBLIC_PATHS.contains("/api/auth/refresh"));
        assertTrue(SecurityConfig.PUBLIC_PATHS.contains("/api/public/**"));
    }
}
```

创建 `scm-auth/src/test/java/com/scmcloud/auth/config/SocialAuthPropertiesTest.java`:

```java
package com.scmcloud.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocialAuthPropertiesTest {

    private static SocialAuthProperties bind(MockEnvironment env) {
        return new Binder(ConfigurationPropertySources.get(env))
                .bind("security.oauth2.social", Bindable.of(SocialAuthProperties.class))
                .orElseGet(() -> new SocialAuthProperties(
                        false, false, java.util.List.of(), 60, 300,
                        "http://localhost:3000", Map.of()));
    }

    @Test
    void bindsTheSocialAuthBlock() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("security.oauth2.social.enabled", "true")
                .withProperty("security.oauth2.social.auto-provision", "false")
                .withProperty("security.oauth2.social.ticket-ttl-seconds", "60")
                .withProperty("security.oauth2.social.state-ttl-seconds", "300")
                .withProperty("security.oauth2.social.frontend-base-url", "http://localhost:3000")
                .withProperty("security.oauth2.social.providers.google.client-id", "cid")
                .withProperty("security.oauth2.social.providers.google.client-secret", "secret")
                .withProperty("security.oauth2.social.providers.google.redirect-uri", "http://x/cb");

        SocialAuthProperties props = bind(env);

        assertTrue(props.enabled());
        assertFalse(props.autoProvision());
        assertEquals(60L, props.ticketTtlSeconds());
        assertEquals(300L, props.stateTtlSeconds());
        assertEquals("http://localhost:3000", props.frontendBaseUrl());
        assertEquals("cid", props.providers().get("google").clientId());
        assertEquals("secret", props.providers().get("google").clientSecret());
        assertEquals("http://x/cb", props.providers().get("google").redirectUri());
    }

    @Test
    void aProviderWithoutCredentialsIsNotConfigured() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("security.oauth2.social.providers.google.client-id", "cid")
                .withProperty("security.oauth2.social.providers.google.client-secret", "");

        SocialAuthProperties props = bind(env);

        assertFalse(props.providers().get("google").isConfigured());
    }

    /**
     * 把「社交端点必须免租户头」这条钉在配置上。
     * TenantFilter 默认 required=true,/authorize 是浏览器导航、Apple 回调是跨域 POST,
     * 两者都带不了 X-Tenant-Id —— 少了这行,两个端点会在进控制器之前就被拒。
     */
    @Test
    void scmAuthExcludesTheSocialEndpointsFromTheTenantFilter() throws Exception {
        String yaml = Files.readString(
                Path.of("src", "main", "resources", "application.yaml"));

        assertTrue(yaml.contains("/api/auth/oauth2/**"),
                "scm.tenant.exclude-paths must contain /api/auth/oauth2/**");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl ../scm-common/web,../scm-auth -f com.scm.parent/pom.xml`
Expected: FAIL —— 编译错误 `cannot find symbol: SecurityConfig.PUBLIC_PATHS` / `SocialAuthProperties`

- [ ] **Step 3: 创建 `SocialAuthProperties`**

`scm-auth/src/main/java/com/scmcloud/auth/config/SocialAuthProperties.java`:

```java
package com.scmcloud.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * 社交登录(provider)配置。
 *
 * <p>两个 provider 走同一套配置形状:google 与 apple 都是 {@code client-id} /
 * {@code client-secret} / {@code redirect-uri}。Apple 的 {@code client-id} 存的是
 * Services ID(形如 {@code teamid.bundleid}),{@code client-secret} 存的是用团队私钥
 * ES256 签名、有效期不超过 6 个月的 JWT —— 它在服务外预先生成,以环境变量注入。
 */
@ConfigurationProperties(prefix = "security.oauth2.social")
public record SocialAuthProperties(
        boolean enabled,
        boolean autoProvision,
        List<String> defaultRoleCodes,
        long ticketTtlSeconds,
        long stateTtlSeconds,
        String frontendBaseUrl,
        Map<String, ProviderProperties> providers) {

    public SocialAuthProperties {
        if (defaultRoleCodes == null) {
            defaultRoleCodes = List.of();
        }
        if (providers == null) {
            providers = Map.of();
        }
    }

    public record ProviderProperties(String clientId, String clientSecret, String redirectUri) {

        /** 三项齐全才算配置完整;否则该 provider 不出现在 /providers 列表里。 */
        public boolean isConfigured() {
            return hasText(clientId) && hasText(clientSecret) && hasText(redirectUri);
        }

        private static boolean hasText(String value) {
            return value != null && !value.isBlank();
        }
    }
}
```

- [ ] **Step 4: 在 `SecurityConfig` 抽出 PUBLIC_PATHS 并加入社交端点**

`scm-common/web/src/main/java/com/scmcloud/common/security/config/SecurityConfig.java`:

在类的字段区加上:

```java
    /** 无需认证即可访问的路径。社交登录的 authorize/callback 是浏览器导航与跨域 POST,带不了认证头。 */
    public static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/api/auth/oauth2/**",
            "/api/public/**"
    );
```

并把 `authorizeHttpRequests` 里那段硬编码列表替换为:

```java
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConfig.PUBLIC_PATHS.toArray(String[]::new)).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
```

- [ ] **Step 5: 配置 `scm-auth` 的 application.yaml**

在 `scm-auth/src/main/resources/application.yaml` 里:

(a) 加 `scm.tenant.exclude-paths`(覆盖默认值,加入社交登录路径):

```yaml
scm:
  tenant:
    exclude-paths:
      - /actuator/**
      - /v3/api-docs/**
      - /api/auth/oauth2/**
```

(b) 在 `security:` 块下加:

```yaml
  oauth2:
    social:
      enabled: ${SOCIAL_AUTH_ENABLED:false}
      auto-provision: ${SOCIAL_AUTH_AUTO_PROVISION:false}
      default-role-codes: []
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

> `scm.tenant.exclude-paths` 是**必须项**:`TenantFilter` 默认 `required=true`,而 `/authorize`
> 是浏览器导航、Apple 回调是跨域 POST,两者都带不了 `X-Tenant-Id`。

- [ ] **Step 6: 运行测试确认通过**

Run: `mvn test -pl ../scm-common/web,../scm-auth -f com.scm.parent/pom.xml`
Expected: PASS(4 个用例)

- [ ] **Step 7: 提交**

```bash
git add scm-common/web/src/main/java/com/scmcloud/common/security/config/SecurityConfig.java \
        scm-common/web/src/test/java/com/scmcloud/common/security/config/SecurityConfigPublicPathsTest.java \
        scm-auth/src/main/java/com/scmcloud/auth/config/SocialAuthProperties.java \
        scm-auth/src/test/java/com/scmcloud/auth/config/SocialAuthPropertiesTest.java \
        scm-auth/src/main/resources/application.yaml
git commit -m "feat(auth): 社交登录配置绑定与 authorize/callback 放行"
```

---

### Task 2: scm-system 社交账号绑定服务与 Dubbo API

**Files:**
- Create: `scm-system/api/src/main/java/com/scmcloud/system/api/dto/OAuthIdentityDTO.java`
- Modify: `scm-system/api/src/main/java/com/scmcloud/system/api/UserDubboService.java`
- Modify: `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysUserMapper.java`
- Modify: `scm-system/service/src/main/java/com/scmcloud/system/mapper/SysUserOauthMapper.java`
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/ISysUserOAuthService.java`
- Create: `scm-system/service/src/main/java/com/scmcloud/system/service/Impl/SysUserOAuthServiceImpl.java`
- Create: `scm-system/service/src/test/java/com/scmcloud/system/service/SysUserOAuthServiceTest.java`

**Interfaces:**
- Consumes: 既有 `SysUserOauth` 实体、`SysUserOauthMapper`、`SysUserMapper`、`SysUser`
- Produces:
  - `OAuthIdentityDTO(UUID userId, String provider, String openid, String email, String nickname, String avatar, Map<String,Object> rawUserInfo)`
  - `UserDubboService` 新增:`Optional<OAuthIdentityDTO> findOAuthBinding(String provider, String openid)`、`List<SecurityUser> findUsersByEmail(String email)`、`UUID upsertOAuthBinding(UUID userId, String provider, String openid, String email, String nickname, String avatar, Map<String,Object> rawUserInfo)`、`UUID createSocialUser(String username, String email, String realName, String avatar, String passwordHash)`、`void updateOAuthLastLogin(UUID bindingId)`
  - `ISysUserOAuthService`:`Optional<OAuthIdentityDTO> findBinding(String provider, String openid)`、`List<SecurityUser> findUsersByEmail(String email)`、`UUID upsertBinding(...)`、`UUID createSocialUser(...)`、`void touchLastLogin(UUID bindingId)`

- [ ] **Step 1: 写失败的测试**

创建 `scm-system/service/src/test/java/com/scmcloud/system/service/SysUserOAuthServiceTest.java`:

```java
package com.scmcloud.system.service;

import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.domain.entity.SysUserOauth;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.mapper.SysUserOauthMapper;
import com.scmcloud.system.service.Impl.SysUserOAuthServiceImpl;
import com.scmcloud.system.api.dto.OAuthIdentityDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysUserOAuthServiceTest {

    private SysUserOauthMapper oauthMapper;
    private SysUserMapper userMapper;
    private SysUserOAuthServiceImpl service;

    @BeforeEach
    void setUp() {
        oauthMapper = mock(SysUserOauthMapper.class);
        userMapper = mock(SysUserMapper.class);
        service = new SysUserOAuthServiceImpl(oauthMapper, userMapper);
    }

    @Test
    void findBindingReturnsTheBoundIdentity() {
        UUID userId = UUID.randomUUID();
        SysUserOauth row = new SysUserOauth()
                .setUserId(userId)
                .setProvider("google")
                .setOauthOpenid("sub-1")
                .setOauthEmail("a@example.com");
        when(oauthMapper.findByProviderAndOpenid("google", "sub-1")).thenReturn(row);

        Optional<OAuthIdentityDTO> found = service.findBinding("google", "sub-1");

        assertTrue(found.isPresent());
        assertEquals(userId, found.get().userId());
        assertEquals("a@example.com", found.get().email());
    }

    @Test
    void findUsersByEmailReturnsEveryMatchSoTheCallerCanDetectAmbiguity() {
        SysUser a = new SysUser().setUsername("a").setEmail("dup@example.com");
        SysUser b = new SysUser().setUsername("b").setEmail("dup@example.com");
        when(userMapper.findByEmail("dup@example.com")).thenReturn(List.of(a, b));

        assertEquals(2, service.findUsersByEmail("dup@example.com").size());
    }

    @Test
    void upsertBindingInsertsWhenTheProviderIdentityIsNew() {
        when(oauthMapper.findByProviderAndOpenid("google", "sub-1")).thenReturn(null);
        UUID userId = UUID.randomUUID();

        UUID bindingId = service.upsertBinding(userId, "google", "sub-1",
                "a@example.com", "Ann", null, Map.of("sub", "sub-1"));

        assertEquals(userId, bindingId);
        ArgumentCaptor<SysUserOauth> captor = ArgumentCaptor.forClass(SysUserOauth.class);
        verify(oauthMapper).insert(captor.capture());
        assertEquals("google", captor.getValue().getProvider());
        assertEquals("sub-1", captor.getValue().getOauthOpenid());
        assertEquals("a@example.com", captor.getValue().getOauthEmail());
    }

    @Test
    void upsertBindingUpdatesTheProfileWhenTheIdentityIsKnown() {
        UUID userId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        SysUserOauth row = new SysUserOauth()
                .setId(bindingId)
                .setUserId(userId)
                .setProvider("google")
                .setOauthOpenid("sub-1")
                .setOauthEmail("old@example.com");
        when(oauthMapper.findByProviderAndOpenid("google", "sub-1")).thenReturn(row);

        UUID result = service.upsertBinding(userId, "google", "sub-1",
                "new@example.com", "Ann", null, Map.of());

        assertEquals(bindingId, result);
        verify(oauthMapper, never()).insert(any());
    }

    @Test
    void createSocialUserDoesNotWriteTheMissingTenantIdColumn() {
        when(userMapper.findByUsername("ann")).thenReturn(null);
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);

        UUID userId = service.createSocialUser("ann", "ann@example.com", "Ann", null, "hashed");

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).insert(captor.capture());
        SysUser inserted = captor.getValue();
        assertEquals("ann", inserted.getUsername());
        assertEquals("ann@example.com", inserted.getEmail());
        assertEquals(1, inserted.getStatus());
        assertEquals(null, inserted.getTenantId());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl ../scm-system/service -Dtest=SysUserOAuthServiceTest -f com.scm.parent/pom.xml`
Expected: FAIL —— 编译错误 `cannot find symbol: SysUserOAuthServiceImpl` / `OAuthIdentityDTO`

- [ ] **Step 3: 创建 DTO 与 Dubbo 方法**

`scm-system/api/src/main/java/com/scmcloud/system/api/dto/OAuthIdentityDTO.java`:

```java
package com.scmcloud.system.api.dto;

import java.util.Map;
import java.util.UUID;

/** 社交账号(provider)与本地用户的绑定关系。 */
public record OAuthIdentityDTO(
        UUID userId,
        String provider,
        String openid,
        String email,
        String nickname,
        String avatar,
        Map<String, Object> rawUserInfo) {
}
```

`UserDubboService` 追加:

```java
    /**
     * 按 provider 与主体标识查找社交账号绑定。
     *
     * @param provider google / apple
     * @param openid   provider 的稳定主体标识
     * @return 绑定信息;未绑定时返回 empty
     */
    Optional<OAuthIdentityDTO> findOAuthBinding(String provider, String openid);

    /**
     * 按邮箱查找本地用户。邮箱列没有唯一约束,可能返回多条 ——
     * 调用方必须自行判断是否存在歧义。
     */
    List<SecurityUser> findUsersByEmail(String email);

    /**
     * 用户名是否已被占用。{@code sys_user.username} 是全局唯一约束,
     * 自动开通社交账号时需要据此去重。
     */
    boolean existsByUsername(String username);

    /**
     * 新增或更新社交账号绑定,并返回绑定记录 id。
     */
    UUID upsertOAuthBinding(UUID userId, String provider, String openid, String email,
                            String nickname, String avatar, Map<String, Object> rawUserInfo);

    /**
     * 为社交登录新建本地用户。密码由调用方以 BCrypt 哈希传入(随机 UUID 的哈希),
     * 使密码登录不可能成功。
     */
    UUID createSocialUser(String username, String email, String realName, String avatar, String passwordHash);

    /** 更新绑定的最后登录时间。 */
    void updateOAuthLastLogin(UUID bindingId);
```

- [ ] **Step 4: Mapper 增补**

`SysUserMapper` 追加:

```java
    /**
     * 按邮箱查找用户。邮箱列没有唯一约束,可能返回多条。
     */
    @Select("""
            SELECT * FROM sys_user
            WHERE email = #{email} AND NOT deleted
            """)
    List<SysUser> findByEmail(@Param("email") String email);
```

`SysUserOauthMapper` 追加:

```java
    /**
     * 更新绑定上的 profile 信息(邮箱/昵称/头像/原始 userinfo)。
     */
    @Update("""
            UPDATE sys_user_oauth
            SET oauth_email = #{email}, oauth_nickname = #{nickname}, oauth_avatar = #{avatar},
                raw_user_info = CAST(#{rawUserInfo} AS jsonb), update_time = NOW()
            WHERE id = #{id}
            """)
    int updateProfile(@Param("id") UUID id,
                       @Param("email") String email,
                       @Param("nickname") String nickname,
                       @Param("avatar") String avatar,
                       @Param("rawUserInfo") String rawUserInfo);
```

> `raw_user_info` 是 JSONB,MyBatis 传 Map 需要显式转 JSON —— 这里传已经序列化好的字符串,
> 由 service 层用 Jackson 序列化。

- [ ] **Step 5: 创建 service 实现**

`scm-system/service/src/main/java/com/scmcloud/system/service/ISysUserOAuthService.java`:

```java
package com.scmcloud.system.service;

import com.scmcloud.system.api.dto.OAuthIdentityDTO;
import com.scmcloud.common.web.domain.SecurityUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ISysUserOAuthService {

    Optional<OAuthIdentityDTO> findBinding(String provider, String openid);

    List<SecurityUser> findUsersByEmail(String email);

    boolean existsByUsername(String username);

    UUID upsertBinding(UUID userId, String provider, String openid, String email,
                       String nickname, String avatar, Map<String, Object> rawUserInfo);

    UUID createSocialUser(String username, String email, String realName, String avatar, String passwordHash);

    void touchLastLogin(UUID bindingId);
}
```

`scm-system/service/src/main/java/com/scmcloud/system/service/Impl/SysUserOAuthServiceImpl.java`:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserOAuthServiceImpl implements ISysUserOAuthService {

    private final SysUserOauthMapper oauthMapper;
    private final SysUserMapper userMapper;

    // 方法体由同包测试与接口签名决定,这里只钉住实现里容易做错的四点:
    //  1. findBinding 返回 Optional.empty()(不是 null),DTO 字段顺序为
    //     (userId, provider, openid, email, nickname, avatar, rawUserInfo)
    //  2. findUsersByEmail 把 SysUser 映射成 SecurityUser.builder()
    //     .userId/.username/.password/.realName/.status,返回全部命中(不自行去重)
    //  3. upsertBinding:findByProviderAndOpenid 命中 → 调 updateProfile 返回既有 id;
    //     未命中 → new SysUserOauth().setUserId(...).setProvider(...)...
    //     .setOauthOpenid(...).setOauthEmail(...).setOauthNickname(...).setOauthAvatar(...)
    //     .setRawUserInfo(map) 后 oauthMapper.insert(row),返回 row.getId()
    //  4. createSocialUser:new SysUser().setUsername(...).setEmail(...).setRealName(...)
    //     .setAvatar(...).setPassword(passwordHash).setStatus(1).setAccountType(1)
    //     .setUserLevel(1).setTenantId(null)  ← 必须显式置 null
    //     然后 userMapper.insert(user),返回 user.getId()
    //  updateProfile 的 rawUserInfo 参数是 JSON 字符串:用 ObjectMapper.writeValueAsString
    //  序列化,序列化失败时写 "{}" 并记 warn,不抛异常
}
```

> `createSocialUser` 里显式 `.setTenantId(null)` 是关键:`AuditMetaObjectHandler` 只在
> `TenantContextHolder` 有值时填充 `tenant_id`,但实体字段一旦被填上,MyBatis-Plus 会把
> `tenant_id` 写进 INSERT —— 而实际建表脚本里 `sys_user` **没有这一列**,会直接报错。
> 显式置 null 让该列被 NOT_NULL 插入策略省略。

- [ ] **Step 6: 在 `UserDubboServiceImpl` 实现新方法**

`scm-system/service/src/main/java/com/scmcloud/system/service/dubbo/UserDubboServiceImpl.java` 注入
`ISysUserOAuthService`,并实现:

```java
    @Override
    public Optional<OAuthIdentityDTO> findOAuthBinding(String provider, String openid) {
        return sysUserOAuthService.findBinding(provider, openid);
    }

    @Override
    public List<SecurityUser> findUsersByEmail(String email) {
        return sysUserOAuthService.findUsersByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return sysUserOAuthService.existsByUsername(username);
    }

    @Override
    public UUID upsertOAuthBinding(UUID userId, String provider, String openid, String email,
                                    String nickname, String avatar, Map<String, Object> rawUserInfo) {
        return sysUserOAuthService.upsertBinding(userId, provider, openid, email, nickname, avatar, rawUserInfo);
    }

    @Override
    public UUID createSocialUser(String username, String email, String realName,
                                 String avatar, String passwordHash) {
        return sysUserOAuthService.createSocialUser(username, email, realName, avatar, passwordHash);
    }

    @Override
    public void updateOAuthLastLogin(UUID bindingId) {
        sysUserOAuthService.touchLastLogin(bindingId);
    }
```

- [ ] **Step 7: 运行测试确认通过**

Run: `mvn test -pl ../scm-system/service -Dtest=SysUserOAuthServiceTest -f com.scm.parent/pom.xml`
Expected: PASS(5 个用例)

- [ ] **Step 8: 提交**

```bash
git add scm-system/api/src/main/java/com/scmcloud/system/api/dto/OAuthIdentityDTO.java \
        scm-system/api/src/main/java/com/scmcloud/system/api/UserDubboService.java \
        scm-system/service/src/main/java/com/scmcloud/system/mapper/SysUserMapper.java \
        scm-system/service/src/main/java/com/scmcloud/system/mapper/SysUserOauthMapper.java \
        scm-system/service/src/main/java/com/scmcloud/system/service/ISysUserOAuthService.java \
        scm-system/service/src/main/java/com/scmcloud/system/service/Impl/SysUserOAuthServiceImpl.java \
        scm-system/service/src/main/java/com/scmcloud/system/service/dubbo/UserDubboServiceImpl.java \
        scm-system/service/src/test/java/com/scmcloud/system/service/SysUserOAuthServiceTest.java
git commit -m "feat(system): 新增社交账号绑定服务与 Dubbo 接口"
```

---

### Task 3: provider 抽象与 Google 实现

**Files:**
- Create: `scm-auth/src/main/java/com/scmcloud/auth/social/SocialProfile.java`
- Create: `scm-auth/src/main/java/com/scmcloud/auth/social/SocialIdentityProvider.java`
- Create: `scm-auth/src/main/java/com/scmcloud/auth/social/GoogleIdentityProvider.java`
- Create: `scm-auth/src/test/java/com/scmcloud/auth/social/GoogleIdentityProviderTest.java`

**Interfaces:**
- Consumes: Task 1 的 `SocialAuthProperties.ProviderProperties`
- Produces:
  - `SocialProfile(String provider, String subject, String email, boolean emailVerified, String name, String avatar, Map<String,Object> rawUserInfo)` —— record
  - `SocialIdentityProvider`:`String provider()`、`boolean isConfigured()`、`URI authorizeUri(String state)`、`SocialProfile exchange(String code)`、`String scope()`
  - `GoogleIdentityProvider(SocialAuthProperties.ProviderProperties props, RestClient.Builder restClientBuilder)`

- [ ] **Step 1: 写失败的测试**

创建 `scm-auth/src/test/java/com/scmcloud/auth/social/GoogleIdentityProviderTest.java`:

```java
package com.scmcloud.auth.social;

import com.scmcloud.auth.config.SocialAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleIdentityProviderTest {

    private MockRestServiceServer server;
    private GoogleIdentityProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new GoogleIdentityProvider(
                new SocialAuthProperties.ProviderProperties(
                        "client-id", "client-secret", "http://localhost:8761/auth/api/auth/oauth2/google/callback"),
                builder);
    }

    @Test
    void authorizeUriCarriesTheStateAndTheRedirectUri() {
        URI uri = provider.authorizeUri("state-123");

        assertEquals("https://accounts.google.com/o/oauth2/v2/auth", uri.getScheme() + "://" + uri.getAuthority() + uri.getPath());
        assertTrue(uri.getQuery().contains("client_id=client-id"));
        assertTrue(uri.getQuery().contains("state=state-123"));
        assertTrue(uri.getQuery().contains("scope=openid%20email%20profile"));
        assertTrue(uri.getQuery().contains("response_type=code"));
    }

    @Test
    void exchangeReturnsTheProfileAndRejectsAnUnverifiedEmail() {
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess("{\"access_token\":\"at\",\"token_type\":\"Bearer\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
                .andRespond(withSuccess(
                        "{\"sub\":\"sub-1\",\"email\":\"a@example.com\",\"email_verified\":true,\"name\":\"Ann\",\"picture\":\"http://pic\"}",
                        MediaType.APPLICATION_JSON));

        SocialProfile profile = provider.exchange("code-1");

        assertEquals("google", profile.provider());
        assertEquals("sub-1", profile.subject());
        assertEquals("a@example.com", profile.email());
        assertTrue(profile.emailVerified());
        assertEquals("Ann", profile.name());
    }

    @Test
    void isConfiguredIsFalseWhenTheSecretIsMissing() {
        GoogleIdentityProvider blank = new GoogleIdentityProvider(
                new SocialAuthProperties.ProviderProperties("client-id", "", "http://x"),
                RestClient.builder());
        assertFalse(blank.isConfigured());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl ../scm-auth -Dtest=GoogleIdentityProviderTest -f com.scm.parent/pom.xml`
Expected: FAIL —— 编译错误 `cannot find symbol: GoogleIdentityProvider`

- [ ] **Step 3: 创建 `SocialProfile` 与 `SocialIdentityProvider`**

`scm-auth/src/main/java/com/scmcloud/auth/social/SocialProfile.java`:

```java
package com.scmcloud.auth.social;

import java.util.Map;

/**
 * provider 返回的用户画像。
 *
 * @param subject        provider 的稳定主体标识(Google 的 {@code sub},Apple 的 {@code sub})
 * @param emailVerified  邮箱是否经过 provider 验证;为 false 时不得用于匹配本地账号
 */
public record SocialProfile(
        String provider,
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String avatar,
        Map<String, Object> rawUserInfo) {
}
```

`scm-auth/src/main/java/com/scmcloud/auth/social/SocialIdentityProvider.java`:

```java
package com.scmcloud.auth.social;

import java.net.URI;

/** 社交 provider 的差异收敛在这个接口后面。 */
public interface SocialIdentityProvider {

    /** provider 代码,如 {@code google} / {@code apple}。 */
    String provider();

    /** 凭据是否齐全。未配置的 provider 不会出现在 /providers 列表里。 */
    boolean isConfigured();

    /** 构造授权页 URL。 */
    URI authorizeUri(String state);

    /** 用授权码换 token 并拉取用户画像。 */
    SocialProfile exchange(String code);
}
```

- [ ] **Step 4: 创建 `GoogleIdentityProvider`**

`scm-auth/src/main/java/com/scmcloud/auth/social/GoogleIdentityProvider.java`:

```java
package com.scmcloud.auth.social;

import com.scmcloud.auth.config.SocialAuthProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Google OAuth2 授权码流程。
 *
 * <p>client secret 只存在于服务端;浏览器只拿到授权页 URL。
 */
@Slf4j
@Component
public class GoogleIdentityProvider implements SocialIdentityProvider {

    private static final String AUTH_URI = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URI = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SCOPE = "openid email profile";

    private final SocialAuthProperties.ProviderProperties props;
    private final RestClient restClient;

    public GoogleIdentityProvider(SocialAuthProperties.ProviderProperties props, RestClient.Builder restClientBuilder) {
        this.props = props;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public String provider() {
        return "google";
    }

    @Override
    public boolean isConfigured() {
        return props.isConfigured();
    }

    @Override
    public URI authorizeUri(String state) {
        return UriComponentsBuilder.fromUriString(AUTH_URI)
                .queryParam("client_id", props.clientId())
                .queryParam("redirect_uri", props.redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .build(true)
                .toUri();
    }

    @Override
    public SocialProfile exchange(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        form.add("redirect_uri", props.redirectUri());
        form.add("grant_type", "authorization_code");

        @SuppressWarnings("unchecked")
        Map<String, Object> token = restClient.post()
                .uri(TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        String accessToken = (String) token.get("access_token");

        @SuppressWarnings("unchecked")
        Map<String, Object> info = restClient.get()
                .uri(USERINFO_URI)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        return new SocialProfile(
                provider(),
                (String) info.get("sub"),
                (String) info.get("email"),
                Boolean.TRUE.equals(info.get("email_verified")),
                (String) info.get("name"),
                (String) info.get("picture"),
                info);
    }
}
```

> `UriComponentsBuilder.build(true)` 表示参数已编码;`scope` 里的空格会被编码成 `%20`。

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn test -pl ../scm-auth -Dtest=GoogleIdentityProviderTest -f com.scm.parent/pom.xml`
Expected: PASS(3 个用例)

- [ ] **Step 6: 提交**

```bash
git add scm-auth/src/main/java/com/scmcloud/auth/social/SocialProfile.java \
        scm-auth/src/main/java/com/scmcloud/auth/social/SocialIdentityProvider.java \
        scm-auth/src/main/java/com/scmcloud/auth/social/GoogleIdentityProvider.java \
        scm-auth/src/test/java/com/scmcloud/auth/social/GoogleIdentityProviderTest.java
git commit -m "feat(auth): 新增 provider 抽象与 Google 授权码实现"
```

---

### Task 4: 绑定策略、账号状态校验与平台令牌签发

**Files:**
- Create: `scm-auth/src/main/java/com/scmcloud/auth/service/SocialAuthService.java`
- Create: `scm-auth/src/main/java/com/scmcloud/auth/service/Impl/SocialAuthServiceImpl.java`
- Create: `scm-auth/src/test/java/com/scmcloud/auth/service/SocialAuthServiceImplTest.java`

**Interfaces:**
- Consumes: Task 2 的 `UserDubboService`;Task 3 的 `SocialIdentityProvider` / `SocialProfile`;既有 `JwtUtils`、`SecurityUser`、`PasswordEncoder`
- Produces:
  - `SocialAuthService`:`List<String> enabledProviders()`、`URI buildAuthorizeUri(String provider, String state)`、`LoginResponse completeLogin(SocialProfile profile, String ipAddress, String deviceId)`
  - 异常:`SocialAuthException(String message)` —— 绑定失败/账号不可用/邮箱歧义时抛出,控制器转成 400

- [ ] **Step 1: 写失败的测试**

创建 `scm-auth/src/test/java/com/scmcloud/auth/service/SocialAuthServiceImplTest.java`:

```java
package com.scmcloud.auth.service;

import com.scmcloud.auth.config.SocialAuthProperties;
import com.scmcloud.auth.social.SocialProfile;
import com.scmcloud.common.dto.user.LoginResponse;
import com.scmcloud.common.log.service.ISysAuditLogService;
import com.scmcloud.common.metrics.BusinessMetrics;
import com.scmcloud.common.security.util.JwtUtils;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.system.api.UserDubboService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SocialAuthServiceImplTest {

    private UserDubboService userDubboService;
    private JwtUtils jwtUtils;
    private PasswordEncoder passwordEncoder;
    private ISysAuditLogService auditLogService;
    private BusinessMetrics businessMetrics;
    private SocialAuthServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userDubboService = mock(UserDubboService.class);
        jwtUtils = mock(JwtUtils.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditLogService = mock(ISysAuditLogService.class);
        businessMetrics = mock(BusinessMetrics.class);
        service = new SocialAuthServiceImpl(
                userDubboService,
                jwtUtils,
                passwordEncoder,
                auditLogService,
                businessMetrics,
                new SocialAuthProperties(false, false, List.of(), 60, 300, "http://localhost:3000", Map.of()),
                Map.of());
    }

    private static SocialProfile profile(boolean verified) {
        return new SocialProfile("google", "sub-1", "a@example.com", verified, "Ann", null, Map.of());
    }

    private static SecurityUser enabledUser() {
        return SecurityUser.builder()
                .userId(USER_ID)
                .username("ann")
                .realName("Ann")
                .status(1)
                .roles(Set.of("system:user:list"))
                .permissions(Set.of("system:user:list"))
                .build();
    }

    @Test
    void anExistingBindingSignsTheUserInWithTheProviderAsTheAmr() {
        when(userDubboService.findOAuthBinding("google", "sub-1"))
                .thenReturn(Optional.of(new com.scmcloud.system.api.dto.OAuthIdentityDTO(
                        USER_ID, "google", "sub-1", "a@example.com", "Ann", null, Map.of())));
        when(userDubboService.getUserByUsername("ann")).thenReturn(enabledUser());
        when(jwtUtils.generateAccessToken(any(), anyString(), any(), any(), anyString(), anyString(), anyList()))
                .thenReturn("access-token");

        LoginResponse response = service.completeLogin(profile(true), "1.2.3.4", "device-1");

        assertEquals("access-token", response.getAccessToken());
        ArgumentCaptor<List<String>> amr = ArgumentCaptor.forClass(List.class);
        verify(jwtUtils).generateAccessToken(eq(USER_ID), eq("ann"), any(), any(),
                eq("device-1"), eq("1.2.3.4"), amr.capture());
        assertEquals(List.of("google"), amr.getValue());
    }

    @Test
    void anUnknownIdentityWithoutAutoProvisionIsRejected() {
        when(userDubboService.findOAuthBinding("google", "sub-1")).thenReturn(Optional.empty());
        when(userDubboService.findUsersByEmail("a@example.com")).thenReturn(List.of());

        SocialAuthException error = assertThrows(SocialAuthException.class,
                () -> service.completeLogin(profile(true), "1.2.3.4", "device-1"));
        assertTrue(error.getMessage().contains("a@example.com"));
    }

    @Test
    void anAmbiguousEmailIsRejectedInsteadOfPickingOne() {
        when(userDubboService.findOAuthBinding("google", "sub-1")).thenReturn(Optional.empty());
        when(userDubboService.findUsersByEmail("a@example.com"))
                .thenReturn(List.of(enabledUser(), enabledUser()));

        SocialAuthException error = assertThrows(SocialAuthException.class,
                () -> service.completeLogin(profile(true), "1.2.3.4", "device-1"));
        assertTrue(error.getMessage().contains("a@example.com"));
        verify(userDubboService, never()).upsertOAuthBinding(any(), anyString(), anyString(),
                anyString(), any(), any(), any());
    }

    @Test
    void anUnverifiedEmailCannotBindToALocalAccount() {
        when(userDubboService.findOAuthBinding("google", "sub-1")).thenReturn(Optional.empty());

        assertThrows(SocialAuthException.class,
                () -> service.completeLogin(profile(false), "1.2.3.4", "device-1"));
        verify(userDubboService, never()).findUsersByEmail(anyString());
    }

    @Test
    void aDisabledAccountCannotSignInThroughSocialLogin() {
        SecurityUser disabled = SecurityUser.builder()
                .userId(USER_ID).username("ann").status(0).roles(Set.of()).permissions(Set.of()).build();
        when(userDubboService.findOAuthBinding("google", "sub-1"))
                .thenReturn(Optional.of(new com.scmcloud.system.api.dto.OAuthIdentityDTO(
                        USER_ID, "google", "sub-1", "a@example.com", "Ann", null, Map.of())));
        when(userDubboService.getUserByUsername("ann")).thenReturn(disabled);

        assertThrows(SocialAuthException.class,
                () -> service.completeLogin(profile(true), "1.2.3.4", "device-1"));
        verify(jwtUtils, never()).generateAccessToken(any(), anyString(), any(), any(), anyString(), anyString(), anyList());
    }

    @Test
    void aSuccessfulSocialLoginIsWrittenToTheAuditTrailAndTheMetrics() {
        when(userDubboService.findOAuthBinding("google", "sub-1"))
                .thenReturn(Optional.of(new com.scmcloud.system.api.dto.OAuthIdentityDTO(
                        USER_ID, "google", "sub-1", "a@example.com", "Ann", null, Map.of())));
        when(userDubboService.getUserByUsername("ann")).thenReturn(enabledUser());
        when(jwtUtils.generateAccessToken(any(), anyString(), any(), any(), anyString(), anyString(), anyList()))
                .thenReturn("access-token");

        service.completeLogin(profile(true), "1.2.3.4", "device-1");

        verify(auditLogService).recordLogin(USER_ID, "ann", "1.2.3.4", true, "Social login: google");
        verify(businessMetrics).recordLogin(true, "google");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl ../scm-auth -Dtest=SocialAuthServiceImplTest -f com.scm.parent/pom.xml`
Expected: FAIL —— 编译错误 `cannot find symbol: SocialAuthServiceImpl`

- [ ] **Step 3: 创建异常与服务接口**

`scm-auth/src/main/java/com/scmcloud/auth/service/SocialAuthException.java`:

```java
package com.scmcloud.auth.service;

/** 社交登录过程中的业务失败(账号未开通、邮箱歧义、账号不可用)。 */
public class SocialAuthException extends RuntimeException {

    public SocialAuthException(String message) {
        super(message);
    }
}
```

`scm-auth/src/main/java/com/scmcloud/auth/service/SocialAuthService.java`:

```java
package com.scmcloud.auth.service;

import com.scmcloud.auth.social.SocialProfile;
import com.scmcloud.common.dto.user.LoginResponse;

import java.net.URI;
import java.util.List;

public interface SocialAuthService {

    /** 已配置凭据的 provider 代码列表。 */
    List<String> enabledProviders();

    /** 构造 provider 授权页 URL。 */
    URI buildAuthorizeUri(String provider, String state);

    /**
     * 用授权码换取 provider 令牌并拉取用户画像。
     *
     * @throws SocialAuthException provider 未配置或授权码无效
     */
    SocialProfile exchangeProfile(String provider, String code);

    /**
     * 完成登录:绑定或查找本地账号 → 校验账号状态 → 签发平台令牌。
     *
     * @throws SocialAuthException 账号未开通、邮箱歧义、账号停用或锁定
     */
    LoginResponse completeLogin(SocialProfile profile, String ipAddress, String deviceId);
}
```

- [ ] **Step 4: 实现 `SocialAuthServiceImpl`**

`scm-auth/src/main/java/com/scmcloud/auth/service/Impl/SocialAuthServiceImpl.java`:

```java
@Slf4j
@Service
public class SocialAuthServiceImpl implements SocialAuthService {

    private final UserDubboService userDubboService;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final ISysAuditLogService auditLogService;
    private final BusinessMetrics businessMetrics;
    private final SocialAuthProperties properties;
    private final Map<String, SocialIdentityProvider> providers;

    // 构造器入参就是上面这 7 个,顺序固定,便于单测直接 new 出来。
    // 方法体由同包测试与接口签名决定,这里只钉住实现里容易做错的决策:
    //
    //  enabledProviders():providers.values() 里 filter(isConfigured).map(provider())
    //                      .sorted().toList() —— 未配置的 provider 不出现
    //  buildAuthorizeUri(p, state) / exchangeProfile(p, code):
    //                      都先 requireProvider(p);未配置或不认识则抛
    //                      SocialAuthException("Unsupported social provider: " + p)
    //  completeLogin(profile, ipAddress, deviceId):
    //    1. userDubboService.findOAuthBinding(profile.provider(), profile.subject())
    //    2. 命中 → 用 binding.userId() 调 userDubboService.getUserById(userId)
    //       拿 username,再 getUserByUsername(username) 取 SecurityUser
    //       (roles / permissions 都在 SecurityUser 上)
    //    3. 未命中 → resolveNewUser(profile)
    //    4. assertUsable(user):user 为 null → "Bound account no longer exists";
    //       status == null 或 != 1 → "Account is disabled";status == 2 → "Account is locked"
    //    5. 未命中绑定时调 userDubboService.upsertOAuthBinding(userId, provider, subject,
    //       email, name, avatar, rawUserInfo)
    //    6. amr = List.of(profile.provider())  ← 只能是 provider,不得写 "pwd"
    //    7. jwtUtils.generateAccessToken(userId, username, roles, permissions,
    //       deviceId, ipAddress, amr)
    //    8. jwtUtils.generateRefreshToken(userId, username, deviceId)
    //    9. userDubboService.updateLastLogin(userId, ipAddress, LocalDateTime.now())
    //   10. auditLogService.recordLogin(userId, username, ipAddress, true,
    //       "Social login: " + profile.provider())
    //   11. businessMetrics.recordLogin(true, profile.provider())
    //   12. 返回 LoginResponse.builder():accessToken / refreshToken / tokenType="Bearer"
    //       / userId / username / realName / roles / permissions
    //
    //  resolveNewUser(profile):
    //    a. profile.emailVerified() 为 false 或 email 为空 → 抛
    //       SocialAuthException("Provider did not return a verified email address")
    //    b. userDubboService.findUsersByEmail(email):
    //       size > 1 → 抛 SocialAuthException("Multiple local accounts share the email "
    //       + email + "; contact an administrator")   ← 绝不取第一条
    //       size == 1 → 返回那一个
    //       size == 0 → 走自动开通分支
    //    c. 自动开通分支(properties.autoProvision() 为 false 时抛
    //       SocialAuthException("No account is provisioned for " + email +
    //       "; contact an administrator")):
    //       username = uniqueUsername(email)
    //       passwordHash = passwordEncoder.encode(UUID.randomUUID().toString())
    //       userId = userDubboService.createSocialUser(username, email, profile.name(),
    //                   profile.avatar(), passwordHash)
    //       返回 SecurityUser.builder().userId(userId).username(username)
    //             .realName(profile.name()).status(1)
    //             .roles(Set.copyOf(properties.defaultRoleCodes())).permissions(Set.of()).build()
    //
    //  uniqueUsername(email):local = email 的 @ 前缀;candidate = local;
    //    while (userDubboService.existsByUsername(candidate)) candidate = local + suffix++;
    //    返回 candidate
}
```

> 并发首次登录时两个请求可能算出同一个用户名 —— 靠 `sys_user.username` 的唯一约束兜底,
> 捕获 `DuplicateKeyException` 后重试下一个后缀。

> **审计与指标**:密码登录路径会写 `auditLogService.recordLogin(...)` 与
> `businessMetrics.recordLogin(...)`。社交路径必须写同样的记录,否则登录审计里查不到
> 社交登录。`SocialAuthServiceImpl` 因此要注入 `ISysAuditLogService` 与 `BusinessMetrics`
> (分别在 `scm-common/web` 与 `scm-common/monitoring`,`scm-auth` 已有依赖)。
> `SocialAuthServiceImplTest` 里补一条断言这两个方法被调用的用例。

> `assertUsable` 里 `status != 1` 已经覆盖了 `status == 2`(锁定),两个分支保留是为了错误文案可读。

> **审计与指标**:密码登录路径会写 `auditLogService.recordLogin(...)` 与
> `businessMetrics.recordLogin(...)`。社交路径必须写同样的记录,否则登录审计里查不到
> 社交登录。`SocialAuthServiceImpl` 因此要注入 `ISysAuditLogService` 与 `BusinessMetrics`
> (两者在 `scm-common/web` 与 `scm-common/monitoring`,`scm-auth` 已有依赖),并在签发令牌后写:
> ```java
> auditLogService.recordLogin(user.getUserId(), username, ipAddress, true,
>         "Social login: " + profile.provider());
> businessMetrics.recordLogin(true, profile.provider());
> ```
> `SocialAuthServiceImplTest` 里补一条断言这两个方法被调用的用例。

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn test -pl ../scm-auth -Dtest=SocialAuthServiceImplTest -f com.scm.parent/pom.xml`
Expected: PASS(5 个用例)

- [ ] **Step 6: 提交**

```bash
git add scm-auth/src/main/java/com/scmcloud/auth/service/SocialAuthException.java \
        scm-auth/src/main/java/com/scmcloud/auth/service/SocialAuthService.java \
        scm-auth/src/main/java/com/scmcloud/auth/service/Impl/SocialAuthServiceImpl.java \
        scm-auth/src/test/java/com/scmcloud/auth/service/SocialAuthServiceImplTest.java
git commit -m "feat(auth): 社交登录账号绑定、状态校验与平台令牌签发"
```

---

### Task 5: 一次性票据、state 与四个 HTTP 端点

**Files:**
- Create: `scm-auth/src/main/java/com/scmcloud/auth/service/SocialAuthTicketStore.java`
- Create: `scm-auth/src/main/java/com/scmcloud/auth/service/Impl/RedisSocialAuthTicketStore.java`
- Create: `scm-auth/src/main/java/com/scmcloud/auth/controller/SocialAuthController.java`
- Create: `scm-auth/src/test/java/com/scmcloud/auth/controller/SocialAuthControllerTest.java`

**Interfaces:**
- Consumes: Task 4 的 `SocialAuthService`;既有 `StringRedisTemplate`、`UUIDv7Util`、`ApiResponse`
- Produces:
  - `SocialAuthTicketStore`:`void store(String ticket, LoginResponse response)`、`Optional<LoginResponse> consume(String ticket)`、`void storeState(String state, String locale, String returnTo)`、`Optional<State> consumeState(String state)`
  - `SocialAuthTicketStore.State(String locale, String returnTo)` —— record
  - `SocialAuthController` —— 4 个端点(见下)

- [ ] **Step 1: 写失败的测试**

创建 `scm-auth/src/test/java/com/scmcloud/auth/controller/SocialAuthControllerTest.java`:

```java
package com.scmcloud.auth.controller;

import com.scmcloud.auth.service.SocialAuthException;
import com.scmcloud.auth.service.SocialAuthService;
import com.scmcloud.auth.service.SocialAuthTicketStore;
import com.scmcloud.common.dto.user.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SocialAuthControllerTest {

    private SocialAuthService socialAuthService;
    private SocialAuthTicketStore ticketStore;
    private SocialAuthController controller;

    @BeforeEach
    void setUp() {
        socialAuthService = mock(SocialAuthService.class);
        ticketStore = mock(SocialAuthTicketStore.class);
        controller = new SocialAuthController(socialAuthService, ticketStore, "http://localhost:3000");
    }

    @Test
    void providersOnlyListsConfiguredOnes() {
        when(socialAuthService.enabledProviders()).thenReturn(List.of("google"));

        assertEquals(List.of("google"), controller.providers().getData());
    }

    @Test
    void authorizeRedirectsToTheProviderAndStoresTheState() {
        when(socialAuthService.buildAuthorizeUri(anyString(), anyString()))
                .thenReturn(java.net.URI.create("https://accounts.google.com/o/oauth2/v2/auth?state=s1"));

        ResponseEntity<Void> response = controller.authorize("google", "zh-CN", "/zh-CN/dashboard",
                new MockHttpServletRequest());

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("https://accounts.google.com/o/oauth2/v2/auth?state=s1",
                response.getHeaders().getFirst("Location"));
    }

    @Test
    void callbackRedirectsBackToTheFrontendWithATicket() {
        LoginResponse login = LoginResponse.builder().accessToken("at").userId(UUID.randomUUID()).username("ann").build();
        when(ticketStore.consumeState("state-1"))
                .thenReturn(Optional.of(new SocialAuthTicketStore.State("zh-CN", "/zh-CN/dashboard")));
        when(socialAuthService.exchangeProfile("google", "code-1"))
                .thenReturn(new SocialProfile("google", "sub-1", "a@example.com", true, "Ann", null, Map.of()));
        when(socialAuthService.completeLogin(any(), anyString(), anyString())).thenReturn(login);

        ResponseEntity<Void> response = controller.callback("google", "code-1", "state-1",
                new MockHttpServletRequest());

        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        String location = response.getHeaders().getFirst("Location");
        assertEquals(true, location.startsWith("http://localhost:3000/zh-CN/auth/callback?ticket="));
    }

    @Test
    void exchangeReturnsTheLoginResponseConsumedFromTheTicket() {
        LoginResponse login = LoginResponse.builder().accessToken("at").userId(UUID.randomUUID()).username("ann").build();
        when(ticketStore.consume("ticket-1")).thenReturn(Optional.of(login));

        assertEquals("at", controller.exchange(new SocialAuthController.ExchangeRequest("ticket-1")).getData().getAccessToken());
    }

    @Test
    void exchangeRejectsAnUnknownTicket() {
        when(ticketStore.consume("bad")).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.exchangeRaw(new SocialAuthController.ExchangeRequest("bad"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void aBusinessFailureBecomesABadRequest() {
        when(ticketStore.consumeState("state-1"))
                .thenReturn(Optional.of(new SocialAuthTicketStore.State("zh-CN", "")));
        when(socialAuthService.completeLogin(any(), anyString(), anyString()))
                .thenThrow(new SocialAuthException("no account"));

        ResponseEntity<?> response = controller.callbackRaw("google", "code-1", "state-1",
                new MockHttpServletRequest());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn test -pl ../scm-auth -Dtest=SocialAuthControllerTest -f com.scm.parent/pom.xml`
Expected: FAIL —— 编译错误 `cannot find symbol: SocialAuthController`

- [ ] **Step 3: 创建票据存储接口与 Redis 实现**

`scm-auth/src/main/java/com/scmcloud/auth/service/SocialAuthTicketStore.java`:

```java
package com.scmcloud.auth.service;

import com.scmcloud.common.dto.user.LoginResponse;

import java.util.Optional;

/**
 * 社交登录的一次性票据与 state 存储。
 *
 * <p>票据承载已经签发好的平台令牌,前端用票据换回与密码登录同结构的 LoginResponse ——
 * 令牌因此不会出现在 URL / 浏览器历史 / referrer 里。
 */
public interface SocialAuthTicketStore {

    void store(String ticket, LoginResponse response);

    /** 读取即删除:同一张票据只能兑换一次。 */
    Optional<LoginResponse> consume(String ticket);

    void storeState(String state, String locale, String returnTo);

    Optional<State> consumeState(String state);

    /** state 携带的回跳信息。 */
    record State(String locale, String returnTo) {
    }
}
```

`scm-auth/src/main/java/com/scmcloud/auth/service/Impl/RedisSocialAuthTicketStore.java`:

```java
package com.scmcloud.auth.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.auth.service.SocialAuthTicketStore;
import com.scmcloud.common.dto.user.LoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSocialAuthTicketStore implements SocialAuthTicketStore {

    private static final String TICKET_PREFIX = "auth:oauth2:ticket:";
    private static final String STATE_PREFIX = "auth:oauth2:state:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void store(String ticket, LoginResponse response) {
        try {
            redisTemplate.opsForValue().set(
                    TICKET_PREFIX + ticket,
                    objectMapper.writeValueAsString(response),
                    Duration.ofSeconds(60));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store the social login ticket", e);
        }
    }

    @Override
    public Optional<LoginResponse> consume(String ticket) {
        String key = TICKET_PREFIX + ticket;
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            return Optional.empty();
        }
        redisTemplate.delete(key);
        try {
            return Optional.of(objectMapper.readValue(payload, LoginResponse.class));
        } catch (Exception e) {
            log.warn("Failed to read the social login ticket: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void storeState(String state, String locale, String returnTo) {
        try {
            redisTemplate.opsForValue().set(
                    STATE_PREFIX + state,
                    objectMapper.writeValueAsString(new State(locale, returnTo)),
                    Duration.ofSeconds(300));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to store the social login state", e);
        }
    }

    @Override
    public Optional<State> consumeState(String state) {
        String key = STATE_PREFIX + state;
        String payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            return Optional.empty();
        }
        redisTemplate.delete(key);
        try {
            return Optional.of(objectMapper.readValue(payload, State.class));
        } catch (Exception e) {
            log.warn("Failed to read the social login state: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
```

- [ ] **Step 4: 创建控制器**

`scm-auth/src/main/java/com/scmcloud/auth/controller/SocialAuthController.java`:

```java
@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth/oauth2")
@Tag(name = "社交登录", description = "提供 Google / Apple 授权码登录与票据兑换能力。")
public class SocialAuthController {

    private final SocialAuthService socialAuthService;
    private final SocialAuthTicketStore ticketStore;
    private final String frontendBaseUrl;

    // 构造器入参顺序固定为上面 3 个;frontendBaseUrl 用
    // @Value("${security.oauth2.social.frontend-base-url:http://localhost:3000}") 注入。
    // 注意:**不要**加 @RequiredArgsConstructor —— 显式构造器已经存在,两者并存会编译失败。
}
```

四个端点的行为契约(方法体由同包测试与下表决定):

| 方法 | 路径 | 成功响应 | 失败响应 |
|------|------|----------|----------|
| GET | `/api/auth/oauth2/providers` | `ApiResponse<List<String>>`,data 是已配置的 provider 代码 | —— |
| GET | `/api/auth/oauth2/{provider}/authorize` | 302,`Location` = provider 授权页 URL | `SocialAuthException` → 400 |
| GET | `/api/auth/oauth2/{provider}/callback` | 302,`Location` = `{frontendBaseUrl}/{locale}/auth/callback?ticket={ticket}` | `SocialAuthException` → 400 |
| POST | `/api/auth/oauth2/{provider}/callback` | 同上(Apple 的 form_post 走这个) | 同上 |
| POST | `/api/auth/oauth2/exchange` | `ApiResponse<LoginResponse>`,结构与 `POST /api/auth/login` 完全相同 | 票据无效/过期 → 400 |

`authorize` 的实现要点:

- `state = UUID.randomUUID().toString()`
- `ticketStore.storeState(state, locale == null ? "zh-CN" : locale, returnTo == null ? "" : returnTo)`
- `socialAuthService.buildAuthorizeUri(provider, state)` → 302 `Location`

`callback` 的实现要点:

- `ticketStore.consumeState(state)` 拿回 locale;拿不到 → 抛
  `SocialAuthException("Unknown or expired state")`
- `socialAuthService.exchangeProfile(provider, code)` → `SocialProfile`
- `socialAuthService.completeLogin(profile, IpUtils.getClientIp(request),
  HttpServletRequestUtils.getDeviceId(request))` → `LoginResponse`
- `ticket = UUID.randomUUID().toString()`;`ticketStore.store(ticket, response)`
- 302 到 `{frontendBaseUrl}/{locale}/auth/callback?ticket={ticket}`

为了让单测能直接断言状态码,把两个失败路径做成包可见的方法:

- `ResponseEntity<Void> callbackRaw(String provider, String code, String state, HttpServletRequest request)` ——
  GET/POST 两个 callback 端点都委托给它
- `ResponseEntity<?> exchangeRaw(ExchangeRequest request)` —— `exchange` 端点委托给它,
  票据无效时返回 400

请求体:

```java
public record ExchangeRequest(@NotBlank String ticket) {
}
```

> `SocialAuthService.exchangeProfile(provider, code)`(Task 4 已定义)负责委托给对应 provider 实现,
> 控制器通过它拿到 `SocialProfile`。

- [ ] **Step 5: 运行测试确认通过**

Run: `mvn test -pl ../scm-auth -Dtest=SocialAuthControllerTest -f com.scm.parent/pom.xml`
Expected: PASS(6 个用例)

- [ ] **Step 6: 提交**

```bash
git add scm-auth/src/main/java/com/scmcloud/auth/service/SocialAuthTicketStore.java \
        scm-auth/src/main/java/com/scmcloud/auth/service/Impl/RedisSocialAuthTicketStore.java \
        scm-auth/src/main/java/com/scmcloud/auth/controller/SocialAuthController.java \
        scm-auth/src/main/java/com/scmcloud/auth/service/SocialAuthService.java \
        scm-auth/src/main/java/com/scmcloud/auth/service/Impl/SocialAuthServiceImpl.java \
        scm-auth/src/test/java/com/scmcloud/auth/controller/SocialAuthControllerTest.java
git commit -m "feat(auth): 新增社交登录票据存储与 authorize/callback/exchange 端点"
```

---

### Task 6: 前端社交按钮与 provider 列表

**Files:**
- Create: `scm-web/src/lib/api/social-auth-client.ts`
- Modify: `scm-web/src/lib/api/services/auth.ts`
- Modify: `scm-web/src/lib/api/contracts/auth.ts`
- Create: `scm-web/src/components/auth/social-auth-buttons.tsx`
- Create: `scm-web/src/components/auth/social-auth-buttons.test.tsx`
- Modify: `scm-web/src/components/auth/login-form.tsx`

**Interfaces:**
- Consumes: Task 5 的 `GET /auth/api/auth/oauth2/providers`;前置计划产出的 `LoginCard` / `LoginForm`
- Produces:
  - `socialAuthClient` —— 独立 axios 实例(无 401 自动刷新/跳转)
  - `authService.oauth2.providers(): Promise<string[]>`、`authService.oauth2.exchange(ticket): Promise<LoginResponse>`
  - `SocialAuthButtons()` —— 渲染已配置 provider 的按钮 + 「或」分隔线

- [ ] **Step 1: 写失败的测试**

创建 `scm-web/src/components/auth/social-auth-buttons.test.tsx`:

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const providers = vi.fn();

vi.mock("@/lib/api/services/auth", () => ({
  authService: { oauth2: { providers: (...args: unknown[]) => providers(...args) } },
}));

vi.mock("next-intl", () => ({
  useTranslations: () => (key: string) => key,
  useLocale: () => "zh-CN",
}));

describe("SocialAuthButtons", () => {
  beforeEach(() => {
    providers.mockReset();
    providers.mockResolvedValue(["google"]);
  });

  it("renders a button for every configured provider", async () => {
    const { SocialAuthButtons } = await import("./social-auth-buttons");
    render(<SocialAuthButtons />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "login.social.google" })).toBeInTheDocument();
    });
    expect(screen.queryByRole("button", { name: "login.social.apple" })).not.toBeInTheDocument();
  });

  it("hides the buttons when the provider list cannot be fetched", async () => {
    providers.mockRejectedValue(new Error("network"));
    const { SocialAuthButtons } = await import("./social-auth-buttons");
    render(<SocialAuthButtons />);

    await waitFor(() => {
      expect(screen.queryByRole("button", { name: "login.social.google" })).not.toBeInTheDocument();
    });
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd scm-web && npm test -- src/components/auth/social-auth-buttons.test.tsx`
Expected: FAIL —— `Cannot find module './social-auth-buttons'`

- [ ] **Step 3: 创建独立 axios 实例**

`scm-web/src/lib/api/social-auth-client.ts`:

```ts
import axios from "axios";

/**
 * 社交登录专用客户端。
 *
 * 不能复用 `apiClient`:它的响应拦截器对任何 401 都会尝试刷新 token,失败后
 * `window.location.href = '/login'` —— 票据无效/过期时那会变成静默跳转,
 * 回调页的错误态就显示不出来了。这里不装任何 401 拦截器。
 */
const socialAuthClient = axios.create({
  baseURL:
    process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8761",
  timeout: 30000,
  headers: { "Content-Type": "application/json" },
});

export default socialAuthClient;
```

- [ ] **Step 4: 扩展 auth service 与 contracts**

`scm-web/src/lib/api/contracts/auth.ts` 追加:

```ts
export interface OAuthExchangeRequest {
  ticket: string;
}
```

`scm-web/src/lib/api/services/auth.ts` 追加(文件顶部 `import socialAuthClient from "../social-auth-client";`):

```ts
  oauth2: {
    providers: () =>
      unwrap<string[]>(apiClient.get(`${AUTH_BASE}/oauth2/providers`)),

    // 用独立客户端:票据兑换失败必须能把错误抛回回调页,而不是被全局 401 逻辑吞掉
    exchange: (ticket: string) =>
      unwrap<LoginResponse>(
        socialAuthClient.post(`${AUTH_BASE}/oauth2/exchange`, { ticket })
      ),
  },
```

- [ ] **Step 5: 创建社交按钮组件**

`scm-web/src/components/auth/social-auth-buttons.tsx`:

```tsx
"use client";

import { useQuery } from "@tanstack/react-query";
import { useTranslations } from "next-intl";

import { authService } from "@/lib/api/services/auth";

const PROVIDER_ICONS: Record<string, React.ReactNode> = {
  google: (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="size-4">
      <path
        fill="#4285F4"
        d="M21.6 12.2c0-.7-.06-1.36-.18-2H12v3.79h5.38a4.6 4.6 0 0 1-2 3.02v2.5h3.24c1.9-1.75 2.98-4.32 2.98-7.3Z"
      />
      <path
        fill="#34A853"
        d="M12 22c2.7 0 4.98-.9 6.63-2.42l-3.24-2.51c-.9.6-2.05.96-3.39.96-2.6 0-4.81-1.76-5.6-4.13H3.06v2.6A10 10 0 0 0 12 22Z"
      />
      <path
        fill="#FBBC05"
        d="M6.4 13.9a6 6 0 0 1 0-3.8V7.5H3.06a10 10 0 0 0 0 9l3.34-2.6Z"
      />
      <path
        fill="#EA4335"
        d="M12 5.97c1.47 0 2.79.5 3.83 1.5l2.87-2.87A9.63 9.63 0 0 0 12 2a10 10 0 0 0-8.94 5.5l3.34 2.6C7.19 7.73 9.4 5.97 12 5.97Z"
      />
    </svg>
  ),
  apple: (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="size-4 fill-current">
      <path d="M16.36 12.7c-.02-2.2 1.8-3.26 1.88-3.31-1.02-1.49-2.6-1.7-3.16-1.72-1.34-.14-2.62.79-3.3.79-.69 0-1.74-.77-2.86-.75-1.47.02-2.83.85-3.58 2.17-1.53 2.65-.39 6.57 1.1 8.72.73 1.05 1.6 2.23 2.74 2.19 1.1-.04 1.52-.71 2.85-.71 1.32 0 1.7.71 2.86.69 1.18-.02 1.93-1.07 2.65-2.12.84-1.22 1.18-2.4 1.2-2.46-.03-.01-2.3-.88-2.32-3.49ZM14.2 6.1c.59-.72.99-1.72.88-2.71-.85.03-1.88.57-2.49 1.28-.55.63-1.03 1.66-.9 2.64.95.07 1.92-.48 2.51-1.21Z" />
    </svg>
  ),
};

interface SocialAuthButtonsProps {
  onProviderClick?: (provider: string) => void;
}

export function SocialAuthButtons({ onProviderClick }: SocialAuthButtonsProps) {
  const t = useTranslations("login");
  const { data: providers } = useQuery({
    queryKey: ["oauth2-providers"],
    queryFn: () => authService.oauth2.providers(),
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  const available = providers ?? [];
  if (available.length === 0) {
    return null;
  }

  return (
    <div className="flex flex-col gap-3">
      {available.map((provider) => (
        <button
          key={provider}
          type="button"
          onClick={() => onProviderClick?.(provider)}
          className="flex h-12 w-full items-center justify-center gap-2.5 rounded-lg bg-[var(--auth-btn-soft-bg)] text-sm font-medium text-[var(--auth-ink)] transition-colors hover:bg-[var(--auth-btn-soft-hover)]"
        >
          {PROVIDER_ICONS[provider]}
          {t(`social.${provider}`)}
        </button>
      ))}

      <div className="flex items-center gap-3 py-1">
        <span className="h-px flex-1 bg-[var(--auth-line)]" />
        <span className="text-xs text-[var(--auth-ink-soft)]">{t("social.or")}</span>
        <span className="h-px flex-1 bg-[var(--auth-line)]" />
      </div>
    </div>
  );
}
```

- [ ] **Step 6: 接进登录表单**

在 `scm-web/src/components/auth/login-form.tsx` 里:

(a) 顶部加 import:

```tsx
import { SocialAuthButtons } from "./social-auth-buttons";
```

(b) 在 `<form>` 内、用户名字段**之前**插入:

```tsx
        <SocialAuthButtons
          onProviderClick={(provider) => {
            const base = process.env.NEXT_PUBLIC_API_BASE_URL || "";
            window.location.href =
              `${base}/auth/api/auth/oauth2/${provider}/authorize` +
              `?locale=${encodeURIComponent(locale)}` +
              `&returnTo=${encodeURIComponent(returnTo)}`;
          }}
        />
```

`locale` 与 `returnTo` 的来源 —— 在 `LoginForm` 组件里取:

```tsx
const locale = useLocale();
const returnTo = usePathname();
```

> `useLocale()` 来自 `next-intl`,`usePathname()` 来自 `next/navigation`;两者都要在
> `LoginForm` 顶部 import。**不要**用 `window.location.pathname.split("/")` 解析 locale ——
> 那在 `next-intl` 的 locale 前缀路由下容易取错段。

(c) `messages/{zh-CN,en-US}.json` 的 `login` 命名空间补:

```json
    "social": {
      "or": "或",
      "google": "使用 Google 继续",
      "apple": "使用 Apple 继续"
    }
```
(en-US: `"or": "OR"`, `"google": "Continue with Google"`, `"apple": "Continue with Apple"`)

> 「使用 Google 继续」等文案在 zh-CN 里保留英文产品名,与参考图一致。

- [ ] **Step 7: 运行测试确认通过**

Run: `cd scm-web && npm test -- src/components/auth/social-auth-buttons.test.tsx`
Expected: PASS(2 个用例)

- [ ] **Step 8: 提交**

```bash
cd scm-web
git add src/lib/api/social-auth-client.ts src/lib/api/services/auth.ts \
        src/lib/api/contracts/auth.ts src/components/auth/social-auth-buttons.tsx \
        src/components/auth/social-auth-buttons.test.tsx src/components/auth/login-form.tsx \
        messages/zh-CN.json messages/en-US.json
git commit -m "feat(web): 登录卡片加入 Google/Apple 社交登录按钮"
```

---

### Task 7: 前端回调页与放行

**Files:**
- Create: `scm-web/src/app/[locale]/auth/callback/page.tsx`
- Create: `scm-web/src/components/auth/social-auth-callback.tsx`
- Create: `scm-web/src/components/auth/social-auth-callback.test.tsx`
- Modify: `scm-web/src/proxy.ts`

**Interfaces:**
- Consumes: Task 6 的 `authService.oauth2.exchange(ticket)`;前置计划的 `useCompleteLogin()`
- Produces: 路由 `/{locale}/auth/callback?ticket=...`

- [ ] **Step 1: 写失败的测试**

创建 `scm-web/src/components/auth/social-auth-callback.test.tsx`:

```tsx
import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const exchange = vi.fn();
const complete = vi.fn();

vi.mock("@/lib/api/services/auth", () => ({
  authService: { oauth2: { exchange: (...args: unknown[]) => exchange(...args) } },
}));

vi.mock("@/lib/auth/use-complete-login", () => ({
  useCompleteLogin: () => complete,
}));

describe("SocialAuthCallback", () => {
  beforeEach(() => {
    exchange.mockReset();
    complete.mockReset();
  });

  it("exchanges the ticket and establishes the session", async () => {
    exchange.mockResolvedValue({ accessToken: "at", userId: "u", username: "ann" });
    const { SocialAuthCallback } = await import("./social-auth-callback");
    render(<SocialAuthCallback ticket="ticket-1" />);

    await waitFor(() => expect(exchange).toHaveBeenCalledWith("ticket-1"));
    await waitFor(() => expect(complete).toHaveBeenCalled());
  });

  it("shows an error instead of silently redirecting when the ticket is rejected", async () => {
    exchange.mockRejectedValue(new Error("Invalid or expired ticket"));
    const { SocialAuthCallback } = await import("./social-auth-callback");
    render(<SocialAuthCallback ticket="bad" />);

    await waitFor(() => {
      expect(screen.getByRole("alert")).toBeInTheDocument();
    });
    expect(complete).not.toHaveBeenCalled();
  });
});
```

- [ ] **Step 2: 运行测试确认失败**

Run: `cd scm-web && npm test -- src/components/auth/social-auth-callback.test.tsx`
Expected: FAIL —— `Cannot find module './social-auth-callback'`

- [ ] **Step 3: 创建回调组件**

`scm-web/src/components/auth/social-auth-callback.tsx`:

```tsx
"use client";

import { useEffect, useState } from "react";
import { useTranslations } from "next-intl";

import { authService } from "@/lib/api/services/auth";
import { useCompleteLogin } from "@/lib/auth/use-complete-login";

interface SocialAuthCallbackProps {
  ticket?: string;
}

export function SocialAuthCallback({ ticket }: SocialAuthCallbackProps) {
  const t = useTranslations("login");
  const completeLogin = useCompleteLogin();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!ticket) {
      setError(t("social.missingTicket"));
      return;
    }
    let cancelled = false;
    authService.oauth2
      .exchange(ticket)
      .then((response) => {
        if (!cancelled) {
          void completeLogin(response);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError(t("social.invalidTicket"));
        }
      });
    return () => {
      cancelled = true;
    };
  }, [ticket, completeLogin, t]);

  if (error) {
    return (
      <div role="alert" className="rounded-2xl border border-[var(--auth-line)] bg-[var(--auth-surface)] p-8 text-center">
        <p className="text-sm text-[var(--auth-ink-soft)]">{error}</p>
        <a
          href="/"
          className="mt-4 inline-block rounded-lg bg-[var(--auth-btn)] px-4 py-2 text-sm font-medium text-[var(--auth-btn-fg)]"
        >
          {t("social.backToLogin")}
        </a>
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-[var(--auth-line)] bg-[var(--auth-surface)] p-8 text-center">
      <p className="text-sm text-[var(--auth-ink-soft)]">{t("social.signingIn")}</p>
    </div>
  );
}
```

- [ ] **Step 4: 创建回调路由页**

`scm-web/src/app/[locale]/auth/callback/page.tsx`:

```tsx
import { Suspense } from "react";

import { AuthShell } from "@/components/auth/auth-shell";
import { SocialAuthCallback } from "@/components/auth/social-auth-callback";

export default async function SocialAuthCallbackPage({
  searchParams,
}: {
  searchParams: Promise<{ ticket?: string }>;
}) {
  const { ticket } = await searchParams;

  return (
    <AuthShell>
      <Suspense fallback={null}>
        <SocialAuthCallback ticket={ticket} />
      </Suspense>
    </AuthShell>
  );
}
```

- [ ] **Step 5: 把回调路径加入 `proxy.ts` 的公开路径**

`scm-web/src/proxy.ts` 里:

```ts
const publicPaths = ['/login', '/register', '/forgot-password', '/auth/callback']
```

> 没有这一条,未持有 `access_token` cookie 的浏览器会被守卫重定向到 `/login`,
> 票据随之丢失 —— 而且 `proxy.ts` 的 matcher 会匹配 `/{locale}/auth/callback`。

- [ ] **Step 6: 补 i18n 文案**

`messages/{zh-CN,en-US}.json` 的 `login.social` 下追加:

```json
      "signingIn": "正在完成登录…",
      "missingTicket": "登录链接无效,缺少票据。",
      "invalidTicket": "登录链接已过期,请重新登录。",
      "backToLogin": "返回登录"
```

(en-US: `"signingIn": "Finishing sign-in…"`, `"missingTicket": "The sign-in link is missing its ticket."`,
`"invalidTicket": "The sign-in link has expired. Please sign in again."`, `"backToLogin": "Back to sign in"`)

- [ ] **Step 7: 运行测试确认通过**

Run: `cd scm-web && npm test -- src/components/auth/social-auth-callback.test.tsx`
Expected: PASS(2 个用例)

- [ ] **Step 8: 跑全量前端校验**

Run: `cd scm-web && npm test && npm run typecheck && npm run lint && npm run build`
Expected: 全部通过

- [ ] **Step 9: 提交**

```bash
cd scm-web
git add "src/app/[locale]/auth/callback/page.tsx" \
        src/components/auth/social-auth-callback.tsx \
        src/components/auth/social-auth-callback.test.tsx \
        src/proxy.ts messages/zh-CN.json messages/en-US.json
git commit -m "feat(web): 新增社交登录回调页与票据兑换"
```

---

## 完成定义

全部 7 个 Task 完成后,下列命令必须全绿:

```bash
mvn clean install -DskipTests -f com.scm.parent/pom.xml
mvn test -pl ../scm-auth,../scm-system/service,../scm-common/web -f com.scm.parent/pom.xml
cd scm-web && npm run typecheck && npm run lint && npm test && npm run build
```

且下列人工检查项成立:

- `GET /auth/api/auth/oauth2/providers` 只返回凭据齐全的 provider;全部未配置时返回空数组
- 没有 `X-Tenant-Id` 头时,`/authorize` 与 `/callback` 仍然可用(不被 `TenantFilter` 拒绝)
- provider 未配置时,登录页不渲染该按钮;provider 列表请求失败时登录表单照常可用
- 社交登录产出的令牌能被网关 `JwtAuthenticationFilter` 校验通过,`amr` 为 `["google"]`
- 停用/锁定账号、邮箱歧义、未开通账号都被拒绝,并给出可读提示
- 同一张票据第二次兑换失败
- 回调页在票据无效时**显示错误**,而不是静默跳回登录页
- 密码登录与 TOTP 流程无回归(前置计划的完成定义仍然成立)

## 部署前置(不在本计划范围)

以下由使用方在部署环境准备,代码侧不阻塞:

1. Google Cloud Console 创建 OAuth 2.0 客户端,登记回调地址
   `https://<网关域名>/auth/api/auth/oauth2/google/callback`,取得 client id / secret,
   以 `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` 注入
2. `SOCIAL_AUTH_ENABLED=true` 开启功能
3. `SOCIAL_AUTH_FRONTEND_BASE_URL` 指向前端站点(如 `https://scm.example.com`)
4. Apple 另需付费 Apple Developer 账号、Services ID(`APPLE_SERVICE_ID`)、
   以及用团队私钥 ES256 签名、有效期 ≤6 个月的 JWT(`APPLE_CLIENT_SECRET`);
   Apple 的 `response_mode=form_post` 回调路径与 Google 共用同一组端点
