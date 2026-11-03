package com.scmcloud.decision.constraint;

import com.scmcloud.decision.engine.ConstraintResult;
import com.scmcloud.decision.engine.ConstraintType;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ConstraintChain<C> {
    private final List<Constraint<C>> constraints;

    public ConstraintChain(List<Constraint<C>> constraints) {
        this.constraints = constraints.stream()
                .sorted(Comparator.comparingInt(Constraint::priority))
                .collect(Collectors.toList());
    }

    public List<ConstraintResult> validate(C context) {
        List<ConstraintResult> results = new ArrayList<>();
        for (Constraint<C> constraint : constraints) {
            ConstraintResult result = constraint.validate(context);
            results.add(result);
            if (constraint.type() == ConstraintType.HARD && !result.isPassed()) {
                log.debug("HARD constraint {} failed: {}", constraint.name(), result.getReason());
                break;
            }
        }
        return results;
    }

    public boolean allHardConstraintsPassed(C context) {
        for (Constraint<C> constraint : constraints) {
            if (constraint.type() == ConstraintType.HARD) {
                if (!constraint.validate(context).isPassed()) {
                    return false;
                }
            }
        }
        return true;
    }
}
