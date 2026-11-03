package com.scmcloud.system.service.Impl;

import com.scmcloud.system.domain.entity.SysDictItem;
import com.scmcloud.system.domain.entity.SysDictType;
import com.scmcloud.system.mapper.SysDictItemMapper;
import com.scmcloud.system.mapper.SysDictTypeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SysDictServiceImplTest {

    @Mock
    private SysDictTypeMapper dictTypeMapper;

    @Mock
    private SysDictItemMapper dictItemMapper;

    @InjectMocks
    private SysDictServiceImpl service;

    @Test
    void createDictTypeGeneratesIdBeforeInsert() {
        SysDictType entity = new SysDictType();

        service.createDictType(entity);

        assertNotNull(entity.getId());
        verify(dictTypeMapper).insert(entity);
    }

    @Test
    void createDictItemGeneratesIdBeforeInsert() {
        SysDictItem entity = new SysDictItem();

        service.createDictItem(entity);

        assertNotNull(entity.getId());
        verify(dictItemMapper).insert(entity);
    }
}
