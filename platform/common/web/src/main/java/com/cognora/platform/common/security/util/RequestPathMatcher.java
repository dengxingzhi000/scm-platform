package com.cognora.platform.common.security.util;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Path matching for the security filters.
 *
 * <p>Spring MVC matches on the <em>decoded</em> path with matrix parameters removed and the
 * trailing slash dropped. Comparing a raw {@code getRequestURI()} against a configured path lets
 * {@code /sys-audit-log;x=1} or {@code /%73ys-audit-log} through, so every security decision goes
 * through this class.
 */
public final class RequestPathMatcher {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private RequestPathMatcher() {
    }

    /**
     * Normalise a raw request URI: strip matrix parameters per segment, percent-decode, drop the
     * trailing slash.
     *
     * @return the normalised path, or {@code null} when a segment cannot be decoded
     */
    public static String normalizePath(String rawUri) {
        if (rawUri == null) {
            return null;
        }
        String[] segments = rawUri.split("/", -1);
        StringBuilder normalized = new StringBuilder(rawUri.length());
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                normalized.append('/');
            }
            String segment = segments[i];
            int matrixStart = segment.indexOf(';');
            if (matrixStart >= 0) {
                segment = segment.substring(0, matrixStart);
            }
            try {
                segment = UriUtils.decode(segment, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return null;
            }
            normalized.append(segment);
        }
        String path = normalized.toString();
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    /**
     * Match a raw request URI against configured paths. A pattern containing a wildcard is treated
     * as an Ant pattern, everything else is an exact match.
     */
    public static boolean matches(String rawUri, List<String> patterns) {
        String path = normalizePath(rawUri);
        return path != null && matchesNormalized(path, patterns);
    }

    /**
     * Match an already normalised path against configured paths.
     */
    public static boolean matchesNormalized(String normalizedPath, List<String> patterns) {
        if (normalizedPath == null || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (!StringUtils.hasText(pattern)) {
                continue;
            }
            String candidate = pattern.trim();
            if (candidate.indexOf('*') >= 0) {
                if (MATCHER.match(candidate, normalizedPath)) {
                    return true;
                }
            } else if (candidate.equals(normalizedPath)) {
                return true;
            }
        }
        return false;
    }
}
