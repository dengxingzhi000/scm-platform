package com.scmcloud.decision.config;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boot-time default for hosts of the decision engine; registered only when no other
 * WeightRepository bean exists. Profiles kept here do not survive a restart.
 */
public class InMemoryWeightRepository implements WeightRepository {

    private final Map<String, WeightProfile> profiles = new ConcurrentHashMap<>();

    @Override
    public List<WeightProfile> findActive(String engineType, String scene) {
        return profiles.values().stream()
                .filter(profile -> profile.isActive()
                        && engineType.equals(profile.getEngineType())
                        && scene.equals(profile.getScene()))
                .sorted(Comparator.comparingInt(WeightProfile::getVersion).reversed())
                .toList();
    }

    @Override
    public List<WeightProfile> findAllByEngineType(String engineType) {
        return profiles.values().stream()
                .filter(profile -> engineType.equals(profile.getEngineType()))
                .toList();
    }

    @Override
    public WeightProfile findById(String id) {
        return profiles.get(id);
    }

    @Override
    public WeightProfile save(WeightProfile profile) {
        profiles.put(profile.getId(), profile);
        return profile;
    }

    @Override
    public void activate(String id) {
        WeightProfile target = profiles.get(id);
        if (target == null) {
            return;
        }
        profiles.values().stream()
                .filter(profile -> target.getEngineType().equals(profile.getEngineType())
                        && target.getScene().equals(profile.getScene()))
                .forEach(profile -> profile.setActive(false));
        target.setActive(true);
    }

    @Override
    public void deleteById(String id) {
        profiles.remove(id);
    }
}
