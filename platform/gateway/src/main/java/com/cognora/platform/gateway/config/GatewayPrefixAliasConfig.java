package com.cognora.platform.gateway.config;

import com.cognora.platform.gateway.properties.GatewayPrefixAliasProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;

/**
 * Transitional aliases for the SPA's legacy prefixes.
 *
 * <p>The routes are defined here instead of {@code application.yaml} so they can
 * be registered conditionally: when
 * {@code cognora.gateway.prefix-aliases.enabled} is false (the default) this
 * config registers no {@link RouteDefinitionLocator} at all, so nothing shadows
 * the discovery-locator routes derived from the registered service names.
 *
 * <p>Aliases use {@code order: -1}, i.e. they win over the discovery locator.
 * That is only safe once every target is registered under its canonical name;
 * see {@code docs/operations/GATEWAY_PREFIX_ALIAS_MIGRATION.md}.
 */
@Configuration
@EnableConfigurationProperties(GatewayPrefixAliasProperties.class)
public class GatewayPrefixAliasConfig {

    private static final Logger log = LoggerFactory.getLogger(GatewayPrefixAliasConfig.class);

    private static final int ALIAS_ORDER = -1;

    /** Alias table: legacy path pattern -> canonical service id. */
    static final List<AliasRoute> ALIAS_ROUTES = List.of(
            new AliasRoute("alias-auth", "/auth/**", "lb://platform-auth"),
            new AliasRoute("alias-system", "/system/**", "lb://platform-system"),
            new AliasRoute("alias-common", "/common/**", "lb://platform-system"),
            new AliasRoute("alias-scm-product", "/scm-product/**", "lb://platform-product"),
            new AliasRoute("alias-scm-inventory", "/scm-inventory/**", "lb://platform-inventory"),
            new AliasRoute("alias-scm-order", "/scm-order/**", "lb://scm-order"),
            new AliasRoute("alias-scm-warehouse", "/scm-warehouse/**", "lb://scm-warehouse"),
            new AliasRoute("alias-scm-logistics", "/scm-logistics/**", "lb://lms-tms"),
            new AliasRoute("alias-scm-supplier", "/scm-supplier/**", "lb://scm-supplier"),
            new AliasRoute("alias-scm-purchase", "/scm-purchase/**", "lb://scm-purchase"),
            new AliasRoute("alias-scm-finance", "/scm-finance/**", "lb://scm-finance"),
            new AliasRoute("alias-scm-approval", "/scm-approval/**", "lb://platform-approval"),
            new AliasRoute("alias-scm-audit", "/scm-audit/**", "lb://platform-audit"),
            new AliasRoute("alias-scm-notify", "/scm-notify/**", "lb://platform-notify"),
            new AliasRoute("alias-scm-tenant", "/scm-tenant/**", "lb://platform-tenant"),
            new AliasRoute("alias-scm-document", "/scm-document/**", "lb://platform-document"),
            new AliasRoute("alias-scm-file", "/scm-file/**", "lb://platform-file"),
            new AliasRoute("alias-scm-analytics", "/scm-analytics/**", "lb://ecom-analytics"),
            new AliasRoute("alias-scm-mall", "/scm-mall/**", "lb://ecom-mall"),
            new AliasRoute("alias-scm-member", "/scm-member/**", "lb://ecom-member"),
            new AliasRoute("alias-scm-promotion", "/scm-promotion/**", "lb://ecom-promotion"),
            new AliasRoute("alias-scm-payment", "/scm-payment/**", "lb://ecom-payment"),
            new AliasRoute("alias-scm-order-center", "/scm-order-center/**", "lb://ecom-order-center"),
            new AliasRoute("alias-scm-fulfillment", "/scm-fulfillment/**", "lb://ecom-fulfillment"),
            new AliasRoute("alias-scm-search", "/scm-search/**", "lb://ecom-search")
    );

    @Bean
    @ConditionalOnProperty(prefix = "cognora.gateway.prefix-aliases", name = "enabled", havingValue = "true")
    public RouteDefinitionLocator prefixAliasRouteDefinitionLocator() {
        return () -> Flux.fromIterable(ALIAS_ROUTES.stream()
                .map(GatewayPrefixAliasConfig::toDefinition)
                .toList());
    }

    @Bean
    public ApplicationRunner prefixAliasStartupReporter(GatewayPrefixAliasProperties properties) {
        return args -> {
            if (properties.isEnabled()) {
                log.info("Gateway prefix aliases ENABLED: {} transitional alias routes registered with order {}",
                        ALIAS_ROUTES.size(), ALIAS_ORDER);
            } else {
                log.info("Gateway prefix aliases DISABLED: 0 of {} alias routes registered; "
                        + "/scm-* paths resolve through the discovery locator", ALIAS_ROUTES.size());
            }
        };
    }

    static RouteDefinition toDefinition(AliasRoute route) {
        RouteDefinition definition = new RouteDefinition();
        definition.setId(route.id());
        definition.setUri(URI.create(route.uri()));
        definition.setPredicates(List.of(new PredicateDefinition("Path=" + route.path())));
        definition.setFilters(List.of(new FilterDefinition("StripPrefix=1")));
        definition.setOrder(ALIAS_ORDER);
        return definition;
    }

    /** A single alias route. */
    record AliasRoute(String id, String path, String uri) {
    }
}
