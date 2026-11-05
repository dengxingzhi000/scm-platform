package com.cognora.scm.finance.controller;

import com.cognora.platform.common.log.annotation.AuditLog;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 财务改动全部涉及金额或凭证，任何写接口漏加 @AuditLog 都属于合规缺口。
 * 这条用例防止后续新增写接口时忘记审计注解。
 */
class FinanceAuditAnnotationTest {

    private static final Class<?>[] CONTROLLERS = {
            FreightRuleController.class,
            SettlementOrderController.class,
            SettlementItemController.class,
            InvoiceController.class,
            ReconciliationRecordController.class,
            PlatformServiceFeeController.class
    };

    @Test
    void everyFinanceWriteEndpointMustBeAudited() {
        int checked = 0;
        for (Class<?> controller : CONTROLLERS) {
            for (Method method : controller.getDeclaredMethods()) {
                if (!isWriteEndpoint(method)) {
                    continue;
                }
                checked++;
                AuditLog auditLog = method.getAnnotation(AuditLog.class);
                assertTrue(auditLog != null,
                        controller.getSimpleName() + "#" + method.getName() + " 缺少 @AuditLog");
                assertTrue(auditLog.riskLevel() >= 3,
                        controller.getSimpleName() + "#" + method.getName()
                                + " 是金额/凭证类写操作，riskLevel 至少为 3");
            }
        }
        assertTrue(checked >= 24, "财务写接口数量异常，实际检查到 " + checked);
    }

    private boolean isWriteEndpoint(Method method) {
        return method.isAnnotationPresent(PostMapping.class)
                || method.isAnnotationPresent(PutMapping.class)
                || method.isAnnotationPresent(DeleteMapping.class);
    }
}
