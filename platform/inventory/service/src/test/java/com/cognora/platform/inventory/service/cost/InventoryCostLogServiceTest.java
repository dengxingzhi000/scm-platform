package com.cognora.platform.inventory.service.cost;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.InvCostLog;
import com.cognora.platform.inventory.domain.entity.Inventory;
import com.cognora.platform.inventory.mapper.InvCostLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 成本流水写入与幂等语义。
 */
class InventoryCostLogServiceTest {

  private static final String TENANT = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final String SKU = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String WAREHOUSE = "cccccccc-cccc-cccc-cccc-cccccccccccc";
  private static final String INVENTORY_ID = "dddddddd-dddd-dddd-dddd-dddddddddddd";

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  @DisplayName("成本流水字段完整：方向、单价/金额精度、前后均价与结存")
  void recordWritesCompleteCostFields() {
    InvCostLogMapper mapper = mock(InvCostLogMapper.class);
    when(mapper.insertLog(any(InvCostLog.class))).thenReturn(1);
    InventoryCostLogService service = new InventoryCostLogService(mapper);

    Inventory inventory = inventory();
    CostMovement movement = CostMovement.builder()
        .direction("IN")
        .quantity(10)
        .unitPrice(new BigDecimal("12.3456789"))
        .amount(new BigDecimal("123.456789"))
        .beforeAverageCost(new BigDecimal("10.000000"))
        .afterAverageCost(new BigDecimal("11.615000"))
        .beforeBalanceQty(3)
        .afterBalanceQty(13)
        .build();
    CostContext context = CostContext.builder()
        .businessType("TRANSFER_IN")
        .businessKey("TR-1")
        .referenceType("TRANSFER")
        .referenceId(INVENTORY_ID)
        .referenceNo("TR-1")
        .counterpartKey("TRANSFER_OUT:TR-1")
        .operatorId("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee")
        .operatorName("alice")
        .idempotencyKey("ADJUST:TR-1:7:IN")
        .remark("调拨转入")
        .build();

    assertTrue(service.record(inventory, movement, context, TENANT));

    ArgumentCaptor<InvCostLog> captor = ArgumentCaptor.forClass(InvCostLog.class);
    verify(mapper).insertLog(captor.capture());
    InvCostLog saved = captor.getValue();

    assertEquals(TENANT, saved.getTenantId());
    assertEquals(INVENTORY_ID, saved.getInventoryId());
    assertEquals(SKU, saved.getSkuId());
    assertEquals(WAREHOUSE, saved.getWarehouseId());
    assertEquals("IN", saved.getDirection());
    assertEquals(10, saved.getQuantity());
    assertEquals(new BigDecimal("12.345679"), saved.getUnitPrice(), "单价 6 位 HALF_UP");
    assertEquals(new BigDecimal("123.46"), saved.getAmount(), "金额 2 位 HALF_UP");
    assertEquals(new BigDecimal("10.000000"), saved.getBeforeAverageCost());
    assertEquals(new BigDecimal("11.615000"), saved.getAfterAverageCost());
    assertEquals(3, saved.getBeforeBalanceQty());
    assertEquals(13, saved.getAfterBalanceQty());
    assertEquals("TRANSFER_IN", saved.getBusinessType());
    assertEquals("TR-1", saved.getBusinessKey());
    assertEquals("TRANSFER_OUT:TR-1", saved.getCounterpartKey());
    assertEquals("DB", saved.getLedgerSource());
    assertEquals("ADJUST:TR-1:7:IN", saved.getIdempotencyKey());
    assertEquals("调拨转入", saved.getRemark());
    assertEquals("{}", saved.getExtraData());
  }

  @Test
  @DisplayName("重复幂等键（INSERT 冲突返回 0）不重复写入")
  void duplicateIdempotencyKeyIsNotWrittenTwice() {
    InvCostLogMapper mapper = mock(InvCostLogMapper.class);
    when(mapper.insertLog(any(InvCostLog.class))).thenReturn(0);
    InventoryCostLogService service = new InventoryCostLogService(mapper);

    CostMovement movement = MovingAverageCostService.outboundMovement(
        new BigDecimal("10"), 10, 2);
    boolean recorded = service.record(inventory(), movement, CostContext.builder()
        .businessType("CONFIRM")
        .idempotencyKey("CONFIRM:SO-1")
        .build(), TENANT);

    assertFalse(recorded);
    verify(mapper).insertLog(any(InvCostLog.class));
  }

