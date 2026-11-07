package com.cognora.platform.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.dto.user.ChangePasswordRequest;
import com.cognora.platform.common.dto.user.UpdateUserRequest;
import com.cognora.platform.common.dto.user.UserDetailDTO;
import com.cognora.platform.common.dto.user.UserQueryRequest;
import com.cognora.platform.common.dto.user.UserSummaryDTO;
import com.cognora.platform.common.domain.PageResult;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.system.service.command.BatchUserCommandService;
import com.cognora.platform.system.service.command.ISysUserCommandService;
import com.cognora.platform.system.service.command.IUserRoleCommandService;
import com.cognora.platform.system.service.query.ISysUserQueryService;
import com.cognora.platform.system.service.support.UserReadAccessGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Mock
    private BatchUserCommandService batchUserCommandService;

    @Mock
    private UserReadAccessGuard userReadAccessGuard;

    @Mock
    private PermissionChecker permissionChecker;

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
    void updateDelegatesToCommandServiceAndReturnsDetail() {
        UUID userId = UUID.randomUUID();
        UpdateUserRequest request = new UpdateUserRequest();
        request.setVersion(0);

        when(userQueryService.getUserDetail(userId)).thenReturn(new UserDetailDTO());
        when(permissionChecker.hasPermission(org.mockito.ArgumentMatchers.any(), anyString()))
                .thenReturn(true);

        ApiResponse<UserDetailDTO> response = controller.update(userId, request);

        assertEquals(200, response.code());
        verify(userCommandService).updateUser(userId, request);
        verify(userQueryService).getUserDetail(userId);
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
        UserSummaryDTO user = new UserSummaryDTO();
        user.setUsername("tester");
        Page<UserSummaryDTO> page = new Page<>(1, 10, 25);
        page.setRecords(List.of(user));
        UserQueryRequest request = new UserQueryRequest();
        when(userQueryService.listUsers(request)).thenReturn(page);

        ApiResponse<PageResult<UserSummaryDTO>> response = controller.list(request);

        assertEquals(200, response.code());
        assertEquals(25L, response.data().getTotal());
        assertEquals(1, response.data().getRecords().size());
    }
}
