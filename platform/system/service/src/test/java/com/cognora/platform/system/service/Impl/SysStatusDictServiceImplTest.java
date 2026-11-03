package com.cognora.platform.system.service.Impl;

import com.cognora.platform.system.cache.StatusDictCacheManager;
import com.cognora.platform.system.cache.StatusDictEventPublisher;
import com.cognora.platform.system.domain.entity.SysStatusDict;
import com.cognora.platform.system.domain.entity.SysStatusTransition;
import com.cognora.platform.system.mapper.SysStatusDictMapper;
import com.cognora.platform.system.mapper.SysStatusTransitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SysStatusDictServiceImplTest {

    @Mock
    private SysStatusDictMapper statusDictMapper;

    @Mock
    private SysStatusTransitionMapper transitionMapper;

    @Mock
    private StatusDictCacheManager cacheManager;

    @Mock
    private StatusDictEventPublisher eventPublisher;

    @InjectMocks
    private SysStatusDictServiceImpl service;

    @Test
    void createStatusDictGeneratesIdAndPublishesChange() {
        SysStatusDict entity = new SysStatusDict();
        entity.setBizType("ORDER");

        service.createStatusDict(entity);

        assertNotNull(entity.getId());
        verify(statusDictMapper).insert(entity);
        verify(eventPublisher).publishChange("global", "ORDER");
    }

    @Test
    void createTransitionGeneratesIdAndPublishesChange() {
        SysStatusTransition entity = new SysStatusTransition();
        entity.setBizType("ORDER");

        service.createTransition(entity);

        assertNotNull(entity.getId());
        verify(transitionMapper).insert(entity);
        verify(eventPublisher).publishChange("global", "ORDER");
    }
}