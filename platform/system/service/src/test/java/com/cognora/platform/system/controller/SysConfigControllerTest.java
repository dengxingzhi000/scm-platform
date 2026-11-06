package com.cognora.platform.system.controller;

import com.cognora.platform.common.dto.config.SysConfigItemDTO;
import com.cognora.platform.system.service.ISysConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 配置控制器：查询/写入委托，以及权限码注解。
 */
@ExtendWith(MockitoExtension.class)
class SysConfigControllerTest {

    @Mock private ISysConfigService configService;
    @InjectMocks private SysConfigController controller;

    @Test
    void getConfigDelegatesToMergedView() {
        SysConfigItemDTO platformItem = new SysConfigItemDTO();
        platformItem.setConfigKey("order.maxLines");
        platformItem.setConfigValue("50");
        platformItem.setScope("PLATFORM");
        when(configService.getMergedConfig()).thenReturn(List.of(platformItem));

        var response = controller.getConfig();

        assertEquals(200, response.code());
        assertEquals(1, response.data().size());
        assertEquals("order.maxLines", response.data().get(0).getConfigKey());
        verify(configService).getMergedConfig();
    }

    @Test
    void updateConfigDelegatesSingleItem() {
        SysConfigItemDTO request = new SysConfigItemDTO();
        request.setConfigKey("order.maxLines");
        request.setConfigValue("80");
        request.setScope("TENANT");
        when(configService.updateConfig(request)).thenReturn(request);

        var response = controller.updateConfig(request);

        assertEquals(200, response.code());
        assertEquals("80", response.data().getConfigValue());
        verify(configService).updateConfig(request);
    }

    @Test
    void viewEndpointRequiresViewAuthority() throws Exception {
        Method method = SysConfigController.class.getMethod("getConfig");
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasAuthority('system:config:view')", preAuthorize.value());
    }

    @Test
    void editEndpointRequiresEditAuthority() throws Exception {
        Method method = SysConfigController.class.getMethod("updateConfig", SysConfigItemDTO.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("hasAuthority('system:config:edit')", preAuthorize.value());
    }
}
