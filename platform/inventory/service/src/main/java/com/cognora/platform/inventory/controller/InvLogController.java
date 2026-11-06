package com.cognora.platform.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.inventory.dto.InvLogQueryRequest;
import com.cognora.platform.inventory.dto.InvLogResponse;
import com.cognora.platform.inventory.service.query.InvLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存流水查询控制器。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory/logs")
@Tag(name = "库存流水", description = "提供库存变动流水分页查询。")
public class InvLogController {

  private final InvLogQueryService queryService;

  @Operation(summary = "分页查询库存流水",
      description = "按 SKU、单据号、仓库、变更类型与时间范围分页查询，返回变更前后四元组。")
  @PostMapping("/query")
  public ApiResponse<Page<InvLogResponse>> query(
      @RequestBody @Valid InvLogQueryRequest request) {
    return ApiResponse.success(queryService.query(request));
  }
}
