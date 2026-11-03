package com.cognora.platform.common.rest.interceptor;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.cognora.platform.common.rest.signature.InternalSignatureSecretResolver;
import com.cognora.platform.common.util.UUIDv7Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RestClient 鐠囬攱鐪扮粵鎯ф倳閹凤附鍩呴敓?
 * <p>閺囧じ鍞?OpenFeign 閿熺唇eignRequestSignatureInterceptor</p>
 *
 * <p>閸旂喕鍏橀敓?
 * <ul>
 *   <li>閼奉亜濮╂稉鐑樺閿熺纯TTP 鐠囬攱鐪板ǎ璇插 HMAC-SHA256 缁涙儳鎮?/li>
 *   <li>闂冩煡鍣搁弨鐐暰閸戜紮绱版担璺ㄦ暏閺冨爼妫块敓? nonce</li>
 *   <li>閸欏倹鏆熼幒鎺戠碍閿涙氨'娣囨繄顒烽崥宥勭閼疯揪鎷?li>
 *   <li>閺堝秴濮熼梻纾嬵吇鐠囦緤绱伴崺杞扮艾 App-ID 閿熺祳ecret-Key</li>
 * </ul>
 *
 * <p>缁涙儳鎮曢弽鐓庣础閿?
 * <pre>
 * signature = HMAC-SHA256(secretKey, timestamp + nonce + appId + uri + sortedParams)
 * </pre>
 *
 * <p>HTTP Headers閿?
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

    /**
     * The secret is resolved through {@link InternalSignatureSecretResolver} so that the signer and
     * the verifier of the service-to-service channel always read the same value. There is no
     * built-in fallback any more: a missing secret is a configuration error, not a constant that
     * ships with the repository.
     */
    private final String appId;
    private final String secretKey;

    public RestClientRequestSignatureInterceptor(InternalSignatureSecretResolver secretResolver) {
        this.appId = secretResolver.resolveAppId();
        this.secretKey = secretResolver.resolveSecret();
        if (!StringUtils.hasText(secretKey)) {
            log.error("No internal signature secret is configured, so every service-to-service "
                    + "request is signed with an empty key and the receiving side will reject it. "
                    + "Set API_SECRET_INTERNAL_SERVICE (or security.signature.app-secrets.internal-service) "
                    + "to the same random value on every service.");
        }
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // 閻㈢喐鍨氶弮鍫曟？閹村啿鎷?nonce
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUIDv7Util.generate().toString().replace("-", "");

        // 鐠?鐣荤粵鎯ф倳
        String signature = calculateSignature(request, timestamp, nonce);

        // 濞ｈ濮炵粵鎯ф倳 Header
        request.getHeaders().set("X-Timestamp", timestamp);
        request.getHeaders().set("X-Nonce", nonce);
        request.getHeaders().set("X-Signature", signature);
        request.getHeaders().set("X-App-Id", appId);

        if (log.isDebugEnabled()) {
            log.debug("RestClient request signed: {} {}, signature: {}",
                      request.getMethod(), request.getURI(), signature);
        }

        // 缂佈呯敾閹笛嗩攽鐠囬攱鐪?
        return execution.execute(request, body);
    }

    /**
     * 鐠?鐣荤拠閿嬬湴缁涙儳鎮?
     *
     * <p>缁涙儳鎮曠粻妤佺《閿?
     * <pre>
     * 1. 閹绘劕褰?URI 鐠侯垰绶為敍鍫滅瑝閸氼偄鐓欓崥宥呮嫲缁旑垰褰涢敓?
     * 2. 閹绘劕褰囬弻銉嚄閸欏倹鏆熼獮鑸靛瘻 key 閹烘帒绨?
     * 3. 閹峰吋甯寸粵鎯ф倳閸愬懎顔愰敍姝礽mestamp + nonce + appId + uri + sortedParams
     * 4. 娴ｈ法鏁?HMAC-SHA256 鐠?鐣荤粵鎯ф倳
     * </pre>
     *
     * @param request   HTTP 鐠囬攱鐪?
     * @param timestamp 閺冨爼妫块敓?
     * @param nonce     闂呭繑婧€閿?
     * @return 缁涙儳鎮曠€涙顑佹稉璇х礄閸椾礁鍙氭潻娑樺煑閿?
     */
    private String calculateSignature(HttpRequest request, String timestamp, String nonce) {
        // 閼惧嘲褰?URI 鐠侯垰绶?
        String uri = request.getURI().getPath();

        // 閼惧嘲褰囬弻銉嚄閸欏倹鏆熼獮鑸靛笓閿?
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

        // 閹烘帒绨獮鑸靛閹恒儱寮敓?
        String sortedParams = sortAndConcatParams(params);

        // 閺嬪嫬缂撶粵鎯ф倳閸愬懎顔?
        String signContent = timestamp + nonce + appId + uri + sortedParams;

        if (log.isTraceEnabled()) {
            log.trace("Signature content: {}", signContent);
        }

        // 鐠?鐣?HMAC-SHA256 缁涙儳鎮?
        return SecureUtil.hmac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(StandardCharsets.UTF_8))
            .digestHex(signContent);
    }

    /**
     * 鐎电懓寮弫鎷岀箻鐞涘本甯撴惔蹇撹嫙閹峰吋甯?
     *
     * <p>閺嶇厧绱￠敍姝琫y1=value1&key2=value2&...閿涘牊瀵?key 鐎涙鍚€鎼村繑甯撴惔蹇ョ礆</p>
     *
     * @param params 閸欏倹鏆?Map
     * @return 閹烘帒绨崥搴ｆ畱閸欏倹鏆熺€涙顑侀敓?
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
