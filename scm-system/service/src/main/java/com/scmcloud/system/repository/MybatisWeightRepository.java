package com.scmcloud.system.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scmcloud.decision.config.WeightCondition;
import com.scmcloud.decision.config.WeightProfile;
import com.scmcloud.decision.config.WeightRepository;
import com.scmcloud.decision.entity.WeightProfileEntity;
import com.scmcloud.system.mapper.WeightProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Persistent {@link WeightRepository} that replaces the in-memory default registered by
 * {@code DecisionEngineAutoConfiguration}.
 */
@Repository
@RequiredArgsConstructor
public class MybatisWeightRepository implements WeightRepository {

    private static final TypeReference<Map<String, Double>> WEIGHTS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<WeightCondition>> CONDITIONS_TYPE = new TypeReference<>() {
    };

    private final WeightProfileMapper weightMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<WeightProfile> findActive(String engineType, String scene) {
        return weightMapper.selectList(selectScoped(engineType, scene)
                        .eq(WeightProfileEntity::getActive, true)
                        .orderByDesc(WeightProfileEntity::getVersion))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<WeightProfile> findAllByEngineType(String engineType) {
        return weightMapper.selectList(new LambdaQueryWrapper<WeightProfileEntity>()
                        .eq(WeightProfileEntity::getEngineType, engineType)
                        .orderByDesc(WeightProfileEntity::getVersion))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public WeightProfile findById(String id) {
        WeightProfileEntity entity = weightMapper.selectById(id);
        return entity == null ? null : toDomain(entity);
    }

    @Override
    public WeightProfile save(WeightProfile profile) {
        WeightProfileEntity entity = toEntity(profile);
        if (weightMapper.selectById(entity.getId()) == null) {
            if (entity.getVersion() == null || entity.getVersion() <= 0) {
                entity.setVersion(nextVersion(entity.getEngineType(), entity.getScene()));
            }
            weightMapper.insert(entity);
        } else {
            weightMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public void activate(String id) {
        WeightProfileEntity target = weightMapper.selectById(id);
        if (target == null) {
            return;
        }

        WeightProfileEntity deactivate = new WeightProfileEntity();
        deactivate.setActive(false);
        weightMapper.update(deactivate, updateScoped(target.getEngineType(), target.getScene())
                .eq(WeightProfileEntity::getActive, true));

        target.setActive(true);
        weightMapper.updateById(target);
    }

    @Override
    public void deleteById(String id) {
        weightMapper.deleteById(id);
    }

    /** A blank scene means "every scene of this engine", which is what the UI sends by default. */
    private static LambdaQueryWrapper<WeightProfileEntity> selectScoped(String engineType, String scene) {
        LambdaQueryWrapper<WeightProfileEntity> query = new LambdaQueryWrapper<WeightProfileEntity>()
                .eq(WeightProfileEntity::getEngineType, engineType);
        if (StringUtils.hasText(scene)) {
            query.eq(WeightProfileEntity::getScene, scene);
        }
        return query;
    }

    /** Activation is scoped to the exact scene, so a scene-less profile only replaces other scene-less ones. */
    private static LambdaUpdateWrapper<WeightProfileEntity> updateScoped(String engineType, String scene) {
        LambdaUpdateWrapper<WeightProfileEntity> update = new LambdaUpdateWrapper<WeightProfileEntity>()
                .eq(WeightProfileEntity::getEngineType, engineType);
        if (scene == null) {
            update.isNull(WeightProfileEntity::getScene);
        } else {
            update.eq(WeightProfileEntity::getScene, scene);
        }
        return update;
    }

    private int nextVersion(String engineType, String scene) {
        return weightMapper.selectList(new LambdaQueryWrapper<WeightProfileEntity>()
                        .select(WeightProfileEntity::getVersion)
                        .eq(WeightProfileEntity::getEngineType, engineType)
                        .eq(StringUtils.hasText(scene), WeightProfileEntity::getScene, scene))
                .stream()
                .map(WeightProfileEntity::getVersion)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(0) + 1;
    }

    private WeightProfileEntity toEntity(WeightProfile profile) {
        WeightProfileEntity entity = new WeightProfileEntity();
        entity.setId(profile.getId());
        entity.setEngineType(profile.getEngineType());
        entity.setScene(profile.getScene());
        entity.setVersion(profile.getVersion() == 0 ? null : profile.getVersion());
        entity.setWeights(writeJson(profile.getWeights()));
        entity.setConditions(profile.getConditions() == null || profile.getConditions().isEmpty()
                ? null
                : writeJson(profile.getConditions()));
        entity.setActive(profile.isActive());
        return entity;
    }

    private WeightProfile toDomain(WeightProfileEntity entity) {
        WeightProfile profile = new WeightProfile();
        profile.setId(entity.getId());
        profile.setEngineType(entity.getEngineType());
        profile.setScene(entity.getScene());
        profile.setVersion(entity.getVersion() == null ? 0 : entity.getVersion());
        profile.setWeights(readWeights(entity.getWeights()));
        profile.setConditions(readConditions(entity.getConditions()));
        profile.setActive(Boolean.TRUE.equals(entity.getActive()));
        return profile;
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialise decision weight payload", exception);
        }
    }

    private Map<String, Double> readWeights(String json) {
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        return read(json, WEIGHTS_TYPE);
    }

    private List<WeightCondition> readConditions(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        return read(json, CONDITIONS_TYPE);
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to read decision weight payload: " + json, exception);
        }
    }
}
