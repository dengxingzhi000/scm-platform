package com.scmcloud.common.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Jwt 閰嶇疆锟?
 *
 * @author Deng
 * createData 2025/10/11 11:05
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "jwt")
@Data
public class JwtProperties {

    /**
     * JWT绛惧悕瀵嗛挜锛堣嚦锟?2浣嶏級
     */
    private String secret;

    /**
     * Access Token杩囨湡鏃堕棿锛堟绉掞級
     * 榛樿: 2灏忔椂
     */
    private Long expiration = 7200000L;

    /**
     * Refresh Token杩囨湡鏃堕棿锛堟绉掞級
     * 榛樿: 7锟?
     */
    private Long refreshExpiration = 604800000L;

    /**
     * Token 绛惧彂锟?
     */
    private String issuer = "nearsync-auth-service";

    /**
     * Token 璇锋眰澶村悕锟?
     */
    private String header = "Authorization";

    /**
     * Token 鍓嶇紑
     */
    private String prefix = "Bearer ";

    /**
     * 鏄惁鍚敤涓ユ牸鐨処P妫€锟?
     * true: Token鍙兘鍦ㄧ鍙戠殑IP涓婁娇锟?
     * false: 鍏佽IP鍙樻洿
     */
    private boolean strictIpCheck = false;

    /**
     * 鏄惁鍚敤璁惧鎸囩汗楠岃瘉
     */
    private boolean deviceFingerprintEnabled = true;

    /**
     * Token鑷姩缁湡闃堝€硷紙姣锟?
     * 褰揟oken鍓╀綑鏃堕棿灏忎簬姝ゅ€兼椂锛岃嚜鍔ㄧ画锟?
     * 榛樿: 30鍒嗛挓
     */
    private Long autoRenewThreshold = 1800000L;

    /**
     * 鏄惁鍚敤 Token鑷姩缁湡
     */
    private boolean autoRenewEnabled = false;

    /**
     * 鍚屼竴鐢ㄦ埛鏈€澶у苟鍙戜細璇濇暟
     * 0琛ㄧず涓嶉檺锟?
     */
    private Integer maxConcurrentSessions = 0;

    /**
     * 浼氳瘽浜掓枼绛栫暐
     * ALLOW_ALL: 鍏佽鎵€鏈変細锟?
     * REPLACE_OLD: 鏂颁細璇濇浛鎹㈡棫浼氳瘽
     * REJECT_NEW: 鎷掔粷鏂颁細锟?
     */
    private SessionPolicy sessionPolicy = SessionPolicy.ALLOW_ALL;

    /**
     * Token榛戝悕鍗曟竻鐞嗙瓥锟?
     * LAZY: 鎳掓竻鐞嗭紙杩囨湡鏃惰嚜鍔ㄥ垹闄わ級
     * SCHEDULED: 瀹氭椂娓呯悊
     */
    private CleanupStrategy blacklistCleanupStrategy = CleanupStrategy.LAZY;

    /**
     * 榛戝悕鍗曞畾鏃舵竻鐞嗛棿闅旓紙灏忔椂锟?
     */
    private Integer blacklistCleanupInterval = 24;

    public enum SessionPolicy {
        ALLOW_ALL,
        REPLACE_OLD,
        REJECT_NEW
    }

    public enum CleanupStrategy {
        LAZY,
        SCHEDULED
    }
}