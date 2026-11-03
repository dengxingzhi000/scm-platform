package com.cognora.platform.common.rest.signature;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Resolves the shared secret of the service-to-service HMAC channel.
 *
 * <p>The signer ({@code RestClientRequestSignatureInterceptor}) and the verifier (scm-audit's
 * {@code AuditWriteRequestGuard}) must resolve the <em>same</em> value. If they drift apart every
 * internal audit write is rejected with 403 and auditing stops platform-wide, so both sides go
 * through this class instead of declaring their own {@code @Value} defaults.
 *
 * <p>Secret candidates, first non-blank hit wins:
 * <ol>
 *   <li>{@code security.signature.app-secrets.internal-service} — platform property (gateway)</li>
 *   <li>{@code API_SECRET_INTERNAL_SERVICE} — platform environment variable</li>
 *   <li>{@code security.feign.secret-key} — legacy per-service override</li>
 *   <li>{@code API_SECRET_INTERNAL_SECRET} — legacy environment variable</li>
 * </ol>
 *
 * <p>There is deliberately no built-in fallback: a missing secret has to surface as a startup
 * failure, never as a constant that is committed to the repository. Blank candidates are skipped,
 * so an exported-but-empty variable falls through instead of silently signing with "".
 */
@Component
@RequiredArgsConstructor
public class InternalSignatureSecretResolver {

    /** App id sent as {@code X-App-Id}. It is an identifier, never a secret. */
    public static final String DEFAULT_APP_ID = "internal-service";

    static final List<String> SECRET_CANDIDATES = List.of(
            "security.signature.app-secrets.internal-service",
            "API_SECRET_INTERNAL_SERVICE",
            "security.feign.secret-key",
            "API_SECRET_INTERNAL_SECRET");

    static final List<String> APP_ID_CANDIDATES = List.of(
            "security.feign.app-id",
            "API_APP_ID");

    private final Environment environment;

    /**
     * The shared secret, or an empty string when nothing is configured.
     */
    public String resolveSecret() {
        return resolve(SECRET_CANDIDATES);
    }

    /**
     * The caller identity sent as {@code X-App-Id}. Never blank: a blank app id turns every
     * signature check into a rejection, which would silently disable auditing platform-wide.
     */
    public String resolveAppId() {
        String appId = resolve(APP_ID_CANDIDATES);
        return appId.isEmpty() ? DEFAULT_APP_ID : appId;
    }

    private String resolve(List<String> candidates) {
        for (String candidate : candidates) {
            String value = environment.getProperty(candidate);
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
