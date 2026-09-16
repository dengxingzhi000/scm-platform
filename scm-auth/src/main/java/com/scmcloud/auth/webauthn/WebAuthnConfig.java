package com.scmcloud.auth.webauthn;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WebAuthn 閰嶇疆
 *
 * @author Deng
 * @since 2025-12-15
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "webauthn.rp")
public class WebAuthnConfig {

    /**
     * Relying Party ID (閫氬父鏄煙锟?
     */
    private String id = "localhost";

    /**
     * Relying Party Name (鏄剧ず鍚嶇О)
     */
    private String name = "CommonPermissionsFramework";

    /**
     * Relying Party Origin (瀹屾暣锟給rigin URL)
     */
    private String origin = "https://localhost";

    /**
     * 鏄惁瑕佹眰鐢ㄦ埛楠岃瘉
     */
    private boolean userVerificationRequired = false;

    /**
     * 鏀寔鐨勮璇佸櫒闄勪欢绫诲瀷
     * platform - 浠呭钩鍙拌璇佸櫒 (锟絋ouchID, FaceID)
     * cross-platform - 浠呰法骞冲彴璁よ瘉锟?锟結ubiKey)
     * 锟? 涓よ€呴兘鏀寔
     */
    private String authenticatorAttachment;

    /**
     * 鏀寔锟絩esident key 瑕佹眰
     * required, preferred, discouraged
     */
    private String residentKey = "preferred";

    // ==================== 瓒呮椂涓庣敓鍛藉懆鏈熼厤锟?===================

    /**
     * 璁よ瘉鎸戞垬杩囨湡鏃堕棿锛堢锟?
     * 榛樿 120 绉掞紙2 鍒嗛挓锟?
     */
    private long challengeExpirySeconds = 120L;

    /**
     * 娉ㄥ唽鎸戞垬杩囨湡鏃堕棿锛堢锟?
     * 榛樿 300 绉掞紙5 鍒嗛挓锛夛紝娉ㄥ唽娴佺▼閫氬父闇€瑕佹洿闀挎椂锟?
     */
    private long registrationChallengeExpirySeconds = 300L;

    /**
     * 鍑瘉涓嶆椿璺冮槇鍊硷紙澶╋級
     * 瓒呰繃姝ゅぉ鏁版湭浣跨敤鐨勫嚟璇佸皢琚爣璁颁负涓嶅仴锟?
     * 榛樿 90 锟?
     */
    private long credentialInactiveDays = 90L;

    /**
     * 璁よ瘉灏濊瘯鏃ュ織淇濈暀鏃堕棿锛堝ぉ锟?
     * 榛樿 30 锟?
     */
    private long authAttemptRetentionDays = 30L;
}
