package com.scmcloud.decision.execution;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DecisionPlan<C> {
    private String planId;
    private String engineType;
    private List<Command<C>> commands;
    private List<Command<C>> rollbackCommands;
    private Map<String, Object> metadata;
}
