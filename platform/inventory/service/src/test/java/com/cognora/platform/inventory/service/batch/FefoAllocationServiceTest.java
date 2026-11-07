package com.cognora.platform.inventory.service.batch;

import com.cognora.platform.common.exception.ServiceException;
import com.cognora.platform.inventory.domain.entity.InvBatch;
import com.cognora.platform.inventory.mapper.InvBatchMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * FEFO 分配服务用例：预览只读、无批次回退、量不足拒绝、并发扣减失败即整单失败。
 */
class FefoAllocationServiceTest {

  private static final String TENANT = "019a0aee-0000-7000-8000-000000000002";
  private static final String SKU = "22222222-2222-2222-2222-222222222222";
  private static final String WH = "33333333-3333-3333-3333-333333333333";

  private final InvBatchMapper mapper = mock(InvBatchMapper.class);
  private final FefoAllocationService service = new FefoAllocationService(mapper);

  @Test
  @DisplayName("无任何批次记录：返回 batchManaged=false，调用方回退台账扣减")
  void previewWithoutBatchesFallsBackToLedger() {
    when(mapper.countManagedBatches(TENANT, SKU, WH)).thenReturn(0);

    FefoAllocationPlan plan = service.preview(TENANT, SKU, WH, 5);

    assertFalse(plan.batchManaged());
    assertEquals(5, plan.requested());
    assertEquals(0, plan.allocated());
    verify(mapper, never()).selectFefoCandidates(anyString(), anyString(), anyString());
    verify(mapper, never()).deductAvailable(anyString(), anyString(), anyInt());
  }

  @Test
  @DisplayName("预览只读：计算分配结果但不扣减任何批次")
  void previewDoesNotDeduct() {
    when(mapper.countManagedBatches(TENANT, SKU, WH)).thenReturn(2);
    when(mapper.selectFefoCandidates(TENANT, SKU, WH)).thenReturn(List.of(
        batch("B-1", "id-1", 4, LocalDate.now().plusDays(10), LocalDateTime.of(2026, 10, 1, 0, 0)),
        batch("B-2", "id-2", 6, LocalDate.now().plusDays(20), LocalDateTime.of(2026, 10, 2, 0, 0))));

    FefoAllocationPlan plan = service.preview(TENANT, SKU, WH, 8);

    assertTrue(plan.batchManaged());
    assertTrue(plan.sufficient());
    assertEquals(2, plan.allocations().size());
    assertEquals(4, plan.allocations().get(0).quantity());
    assertEquals(4, plan.allocations().get(1).quantity());
    verify(mapper, never()).deductAvailable(anyString(), anyString(), anyInt());
  }

  @Test
  @DisplayName("实际分配：每个批次只扣减一次，数量与计划一致")
  void allocateDeductsEachBatchOnce() {
    when(mapper.countManagedBatches(TENANT, SKU, WH)).thenReturn(2);
    when(mapper.selectFefoCandidates(TENANT, SKU, WH)).thenReturn(List.of(
        batch("B-1", "id-1", 5, LocalDate.now().plusDays(10), LocalDateTime.of(2026, 10, 1, 0, 0)),
        batch("B-2", "id-2", 5, LocalDate.now().plusDays(20), LocalDateTime.of(2026, 10, 2, 0, 0))));
    when(mapper.deductAvailable(anyString(), anyString(), anyInt())).thenReturn(1);

    FefoAllocationPlan plan = service.allocate(TENANT, SKU, WH, 8);

    ArgumentCaptor<String> ids = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> quantities = ArgumentCaptor.forClass(Integer.class);
    verify(mapper, times(2)).deductAvailable(ids.capture(), anyString(), quantities.capture());
    assertEquals(List.of("id-1", "id-2"), ids.getAllValues());
    assertEquals(List.of(5, 3), quantities.getAllValues());
    assertEquals(8, plan.allocated());
  }

