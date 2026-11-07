package com.cognora.platform.inventory.service.command;

import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.common.integration.outbox.OutboxService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.InvCostLog;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InventoryAdjustRequest;
import com.cognora.platform.inventory.dto.InventoryTransferRequest;
import com.cognora.platform.inventory.mapper.InvCostLogMapper;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.service.batch.FefoAllocation;
import com.cognora.platform.inventory.service.batch.FefoAllocationPlan;
import com.cognora.platform.inventory.service.batch.FefoAllocationService;
import com.cognora.platform.inventory.service.cost.CostMovement;
import com.cognora.platform.inventory.service.cost.InventoryCostLogService;
import com.cognora.platform.inventory.service.cost.MovingAverageCostService;
import com.cognora.platform.inventory.service.flow.FlowLogEntry;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 批次出库记账用例：跨批次逐条写数量/成本流水、对手键与幂等键口径、分配失败不落流水。
 */
class InvInventoryCommandServiceFefoTest {

  private static final String TENANT = "019a0aee-0000-7000-8000-000000000002";
  private static final String SKU = "22222222-2222-2222-2222-222222222222";
  private static final String FROM_WH = "33333333-3333-3333-3333-333333333333";
  private static final String TO_WH = "55555555-5555-5555-5555-555555555555";
  private static final String INVENTORY_ID = "11111111-1111-1111-1111-111111111111";
  private static final String TO_ID = "77777777-7777-7777-7777-777777777777";
  private static final String OPERATOR = "44444444-4444-4444-4444-444444444444";
  private static final String BATCH_A_ID = "aaaaaaaa-0000-0000-0000-000000000001";
  private static final String BATCH_B_ID = "bbbbbbbb-0000-0000-0000-000000000002";

  private InvInventoryMapper inventoryMapper;
  private InvCostLogMapper costLogMapper;
  private InventoryFlowLogService flowLog;
  private MovingAverageCostService costService;
  private FefoAllocationService fefo;
  private InvInventoryCommandService service;

