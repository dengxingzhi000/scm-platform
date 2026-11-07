package com.cognora.platform.system.service.command;

import com.cognora.platform.common.dto.user.CreateUserRequest;
import com.cognora.platform.common.dto.user.UpdateUserRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 防止写入 DTO 重新暴露可批量赋值的安全敏感字段。
 */
class UserCommandDtoSplitTest {

    private static final Set<String> FORBIDDEN_SETTERS = Set.of(
            "setId", "setUsername", "setPassword", "setStatus", "setTenantId",
            "setLoginAttempts", "setLockedUntil", "setDataScope", "setUserType",
            "setDeleted", "setCreateBy", "setUpdateBy"
    );

    @Test
    void updateRequestDoesNotExposeSecuritySensitiveSetters() {
        Set<String> setters = setterNames(UpdateUserRequest.class);
        for (String forbidden : FORBIDDEN_SETTERS) {
            assertFalse(setters.contains(forbidden), "UpdateUserRequest 不应暴露 " + forbidden);
        }
    }

    @Test
    void createRequestAllowsUsernameButNotStatusOrTenant() {
        Set<String> setters = setterNames(CreateUserRequest.class);
        assertTrue(setters.contains("setUsername"));
        assertFalse(setters.contains("setStatus"));
        assertFalse(setters.contains("setTenantId"));
        assertFalse(setters.contains("setLoginAttempts"));
        assertFalse(setters.contains("setLockedUntil"));
    }

    private Set<String> setterNames(Class<?> type) {
        return Arrays.stream(type.getMethods())
                .map(Method::getName)
                .filter(name -> name.startsWith("set"))
                .collect(Collectors.toSet());
    }
}
