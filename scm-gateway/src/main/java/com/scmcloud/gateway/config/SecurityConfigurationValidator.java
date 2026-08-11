package com.scmcloud.gateway.config;

import com.scmcloud.gateway.properties.ApiSignatureProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 瀹夊叏閰嶇疆楠岃瘉锟?
 * 纭繚鍦ㄥ簲鐢ㄧ▼搴忓惎鍔ㄥ墠鎵€鏈夊叧閿畨鍏ㄩ厤缃兘宸叉纭缃拷
 * 閬靛惊 Google/Netflix 鐨勬渶浣冲疄璺碉細濡傛灉瀹夊叏閰嶇疆缂哄け锛屽垯蹇€熷け璐ワ拷
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityConfigurationValidator {
    private final ApiSignatureProperties signatureProperties;

    @Value("${security.signature.app-secrets.web-app:}")
    private String webAppSecret;

    @Value("${security.signature.app-secrets.internal-service:}")
    private String internalServiceSecret;

    @Value("${security.identity.signature-secret:}")
    private String identitySignatureSecret;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 鍦ㄥ惎鍔ㄦ椂楠岃瘉鎵€鏈夊叧閿畨鍏ㄩ厤缃拷
     * 濡傛灉鍦ㄧ敓浜х幆澧冧腑缂哄皯浠讳綍蹇呴渶鐨勯厤缃紝搴旂敤绋嬪簭灏嗘棤娉曞惎鍔拷
     */
    @PostConstruct
    public void validateSecurityConfiguration() {
        List<String> missingConfigs = new ArrayList<>();

        boolean isProductionLike = isProductionLikeEnvironment();

        log.info("Validating security configuration (profile: {}, strict mode: {})",
                activeProfile, isProductionLike);

        if (!StringUtils.hasText(webAppSecret)) {
            String error = "security.signature.app-secrets.web-app (env: API_SECRET_WEB_APP)";
            missingConfigs.add(error);
        }

        if (!StringUtils.hasText(internalServiceSecret)) {
            String error = "security.signature.app-secrets.internal-service (env: API_SECRET_INTERNAL_SERVICE)";
            missingConfigs.add(error);
        }

        if (!StringUtils.hasText(identitySignatureSecret)) {
            String error = "security.identity.signature-secret (env: IDENTITY_SIGNATURE_SECRET)";
            missingConfigs.add(error);
        }

        if (!missingConfigs.isEmpty()) {
            String errorMessage = buildErrorMessage(missingConfigs);

            if (isProductionLike) {
                log.error("CRITICAL SECURITY ERROR: {}", errorMessage);
                throw new IllegalStateException(errorMessage);
            } else {
                String warningMessage = """

                    ================================================================================
                      SECURITY WARNING: Missing configuration (acceptable in dev mode)
                      {}
                      These MUST be set via environment variables in production!
                    ================================================================================
                    """;
                log.warn(warningMessage, errorMessage);
            }
        } else {
            log.info("[SecurityConfigurationValidator] All critical security configurations are properly set");

            if (isProductionLike) {
                validateSecretStrength();
            }
        }

        // Validate clock skew configuration consistency
        validateClockSkewConfiguration();
    }

    /**
     * 楠岃瘉瀵嗙爜寮哄害鏄惁绗﹀悎瑕佹眰锟?
     */
    private void validateSecretStrength() {
        List<String> weakSecrets = new ArrayList<>();

        if (webAppSecret.length() < 32) {
            weakSecrets.add("web-app secret is too short (minimum 32 characters)");
        }

        if (internalServiceSecret.length() < 32) {
            weakSecrets.add("internal-service secret is too short (minimum 32 characters)");
        }

        if (identitySignatureSecret.length() < 64) {
            weakSecrets.add("identity signature secret is too short (minimum 64 characters for HMAC-SHA256)");
        }

        if (!weakSecrets.isEmpty()) {
            log.warn("SECURITY WARNING: Weak secrets detected:\n  - {}",
                    String.join("\n  - ", weakSecrets));
        }
    }

    /**
     * 纭畾褰撳墠鐜鏄惁闇€瑕佷弗鏍肩殑瀹夊叏楠岃瘉锟?
     */
    private boolean isProductionLikeEnvironment() {
        return activeProfile != null &&
                (activeProfile.contains("prod") ||
                        activeProfile.contains("production") ||
                        activeProfile.contains("staging") ||
                        activeProfile.contains("uat"));
    }

    /**
     * 楠岃瘉鏃堕挓鍋忕Щ閰嶇疆鐨勪竴鑷存€э拷
     * nonceTtl 蹇呴』 >= allowedClockSkew锛屽惁鍒欏彲鑳藉鑷撮噸鏀炬敾鍑伙拷
     */
    private void validateClockSkewConfiguration() {
        Duration nonceTtl = signatureProperties.getNonceTtl();
        Duration allowedClockSkew = signatureProperties.getAllowedClockSkew();

        if (nonceTtl.compareTo(allowedClockSkew) < 0) {
            String errorMessage = String.format(
                "CRITICAL CONFIGURATION ERROR: nonceTtl (%s) must be >= allowedClockSkew (%s) " +
                "to prevent replay attacks. Current configuration allows requests within %s window " +
                "but only protects against replay for %s.",
                nonceTtl, allowedClockSkew, allowedClockSkew, nonceTtl
            );

            if (isProductionLikeEnvironment()) {
                log.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            } else {
                log.warn("鈿狅笍  {}", errorMessage);
            }
        } else {
            log.info("[SecurityConfigurationValidator] Clock skew configuration validated: nonceTtl={}, allowedClockSkew={}",
                    nonceTtl, allowedClockSkew);
        }
    }

    /**
     * 鐢熸垚閽堝閰嶇疆缂哄け鐨勮缁嗛敊璇秷鎭拷
     */
    private String buildErrorMessage(List<String> missingConfigs) {
        return """
            Missing %d required security configuration(s):
              - %s

            SOLUTION: Set these via environment variables:
              export API_SECRET_WEB_APP='your-secret-here'
              export API_SECRET_INTERNAL_SERVICE='your-secret-here'
              export IDENTITY_SIGNATURE_SECRET='your-secret-here'

            For production deployment, use HashiCorp Vault, AWS Secrets Manager, or equivalent.
            """.formatted(missingConfigs.size(), String.join("\n  - ", missingConfigs));
    }
}
