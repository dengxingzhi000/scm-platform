package com.cognora.platform.inventory.service.flow;

import com.cognora.platform.common.integration.outbox.OutboxService;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.ReservationStatus;
import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import com.cognora.platform.inventory.domain.entity.InvTccReservation;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.dto.InventoryAdjustRequest;
import com.cognora.platform.inventory.dto.InventoryReservationRequest;
import com.cognora.platform.inventory.dto.InventoryTransferRequest;
import com.cognora.platform.inventory.lock.DistributedLock;
import com.cognora.platform.inventory.mapper.InvInventoryMapper;
import com.cognora.platform.inventory.mapper.InvReservationRecordMapper;
import com.cognora.platform.inventory.mapper.InvTccReservationMapper;
import com.cognora.platform.inventory.service.RedisInventoryService;
import com.cognora.platform.inventory.service.batch.FefoAllocationPlan;
import com.cognora.platform.inventory.service.batch.FefoAllocationService;
import com.cognora.platform.inventory.service.command.InvInventoryCommandService;
import com.cognora.platform.inventory.service.cost.InventoryCostLogService;
import com.cognora.platform.inventory.service.cost.MovingAverageCostService;
import com.cognora.platform.inventory.service.dubbo.InventoryDubboServiceImpl;
import com.cognora.platform.inventory.service.impl.InvReservationServiceImpl;
import com.cognora.platform.inventory.service.impl.InventoryTccServiceImpl;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数量变更链路写流水用例：调整/调拨、预占四态、TCC 三态、Dubbo 扣减。
 *
 * <p>这些用例验证调用方传给 {@link InventoryFlowLogService} 的内容（前后快照、
 * 业务类型、操作人、业务单号、幂等键）以及“业务失败不写流水 / 重复请求不重复写”。
 * 真实事务回滚由 Spring 事务保证，PR 中说明本地无真实 PG 事务级验证。
 */
class InventoryFlowLogWiringTest {

  private static final String TENANT = "019a0aee-0000-7000-8000-000000000002";
  private static final String INVENTORY_ID = "11111111-1111-1111-1111-111111111111";
  private static final String FROM_ID = "66666666-6666-6666-6666-666666666666";
  private static final String TO_ID = "77777777-7777-7777-7777-777777777777";
  private static final String SKU_ID = "22222222-2222-2222-2222-222222222222";
  private static final String WH_ID = "33333333-3333-3333-3333-333333333333";
  private static final String OPERATOR_ID = "44444444-4444-4444-4444-444444444444";

  private InventoryFlowLogService flowLog;
  private FefoAllocationService fefo;