  @Test
  @DisplayName("无租户上下文不写成本流水")
  void missingTenantSkipsCostLog() {
    InvCostLogMapper mapper = mock(InvCostLogMapper.class);
    InventoryCostLogService service = new InventoryCostLogService(mapper);

    boolean recorded = service.record(inventory(),
        MovingAverageCostService.outboundMovement(BigDecimal.TEN, 10, 1),
        ContextHolderTestSupport.emptyContext(), null);

    assertFalse(recorded);
    verify(mapper, never()).insertLog(any(InvCostLog.class));
  }

  @Test
  @DisplayName("Redis 路径没有台账行时用 legacy sku/仓库编码记账")
  void redisPathUsesLegacyIdentifiers() {
    InvCostLogMapper mapper = mock(InvCostLogMapper.class);
    when(mapper.insertLog(any(InvCostLog.class))).thenReturn(1);
    InventoryCostLogService service = new InventoryCostLogService(mapper);

    CostContext context = ContextHolderTestSupport.redisContext();
    assertTrue(service.record(null,
        MovingAverageCostService.outboundMovement(new BigDecimal("3.5"), null, 4),
        context, TENANT));

    ArgumentCaptor<InvCostLog> captor = ArgumentCaptor.forClass(InvCostLog.class);
    verify(mapper).insertLog(captor.capture());
    InvCostLog saved = captor.getValue();
    assertEquals(1001L, saved.getLegacySkuId());
    assertEquals("WH-MAIN", saved.getWarehouseCode());
    assertEquals("REDIS", saved.getLedgerSource());
    assertEquals("OUT", saved.getDirection());
    assertEquals(new BigDecimal("3.500000"), saved.getUnitPrice());
    assertEquals(new BigDecimal("14.00"), saved.getAmount());
    assertEquals(null, saved.getBeforeBalanceQty());
  }

  @Test
  @DisplayName("显式租户参数优先于上下文")
  void explicitTenantOverridesContext() {
    TenantContextHolder.setTenantId(UUID.fromString(
        "99999999-9999-9999-9999-999999999999"));
    InvCostLogMapper mapper = mock(InvCostLogMapper.class);
    when(mapper.insertLog(any(InvCostLog.class))).thenReturn(1);
    InventoryCostLogService service = new InventoryCostLogService(mapper);

    service.record(inventory(), MovingAverageCostService.outboundMovement(BigDecimal.ONE, 1, 1),
        CostContext.builder().businessType("ADJUST").build(), TENANT);

    ArgumentCaptor<InvCostLog> captor = ArgumentCaptor.forClass(InvCostLog.class);
    verify(mapper).insertLog(captor.capture());
    assertEquals(TENANT, captor.getValue().getTenantId());
  }

  private static Inventory inventory() {
    Inventory inventory = new Inventory();
    inventory.setId(INVENTORY_ID);
    inventory.setSkuId(SKU);
    inventory.setWarehouseId(WAREHOUSE);
    inventory.setTotalStock(10);
    inventory.setAvailableStock(10);
    inventory.setAverageCost(new BigDecimal("10.000000"));
    return inventory;
  }

  /** 集中构造测试上下文，避免测试主体被冗长的 builder 淹没。 */
  private static final class ContextHolderTestSupport {
    static CostContext emptyContext() {
      return CostContext.builder().businessType("ADJUST").build();
    }

    static CostContext redisContext() {
      return CostContext.builder()
          .businessType("DUBBO_DEDUCT")
          .businessKey("REQ-1")
          .requestId("REQ-1")
          .referenceType("DUBBO")
          .legacySkuId(1001L)
          .warehouseCode("WH-MAIN")
          .ledgerSource("REDIS")
          .idempotencyKey("DUBBO_DEDUCT:REQ-1:1001")
          .build();
    }
  }
}
