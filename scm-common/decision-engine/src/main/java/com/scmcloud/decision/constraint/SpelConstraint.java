package com.scmcloud.decision.constraint;

import com.scmcloud.decision.engine.ConstraintResult;
import com.scmcloud.decision.engine.ConstraintType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

@Slf4j
public class SpelConstraint<C> implements Constraint<C> {
    private static final ExpressionParser parser = new SpelExpressionParser();

    private final String name;
    private final String expression;
    private final ConstraintType type;
    private final int priority;
    private final String errorCode;
    private final String errorMessage;
    private final double penaltyWeight;

    public SpelConstraint(String name, String expression, ConstraintType type,
                          int priority, String errorCode, String errorMessage) {
        this(name, expression, type, priority, errorCode, errorMessage, 0.0);
    }

    public SpelConstraint(String name, String expression, ConstraintType type,
                          int priority, String errorCode, String errorMessage, double penaltyWeight) {
        this.name = name;
        this.expression = expression;
        this.type = type;
        this.priority = priority;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.penaltyWeight = penaltyWeight;
    }

    @Override
    public ConstraintResult validate(C context) {
        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext();
            if (context instanceof Map) {
                ((Map<?, ?>) context).forEach((k, v) -> evalContext.setVariable(String.valueOf(k), v));
            } else {
                evalContext.setVariable("ctx", context);
            }

            Expression exp = parser.parseExpression(expression);
            Boolean passed = exp.getValue(evalContext, Boolean.class);

            if (Boolean.TRUE.equals(passed)) {
                return ConstraintResult.passed(name, type);
            } else if (type == ConstraintType.SOFT) {
                return ConstraintResult.softPenalty(name, errorCode, errorMessage, penaltyWeight);
            } else {
                return ConstraintResult.failed(name, type, errorCode, errorMessage);
            }
        } catch (Exception e) {
            log.error("Constraint {} evaluation failed: {}", name, e.getMessage());
            return ConstraintResult.failed(name, type, "EVAL_ERROR", "Expression evaluation error: " + e.getMessage());
        }
    }

    @Override
    public String name() { return name; }

    @Override
    public ConstraintType type() { return type; }

    @Override
    public int priority() { return priority; }
}
