package com.cognora.platform.decision.config;

import com.cognora.platform.decision.rule.InMemoryRuleRepository;
import com.cognora.platform.decision.rule.RuleConflictDetector;
import com.cognora.platform.decision.rule.RuleRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class DecisionEngineAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RuleRepository.class)
    public RuleRepository ruleRepository() {
        return new InMemoryRuleRepository();
    }

    /** com.cognora.platform.decision is outside the service component-scan roots, so register it here. */
    @Bean
    @ConditionalOnMissingBean(RuleConflictDetector.class)
    public RuleConflictDetector ruleConflictDetector() {
        return new RuleConflictDetector();
    }

    @Bean
    @ConditionalOnMissingBean(WeightRepository.class)
    public WeightRepository weightRepository() {
        return new InMemoryWeightRepository();
    }
}
