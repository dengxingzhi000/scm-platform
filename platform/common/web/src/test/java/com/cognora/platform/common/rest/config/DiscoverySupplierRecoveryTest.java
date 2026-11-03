package com.cognora.platform.common.rest.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.loadbalancer.core.DiscoveryClientServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.mock.env.MockEnvironment;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscoverySupplierRecoveryTest {

    @Test
    void reResolvesOnEveryCallAndPicksUpNewAddress() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        ServiceInstance first = instance("a", "172.25.0.14");
        ServiceInstance second = instance("b", "172.25.0.15");
        when(discoveryClient.getInstances("platform-system"))
                .thenReturn(List.of(), List.of(first), List.of(second));

        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(LoadBalancerClientFactory.PROPERTY_NAME, "platform-system");
        DiscoveryClientServiceInstanceListSupplier supplier =
                new DiscoveryClientServiceInstanceListSupplier(discoveryClient, environment);

        assertThat(supplier.get().blockFirst()).isEmpty();
        assertThat(supplier.get().blockFirst()).containsExactly(first);
        assertThat(supplier.get().blockFirst()).containsExactly(second);
        verify(discoveryClient, times(3)).getInstances("platform-system");
    }

    private static ServiceInstance instance(String instanceId, String host) {
        return new DefaultServiceInstance(instanceId, "platform-system", host, 8081, false);
    }
}
