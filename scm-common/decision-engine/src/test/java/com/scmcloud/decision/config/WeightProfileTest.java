package com.scmcloud.decision.config;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeightProfileTest {

    @Test
    void validate_throwsWhenWeightsNotSumToOne() {
        WeightProfile profile = new WeightProfile();
        profile.setWeights(Map.of("a", 0.3, "b", 0.3));

        assertThrows(IllegalArgumentException.class, profile::validate);
    }

    @Test
    void validate_passesWhenWeightsSumToOne() {
        WeightProfile profile = new WeightProfile();
        profile.setWeights(Map.of("a", 0.4, "b", 0.6));

        assertDoesNotThrow(profile::validate);
    }

    @Test
    void matchesConditions_withEquality() {
        WeightProfile profile = new WeightProfile();
        WeightCondition condition = new WeightCondition();
        condition.setField("orderType");
        condition.setOperator("==");
        condition.setValue("FLASH_SALE");
        profile.setConditions(List.of(condition));

        assertTrue(profile.matchesConditions(Map.of("orderType", "FLASH_SALE")));
        assertFalse(profile.matchesConditions(Map.of("orderType", "NORMAL")));
    }
}
