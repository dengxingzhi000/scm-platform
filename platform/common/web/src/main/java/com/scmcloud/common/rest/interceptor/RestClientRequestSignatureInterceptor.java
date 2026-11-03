package com.scmcloud.common.rest.interceptor;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RestClient 璇锋眰绛惧悕鎷︽埅锟?
 * <p>鏇夸唬 OpenFeign 锟紽eignRequestSignatureInterceptor</p>
 *
 * <p>鍔熻兘锟?
 * <ul>
 *   <li>鑷姩涓烘墍锟紿TTP 璇锋眰娣诲姞 HMAC-SHA256 绛惧悕</li>
 *   <li>闃查噸鏀炬敾鍑伙細浣跨敤鏃堕棿锟? nonce</li>
 *   <li>鍙傛暟鎺掑簭锛氱'淇濈鍚嶄竴鑷达拷/li>
 *   <li>鏈嶅姟闂磋璇侊細鍩轰簬 App-ID 锟絊ecret-Key</li>
 * </ul>
 *
 * <p>绛惧悕鏍煎紡锟?
 * <pre>
 * signature = HMAC-SHA256(secretKey, timestamp + nonce + appId + uri + sortedParams)
 * </pre>
 *
 * <p>HTTP Headers锟?
 * <pre>
 * X-Timestamp: 1640995200000
 * X-Nonce: 550e8400e29b41d4a716446655440000
 * X-Signature: a1b2c3d4e5f6...
 * X-App-Id: internal-service
 * </pre>
 *
 * @author Claude
 * @since 2025-12-29
 */
@Component
@Slf4j
public class RestClientRequestSignatureInterceptor implements ClientHttpRequestInterceptor {

    @Value("${security.feign.app-id:${API_SECRET_INTERNAL_SERVICE:internal-service}}")
    private String appId;

    @Value("${security.feign.secret-key:${API_SECRET_INTERNAL_SECRET:your-internal-secret-key}}")
    private String secretKey;

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // 鐢熸垚鏃堕棿鎴冲拰 nonce
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUIDv7Util.generate().toString().replace("-", "");

        // 璁$畻绛惧悕
        String signature = calculateSignature(request, timestamp, nonce);

        // 娣诲姞绛惧悕 Header
        request.getHeaders().set("X-Timestamp", timestamp);
        request.getHeaders().set("X-Nonce", nonce);
        request.getHeaders().set("X-Signature", signature);
        request.getHeaders().set("X-App-Id", appId);

        if (log.isDebugEnabled()) {
            log.debug("RestClient request signed: {} {}, signature: {}",
                      request.getMethod(), request.getURI(), signature);
        }

        // 缁х画鎵ц璇锋眰
        return execution.execute(request, body);
    }

    /**
     * 璁$畻璇锋眰绛惧悕
     *
     * <p>绛惧悕绠楁硶锟?
     * <pre>
     * 1. 鎻愬彇 URI 璺緞锛堜笉鍚煙鍚嶅拰绔彛锟?
     * 2. 鎻愬彇鏌ヨ鍙傛暟骞舵寜 key 鎺掑簭
     * 3. 鎷兼帴绛惧悕鍐呭锛歵imestamp + nonce + appId + uri + sortedParams
     * 4. 浣跨敤 HMAC-SHA256 璁$畻绛惧悕
     * </pre>
     *
     * @param request   HTTP 璇锋眰
     * @param timestamp 鏃堕棿锟?
     * @param nonce     闅忔満锟?
     * @return 绛惧悕瀛楃涓诧紙鍗佸叚杩涘埗锟?
     */
    private String calculateSignature(HttpRequest request, String timestamp, String nonce) {
        // 鑾峰彇 URI 璺緞
        String uri = request.getURI().getPath();

        // 鑾峰彇鏌ヨ鍙傛暟骞舵帓锟?
        Map<String, String> params = new HashMap<>();
        String query = request.getURI().getQuery();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=", 2);
                String key = kv[0];
                String value = kv.length > 1 ? kv[1] : "";
                params.put(key, value);
            }
        }

        // 鎺掑簭骞舵嫾鎺ュ弬锟?
        String sortedParams = sortAndConcatParams(params);

        // 鏋勫缓绛惧悕鍐呭
        String signContent = timestamp + nonce + appId + uri + sortedParams;

        if (log.isTraceEnabled()) {
            log.trace("Signature content: {}", signContent);
        }

        // 璁$畻 HMAC-SHA256 绛惧悕
        return SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8))
            .digestHex(signContent);
    }

    /**
     * 瀵瑰弬鏁拌繘琛屾帓搴忓苟鎷兼帴
     *
     * <p>鏍煎紡锛歬ey1=value1&key2=value2&...锛堟寜 key 瀛楀吀搴忔帓搴忥級</p>
     *
     * @param params 鍙傛暟 Map
     * @return 鎺掑簭鍚庣殑鍙傛暟瀛楃锟?
     */
    private String sortAndConcatParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        return params.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("&"));
    }
}
