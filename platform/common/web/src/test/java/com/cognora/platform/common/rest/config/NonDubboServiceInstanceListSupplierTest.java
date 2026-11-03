package com.cognora.platform.common.rest.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NonDubboServiceInstanceListSupplierTest {

    @Test
    void filtersDubboRpcInstances() {
        ServiceInstance http = instance("http", "172.25.0.14", 8081, Map.of());
        ServiceInstance dubbo = instance("dubbo", "172.25.0.14", 20880,
                Map.of("dubbo.endpoints", "[{\"port\":20880}]"));
        ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
        when(delegate.getServiceId()).thenReturn("platform-system");
        when(delegate.get()).thenReturn(Flux.just(List.of(http, dubbo)));

        NonDubboServiceInstanceListSupplier supplier = new NonDubboServiceInstanceListSupplier(delegate);

        assertThat(supplier.getServiceId()).isEqualTo("platform-system");
        assertThat(supplier.get().blockFirst()).containsExactly(http);
    }

    @Test
    void picksUpNewInstanceAddressWithoutCaching() {
        ServiceInstance first = instance("http-1", "172.25.0.14", 8081, Map.of());
        ServiceInstance second = instance("http-2", "172.25.0.15", 8081, Map.of());
        ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
        when(delegate.get()).thenReturn(Flux.just(List.of(first)), Flux.just(List.of(second)));

        NonDubboServiceInstanceListSupplier supplier = new NonDubboServiceInstanceListSupplier(delegate);

        assertThat(supplier.get().blockFirst()).containsExactly(first);
        assertThat(supplier.get().blockFirst()).containsExactly(second);
    }

    @Test
    void recoversAfterEmptyDiscoveryResult() {
        ServiceInstance http = instance("http", "172.25.0.14", 8081, Map.of());
        ServiceInstanceListSupplier delegate = mock(ServiceInstanceListSupplier.class);
        when(delegate.get()).thenReturn(Flux.just(List.of()), Flux.just(List.of(http)));

        NonDubboServiceInstanceListSupplier supplier = new NonDubboServiceInstanceListSupplier(delegate);

        assertThat(supplier.get().blockFirst()).isEmpty();
        assertThat(supplier.get().blockFirst()).containsExactly(http);
    }

    private static ServiceInstance instance(String instanceId, String host, int port, Map<String, String> metadata) {
        return new DefaultServiceInstance(instanceId, "platform-system", host, port, false, metadata);
    }
}
