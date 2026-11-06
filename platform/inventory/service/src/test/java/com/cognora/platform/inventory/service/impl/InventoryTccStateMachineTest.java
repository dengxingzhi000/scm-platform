package com.cognora.platform.inventory.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.InvTccReservation;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.service.cost.InventoryCostLogService;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import com.cognora.platform.inventory.mapper.InvTccReservationMapper;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TCC 状态机与幂等用例。
 *
 * <p>状态机只允许 TRYING→CONFIRMED / TRYING→CANCELLED：
 * CONFIRMED 之后 Cancel、CANCELLED 之后 Confirm 都必须被拒绝，
 * 且不会触碰到库存。
 */
class InventoryTccStateMachineTest {

    private static final String TENANT = UUID.randomUUID().toString();
    private static final String INVENTORY_ID = "11111111-1111-1111-1111-111111111111";

    private InvInventoryMapper inventoryMapper;
    private InvTccReservationMapper reservationMapper;
    private InventoryTccServiceImpl service;

    private InvTccReservation reservation;
    private Inventory ledger;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(UUID.fromString(TENANT));

        inventoryMapper = mock(InvInventoryMapper.class);
        reservationMapper = mock(InvTccReservationMapper.class);
        service = new InventoryTccServiceImpl(inventoryMapper, reservationMapper,
                mock(InventoryFlowLogService.class), mock(InventoryCostLogService.class));

        ledger = new Inventory();
        ledger.setId(INVENTORY_ID);
        ledger.setTenantId(TENANT);
        ledger.setSkuId("22222222-2222-2222-2222-222222222222");
        ledger.setWarehouseId("33333333-3333-3333-3333-333333333333");
        ledger.setAvailableStock(100);
        ledger.setLockedStock(5);
        ledger.setTotalStock(100);
        when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(ledger);

