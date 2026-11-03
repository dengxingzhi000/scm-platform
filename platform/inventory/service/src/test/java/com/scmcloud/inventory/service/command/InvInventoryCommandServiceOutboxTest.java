package com.scmcloud.inventory.service.command;

import com.scmcloud.common.tenant.TenantContextHolder;
import com.scmcloud.common.integration.outbox.OutboxService;
import com.scmcloud.inventory.domain.entity.Inventory;
import com.scmcloud.inventory.dto.InventoryAdjustRequest;
import com.scmcloud.inventory.event.InventoryAdjustedEvent;
import com.scmcloud.inventory.mapper.InvInventoryMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvInventoryCommandServiceOutboxTest {

    @Mock private InvInventoryMapper inventoryMapper;
    @Mock private OutboxService outboxService;

    private InvInventoryCommandService service;

    @BeforeEach
    void setUp() {
        service = new InvInventoryCommandService(inventoryMapper, outboxService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void adjustInventoryShouldWriteOutboxEventWithInventoryAggregateType() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Inventory existing = new Inventory();
        existing.setId("inv-123");
        existing.setSkuId("SKU-1");
        existing.setWarehouseId("WH-1");
        existing.setAvailableStock(100);
        existing.setTotalStock(100);
        existing.setLockedStock(0);
        existing.setDamagedStock(0);
        existing.setSafetyStock(10);

        when(inventoryMapper.selectOne(any())).thenReturn(existing);

        InventoryAdjustRequest request = new InventoryAdjustRequest();
        request.setSkuId("SKU-1");
        request.setWarehouseId("WH-1");
        request.setQuantity(10);
        request.setAdjustType(1);
        request.setOperatorId("op-1");

        service.adjustInventory(request);

        verify(outboxService).save(
                eq("INVENTORY_ADJUSTED"),
                eq("Inventory"),
                eq("inv-123"),
                any(InventoryAdjustedEvent.class),
                eq(tenantId)
        );
    }

    @Test
    void adjustInventoryShouldPropagateExceptionWhenOutboxSaveFails() {
        UUID tenantId = UUID.randomUUID();
        TenantContextHolder.setTenantId(tenantId);

        Inventory existing = new Inventory();
        existing.setId("inv-456");
        existing.setSkuId("SKU-2");
        existing.setWarehouseId("WH-1");
        existing.setAvailableStock(50);
        existing.setTotalStock(50);
        existing.setSafetyStock(10);

        when(inventoryMapper.selectOne(any())).thenReturn(existing);
        doThrow(new RuntimeException("outbox save failed"))
                .when(outboxService).save(anyString(), anyString(), anyString(), any(), any());

        InventoryAdjustRequest request = new InventoryAdjustRequest();
        request.setSkuId("SKU-2");
        request.setWarehouseId("WH-1");
        request.setQuantity(5);
        request.setAdjustType(1);
        request.setOperatorId("op-1");

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () -> service.adjustInventory(request)
        );
    }
}