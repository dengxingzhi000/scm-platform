package com.scmcloud.system.statemachine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * An available action from a given status.
 * Returned by getAvailableActions() for UI rendering and validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableAction {

    private String actionCode;
    private String actionName;
    private String actionNameEn;
    private String targetStatus;
    private String targetStatusName;
    private boolean needApproval;
    private String conditionExpression;
}
