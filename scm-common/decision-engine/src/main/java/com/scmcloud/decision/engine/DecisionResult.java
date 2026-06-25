package com.scmcloud.decision.engine;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DecisionResult<O> {
    private boolean success;
    private O output;
    private String weightProfileId;
    private String experimentId;
    private List<ConstraintResult> constraintResults;
    private Map<String, Object> metadata;

    public static <O> DecisionResult<O> success(O output) {
        DecisionResult<O> result = new DecisionResult<>();
        result.setSuccess(true);
        result.setOutput(output);
        return result;
    }

    public static <O> DecisionResult<O> failure(String reason) {
        DecisionResult<O> result = new DecisionResult<>();
        result.setSuccess(false);
        return result;
    }
}