        reservation = new InvTccReservation();
        reservation.setId(1L);
        reservation.setTenantId(TENANT);
        reservation.setBusinessKey("ORD-1");
        reservation.setSkuId(1L);
        reservation.setSkuUuid(ledger.getSkuId());
        reservation.setInventoryId(INVENTORY_ID);
        reservation.setWarehouseId(ledger.getWarehouseId());
        reservation.setQuantity(5);
        reservation.setStatus(InvTccReservation.Status.TRYING);
        reservation.setTryTime(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("重复 Confirm 幂等：只实扣一次")
    void confirmTwiceDeductsOnce() {
        when(reservationMapper.selectByBusinessKey(TENANT, "ORD-1"))
                .thenReturn(reservation, confirmedCopy());
        when(reservationMapper.markConfirmed(1L, TENANT)).thenReturn(1);
        when(inventoryMapper.confirmStock(INVENTORY_ID, 5)).thenReturn(1);

        assertTrue(service.confirmReserve(context("ORD-1")));
        assertTrue(service.confirmReserve(context("ORD-1")));

        verify(reservationMapper, times(1)).markConfirmed(1L, TENANT);
        verify(inventoryMapper, times(1)).confirmStock(INVENTORY_ID, 5);
    }

    @Test
    @DisplayName("重复 Cancel 幂等：只归还一次")
    void cancelTwiceReleasesOnce() {
        when(reservationMapper.selectByBusinessKey(TENANT, "ORD-1"))
                .thenReturn(reservation, cancelledCopy());
        when(reservationMapper.markCancelled(1L, TENANT)).thenReturn(1);
        when(inventoryMapper.releaseStock(INVENTORY_ID, 5)).thenReturn(1);

        assertTrue(service.cancelReserve(context("ORD-1")));
        assertTrue(service.cancelReserve(context("ORD-1")));

        verify(reservationMapper, times(1)).markCancelled(1L, TENANT);
        verify(inventoryMapper, times(1)).releaseStock(INVENTORY_ID, 5);
    }

    @Test
    @DisplayName("已取消的预留不能再被 Confirm，且不扣库存")
    void confirmAfterCancelIsRejected() {
        InvTccReservation cancelled = cancelledCopy();
        when(reservationMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(cancelled);

        assertFalse(service.confirmReserve(context("ORD-1")));
        verify(inventoryMapper, never()).confirmStock(anyString(), anyInt());
        verify(reservationMapper, never()).markConfirmed(anyLong(), anyString());
    }

    @Test
    @DisplayName("已确认的预留不能再被 Cancel，且不归还库存")
    void cancelAfterConfirmIsRejected() {
        InvTccReservation confirmed = confirmedCopy();
        when(reservationMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(confirmed);

        assertFalse(service.cancelReserve(context("ORD-1")));
        verify(inventoryMapper, never()).releaseStock(anyString(), anyInt());
        verify(reservationMapper, never()).markCancelled(anyLong(), anyString());
    }

    @Test
    @DisplayName("Try 记录不存在时 Confirm 返回 false（防悬挂）")
    void confirmWithoutTryReturnsFalse() {
        when(reservationMapper.selectByBusinessKey(TENANT, "ORD-404")).thenReturn(null);

        assertFalse(service.confirmReserve(context("ORD-404")));
        verify(inventoryMapper, never()).confirmStock(anyString(), anyInt());
    }

    @Test
    @DisplayName("Cancel 空回滚写入 CANCELLED 记录，阻止后续 Try 悬挂")
    void cancelWithoutTryWritesTombstone() {
        when(reservationMapper.selectByBusinessKey(TENANT, "ORD-404")).thenReturn(null);
        when(reservationMapper.insert(org.mockito.ArgumentMatchers.any(InvTccReservation.class)))
                .thenReturn(1);

        assertTrue(service.cancelReserve(context("ORD-404")));
        verify(reservationMapper, times(1))
                .insert(org.mockito.ArgumentMatchers.<InvTccReservation>argThat(r ->
                        InvTccReservation.Status.CANCELLED.equals(r.getStatus())
                                && "ORD-404".equals(r.getBusinessKey())));
    }

    @Test
    @DisplayName("终态业务键不允许重新 Try，且不重复锁定库存")
    void tryOnCancelledBusinessKeyIsRejected() {
        when(reservationMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(cancelledCopy());

        assertFalse(service.reserveInventory(1L, 5, "ORD-1"));
        verify(inventoryMapper, never()).lockStock(anyString(), anyInt());
    }

    @Test
    @DisplayName("TRYING 记录存在时重复 Try 幂等返回，不重复锁定")
    void repeatedTryIsIdempotent() {
        when(reservationMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(reservation);

        assertTrue(service.reserveInventory(1L, 5, "ORD-1"));
        verify(inventoryMapper, never()).lockStock(anyString(), anyInt());
        verify(reservationMapper, never()).insert(org.mockito.ArgumentMatchers.any(InvTccReservation.class));
    }

    @Test
    @DisplayName("Confirm 使用持久化数量，不信任 action context 里的数量")
    void confirmUsesPersistedQuantity() {
        reservation.setQuantity(3);
        when(reservationMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(reservation);
        when(reservationMapper.markConfirmed(1L, TENANT)).thenReturn(1);
        when(inventoryMapper.confirmStock(INVENTORY_ID, 3)).thenReturn(1);

        Map<String, Object> params = new HashMap<>();
        params.put("businessKey", "ORD-1");
        params.put("quantity", 9);
        BusinessActionContext tampered = new BusinessActionContext("xid-1", "1", params);

        assertTrue(service.confirmReserve(tampered));
        verify(inventoryMapper).confirmStock(eq(INVENTORY_ID), eq(3));
        assertEquals(3, reservation.getQuantity());
    }

    private BusinessActionContext context(String businessKey) {
        Map<String, Object> params = new HashMap<>();
        params.put("businessKey", businessKey);
        params.put("skuId", 1L);
        params.put("quantity", 5);
        return new BusinessActionContext("xid-1", "1", params);
    }

    private InvTccReservation confirmedCopy() {
        return copyWithStatus(InvTccReservation.Status.CONFIRMED);
    }

    private InvTccReservation cancelledCopy() {
        return copyWithStatus(InvTccReservation.Status.CANCELLED);
    }

    private InvTccReservation copyWithStatus(String status) {
        InvTccReservation copy = new InvTccReservation();
        copy.setId(reservation.getId());
        copy.setTenantId(reservation.getTenantId());
        copy.setBusinessKey(reservation.getBusinessKey());
        copy.setSkuId(reservation.getSkuId());
        copy.setSkuUuid(reservation.getSkuUuid());
        copy.setInventoryId(reservation.getInventoryId());
        copy.setWarehouseId(reservation.getWarehouseId());
        copy.setQuantity(reservation.getQuantity());
        copy.setStatus(status);
        return copy;
    }
}
