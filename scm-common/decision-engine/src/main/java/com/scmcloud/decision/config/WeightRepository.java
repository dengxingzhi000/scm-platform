package com.scmcloud.decision.config;

import java.util.List;

public interface WeightRepository {
    List<WeightProfile> findActive(String engineType, String scene);
    List<WeightProfile> findAllByEngineType(String engineType);
    WeightProfile findById(String id);
    WeightProfile save(WeightProfile profile);
    void activate(String id);
    void deleteById(String id);
}
