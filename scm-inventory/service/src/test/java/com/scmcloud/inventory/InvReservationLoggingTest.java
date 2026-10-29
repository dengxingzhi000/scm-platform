package com.scmcloud.inventory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.scmcloud.inventory.controller.InvReservationController;
import com.scmcloud.inventory.dto.InventoryReservationRequest;
import com.scmcloud.inventory.service.IInvReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvReservationLoggingTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(InvReservationController.class);
    private final IInvReservationService service = mock(IInvReservationService.class);
    private final InvReservationController controller = new InvReservationController(service);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level originalLevel;

    @BeforeEach
    void captureLogs() {
        originalLevel = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void restoreLogger() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(originalLevel);
    }

    @Test
    void releaseSanitizesLogsWithoutChangingBusinessKey() {
        String key = "order\r\nforged-entry";
        when(service.releaseReservation(key)).thenReturn(true);

        assertTrue(controller.releaseReservation(key));

        verify(service).releaseReservation(key);
        assertEquals(2, appender.list.size());
        assertSingleLineMessages("order__forged-entry");
    }

    @Test
    void queryLogsSanitizeKeysAndPreserveResults() {
        String key = "order\r\nforged-entry";
        when(service.checkReservationExists(key)).thenReturn(true);
        when(service.getReservedQuantity(key)).thenReturn(3);

        assertTrue(controller.checkReservationExists(key));
        assertEquals(3, controller.getReservedQuantity(key));

        verify(service).checkReservationExists(key);
        verify(service).getReservedQuantity(key);
        assertEquals(4, appender.list.size());
        assertSingleLineMessages("order__forged-entry");
    }

    @Test
    void releaseFailureSanitizesExceptionMessageAndPreservesException() {
        String key = "order\r\nforged-entry";
        RuntimeException failure = new IllegalStateException("invalid\r\npayload");
        when(service.releaseReservation(key)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> controller.releaseReservation(key)));

        assertSingleLineMessages("order__forged-entry");
        assertTrue(appender.list.getLast().getFormattedMessage().contains("invalid__payload"));
    }

    @Test
    void reserveValidationFailureSanitizesExceptionMessage() {
        InventoryReservationRequest request = new InventoryReservationRequest();
        request.setBusinessKey("order-1");
        request.setQuantity(1);
        IllegalArgumentException failure = new IllegalArgumentException("invalid\r\npayload");
        when(service.reserveInventory(request)).thenThrow(failure);

        assertSame(failure, assertThrows(IllegalArgumentException.class,
                () -> controller.reserveInventory(request)));

        String message = appender.list.getLast().getFormattedMessage();
        assertTrue(message.contains("invalid__payload"));
        assertFalse(message.contains("\r"));
        assertFalse(message.contains("\n"));
    }

    private void assertSingleLineMessages(String expectedKey) {
        assertFalse(appender.list.isEmpty());
        for (ILoggingEvent event : appender.list) {
            String message = event.getFormattedMessage();
            assertTrue(message.contains(expectedKey));
            assertFalse(message.contains("\r"));
            assertFalse(message.contains("\n"));
        }
    }
}
