package com.scmcloud.common.rest.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ReactorLoadBalancer;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Load-balancer strategy beans, instantiated once per load-balancer client context by
 * {@link LoadBalancerConfiguration}.
 *
 * <p>Deliberately not a {@code @Configuration} class: component scanning would pick it up in the
 * application context and reintroduce the shared "default" bean this setup exists to avoid.
 */
@Slf4j
public class LoadBalancerClientStrategyConfiguration {

    @Bean
    @ConditionalOnProperty(
        name = "spring.cloud.loadbalancer.strategy",
        havingValue = "round-robin",
        matchIfMissing = true
    )
    public ReactorLoadBalancer<ServiceInstance> roundRobinLoadBalancer(
            Environment environment,
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME, "default");
        log.info("Using RoundRobinLoadBalancer for service: {}", name);

        return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider, name);
    }

    @Bean
    @ConditionalOnProperty(
        name = "spring.cloud.loadbalancer.strategy",
        havingValue = "random"
    )
    public ReactorLoadBalancer<ServiceInstance> randomLoadBalancer(
            Environment environment,
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME, "default");
        log.info("Using RandomLoadBalancer for service: {}", name);

        return new RandomLoadBalancer(serviceInstanceListSupplierProvider, name);
    }

    @Bean
    @ConditionalOnProperty(
        name = "spring.cloud.loadbalancer.strategy",
        havingValue = "weighted-round-robin"
    )
    public ReactorLoadBalancer<ServiceInstance> weightedRoundRobinLoadBalancer(
            Environment environment,
            ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {

        String name = environment.getProperty(LoadBalancerClientFactory.PROPERTY_NAME, "default");
        log.info("Using WeightedRoundRobinLoadBalancer (Nacos-based) for service: {}", name);

        return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider, name);
    }
}
