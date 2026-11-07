package com.cognora.platform.inventory.service.cost;

import com.cognora.platform.common.integration.outbox.OutboxService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.InvCostLog;
import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InventoryAdjustRequest;
import com.cognora.platform.inventory.dto.InventoryTransferRequest;
import com.cognora.platform.inventory.lock.DistributedLock;
import com.cognora.platform.inventory.mapper.InvCostLogMapper;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.mapper.InvReservationRecordMapper;
import com.cognora.platform.inventory.service.RedisInventoryService;
import com.cognora.platform.inventory.service.batch.FefoAllocationPlan;
import com.cognora.platform.inventory.service.batch.FefoAllocationService;
import com.cognora.platform.inventory.service.command.InvInventoryCommandService;
import com.cognora.platform.inventory.service.dubbo.InventoryDubboServiceImpl;
import com.cognora.platform.inventory.service.flow.InventoryFlowLogService;
import com.cognora.platform.inventory.service.impl.InvReservationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * 成本流水与各业务链路的接线用例：调整、调拨、预占确认。
 */
class InventoryCostWiringTest {

  private static final String TENANT = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final String SKU = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String FROM_WH = "cccccccc-cccc-cccc-cccc-cccccccccccc";
  private static final String TO_WH = "dddddddd-dddd-dddd-dddd-dddddddddddd";
  private static final String FROM_ID = "11111111-1111-1111-1111-111111111111";
  private static final String TO_ID = "22222222-2222-2222-2222-222222222222";
  private static final String OPERATOR = "33333333-3333-3333-3333-333333333333";
  private static final String RECORD_ID = "44444444-4444-4444-4444-444444444444";

  private InvCostLogMapper costLogMapper;
  private FefoAllocationService fefo;

