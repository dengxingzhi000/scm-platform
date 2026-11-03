package com.cognora.platform.system.controller;

import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.system.domain.entity.SysStatusDict;
import com.cognora.platform.system.domain.entity.SysStatusTransition;
import com.cognora.platform.system.service.ISysStatusDictService;
import com.cognora.platform.system.statemachine.StateMachineEngine;
import com.cognora.platform.system.statemachine.TransitionCheckResult;
import com.cognora.platform.system.statemachine.TransitionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysStatusDictControllerTest {

    @Mock
    private ISysStatusDictService statusDictService;

    @Mock
    private StateMachineEngine stateMachineEngine;

    @InjectMocks
    private SysStatusDictController controller;

    @Test
    void createStatusDictDelegatesIdGenerationToService() {
        SysStatusDict entity = new SysStatusDict();
        entity.setBizType("ORDER");

        ApiResponse<Void> response = controller.createStatusDict(entity);

        assertEquals(200, response.code());
        assertNull(entity.getId());
        verify(statusDictService).createStatusDict(entity);
    }

    @Test
    void createTransitionDelegatesIdGenerationToService() {
        SysStatusTransition entity = new SysStatusTransition();
        entity.setBizType("ORDER");

        ApiResponse<Void> response = controller.createTransition(entity);

        assertEquals(200, response.code());
        assertNull(entity.getId());
        verify(statusDictService).createTransition(entity);
    }

    @Test
    void checkTransitionForwardsToStateMachineEngine() {
        TransitionCheckResult result = TransitionCheckResult.allow("ORDER", "PAID", "PENDING_SHIP");
        when(stateMachineEngine.canTransition("ORDER", "PAID", "PENDING_SHIP")).thenReturn(result);

        ApiResponse<TransitionCheckResult> response =
                controller.checkTransition("ORDER", "PAID", "PENDING_SHIP");

        assertEquals(200, response.code());
        assertTrue(response.data().isAllowed());
    }

    @Test
    void executeTransitionForwardsToStateMachineEngine() {
        TransitionResult result = TransitionResult.builder().success(true).build();
        when(stateMachineEngine.transition("ORDER", "PAID", "SHIP")).thenReturn(result);

        ApiResponse<TransitionResult> response = controller.executeTransition("ORDER", "PAID", "SHIP");

        assertEquals(200, response.code());
        assertTrue(response.data().isSuccess());
    }

    @Test
    void getValidNextStatusesReturnsEngineResult() {
        List<String> statuses = List.of("PENDING_SHIP", "CANCELLED");
        when(stateMachineEngine.getValidNextStatuses("ORDER", "PAID")).thenReturn(statuses);

        ApiResponse<List<String>> response = controller.getValidNextStatuses("ORDER", "PAID");

        assertEquals(200, response.code());
        assertEquals(statuses, response.data());
    }
}