  @Test
  @DisplayName("合格批次不足：整单拒绝（400），不做任何扣减")
  void allocateRejectsWhenInsufficient() {
    when(mapper.countManagedBatches(TENANT, SKU, WH)).thenReturn(1);
    when(mapper.selectFefoCandidates(TENANT, SKU, WH)).thenReturn(List.of(
        batch("B-1", "id-1", 3, LocalDate.now().plusDays(10), LocalDateTime.of(2026, 10, 1, 0, 0))));

    ServiceException ex = assertThrows(ServiceException.class,
        () -> service.allocate(TENANT, SKU, WH, 5));

    assertEquals(400, ex.getCode());
    assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    verify(mapper, never()).deductAvailable(anyString(), anyString(), anyInt());
  }

  @Test
  @DisplayName("过期批次被跳过：只扣减合格批次，消息中带过期批次号")
  void allocateSkipsExpiredBatches() {
    when(mapper.countManagedBatches(TENANT, SKU, WH)).thenReturn(2);
    when(mapper.selectFefoCandidates(TENANT, SKU, WH)).thenReturn(List.of(
        batch("B-EXPIRED", "id-exp", 10, LocalDate.now().minusDays(1), LocalDateTime.of(2026, 1, 1, 0, 0)),
        batch("B-VALID", "id-val", 6, LocalDate.now().plusDays(10), LocalDateTime.of(2026, 10, 1, 0, 0))));
    when(mapper.deductAvailable(anyString(), anyString(), anyInt())).thenReturn(1);

    FefoAllocationPlan plan = service.allocate(TENANT, SKU, WH, 4);

    assertTrue(plan.expiredSkipped());
    assertEquals(List.of("B-EXPIRED"), plan.expiredBatchNos());
    verify(mapper, times(1)).deductAvailable(anyString(), anyString(), anyInt());
    assertEquals("id-val", plan.allocations().get(0).batchId());
  }

  @Test
  @DisplayName("并发扣减同一批次：条件更新返回 0 时整单失败（409），不产生重复分配")
  void allocateFailsWhenConcurrentUpdateLost() {
    when(mapper.countManagedBatches(TENANT, SKU, WH)).thenReturn(2);
    when(mapper.selectFefoCandidates(TENANT, SKU, WH)).thenReturn(List.of(
        batch("B-1", "id-1", 5, LocalDate.now().plusDays(10), LocalDateTime.of(2026, 10, 1, 0, 0)),
        batch("B-2", "id-2", 5, LocalDate.now().plusDays(20), LocalDateTime.of(2026, 10, 2, 0, 0))));
    // 第一个批次扣减成功，第二个批次已被并发事务改小，条件更新返回 0
    when(mapper.deductAvailable(anyString(), anyString(), anyInt())).thenReturn(1, 0);

    ServiceException ex = assertThrows(ServiceException.class,
        () -> service.allocate(TENANT, SKU, WH, 8));

    assertEquals(409, ex.getCode());
    assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    assertTrue(ex.getMessage().contains("B-2"));
  }

  @Test
  @DisplayName("必填参数校验：租户/SKU/仓库为空或数量非正时拒绝")
  void validateRejectsInvalidInput() {
    assertThrows(ServiceException.class, () -> service.preview(" ", SKU, WH, 1));
    assertThrows(ServiceException.class, () -> service.preview(TENANT, "", WH, 1));
    assertThrows(ServiceException.class, () -> service.preview(TENANT, SKU, null, 1));
    assertThrows(ServiceException.class, () -> service.preview(TENANT, SKU, WH, 0));
  }

  private static InvBatch batch(String code, String id, int available, LocalDate expiry,
                                LocalDateTime createTime) {
    InvBatch batch = new InvBatch();
    batch.setId(id);
    batch.setBatchCode(code);
    batch.setExpiryDate(expiry);
    batch.setTotalQuantity(available);
    batch.setAvailableQuantity(available);
    batch.setLockedQuantity(0);
    batch.setDamagedQuantity(0);
    batch.setQualityStatus(1);
    batch.setStatus(1);
    batch.setCreateTime(createTime);
    return batch;
  }
}
