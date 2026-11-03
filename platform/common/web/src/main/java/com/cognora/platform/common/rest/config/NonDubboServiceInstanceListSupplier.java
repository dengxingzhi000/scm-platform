package com.cognora.platform.common.rest.config;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * Removes Dubbo RPC registrations from the instance list.
 *
 * <p>Dubbo registers the same application name on its own port (20880) with
 * {@code dubbo.endpoints} metadata. An HTTP RestClient must not select those RPC ports, so the
 * load balancer sees only instances that expose an HTTP endpoint.
 *
 * <p>The delegate is queried on every {@link #get()}, so a changed instance list (including a
 * new IP after a restart) is observed without rebuilding the client.
 */
public class NonDubboServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {

    private static final String DUBBO_ENDPOINTS_METADATA = "dubbo.endpoints";

    public NonDubboServiceInstanceListSupplier(ServiceInstanceListSupplier delegate) {
        super(delegate);
    }

    @Override
    public Flux<List<ServiceInstance>> get() {
        return delegate.get().map(this::withoutDubboInstances);
    }

    private List<ServiceInstance> withoutDubboInstances(List<ServiceInstance> instances) {
        return instances.stream()
                .filter(this::isHttpInstance)
                .toList();
    }

    private boolean isHttpInstance(ServiceInstance instance) {
        Map<String, String> metadata = instance.getMetadata();
        return metadata == null || metadata.get(DUBBO_ENDPOINTS_METADATA) == null;
    }
}
