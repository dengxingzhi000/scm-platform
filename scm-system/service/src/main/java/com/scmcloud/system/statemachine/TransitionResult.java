package com.scmcloud.system.statemachine;

import com.scmcloud.system.domain.entity.SysStatusTransition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of executing a state transition.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionResult {

    private boolean success;
    private String bizType;
    private String fromStatus;
    private String toStatus;
    private String actionCode;
    private boolean needApproval;
    private String postAction;
    private String errorMessage;

    public static TransitionResult success(SysStatusTransition transition) {
        return TransitionResult.builder()
                .success(true)
                .bizType(transition.getBizType())
                .fromStatus(transition.getFromStatus())
                .toStatus(transition.getToStatus())
                .actionCode(transition.getActionCode())
                .needApproval(transition.getNeedApproval())
                .postAction(transition.getPostAction())
                .build();
    }

    public static TransitionResult failure(String bizType, String fromStatus, String actionCode, String errorMessage) {
        return TransitionResult.builder()
                .success(false)
                .bizType(bizType)
                .fromStatus(fromStatus)
                .actionCode(actionCode)
                .errorMessage(errorMessage)
                .build();
    }
}
