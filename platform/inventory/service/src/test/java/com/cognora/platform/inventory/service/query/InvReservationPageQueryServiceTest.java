package com.cognora.platform.inventory.service.query;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import com.cognora.platform.inventory.dto.InvReservationQueryRequest;
import com.cognora.platform.inventory.dto.InvReservationResponse;
import com.cognora.platform.inventory.mapper.InvReservationRecordMapper;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 预占分页查询用例：分页映射、筛选条件、状态校验。
 */
class InvReservationPageQueryServiceTest {

  @BeforeAll
  static void initTableMetadata() {
    TableInfoHelper.initTableInfo(
        new MapperBuilderAssistant(new MybatisConfiguration(), ""), InvReservationRecord.class);
  }

  private InvReservationRecordMapper recordMapper;

  private InvReservationPageQueryService service;

  @BeforeEach
  void setUp() {
    recordMapper = mock(InvReservationRecordMapper.class);
    service = new InvReservationPageQueryService(recordMapper);
  }

  @Test
  @DisplayName("分页查询返回预占行与关联单据，状态名可读")
  void queryMapsReservationRows() {
    when(recordMapper.selectPage(any(), any())).thenReturn(page(record("RESERVED")));

    InvReservationQueryRequest request = new InvReservationQueryRequest();
    request.setBusinessKey("ORD");
    request.setStatus("reserved");
    request.setStartTime(LocalDateTime.now().minusDays(1));
    request.setEndTime(LocalDateTime.now());

    Page<InvReservationResponse> result = service.query(request);

    assertEquals(1, result.getTotal());
    assertEquals(1, result.getRecords().size());
    InvReservationResponse row = result.getRecords().get(0);
    assertEquals("ORD-1", row.getBusinessKey());
    assertEquals("RESERVED", row.getStatus());
    assertEquals("已预占", row.getStatusName());
    assertEquals("ORDER", row.getDocumentType());
    assertEquals("ORD-1", row.getDocumentNo());
    assertEquals(3, row.getQuantity());
    assertNotNull(row.getExpireAt());
  }

  @Test
  @DisplayName("筛选条件落到 SQL：业务键模糊、状态归一化、时间范围")
  void filtersAreApplied() {
    when(recordMapper.selectPage(any(), any())).thenReturn(page(record("RESERVED")));

    InvReservationQueryRequest request = new InvReservationQueryRequest();
    request.setBusinessKey("ORD");
    request.setStatus("confirmed");
    request.setSkuId("SKU-1");
    request.setWarehouseId("WH-1");
    request.setStartTime(LocalDateTime.of(2026, 1, 1, 0, 0));
    request.setEndTime(LocalDateTime.of(2026, 12, 31, 23, 59));
    service.query(request);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Wrapper<InvReservationRecord>> captor = ArgumentCaptor.forClass(Wrapper.class);
    verify(recordMapper).selectPage(any(), captor.capture());
    String sql = captor.getValue().getSqlSegment();
    assertTrue(sql.contains("business_key"), sql);
    assertTrue(sql.contains("status"), sql);
    assertTrue(sql.contains("create_time"), sql);
    assertTrue(sql.contains("sku_id"), sql);
    assertTrue(sql.contains("warehouse_id"), sql);
    java.util.Map<String, Object> params = paramValues(captor.getValue());
    assertTrue(params.containsValue("%ORD%"), params.toString());
    assertTrue(params.containsValue("CONFIRMED"), params.toString());
    assertTrue(params.containsValue("SKU-1"), params.toString());
    assertTrue(params.containsValue("WH-1"), params.toString());
  }

  @Test
  @DisplayName("非法状态直接拒绝，不落到 SQL")
  void invalidStatusRejected() {
    InvReservationQueryRequest request = new InvReservationQueryRequest();
    request.setStatus("UNKNOWN");
    assertThrows(IllegalArgumentException.class, () -> service.query(request));
  }

  @Test
  @DisplayName("按业务键查询：不存在返回 null，存在返回带状态的行")
  void getByBusinessKey() {
    when(recordMapper.selectOne(any())).thenReturn(null);
    assertNull(service.getByBusinessKey("MISSING"));

    when(recordMapper.selectOne(any())).thenReturn(record("EXPIRED"));
    InvReservationResponse response = service.getByBusinessKey("ORD-1");
    assertNotNull(response);
    assertEquals("已过期", response.getStatusName());
  }

  @SuppressWarnings("unchecked")
  private static java.util.Map<String, Object> paramValues(Wrapper<?> wrapper) {
    return ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs();
  }

  private static Page<InvReservationRecord> page(InvReservationRecord record) {
    Page<InvReservationRecord> page = new Page<>(1, 20, 1);
    page.setRecords(List.of(record));
    return page;
  }

  private static InvReservationRecord record(String status) {
    InvReservationRecord record = new InvReservationRecord();
    record.setId("res-1");
    record.setTenantId("tenant-1");
    record.setBusinessKey("ORD-1");
    record.setSkuId("SKU-1");
    record.setWarehouseId("WH-1");
    record.setInventoryId("inv-1");
    record.setQuantity(3);
    record.setStatus(status);
    record.setExpireAt(LocalDateTime.now().plusMinutes(5));
    record.setReservedAt(LocalDateTime.now());
    record.setCreateTime(LocalDateTime.now());
    record.setUpdateTime(LocalDateTime.now());
    return record;
  }
}
