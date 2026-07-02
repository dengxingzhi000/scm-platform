package com.scmcloud.common.access;

import com.scmcloud.common.rest.client.SysPermissionServiceClient;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.security.PermissionService.PermissionServiceException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RestClient Permission Access Tests")
class FeignPermissionAccessTest {

    @Mock
    private SysPermissionServiceClient permissionServiceClient;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter successCounter;

    @Mock
    private Counter failureCounter;

    @InjectMocks
    private FeignPermissionAccess feignPermissionAccess;

    private String testUrl;
    private String testMethod;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUrl = "/api/users";
        testMethod = "GET";
        testUserId = UUID.randomUUID();

        when(meterRegistry.counter("security.permissions.rest.lookup.success")).thenReturn(successCounter);
        when(meterRegistry.counter("security.permissions.rest.lookup.fail")).thenReturn(failureCounter);
        when(meterRegistry.counter("security.permissions.rest.user.success")).thenReturn(successCounter);
        when(meterRegistry.counter("security.permissions.rest.user.fail")).thenReturn(failureCounter);
    }

    @Test
    @DisplayName("Should return permissions when service call succeeds")
    void testFindPermissionsByUrl_Success() {
        List<String> expectedPermissions = List.of("user:read", "user:list");
        when(permissionServiceClient.findPermissionsByUrl(testUrl, testMethod))
                .thenReturn(expectedPermissions);

        List<String> actualPermissions = feignPermissionAccess.findPermissionsByUrl(testUrl, testMethod);

        assertThat(actualPermissions).containsExactlyInAnyOrderElementsOf(expectedPermissions);
        verify(successCounter).increment();
        verify(failureCounter, never()).increment();
    }

    @Test
    @DisplayName("Should return empty list when no permissions found")
    void testFindPermissionsByUrl_NoPermissions() {
        when(permissionServiceClient.findPermissionsByUrl(testUrl, testMethod))
                .thenReturn(null);

        List<String> actualPermissions = feignPermissionAccess.findPermissionsByUrl(testUrl, testMethod);

        assertThat(actualPermissions).isEmpty();
        verify(successCounter).increment();
    }

    @Test
    @DisplayName("SECURITY: Should DENY access when service call fails (fail-closed)")
    void testFindPermissionsByUrl_ServiceFailure_DeniesAccess() {
        when(permissionServiceClient.findPermissionsByUrl(testUrl, testMethod))
                .thenThrow(new RuntimeException("Service unavailable"));

        assertThatThrownBy(() -> feignPermissionAccess.findPermissionsByUrl(testUrl, testMethod))
                .isInstanceOf(PermissionServiceException.class)
                .hasMessageContaining("Permission service unavailable")
                .hasMessageContaining("access denied");

        verify(failureCounter).increment();
        verify(successCounter, never()).increment();
    }

    @Test
    @DisplayName("Should find user permissions successfully")
    void testFindAllPermissionsByUserId_Success() {
        Set<String> expectedPermissions = Set.of("user:read", "user:write", "admin:access");
        ApiResponse<Set<String>> response = ApiResponse.success(expectedPermissions);

        when(permissionServiceClient.getUserPermissions(testUserId))
                .thenReturn(response);

        Set<String> actualPermissions = feignPermissionAccess.findAllPermissionsByUserId(testUserId);

        assertThat(actualPermissions).containsExactlyInAnyOrderElementsOf(expectedPermissions);
        verify(successCounter).increment();
    }

    @Test
    @DisplayName("Should return empty set when user has no permissions")
    void testFindAllPermissionsByUserId_NoPermissions() {
        when(permissionServiceClient.getUserPermissions(testUserId))
                .thenReturn(null);

        Set<String> actualPermissions = feignPermissionAccess.findAllPermissionsByUserId(testUserId);

        assertThat(actualPermissions).isEmpty();
        verify(successCounter).increment();
    }

    @Test
    @DisplayName("SECURITY: Should DENY access when user permission lookup fails (fail-closed)")
    void testFindAllPermissionsByUserId_ServiceFailure_DeniesAccess() {
        when(permissionServiceClient.getUserPermissions(testUserId))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThatThrownBy(() -> feignPermissionAccess.findAllPermissionsByUserId(testUserId))
                .isInstanceOf(PermissionServiceException.class)
                .hasMessageContaining("Permission service unavailable")
                .hasMessageContaining("access denied");

        verify(failureCounter).increment();
    }

    @Test
    @DisplayName("Should handle RestClient timeout gracefully - DENY access")
    void testFindPermissionsByUrl_Timeout_DeniesAccess() {
        when(permissionServiceClient.findPermissionsByUrl(testUrl, testMethod))
                .thenThrow(new RuntimeException("Read timed out"));

        assertThatThrownBy(() -> feignPermissionAccess.findPermissionsByUrl(testUrl, testMethod))
                .isInstanceOf(PermissionServiceException.class)
                .hasMessageContaining("access denied");

        verify(failureCounter).increment();
    }

    @Test
    @DisplayName("Should handle multiple rapid permission checks")
    void testConcurrentPermissionChecks() throws InterruptedException {
        List<String> permissions = List.of("user:read");
        when(permissionServiceClient.findPermissionsByUrl(anyString(), anyString()))
                .thenReturn(permissions);

        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                List<String> result = feignPermissionAccess.findPermissionsByUrl(testUrl, testMethod);
                assertThat(result).containsExactly("user:read");
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        verify(successCounter, times(10)).increment();
    }

    @Test
    @DisplayName("Should differentiate between GET and POST method permissions")
    void testFindPermissionsByUrl_DifferentMethods() {
        List<String> getPermissions = List.of("user:read");
        List<String> postPermissions = List.of("user:write");

        when(permissionServiceClient.findPermissionsByUrl(testUrl, "GET"))
                .thenReturn(getPermissions);
        when(permissionServiceClient.findPermissionsByUrl(testUrl, "POST"))
                .thenReturn(postPermissions);

        List<String> getResult = feignPermissionAccess.findPermissionsByUrl(testUrl, "GET");
        List<String> postResult = feignPermissionAccess.findPermissionsByUrl(testUrl, "POST");

        assertThat(getResult).containsExactly("user:read");
        assertThat(postResult).containsExactly("user:write");
    }

    @Test
    @DisplayName("Should handle API response with null data gracefully")
    void testFindAllPermissionsByUserId_NullResponseData() {
        ApiResponse<Set<String>> response = ApiResponse.success(null);
        when(permissionServiceClient.getUserPermissions(testUserId))
                .thenReturn(response);

        Set<String> result = feignPermissionAccess.findAllPermissionsByUserId(testUserId);

        assertThat(result).isEmpty();
    }
}
