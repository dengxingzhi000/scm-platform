package com.scmcloud.common.security.util;

import com.scmcloud.common.exception.UnauthorizedException;
import com.scmcloud.common.redis.script.RedisLuaScriptLoader;
import com.scmcloud.common.security.properties.JwtProperties;
import com.scmcloud.common.util.UUIDv7Util;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

    private SecretKey signingKey;

    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKENS_HASH = "jwt:user:tokens:";  // Hash key per user
    private static final String TOKEN_FINGERPRINT_PREFIX = "jwt:fingerprint:";
    private static final String REFRESH_LOCK_PREFIX = "jwt:refresh:lock:";

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
     * 鐢熸垚璁块棶浠ょ墝
     */
    public String generateAccessToken(UUID userId, String username, Set<String> roles, Set<String> permissions,
                                      String deviceId, String ipAddress) {
        return generateAccessToken(userId, username, roles, permissions, deviceId, ipAddress, null);
    }

    /**
     * 鐢熸垚璁块棶浠ょ墝锛堝彲鎸囧畾 AMR锟?
     */
    public String generateAccessToken(UUID userId, String username, Set<String> roles, Set<String> permissions,
                                      String deviceId, String ipAddress, List<String> amr) {
        String jti = UUIDv7Util.generateString();
        String tokenType = "access";

        Map<String, Object> claims = buildClaims(userId, username, roles, permissions, tokenType, deviceId, ipAddress,
                jti
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
    public String generateRefreshToken(UUID userId, String username, String deviceId) {
        String jti = UUIDv7Util.generateString();

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("tokenType", "refresh");
        claims.put("deviceId", deviceId);
        claims.put("jti", jti);

        return createToken(claims, userId.toString(), jwtProperties.getRefreshExpiration());
    }

    /**
     * 楠岃瘉Token - 鎷嗗垎涓哄涓皬鏂规硶
     */
    public boolean validateToken(String token, String currentIp, String currentDeviceId) {
        // 1. 瑙ｆ瀽Token
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
            return generateAccessToken(userId, username, roles, permissions, deviceId, ipAddress);
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
        Map<Object, Object> deviceTokens = redisTemplate.opsForHash().entries(hashKey);

        if (deviceTokens != null && !deviceTokens.isEmpty()) {
            log.info("Revoking {} token(s) for user {}", deviceTokens.size(), userId);

            for (Map.Entry<Object, Object> entry : deviceTokens.entrySet()) {
                String deviceId = (String) entry.getKey();
                String token = (String) entry.getValue();

                if (token != null) {
                    try {
                        revokeToken(token, "Admin forced logout");
                        log.debug("Revoked token for user {} device {}", userId, deviceId);
                    } catch (Exception e) {
                        log.error("Failed to revoke token for user {} device {}: {}",
                                 userId, deviceId, e.getMessage());
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
     * 瑙ｆ瀽Token锛堜笉鎶涘嚭寮傚父锛屽甫缂撳瓨锟?
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
     * 瑙ｆ瀽Token锛堟姏鍑哄紓甯革級
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
                                            String tokenType, String deviceId, String ipAddress, String jti) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("roles", roles);
        claims.put("permissions", permissions);
        claims.put("tokenType", tokenType);
        claims.put("deviceId", deviceId);
        claims.put("ipAddress", ipAddress);
        claims.put("jti", jti);

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
        String tokenDeviceId = (String) claims.get("deviceId");
        if (tokenDeviceId != null && !tokenDeviceId.equals(currentDeviceId)) {
            log.warn("Device mismatch: expected={}, actual={}",
                    tokenDeviceId, currentDeviceId);
            return false;
        }
        return true;
    }

    private boolean validateIpAddress(Claims claims, String currentIp) {
        String tokenIp = (String) claims.get("ipAddress");
        if (tokenIp != null && !tokenIp.equals(currentIp)) {
            log.warn("IP changed: {} -> {}", tokenIp, currentIp);
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
        redisTemplate.execute(
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
        redisTemplate.execute(
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
        String oldToken = (String) redisTemplate.opsForHash().get(userTokensHash, deviceId);
        if (oldToken != null) {
            revokeToken(oldToken, "Token refreshed");
            log.debug("Revoked old token for user {} device {}", userId, deviceId);
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
            String newAccessToken = generateAccessToken(userId, username, roles, permissions, deviceId, ipAddress);
            String newRefreshToken = generateRefreshToken(userId, username, deviceId);

            return new TokenPair(newAccessToken, newRefreshToken);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
