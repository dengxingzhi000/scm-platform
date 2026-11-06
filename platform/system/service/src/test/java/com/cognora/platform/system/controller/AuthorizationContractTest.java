package com.cognora.platform.system.controller;

import com.cognora.platform.common.dto.config.SysConfigItemDTO;
import com.cognora.platform.common.dto.user.UpdateProfileRequest;
import com.cognora.platform.common.dto.user.UserDTO;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.expression.Expression;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 方法级权限的越权断言。
 *
 * <p>{@code @PreAuthorize} 抛出的 {@code AccessDeniedException} 由
 * {@code SecurityRestExceptionHandler} 映射为 HTTP 403（该映射已有独立测试）。
 * 这里用 Spring Security 自己的表达式解析器验证：普通用户（只带 ROLE_OPERATOR）
 * 对管理员端点求值为 false，即请求会被拒绝；持有对应权限码的用户求值为 true。</p>
 */
class AuthorizationContractTest {

    @Test
    void normalUserCannotCallUpdateUserEndpoint() throws Exception {
        Method method = SysUserController.class.getMethod("update", UUID.class, UserDTO.class);
        String expression = preAuthorizeOf(method);

        assertEquals("hasAuthority('system:user:edit')", expression);
        assertFalse(evaluate(expression, method, "ROLE_OPERATOR"));
        assertTrue(evaluate(expression, method, "system:user:edit"));
    }

    @Test
    void selfProfileEndpointIsTokenBoundAndRequiresAuthentication() throws Exception {
        Method method = SysUserController.class.getMethod("updateOwnProfile", UpdateProfileRequest.class);

        assertEquals("isAuthenticated()", preAuthorizeOf(method));
        assertFalse(Arrays.stream(method.getParameters())
                .anyMatch(parameter -> parameter.isAnnotationPresent(PathVariable.class)),
                "PUT /users/me 不允许路径参数，目标用户只能来自令牌");
        assertTrue(evaluate("isAuthenticated()", method));
    }

    @Test
    void configReadRequiresViewAuthority() throws Exception {
        Method method = SysConfigController.class.getMethod("getConfig");
        String expression = preAuthorizeOf(method);

        assertEquals("hasAuthority('system:config:view')", expression);
        assertFalse(evaluate(expression, method, "ROLE_OPERATOR"));
        assertTrue(evaluate(expression, method, "system:config:view"));
    }

    @Test
    void configWriteRequiresEditAuthority() throws Exception {
        Method method = SysConfigController.class.getMethod("updateConfig", SysConfigItemDTO.class);
        String expression = preAuthorizeOf(method);

        assertEquals("hasAuthority('system:config:edit')", expression);
        assertFalse(evaluate(expression, method, "ROLE_OPERATOR"));
        assertTrue(evaluate(expression, method, "system:config:edit"));
    }

    private static String preAuthorizeOf(Method method) {
        var annotation = method.getAnnotation(
                org.springframework.security.access.prepost.PreAuthorize.class);
        assertNotNull(annotation, "端点必须声明 @PreAuthorize");
        return annotation.value();
    }

    private static boolean evaluate(String expression, Method method, String... authorities) throws Exception {
        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.getMethod()).thenReturn(method);

        String[] granted = authorities.length == 0 ? new String[]{"ROLE_OPERATOR"} : authorities;
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "tester", "n/a",
                List.of(Arrays.stream(granted).map(SimpleGrantedAuthority::new).toArray(SimpleGrantedAuthority[]::new)));

        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        var context = handler.createEvaluationContext(() -> authentication, invocation);
        Expression parsed = handler.getExpressionParser().parseExpression(expression);
        return Boolean.TRUE.equals(parsed.getValue(context));
    }
}
