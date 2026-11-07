package com.cognora.platform.gateway.config;

import com.cognora.platform.gateway.properties.GatewayPrefixAliasProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.util.AntPathMatcher;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the {@code cognora.gateway.prefix-aliases.enabled} switch.
 *
 * <p>The switch is the deployment guard for the transitional aliases: with it
 * off (the default) no alias route definition is registered, so the discovery
 * locator's old-name routes stay in effect. With it on the aliases keep their
 * {@code order: -1}, i.e. they outrank the locator.
 */
class GatewayPrefixAliasConfigTest {

    private static final String SWITCH = "cognora.gateway.prefix-aliases.enabled";

    /** Mirrors the {@code Path} predicate matching the gateway performs. */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(GatewayPrefixAliasConfig.class);

    @Test
    void aliasesStayUnregisteredByDefault() {
        runner.run(context -> {
            assertThat(context.getBean(GatewayPrefixAliasProperties.class).isEnabled()).isFalse();
            assertThat(context.getBeansOfType(RouteDefinitionLocator.class)).isEmpty();
        });
    }

    @Test
    void aliasesAreRegisteredWhenSwitchIsOn() {
        runner.withPropertyValues(SWITCH + "=true").run(context -> {
            assertThat(context.getBean(GatewayPrefixAliasProperties.class).isEnabled()).isTrue();

            List<RouteDefinition> definitions = aliasDefinitions(context);
            assertThat(definitions).hasSize(GatewayPrefixAliasConfig.ALIAS_ROUTES.size());
            assertThat(definitions).allSatisfy(definition ->
                    assertThat(definition.getOrder()).isEqualTo(-1));
        });
    }

    @Test
    void everyAliasMatchesTheDeclaredTable() {
        runner.withPropertyValues(SWITCH + "=true").run(context -> {
            List<RouteDefinition> definitions = aliasDefinitions(context);
            for (GatewayPrefixAliasConfig.AliasRoute route : GatewayPrefixAliasConfig.ALIAS_ROUTES) {
                RouteDefinition definition = definitions.stream()
                        .filter(candidate -> route.id().equals(candidate.getId()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("missing alias route " + route.id()));

                assertThat(definition.getUri()).isEqualTo(URI.create(route.uri()));
                assertThat(definition.getPredicates())
                        .singleElement()
                        .satisfies(predicate -> {
                            assertThat(predicate.getName()).isEqualTo("Path");
                            assertThat(predicate.getArgs()).containsValue(route.path());
                        });
                assertThat(definition.getFilters())
                        .singleElement()
                        .satisfies(filter -> {
                            assertThat(filter.getName()).isEqualTo("StripPrefix");
                            assertThat(filter.getArgs()).containsValue("1");
                        });
            }
        });
    }

    @Test
    void disabledSwitchLeavesTheLegacyDiscoveryRoutesInEffect() {
        runner.run(context -> {
            assertThat(context.getBeansOfType(RouteDefinitionLocator.class)).isEmpty();

            // The routes the discovery locator would derive from the old names.
            List<RouteDefinition> locatorRoutes = legacyLocatorRoutes("scm-audit", "scm-order");

            RouteDefinition audit = winningRoute(locatorRoutes, "/scm-audit/sys-audit-log/page");
            assertThat(audit.getId()).isEqualTo("scm-audit");
            assertThat(audit.getUri()).isEqualTo(URI.create("lb://scm-audit"));

            RouteDefinition order = winningRoute(locatorRoutes, "/scm-order/api/v1/orders");
            assertThat(order.getId()).isEqualTo("scm-order");
            assertThat(order.getUri()).isEqualTo(URI.create("lb://scm-order"));
        });
    }

    @Test
    void enabledAliasesOutrankTheLegacyDiscoveryRoutes() {
        runner.withPropertyValues(SWITCH + "=true").run(context -> {
            List<RouteDefinition> definitions = new ArrayList<>(aliasDefinitions(context));
            definitions.addAll(legacyLocatorRoutes("scm-audit", "scm-order"));

            RouteDefinition audit = winningRoute(definitions, "/scm-audit/sys-audit-log/page");
            assertThat(audit.getId()).isEqualTo("alias-scm-audit");
            assertThat(audit.getUri()).isEqualTo(URI.create("lb://platform-audit"));
            assertThat(audit.getOrder()).isEqualTo(-1);

            RouteDefinition order = winningRoute(definitions, "/scm-order/api/v1/orders");
            assertThat(order.getId()).isEqualTo("alias-scm-order");
            assertThat(order.getUri()).isEqualTo(URI.create("lb://scm-order"));
            assertThat(order.getOrder()).isEqualTo(-1);
        });
    }

    private static List<RouteDefinition> aliasDefinitions(
            org.springframework.context.ApplicationContext context) {
        RouteDefinitionLocator locator = context.getBean(RouteDefinitionLocator.class);
        List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();
        assertThat(definitions).isNotNull();
        return definitions;
    }

    /**
     * Stand-in for the route the discovery locator builds for an old-name
     * registration: {@code /<serviceId>/** -> lb://<serviceId>}, order 0.
     */
    private static List<RouteDefinition> legacyLocatorRoutes(String... serviceIds) {
        List<RouteDefinition> definitions = new ArrayList<>();
        for (String serviceId : serviceIds) {
            RouteDefinition definition = new RouteDefinition();
            definition.setId(serviceId);
            definition.setUri(URI.create("lb://" + serviceId));
            definition.setPredicates(List.of(new PredicateDefinition("Path=/" + serviceId + "/**")));
            definition.setFilters(List.of(new FilterDefinition("StripPrefix=1")));
            definition.setOrder(0);
            definitions.add(definition);
        }
        return definitions;
    }

    private static boolean matchesPath(RouteDefinition definition, String path) {
        return definition.getPredicates().stream()
                .filter(predicate -> "Path".equals(predicate.getName()))
                .anyMatch(predicate -> predicate.getArgs().values().stream()
                        .anyMatch(pattern -> PATH_MATCHER.match(pattern, path)));
    }

    /** Lowest {@code order} wins, which is how the gateway picks a route. */
    private static RouteDefinition winningRoute(List<RouteDefinition> definitions, String path) {
        return definitions.stream()
                .filter(definition -> matchesPath(definition, path))
                .min(Comparator.comparingInt(RouteDefinition::getOrder))
                .orElseThrow(() -> new AssertionError("no route matches " + path));
    }
}
