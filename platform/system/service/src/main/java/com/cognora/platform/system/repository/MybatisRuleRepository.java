package com.cognora.platform.system.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cognora.platform.decision.entity.DecisionRuleEntity;
import com.cognora.platform.decision.rule.DecisionRule;
import com.cognora.platform.decision.rule.RuleRepository;
import com.cognora.platform.system.mapper.DecisionRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Persistent {@link RuleRepository} that replaces the in-memory default registered by
 * {@code DecisionEngineAutoConfiguration} (its {@code @ConditionalOnMissingBean} backs off
 * once this bean is scanned).
 */
@Repository
@RequiredArgsConstructor
public class MybatisRuleRepository implements RuleRepository {

    private static final int DEFAULT_PRIORITY = 100;
    private static final int DEFAULT_VERSION = 1;

    private final DecisionRuleMapper ruleMapper;

    @Override
    public List<DecisionRule> findByEngineType(String engineType) {
        return ruleMapper.selectList(new LambdaQueryWrapper<DecisionRuleEntity>()
                        .eq(DecisionRuleEntity::getEngineType, engineType)
                        .orderByAsc(DecisionRuleEntity::getPriority)
                        .orderByAsc(DecisionRuleEntity::getCreateTime))
                .stream()
                .map(MybatisRuleRepository::toDomain)
                .toList();
    }

    @Override
    public List<DecisionRule> findByEngineTypeAndRuleType(String engineType, String ruleType) {
        return ruleMapper.selectList(new LambdaQueryWrapper<DecisionRuleEntity>()
                        .eq(DecisionRuleEntity::getEngineType, engineType)
                        .eq(DecisionRuleEntity::getRuleType, ruleType)
                        .orderByAsc(DecisionRuleEntity::getPriority)
                        .orderByAsc(DecisionRuleEntity::getCreateTime))
                .stream()
                .map(MybatisRuleRepository::toDomain)
                .toList();
    }

    @Override
    public DecisionRule save(DecisionRule rule) {
        DecisionRuleEntity entity = toEntity(rule);
        if (ruleMapper.selectById(entity.getId()) == null) {
            ruleMapper.insert(entity);
        } else {
            ruleMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public void delete(String id) {
        ruleMapper.deleteById(id);
    }

    @Override
    public void toggle(String id, boolean enabled) {
        DecisionRuleEntity patch = new DecisionRuleEntity();
        patch.setId(id);
        patch.setEnabled(enabled);
        ruleMapper.updateById(patch);
    }

    private static DecisionRuleEntity toEntity(DecisionRule rule) {
        DecisionRuleEntity entity = new DecisionRuleEntity();
        entity.setId(rule.getId());
        entity.setEngineType(rule.getEngineType());
        entity.setScene(rule.getScene());
        entity.setRuleType(rule.getRuleType());
        entity.setExpression(rule.getExpression());
        entity.setDescription(rule.getDescription());
        entity.setEnabled(rule.isEnabled());
        entity.setPriority(rule.getPriority());
        entity.setVersion(rule.getVersion());
        return entity;
    }

    private static DecisionRule toDomain(DecisionRuleEntity entity) {
        DecisionRule rule = new DecisionRule();
        rule.setId(entity.getId());
        rule.setEngineType(entity.getEngineType());
        rule.setScene(entity.getScene());
        rule.setRuleType(entity.getRuleType());
        rule.setExpression(entity.getExpression());
        rule.setDescription(entity.getDescription());
        rule.setEnabled(Boolean.TRUE.equals(entity.getEnabled()));
        rule.setPriority(entity.getPriority() == null ? DEFAULT_PRIORITY : entity.getPriority());
        rule.setVersion(entity.getVersion() == null ? DEFAULT_VERSION : entity.getVersion());
        return rule;
    }
}
