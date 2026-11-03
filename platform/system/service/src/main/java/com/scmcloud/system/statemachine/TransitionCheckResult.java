package com.scmcloud.system.statemachine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a transition eligibility check.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransitionCheckResult {

    private boolean allowed;
    private String bizType;
    private String fromStatus;
    private String toStatus;
    private String reason;

    public static TransitionCheckResult allow(String bizType, String fromStatus, String toStatus) {
        return TransitionCheckResult.builder()
                .allowed(true)
                .bizType(bizType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .build();
    }

    public static TransitionCheckResult deny(String bizType, String fromStatus, String toStatus, String reason) {
        return TransitionCheckResult.builder()
                .allowed(false)
                .bizType(bizType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .reason(reason)
                .build();
    }
}
