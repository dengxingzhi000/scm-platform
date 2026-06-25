package com.scmcloud.decision.execution;

public interface DecisionExecutor<C, R> {
    ExecutionResult<R> execute(DecisionPlan<C> plan);
    void rollback(DecisionPlan<C> plan);
}
