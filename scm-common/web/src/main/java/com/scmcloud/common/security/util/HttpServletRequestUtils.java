package com.scmcloud.common.security.util;

import com.scmcloud.common.security.properties.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 缁熶竴澶勭悊 Token鎻愬彇鍜岃澶嘔D鐢熸垚
 *
 * @author Deng
 * createData 2025/10/20 16:23
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class HttpServletRequestUtils {
    private final JwtProperties jwtProperties;

    // 璁惧ID鐩稿叧甯搁噺
    private static final String DEVICE_ID_HEADER = "X-Device-ID";
    private static final int MAX_DEVICE_ID_LENGTH = 128;
    private static final String DEVICE_ID_PATTERN = "[^a-zA-Z0-9-_]";
    private static final String DEFAULT_USER_AGENT = "unknown";
    private static final String DEFAULT_IP = "0.0.0.0";

    // 璇锋眰绾х紦瀛榢ey
    private static final String CACHED_DEVICE_ID_ATTR = "cached.device.id";

    /**
     * 浠嶩TTP璇锋眰涓彁鍙朖WT token
     *
     * @param request HTTP璇锋眰瀵硅薄
     * @return JWT token锛屽鏋滀笉瀛樺湪鎴栨牸寮忛敊璇垯杩斿洖null
     */
    public String getTokenFromRequest(HttpServletRequest request) {
        try {
            // 鑾峰彇閰嶇疆鐨刪eader鍚嶇О鍜宲refix
            String headerName = jwtProperties.getHeader();
            String prefix = jwtProperties.getPrefix();

            // 楠岃瘉閰嶇疆鏄惁鏈夋晥
            if (headerName == null || prefix == null) {
                return null;
            }

            // 鑾峰彇header鍊煎苟trim
            String bearerToken = request.getHeader(headerName);
            if (!StringUtils.hasText(bearerToken)) {
                return null;
            }

            bearerToken = bearerToken.trim();

            // 楠岃瘉prefix骞舵彁鍙杢oken
            if (bearerToken.startsWith(prefix)) {
                // 楠岃瘉闀垮害鏄惁瓒冲
                if (bearerToken.length() <= prefix.length()) {
                    return null;
                }

                String token = bearerToken.substring(prefix.length()).trim();
                return StringUtils.hasText(token) ? token : null;
            }

            return null;
        } catch (Exception e) {
            // 璁板綍寮傚父浣嗕笉鍚戜笂鎶涘嚭锛岄伩鍏嶅奖鍝嶈璇佹祦锟?
            return null;
        }
    }

    /**
     * 鑾峰彇鎴栫敓鎴愯澶嘔D
     * <p>
     * 浼樺厛浣跨敤瀹㈡埛绔彁渚涚殑X-Device-ID header锛屽鏋滀笉瀛樺湪鍒欏熀浜嶶ser-Agent鍜孖P鐢熸垚
     * 浣跨敤璇锋眰绾х紦瀛橈紝閬垮厤鍦ㄥ悓涓€璇锋眰涓噸澶嶈锟?
     * </p>
     *
     * @param request HTTP璇锋眰瀵硅薄
     * @return 璁惧ID锛屼繚璇侀潪绌轰笖闀垮害涓嶈秴锟?8瀛楃
     */
    public String getDeviceId(HttpServletRequest request) {
        // 妫€鏌ヨ姹傜骇缂撳瓨
        Object cached = request.getAttribute(CACHED_DEVICE_ID_ATTR);
        if (cached instanceof String) {
            return (String) cached;
        }

        String deviceId = request.getHeader(DEVICE_ID_HEADER);

        if (StringUtils.hasText(deviceId)) {
            // 娓呯悊闈炴硶瀛楃锛堝彧淇濈暀瀛楁瘝銆佹暟瀛椼€佹í绾裤€佷笅鍒掔嚎锟?
            deviceId = deviceId.replaceAll(DEVICE_ID_PATTERN, "");

            // 濡傛灉娓呯悊鍚庝负绌猴紝鍒欓噸鏂扮敓锟?
            if (!StringUtils.hasText(deviceId)) {
                deviceId = generateDeviceId(request);
            } else if (deviceId.length() > MAX_DEVICE_ID_LENGTH) {
                // 闄愬埗闀垮害
                deviceId = deviceId.substring(0, MAX_DEVICE_ID_LENGTH);
            }
        } else {
            // 娌℃湁鎻愪緵璁惧ID锛屽熀浜庤姹備俊鎭敓锟?
            deviceId = generateDeviceId(request);
        }

        // 缂撳瓨鍒拌姹傚睘鎬т腑
        request.setAttribute(CACHED_DEVICE_ID_ATTR, deviceId);

        return deviceId;
    }

    /**
     * 鍩轰簬璇锋眰淇℃伅鐢熸垚璁惧ID
     * <p>
     * 浣跨敤User-Agent鍜孖P鍦板潃鐨勭粍鍚堢敓鎴怱HA256鍝堝笇浣滀负璁惧ID
     * </p>
     *
     * @param request HTTP璇锋眰瀵硅薄
     * @return 鐢熸垚鐨勮澶嘔D锛圫HA256鍝堝笇鍊硷級
     */
    private String generateDeviceId(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String ip = IpUtils.getClientIp(request);

        // 浣跨敤榛樿鍊煎鐞唍ull鎯呭喌
        String safeUserAgent = (userAgent != null && !userAgent.isEmpty()) ? userAgent : DEFAULT_USER_AGENT;
        String safeIp = (ip != null && !ip.isEmpty()) ? ip : DEFAULT_IP;

        // 鐢熸垚鍞竴鏍囪瘑
        String raw = safeUserAgent + "|" + safeIp;
        return DigestUtils.sha256Hex(raw);
    }
}
