package com.scmcloud.decision.execution;

import lombok.Data;

@Data
public class Command<C> {
    private String commandType;
    private C payload;
    private int order;
}
