package com.cognora.platform.system.controller;

import com.cognora.platform.common.log.annotation.AuditLog;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 审计文案防回归测试。
 *
 * <p>这 5 个 {@code @AuditLog.operation} 曾被 GBK/UTF-8 二次编码损坏，写入审计库后
 * 用户看到的是乱码。这里锁定精确中文文案，并保证文案中不含 Unicode 私用区码点
 * （U+E000–U+F8FF 与 U+951F 都是二次编码留下的残字特征）。</p>
 */
class AuditLogOperationTextTest {

    /** 期望的审计操作文案：类名#方法名 -> operation。 */
    private static final Map<String, String> EXPECTED_OPERATIONS = new LinkedHashMap<>();

    static {
        EXPECTED_OPERATIONS.put("SysRoleController#update", "修改角色");
        EXPECTED_OPERATIONS.put("SysRoleController#delete", "删除角色");
        EXPECTED_OPERATIONS.put("SysRoleController#grantPermissions", "角色授权");
        EXPECTED_OPERATIONS.put("SysPermissionController#update", "修改权限");
        EXPECTED_OPERATIONS.put("SysPermissionController#delete", "删除权限");
    }

    private static final Class<?>[] CONTROLLERS = {
            SysRoleController.class,
            SysPermissionController.class
    };

    @Test
    void auditOperationLiteralsAreExactChineseText() throws Exception {
        assertEquals(5, EXPECTED_OPERATIONS.size(), "回归用例应恰好覆盖 5 个审计文案");

        for (Map.Entry<String, String> entry : EXPECTED_OPERATIONS.entrySet()) {
            String key = entry.getKey();
            String expected = entry.getValue();
            String[] parts = key.split("#");

            Class<?> controller = resolveController(parts[0]);
            Method method = findAnnotatedMethod(controller, parts[1]);

            AuditLog auditLog = method.getAnnotation(AuditLog.class);
            assertNotNull(auditLog, () -> key + " 缺少 @AuditLog 注解");
            assertEquals(expected, auditLog.operation(), () -> key + " 的 operation 文案被改坏了");
            assertNoMojibakeResidue(key + " 的 operation", auditLog.operation());
        }
    }

    @Test
    void noAuditLogLiteralCarriesPrivateUseCodePoints() {
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                AuditLog auditLog = method.getAnnotation(AuditLog.class);
                if (auditLog == null) {
                    continue;
                }
                String where = controller.getSimpleName() + "#" + method.getName();
                assertNoMojibakeResidue(where + " 的 operation", auditLog.operation());
                assertNoMojibakeResidue(where + " 的 businessType", auditLog.businessType());
            }
        }
    }

    private static Class<?> resolveController(String simpleName) {
        for (Class<?> controller : CONTROLLERS) {
            if (controller.getSimpleName().equals(simpleName)) {
                return controller;
            }
        }
        throw new IllegalArgumentException("未知控制器: " + simpleName);
    }

    private static Method findAnnotatedMethod(Class<?> controller, String methodName) {
        for (Method method : controller.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new IllegalArgumentException(controller.getSimpleName() + " 中找不到方法 " + methodName);
    }

    private static void assertNoMojibakeResidue(String label, String text) {
        assertNotNull(text, () -> label + " 不能为 null");
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            boolean privateUse = codePoint >= 0xE000 && codePoint <= 0xF8FF;
            boolean gbkResidue = codePoint == 0x951F || codePoint == 0xFFFD;
            assertFalse(privateUse || gbkResidue,
                    () -> label + " 含乱码残字码点 U+" + Integer.toHexString(codePoint).toUpperCase());
            i += Character.charCount(codePoint);
        }
    }
}
