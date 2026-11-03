package com.cognora.platform.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.domain.PageResult;
import com.cognora.platform.common.dto.role.RoleDTO;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.system.service.ISysRoleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysRoleControllerTest {

    @Mock
    private ISysRoleService roleService;

    @InjectMocks
    private SysRoleController controller;

    @Test
    void updateDelegatesIdBindingToService() {
        UUID id = UUID.randomUUID();
        RoleDTO roleDTO = new RoleDTO();

        ApiResponse<Void> response = controller.update(id, roleDTO);

        assertEquals(200, response.code());
        assertNull(roleDTO.getId());
        verify(roleService).updateRole(id, roleDTO);
    }

    @Test
    void listWrapsPageResult() {
        RoleDTO role = new RoleDTO();
        role.setRoleName("admin");
        Page<RoleDTO> page = new Page<>(1, 10, 7);
        page.setRecords(List.of(role));
        when(roleService.listRoles(1, 10, null)).thenReturn(page);

        ApiResponse<PageResult<RoleDTO>> response = controller.list(1, 10, null);

        assertEquals(200, response.code());
        assertEquals(7L, response.data().getTotal());
        assertEquals(1, response.data().getRecords().size());
    }
}
