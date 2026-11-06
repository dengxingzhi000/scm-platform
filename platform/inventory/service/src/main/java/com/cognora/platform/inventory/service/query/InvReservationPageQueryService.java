package com.cognora.platform.inventory.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.inventory.domain.ReservationStatus;
import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import com.cognora.platform.inventory.dto.InvReservationQueryRequest;
import com.cognora.platform.inventory.dto.InvReservationResponse;
import com.cognora.platform.inventory.mapper.InvReservationRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 预占持久化记录的分页查询服务（供前端列表/详情使用）。
 *
 * <p>与 Redis 版的 {@link InvReservationQueryService}（存在性/数量查询）职责区分：
 * 本服务查询 {@code inv_reservation_record} 的完整状态行。
 *
 * <p>租户隔离由 TenantInterceptor 在 SQL 层追加 tenant_id；本服务不额外拼租户条件。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Service
@RequiredArgsConstructor
public class InvReservationPageQueryService {

  private final InvReservationRecordMapper reservationRecordMapper;

  @Slave
  public Page<InvReservationResponse> query(InvReservationQueryRequest request) {
    LambdaQueryWrapper<InvReservationRecord> wrapper = Wrappers.lambdaQuery();
    wrapper.eq(StringUtils.hasText(request.getSkuId()),
            InvReservationRecord::getSkuId, request.getSkuId())
        .eq(StringUtils.hasText(request.getWarehouseId()),
            InvReservationRecord::getWarehouseId, request.getWarehouseId())
        .like(StringUtils.hasText(request.getBusinessKey()),
            InvReservationRecord::getBusinessKey, request.getBusinessKey())
        .eq(StringUtils.hasText(request.getStatus()),
            InvReservationRecord::getStatus, normalizeStatus(request.getStatus()))
        .ge(request.getStartTime() != null,
            InvReservationRecord::getCreateTime, request.getStartTime())
        .le(request.getEndTime() != null,
            InvReservationRecord::getCreateTime, request.getEndTime())
        .orderByDesc(InvReservationRecord::getCreateTime);

    Page<InvReservationRecord> page = reservationRecordMapper.selectPage(
        new Page<>(request.getPage(), request.getSize()), wrapper);
    Page<InvReservationResponse> responsePage =
        new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    responsePage.setRecords(page.getRecords().stream()
        .map(InvReservationResponse::from).toList());
    return responsePage;
  }

  @Slave
  public InvReservationResponse getByBusinessKey(String businessKey) {
    InvReservationRecord record = reservationRecordMapper.selectOne(
        Wrappers.<InvReservationRecord>lambdaQuery()
            .eq(InvReservationRecord::getBusinessKey, businessKey));
    return record == null ? null : InvReservationResponse.from(record);
  }

  private static String normalizeStatus(String status) {
    try {
      return ReservationStatus.valueOf(status.trim().toUpperCase()).name();
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("非法预占状态: " + status
          + "，可选 RESERVED/CONFIRMED/RELEASED/EXPIRED");
    }
  }
}
