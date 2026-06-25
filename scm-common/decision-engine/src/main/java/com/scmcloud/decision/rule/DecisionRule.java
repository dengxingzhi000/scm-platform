package com.scmcloud.decision.rule;

import lombok.Data;

@Data
public class DecisionRule {
    private String id;
    private String engineType;
    private String scene;
    private String ruleType;
    private String expression;
    private String description;
    private boolean enabled;
    private int priority;
    private int version;
}
