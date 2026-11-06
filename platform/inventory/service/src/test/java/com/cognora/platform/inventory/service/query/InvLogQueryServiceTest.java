package com.cognora.platform.inventory.service.query;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.inventory.domain.entity.InvLog;
import com.cognora.platform.inventory.dto.InvLogQueryRequest;
import com.cognora.platform.inventory.dto.InvLogResponse;
import com.cognora.platform.inventory.mapper.InvLogMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存流水查询接口用例：前后四元组映射、筛选条件。
 */
class InvLogQueryServiceTest {

  @BeforeAll
  static void initTableMetadata() {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), ""), InvLog.class);
  }

  private InvLogMapper invLogMapper;

  private InvLogQueryService service;

  @BeforeEach
  void setUp() {
    invLogMapper = mock(InvLogMapper.class);
    service = new InvLogQueryService(invLogMapper);
  }

  @Test
  @DisplayName("分页返回变更前后 total/available/locked/damaged")
  void queryMapsBeforeAfterSnapshot() {
    when(invLogMapper.selectPage(any(), any())).thenReturn(page(log()));

    Page<InvLogResponse> result = service.query(new InvLogQueryRequest());

    assertEquals(1, result.getTotal());
    InvLogResponse row = result.getRecords().get(0);
    assertEquals("ORD-1", row.getBusinessKey());
    assertEquals("RESERVE", row.getBusinessType());
    assertEquals(-3, row.getChangeQuantity());
    assertEquals(10, row.getBeforeTotal());
    assertEquals(10, row.getBeforeAvailable());
    assertEquals(0, row.getBeforeLocked());
    assertEquals(0, row.getBeforeDamaged());
    assertEquals(10, row.getAfterTotal());
    assertEquals(7, row.getAfterAvailable());
    assertEquals(3, row.getAfterLocked());
    assertEquals(0, row.getAfterDamaged());
    assertEquals("张三", row.getOperatorName());
    assertEquals("DB", row.getLedgerSource());
    assertNotNull(row.getCreateTime());
  }

  @Test
  @DisplayName("筛选：SKU、单据号、仓库、变更类型、时间范围都进入 SQL")
  void filtersAreApplied() {
    when(invLogMapper.selectPage(any(), any())).thenReturn(page(log()));

    InvLogQueryRequest request = new InvLogQueryRequest();
    request.setSkuId("SKU-1");
    request.setWarehouseId("WH-1");
    request.setBusinessKey("ORD-1");
    request.setBusinessType("RESERVE");
    request.setOperationType(3);
    request.setStartTime(LocalDateTime.of(2026, 1, 1, 0, 0));
    request.setEndTime(LocalDateTime.of(2026, 12, 31, 23, 59));
    service.query(request);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Wrapper<InvLog>> captor = ArgumentCaptor.forClass(Wrapper.class);
    verify(invLogMapper).selectPage(any(), captor.capture());
    String sql = captor.getValue().getSqlSegment();
    assertTrue(sql.contains("sku_id"), sql);
    assertTrue(sql.contains("warehouse_id"), sql);
    assertTrue(sql.contains("business_key"), sql);
    assertTrue(sql.contains("business_type"), sql);
    assertTrue(sql.contains("operation_type"), sql);
    assertTrue(sql.contains("create_time"), sql);
    java.util.Map<String, Object> params = paramValues(captor.getValue());
    assertTrue(params.containsValue("SKU-1"), params.toString());
    assertTrue(params.containsValue("WH-1"), params.toString());
    assertTrue(params.containsValue("ORD-1"), params.toString());
    assertTrue(params.containsValue("RESERVE"), params.toString());
    assertTrue(params.containsValue(3), params.toString());
    assertTrue(params.containsValue(LocalDateTime.of(2026, 1, 1, 0, 0)), params.toString());
  }

  @Test
  @DisplayName("空结果返回 total=0 且 records 为空")
  void emptyPage() {
    Page<InvLog> empty = new Page<>(1, 20, 0);
    empty.setRecords(List.of());
    when(invLogMapper.selectPage(any(), any())).thenReturn(empty);

    Page<InvLogResponse> result = service.query(new InvLogQueryRequest());
    assertEquals(0, result.getTotal());
    assertTrue(result.getRecords().isEmpty());
  }

  @SuppressWarnings("unchecked")
  private static java.util.Map<String, Object> paramValues(Wrapper<?> wrapper) {
    return ((com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?>) wrapper)
        .getParamNameValuePairs();
  }

  private static Page<InvLog> page(InvLog log) {
    Page<InvLog> page = new Page<>(1, 20, 1);
    page.setRecords(List.of(log));
    return page;
  }

  private static InvLog log() {
    InvLog log = new InvLog();
    log.setId("log-1");
    log.setTenantId("tenant-1");
    log.setSkuId("SKU-1");
    log.setWarehouseId("WH-1");
    log.setOperationType(3);
    log.setBusinessType("RESERVE");
    log.setQuantity(-3);
    log.setBeforeTotal(10);
    log.setBeforeAvailable(10);
    log.setBeforeLocked(0);
    log.setBeforeDamaged(0);
    log.setAfterTotal(10);
    log.setAfterAvailable(7);
    log.setAfterLocked(3);
    log.setAfterDamaged(0);
    log.setBusinessKey("ORD-1");
    log.setRequestId("REQ-1");
    log.setOperatorId("user-1");
    log.setOperatorName("张三");
    log.setLedgerSource("DB");
    log.setCreateTime(LocalDateTime.now());
    return log;
  }
}
