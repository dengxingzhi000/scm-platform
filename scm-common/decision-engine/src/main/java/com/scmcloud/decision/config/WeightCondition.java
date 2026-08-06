package com.scmcloud.decision.config;

import lombok.Data;
import java.util.List;
import java.util.Objects;

@Data
public class WeightCondition {
    private String field;
    private String operator;
    private Object value;

    public boolean matches(Object actual) {
        return switch (operator) {
            case "==" -> Objects.equals(actual, value);
            case "!=" -> !Objects.equals(actual, value);
            case "in" -> ((List<?>) value).contains(actual);
            case ">" -> ((Comparable<Object>) actual).compareTo(value) > 0;
            case "<" -> ((Comparable<Object>) actual).compareTo(value) < 0;
            default -> false;
        };
    }
}
