package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.cognora.lms.logistics.domain.entity.TmsCarrierDispatch;
import com.cognora.lms.logistics.mapper.TmsCarrierDispatchMapper;
import com.cognora.platform.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次 3：承运商下单幂等键与降级结果落库用例。
 */
class TmsCarrierDispatchServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TmsCarrierDispatchMapper mapper;
    private TmsCarrierDispatchServiceImpl service;

    @BeforeEach
    void setUp() {
        mapper = mock(TmsCarrierDispatchMapper.class);
        service = new TmsCarrierDispatchServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void beginDispatchShouldPersistRunningRecord() {
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        TmsCarrierDispatch dispatch = service.beginDispatch("key-1", "WB-1", "carrier-a");

        ArgumentCaptor<TmsCarrierDispatch> captor = ArgumentCaptor.forClass(TmsCarrierDispatch.class);
        verify(mapper).insert(captor.capture());
        TmsCarrierDispatch saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals("key-1", saved.getIdempotencyKey());
        assertEquals("WB-1", saved.getWaybillNo());
        assertEquals("carrier-a", saved.getPrimaryCarrierId());
        assertEquals(TmsCarrierDispatch.RESULT_RUNNING, saved.getResult());
        assertEquals(0, saved.getAttemptCount());
        assertSame(saved, dispatch);
    }

    @Test
    void repeatedIdempotencyKeyShouldReturnExistingRecord() {
        TmsCarrierDispatch existing = new TmsCarrierDispatch();
        existing.setId("dispatch-1");
        existing.setIdempotencyKey("key-1");
        existing.setResult(TmsCarrierDispatch.RESULT_FALLBACK);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(existing));

        TmsCarrierDispatch dispatch = service.beginDispatch("key-1", "WB-1", "carrier-a");

        assertSame(existing, dispatch);
        verify(mapper, never()).insert(any(TmsCarrierDispatch.class));
    }

    @Test
    void concurrentDuplicateInsertShouldReturnExistingRecord() {
        TmsCarrierDispatch existing = new TmsCarrierDispatch();
        existing.setId("dispatch-1");
        existing.setResult(TmsCarrierDispatch.RESULT_FALLBACK);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(), List.of(existing));
        when(mapper.insert(any(TmsCarrierDispatch.class)))
                .thenThrow(new DuplicateKeyException("uk_tms_carrier_dispatch_idem"));

        TmsCarrierDispatch dispatch = service.beginDispatch("key-1", "WB-1", "carrier-a");

        assertSame(existing, dispatch);
    }

    @Test
    void blankIdempotencyKeyShouldBeRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.beginDispatch(" ", "WB-1", "carrier-a"));
        verify(mapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void missingTenantContextShouldBeRejected() {
        TenantContextHolder.clear();

        assertThrows(AccessDeniedException.class,
                () -> service.beginDispatch("key-1", "WB-1", "carrier-a"));
        verify(mapper, never()).insert(any(TmsCarrierDispatch.class));
    }

    @Test
    void completeDispatchShouldRecordFallbackResult() {
        TmsCarrierDispatch existing = new TmsCarrierDispatch();
        existing.setId("dispatch-1");
        when(mapper.selectById("dispatch-1")).thenReturn(existing);

        TmsCarrierDispatch completed = service.completeDispatch(
                "dispatch-1", "carrier-b", 3, TmsCarrierDispatch.RESULT_FALLBACK, "carrier-a timeout");

        ArgumentCaptor<TmsCarrierDispatch> captor = ArgumentCaptor.forClass(TmsCarrierDispatch.class);
        verify(mapper).updateById(captor.capture());
        TmsCarrierDispatch patch = captor.getValue();
        assertEquals("carrier-b", patch.getActualCarrierId());
        assertEquals(3, patch.getAttemptCount());
        assertEquals(TmsCarrierDispatch.RESULT_FALLBACK, patch.getResult());
        assertEquals("carrier-a timeout", patch.getErrorMessage());
        assertEquals(TmsCarrierDispatch.RESULT_FALLBACK, completed.getResult());
        assertEquals("carrier-b", completed.getActualCarrierId());
    }

    @Test
    void completeDispatchOutsideTenantShouldReturn403() {
        when(mapper.selectById("dispatch-1")).thenReturn(null);

        assertThrows(AccessDeniedException.class,
                () -> service.completeDispatch("dispatch-1", "carrier-b", 1, TmsCarrierDispatch.RESULT_SUCCESS, null));
        verify(mapper, never()).updateById(any(TmsCarrierDispatch.class));
    }
}
