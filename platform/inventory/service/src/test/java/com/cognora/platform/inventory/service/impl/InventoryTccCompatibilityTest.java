package com.cognora.platform.inventory.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.tenant.TenantParseException;
import com.cognora.platform.inventory.domain.entity.InvTccReservation;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.service.cost.InventoryCostLogService;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import com.cognora.platform.inventory.mapper.InvTccReservationMapper;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
 * TCC 的兼容性与边界用例：
 * businessKey 空值、缺租户、跨租户同单号、Long SKU 与 UUID 台账解析、
 * 以及 Confirm 后库存恒等式依赖的 SQL 语义。
 */
class InventoryTccCompatibilityTest {

    private static final String INVENTORY_ID = "11111111-1111-1111-1111-111111111111";
    private static final String SKU_UUID = "22222222-2222-2222-2222-222222222222";
    private static final String WAREHOUSE_ID = "33333333-3333-3333-3333-333333333333";

    private InvInventoryMapper inventoryMapper;
    private InvTccReservationMapper reservationMapper;
    private InventoryTccServiceImpl service;
    private Inventory ledger;

    @BeforeEach
    void setUp() {
        inventoryMapper = mock(InvInventoryMapper.class);
        reservationMapper = mock(InvTccReservationMapper.class);
        service = new InventoryTccServiceImpl(inventoryMapper, reservationMapper,
                mock(InventoryFlowLogService.class), mock(InventoryCostLogService.class));

        ledger = new Inventory();
        ledger.setId(INVENTORY_ID);
        ledger.setSkuId(SKU_UUID);
        ledger.setWarehouseId(WAREHOUSE_ID);
        ledger.setAvailableStock(50);
        ledger.setLockedStock(0);
        ledger.setTotalStock(50);

        TenantContextHolder.setTenantId(UUID.randomUUID());
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("Confirm 缺少 businessKey 抛参数异常，不产生 NPE")
    void confirmWithoutBusinessKeyDoesNotThrowNpe() {
        BusinessActionContext noKey = new BusinessActionContext("xid-1", "1", new HashMap<>());
        BusinessActionContext nullMap = new BusinessActionContext("xid-1", "1", null);

        assertThrows(IllegalArgumentException.class, () -> service.confirmReserve(noKey));
        assertThrows(IllegalArgumentException.class, () -> service.confirmReserve(nullMap));
    }

    @Test
    @DisplayName("Cancel 缺少 businessKey 同样抛参数异常")
    void cancelWithoutBusinessKeyIsRejected() {
        BusinessActionContext noKey = new BusinessActionContext("xid-1", "1", new HashMap<>());
        assertThrows(IllegalArgumentException.class, () -> service.cancelReserve(noKey));
    }

    @Test
    @DisplayName("Try 的 businessKey 为空时拒绝，不写库")
    void tryWithBlankBusinessKeyIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.reserveInventory(1L, 1, "  "));
        verify(reservationMapper, never()).insert(any(InvTccReservation.class));
    }

    @Test
    @DisplayName("缺少租户上下文时 confirm/cancel/try 全部 fail-closed")
    void missingTenantFailsClosed() {
        TenantContextHolder.clear();
        Map<String, Object> params = new HashMap<>();
        params.put("businessKey", "ORD-1");
        BusinessActionContext context = new BusinessActionContext("xid-1", "1", params);

        assertThrows(TenantParseException.class, () -> service.confirmReserve(context));
        assertThrows(TenantParseException.class, () -> service.cancelReserve(context));
        assertThrows(TenantParseException.class, () -> service.reserveInventory(1L, 1, "ORD-1"));
    }

    @Test
    @DisplayName("跨租户确认他人预留抛 403，不扣库存")
    void confirmForeignTenantReservationIsDenied() {
        InvTccReservation foreign = new InvTccReservation();
        foreign.setId(9L);
        foreign.setTenantId(UUID.randomUUID().toString());
        foreign.setBusinessKey("ORD-1");
        foreign.setInventoryId(INVENTORY_ID);
        foreign.setQuantity(2);
        foreign.setStatus(InvTccReservation.Status.TRYING);
        when(reservationMapper.selectByBusinessKey(anyString(), anyString())).thenReturn(foreign);

        assertThrows(AccessDeniedException.class, () -> service.confirmReserve(context("ORD-1")));
        assertThrows(AccessDeniedException.class, () -> service.cancelReserve(context("ORD-1")));
        verify(inventoryMapper, never()).confirmStock(anyString(), anyInt());
        verify(inventoryMapper, never()).releaseStock(anyString(), anyInt());
    }

    @Test
    @DisplayName("相同业务键在不同租户下各自插入成功")
    void sameBusinessKeyIsSeparatedByTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        Map<String, InvTccReservation> records = new HashMap<>();
        List<InvTccReservation> inserted = new ArrayList<>();

        when(reservationMapper.selectByBusinessKey(anyString(), anyString()))
                .thenAnswer(invocation -> records.get(
                        invocation.getArgument(0) + ":" + invocation.getArgument(1)));
        when(reservationMapper.insert(any(InvTccReservation.class))).thenAnswer(invocation -> {
            InvTccReservation record = invocation.getArgument(0);
            records.put(record.getTenantId() + ":" + record.getBusinessKey(), record);
            inserted.add(record);
            return 1;
        });
        when(inventoryMapper.selectByLegacySkuId(anyString(), anyLong())).thenReturn(List.of(ledger));
        when(inventoryMapper.lockStock(anyString(), anyInt())).thenReturn(1);

        TenantContextHolder.setTenantId(tenantA);
        assertTrue(service.reserveInventory(1001L, 2, "ORD-1"));

        TenantContextHolder.setTenantId(tenantB);
        assertTrue(service.reserveInventory(1001L, 3, "ORD-1"));

        assertEquals(2, inserted.size());
        assertEquals(2, records.size());
        verify(inventoryMapper, times(2)).lockStock(anyString(), anyInt());
    }

    @Test
    @DisplayName("同一数字 SKU 命中多仓台账行时拒绝，避免扣错仓库")
    void ambiguousLegacySkuIsRejected() {
        Inventory second = new Inventory();
        second.setId("44444444-4444-4444-4444-444444444444");
        second.setSkuId(SKU_UUID);
        second.setWarehouseId("55555555-5555-5555-5555-555555555555");
        when(inventoryMapper.selectByLegacySkuId(anyString(), anyLong()))
                .thenReturn(List.of(ledger, second));

        assertThrows(IllegalStateException.class,
                () -> service.reserveInventory(1001L, 1, "ORD-MULTI"));
        verify(inventoryMapper, never()).lockStock(anyString(), anyInt());
    }

    @Test
    @DisplayName("UUID 版 Try 写入 inventory_id 与 warehouse_id")
    void uuidTryPersistsLedgerIdentity() {
        when(inventoryMapper.selectBySkuUuidAndWarehouse(anyString(), anyString(), anyString()))
                .thenReturn(ledger);
        when(inventoryMapper.lockStock(INVENTORY_ID, 4)).thenReturn(1);
        when(reservationMapper.insert(any(InvTccReservation.class))).thenReturn(1);

        assertTrue(service.reserveInventoryBySku(SKU_UUID, WAREHOUSE_ID, 4, "ORD-UUID"));

        verify(inventoryMapper).lockStock(INVENTORY_ID, 4);
        verify(reservationMapper).insert(org.mockito.ArgumentMatchers.<InvTccReservation>argThat(r ->
                INVENTORY_ID.equals(r.getInventoryId())
                        && SKU_UUID.equals(r.getSkuUuid())
                        && WAREHOUSE_ID.equals(r.getWarehouseId())
                        && InvTccReservation.Status.TRYING.equals(r.getStatus())));
    }

    @Test
    @DisplayName("Confirm/Cancel 的 SQL 同时维护 locked 与 total，保证恒等式")
    void inventorySqlMaintainsBalanceIdentity() throws NoSuchMethodException {
        Method confirm = InvInventoryMapper.class.getMethod(
                "confirmStock", String.class, int.class);
        String confirmSql = String.join("", confirm.getAnnotation(Update.class).value());

        Method release = InvInventoryMapper.class.getMethod(
                "releaseStock", String.class, int.class);
        String releaseSql = String.join("", release.getAnnotation(Update.class).value());

        assertTrue(confirmSql.contains("locked_stock = locked_stock - #{quantity}"),
                "Confirm 必须减少锁定库存");
        assertTrue(confirmSql.contains("total_stock = total_stock - #{quantity}"),
                "Confirm 必须同步减少总库存，否则违反 total=available+locked+damaged");
        assertTrue(releaseSql.contains("available_stock = available_stock + #{quantity}"),
                "Cancel 必须归还可用库存");
        assertTrue(releaseSql.contains("locked_stock = locked_stock - #{quantity}"),
                "Cancel 必须减少锁定库存");
    }

    @Test
    @DisplayName("Confirm 通过 inventory_id 精确扣减，而非按 sku_id")
    void confirmUsesInventoryId() {
        InvTccReservation record = new InvTccReservation();
        record.setId(7L);
        record.setTenantId(TenantContextHolder.getRequiredTenantId().toString());
        record.setBusinessKey("ORD-1");
        record.setInventoryId(INVENTORY_ID);
        record.setSkuId(1001L);
        record.setQuantity(2);
        record.setStatus(InvTccReservation.Status.TRYING);

        when(reservationMapper.selectByBusinessKey(anyString(), eq("ORD-1"))).thenReturn(record);
        when(reservationMapper.markConfirmed(7L, record.getTenantId())).thenReturn(1);
        when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(ledger);
        when(inventoryMapper.confirmStock(INVENTORY_ID, 2)).thenReturn(1);

        assertDoesNotThrow(() -> service.confirmReserve(context("ORD-1")));
        assertNotNull(record.getInventoryId());
        verify(inventoryMapper).confirmStock(INVENTORY_ID, 2);
    }

    private static BusinessActionContext context(String businessKey) {
        Map<String, Object> params = new HashMap<>();
        params.put("businessKey", businessKey);
        params.put("skuId", 1001L);
        params.put("quantity", 2);
        return new BusinessActionContext("xid-1", "1", params);
    }
}
