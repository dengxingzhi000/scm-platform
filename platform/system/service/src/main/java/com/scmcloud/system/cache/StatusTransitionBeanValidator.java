package com.scmcloud.system.cache;

import com.scmcloud.system.domain.entity.SysStatusTransition;
import com.scmcloud.system.mapper.SysStatusTransitionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 启动时校验所有状态流转规则的 pre_action / post_action Bean 是否存在。
 * 不存在的 Bean 会导致启动失败，防止运行时才发现配置错误。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatusTransitionBeanValidator {

    private final SysStatusTransitionMapper transitionMapper;
    private final ApplicationContext applicationContext;

    @EventListener(ApplicationReadyEvent.class)
    @Order(5)
    public void validateOnStartup() {
        log.info("[BeanValidator] Validating status transition pre_action/post_action beans...");

        List<SysStatusTransition> allTransitions = transitionMapper.selectList(
                new LambdaQueryWrapper<SysStatusTransition>()
                        .eq(SysStatusTransition::getDeleted, false)
                        .eq(SysStatusTransition::getEnabled, true));

        List<String> errors = new ArrayList<>();

        for (SysStatusTransition t : allTransitions) {
            validateBean(t.getPreAction(), "pre_action", t, errors);
            validateBean(t.getPostAction(), "post_action", t, errors);
        }

        if (!errors.isEmpty()) {
            String message = "[BeanValidator] Status transition Bean validation FAILED:\n" + String.join("\n", errors);
            log.error(message);
            throw new IllegalStateException(message);
        }

        log.info("[BeanValidator] All {} transitions validated OK", allTransitions.size());
    }

    private void validateBean(String beanName, String fieldName,
                              SysStatusTransition transition, List<String> errors) {
        if (!StringUtils.hasText(beanName)) {
            return;
        }

        // 支持逗号分隔的多个 Bean 名称
        String[] names = beanName.split(",");
        for (String name : names) {
            String trimmed = name.trim();
            if (!applicationContext.containsBean(trimmed)) {
                errors.add(String.format(
                        "  Bean '%s' not found (%s on %s %s→%s action=%s)",
                        trimmed, fieldName,
                        transition.getBizType(), transition.getFromStatus(),
                        transition.getToStatus(), transition.getActionCode()));
            }
        }
    }
}
