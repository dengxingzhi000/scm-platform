package com.scmcloud.common.status;

import com.scmcloud.system.api.StatusMachineDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

/**
 * 通用状态流转校验器。
 * 封装 StatusMachineDubboService 调用，提供简洁的校验 API。
 *
 * <p>所有业务模块注入此组件即可使用状态字典校验，无需关心 Dubbo 调用细节。</p>
 *
 * <pre>
 * statusValidator.validateTransition("ORDER", "PAID", "SHIPPED");
 * statusValidator.validateTransition("PURCHASE", "DRAFT", "PENDING_APPROVAL");
 * </pre>
 */
@Slf4j
@Component
public class StatusValidator {

    @DubboReference
    private StatusMachineDubboService statusMachine;

    /**
     * 校验状态流转是否合法。不合法时抛出 IllegalStateException。
     */
    public void validateTransition(String bizType, String fromStatus, String toStatus) {
        StatusMachineDubboService.TransitionCheckDTO check =
                statusMachine.canTransition(bizType, fromStatus, toStatus);
        if (!check.allowed()) {
            throw new IllegalStateException(
                    "非法状态流转: " + bizType + " " + fromStatus + " -> " + toStatus
                            + ": " + check.reason());
        }
    }

    /**
     * 校验状态流转是否合法。不合法时返回 false。
     */
    public boolean canTransition(String bizType, String fromStatus, String toStatus) {
        return statusMachine.canTransition(bizType, fromStatus, toStatus).allowed();
    }

    /**
     * 执行状态流转。返回目标状态编码。
     */
    public String transition(String bizType, String fromStatus, String actionCode) {
        StatusMachineDubboService.TransitionResultDTO result =
                statusMachine.transition(bizType, fromStatus, actionCode);
        if (!result.success()) {
            throw new IllegalStateException(
                    "状态流转失败: " + bizType + " " + fromStatus + " action=" + actionCode
                            + ": " + result.errorMessage());
        }
        return result.toStatus();
    }
}
