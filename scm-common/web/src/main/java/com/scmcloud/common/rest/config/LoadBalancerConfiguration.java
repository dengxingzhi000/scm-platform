package com.scmcloud.common.rest.config;

import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the load-balancer strategy beans as a per-client configuration.
 *
 * <p>These beans must not live in the application context: a {@code ReactorLoadBalancer} declared
 * here is created once with the client name "default", and every load-balancer child context then
 * reuses it. {@code LoadBalancerClient.choose("system-service")} consequently asks for a service
 * called "default", finds no instances and returns null.
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = LoadBalancerClientStrategyConfiguration.class)
public class LoadBalancerConfiguration {
}
