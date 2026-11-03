package com.cognora.platform.decision.constraint;

import com.cognora.platform.decision.engine.ConstraintResult;
import com.cognora.platform.decision.engine.ConstraintType;

public interface Constraint<C> {
    ConstraintResult validate(C context);
    String name();
    ConstraintType type();
    int priority();
}
