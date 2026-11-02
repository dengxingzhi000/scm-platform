package com.scmcloud.system.service.command;

import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.rest.client.SysAuthServiceClient;
import com.scmcloud.common.security.PermissionChecker;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.event.DataSyncEventPublisher;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.service.query.RoleCrossDatabaseQueryService;
import com.scmcloud.system.service.query.UserCrossDatabaseQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserCommandServiceImplTest {

    @Mock private SysUserMapper userMapper;
    @Mock private UserCrossDatabaseQueryService userQueryService;
    @Mock private RoleCrossDatabaseQueryService roleQueryService;
    @Mock private UserRoleCrossDatabaseCommandService userRoleCommandService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private DataSyncEventPublisher dataSyncEventPublisher;
    @Mock private PermissionChecker permissionChecker;
    @Mock private StatusValidator statusValidator;
    @Mock private UserCommandTemplate commandTemplate;
    @Mock private SysAuthServiceClient authServiceClient;

    @InjectMocks
    private SysUserCommandServiceImpl service;

    @Test
    void forceLogoutDelegatesToAuthServiceClient() {
        UUID userId = UUID.randomUUID();

        service.forceLogout(userId, "security policy");

        verify(authServiceClient).forceLogout(userId, "security policy");
    }

    @Test
    void updateUserBindsPathVariableIdToDto() {
        UUID userId = UUID.randomUUID();
        UserDTO userDTO = new UserDTO();
        when(commandTemplate.execute(eq("UPDATE"), eq("user:update"), eq(userId), any()))
                .thenReturn(new SysUser());

        service.updateUser(userId, userDTO);

        assertEquals(userId, userDTO.getId());
        verify(commandTemplate).execute(eq("UPDATE"), eq("user:update"), eq(userId), any());
    }
}
