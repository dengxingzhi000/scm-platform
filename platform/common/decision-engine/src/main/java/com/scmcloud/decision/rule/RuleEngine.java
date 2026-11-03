package com.scmcloud.decision.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

@Slf4j
public class RuleEngine {
    private static final ExpressionParser parser = new SpelExpressionParser();

    public <T> T evaluate(String expression, Map<String, Object> variables, Class<T> resultType) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        variables.forEach(context::setVariable);

        try {
            Expression exp = parser.parseExpression(expression);
            return exp.getValue(context, resultType);
        } catch (Exception e) {
            log.error("Rule evaluation failed: expression={}, error={}", expression, e.getMessage());
            throw new RuntimeException("Rule evaluation failed", e);
        }
    }

    public boolean evaluateBoolean(String expression, Map<String, Object> variables) {
        return evaluate(expression, variables, Boolean.class);
    }
}
