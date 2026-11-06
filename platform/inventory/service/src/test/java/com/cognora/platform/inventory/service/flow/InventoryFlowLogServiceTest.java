package com.cognora.platform.inventory.service.flow;

import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.inventory.domain.entity.InvLog;
import com.cognora.platform.inventory.mapper.InvLogMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 流水写入服务用例：字段完整性、幂等去重、租户解析。
 */
class InventoryFlowLogServiceTest {

  private InvLogMapper invLogMapper;

  private InventoryFlowLogService service;

  @BeforeEach
  void setUp() {
    invLogMapper = mock(InvLogMapper.class);
    service = new InventoryFlowLogService(invLogMapper);
  }

  @AfterEach
  void tearDown() {
    TenantContextHolder.clear();
  }

  @Test
  @DisplayName("字段完整：前后 total/available/locked/damaged、业务类型、操作人、业务单号、租户、时间")
  void recordPersistsAllFields() {
    String tenant = UUID.randomUUID().toString();
    String sku = UUID.randomUUID().toString();
    String warehouse = UUID.randomUUID().toString();
    String inventory = UUID.randomUUID().toString();
    String operator = UUID.randomUUID().toString();

    FlowLogEntry entry = FlowLogEntry.builder()
        .tenantId(tenant)
        .skuId(sku)
        .warehouseId(warehouse)
        .operationType(6)
        .businessType("ADJUST")
        .changeQuantity(5)
        .beforeTotal(10)
        .beforeAvailable(10)
        .beforeLocked(0)
        .beforeDamaged(0)
        .afterTotal(15)
        .afterAvailable(15)
        .afterLocked(0)
        .afterDamaged(0)
        .reason("盘点调整")
        .businessKey("DOC-1")
        .requestId("REQ-1")
        .referenceType("ADJUST")
        .referenceId(inventory)
        .referenceNo("DOC-1")
        .operatorId(operator)
        .operatorName("张三")
        .idempotencyKey("ADJUST:DOC-1:6:IN")
        .build();

    when(invLogMapper.claimIdempotency(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(1);
    when(invLogMapper.insertLog(any(InvLog.class))).thenReturn(1);

    assertTrue(service.record(entry));

    ArgumentCaptor<InvLog> captor = ArgumentCaptor.forClass(InvLog.class);
    verify(invLogMapper).insertLog(captor.capture());
    InvLog log = captor.getValue();

    assertNotNull(log.getId());
    assertEquals(tenant, log.getTenantId());
    assertEquals(sku, log.getSkuId());
    assertEquals(warehouse, log.getWarehouseId());
    assertEquals(6, log.getOperationType());
    assertEquals("ADJUST", log.getBusinessType());
    assertEquals(5, log.getQuantity());
    assertEquals(10, log.getBeforeTotal());
    assertEquals(10, log.getBeforeAvailable());
    assertEquals(0, log.getBeforeLocked());
    assertEquals(0, log.getBeforeDamaged());
    assertEquals(15, log.getAfterTotal());
    assertEquals(15, log.getAfterAvailable());
    assertEquals(0, log.getAfterLocked());
    assertEquals(0, log.getAfterDamaged());
    assertEquals("盘点调整", log.getReason());
    assertEquals("DOC-1", log.getBusinessKey());
    assertEquals("REQ-1", log.getRequestId());
    assertEquals("ADJUST", log.getReferenceType());
    assertEquals(inventory, log.getReferenceId());
    assertEquals("DOC-1", log.getReferenceNo());
    assertEquals(operator, log.getOperatorId());
    assertEquals("张三", log.getOperatorName());
    assertEquals("DB", log.getLedgerSource());
    assertEquals("{}", log.getExtraData());
    assertNotNull(log.getCreateTime());
  }

  @Test
  @DisplayName("重复幂等键只写一条：claim 返回 0 时不再 insert")
  void duplicateIdempotencyKeyWritesNothing() {
    FlowLogEntry entry = baseEntry().idempotencyKey("DUBBO_DEDUCT:REQ-9:1").build();
    when(invLogMapper.claimIdempotency(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(0);

    assertFalse(service.record(entry));
    verify(invLogMapper, never()).insertLog(any(InvLog.class));
  }

  @Test
  @DisplayName("首次写入：claim 与 insert 各一次")
  void firstWriteClaimsAndInserts() {
    FlowLogEntry entry = baseEntry().idempotencyKey("RESERVE:ORD-1").build();
    when(invLogMapper.claimIdempotency(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(1);
    when(invLogMapper.insertLog(any(InvLog.class))).thenReturn(1);

    assertTrue(service.record(entry));
    verify(invLogMapper).insertLog(any(InvLog.class));
  }

  @Test
  @DisplayName("无 idempotencyKey 时直接写流水")
  void withoutIdempotencyKeyAlwaysInserts() {
    when(invLogMapper.insertLog(any(InvLog.class))).thenReturn(1);

    assertTrue(service.record(baseEntry().tenantId(UUID.randomUUID().toString()).build()));
    verify(invLogMapper, never())
        .claimIdempotency(anyString(), anyString(), anyString(), anyString());
    verify(invLogMapper).insertLog(any(InvLog.class));
  }

  @Test
  @DisplayName("缺租户上下文时跳过写入，不误写其它租户")
  void missingTenantSkipsWrite() {
    TenantContextHolder.clear();
    assertFalse(service.record(baseEntry().tenantId(null).build()));
    verifyNoInteractions(invLogMapper);
  }

  @Test
  @DisplayName("未提供 tenantId 时从登录上下文取租户")
  void tenantFallbackFromContext() {
    UUID tenant = UUID.randomUUID();
    TenantContextHolder.setTenantId(tenant);
    when(invLogMapper.insertLog(any(InvLog.class))).thenReturn(1);

    assertTrue(service.record(baseEntry().tenantId(null).build()));
    ArgumentCaptor<InvLog> captor = ArgumentCaptor.forClass(InvLog.class);
    verify(invLogMapper).insertLog(captor.capture());
    assertEquals(tenant.toString(), captor.getValue().getTenantId());
  }

  @Test
  @DisplayName("operationType 缺失时拒绝写流水")
  void missingOperationTypeRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> service.record(baseEntry().operationType(null).build()));
    verifyNoInteractions(invLogMapper);
  }

  @Test
  @DisplayName("恒等式：before/after 均满足 total = available + locked + damaged")
  void balanceIdentityHolds() {
    FlowLogEntry entry = baseEntry()
        .beforeTotal(10).beforeAvailable(4).beforeLocked(5).beforeDamaged(1)
        .afterTotal(12).afterAvailable(6).afterLocked(5).afterDamaged(1)
        .build();
    when(invLogMapper.insertLog(any(InvLog.class))).thenReturn(1);

    assertTrue(service.record(entry));
    assertEquals(entry.getBeforeTotal(),
        entry.getBeforeAvailable() + entry.getBeforeLocked() + entry.getBeforeDamaged());
    assertEquals(entry.getAfterTotal(),
        entry.getAfterAvailable() + entry.getAfterLocked() + entry.getAfterDamaged());
  }

  private static FlowLogEntry.FlowLogEntryBuilder baseEntry() {
    return FlowLogEntry.builder()
        .tenantId(UUID.randomUUID().toString())
        .skuId(UUID.randomUUID().toString())
        .warehouseId(UUID.randomUUID().toString())
        .operationType(3)
        .businessType("RESERVE")
        .changeQuantity(-1)
        .beforeTotal(10)
        .beforeAvailable(10)
        .beforeLocked(0)
        .beforeDamaged(0)
        .afterTotal(10)
        .afterAvailable(9)
        .afterLocked(1)
        .afterDamaged(0)
        .businessKey("ORD-1");
  }
}
