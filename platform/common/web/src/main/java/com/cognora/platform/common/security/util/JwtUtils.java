package com.cognora.platform.common.security.util;

import com.cognora.platform.common.exception.UnauthorizedException;
import com.cognora.platform.common.redis.script.RedisLuaScriptLoader;
import com.cognora.platform.common.security.properties.JwtProperties;
import com.cognora.platform.common.util.UUIDv7Util;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Jwt 宸ュ叿锟?
 *
 * @author Deng
 * createData 2025/10/11 11:08
 * @version 1.0
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtUtils {
    private final JwtProperties jwtProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private SecretKey signingKey;

    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKENS_HASH = "jwt:user:tokens:";  // Hash key per user
    private static final String TOKEN_FINGERPRINT_PREFIX = "jwt:fingerprint:";
    private static final String REFRESH_LOCK_PREFIX = "jwt:refresh:lock:";

    /** Token 绑定的租户 ID，签发后不可跨租户使用。 */
    public static final String CLAIM_TENANT_ID = "tenantId";
    /** Token 绑定的租户编码，用于展示与排障，安全判定以 tenantId 为准。 */
    public static final String CLAIM_TENANT_CODE = "tenantCode";
    /** 令牌受众（JWT 标准 aud 声明），区分管理台与商城前台。 */
    public static final String CLAIM_AUDIENCE = "aud";

    /**
     * Token 与请求头租户的核对结果。
     */
    public enum TenantBinding {
        /** 租户声明存在且与请求头一致。 */
        VALID,
        /** Token 里没有 tenantId 声明（升级前的旧 token）。 */
        CLAIM_MISSING,
        /** 请求头缺租户，或与 Token 里的 tenantId 不一致。 */
        MISMATCH
    }

    // ThreadLocal cache for parsed tokens within same request
    private static final ThreadLocal<Map<String, Claims>> TOKEN_CACHE = ThreadLocal.withInitial(HashMap::new);

    // Lua script for atomic token metadata storage
    private static final RedisScript<Long> STORE_TOKEN_METADATA_SCRIPT =
            RedisLuaScriptLoader.load("lua/jwt/store_token_metadata.lua", Long.class);

    // Lua script for atomic blacklist addition
    private static final RedisScript<Long> ADD_TO_BLACKLIST_SCRIPT =
            RedisLuaScriptLoader.load("lua/jwt/add_to_blacklist.lua", Long.class);

    @PostConstruct
    public void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("jwt.secret must be configured via Nacos/ENV (JWT_SECRET)");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 512 bits, current: " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 鐢熸垚璁块棶浠ょ墝锛堝彲鎸囧畾 AMR锛屽苟缁戝畾绉熸埛锟?
     *
     * @param tenantId   Token 所属租户，必填；缺失时拒绝签发（不做无租户降级）
     * @param tenantCode 租户编码，用于展示/排障，可为空
     */
    public String generateAccessToken(UUID userId, String username, Set<String> roles, Set<String> permissions,
                                      String deviceId, String ipAddress, List<String> amr,
                                      UUID tenantId, String tenantCode) {
        return generateAccessToken(userId, username, roles, permissions, deviceId, ipAddress, amr,
                tenantId, tenantCode, null);
    }

    /**
     * 生成访问令牌，并写入受众声明。
     *
     * @param audience 令牌受众（{@code cognora-admin} / {@code cognora-mall}）；
     *                 为空表示未绑定受众，仅供升级前的兼容调用。
     */
    public String generateAccessToken(UUID userId, String username, Set<String> roles, Set<String> permissions,
                                      String deviceId, String ipAddress, List<String> amr,
                                      UUID tenantId, String tenantCode, String audience) {
        if (tenantId == null) {
            throw new UnauthorizedException("invalid_token_tenant_missing");
        }

        String jti = UUIDv7Util.generateString();
        String tokenType = "access";

        Map<String, Object> claims = buildClaims(userId, username, roles, permissions, tokenType, deviceId, ipAddress,
                jti, tenantId, tenantCode, audience
        );

        if (amr != null && !amr.isEmpty()) {
            claims.put("amr", amr);
        }

        String token = createToken(claims, userId.toString(), jwtProperties.getExpiration());

        // 瀛樺偍 Token鍏冩暟锟?
        storeTokenMetadata(userId, deviceId, token, jti, ipAddress, jwtProperties.getExpiration());

        return token;
    }

    /**
     * 鐢熸垚鍒锋柊浠ょ墝
     */
    public String generateRefreshToken(UUID userId, String username, String deviceId,
                                       UUID tenantId, String tenantCode) {
        return generateRefreshToken(userId, username, deviceId, tenantId, tenantCode, null);
    }

    /**
     * 生成刷新令牌，并携带受众，保证轮换后的新访问令牌仍属于同一入口。
     */
    public String generateRefreshToken(UUID userId, String username, String deviceId,
                                       UUID tenantId, String tenantCode, String audience) {
        if (tenantId == null) {
            throw new UnauthorizedException("invalid_token_tenant_missing");
        }
        String jti = UUIDv7Util.generateString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("tokenType", "refresh");
        claims.put("deviceId", deviceId);
        claims.put("jti", jti);
        claims.put(CLAIM_TENANT_ID, tenantId.toString());
        if (tenantCode != null && !tenantCode.isBlank()) {
            claims.put(CLAIM_TENANT_CODE, tenantCode);
        }
        if (audience != null && !audience.isBlank()) {
            claims.put(CLAIM_AUDIENCE, audience);
        }

        return createToken(claims, userId.toString(), jwtProperties.getRefreshExpiration());
    }

    /**
     * 楠岃瘉Token - 鎷嗗垎涓哄涓皬鏂规硶
     */
    public boolean validateToken(String token, String currentIp, String currentDeviceId) {
        // 1. 瑙f瀽Token
        Claims claims = parseToken(token);
        if (claims == null) {
            log.debug("Failed to parse token");
            return false;
        }

        // 2. 鍩虹楠岃瘉
        if (!validateBasicClaims(claims)) {
            return false;
        }

        // 3. 榛戝悕鍗曟锟?
        if (isTokenBlacklisted(getJti(claims))) {
            log.warn("Token is blacklisted");
            return false;
        }

        // 4. 璁惧楠岃瘉
        if (!validateDevice(claims, currentDeviceId)) {
            return false;
        }

        // 5. IP楠岃瘉锛堝彲閰嶇疆锟?
        if (jwtProperties.isStrictIpCheck() &&
                !validateIpAddress(claims, currentIp)) {
            return false;
        }

        // 6. 鎸囩汗楠岃瘉
        return validateFingerprint(claims);
    }

    /**
     * 妫€鏌ュ埛鏂颁护鐗屾槸鍚︽棤锟?
     *
     * @param token 鍒锋柊浠ょ墝
     * @return true 濡傛灉浠ょ墝鏃犳晥锛宖alse 濡傛灉浠ょ墝鏈夋晥
     */
    public boolean isRefreshTokenInvalid(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return true;
        }

        String tokenType = (String) claims.get("tokenType");
        Date expiration = claims.getExpiration();

        return !"refresh".equals(tokenType) || expiration == null || !expiration.after(new Date());
    }

    /**
     * 鍒锋柊Token - 娣诲姞骞跺彂鎺у埗
     */
    public String refreshToken(String refreshToken, Set<String> roles, Set<String> permissions, String deviceId,
                               String ipAddress) {
        if (isRefreshTokenInvalid(refreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        UUID userId = getUserIdFromToken(refreshToken);
        UUID tenantId = getTenantIdFromToken(refreshToken);
        if (tenantId == null) {
            // 升级前的 refresh token 没有租户声明：统一 401，不允许静默降级
            throw new UnauthorizedException("invalid_token_tenant_missing");
        }
        String tenantCode = getTenantCodeFromToken(refreshToken);
        String audience = getAudienceFromToken(refreshToken);
        String lockKey = REFRESH_LOCK_PREFIX + userId;

        try {
            // 鍒嗗竷寮忛攣锛岄槻姝㈠苟鍙戝埛锟?
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

            if (Boolean.FALSE.equals(acquired)) {
                throw new UnauthorizedException("Token refresh in progress");
            }

            String username = getUsernameFromToken(refreshToken);

            // 鎾ら攢鏃х殑璁块棶浠ょ墝
            revokeUserAccessTokens(userId, deviceId);

            // 鐢熸垚鏂扮殑璁块棶浠ょ墝
            return generateAccessToken(userId, username, roles, permissions, deviceId, ipAddress, null,
                    tenantId, tenantCode, audience);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * 鎾ら攢 Token
     * @throws UnauthorizedException if token is invalid or revocation fails
     */
    public void revokeToken(String token, String reason) {
        Claims claims = parseTokenOrThrow(token);

        String jti = getJti(claims);
        Date expiration = claims.getExpiration();
        UUID userId = UUID.fromString(claims.getSubject());
        String deviceId = (String) claims.get("deviceId");

        long ttl = expiration.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            try {
                // 鍔犲叆榛戝悕锟?
                addToBlacklist(jti, userId, reason, ttl);

                // 鍒犻櫎 Token缂撳瓨
                deleteTokenCache(userId, deviceId);

                // 鍒犻櫎鎸囩汗
                deleteFingerprint(jti);

                log.info("Token revoked: userId={}, reason={}", userId, reason);
            } catch (Exception e) {
                log.error("Failed to revoke token: userId={}, error={}", userId, e.getMessage());
                throw new UnauthorizedException("Token revocation failed: " + e.getMessage());
            }
        }
    }

    /**
     * Revokes all tokens for a given user across all devices.
     * PERFORMANCE: Uses Redis Hash instead of KEYS command for O(1) lookup.
     * Each user has a hash: jwt:user:tokens:{userId} -> { deviceId: token }
     */
    public void revokeAllUserTokens(UUID userId) {
        String hashKey = USER_TOKENS_HASH + userId;

        // Get all device tokens for this user (O(N) where N = devices per user, typically < 10)
        // 令牌哈希由 Lua 脚本经 StringRedisTemplate 以原始字符串写入，必须用同一个模板读取，
        // 否则 JSON 序列化器会把 JWT 文本当成 JSON 解析而失败。
        Map<String, String> deviceSessions = stringRedisTemplate.<String, String>opsForHash().entries(hashKey);

        if (deviceSessions != null && !deviceSessions.isEmpty()) {
            log.info("Revoking {} token(s) for user {}", deviceSessions.size(), userId);

            for (Map.Entry<String, String> entry : deviceSessions.entrySet()) {
                String deviceId = entry.getKey();
                String token = entry.getValue();

                if (token != null) {
                    try {
                        revokeToken(token, "Admin forced logout");
                        log.debug("Revoked session for user {} device {}", userId, maskIdentifier(deviceId));
                    } catch (Exception e) {
                        log.error("Failed to revoke session for user {} device {}: {}",
                                 userId, maskIdentifier(deviceId), e.getMessage());
                    }
                }
            }

            // Clean up the hash
            redisTemplate.delete(hashKey);
        } else {
            log.debug("No active tokens found for user {}", userId);
        }
    }

    /**
     * 列出某个用户当前有效的访问令牌会话。
     *
     * <p>数据源是 {@code jwt:user:tokens:{userId}}（deviceId → token）与
     * {@code jwt:fingerprint:{jti}}（设备/IP/签发时间）。已过期、已加入黑名单
     * 或缺少指纹的令牌会被过滤。最近活跃时间取令牌最近一次签发/刷新时间：
     * 刷新流程会撤销旧访问令牌并写入新的指纹。结果按最近活跃时间倒序。</p>
     */
    public List<TokenSession> listActiveSessions(UUID userId) {
        Map<String, String> deviceTokens = stringRedisTemplate.<String, String>opsForHash()
                .entries(USER_TOKENS_HASH + userId);
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return Collections.emptyList();
        }

        long now = System.currentTimeMillis();
        List<TokenSession> sessions = new ArrayList<>(deviceTokens.size());
        for (Map.Entry<String, String> entry : deviceTokens.entrySet()) {
            String deviceId = entry.getKey();
            String token = entry.getValue();
            if (deviceId == null || token == null) {
                continue;
            }
            Claims claims = parseToken(token);
            if (claims == null || claims.getExpiration() == null) {
                continue;
            }
            long expiresAt = claims.getExpiration().getTime();
            if (expiresAt <= now) {
                continue;
            }
            String jti = getJti(claims);
            if (jti == null || isTokenBlacklisted(jti)) {
                continue;
            }
            Map<String, String> fingerprint = stringRedisTemplate.<String, String>opsForHash()
                    .entries(TOKEN_FINGERPRINT_PREFIX + jti);
            if (fingerprint == null || fingerprint.isEmpty()) {
                continue;
            }
            long issuedAt = parseEpochMillis(fingerprint.get("issueTime"), claims.getIssuedAt());
            String ipAddress = fingerprint.get("ipAddress");
            sessions.add(new TokenSession(deviceId, ipAddress,
                    Instant.ofEpochMilli(issuedAt), Instant.ofEpochMilli(expiresAt)));
        }

        sessions.sort((left, right) -> right.getIssuedAt().compareTo(left.getIssuedAt()));
        return sessions;
    }

    /**
     * 撤销某个用户除当前 deviceId 以外的全部访问令牌。
     *
     * <p>{@code currentDeviceId} 必须来自当前访问令牌的 {@code deviceId} 声明，
     * 不能由调用方传入任意值，否则等同于「强制其他设备下线」。撤销失败时抛出
     * {@link UnauthorizedException}（fail-closed），避免静默留下有效会话。</p>
     *
     * @return 实际撤销的会话数量
     */
    public int revokeOtherDeviceTokens(UUID userId, String currentDeviceId, String reason) {
        Map<String, String> deviceTokens = stringRedisTemplate.<String, String>opsForHash()
                .entries(USER_TOKENS_HASH + userId);
        if (deviceTokens == null || deviceTokens.isEmpty()) {
            return 0;
        }

        int revoked = 0;
        int failed = 0;
        for (Map.Entry<String, String> entry : deviceTokens.entrySet()) {
            String deviceId = entry.getKey();
            String token = entry.getValue();
            if (currentDeviceId != null && currentDeviceId.equals(deviceId)) {
                continue;
            }
            if (token == null) {
                continue;
            }
            try {
                revokeToken(token, reason);
                revoked++;
            } catch (Exception e) {
                failed++;
                log.error("Failed to revoke other session for user {} device {}: {}",
                        userId, maskIdentifier(deviceId), e.getMessage());
            }
        }
        if (failed > 0) {
            throw new UnauthorizedException("Failed to revoke " + failed + " other session(s)");
        }
        return revoked;
    }

    private long parseEpochMillis(String value, Date fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return fallback == null ? 0L : fallback.getTime();
        }
    }

    /**
     * 会话快照，供 {@code GET /api/auth/sessions} 使用。
     */
    @Data
    @AllArgsConstructor
    public static class TokenSession {
        private String deviceId;
        private String ipAddress;
        private Instant issuedAt;
        private Instant expiresAt;
    }

    /**
     * 锟絋oken涓彁鍙栬锟?
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return Collections.emptySet();
        List<String> roleList = (List<String>) claims.get("roles");

        return roleList != null ? new HashSet<>(roleList) : Collections.emptySet();
    }

    /**
     * 锟絋oken涓彁鍙栨潈锟?
     */
    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return Collections.emptySet();
        List<String> permList = (List<String>) claims.get("permissions");

        return permList != null ? new HashSet<>(permList) : Collections.emptySet();
    }


    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new UnauthorizedException("Invalid token");
        }
        Object userId = claims.get("userId");
        if (userId == null) {
            throw new UnauthorizedException("Token missing userId");
        }

        return userId instanceof UUID ? (UUID) userId : UUID.fromString(userId.toString());
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new UnauthorizedException("Invalid token");
        }

        return (String) claims.get("username");
    }

    /**
     * 读取 Token 绑定的租户 ID；旧 Token（无该声明）返回 {@code null}。
     */
    public UUID getTenantIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object tenantId = claims.get(CLAIM_TENANT_ID);
        if (tenantId == null || tenantId.toString().isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(tenantId.toString());
        } catch (IllegalArgumentException e) {
            log.warn("Token contains malformed {} claim", CLAIM_TENANT_ID);
            return null;
        }
    }

    /**
     * 读取 Token 绑定的租户编码；缺失时返回 {@code null}（安全判定不依赖它）。
     */
    public String getTenantCodeFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object tenantCode = claims.get(CLAIM_TENANT_CODE);
        return tenantCode == null ? null : tenantCode.toString();
    }

    /**
     * 读取令牌受众（{@code aud}）。旧 Token（无该声明）返回 {@code null}；
     * 标准允许 {@code aud} 为字符串或字符串数组，这里取第一个值。
     */
    public String getAudienceFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        return extractAudience(claims.get(CLAIM_AUDIENCE));
    }

    private static String extractAudience(Object audience) {
        if (audience instanceof Collection<?> values) {
            for (Object value : values) {
                if (value != null && !value.toString().isBlank()) {
                    return value.toString();
                }
            }
            return null;
        }
        return audience == null || audience.toString().isBlank() ? null : audience.toString();
    }

    /**
     * 核对 Token 租户声明与请求头租户。
     *
     * <p>旧 Token（无 tenantId 声明）返回 {@link TenantBinding#CLAIM_MISSING}，调用方必须 401，
     * 不做“使用请求头租户补齐”的静默降级。</p>
     */
    public TenantBinding validateTenantBinding(String token, String requestTenantId) {
        UUID claimTenantId = getTenantIdFromToken(token);
        if (claimTenantId == null) {
            return TenantBinding.CLAIM_MISSING;
        }
        if (requestTenantId == null || requestTenantId.isBlank()) {
            return TenantBinding.MISMATCH;
        }
        return claimTenantId.toString().equalsIgnoreCase(requestTenantId)
                ? TenantBinding.VALID : TenantBinding.MISMATCH;
    }

    /**
     * 浠嶵oken涓彁锟紸MR锛堣璇佹柟娉曞紩鐢級
     */
    @SuppressWarnings("unchecked")
    public Set<String> getAmrFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return Collections.emptySet();
        List<String> amr = (List<String>) claims.get("amr");

        return amr != null ? new HashSet<>(amr) : Collections.emptySet();
    }

    // ==================== 绉佹湁鏂规硶 ====================

    /**
     * 娓呯悊ThreadLocal缂撳瓨锛堝簲鍦ㄨ姹傜粨鏉熸椂璋冪敤锟?
     */
    public static void clearTokenCache() {
        TOKEN_CACHE.remove();
    }

    /**
     * 瑙f瀽Token锛堜笉鎶涘嚭寮傚父锛屽甫缂撳瓨锟?
     * @return Claims or null if parsing fails
     */
    private Claims parseToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        // Check cache first
        Map<String, Claims> cache = TOKEN_CACHE.get();
        Claims cached = cache.get(token);
        if (cached != null) {
            return cached;
        }

        // Parse and cache
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            cache.put(token, claims);
            return claims;
        } catch (Exception e) {
            log.debug("Failed to parse token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 瑙f瀽Token锛堟姏鍑哄紓甯革級
     * @throws UnauthorizedException if parsing fails
     */
    private Claims parseTokenOrThrow(String token) {
        if (token == null || token.isEmpty()) {
            throw new UnauthorizedException("Token is empty");
        }
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException("Token expired");
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid token: " + e.getMessage());
        }
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        Date expiryDate = new Date(nowMillis + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .issuer(jwtProperties.getIssuer())
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    private Map<String, Object> buildClaims(UUID userId, String username, Set<String> roles, Set<String> permissions,
                                            String tokenType, String deviceId, String ipAddress, String jti,
                                            UUID tenantId, String tenantCode, String audience) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("tokenType", tokenType);
        claims.put("deviceId", deviceId);
        claims.put("ipAddress", ipAddress);
        claims.put("jti", jti);
        claims.put(CLAIM_TENANT_ID, tenantId.toString());
        if (tenantCode != null && !tenantCode.isBlank()) {
            claims.put(CLAIM_TENANT_CODE, tenantCode);
        }
        if (audience != null && !audience.isBlank()) {
            claims.put(CLAIM_AUDIENCE, audience);
        }

        return claims;
    }

    private boolean validateBasicClaims(Claims claims) {
        String tokenType = (String) claims.get("tokenType");
        Date expiration = claims.getExpiration();

        if (!"access".equals(tokenType)) {
            log.warn("Invalid token type: {}", tokenType);
            return false;
        }

        if (expiration.before(new Date())) {
            log.debug("Token expired");
            return false;
        }

        return true;
    }

    private boolean validateDevice(Claims claims, String currentDeviceId) {
        String claimDeviceId = (String) claims.get("deviceId");
        if (claimDeviceId != null && !claimDeviceId.equals(currentDeviceId)) {
            log.warn("Device mismatch: expected={}, actual={}",
                    maskIdentifier(claimDeviceId), maskIdentifier(currentDeviceId));
            return false;
        }
        return true;
    }

    private boolean validateIpAddress(Claims claims, String currentIp) {
        String claimIp = (String) claims.get("ipAddress");
        if (claimIp != null && !claimIp.equals(currentIp)) {
            log.warn("IP changed: {} -> {}", (maskIdentifier(claimIp) + "").replace("\r", "_").replace("\n", "_"), (maskIdentifier(currentIp) + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }
        return true;
    }

    private boolean validateFingerprint(Claims claims) {
        String jti = getJti(claims);
        String fingerprintKey = TOKEN_FINGERPRINT_PREFIX + jti;

        return redisTemplate.hasKey(fingerprintKey);
    }

    /**
     * Stores token metadata in Redis using Hash structure for efficient lookups.
     * PERFORMANCE OPTIMIZATION:
     * - User tokens stored in Hash: jwt:user:tokens:{userId} -> {deviceId: token}
     * - Allows O(1) lookup and O(N) revocation where N = devices (typically < 10)
     * - Avoids O(N) KEYS scan where N = total tokens in Redis
     * - Uses Lua script to ensure atomic execution of HSET + EXPIRE operations
     */
    private void storeTokenMetadata(UUID userId, String deviceId, String token, String jti, String ipAddress,
                                    long ttl) {
        String userTokensHash = USER_TOKENS_HASH + userId;
        String fingerprintKey = TOKEN_FINGERPRINT_PREFIX + jti;
        long ttlSeconds = ttl / 1000;

        // Execute Lua script for atomic operations
        stringRedisTemplate.execute(
                STORE_TOKEN_METADATA_SCRIPT,
                List.of(userTokensHash, fingerprintKey),
                deviceId, token, String.valueOf(ttlSeconds),
                userId.toString(), ipAddress, String.valueOf(System.currentTimeMillis())
        );
    }

    private void addToBlacklist(String jti, UUID userId, String reason, long ttl) {
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;
        long ttlSeconds = ttl / 1000;

        // Execute Lua script for atomic operations
        stringRedisTemplate.execute(
                ADD_TO_BLACKLIST_SCRIPT,
                List.of(blacklistKey),
                String.valueOf(System.currentTimeMillis()),
                reason,
                userId.toString(),
                String.valueOf(ttlSeconds)
        );
    }


    private void deleteTokenCache(UUID userId, String deviceId) {
        String userTokensHash = USER_TOKENS_HASH + userId;
        redisTemplate.opsForHash().delete(userTokensHash, deviceId);
        log.debug("Deleted token cache for user {} device {}", userId, deviceId);
    }

    private void deleteFingerprint(String jti) {
        String fingerprintKey = TOKEN_FINGERPRINT_PREFIX + jti;
        redisTemplate.delete(fingerprintKey);
        log.debug("Deleted fingerprint for jti {}", jti);
    }

    private boolean isTokenBlacklisted(String jti) {
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + jti;

        return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
    }

    /**
     * Revokes access tokens for a specific user device.
     * Uses Hash lookup (HGET) instead of key lookup.
     */
    private void revokeUserAccessTokens(UUID userId, String deviceId) {
        String userTokensHash = USER_TOKENS_HASH + userId;
        String oldToken = stringRedisTemplate.<String, String>opsForHash().get(userTokensHash, deviceId);
        if (oldToken != null) {
            revokeToken(oldToken, "Token refreshed");
            log.debug("Revoked old token for user {} device {}", (userId + "").replace("\r", "_").replace("\n", "_"), (deviceId + "").replace("\r", "_").replace("\n", "_"));
        }
    }

    public String getDeviceIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            throw new UnauthorizedException("Invalid token");
        }
        return (String) claims.get("deviceId");
    }

    private String getJti(Claims claims) {
        return (String) claims.get("jti");
    }

    /**
     * Token pair containing access token and refresh token
     */
    @Data
    @AllArgsConstructor
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
    }

    /**
     * 鍒锋柊Token骞惰疆鎹㈠埛鏂颁护鐗岋紙鎺ㄨ崘浣跨敤锟?
     * 瀹炵幇鍒锋柊浠ょ墝杞崲鏈哄埗锛屾瘡娆″埛鏂版椂鐢熸垚鏂扮殑璁块棶浠ょ墝鍜屽埛鏂颁护锟?
     *
     * @return TokenPair containing new access token and new refresh token
     */
    public TokenPair refreshTokenWithRotation(String oldRefreshToken, Set<String> roles, Set<String> permissions,
                                              String deviceId, String ipAddress) {
        if (isRefreshTokenInvalid(oldRefreshToken)) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        UUID userId = getUserIdFromToken(oldRefreshToken);
        String username = getUsernameFromToken(oldRefreshToken);
        UUID tenantId = getTenantIdFromToken(oldRefreshToken);
        if (tenantId == null) {
            throw new UnauthorizedException("invalid_token_tenant_missing");
        }
        String tenantCode = getTenantCodeFromToken(oldRefreshToken);
        String audience = getAudienceFromToken(oldRefreshToken);
        String lockKey = REFRESH_LOCK_PREFIX + userId;

        try {
            // 鍒嗗竷寮忛攣锛岄槻姝㈠苟鍙戝埛锟?
            Boolean acquired = redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

            if (Boolean.FALSE.equals(acquired)) {
                throw new UnauthorizedException("Token refresh in progress");
            }

            // 鎾ら攢鏃х殑鍒锋柊浠ょ墝
            revokeToken(oldRefreshToken, "Refresh token rotated");

            // 鎾ら攢鏃х殑璁块棶浠ょ墝
            revokeUserAccessTokens(userId, deviceId);

            // 鐢熸垚鏂扮殑璁块棶浠ょ墝鍜屽埛鏂颁护锟?
            String newAccessToken = generateAccessToken(userId, username, roles, permissions, deviceId, ipAddress,
                    null, tenantId, tenantCode, audience);
            String newRefreshToken = generateRefreshToken(userId, username, deviceId, tenantId, tenantCode, audience);

            return new TokenPair(newAccessToken, newRefreshToken);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * Keeps a short prefix so operators can still correlate logs, without writing the full
     * device identifier or client IP to disk.
     */
    private static String maskIdentifier(String value) {
        if (value == null || value.isEmpty()) {
            return "***";
        }
        return value.length() <= 4 ? "***" : value.substring(0, 4) + "***";
    }
}
