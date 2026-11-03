package com.scmcloud.decision.rule;

import java.util.List;

public interface RuleRepository {
    List<DecisionRule> findByEngineType(String engineType);
    List<DecisionRule> findByEngineTypeAndRuleType(String engineType, String ruleType);
    DecisionRule save(DecisionRule rule);
    void delete(String id);
    void toggle(String id, boolean enabled);
}
