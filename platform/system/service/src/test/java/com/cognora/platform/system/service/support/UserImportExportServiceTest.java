package com.cognora.platform.system.service.support;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.dto.user.CreateUserRequest;
import com.cognora.platform.common.dto.user.UserImportResult;
import com.cognora.platform.common.dto.user.UserQueryRequest;
import com.cognora.platform.common.dto.user.UserSummaryDTO;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.system.mapper.SysUserMapper;
import com.cognora.platform.system.service.command.ISysUserCommandService;
import com.cognora.platform.system.service.query.ISysUserQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserImportExportServiceTest {

    private ISysUserCommandService commandService;
    private ISysUserQueryService queryService;
    private SysUserMapper userMapper;
    private PasswordPolicyService passwordPolicyService;
    private PermissionChecker permissionChecker;
    private UserImportExportService service;

    @BeforeEach
    void setUp() {
        commandService = mock(ISysUserCommandService.class);
        queryService = mock(ISysUserQueryService.class);
        userMapper = mock(SysUserMapper.class);
        passwordPolicyService = mock(PasswordPolicyService.class);
        permissionChecker = mock(PermissionChecker.class);
        service = new UserImportExportService(commandService, queryService, userMapper,
                passwordPolicyService, permissionChecker);
        TenantContextHolder.setTenantId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void dryRunValidatesWithoutCreatingUsers() {
        when(userMapper.existsByUsername(any(), anyString())).thenReturn(false);
        MockMultipartFile file = csv("username,password,realName,email,phone,deptId,remark\n"
                + "zhangsan,Abcd1234!,张三,zhangsan@example.com,13800000000,,\n");

        UserImportResult result = service.importUsers(file, true);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getCreated());
        assertEquals(0, result.getFailed());
        verify(commandService, never()).addUser(any(CreateUserRequest.class));
    }

    @Test
    void duplicateUsernameIsReportedPerRow() {
        when(userMapper.existsByUsername(any(), anyString())).thenReturn(true);
        MockMultipartFile file = csv("username,password,realName\nzhangsan,Abcd1234!,张三\n");

        UserImportResult result = service.importUsers(file, true);

        assertEquals(1, result.getTotal());
        assertEquals(0, result.getCreated());
        assertEquals(1, result.getFailed());
        assertEquals(1006, result.getErrors().get(0).getCode());
    }

    @Test
    void exportMasksSensitiveFieldsByDefault() {
        UserSummaryDTO user = new UserSummaryDTO();
        user.setUsername("zhangsan");
        user.setRealName("张三");
        user.setPhone("13800000000");
        user.setEmail("zhangsan@example.com");
        user.setStatus(1);
        Page<UserSummaryDTO> page = new Page<>(1, 100, 1);
        page.setRecords(List.of(user));
        when(queryService.listUsers(any())).thenReturn(page);
        when(permissionChecker.hasPermission(any(), anyString())).thenReturn(false);

        String csv = new String(service.export(new UserQueryRequest(), false), StandardCharsets.UTF_8);

        assertTrue(csv.contains("138****0000"), csv);
        // SensitiveType.NAME 对两字姓名只保留姓 + 一个星号。
        assertTrue(csv.contains("张*"), csv);
    }

    @Test
    void exportRevealsSensitiveFieldsOnlyWithExtraPermission() {
        UserSummaryDTO user = new UserSummaryDTO();
        user.setUsername("zhangsan");
        user.setPhone("13800000000");
        Page<UserSummaryDTO> page = new Page<>(1, 100, 1);
        page.setRecords(List.of(user));
        when(queryService.listUsers(any())).thenReturn(page);
        when(permissionChecker.hasPermission(any(), anyString())).thenReturn(true);

        String csv = new String(service.export(new UserQueryRequest(), true), StandardCharsets.UTF_8);

        assertTrue(csv.contains("13800000000"), csv);
    }

    private MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "users.csv", "text/csv",
                content.getBytes(StandardCharsets.UTF_8));
    }
}