  @BeforeEach
  void setUp() {
    TenantContextHolder.setTenantId(UUID.fromString(TENANT));
    inventoryMapper = mock(InvInventoryMapper.class);
    costLogMapper = mock(InvCostLogMapper.class);
    flowLog = mock(InventoryFlowLogService.class);
    costService = mock(MovingAverageCostService.class);
    fefo = mock(FefoAllocationService.class);
    when(costLogMapper.insertLog(any(InvCostLog.class))).thenReturn(1);
    service = new InvInventoryCommandService(inventoryMapper, mock(OutboxService.class), flowLog,
        costService, new InventoryCostLogService(costLogMapper), fefo);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  @DisplayName("出库跨两个批次：数量流水与成本流水按批次逐条写，幂等键带批次后缀")
  void outboundAcrossTwoBatchesWritesPerBatchRows() {
    Inventory existed = ledger(INVENTORY_ID, 10, 10, new BigDecimal("8"));
    Inventory fresh = ledger(INVENTORY_ID, 4, 4, new BigDecimal("8"));
    when(inventoryMapper.selectOne(any())).thenReturn(existed);
    when(inventoryMapper.applyAdjustment(anyString(), anyInt(), anyInt(), anyString())).thenReturn(1);
    when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(fresh);
    when(fefo.allocate(TENANT, SKU, FROM_WH, 6)).thenReturn(twoBatchPlan(6));

    service.adjustInventory(adjustRequest(-6));

    ArgumentCaptor<FlowLogEntry> flowCaptor = ArgumentCaptor.forClass(FlowLogEntry.class);
    verify(flowLog, times(2)).record(flowCaptor.capture());
    List<FlowLogEntry> flows = flowCaptor.getAllValues();

    assertEquals("BAT-A", flows.get(0).getBatchNo());
    assertEquals(LocalDate.of(2026, 12, 1), flows.get(0).getExpiryDate());
    assertEquals(-4, flows.get(0).getChangeQuantity());
    assertEquals(4, flows.get(0).getBeforeTotal());
    assertEquals(0, flows.get(0).getAfterTotal());
    assertEquals(4, flows.get(0).getBeforeAvailable());
    assertEquals(0, flows.get(0).getAfterAvailable());
    assertEquals("ADJUST:DOC-1:2:OUT:B:BAT-A", flows.get(0).getIdempotencyKey());
    assertEquals("OUTBOUND", flows.get(0).getBusinessType());
    assertEquals("DOC-1", flows.get(0).getBusinessKey());
    assertEquals("ADJUST", flows.get(0).getReferenceType());

    assertEquals("BAT-B", flows.get(1).getBatchNo());
    assertEquals(-2, flows.get(1).getChangeQuantity());
    assertEquals(6, flows.get(1).getBeforeTotal());
    assertEquals(4, flows.get(1).getAfterTotal());
    assertEquals("ADJUST:DOC-1:2:OUT:B:BAT-B", flows.get(1).getIdempotencyKey());

    ArgumentCaptor<InvCostLog> costCaptor = ArgumentCaptor.forClass(InvCostLog.class);
    verify(costLogMapper, times(2)).insertLog(costCaptor.capture());
    List<InvCostLog> costs = costCaptor.getAllValues();

    InvCostLog first = costs.get(0);
    assertEquals("OUT", first.getDirection());
    assertEquals(4, first.getQuantity().intValue());
    assertEquals(new BigDecimal("8.000000"), first.getUnitPrice());
    assertEquals(new BigDecimal("32.00"), first.getAmount());
    assertEquals(10, first.getBeforeBalanceQty().intValue());
    assertEquals(6, first.getAfterBalanceQty().intValue());
    assertEquals("BAT-A", first.getBatchNo());
    assertEquals(LocalDate.of(2026, 12, 1), first.getExpiryDate());
    assertEquals("OUTBOUND", first.getBusinessType());
    assertEquals("ADJUST:DOC-1:2:OUT:B:BAT-A", first.getIdempotencyKey());
    assertNull(first.getCounterpartKey());

    InvCostLog second = costs.get(1);
    assertEquals(2, second.getQuantity().intValue());
    assertEquals(new BigDecimal("16.00"), second.getAmount());
    assertEquals(6, second.getBeforeBalanceQty().intValue());
    assertEquals(4, second.getAfterBalanceQty().intValue());
    assertEquals("BAT-B", second.getBatchNo());
    assertEquals("ADJUST:DOC-1:2:OUT:B:BAT-B", second.getIdempotencyKey());
  }

  @Test
  @DisplayName("调拨转出跨批次：每条批次成本流水保持对手键 TRANSFER_IN:<单号>")
  void transferOutAcrossBatchesKeepsCounterpartKey() {
    Inventory from = ledger(INVENTORY_ID, 10, 10, new BigDecimal("12.5"));
    Inventory to = ledger(TO_ID, 0, 0, new BigDecimal("4"));
    Inventory freshFrom = ledger(INVENTORY_ID, 7, 7, new BigDecimal("12.5"));
    Inventory freshTo = ledger(TO_ID, 3, 3, new BigDecimal("10.267"));
    when(inventoryMapper.selectOne(any())).thenReturn(from, to);
    when(inventoryMapper.applyAdjustment(anyString(), anyInt(), anyInt(), anyString())).thenReturn(1);
    when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(freshFrom);
    when(inventoryMapper.selectById(TO_ID)).thenReturn(freshTo);
    when(costService.applyInbound(anyString(), anyInt(), any(), anyInt(), anyString()))
        .thenReturn(CostMovement.builder()
            .direction("IN").quantity(3)
            .unitPrice(new BigDecimal("12.500000"))
            .amount(new BigDecimal("37.50"))
            .beforeAverageCost(new BigDecimal("4.000000"))
            .afterAverageCost(new BigDecimal("10.267000"))
            .beforeBalanceQty(0).afterBalanceQty(3)
            .build());
    when(fefo.allocate(TENANT, SKU, FROM_WH, 3)).thenReturn(transferPlan());

    assertTrue(service.transferInventory(transferRequest()));

    ArgumentCaptor<FlowLogEntry> flowCaptor = ArgumentCaptor.forClass(FlowLogEntry.class);
    verify(flowLog, times(3)).record(flowCaptor.capture());
    List<FlowLogEntry> flows = flowCaptor.getAllValues();
    assertEquals("TRANSFER_OUT", flows.get(0).getBusinessType());
    assertEquals("BAT-A", flows.get(0).getBatchNo());
    assertEquals(-2, flows.get(0).getChangeQuantity());
    assertEquals("ADJUST:TR-1:7:OUT:B:BAT-A", flows.get(0).getIdempotencyKey());
    assertEquals("BAT-B", flows.get(1).getBatchNo());
    assertEquals(-1, flows.get(1).getChangeQuantity());
    assertEquals("ADJUST:TR-1:7:OUT:B:BAT-B", flows.get(1).getIdempotencyKey());
    assertEquals("TRANSFER_IN", flows.get(2).getBusinessType());
    assertNull(flows.get(2).getBatchNo());
    assertEquals("ADJUST:TR-1:7:IN", flows.get(2).getIdempotencyKey());

    ArgumentCaptor<InvCostLog> costCaptor = ArgumentCaptor.forClass(InvCostLog.class);
    verify(costLogMapper, times(3)).insertLog(costCaptor.capture());
    List<InvCostLog> costs = costCaptor.getAllValues();
    assertEquals("BAT-A", costs.get(0).getBatchNo());
    assertEquals("TRANSFER_IN:TR-1", costs.get(0).getCounterpartKey());
    assertEquals(10, costs.get(0).getBeforeBalanceQty().intValue());
    assertEquals(8, costs.get(0).getAfterBalanceQty().intValue());
    assertEquals("BAT-B", costs.get(1).getBatchNo());
    assertEquals("TRANSFER_IN:TR-1", costs.get(1).getCounterpartKey());
    assertEquals(8, costs.get(1).getBeforeBalanceQty().intValue());
    assertEquals(7, costs.get(1).getAfterBalanceQty().intValue());
    assertEquals("IN", costs.get(2).getDirection());
    assertEquals("TRANSFER_OUT:TR-1", costs.get(2).getCounterpartKey());
    assertNull(costs.get(2).getBatchNo());
  }

  @Test
  @DisplayName("FEFO 分配失败（合格批次不足）：异常上抛，不写流水、不写成本、不发事件")
  void allocationFailureWritesNothing() {
    Inventory existed = ledger(INVENTORY_ID, 10, 10, new BigDecimal("8"));
    Inventory fresh = ledger(INVENTORY_ID, 4, 4, new BigDecimal("8"));
    when(inventoryMapper.selectOne(any())).thenReturn(existed);
    when(inventoryMapper.applyAdjustment(anyString(), anyInt(), anyInt(), anyString())).thenReturn(1);
    when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(fresh);
    when(fefo.allocate(TENANT, SKU, FROM_WH, 6))
        .thenThrow(new ServiceException(400, org.springframework.http.HttpStatus.BAD_REQUEST,
            "合格批次可用量不足：需 6，可用 4"));

    assertThrows(ServiceException.class, () -> service.adjustInventory(adjustRequest(-6)));

    verify(flowLog, never()).record(any(FlowLogEntry.class));
    verify(costLogMapper, never()).insertLog(any(InvCostLog.class));
  }

  @Test
  @DisplayName("无批次记录回退台账口径：只写一条整批流水，批次维度为空")
  void fallbackWritesSingleRowWithoutBatchDimension() {
    Inventory existed = ledger(INVENTORY_ID, 10, 10, new BigDecimal("8"));
    Inventory fresh = ledger(INVENTORY_ID, 4, 4, new BigDecimal("8"));
    when(inventoryMapper.selectOne(any())).thenReturn(existed);
    when(inventoryMapper.applyAdjustment(anyString(), anyInt(), anyInt(), anyString())).thenReturn(1);
    when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(fresh);
    when(fefo.allocate(TENANT, SKU, FROM_WH, 6)).thenReturn(FefoAllocationPlan.notBatchManaged(6));

    service.adjustInventory(adjustRequest(-6));

    ArgumentCaptor<FlowLogEntry> flowCaptor = ArgumentCaptor.forClass(FlowLogEntry.class);
    verify(flowLog, times(1)).record(flowCaptor.capture());
    assertNull(flowCaptor.getValue().getBatchNo());
    assertEquals("ADJUST:DOC-1:2:OUT", flowCaptor.getValue().getIdempotencyKey());

    ArgumentCaptor<InvCostLog> costCaptor = ArgumentCaptor.forClass(InvCostLog.class);
    verify(costLogMapper, times(1)).insertLog(costCaptor.capture());
    assertNull(costCaptor.getValue().getBatchNo());
    assertEquals("ADJUST:DOC-1:2:OUT", costCaptor.getValue().getIdempotencyKey());
  }

  private static FefoAllocationPlan twoBatchPlan(int requested) {
    return new FefoAllocationPlan(true, false, List.of(), requested, requested, List.of(
        new FefoAllocation(BATCH_A_ID, "BAT-A", LocalDate.of(2026, 12, 1),
            4, 4, 0, 4, 0, 0, 0),
        new FefoAllocation(BATCH_B_ID, "BAT-B", LocalDate.of(2027, 1, 1),
            2, 6, 4, 6, 4, 0, 0)), "FEFO 分配可满足出库");
  }

  private static FefoAllocationPlan transferPlan() {
    return new FefoAllocationPlan(true, false, List.of(), 3, 3, List.of(
        new FefoAllocation(BATCH_A_ID, "BAT-A", LocalDate.of(2026, 12, 1),
            2, 3, 1, 3, 1, 0, 0),
        new FefoAllocation(BATCH_B_ID, "BAT-B", LocalDate.of(2027, 1, 1),
            1, 7, 6, 7, 6, 0, 0)), "FEFO 分配可满足出库");
  }

  private static Inventory ledger(String id, int total, int available, BigDecimal average) {
    Inventory inventory = new Inventory();
    inventory.setId(id);
    inventory.setTenantId(TENANT);
    inventory.setSkuId(SKU);
    inventory.setWarehouseId(id.equals(TO_ID) ? TO_WH : FROM_WH);
    inventory.setTotalStock(total);
    inventory.setAvailableStock(available);
    inventory.setLockedStock(0);
    inventory.setDamagedStock(0);
    inventory.setSafetyStock(0);
    inventory.setAverageCost(average);
    inventory.setVersion(1);
    inventory.setDeleted(false);
    return inventory;
  }

  private static InventoryAdjustRequest adjustRequest(int quantity) {
    InventoryAdjustRequest request = new InventoryAdjustRequest();
    request.setSkuId(SKU);
    request.setWarehouseId(FROM_WH);
    request.setQuantity(quantity);
    request.setAdjustType(2);
    request.setReferenceNo("DOC-1");
    request.setOperatorId(OPERATOR);
    request.setOperatorName("张三");
    request.setRemark("批次出库");
    return request;
  }

  private static InventoryTransferRequest transferRequest() {
    InventoryTransferRequest request = new InventoryTransferRequest();
    request.setSkuId(SKU);
    request.setFromWarehouseId(FROM_WH);
    request.setToWarehouseId(TO_WH);
    request.setQuantity(3);
    request.setTransferNo("TR-1");
    request.setOperatorId(OPERATOR);
    return request;
  }
}
