package com.scmcloud.common.access;

import com.scmcloud.common.rest.client.SysPermissionServiceClient;
import com.scmcloud.common.security.PermissionService.PermissionServiceException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionAccessFailureTest {

    @Test
    void wrapsDiscoveryFailureAsPermissionServiceException() {
        SysPermissionServiceClient client = mock(SysPermissionServiceClient.class);
        when(client.findPermissionsByUrl(anyString(), anyString()))
                .thenThrow(new IllegalStateException(
                        "Load balancer does not contain an instance for the service system-service"));

        FeignPermissionAccess access = new FeignPermissionAccess(client, new SimpleMeterRegistry());

        assertThatThrownBy(() -> access.findPermissionsByUrl("/api/v1/orders", "GET"))
                .isInstanceOf(PermissionServiceException.class)
                .hasMessageContaining("Permission service unavailable")
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
