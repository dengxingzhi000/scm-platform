package com.cognora.platform.inventory.service.dubbo;

import com.cognora.platform.inventory.api.exception.InsufficientStockException;
import com.cognora.platform.inventory.api.request.BatchDeductStockRequest;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import com.cognora.platform.inventory.service.RedisInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Dubbo 扣减 requestId 幂等用例。
 */
class InventoryDubboIdempotencyTest {

    private RedisInventoryService redisInventoryService;
    private InventoryDubboServiceImpl service;

    @BeforeEach
    void setUp() {
        redisInventoryService = mock(RedisInventoryService.class);
        service = new InventoryDubboServiceImpl(redisInventoryService, mock(InvInventoryMapper.class),
                mock(InventoryFlowLogService.class));
    }

    @Test
    @DisplayName("同一 requestId 重复扣减只执行一次")
    void repeatedRequestIdDeductsOnce() {
        when(redisInventoryService.acquireDeductRequest("REQ-1")).thenReturn(true, false);
        when(redisInventoryService.deductStock(anyString(), anyString(), anyInt())).thenReturn(true);

        service.deductStock(1001L, 2, "REQ-1");
        service.deductStock(1001L, 2, "REQ-1");

        verify(redisInventoryService, times(1)).deductStock(anyString(), anyString(), anyInt());
        verify(redisInventoryService, never()).releaseDeductRequest(anyString());
    }

    @Test
    @DisplayName("库存不足时释放幂等键，允许重试")
    void failedDeductionReleasesIdempotencyKey() {
        when(redisInventoryService.acquireDeductRequest("REQ-2")).thenReturn(true);
        when(redisInventoryService.deductStock(anyString(), anyString(), anyInt())).thenReturn(false);

        assertThrows(InsufficientStockException.class,
                () -> service.deductStock(1001L, 2, "REQ-2"));
        verify(redisInventoryService).releaseDeductRequest("REQ-2");
    }

    @Test
    @DisplayName("requestId 为空直接拒绝，避免无幂等扣减")
    void blankRequestIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.deductStock(1001L, 1, " "));
        assertThrows(IllegalArgumentException.class,
                () -> service.deductStock(1001L, 1, null));

        verify(redisInventoryService, never()).acquireDeductRequest(anyString());
        verify(redisInventoryService, never()).deductStock(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("批量扣减走单个原子脚本，不逐条调用 deductStock")
    void batchDeductUsesSingleAtomicCall() {
        BatchDeductStockRequest request = new BatchDeductStockRequest()
                .setRequestId("REQ-BATCH")
                .setItems(List.of(
                        new BatchDeductStockRequest.StockItem().setSkuId(1001L).setQuantity(1),
                        new BatchDeductStockRequest.StockItem().setSkuId(1002L).setQuantity(2)));

        when(redisInventoryService.acquireDeductRequest("REQ-BATCH")).thenReturn(true);
        when(redisInventoryService.batchDeductStock(anyList())).thenReturn(true);

        assertDoesNotThrow(() -> service.batchDeductStock(request));

        verify(redisInventoryService, times(1)).batchDeductStock(anyList());
        verify(redisInventoryService, never()).deductStock(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("批量扣减失败不留下已扣条目，并释放幂等键")
    void batchFailureLeavesNoPartialState() {
        BatchDeductStockRequest request = new BatchDeductStockRequest()
                .setRequestId("REQ-BATCH-FAIL")
                .setItems(List.of(
                        new BatchDeductStockRequest.StockItem().setSkuId(1001L).setQuantity(1),
                        new BatchDeductStockRequest.StockItem().setSkuId(1002L).setQuantity(9)));

        when(redisInventoryService.acquireDeductRequest("REQ-BATCH-FAIL")).thenReturn(true);
        when(redisInventoryService.batchDeductStock(anyList())).thenReturn(false);

        assertThrows(InsufficientStockException.class, () -> service.batchDeductStock(request));
        verify(redisInventoryService, never()).deductStock(anyString(), anyString(), anyInt());
        verify(redisInventoryService).releaseDeductRequest("REQ-BATCH-FAIL");
    }

    @Test
    @DisplayName("批量扣减的 item 非法时在扣减前拒绝")
    void invalidBatchItemIsRejectedBeforeDeduction() {
        BatchDeductStockRequest request = new BatchDeductStockRequest()
                .setRequestId("REQ-BAD")
                .setItems(List.of(new BatchDeductStockRequest.StockItem().setSkuId(1001L).setQuantity(0)));

        assertThrows(IllegalArgumentException.class, () -> service.batchDeductStock(request));
        verify(redisInventoryService, never()).acquireDeductRequest(anyString());
        verify(redisInventoryService, never()).batchDeductStock(anyList());
    }

    @Test
    @DisplayName("批量扣减重复 requestId 直接返回，不重复扣")
    void repeatedBatchRequestIsIdempotent() {
        BatchDeductStockRequest request = new BatchDeductStockRequest()
                .setRequestId("REQ-BATCH")
                .setItems(List.of(new BatchDeductStockRequest.StockItem().setSkuId(1001L).setQuantity(1)));

        when(redisInventoryService.acquireDeductRequest("REQ-BATCH")).thenReturn(false);

        assertDoesNotThrow(() -> service.batchDeductStock(request));
        verify(redisInventoryService, never()).batchDeductStock(anyList());
    }

    @Test
    @DisplayName("空批量请求在幂等键之前拒绝")
    void emptyBatchRequestIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> service.batchDeductStock(null));
        assertThrows(IllegalArgumentException.class,
                () -> service.batchDeductStock(new BatchDeductStockRequest().setRequestId("REQ-EMPTY")));

        verify(redisInventoryService, never()).acquireDeductRequest(anyString());
        verify(redisInventoryService, never()).deductStock(anyString(), anyString(), anyInt());
    }
}
