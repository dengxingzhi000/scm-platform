package com.cognora.scm.finance.domain.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BadDebtWriteOffStatusTest {

    @Test
    void pendingCanBeApprovedOrRejected() {
        assertTrue(BadDebtWriteOffStatus.PENDING.canTransitionTo(BadDebtWriteOffStatus.APPROVED));
        assertTrue(BadDebtWriteOffStatus.PENDING.canTransitionTo(BadDebtWriteOffStatus.REJECTED));
    }

    @Test
    void pendingCannotBeWrittenOffDirectly() {
        assertFalse(BadDebtWriteOffStatus.PENDING.canTransitionTo(BadDebtWriteOffStatus.WRITTEN_OFF));
    }

    @Test
    void approvedCanOnlyBeWrittenOff() {
        assertTrue(BadDebtWriteOffStatus.APPROVED.canTransitionTo(BadDebtWriteOffStatus.WRITTEN_OFF));
        assertFalse(BadDebtWriteOffStatus.APPROVED.canTransitionTo(BadDebtWriteOffStatus.REJECTED));
        assertFalse(BadDebtWriteOffStatus.APPROVED.canTransitionTo(BadDebtWriteOffStatus.APPROVED));
    }

    @Test
    void terminalStatusesCannotTransition() {
        for (BadDebtWriteOffStatus target : BadDebtWriteOffStatus.values()) {
            assertFalse(BadDebtWriteOffStatus.WRITTEN_OFF.canTransitionTo(target));
            assertFalse(BadDebtWriteOffStatus.REJECTED.canTransitionTo(target));
        }
    }

    @Test
    void nullTargetIsRejected() {
        assertFalse(BadDebtWriteOffStatus.APPROVED.canTransitionTo(null));
    }

    @Test
    void fromCodeMapsKnownCodesAndIgnoresUnknown() {
        assertTrue(BadDebtWriteOffStatus.fromCode(0) == BadDebtWriteOffStatus.PENDING);
        assertTrue(BadDebtWriteOffStatus.fromCode(1) == BadDebtWriteOffStatus.APPROVED);
        assertTrue(BadDebtWriteOffStatus.fromCode(2) == BadDebtWriteOffStatus.WRITTEN_OFF);
        assertTrue(BadDebtWriteOffStatus.fromCode(3) == BadDebtWriteOffStatus.REJECTED);
        assertNull(BadDebtWriteOffStatus.fromCode(99));
        assertNull(BadDebtWriteOffStatus.fromCode(null));
    }

    @Test
    void rejectedDoesNotOccupyTargetButOthersDo() {
        assertFalse(BadDebtWriteOffStatus.REJECTED.isActive());
        assertTrue(BadDebtWriteOffStatus.PENDING.isActive());
        assertTrue(BadDebtWriteOffStatus.APPROVED.isActive());
        assertTrue(BadDebtWriteOffStatus.WRITTEN_OFF.isActive());
    }
}
