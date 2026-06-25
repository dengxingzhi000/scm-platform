package com.scmcloud.decision.scoring;

public interface Scorer<T> {
    double score(T target, ScoringContext ctx);
    String dimension();
    double weight();
}
