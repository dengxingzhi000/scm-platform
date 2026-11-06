package com.cognora.platform.inventory.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.ReservationStatus;
import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import com.cognora.platform.inventory.lock.DistributedLock;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.mapper.InvReservationRecordMapper;
import com.cognora.platform.inventory.service.cost.InventoryCostLogService;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预占状态机与幂等用例。
 *
 * <p>覆盖：重复确认、重复释放、确认后释放被拒、释放后确认被拒、
 * 过期释放只归还一次库存。
 */
class InvReservationStateMachineTest {

    private static final String TENANT = UUID.randomUUID().toString();

    private InvInventoryMapper inventoryMapper;
    private InvReservationRecordMapper reservationRecordMapper;
    private InvReservationServiceImpl service;

    private InvReservationRecord record;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(UUID.fromString(TENANT));

        inventoryMapper = mock(InvInventoryMapper.class);
        reservationRecordMapper = mock(InvReservationRecordMapper.class);
        service = new InvReservationServiceImpl(inventoryMapper, reservationRecordMapper,
                mock(DistributedLock.class), mock(InventoryFlowLogService.class),
                mock(InventoryCostLogService.class));

        record = new InvReservationRecord();
        record.setId("22222222-2222-2222-2222-222222222222");
        record.setTenantId(TENANT);
        record.setBusinessKey("ORD-1");
        record.setSkuId("sku-1");
        record.setWarehouseId("wh-1");
        record.setInventoryId("11111111-1111-1111-1111-111111111111");
        record.setQuantity(5);
        record.setStatus(ReservationStatus.RESERVED.name());
        record.setExpireAt(LocalDateTime.now().plusMinutes(10));
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("重复确认幂等：只实扣一次")
    void confirmTwiceIsIdempotent() {
        when(reservationRecordMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(record, confirmed());
        when(reservationRecordMapper.markConfirmed(record.getId(), TENANT)).thenReturn(1);
        when(inventoryMapper.confirmStock(record.getInventoryId(), 5)).thenReturn(1);

        assertTrue(service.confirmReservation("ORD-1"));
        assertTrue(service.confirmReservation("ORD-1"));

        verify(reservationRecordMapper, times(1)).markConfirmed(record.getId(), TENANT);
        verify(inventoryMapper, times(1)).confirmStock(record.getInventoryId(), 5);
    }

    @Test
    @DisplayName("释放后确认被拒绝，且不触碰锁定库存")
    void confirmAfterReleaseIsRejected() {
        record.setStatus(ReservationStatus.RELEASED.name());
        when(reservationRecordMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(record);

        assertFalse(service.confirmReservation("ORD-1"));
        verify(inventoryMapper, never()).confirmStock(anyString(), eq(5));
    }

    @Test
    @DisplayName("重复释放幂等：只归还一次")
    void releaseTwiceIsIdempotent() {
        when(reservationRecordMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(record, released());
        when(reservationRecordMapper.markReleased(record.getId(), TENANT)).thenReturn(1);
        when(inventoryMapper.releaseStock(record.getInventoryId(), 5)).thenReturn(1);

        assertTrue(service.releaseReservation("ORD-1"));
        assertTrue(service.releaseReservation("ORD-1"));

        verify(reservationRecordMapper, times(1)).markReleased(record.getId(), TENANT);
        verify(inventoryMapper, times(1)).releaseStock(record.getInventoryId(), 5);
    }

    @Test
    @DisplayName("确认后释放被拒绝，锁定库存不会被二次归还")
    void releaseAfterConfirmIsRejected() {
        record.setStatus(ReservationStatus.CONFIRMED.name());
        when(reservationRecordMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(record);

        assertFalse(service.releaseReservation("ORD-1"));
        verify(inventoryMapper, never()).releaseStock(anyString(), eq(5));
    }

    @Test
    @DisplayName("过期预占释放库存且只归还一次")
    void expiredReservationIsReleasedOnce() {
        when(reservationRecordMapper.selectExpired(200)).thenReturn(List.of(record), Collections.emptyList());
        when(reservationRecordMapper.markExpired(record.getId(), TENANT)).thenReturn(1);
        when(inventoryMapper.releaseStock(record.getInventoryId(), 5)).thenReturn(1);

        assertEquals(1, service.releaseExpiredReservations());
        assertEquals(0, service.releaseExpiredReservations());

        verify(inventoryMapper, times(1)).releaseStock(record.getInventoryId(), 5);
    }

    @Test
    @DisplayName("预占记录不存在时确认/释放均返回 false")
    void missingRecordReturnsFalse() {
        when(reservationRecordMapper.selectByBusinessKey(anyString(), anyString())).thenReturn(null);

        assertFalse(service.confirmReservation("ORD-404"));
        assertFalse(service.releaseReservation("ORD-404"));
        verify(inventoryMapper, never()).confirmStock(anyString(), eq(5));
        verify(inventoryMapper, never()).releaseStock(anyString(), eq(5));
    }

    @Test
    @DisplayName("状态机只允许 RESERVED 流转到终态")
    void stateMachineOnlyAllowsReservedToTerminal() {
        assertTrue(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.CONFIRMED));
        assertTrue(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.RELEASED));
        assertTrue(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.EXPIRED));

        assertFalse(ReservationStatus.CONFIRMED.canTransitionTo(ReservationStatus.RELEASED));
        assertFalse(ReservationStatus.RELEASED.canTransitionTo(ReservationStatus.RESERVED));
        assertFalse(ReservationStatus.EXPIRED.canTransitionTo(ReservationStatus.CONFIRMED));
    }

    private InvReservationRecord confirmed() {
        InvReservationRecord copy = copyOf(record);
        copy.setStatus(ReservationStatus.CONFIRMED.name());
        return copy;
    }

    private InvReservationRecord released() {
        InvReservationRecord copy = copyOf(record);
        copy.setStatus(ReservationStatus.RELEASED.name());
        return copy;
    }

    private InvReservationRecord copyOf(InvReservationRecord source) {
        InvReservationRecord copy = new InvReservationRecord();
        copy.setId(source.getId());
        copy.setTenantId(source.getTenantId());
        copy.setBusinessKey(source.getBusinessKey());
        copy.setSkuId(source.getSkuId());
        copy.setWarehouseId(source.getWarehouseId());
        copy.setInventoryId(source.getInventoryId());
        copy.setQuantity(source.getQuantity());
        copy.setStatus(source.getStatus());
        copy.setExpireAt(source.getExpireAt());
        return copy;
    }
}
