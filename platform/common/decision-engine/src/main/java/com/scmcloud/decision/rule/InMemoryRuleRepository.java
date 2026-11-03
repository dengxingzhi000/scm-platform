package com.scmcloud.decision.rule;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Boot-time default so services that host the decision engine can start before a persistent
 * rule store is wired. Registered by auto-configuration only when no other RuleRepository exists;
 * rules kept here do not survive a restart.
 */
public class InMemoryRuleRepository implements RuleRepository {

    private final Map<String, DecisionRule> rules = new ConcurrentHashMap<>();

    @Override
    public List<DecisionRule> findByEngineType(String engineType) {
        return rules.values().stream()
                .filter(rule -> engineType.equals(rule.getEngineType()))
                .toList();
    }

    @Override
    public List<DecisionRule> findByEngineTypeAndRuleType(String engineType, String ruleType) {
        return rules.values().stream()
                .filter(rule -> engineType.equals(rule.getEngineType()) && ruleType.equals(rule.getRuleType()))
                .toList();
    }

    @Override
    public DecisionRule save(DecisionRule rule) {
        rules.put(rule.getId(), rule);
        return rule;
    }

    @Override
    public void delete(String id) {
        rules.remove(id);
    }

    @Override
    public void toggle(String id, boolean enabled) {
        DecisionRule rule = rules.get(id);
        if (rule != null) {
            rule.setEnabled(enabled);
        }
    }
}
