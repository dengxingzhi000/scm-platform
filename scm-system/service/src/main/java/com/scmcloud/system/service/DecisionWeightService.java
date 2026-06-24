package com.scmcloud.system.service;

import com.scmcloud.decision.config.WeightProfile;
import com.scmcloud.decision.config.WeightRepository;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionWeightService {

    private final WeightRepository weightRepository;

    public List<WeightProfile> findActive(String engineType, String scene) {
        return weightRepository.findActive(engineType, scene);
    }

    public List<WeightProfile> findAllVersions(String engineType) {
        return weightRepository.findAllByEngineType(engineType);
    }

    public WeightProfile create(WeightProfile profile) {
        profile.setId(UUIDv7Util.generateString());
        profile.validate();
        return weightRepository.save(profile);
    }

    public void activate(String id) {
        weightRepository.activate(id);
    }

    public void delete(String id) {
        WeightProfile profile = weightRepository.findById(id);
        if (profile.isActive()) {
            throw new IllegalStateException("Cannot delete active profile");
        }
        weightRepository.deleteById(id);
    }
}
