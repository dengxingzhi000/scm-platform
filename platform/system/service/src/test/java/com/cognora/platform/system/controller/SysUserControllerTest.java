package com.cognora.platform.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.dto.user.ChangePasswordRequest;
import com.cognora.platform.common.dto.user.UserDTO;
import com.cognora.platform.common.domain.PageResult;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.system.service.command.ISysUserCommandService;
import com.cognora.platform.system.service.command.IUserRoleCommandService;
import com.cognora.platform.system.service.query.ISysUserQueryService;
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
class SysUserControllerTest {

    @Mock
    private ISysUserQueryService userQueryService;

    @Mock
    private ISysUserCommandService userCommandService;

    @Mock
    private IUserRoleCommandService userRoleCommandService;

    @InjectMocks
    private SysUserController controller;

    @Test
    void forceLogoutDelegatesToCommandService() {
        UUID userId = UUID.randomUUID();

        ApiResponse<Void> response = controller.forceLogout(userId, "security policy");

        assertEquals(200, response.code());
        verify(userCommandService).forceLogout(userId, "security policy");
    }

    @Test
    void updateDelegatesIdBindingToCommandService() {
        UUID userId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO();

        ApiResponse<Void> response = controller.update(userId, userDTO);

        assertEquals(200, response.code());
        assertNull(userDTO.getId());
        verify(userCommandService).updateUser(userId, userDTO);
    }

    @Test
    void changePasswordDelegatesWithoutResolvingUserInController() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old-pass");
        request.setNewPassword("new-pass");

        ApiResponse<Void> response = controller.changePassword(request);

        assertEquals(200, response.code());
        verify(userCommandService).changePassword("old-pass", "new-pass");
    }

    @Test
    void updateLastLoginDelegatesTimestampGenerationToCommandService() {
        UUID userId = UUID.randomUUID();

        ApiResponse<Void> response = controller.updateLastLogin(userId, "10.0.0.1");

        assertEquals(200, response.code());
        verify(userCommandService).updateLastLogin(userId, "10.0.0.1");
    }

    @Test
    void listWrapsPageResult() {
        UserDTO user = new UserDTO();
        user.setUsername("tester");
        Page<UserDTO> page = new Page<>(1, 10, 25);
        page.setRecords(List.of(user));
        when(userQueryService.listUsers(1, 10, null, null)).thenReturn(page);

        ApiResponse<PageResult<UserDTO>> response = controller.list(1, 10, null, null);

        assertEquals(200, response.code());
        assertEquals(25L, response.data().getTotal());
        assertEquals(1, response.data().getRecords().size());
    }
}