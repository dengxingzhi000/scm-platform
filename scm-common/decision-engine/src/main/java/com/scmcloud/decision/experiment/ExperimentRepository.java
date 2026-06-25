package com.scmcloud.decision.experiment;

import java.util.List;

public interface ExperimentRepository {
    List<Experiment> findRunningByEngineType(String engineType);
    Experiment save(Experiment experiment);
    void start(String id);
    void stop(String id);
}
