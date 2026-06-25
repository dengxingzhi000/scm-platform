package com.scmcloud.decision.experiment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class ExperimentRouter {
    private final ExperimentRepository repository;

    public Optional<Variant> route(String engineType, String userId) {
        List<Experiment> experiments = repository.findRunningByEngineType(engineType);
        if (experiments.isEmpty()) return Optional.empty();

        Experiment experiment = experiments.get(0);
        int bucket = hashToBucket(userId + ":" + engineType);

        int cumulative = 0;
        for (Variant variant : experiment.getVariants()) {
            cumulative += variant.getTrafficPercent();
            if (bucket < cumulative) {
                return Optional.of(variant);
            }
        }

        return Optional.empty();
    }

    private int hashToBucket(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Math.abs((hash[0] & 0xFF) % 100);
        } catch (Exception e) {
            return 0;
        }
    }
}
