package com.scmcloud.system.statemachine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 通用 CAS 状态流转执行器。
 *
 * <p>解决并发流转冲突:
 * <ol>
 *   <li>通过状态机验证流转合法性</li>
 *   <li>通过乐观锁 (version) 保证并发安全</li>
 *   <li>冲突时抛出 OptimisticLockException，由调用方决定重试策略</li>
 * </ol>
 *
 * <p>使用方式:
 * <pre>
 * executor.execute("ORDER", order, Order::getStatusEnum, Order::transitionTo, OrderStatus.CANCELLED);
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransitionExecutor {
    private final StateMachineEngine stateMachineEngine;

    /**
     * 执行带乐观锁保护的状态流转。
     *
     * @param bizType       业务类型
     * @param entity        实体对象 (必须已包含 version 字段)
     * @param currentStatus 获取当前状态的函数
     * @param transition    执行流转的函数 (应设置新状态)
     * @param targetStatus  目标状态
     * @param actionCode    动作编码
     * @param saveFunc      保存函数 (应使用 updateById 触发 CAS)
     * @param <T>           实体类型
     * @param <S>           状态类型
     */
    public <T, S> void execute(String bizType, T entity,
                                Function<T, S> currentStatus,
                                BiConsumer3<T, S> transition,
                                S targetStatus,
                                String actionCode,
                                Consumer<T> saveFunc) {

        S fromStatus = currentStatus.apply(entity);
        String fromName = fromStatus.toString();
        String toName = targetStatus.toString();

        // 1. 状态机验证
        TransitionCheckResult check = stateMachineEngine.canTransition(bizType, fromName, toName);
        if (!check.isAllowed()) {
            throw new IllegalStateException(
                    "非法状态流转: " + bizType + " " + fromName + " -> " + toName
                            + ": " + check.getReason());
        }

        // 2. 执行流转
        transition.accept(entity, targetStatus);

        // 3. CAS 保存 (乐观锁)
        try {
            saveFunc.accept(entity);
            log.info("State transition OK: {} {} -> {} (action={})", bizType, fromName, toName, actionCode);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("State transition CONFLICT: {} {} -> {} (action={}), version mismatch",
                    bizType, fromName, toName, actionCode);
            throw new IllegalStateException(
                    "状态流转冲突 (并发修改): " + bizType + " " + fromName + " -> " + toName
                            + ". 请刷新后重试.", e);
        }
    }

    @FunctionalInterface
    public interface BiConsumer3<T, U> {
        void accept(T t, U u);
    }
}
