package com.scmcloud.decision.constraint;

import com.scmcloud.decision.engine.ConstraintResult;
import com.scmcloud.decision.engine.ConstraintType;

public interface Constraint<C> {
    ConstraintResult validate(C context);
    String name();
    ConstraintType type();
    int priority();
}
