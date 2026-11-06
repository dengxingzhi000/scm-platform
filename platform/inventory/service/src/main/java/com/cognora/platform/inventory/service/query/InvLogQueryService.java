package com.cognora.platform.inventory.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.inventory.domain.entity.InvLog;
import com.cognora.platform.inventory.dto.InvLogQueryRequest;
import com.cognora.platform.inventory.dto.InvLogResponse;
import com.cognora.platform.inventory.mapper.InvLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 库存流水查询服务。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@Service
@RequiredArgsConstructor
public class InvLogQueryService {

  private final InvLogMapper invLogMapper;

  @Slave
  public Page<InvLogResponse> query(InvLogQueryRequest request) {
    LambdaQueryWrapper<InvLog> wrapper = Wrappers.lambdaQuery();
    wrapper.eq(StringUtils.hasText(request.getSkuId()), InvLog::getSkuId, request.getSkuId())
        .eq(StringUtils.hasText(request.getWarehouseId()),
            InvLog::getWarehouseId, request.getWarehouseId())
        .eq(StringUtils.hasText(request.getBusinessKey()),
            InvLog::getBusinessKey, request.getBusinessKey())
        .eq(StringUtils.hasText(request.getBusinessType()),
            InvLog::getBusinessType, request.getBusinessType())
        .eq(request.getOperationType() != null,
            InvLog::getOperationType, request.getOperationType())
        .ge(request.getStartTime() != null, InvLog::getCreateTime, request.getStartTime())
        .le(request.getEndTime() != null, InvLog::getCreateTime, request.getEndTime())
        .orderByDesc(InvLog::getCreateTime);

    Page<InvLog> page = invLogMapper.selectPage(
        new Page<>(request.getPage(), request.getSize()), wrapper);
    Page<InvLogResponse> responsePage =
        new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    responsePage.setRecords(page.getRecords().stream()
        .map(InvLogResponse::from).toList());
    return responsePage;
  }
}
