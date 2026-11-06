package com.cognora.platform.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads the checked-in {@code application.yaml} and pins the static route table.
 *
 * <p>The legacy aliases are intentionally absent from YAML: they are registered
 * by {@link GatewayPrefixAliasConfig} only when the switch is on, so they cannot
 * silently shadow the discovery-locator routes.
 */
class GatewayRouteConfigTest {

    private static final List<Map<String, Object>> ROUTES = loadRoutes();

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadRoutes() {
        try (InputStream input = new ClassPathResource("application.yaml").getInputStream()) {
            Map<String, Object> root = new Yaml().load(input);
            Map<String, Object> webflux = nested(root, "spring", "cloud", "gateway", "server", "webflux");
            return (List<Map<String, Object>>) webflux.get("routes");
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read gateway routes from application.yaml", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> nested(Map<String, Object> root, String... keys) {
        Map<String, Object> current = root;
        for (String key : keys) {
            current = (Map<String, Object>) current.get(key);
        }
        return current;
    }

    private static List<String> predicates(Map<String, Object> route) {
        return ((List<?>) route.getOrDefault("predicates", List.of())).stream()
                .map(String::valueOf)
                .toList();
    }

    private static Optional<String> pathPattern(Map<String, Object> route) {
        return predicates(route).stream()
                .filter(predicate -> predicate.startsWith("Path="))
                .map(predicate -> predicate.substring("Path=".length()))
                .findFirst();
    }

    private static Optional<Map<String, Object>> routeForPath(String pathPattern) {
        return ROUTES.stream()
                .filter(route -> pathPattern(route).filter(pathPattern::equals).isPresent())
                .findFirst();
    }

    @Test
    void deadPlatformCommonRouteIsRemoved() {
        assertThat(ROUTES).noneMatch(route -> "platform-common".equals(route.get("id")));
        assertThat(ROUTES).noneMatch(route -> predicates(route).contains("Path=/platform-common/**"));
    }

    @Test
    void canonicalAuthAndSystemRoutesAreKept() {
        assertThat(routeForPath("/platform-auth/**")).isPresent();
        assertThat(routeForPath("/platform-system/**")).isPresent();
    }

    @Test
    void legacyAliasesAreNotDeclaredStatically() {
        assertThat(ROUTES).noneMatch(route -> String.valueOf(route.get("id")).startsWith("alias-"));
        assertThat(ROUTES).noneMatch(route -> pathPattern(route)
                .filter(pattern -> pattern.equals("/auth/**")
                        || pattern.equals("/system/**")
                        || pattern.equals("/common/**")
                        || pattern.startsWith("/scm-"))
                .isPresent());
    }
}
