package com.scmcloud.system.controller;

import com.scmcloud.common.dto.dept.DeptDTO;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.system.service.ISysDeptService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysDeptControllerTest {

    @Mock
    private ISysDeptService deptService;

    @InjectMocks
    private SysDeptController controller;

    @Test
    void updateDelegatesIdBindingToService() {
        UUID id = UUID.randomUUID();
        DeptDTO deptDTO = new DeptDTO();

        ApiResponse<Void> response = controller.update(id, deptDTO);

        assertEquals(200, response.code());
        assertNull(deptDTO.getId());
        verify(deptService).updateDept(id, deptDTO);
    }

    @Test
    void treeReturnsServiceResult() {
        List<DeptDTO> tree = List.of(new DeptDTO());
        when(deptService.getDeptTree()).thenReturn(tree);

        ApiResponse<List<DeptDTO>> response = controller.tree();

        assertEquals(200, response.code());
        assertSame(tree, response.data());
    }
}
