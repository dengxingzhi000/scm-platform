package com.cognora.platform.system.service.command;

import com.cognora.platform.common.dto.user.BatchOperationResult;
import com.cognora.platform.common.dto.user.BatchUserRequest;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.status.StatusValidator;
import com.cognora.platform.system.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BatchUserOperationServiceTest {

    private SysUserMapper userMapper;
    private UserCommandTemplate commandTemplate;
    private StatusValidator statusValidator;
    private ISysUserCommandService userCommandService;
    private BatchUserCommandService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(SysUserMapper.class);
        commandTemplate = mock(UserCommandTemplate.class);
        statusValidator = mock(StatusValidator.class);
        userCommandService = mock(ISysUserCommandService.class);
        service = new BatchUserCommandService(userMapper, commandTemplate, statusValidator, userCommandService);
    }

    @Test
    void deleteReturnsPartialSuccessWhenOneRowFails() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        doThrow(new BusinessException(1007, "cannot delete admin"))
                .when(userCommandService).deleteUser(first);

        BatchUserRequest request = new BatchUserRequest();
        request.setIds(List.of(first, second));

        BatchOperationResult result = service.delete(request);

        assertEquals(2, result.getTotal());
        assertEquals(1, result.getSucceeded());
        assertEquals(1, result.getFailed());
        verify(userCommandService).deleteUser(first);
        verify(userCommandService).deleteUser(second);
    }

    @Test
    void transferWithoutDeptIsRejectedBeforeAnyRowRuns() {
        BatchUserRequest request = new BatchUserRequest();
        request.setIds(List.of(UUID.randomUUID()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.transferDept(request));
        assertEquals(422, ex.getCode());
    }

    @Test
    void grantWithoutRolesIsRejected() {
        BatchUserRequest request = new BatchUserRequest();
        request.setIds(List.of(UUID.randomUUID()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.grantRoles(request));
        assertEquals(422, ex.getCode());
    }
}
