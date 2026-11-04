package com.cognora.platform.inventory.service.impl;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.tenant.TenantParseException;
import com.cognora.platform.inventory.domain.ReservationStatus;
import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InventoryReservationRequest;
import com.cognora.platform.inventory.lock.DistributedLock;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.mapper.InvReservationRecordMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预占链路的租户隔离用例。
 */
class InvReservationTenantIsolationTest {

    private InvInventoryMapper inventoryMapper;
    private InvReservationRecordMapper reservationRecordMapper;
    private InvReservationServiceImpl service;

    private final Map<String, InvReservationRecord> records = new HashMap<>();

    @BeforeEach
    void setUp() {
        inventoryMapper = mock(InvInventoryMapper.class);
        reservationRecordMapper = mock(InvReservationRecordMapper.class);
        DistributedLock distributedLock = mock(DistributedLock.class);

        Inventory inventory = new Inventory();
        inventory.setId("11111111-1111-1111-1111-111111111111");
        inventory.setSkuId("sku-1");
        inventory.setWarehouseId("wh-1");
        inventory.setAvailableStock(100);
        inventory.setTotalStock(100);
        inventory.setLockedStock(0);
        when(inventoryMapper.selectOne(any())).thenReturn(inventory);
        when(inventoryMapper.lockStock(anyString(), anyInt())).thenReturn(1);

        when(distributedLock.tryLock(anyString(), anyLong(), any(TimeUnit.class)))
                .thenAnswer(invocation -> new NoopHandle(invocation.getArgument(0)));

        when(reservationRecordMapper.selectByBusinessKey(anyString(), anyString()))
                .thenAnswer(invocation -> records.get(
                        ((String) invocation.getArgument(0)) + ':' + invocation.getArgument(1)));

        when(reservationRecordMapper.insert(any(InvReservationRecord.class))).thenAnswer(invocation -> {
            InvReservationRecord record = invocation.getArgument(0);
            records.put(record.getTenantId() + ':' + record.getBusinessKey(), record);
            return 1;
        });

        service = new InvReservationServiceImpl(inventoryMapper, reservationRecordMapper, distributedLock);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("缺少租户上下文时 fail-closed")
    void reserveWithoutTenantFailsClosed() {
        assertThrows(TenantParseException.class,
                () -> service.reserveInventory(request("ORD-1", 1)));
        assertThrows(TenantParseException.class, () -> service.confirmReservation("ORD-1"));
        assertThrows(TenantParseException.class, () -> service.releaseReservation("ORD-1"));
    }

    @Test
    @DisplayName("确认他人租户的预占抛 403（AccessDenied）")
    void confirmForeignTenantReservationIsDenied() {
        UUID tenantB = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantB);

        InvReservationRecord foreign = new InvReservationRecord();
        foreign.setId("33333333-3333-3333-3333-333333333333");
        foreign.setTenantId(UUID.randomUUID().toString()); // 其他租户
        foreign.setBusinessKey("ORD-1");
        foreign.setInventoryId("11111111-1111-1111-1111-111111111111");
        foreign.setQuantity(2);
        foreign.setStatus(ReservationStatus.RESERVED.name());
        when(reservationRecordMapper.selectByBusinessKey(anyString(), anyString())).thenReturn(foreign);

        assertThrows(AccessDeniedException.class, () -> service.confirmReservation("ORD-1"));
        assertThrows(AccessDeniedException.class, () -> service.releaseReservation("ORD-1"));
    }

    @Test
    @DisplayName("相同业务键在不同租户下互不影响")
    void sameBusinessKeyIsSeparatedByTenant() {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();

        TenantContextHolder.setTenantId(tenantA);
        assertTrue(service.reserveInventory(request("ORD-1", 2)));

        TenantContextHolder.setTenantId(tenantB);
        assertTrue(service.reserveInventory(request("ORD-1", 3)));

        assertEquals(2, records.size(), "两个租户应各自持有一条预占记录");
        verify(inventoryMapper, times(2)).lockStock(anyString(), anyInt());

        // 租户 B 的确认不会影响租户 A 的记录
        TenantContextHolder.setTenantId(tenantA);
        assertTrue(service.checkReservationExists("ORD-1"));
    }

    private InventoryReservationRequest request(String businessKey, int quantity) {
        InventoryReservationRequest request = new InventoryReservationRequest();
        request.setSkuId("sku-1");
        request.setWarehouseId("wh-1");
        request.setQuantity(quantity);
        request.setBusinessKey(businessKey);
        request.setTimeoutSeconds(900);
        return request;
    }

    private static final class NoopHandle extends DistributedLock.LockHandle {
        private NoopHandle(String key) {
            super(key, "test-client", null);
        }

        @Override
        public synchronized void release() {
            // 单测无需真实释放
        }
    }
}
