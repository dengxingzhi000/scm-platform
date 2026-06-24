package com.scmcloud.inventory.engine;

import com.scmcloud.decision.constraint.Constraint;
import com.scmcloud.decision.engine.ConstraintResult;
import com.scmcloud.decision.engine.ConstraintType;

public class StockSufficientConstraint implements Constraint<WarehouseCandidate> {
    @Override
    public ConstraintResult validate(WarehouseCandidate context) {
        if (context.getAvailableStock() <= 0) {
            return ConstraintResult.failed(name(), type(), "STOCK_001", "No stock available");
        }
        return ConstraintResult.passed(name(), type());
    }

    @Override
    public String name() { return "stockSufficient"; }

    @Override
    public ConstraintType type() { return ConstraintType.HARD; }

    @Override
    public int priority() { return 1; }
}
