package com.scmcloud.system.dubbo;

import com.scmcloud.system.api.StatusMachineDubboService;
import com.scmcloud.system.statemachine.AvailableAction;
import com.scmcloud.system.statemachine.StateMachineEngine;
import com.scmcloud.system.statemachine.TransitionCheckResult;
import com.scmcloud.system.statemachine.TransitionResult;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class StatusMachineDubboServiceImpl implements StatusMachineDubboService {

    private final StateMachineEngine stateMachineEngine;

    @Override
    public TransitionCheckDTO canTransition(String bizType, String fromStatus, String toStatus) {
        TransitionCheckResult r = stateMachineEngine.canTransition(bizType, fromStatus, toStatus);
        return new TransitionCheckDTO(r.isAllowed(), r.getBizType(), r.getFromStatus(), r.getToStatus(), r.getReason());
    }

    @Override
    public TransitionCheckDTO canTransitionByAction(String bizType, String fromStatus, String actionCode) {
        TransitionCheckResult r = stateMachineEngine.canTransitionByAction(bizType, fromStatus, actionCode);
        return new TransitionCheckDTO(r.isAllowed(), r.getBizType(), r.getFromStatus(), r.getToStatus(), r.getReason());
    }

    @Override
    public TransitionResultDTO transition(String bizType, String fromStatus, String actionCode) {
        TransitionResult r = stateMachineEngine.transition(bizType, fromStatus, actionCode);
        return new TransitionResultDTO(r.isSuccess(), r.getBizType(), r.getFromStatus(), r.getToStatus(),
                r.getActionCode(), r.isNeedApproval(), r.getPostAction(), r.getErrorMessage());
    }

    @Override
    public List<AvailableActionDTO> getAvailableActions(String bizType, String currentStatus) {
        return stateMachineEngine.getAvailableActions(bizType, currentStatus).stream()
                .map(a -> new AvailableActionDTO(a.getActionCode(), a.getActionName(), a.getActionNameEn(),
                        a.getTargetStatus(), a.isNeedApproval(), a.getConditionExpression()))
                .toList();
    }

    @Override
    public List<String> getValidNextStatuses(String bizType, String currentStatus) {
        return stateMachineEngine.getValidNextStatuses(bizType, currentStatus);
    }
}
