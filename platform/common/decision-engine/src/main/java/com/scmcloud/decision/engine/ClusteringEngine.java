package com.scmcloud.decision.engine;

import java.util.List;

public interface ClusteringEngine<I, O> extends DecisionEngine<I, List<O>> {
    List<O> cluster(I input);
}
