package com.scmcloud.decision.rule;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleConflictDetectorTest {

    @Test
    void detectsOverlappingScoringRules() {
        DecisionRule rule1 = new DecisionRule();
        rule1.setId("R1");
        rule1.setRuleType("SCORING");
        rule1.setDescription("priceScore");

        DecisionRule rule2 = new DecisionRule();
        rule2.setId("R2");
        rule2.setRuleType("SCORING");
        rule2.setDescription("priceScore");

        RuleConflictDetector detector = new RuleConflictDetector();
        List<RuleConflictDetector.Conflict> conflicts = detector.detect(List.of(rule1, rule2));

        assertEquals(1, conflicts.size());
        assertEquals(RuleConflictDetector.ConflictType.OVERLAPPING, conflicts.get(0).getType());
    }

    @Test
    void noConflict_forDifferentDimensions() {
        DecisionRule rule1 = new DecisionRule();
        rule1.setId("R1");
        rule1.setRuleType("SCORING");
        rule1.setDescription("priceScore");

        DecisionRule rule2 = new DecisionRule();
        rule2.setId("R2");
        rule2.setRuleType("SCORING");
        rule2.setDescription("qualityScore");

        RuleConflictDetector detector = new RuleConflictDetector();
        List<RuleConflictDetector.Conflict> conflicts = detector.detect(List.of(rule1, rule2));

        assertTrue(conflicts.isEmpty());
    }
}
