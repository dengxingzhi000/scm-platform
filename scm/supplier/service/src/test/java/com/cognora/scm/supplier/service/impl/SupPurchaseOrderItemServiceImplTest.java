package com.cognora.scm.supplier.service.impl;

import com.cognora.scm.supplier.domain.entity.SupPurchaseOrderItem;
import com.cognora.scm.supplier.mapper.SupPurchaseOrderItemMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SupPurchaseOrderItemServiceImplTest {

    private SupPurchaseOrderItemServiceImpl newService(SupPurchaseOrderItemMapper mapper) {
        SupPurchaseOrderItemServiceImpl service = new SupPurchaseOrderItemServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        return service;
    }

    @Test
    void createShouldComputeSubtotalWithOriginalPrecision() {
        SupPurchaseOrderItemMapper mapper = mock(SupPurchaseOrderItemMapper.class);
        SupPurchaseOrderItemServiceImpl service = newService(mapper);

        SupPurchaseOrderItem item = new SupPurchaseOrderItem();
        item.setUnitPrice(new BigDecimal("12.345"));
        item.setQuantity(3);
        SupPurchaseOrderItem created = service.createItem(item);

        assertNotNull(created.getId());
        assertNotNull(created.getCreateTime());
        assertEquals(new BigDecimal("37.035"), created.getSubtotal());
        assertEquals(3, created.getSubtotal().scale());
        verify(mapper).insert(item);
    }

    @Test
    void createShouldKeepProvidedSubtotal() {
        SupPurchaseOrderItemMapper mapper = mock(SupPurchaseOrderItemMapper.class);
        SupPurchaseOrderItemServiceImpl service = newService(mapper);

        SupPurchaseOrderItem item = new SupPurchaseOrderItem();
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(2);
        item.setSubtotal(new BigDecimal("999.99"));
        service.createItem(item);

        assertEquals(new BigDecimal("999.99"), item.getSubtotal());
    }

    @Test
    void createShouldLeaveSubtotalNullWhenInputsMissing() {
        SupPurchaseOrderItemMapper mapper = mock(SupPurchaseOrderItemMapper.class);
        SupPurchaseOrderItemServiceImpl service = newService(mapper);

        SupPurchaseOrderItem item = new SupPurchaseOrderItem();
        item.setUnitPrice(new BigDecimal("10.00"));
        service.createItem(item);

        assertNull(item.getSubtotal());
    }

    @Test
    void updateShouldComputeSubtotalAndReloadItem() {
        SupPurchaseOrderItemMapper mapper = mock(SupPurchaseOrderItemMapper.class);
        SupPurchaseOrderItemServiceImpl service = newService(mapper);
        UUID id = UUID.randomUUID();
        SupPurchaseOrderItem existing = new SupPurchaseOrderItem();
        existing.setId(id);
        SupPurchaseOrderItem reloaded = new SupPurchaseOrderItem();
        reloaded.setId(id);
        reloaded.setSubtotal(new BigDecimal("20.00"));
        when(mapper.selectById(id)).thenReturn(existing, reloaded);

        SupPurchaseOrderItem item = new SupPurchaseOrderItem();
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setQuantity(2);
        SupPurchaseOrderItem result = service.updateItem(id, item);

        assertEquals(new BigDecimal("20.00"), item.getSubtotal());
        assertEquals(reloaded, result);
        verify(mapper).updateById(item);
    }

    @Test
    void updateShouldReturnNullWhenItemMissing() {
        SupPurchaseOrderItemMapper mapper = mock(SupPurchaseOrderItemMapper.class);
        SupPurchaseOrderItemServiceImpl service = newService(mapper);

        SupPurchaseOrderItem result = service.updateItem(UUID.randomUUID(), new SupPurchaseOrderItem());

        assertNull(result);
        verify(mapper, never()).updateById(any(SupPurchaseOrderItem.class));
    }
}
