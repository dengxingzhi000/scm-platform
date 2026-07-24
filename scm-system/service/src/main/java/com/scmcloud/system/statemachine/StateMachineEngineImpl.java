package com.scmcloud.system.statemachine;

import com.scmcloud.system.domain.entity.SysStatusTransition;
import com.scmcloud.system.service.ISysStatusDictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of StateMachineEngine.
 * Validates transitions against sys_status_transition rules.
 *
 * <p>Supports SPI extension via preAction/postAction fields
 * (Bean names resolved at runtime via Spring ApplicationContext).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StateMachineEngineImpl implements StateMachineEngine {
    private final ISysStatusDictService statusDictService;

    @Override
    public TransitionCheckResult canTransition(String bizType, String fromStatus, String toStatus) {
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(fromStatus) || !StringUtils.hasText(toStatus)) {
            return TransitionCheckResult.deny(bizType, fromStatus, toStatus,
                    "bizType, fromStatus, toStatus must not be blank");
        }

        List<SysStatusTransition> transitions = statusDictService.listTransitionsFrom(bizType, fromStatus);

        boolean allowed = transitions.stream()
                .anyMatch(t -> toStatus.equals(t.getToStatus()) && Boolean.TRUE.equals(t.getEnabled()));

        if (allowed) {
            return TransitionCheckResult.allow(bizType, fromStatus, toStatus);
        }

        return TransitionCheckResult.deny(bizType, fromStatus, toStatus,
                "No enabled transition from " + fromStatus + " to " + toStatus + " for " + bizType);
    }

    @Override
    public TransitionCheckResult canTransitionByAction(String bizType, String fromStatus, String actionCode) {
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(fromStatus) || !StringUtils.hasText(actionCode)) {
            return TransitionCheckResult.deny(bizType, fromStatus, null,
                    "bizType, fromStatus, actionCode must not be blank");
        }

        List<SysStatusTransition> transitions = statusDictService.listTransitionsFrom(bizType, fromStatus);

        SysStatusTransition matched = transitions.stream()
                .filter(t -> actionCode.equals(t.getActionCode()) && Boolean.TRUE.equals(t.getEnabled()))
                .findFirst()
                .orElse(null);

        if (matched != null) {
            return TransitionCheckResult.allow(bizType, fromStatus, matched.getToStatus());
        }

        return TransitionCheckResult.deny(bizType, fromStatus, null,
                "No enabled transition with action '" + actionCode + "' from status " + fromStatus + " for " + bizType);
    }

    @Override
    public TransitionResult transition(String bizType, String fromStatus, String actionCode) {
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(fromStatus) || !StringUtils.hasText(actionCode)) {
            return TransitionResult.failure(bizType, fromStatus, actionCode,
                    "bizType, fromStatus, actionCode must not be blank");
        }

        List<SysStatusTransition> transitions = statusDictService.listTransitionsFrom(bizType, fromStatus);

        SysStatusTransition matched = transitions.stream()
                .filter(t -> actionCode.equals(t.getActionCode()) && Boolean.TRUE.equals(t.getEnabled()))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            return TransitionResult.failure(bizType, fromStatus, actionCode,
                    "No enabled transition with action '" + actionCode + "' from status " + fromStatus);
        }

        log.info("State transition: {} {} -> {} (action={})", bizType, fromStatus, matched.getToStatus(), actionCode);
        return TransitionResult.success(matched);
    }

    @Override
    public List<AvailableAction> getAvailableActions(String bizType, String currentStatus) {
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(currentStatus)) {
            return List.of();
        }

        List<SysStatusTransition> transitions = statusDictService.listTransitionsFrom(bizType, currentStatus);

        return transitions.stream()
                .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                .map(t -> AvailableAction.builder()
                        .actionCode(t.getActionCode())
                        .actionName(t.getActionName())
                        .actionNameEn(t.getActionNameEn())
                        .targetStatus(t.getToStatus())
                        .needApproval(t.getNeedApproval())
                        .conditionExpression(t.getConditionExpression())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getValidNextStatuses(String bizType, String currentStatus) {
        if (!StringUtils.hasText(bizType) || !StringUtils.hasText(currentStatus)) {
            return List.of();
        }

        List<SysStatusTransition> transitions = statusDictService.listTransitionsFrom(bizType, currentStatus);

        return transitions.stream()
                .filter(t -> Boolean.TRUE.equals(t.getEnabled()))
                .map(SysStatusTransition::getToStatus)
                .distinct()
                .collect(Collectors.toList());
    }
}