  @BeforeEach
  void setUp() {
    TenantContextHolder.setTenantId(UUID.fromString(TENANT));
    costLogMapper = mock(InvCostLogMapper.class);
    when(costLogMapper.insertLog(any(InvCostLog.class))).thenReturn(1);
    fefo = mock(FefoAllocationService.class);
    when(fefo.allocate(anyString(), anyString(), anyString(), anyInt()))
        .thenReturn(FefoAllocationPlan.notBatchManaged(0));
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  @DisplayName("出库业务失败（条件更新 0 行）不写数量流水也不写成本流水")
  void failedOutboundWritesNoFlowOrCostLog() {
    InvInventoryMapper mapper = mock(InvInventoryMapper.class);
    InventoryFlowLogService flowLog = mock(InventoryFlowLogService.class);
    when(mapper.selectOne(any())).thenReturn(inventory(FROM_ID, 3, 3, new BigDecimal("9")));
    when(mapper.applyAdjustment(anyString(), anyInt(), anyInt(), anyString())).thenReturn(0);

    InvInventoryCommandService service = new InvInventoryCommandService(mapper,
        mock(OutboxService.class), flowLog, mock(MovingAverageCostService.class),
        new InventoryCostLogService(costLogMapper), fefo);

    InventoryAdjustRequest request = adjustRequest(-5);
    assertThrows(IllegalArgumentException.class, () -> service.adjustInventory(request));

    verify(flowLog, never()).record(any());
    verify(costLogMapper, never()).insertLog(any(InvCostLog.class));
  }

  @Test
  @DisplayName("调拨：转出按转出仓均价出库，转入以该单价入库，两侧成本流水互标对侧")
  void transferWritesBothSidesAtOutboundAverage() {
    InvInventoryMapper mapper = mock(InvInventoryMapper.class);
    InventoryFlowLogService flowLog = mock(InventoryFlowLogService.class);
    MovingAverageCostService costService = mock(MovingAverageCostService.class);

    Inventory from = inventory(FROM_ID, 10, 10, new BigDecimal("12.5"));
    Inventory to = inventory(TO_ID, 10, 10, new BigDecimal("4"));
    Inventory freshFrom = inventory(FROM_ID, 7, 7, new BigDecimal("12.5"));
    Inventory freshTo = inventory(TO_ID, 13, 13, new BigDecimal("5.961538"));
    when(mapper.selectOne(any())).thenReturn(from, to);
    when(mapper.applyAdjustment(anyString(), anyInt(), anyInt(), anyString())).thenReturn(1);
    when(mapper.selectById(FROM_ID)).thenReturn(freshFrom);
    when(mapper.selectById(TO_ID)).thenReturn(freshTo);
    when(costService.applyInbound(anyString(), anyInt(), any(), anyInt(), anyString()))
        .thenReturn(CostMovement.builder()
            .direction("IN").quantity(3)
            .unitPrice(new BigDecimal("12.500000"))
            .amount(new BigDecimal("37.50"))
            .beforeAverageCost(new BigDecimal("4.000000"))
            .afterAverageCost(new BigDecimal("5.961538"))
            .beforeBalanceQty(10).afterBalanceQty(13)
            .build());

    InvInventoryCommandService service = new InvInventoryCommandService(mapper,
        mock(OutboxService.class), flowLog, costService,
        new InventoryCostLogService(costLogMapper), fefo);

    assertTrue(service.transferInventory(transferRequest()));

    ArgumentCaptor<InvCostLog> captor = ArgumentCaptor.forClass(InvCostLog.class);
    verify(costLogMapper, times(2)).insertLog(captor.capture());
    List<InvCostLog> logs = captor.getAllValues();

    InvCostLog out = logs.get(0);
    assertEquals("OUT", out.getDirection());
    assertEquals(3, out.getQuantity().intValue());
    assertEquals(new BigDecimal("12.500000"), out.getUnitPrice());
    assertEquals(new BigDecimal("37.50"), out.getAmount());
    assertEquals(new BigDecimal("12.500000"), out.getBeforeAverageCost());
    assertEquals(new BigDecimal("12.500000"), out.getAfterAverageCost());
    assertEquals(10, out.getBeforeBalanceQty().intValue());
    assertEquals(7, out.getAfterBalanceQty().intValue());
    assertEquals("TRANSFER_OUT", out.getBusinessType());
    assertEquals("TRANSFER_IN:TR-1", out.getCounterpartKey());
    assertEquals("ADJUST:TR-1:7:OUT", out.getIdempotencyKey());

    InvCostLog in = logs.get(1);
    assertEquals("IN", in.getDirection());
    assertEquals(3, in.getQuantity().intValue());
    assertEquals(new BigDecimal("12.500000"), in.getUnitPrice());
    assertEquals(new BigDecimal("37.50"), in.getAmount());
    assertEquals(new BigDecimal("5.961538"), in.getAfterAverageCost());
    assertEquals("TRANSFER_IN", in.getBusinessType());
    assertEquals("TRANSFER_OUT:TR-1", in.getCounterpartKey());
    assertEquals("ADJUST:TR-1:7:IN", in.getIdempotencyKey());
  }

  @Test
  @DisplayName("预占确认按当时均价出库，成本流水与数量流水同业务键")
  void reservationConfirmWritesCostAtCurrentAverage() {
    InvInventoryMapper inventoryMapper = mock(InvInventoryMapper.class);
    InvReservationRecordMapper recordMapper = mock(InvReservationRecordMapper.class);
    InventoryFlowLogService flowLog = mock(InventoryFlowLogService.class);

    InvReservationRecord record = reservationRecord();
    Inventory before = inventory(RECORD_ID, 10, 7, new BigDecimal("7.25"));
    before.setId(FROM_ID);
    before.setLockedStock(3);
    when(recordMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(record);
    when(recordMapper.markConfirmed(record.getId(), TENANT)).thenReturn(1);
    when(inventoryMapper.selectById(FROM_ID)).thenReturn(before);
    when(inventoryMapper.confirmStock(FROM_ID, 3)).thenReturn(1);

    InvReservationServiceImpl service = new InvReservationServiceImpl(inventoryMapper,
        recordMapper, mock(DistributedLock.class), flowLog,
        new InventoryCostLogService(costLogMapper));

    assertTrue(service.confirmReservation("ORD-1"));

    ArgumentCaptor<InvCostLog> captor = ArgumentCaptor.forClass(InvCostLog.class);
    verify(costLogMapper).insertLog(captor.capture());
    InvCostLog saved = captor.getValue();
    assertEquals("OUT", saved.getDirection());
    assertEquals(3, saved.getQuantity().intValue());
    assertEquals(new BigDecimal("7.250000"), saved.getUnitPrice());
    assertEquals(new BigDecimal("21.75"), saved.getAmount());
    assertEquals(new BigDecimal("7.250000"), saved.getBeforeAverageCost());
    assertEquals(new BigDecimal("7.250000"), saved.getAfterAverageCost());
    assertEquals(10, saved.getBeforeBalanceQty().intValue());
    assertEquals(7, saved.getAfterBalanceQty().intValue());
    assertEquals("CONFIRM", saved.getBusinessType());
    assertEquals("CONFIRM:ORD-1", saved.getIdempotencyKey());
  }

  @Test
  @DisplayName("Dubbo 扣减按台账当时均价计价并标记 REDIS 账本来源")
  void dubboDeductWritesCostAtLedgerAverage() {
    RedisInventoryService redis = mock(RedisInventoryService.class);
    when(redis.acquireDeductRequest("REQ-9")).thenReturn(true);
    when(redis.deductStock(anyString(), anyString(), anyInt())).thenReturn(true);

    InvInventoryMapper mapper = mock(InvInventoryMapper.class);
    Inventory ledger = inventory(FROM_ID, 20, 18, new BigDecimal("3.5"));
    when(mapper.selectByLegacySkuId(TENANT, 1001L)).thenReturn(List.of(ledger));

    InventoryDubboServiceImpl service = new InventoryDubboServiceImpl(redis, mapper,
        mock(InventoryFlowLogService.class), new InventoryCostLogService(costLogMapper));

    service.deductStock(1001L, 4, "REQ-9");

    ArgumentCaptor<InvCostLog> captor = ArgumentCaptor.forClass(InvCostLog.class);
    verify(costLogMapper).insertLog(captor.capture());
    InvCostLog saved = captor.getValue();
    assertEquals("OUT", saved.getDirection());
    assertEquals(4, saved.getQuantity().intValue());
    assertEquals(new BigDecimal("3.500000"), saved.getUnitPrice());
    assertEquals(new BigDecimal("14.00"), saved.getAmount());
    assertEquals("REDIS", saved.getLedgerSource());
    assertEquals(1001L, saved.getLegacySkuId().longValue());
    assertEquals("DEFAULT", saved.getWarehouseCode());
    assertEquals("DUBBO_DEDUCT:REQ-9:1001", saved.getIdempotencyKey());
    assertEquals("DUBBO_DEDUCT", saved.getBusinessType());
  }

  @Test
  @DisplayName("成本流水与库存更新都声明 REQUIRED 事务，业务失败可整体回滚")
  void costLogAndCostUpdateShareRequiredTransaction() throws Exception {
    com.cognora.platform.inventory.service.cost.InventoryCostLogService service =
        new InventoryCostLogService(costLogMapper);

    var recordMethod = service.getClass().getMethod("record", Inventory.class,
        CostMovement.class, CostContext.class, String.class);
    var recordTx = recordMethod.getAnnotation(
        org.springframework.transaction.annotation.Transactional.class);
    assertEquals(org.springframework.transaction.annotation.Propagation.REQUIRED,
        recordTx.propagation());

    var inboundMethod = MovingAverageCostService.class.getMethod("applyInbound", String.class,
        int.class, BigDecimal.class, int.class, String.class);
    var inboundTx = inboundMethod.getAnnotation(
        org.springframework.transaction.annotation.Transactional.class);
    assertEquals(org.springframework.transaction.annotation.Propagation.REQUIRED,
        inboundTx.propagation());
  }

  private static Inventory inventory(String id, int total, int available, BigDecimal average) {
    Inventory inventory = new Inventory();
    inventory.setId(id);
    inventory.setSkuId(SKU);
    inventory.setWarehouseId(FROM_WH);
    inventory.setTotalStock(total);
    inventory.setAvailableStock(available);
    inventory.setLockedStock(0);
    inventory.setDamagedStock(0);
    inventory.setSafetyStock(10);
    inventory.setAverageCost(average);
    inventory.setVersion(1);
    return inventory;
  }

  private static InventoryAdjustRequest adjustRequest(int quantity) {
    InventoryAdjustRequest request = new InventoryAdjustRequest();
    request.setSkuId(SKU);
    request.setWarehouseId(FROM_WH);
    request.setQuantity(quantity);
    request.setAdjustType(2);
    request.setReferenceNo("SO-1");
    request.setOperatorId(OPERATOR);
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

  private static InvReservationRecord reservationRecord() {
    InvReservationRecord record = new InvReservationRecord();
    record.setId(RECORD_ID);
    record.setTenantId(TENANT);
    record.setBusinessKey("ORD-1");
    record.setSkuId(SKU);
    record.setWarehouseId(FROM_WH);
    record.setInventoryId(FROM_ID);
    record.setQuantity(3);
    record.setStatus("RESERVED");
    return record;
  }
}
