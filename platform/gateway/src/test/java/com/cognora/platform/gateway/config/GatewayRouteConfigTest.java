package com.cognora.platform.gateway.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.AntPathMatcher;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads the checked-in {@code application.yaml} and pins the transitional route
 * table: the SPA aliases must exist, target the canonical service ids, strip the
 * alias segment and sort before the discovery locator, and the dead
 * {@code /platform-common/**} route must stay deleted.
 */
class GatewayRouteConfigTest {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
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

    static Stream<Arguments> aliasRoutes() {
        return Stream.of(
                Arguments.of("alias-auth", "/auth/**", "lb://platform-auth"),
                Arguments.of("alias-system", "/system/**", "lb://platform-system"),
                Arguments.of("alias-common", "/common/**", "lb://platform-system"),
                Arguments.of("alias-scm-product", "/scm-product/**", "lb://platform-product"),
                Arguments.of("alias-scm-inventory", "/scm-inventory/**", "lb://platform-inventory"),
                Arguments.of("alias-scm-order", "/scm-order/**", "lb://scm-order"),
                Arguments.of("alias-scm-warehouse", "/scm-warehouse/**", "lb://scm-warehouse"),
                Arguments.of("alias-scm-logistics", "/scm-logistics/**", "lb://lms-tms"),
                Arguments.of("alias-scm-supplier", "/scm-supplier/**", "lb://scm-supplier"),
                Arguments.of("alias-scm-purchase", "/scm-purchase/**", "lb://scm-purchase"),
                Arguments.of("alias-scm-finance", "/scm-finance/**", "lb://scm-finance"),
                Arguments.of("alias-scm-approval", "/scm-approval/**", "lb://platform-approval"),
                Arguments.of("alias-scm-audit", "/scm-audit/**", "lb://platform-audit"),
                Arguments.of("alias-scm-notify", "/scm-notify/**", "lb://platform-notify"),
                Arguments.of("alias-scm-tenant", "/scm-tenant/**", "lb://platform-tenant"),
                Arguments.of("alias-scm-document", "/scm-document/**", "lb://platform-document"),
                Arguments.of("alias-scm-file", "/scm-file/**", "lb://platform-file"),
                Arguments.of("alias-scm-analytics", "/scm-analytics/**", "lb://ecom-analytics"),
                Arguments.of("alias-scm-mall", "/scm-mall/**", "lb://ecom-mall"),
                Arguments.of("alias-scm-member", "/scm-member/**", "lb://ecom-member"),
                Arguments.of("alias-scm-promotion", "/scm-promotion/**", "lb://ecom-promotion"),
                Arguments.of("alias-scm-payment", "/scm-payment/**", "lb://ecom-payment"),
                Arguments.of("alias-scm-order-center", "/scm-order-center/**", "lb://ecom-order-center"),
                Arguments.of("alias-scm-fulfillment", "/scm-fulfillment/**", "lb://ecom-fulfillment"),
                Arguments.of("alias-scm-search", "/scm-search/**", "lb://ecom-search")
        );
    }

    static Stream<Arguments> spaRequests() {
        return Stream.of(
                Arguments.of("/auth/api/auth/login", "alias-auth"),
                Arguments.of("/system/api/system/users", "alias-system"),
                Arguments.of("/common/api/system/users", "alias-common"),
                Arguments.of("/scm-product/api/v1/products", "alias-scm-product"),
                Arguments.of("/scm-inventory/api/v1/inventory/alerts/query", "alias-scm-inventory"),
                Arguments.of("/scm-order/api/v1/orders", "alias-scm-order"),
                Arguments.of("/scm-warehouse/wms-inbound/page", "alias-scm-warehouse"),
                Arguments.of("/scm-logistics/api/v1/tms-waybills", "alias-scm-logistics"),
                Arguments.of("/scm-supplier/api/v1/suppliers/page", "alias-scm-supplier"),
                Arguments.of("/scm-purchase/api/v1/pur-order/page", "alias-scm-purchase"),
                Arguments.of("/scm-finance/api/v1/settlements/page", "alias-scm-finance"),
                Arguments.of("/scm-approval/api/v1/approvals/page", "alias-scm-approval"),
                Arguments.of("/scm-audit/sys-audit-log/page", "alias-scm-audit"),
                Arguments.of("/scm-notify/sys-user-notification/page", "alias-scm-notify"),
                Arguments.of("/scm-tenant/api/v1/tenants/page", "alias-scm-tenant"),
                Arguments.of("/scm-document/api/v1/documents/page", "alias-scm-document"),
                Arguments.of("/scm-file/api/v1/files/page", "alias-scm-file"),
                Arguments.of("/scm-analytics/api/v1/dashboard", "alias-scm-analytics"),
                Arguments.of("/scm-mall/api/v1/mall/products", "alias-scm-mall"),
                Arguments.of("/scm-member/api/v1/members/1", "alias-scm-member"),
                Arguments.of("/scm-promotion/api/v1/promotions/page", "alias-scm-promotion"),
                Arguments.of("/scm-payment/api/v1/payments/1", "alias-scm-payment"),
                Arguments.of("/scm-order-center/api/v1/orders/1", "alias-scm-order-center"),
                Arguments.of("/scm-fulfillment/api/v1/fulfillments/1", "alias-scm-fulfillment"),
                Arguments.of("/scm-search/api/v1/search", "alias-scm-search")
        );
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

    @ParameterizedTest
    @MethodSource("aliasRoutes")
    void aliasRouteTargetsCanonicalServiceWithStripPrefix(String id, String path, String uri) {
        Map<String, Object> route = routeForPath(path)
                .orElseThrow(() -> new AssertionError("Missing alias route for " + path));

        assertThat(route.get("id")).isEqualTo(id);
        assertThat(route.get("uri")).isEqualTo(uri);
        assertThat(route.get("filters")).isEqualTo(List.of("StripPrefix=1"));
        assertThat(((Number) route.get("order")).intValue()).isEqualTo(-1);
    }

    @Test
    void aliasOrderSortsBeforeDiscoveryLocator() {
        ROUTES.stream()
                .filter(route -> String.valueOf(route.get("id")).startsWith("alias-"))
                .forEach(route -> assertThat(((Number) route.get("order")).intValue())
                        .as("alias %s must be ordered before the discovery locator (order 0)", route.get("id"))
                        .isLessThan(0));
    }

    @ParameterizedTest
    @MethodSource("spaRequests")
    void spaRequestIsClaimedByExactlyOneAlias(String requestPath, String expectedId) {
        List<Map<String, Object>> matches = ROUTES.stream()
                .filter(route -> String.valueOf(route.get("id")).startsWith("alias-"))
                .filter(route -> pathPattern(route)
                        .filter(pattern -> PATH_MATCHER.match(pattern, requestPath))
                        .isPresent())
                .toList();

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).get("id")).isEqualTo(expectedId);
    }
}
