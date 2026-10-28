package com.scmcloud.decision.config;

import com.scmcloud.decision.rule.InMemoryRuleRepository;
import com.scmcloud.decision.rule.RuleConflictDetector;
import com.scmcloud.decision.rule.RuleRepository;
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

    /** com.scmcloud.decision is outside the service component-scan roots, so register it here. */
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
