package com.scmcloud.purchase.engine;

import com.scmcloud.decision.constraint.Constraint;
import com.scmcloud.decision.engine.ConstraintResult;
import com.scmcloud.decision.engine.ConstraintType;
import com.scmcloud.purchase.engine.PriceComparisonInput.SupplierQuote;

public class SupplierConstraint implements Constraint<SupplierQuote> {
    @Override
    public ConstraintResult validate(SupplierQuote context) {
        if (!"ACTIVE".equals(context.getSupplierStatus())) {
            return ConstraintResult.failed(name(), type(), "SUPPLIER_001", "Supplier not active");
        }
        return ConstraintResult.passed(name(), type());
    }

    @Override
    public String name() { return "supplierEnabled"; }

    @Override
    public ConstraintType type() { return ConstraintType.HARD; }

    @Override
    public int priority() { return 1; }
}
