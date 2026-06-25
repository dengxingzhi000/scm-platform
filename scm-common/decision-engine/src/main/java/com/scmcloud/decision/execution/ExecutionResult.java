package com.scmcloud.decision.execution;

import lombok.Data;
import java.util.List;

@Data
public class ExecutionResult<R> {
    private boolean success;
    private R result;
    private List<CommandResult> commandResults;
    private String failureReason;

    @Data
    public static class CommandResult {
        private String commandType;
        private boolean success;
        private String error;
    }
}
