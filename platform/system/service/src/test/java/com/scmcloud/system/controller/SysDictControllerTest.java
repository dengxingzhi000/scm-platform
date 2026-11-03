package com.scmcloud.system.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.system.domain.entity.SysDictItem;
import com.scmcloud.system.domain.entity.SysDictType;
import com.scmcloud.system.service.ISysDictService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysDictControllerTest {

    @Mock
    private ISysDictService dictService;

    @InjectMocks
    private SysDictController controller;

    @Test
    void createTypeDelegatesIdGenerationToService() {
        SysDictType entity = new SysDictType();
        entity.setDictCode("order_status");

        ApiResponse<Void> response = controller.createType(entity);

        assertEquals(200, response.code());
        assertNull(entity.getId());
        verify(dictService).createDictType(entity);
    }

    @Test
    void createItemDelegatesIdGenerationToService() {
        SysDictItem entity = new SysDictItem();
        entity.setDictCode("order_status");

        ApiResponse<Void> response = controller.createItem(entity);

        assertEquals(200, response.code());
        assertNull(entity.getId());
        verify(dictService).createDictItem(entity);
    }

    @Test
    void listTypesReturnsServiceResult() {
        List<SysDictType> types = List.of(new SysDictType());
        when(dictService.listDictTypes("order_status", 1)).thenReturn(types);

        ApiResponse<List<SysDictType>> response = controller.listTypes("order_status", 1);

        assertEquals(200, response.code());
        assertSame(types, response.data());
    }

    @Test
    void updateTypeOnlyDelegates() {
        SysDictType entity = new SysDictType();
        entity.setId("dict-id");

        ApiResponse<Void> response = controller.updateType(entity);

        assertEquals(200, response.code());
        verify(dictService).updateDictType(entity);
    }
}
