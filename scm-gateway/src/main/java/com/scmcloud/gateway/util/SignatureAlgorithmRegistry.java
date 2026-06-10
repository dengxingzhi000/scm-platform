package com.scmcloud.gateway.util;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 绛惧悕绠楁硶鐗堟湰鎺у埗
 *
 * @author Deng
 * createData 2025/11/11 9:18
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureAlgorithmRegistry {
    private final List<SignatureAlgorithm> algorithmList;
    private final Map<String, SignatureAlgorithm> algorithms = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 鑷姩娉ㄥ唽鎵€鏈夊疄鐜扮被
        for (SignatureAlgorithm algorithm : algorithmList) {
            algorithms.put(algorithm.version(), algorithm);
            log.info("Registered signature algorithm: {}", algorithm.version());
        }

        if (algorithms.isEmpty()) {
            throw new IllegalStateException("No signature algorithms registered");
        }

        // 楠岃瘉榛樿绠楁硶鏄惁瀛樺湪锛堝惎鍔ㄦ椂蹇€熷け璐ワ級
        if (!algorithms.containsKey("HMAC-SHA256-V1")) {
            throw new IllegalStateException(
                "Default signature algorithm 'HMAC-SHA256-V1' not registered. " +
                "Available algorithms: " + algorithms.keySet()
            );
        }

        log.info("[SignatureAlgorithmRegistry] Default signature algorithm 'HMAC-SHA256-V1' validated successfully");
    }

    public SignatureAlgorithm getAlgorithm(String version) {
        SignatureAlgorithm algorithm = algorithms.get(version);
        if (algorithm != null) {
            return algorithm;
        }

        // 鍥為€€鍒伴粯璁ょ増锟?
        SignatureAlgorithm defaultAlgorithm = algorithms.get("HMAC-SHA256-V1");
        if (defaultAlgorithm == null) {
            throw new IllegalStateException("Default signature algorithm 'HMAC-SHA256-V1' not registered");
        }
        log.warn("Unknown signature version '{}', falling back to HMAC-SHA256-V1", version);

        return defaultAlgorithm;
    }
}
