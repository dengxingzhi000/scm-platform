package com.scmcloud.decision.rule;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RuleConflictDetector {

    public List<Conflict> detect(List<DecisionRule> rules) {
        List<Conflict> conflicts = new ArrayList<>();

        List<DecisionRule> scoringRules = rules.stream()
                .filter(r -> "SCORING".equals(r.getRuleType()))
                .toList();

        for (int i = 0; i < scoringRules.size(); i++) {
            for (int j = i + 1; j < scoringRules.size(); j++) {
                if (extractDimension(scoringRules.get(i)).equals(extractDimension(scoringRules.get(j)))) {
                    conflicts.add(new Conflict(
                            ConflictType.OVERLAPPING,
                            scoringRules.get(i).getId(),
                            scoringRules.get(j).getId(),
                            "Overlapping scoring dimension: " + extractDimension(scoringRules.get(i)),
                            ConflictSeverity.WARNING
                    ));
                }
            }
        }

        return conflicts;
    }

    private String extractDimension(DecisionRule rule) {
        return rule.getDescription() != null ? rule.getDescription() : rule.getId();
    }

    @Data
    public static class Conflict {
        private final ConflictType type;
        private final String ruleId1;
        private final String ruleId2;
        private final String description;
        private final ConflictSeverity severity;
    }

    public enum ConflictType { CONTRADICTORY, OVERLAPPING, UNREACHABLE }
    public enum ConflictSeverity { WARNING, ERROR }
}
