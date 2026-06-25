package com.scmcloud.decision.scoring;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class SpelScorer<T> implements Scorer<T> {
    private static final ExpressionParser parser = new SpelExpressionParser();

    private final String dimension;
    private final String expression;
    private final double weight;

    public SpelScorer(String dimension, String expression, double weight) {
        this.dimension = dimension;
        this.expression = expression;
        this.weight = weight;
    }

    @Override
    public double score(T target, ScoringContext ctx) {
        StandardEvaluationContext evalContext = new StandardEvaluationContext();
        evalContext.setVariable("target", target);
        ctx.getVariables().forEach(evalContext::setVariable);

        Expression exp = parser.parseExpression(expression);
        Double score = exp.getValue(evalContext, Double.class);
        return score != null ? score : 0.0;
    }

    @Override
    public String dimension() {
        return dimension;
    }

    @Override
    public double weight() {
        return weight;
    }
}
