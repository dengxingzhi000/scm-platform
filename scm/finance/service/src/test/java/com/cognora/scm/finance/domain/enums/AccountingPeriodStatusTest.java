package com.cognora.scm.finance.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账期状态机：重开是唯一的合法回退路径，其余流转全部拒绝。
 */
class AccountingPeriodStatusTest {

    @Test
    void openShouldOnlyTransitionToClosed() {
        assertTrue(AccountingPeriodStatus.OPEN.canTransitionTo(AccountingPeriodStatus.CLOSED));
        assertFalse(AccountingPeriodStatus.OPEN.canTransitionTo(AccountingPeriodStatus.REOPENED));
        assertFalse(AccountingPeriodStatus.OPEN.canTransitionTo(AccountingPeriodStatus.OPEN));
    }

    @Test
    void closedShouldOnlyTransitionToReopened() {
        assertTrue(AccountingPeriodStatus.CLOSED.canTransitionTo(AccountingPeriodStatus.REOPENED));
        assertFalse(AccountingPeriodStatus.CLOSED.canTransitionTo(AccountingPeriodStatus.CLOSED));
        assertFalse(AccountingPeriodStatus.CLOSED.canTransitionTo(AccountingPeriodStatus.OPEN));
    }

    @Test
    void reopenedShouldOnlyTransitionToClosed() {
        assertTrue(AccountingPeriodStatus.REOPENED.canTransitionTo(AccountingPeriodStatus.CLOSED));
        assertFalse(AccountingPeriodStatus.REOPENED.canTransitionTo(AccountingPeriodStatus.REOPENED));
        assertFalse(AccountingPeriodStatus.REOPENED.canTransitionTo(AccountingPeriodStatus.OPEN));
    }

    @Test
    void nullTargetShouldBeRejected() {
        assertFalse(AccountingPeriodStatus.OPEN.canTransitionTo(null));
        assertFalse(AccountingPeriodStatus.CLOSED.canTransitionTo(null));
        assertFalse(AccountingPeriodStatus.REOPENED.canTransitionTo(null));
    }

    @Test
    void fromCodeShouldMapAllKnownStatuses() {
        assertEquals(AccountingPeriodStatus.OPEN, AccountingPeriodStatus.fromCode(0));
        assertEquals(AccountingPeriodStatus.CLOSED, AccountingPeriodStatus.fromCode(1));
        assertEquals(AccountingPeriodStatus.REOPENED, AccountingPeriodStatus.fromCode(2));
    }

    @Test
    void fromCodeShouldRejectNullAndUnknown() {
        assertThrows(IllegalArgumentException.class, () -> AccountingPeriodStatus.fromCode(null));
        assertThrows(IllegalArgumentException.class, () -> AccountingPeriodStatus.fromCode(99));
    }
}
