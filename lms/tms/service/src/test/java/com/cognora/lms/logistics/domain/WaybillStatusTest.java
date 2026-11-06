package com.cognora.lms.logistics.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 批次 2：运单状态机规则用例。
 */
class WaybillStatusTest {

    @Test
    void forwardChainShouldBeAllowed() {
        WaybillStatus.assertTransitionAllowed(WaybillStatus.CREATED, WaybillStatus.PICKED_UP);
        WaybillStatus.assertTransitionAllowed(WaybillStatus.PICKED_UP, WaybillStatus.IN_TRANSIT);
        WaybillStatus.assertTransitionAllowed(WaybillStatus.IN_TRANSIT, WaybillStatus.OUT_FOR_DELIVERY);
        WaybillStatus.assertTransitionAllowed(WaybillStatus.OUT_FOR_DELIVERY, WaybillStatus.DELIVERED);
    }

    @Test
    void skippingIntermediateStatesManuallyShouldBeRejected() {
        assertThrows(IllegalStatusTransitionException.class,
                () -> WaybillStatus.assertTransitionAllowed(WaybillStatus.CREATED, WaybillStatus.DELIVERED));
        assertThrows(IllegalStatusTransitionException.class,
                () -> WaybillStatus.assertTransitionAllowed(WaybillStatus.CREATED, WaybillStatus.OUT_FOR_DELIVERY));
    }

    @Test
    void deliveredAndReturnedAreIrreversible() {
        assertThrows(IllegalStatusTransitionException.class,
                () -> WaybillStatus.assertTransitionAllowed(WaybillStatus.DELIVERED, WaybillStatus.OUT_FOR_DELIVERY));
        assertThrows(IllegalStatusTransitionException.class,
                () -> WaybillStatus.assertTransitionAllowed(WaybillStatus.RETURNED, WaybillStatus.CREATED));
        assertTrue(WaybillStatus.isTerminal(WaybillStatus.DELIVERED));
        assertTrue(WaybillStatus.isTerminal(WaybillStatus.RETURNED));
        assertFalse(WaybillStatus.isTerminal(WaybillStatus.EXCEPTION));
    }

    @Test
    void sameStatusIsIdempotentReplay() {
        WaybillStatus.assertTransitionAllowed(WaybillStatus.DELIVERED, WaybillStatus.DELIVERED);
        WaybillStatus.assertTransitionAllowed(WaybillStatus.IN_TRANSIT, WaybillStatus.IN_TRANSIT);
    }

    @Test
    void unknownStatusShouldBeRejected() {
        assertThrows(IllegalStatusTransitionException.class,
                () -> WaybillStatus.assertTransitionAllowed(9, WaybillStatus.CREATED));
        assertThrows(IllegalStatusTransitionException.class,
                () -> WaybillStatus.assertTransitionAllowed(WaybillStatus.CREATED, 9));
        assertThrows(IllegalStatusTransitionException.class,
                () -> WaybillStatus.assertTransitionAllowed(null, WaybillStatus.CREATED));
    }

    @Test
    void exceptionCanRecoverButCannotMoveToExceptionFromTerminal() {
        WaybillStatus.assertTransitionAllowed(WaybillStatus.EXCEPTION, WaybillStatus.IN_TRANSIT);
        WaybillStatus.assertTransitionAllowed(WaybillStatus.EXCEPTION, WaybillStatus.OUT_FOR_DELIVERY);
        assertThrows(IllegalStatusTransitionException.class,
                () -> WaybillStatus.assertTransitionAllowed(WaybillStatus.DELIVERED, WaybillStatus.EXCEPTION));
    }

    @Test
    void cancellationShouldOnlyBeAllowedBeforeDeliveryAttempt() {
        assertTrue(WaybillStatus.isCancellable(WaybillStatus.CREATED));
        assertTrue(WaybillStatus.isCancellable(WaybillStatus.PICKED_UP));
        assertTrue(WaybillStatus.isCancellable(WaybillStatus.IN_TRANSIT));
        assertTrue(WaybillStatus.isCancellable(WaybillStatus.EXCEPTION));
        assertFalse(WaybillStatus.isCancellable(WaybillStatus.OUT_FOR_DELIVERY));
        assertFalse(WaybillStatus.isCancellable(WaybillStatus.DELIVERED));
        assertFalse(WaybillStatus.isCancellable(WaybillStatus.RETURNED));
    }

    @Test
    void trackingAdvanceShouldAllowForwardSkipButNotBackward() {
        assertTrue(WaybillStatus.isTrackingAdvanceAllowed(WaybillStatus.IN_TRANSIT, WaybillStatus.DELIVERED));
        assertTrue(WaybillStatus.isTrackingAdvanceAllowed(WaybillStatus.CREATED, WaybillStatus.IN_TRANSIT));
        assertTrue(WaybillStatus.isTrackingAdvanceAllowed(WaybillStatus.PICKED_UP, WaybillStatus.OUT_FOR_DELIVERY));
        assertFalse(WaybillStatus.isTrackingAdvanceAllowed(WaybillStatus.OUT_FOR_DELIVERY, WaybillStatus.IN_TRANSIT));
        assertFalse(WaybillStatus.isTrackingAdvanceAllowed(WaybillStatus.DELIVERED, WaybillStatus.OUT_FOR_DELIVERY));
        assertFalse(WaybillStatus.isTrackingAdvanceAllowed(WaybillStatus.RETURNED, WaybillStatus.IN_TRANSIT));
    }

    @Test
    void trackingEventMappingShouldHandleChineseAndUnknownValues() {
        assertEquals(WaybillStatus.PICKED_UP, WaybillStatus.fromTrackingEvent("PICKED_UP"));
        assertEquals(WaybillStatus.DELIVERED, WaybillStatus.fromTrackingEvent("signed"));
        assertEquals(WaybillStatus.IN_TRANSIT, WaybillStatus.fromTrackingEvent("运输中"));
        assertEquals(-1, WaybillStatus.fromTrackingEvent("UNKNOWN_EVENT"));
        assertEquals(-1, WaybillStatus.fromTrackingEvent(null));
    }
}
