package com.cognora.platform.auth.controller;

import com.cognora.platform.auth.service.ISysAuthService;
import com.cognora.platform.common.dto.user.UserSessionDTO;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.security.util.HttpServletRequestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 会话端点控制器：令牌来源单一、拒绝 userId 入参、返回撤销数量。
 */
@ExtendWith(MockitoExtension.class)
class SysAuthSessionControllerTest {

    private static final String TOKEN = "access-token";

    @Mock private ISysAuthService authService;
    @Mock private HttpServletRequestUtils httpServletRequestUtils;
    @InjectMocks private SysAuthController controller;

    @Test
    void listSessionsOnlyUsesRequestToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        UserSessionDTO session = UserSessionDTO.builder()
                .sessionId("device-current").ip("10.0.0.1").current(true).build();
        when(httpServletRequestUtils.getTokenFromRequest(request)).thenReturn(TOKEN);
        when(authService.listSessions(TOKEN)).thenReturn(List.of(session));

        var response = controller.listSessions(request);

        assertEquals(200, response.code());
        assertEquals(1, response.data().size());
        assertEquals("device-current", response.data().get(0).getSessionId());
        verify(authService).listSessions(TOKEN);
    }

    @Test
    void listSessionsRequiresAuthentication() throws Exception {
        Method method = SysAuthController.class.getMethod("listSessions", jakarta.servlet.http.HttpServletRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("isAuthenticated()", preAuthorize.value());
    }

    @Test
    void logoutOthersRejectsUserIdQueryParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("userId", "019a0aee-0000-7000-8000-000000000123");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.logoutOthers(request, null));

        assertEquals(400, exception.getCode());
        verifyNoInteractions(authService);
    }

    @Test
    void logoutOthersRejectsUserIdInBody() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.logoutOthers(request, Map.of("user_id", "someone-else")));

        assertEquals(400, exception.getCode());
        verifyNoInteractions(authService);
    }

    @Test
    void logoutOthersReturnsRevokedCountFromCurrentSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        when(httpServletRequestUtils.getTokenFromRequest(request)).thenReturn(TOKEN);
        when(authService.logoutOtherSessions(TOKEN)).thenReturn(3);

        var response = controller.logoutOthers(request, null);

        assertEquals(200, response.code());
        assertEquals(3, response.data().getRevokedCount());
        verify(authService).logoutOtherSessions(TOKEN);
        verify(authService, never()).forceLogout(any(), any());
    }

    @Test
    void logoutOthersRequiresAuthentication() throws Exception {
        Method method = SysAuthController.class.getMethod("logoutOthers",
                jakarta.servlet.http.HttpServletRequest.class, Map.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("isAuthenticated()", preAuthorize.value());
    }
}
