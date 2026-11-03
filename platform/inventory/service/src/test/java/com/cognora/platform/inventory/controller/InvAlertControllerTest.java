package com.cognora.platform.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.inventory.dto.InvAlertQueryRequest;
import com.cognora.platform.inventory.dto.InvAlertResponse;
import com.cognora.platform.inventory.dto.InvAlertScanResult;
import com.cognora.platform.inventory.service.command.InvAlertCommandService;
import com.cognora.platform.inventory.service.query.InvAlertQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvAlertControllerTest {

    @Mock
    private InvAlertQueryService queryService;

    @Mock
    private InvAlertCommandService commandService;

    @InjectMocks
    private InvAlertController controller;

    @Test
    void queryShouldReturnTheServiceMappedPage() {
        InvAlertQueryRequest request = new InvAlertQueryRequest();
        Page<InvAlertResponse> page = new Page<>(1, 10, 1);
        page.setRecords(java.util.List.of(new InvAlertResponse()));
        when(queryService.query(request)).thenReturn(page);

        ApiResponse<Page<InvAlertResponse>> response = controller.query(request);

        assertEquals(200, response.code());
        assertSame(page, response.data());
        verify(queryService).query(request);
    }

    @Test
    void getShouldReturn404WhenAlertMissing() {
        when(queryService.getById("alert-1")).thenReturn(null);

        ApiResponse<InvAlertResponse> response = controller.get("alert-1");

        assertEquals(404, response.code());
        assertEquals("告警不存在", response.message());
    }

    @Test
    void resolveShouldReportWhetherARowWasUpdated() {
        when(commandService.resolve("alert-1", null)).thenReturn(1);

        ApiResponse<Boolean> response = controller.resolve("alert-1", null);

        assertEquals(200, response.code());
        assertTrue(response.data());
    }

    @Test
    void scanShouldDelegateToCommandService() {
        InvAlertScanResult result = InvAlertScanResult.builder().scannedInventories(3).build();
        when(commandService.scanAndGenerate()).thenReturn(result);

        ApiResponse<InvAlertScanResult> response = controller.scan();

        assertSame(result, response.data());
    }
}
