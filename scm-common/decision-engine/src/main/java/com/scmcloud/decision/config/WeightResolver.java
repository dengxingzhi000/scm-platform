package com.scmcloud.decision.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class WeightResolver {
    private final WeightRepository repository;

    public Optional<WeightProfile> resolve(String engineType, String scene, Map<String, Object> context) {
        List<WeightProfile> profiles = repository.findActive(engineType, scene);

        return profiles.stream()
                .filter(p -> p.matchesConditions(context))
                .findFirst();
    }

    public Map<String, Double> resolveWeights(String engineType, String scene, Map<String, Object> context) {
        return resolve(engineType, scene, context)
                .map(WeightProfile::getWeights)
                .orElseThrow(() -> new IllegalStateException("No active weight profile for " + engineType + ":" + scene));
    }
}
