package com.scmcloud.system.controller;

import com.scmcloud.common.dto.permission.ApiPermissionDTO;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.system.service.ISysPermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysPermissionControllerTest {

    @Mock
    private ISysPermissionService permissionService;

    @InjectMocks
    private SysPermissionController controller;

    @Test
    void updateDelegatesIdBindingToService() {
        UUID id = UUID.randomUUID();
        PermissionDTO permissionDTO = new PermissionDTO();

        ApiResponse<Void> response = controller.update(id, permissionDTO);

        assertEquals(200, response.code());
        assertNull(permissionDTO.getId());
        verify(permissionService).updatePermission(id, permissionDTO);
    }

    @Test
    void findPermissionsByUrlKeepsRawListResponse() {
        List<String> permissions = List.of("system:user:list");
        when(permissionService.findPermissionsByUrl("/api/system/users", "GET")).thenReturn(permissions);

        List<String> response = controller.findPermissionsByUrl("/api/system/users", "GET");

        assertSame(permissions, response);
    }

    @Test
    void findApiPermissionsKeepsRawListResponse() {
        List<ApiPermissionDTO> permissions = List.of(ApiPermissionDTO.builder()
                .apiPath("/api/system/users")
                .httpMethod("GET")
                .permissionCode("system:user:list")
                .build());
        when(permissionService.findApiPermissions()).thenReturn(permissions);

        List<ApiPermissionDTO> response = controller.findApiPermissions();

        assertSame(permissions, response);
    }

    @Test
    void treeKeepsApiResponseWrapper() {
        List<PermissionDTO> tree = List.of(new PermissionDTO());
        when(permissionService.getPermissionTree()).thenReturn(tree);

        ApiResponse<List<PermissionDTO>> response = controller.tree();

        assertEquals(200, response.code());
        assertSame(tree, response.data());
    }
}
