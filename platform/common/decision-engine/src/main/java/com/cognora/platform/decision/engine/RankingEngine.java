package com.cognora.platform.decision.engine;

import java.util.List;

public interface RankingEngine<I, O> extends DecisionEngine<I, List<O>> {
    List<O> rank(I input);
}
