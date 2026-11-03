package com.scmcloud.system.service;

import com.scmcloud.decision.rule.DecisionRule;
import com.scmcloud.decision.rule.RuleConflictDetector;
import com.scmcloud.decision.rule.RuleRepository;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionRuleService {

    private final RuleRepository ruleRepository;
    private final RuleConflictDetector conflictDetector;

    public List<DecisionRule> findByEngineType(String engineType) {
        return ruleRepository.findByEngineType(engineType);
    }

    public DecisionRule create(DecisionRule rule) {
        rule.setId(UUIDv7Util.generateString());
        rule.setEnabled(true);
        rule.setVersion(1);
        return ruleRepository.save(rule);
    }

    public DecisionRule update(String id, DecisionRule rule) {
        rule.setId(id);
        return ruleRepository.save(rule);
    }

    public void toggle(String id, boolean enabled) {
        ruleRepository.toggle(id, enabled);
    }

    public void delete(String id) {
        ruleRepository.delete(id);
    }

    public List<RuleConflictDetector.Conflict> detectConflicts(String engineType) {
        List<DecisionRule> rules = ruleRepository.findByEngineType(engineType);
        return conflictDetector.detect(rules);
    }
}
