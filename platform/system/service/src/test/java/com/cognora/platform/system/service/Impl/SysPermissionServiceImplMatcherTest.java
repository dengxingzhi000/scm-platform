package com.cognora.platform.system.service.Impl;

import com.cognora.platform.system.domain.entity.SysPermission;
import com.cognora.platform.system.mapper.SysPermissionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SysPermissionServiceImplMatcherTest {

    @Test
    void findPermissionsByUrlUsesCandidateQueryAndTemplateMatching() {
        SysPermissionMapper mapper = mock(SysPermissionMapper.class);
        SysPermissionServiceImpl service = new SysPermissionServiceImpl(null, null, null, null);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);

        SysPermission row = new SysPermission();
        row.setPermissionCode("order:order:view");
        row.setApiPath("/api/v1/orders/{id}");
        row.setHttpMethod("GET");
        when(mapper.findApiPermissionCandidates("/api/v1/orders/123", "GET")).thenReturn(List.of(row));

        assertEquals(List.of("order:order:view"),
                service.findPermissionsByUrl("/api/v1/orders/123", "GET"));
        verify(mapper).findApiPermissionCandidates("/api/v1/orders/123", "GET");
    }
}
