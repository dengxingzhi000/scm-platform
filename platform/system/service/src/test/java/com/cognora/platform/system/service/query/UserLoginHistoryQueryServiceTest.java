package com.cognora.platform.system.service.query;

import com.cognora.platform.common.domain.PageResult;
import com.cognora.platform.common.dto.user.LoginHistoryDTO;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.system.mapper.UserLoginHistoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserLoginHistoryQueryServiceTest {

    private UserLoginHistoryMapper mapper;
    private UserLoginHistoryQueryService service;

    @BeforeEach
    void setUp() {
        mapper = mock(UserLoginHistoryMapper.class);
        service = new UserLoginHistoryQueryService(mapper);
        TenantContextHolder.setTenantId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void defaultsToLastThirtyDays() {
        UUID userId = UUID.randomUUID();
        when(mapper.count(any(), any(), any(), any(), any())).thenReturn(3L);
        when(mapper.selectPage(any(), any(), any(), any(), any(), anyLong(), anyInt()))
                .thenReturn(List.of(new LoginHistoryDTO()));

        PageResult<LoginHistoryDTO> result = service.query(userId, null, null, null, 1, 20);

        assertEquals(3L, result.getTotal().longValue());
        assertEquals(1, result.getRecords().size());
        verify(mapper).count(any(), any(), any(), any(), any());
    }

    @Test
    void rejectsRangeOverOneHundredEightyDays() {
        UUID userId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().minusDays(181);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.query(userId, start, LocalDateTime.now(), null, 1, 20));
        assertEquals(422, ex.getCode());
    }

    @Test
    void rejectsStartAfterEnd() {
        UUID userId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(1);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.query(userId, start, LocalDateTime.now(), null, 1, 20));
        assertEquals(422, ex.getCode());
    }

    @Test
    void capsPageSizeAtHundred() {
        UUID userId = UUID.randomUUID();
        when(mapper.count(any(), any(), any(), any(), any())).thenReturn(0L);
        when(mapper.selectPage(any(), any(), any(), any(), any(), anyLong(), anyInt()))
                .thenReturn(List.of());

        PageResult<LoginHistoryDTO> result = service.query(userId, null, null, null, 1, 500);

        assertEquals(100L, result.getPageSize().longValue());
    }
}
