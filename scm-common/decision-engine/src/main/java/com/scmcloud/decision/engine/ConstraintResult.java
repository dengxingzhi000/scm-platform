package com.scmcloud.decision.engine;

import lombok.Data;

@Data
public class ConstraintResult {
    private String name;
    private ConstraintType type;
    private boolean passed;
    private String code;
    private String reason;
    private double penalty;

    public static ConstraintResult passed(String name, ConstraintType type) {
        ConstraintResult r = new ConstraintResult();
        r.setName(name);
        r.setType(type);
        r.setPassed(true);
        return r;
    }

    public static ConstraintResult failed(String name, ConstraintType type, String code, String reason) {
        ConstraintResult r = new ConstraintResult();
        r.setName(name);
        r.setType(type);
        r.setPassed(false);
        r.setCode(code);
        r.setReason(reason);
        return r;
    }

    public static ConstraintResult softPenalty(String name, String code, String reason, double penalty) {
        ConstraintResult r = new ConstraintResult();
        r.setName(name);
        r.setType(ConstraintType.SOFT);
        r.setPassed(false);
        r.setCode(code);
        r.setReason(reason);
        r.setPenalty(penalty);
        return r;
    }
}