  @BeforeEach
  void setUp() {
    TenantContextHolder.setTenantId(UUID.fromString(TENANT));
    flowLog = mock(InventoryFlowLogService.class);
    fefo = mock(FefoAllocationService.class);
    when(fefo.allocate(anyString(), anyString(), anyString(), anyInt()))
        .thenReturn(FefoAllocationPlan.notBatchManaged(0));
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  @DisplayName("调整成功写一条：前置快照由变更后快照反推，业务失败（条件更新 0）不写流水")
  void adjustWritesFlowAndBusinessFailureWritesNothing() {
    InvInventoryMapper mapper = mock(InvInventoryMapper.class);
    OutboxService outbox = mock(OutboxService.class);
    InvInventoryCommandService service =
        new InvInventoryCommandService(mapper, outbox, flowLog,
            mock(MovingAverageCostService.class), mock(InventoryCostLogService.class), fefo);

    Inventory existed = ledger(10, 10, 0, 0);
    Inventory fresh = ledger(15, 15, 0, 0);
    when(mapper.selectOne(any())).thenReturn(existed);
    when(mapper.applyAdjustment(anyString(), anyInt(), anyInt(), anyString())).thenReturn(1);
    when(mapper.selectById(INVENTORY_ID)).thenReturn(fresh);

    service.adjustInventory(adjustRequest(5, 1));

    ArgumentCaptor<FlowLogEntry> captor = ArgumentCaptor.forClass(FlowLogEntry.class);
    verify(flowLog).record(captor.capture());
    FlowLogEntry entry = captor.getValue();
    assertEquals("INBOUND", entry.getBusinessType());
    assertEquals(1, entry.getOperationType());
    assertEquals(5, entry.getChangeQuantity());
    assertEquals(10, entry.getBeforeAvailable());
    assertEquals(15, entry.getAfterAvailable());
    assertEquals(10, entry.getBeforeTotal());
    assertEquals(15, entry.getAfterTotal());
    assertEquals("DOC-1", entry.getBusinessKey());
    assertEquals(OPERATOR_ID, entry.getOperatorId());
    assertEquals("ADJUST:DOC-1:1:IN", entry.getIdempotencyKey());

    // 业务失败：条件更新返回 0，抛异常且完全不写流水
    when(mapper.applyAdjustment(anyString(), anyInt(), anyInt(), anyString())).thenReturn(0);
    flowLog = mock(InventoryFlowLogService.class);
    InvInventoryCommandService failing =
        new InvInventoryCommandService(mapper, outbox, flowLog,
            mock(MovingAverageCostService.class), mock(InventoryCostLogService.class), fefo);
    assertThrows(IllegalArgumentException.class, () -> failing.adjustInventory(adjustRequest(-20, 2)));
    verify(flowLog, never()).record(any(FlowLogEntry.class));
  }

  @Test
  @DisplayName("调拨两侧各写一条：转出为负向、转入为正向，业务单号相同但幂等键按方向区分")
  void transferWritesTwoDirectionalEntries() {
    InvInventoryMapper mapper = mock(InvInventoryMapper.class);
    InvInventoryCommandService service =
        new InvInventoryCommandService(mapper, mock(OutboxService.class), flowLog,
            mock(MovingAverageCostService.class), mock(InventoryCostLogService.class), fefo);

    Inventory from = ledger(10, 10, 0, 0);
    Inventory to = ledger(0, 2, 0, 0);
    Inventory freshFrom = ledger(7, 7, 0, 0);
    Inventory freshTo = ledger(0, 5, 0, 0);
    from.setId(FROM_ID);
    to.setId(TO_ID);
    freshFrom.setId(FROM_ID);
    freshTo.setId(TO_ID);
    when(mapper.selectOne(any())).thenReturn(from, to);
    when(mapper.applyAdjustment(anyString(), anyInt(), anyInt(), anyString())).thenReturn(1);
    when(mapper.selectById(FROM_ID)).thenReturn(freshFrom);
    when(mapper.selectById(TO_ID)).thenReturn(freshTo);

    InventoryTransferRequest request = new InventoryTransferRequest();
    request.setSkuId(SKU_ID);
    request.setFromWarehouseId(WH_ID);
    request.setToWarehouseId("55555555-5555-5555-5555-555555555555");
    request.setQuantity(3);
    request.setTransferNo("TR-1");
    request.setOperatorId(OPERATOR_ID);
    request.setRemark("常规调拨");

    service.transferInventory(request);

    ArgumentCaptor<FlowLogEntry> captor = ArgumentCaptor.forClass(FlowLogEntry.class);
    verify(flowLog, times(2)).record(captor.capture());
    List<FlowLogEntry> entries = captor.getAllValues();

    assertEquals("TRANSFER_OUT", entries.get(0).getBusinessType());
    assertEquals(-3, entries.get(0).getChangeQuantity());
    assertEquals(10, entries.get(0).getBeforeAvailable());
    assertEquals(7, entries.get(0).getAfterAvailable());
    assertEquals("ADJUST:TR-1:7:OUT", entries.get(0).getIdempotencyKey());

    assertEquals("TRANSFER_IN", entries.get(1).getBusinessType());
    assertEquals(3, entries.get(1).getChangeQuantity());
    assertEquals(2, entries.get(1).getBeforeAvailable());
    assertEquals(5, entries.get(1).getAfterAvailable());
    assertEquals("ADJUST:TR-1:7:IN", entries.get(1).getIdempotencyKey());
  }

  @Test
  @DisplayName("预占写 RESERVE：可用减、锁定加，total 不变")
  void reserveWritesFlow() {
    InvInventoryMapper inventoryMapper = mock(InvInventoryMapper.class);
    InvReservationRecordMapper recordMapper = mock(InvReservationRecordMapper.class);
    DistributedLock lock = mock(DistributedLock.class);
    InvReservationServiceImpl service =
        new InvReservationServiceImpl(inventoryMapper, recordMapper, lock, flowLog,
            mock(InventoryCostLogService.class));

    Inventory inventory = ledger(10, 10, 0, 0);
    when(lock.tryLock(anyString(), anyLong(), any(TimeUnit.class)))
        .thenReturn(mock(DistributedLock.LockHandle.class));
    when(recordMapper.selectByBusinessKey(anyString(), anyString())).thenReturn(null);
    when(inventoryMapper.selectOne(any())).thenReturn(inventory);
    when(inventoryMapper.lockStock(INVENTORY_ID, 3)).thenReturn(1);
    when(recordMapper.insert(any(InvReservationRecord.class))).thenReturn(1);

    InventoryReservationRequest request = new InventoryReservationRequest();
    request.setSkuId(SKU_ID);
    request.setWarehouseId(WH_ID);
    request.setQuantity(3);
    request.setBusinessKey("ORD-1");

    assertTrue(service.reserveInventory(request));

    FlowLogEntry entry = capturedSingle();
    assertEquals("RESERVE", entry.getBusinessType());
    assertEquals(3, entry.getOperationType());
    assertEquals(-3, entry.getChangeQuantity());
    assertEquals(10, entry.getBeforeAvailable());
    assertEquals(7, entry.getAfterAvailable());
    assertEquals(0, entry.getBeforeLocked());
    assertEquals(3, entry.getAfterLocked());
    assertEquals(10, entry.getBeforeTotal());
    assertEquals(10, entry.getAfterTotal());
    assertEquals("RESERVE:ORD-1", entry.getIdempotencyKey());
    assertEquals(TENANT, entry.getTenantId());
  }

  @Test
  @DisplayName("确认写 CONFIRM：total 与 locked 同时扣减，available 不变")
  void confirmWritesFlow() {
    InvInventoryMapper inventoryMapper = mock(InvInventoryMapper.class);
    InvReservationRecordMapper recordMapper = mock(InvReservationRecordMapper.class);
    InvReservationServiceImpl service = new InvReservationServiceImpl(inventoryMapper, recordMapper,
        mock(DistributedLock.class), flowLog, mock(InventoryCostLogService.class));

    InvReservationRecord record = reservationRecord(ReservationStatus.RESERVED);
    when(recordMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(record);
    when(recordMapper.markConfirmed(record.getId(), TENANT)).thenReturn(1);
    when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(ledger(10, 7, 3, 0));
    when(inventoryMapper.confirmStock(INVENTORY_ID, 3)).thenReturn(1);

    assertTrue(service.confirmReservation("ORD-1"));

    FlowLogEntry entry = capturedSingle();
    assertEquals("CONFIRM", entry.getBusinessType());
    assertEquals(5, entry.getOperationType());
    assertEquals(10, entry.getBeforeTotal());
    assertEquals(7, entry.getAfterTotal());
    assertEquals(7, entry.getBeforeAvailable());
    assertEquals(7, entry.getAfterAvailable());
    assertEquals(3, entry.getBeforeLocked());
    assertEquals(0, entry.getAfterLocked());
    assertEquals("CONFIRM:ORD-1", entry.getIdempotencyKey());
  }

  @Test
  @DisplayName("释放写 RELEASE：可用加、锁定减；重复释放不重复写")
  void releaseWritesFlowOnce() {
    InvInventoryMapper inventoryMapper = mock(InvInventoryMapper.class);
    InvReservationRecordMapper recordMapper = mock(InvReservationRecordMapper.class);
    InvReservationServiceImpl service = new InvReservationServiceImpl(inventoryMapper, recordMapper,
        mock(DistributedLock.class), flowLog, mock(InventoryCostLogService.class));

    InvReservationRecord reserved = reservationRecord(ReservationStatus.RESERVED);
    InvReservationRecord released = reservationRecord(ReservationStatus.RELEASED);
    when(recordMapper.selectByBusinessKey(TENANT, "ORD-1")).thenReturn(reserved, released);
    when(recordMapper.markReleased(reserved.getId(), TENANT)).thenReturn(1);
    when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(ledger(10, 7, 3, 0));
    when(inventoryMapper.releaseStock(INVENTORY_ID, 3)).thenReturn(1);

    assertTrue(service.releaseReservation("ORD-1"));
    assertTrue(service.releaseReservation("ORD-1"));

    FlowLogEntry entry = capturedSingle();
    assertEquals("RELEASE", entry.getBusinessType());
    assertEquals(4, entry.getOperationType());
    assertEquals(3, entry.getChangeQuantity());
    assertEquals(7, entry.getBeforeAvailable());
    assertEquals(10, entry.getAfterAvailable());
    assertEquals(3, entry.getBeforeLocked());
    assertEquals(0, entry.getAfterLocked());
    assertEquals("RELEASE:ORD-1", entry.getIdempotencyKey());
  }

  @Test
  @DisplayName("过期释放写 RELEASE_EXPIRED，幂等键按预占记录 ID")
  void expiredReleaseWritesFlow() {
    InvInventoryMapper inventoryMapper = mock(InvInventoryMapper.class);
    InvReservationRecordMapper recordMapper = mock(InvReservationRecordMapper.class);
    InvReservationServiceImpl service = new InvReservationServiceImpl(inventoryMapper, recordMapper,
        mock(DistributedLock.class), flowLog, mock(InventoryCostLogService.class));

    InvReservationRecord record = reservationRecord(ReservationStatus.RESERVED);
    when(recordMapper.selectExpired(200)).thenReturn(List.of(record));
    when(recordMapper.markExpired(record.getId(), TENANT)).thenReturn(1);
    when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(ledger(10, 7, 3, 0));
    when(inventoryMapper.releaseStock(INVENTORY_ID, 3)).thenReturn(1);

    assertEquals(1, service.releaseExpiredReservations());

    FlowLogEntry entry = capturedSingle();
    assertEquals("RELEASE_EXPIRED", entry.getBusinessType());
    assertEquals(4, entry.getOperationType());
    assertEquals(3, entry.getChangeQuantity());
    assertEquals("RELEASE_EXPIRED:" + record.getId(), entry.getIdempotencyKey());
  }

  @Test
  @DisplayName("TCC Try 写 TCC_TRY，Confirm 写 TCC_CONFIRM；重复 Confirm 只写一条")
  void tccTryConfirmWriteFlowOnce() {
    InvInventoryMapper inventoryMapper = mock(InvInventoryMapper.class);
    InvTccReservationMapper reservationMapper = mock(InvTccReservationMapper.class);
    InventoryTccServiceImpl service =
        new InventoryTccServiceImpl(inventoryMapper, reservationMapper, flowLog,
            mock(InventoryCostLogService.class));

    Inventory inventory = ledger(10, 10, 0, 0);
    when(reservationMapper.selectByBusinessKey(TENANT, "TCC-1")).thenReturn(null);
    when(inventoryMapper.selectBySkuUuidAndWarehouse(TENANT, SKU_ID, WH_ID)).thenReturn(inventory);
    when(inventoryMapper.lockStock(INVENTORY_ID, 3)).thenReturn(1);
    when(reservationMapper.insert(any(InvTccReservation.class))).thenReturn(1);

    assertTrue(service.reserveInventoryBySku(SKU_ID, WH_ID, 3, "TCC-1"));
    FlowLogEntry tryEntry = capturedSingle();
    assertEquals("TCC_TRY", tryEntry.getBusinessType());
    assertEquals(10, tryEntry.getBeforeAvailable());
    assertEquals(7, tryEntry.getAfterAvailable());
    assertEquals("TCC_TRY:TCC-1", tryEntry.getIdempotencyKey());

    InvTccReservation trying = tccReservation("TRYING");
    InvTccReservation confirmed = tccReservation("CONFIRMED");
    when(reservationMapper.selectByBusinessKey(TENANT, "TCC-1")).thenReturn(trying, confirmed);
    when(reservationMapper.markConfirmed(1L, TENANT)).thenReturn(1);
    when(inventoryMapper.selectById(INVENTORY_ID)).thenReturn(ledger(10, 7, 3, 0));
    when(inventoryMapper.confirmStock(INVENTORY_ID, 3)).thenReturn(1);

    assertTrue(service.confirmReserve(context("TCC-1")));
    assertTrue(service.confirmReserve(context("TCC-1")));

    verify(flowLog, times(2)).record(any(FlowLogEntry.class));
    ArgumentCaptor<FlowLogEntry> captor = ArgumentCaptor.forClass(FlowLogEntry.class);
    verify(flowLog, times(2)).record(captor.capture());
    FlowLogEntry confirmEntry = captor.getAllValues().get(1);
    assertEquals("TCC_CONFIRM", confirmEntry.getBusinessType());
    assertEquals(10, confirmEntry.getBeforeTotal());
    assertEquals(7, confirmEntry.getAfterTotal());
    assertEquals("TCC_CONFIRM:TCC-1", confirmEntry.getIdempotencyKey());
  }

  @Test
  @DisplayName("Dubbo 同一 requestId 重复扣减只写一条流水，来源标记 REDIS")
  void dubboDeductWritesOneFlowPerRequestId() {
    RedisInventoryService redis = mock(RedisInventoryService.class);
    InventoryDubboServiceImpl service = new InventoryDubboServiceImpl(redis,
        mock(InvInventoryMapper.class), flowLog, mock(InventoryCostLogService.class));

    when(redis.acquireDeductRequest("REQ-1")).thenReturn(true, false);
    when(redis.deductStock("7", "DEFAULT", 5)).thenReturn(true);
    when(redis.getStock("7", "DEFAULT")).thenReturn(5);

    service.deductStock(7L, 5, "REQ-1");
    service.deductStock(7L, 5, "REQ-1");

    FlowLogEntry entry = capturedSingle();
    assertEquals("DUBBO_DEDUCT", entry.getBusinessType());
    assertEquals(2, entry.getOperationType());
    assertEquals(-5, entry.getChangeQuantity());
    assertEquals(10, entry.getBeforeAvailable());
    assertEquals(5, entry.getAfterAvailable());
    assertEquals("REQ-1", entry.getRequestId());
    assertEquals("REQ-1", entry.getBusinessKey());
    assertEquals(7L, entry.getLegacySkuId());
    assertEquals("REDIS", entry.getLedgerSource());
    assertEquals("DUBBO_DEDUCT:REQ-1:7", entry.getIdempotencyKey());
  }

  private FlowLogEntry capturedSingle() {
    ArgumentCaptor<FlowLogEntry> captor = ArgumentCaptor.forClass(FlowLogEntry.class);
    verify(flowLog).record(captor.capture());
    return captor.getValue();
  }

  private static Inventory ledger(int total, int available, int locked, int damaged) {
    Inventory inventory = new Inventory();
    inventory.setId(INVENTORY_ID);
    inventory.setTenantId(TENANT);
    inventory.setSkuId(SKU_ID);
    inventory.setWarehouseId(WH_ID);
    inventory.setTotalStock(total);
    inventory.setAvailableStock(available);
    inventory.setLockedStock(locked);
    inventory.setDamagedStock(damaged);
    inventory.setSafetyStock(0);
    inventory.setDeleted(false);
    return inventory;
  }

  private static InventoryAdjustRequest adjustRequest(int quantity, int adjustType) {
    InventoryAdjustRequest request = new InventoryAdjustRequest();
    request.setSkuId(SKU_ID);
    request.setWarehouseId(WH_ID);
    request.setQuantity(quantity);
    request.setAdjustType(adjustType);
    request.setReferenceNo("DOC-1");
    request.setOperatorId(OPERATOR_ID);
    request.setOperatorName("张三");
    request.setRemark("测试调整");
    return request;
  }

  private static InvReservationRecord reservationRecord(ReservationStatus status) {
    InvReservationRecord record = new InvReservationRecord();
    record.setId("22222222-2222-2222-2222-222222222222");
    record.setTenantId(TENANT);
    record.setBusinessKey("ORD-1");
    record.setSkuId(SKU_ID);
    record.setWarehouseId(WH_ID);
    record.setInventoryId(INVENTORY_ID);
    record.setQuantity(3);
    record.setStatus(status.name());
    record.setExpireAt(LocalDateTime.now().plusMinutes(10));
    return record;
  }

  private static InvTccReservation tccReservation(String status) {
    InvTccReservation reservation = new InvTccReservation();
    reservation.setId(1L);
    reservation.setTenantId(TENANT);
    reservation.setBusinessKey("TCC-1");
    reservation.setSkuId(7L);
    reservation.setSkuUuid(SKU_ID);
    reservation.setInventoryId(INVENTORY_ID);
    reservation.setWarehouseId(WH_ID);
    reservation.setQuantity(3);
    reservation.setStatus(status);
    return reservation;
  }

  private static BusinessActionContext context(String businessKey) {
    BusinessActionContext context = new BusinessActionContext();
    context.setXid("xid-1");
    Map<String, Object> actionContext = new HashMap<>();
    actionContext.put("businessKey", businessKey);
    context.setActionContext(actionContext);
    return context;
  }
}
