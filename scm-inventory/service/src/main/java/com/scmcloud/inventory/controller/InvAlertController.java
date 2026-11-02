package com.scmcloud.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.inventory.domain.entity.InvAlert;
import com.scmcloud.inventory.dto.InvAlertQueryRequest;
import com.scmcloud.inventory.dto.InvAlertResponse;
import com.scmcloud.inventory.dto.InvAlertScanResult;
import com.scmcloud.inventory.dto.InvAlertStatsResponse;
import com.scmcloud.inventory.service.command.InvAlertCommandService;
import com.scmcloud.inventory.service.query.InvAlertQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存预警控制器
 *
 * <p>提供告警分页查询、统计、处理以及手动触发扫描的接口。
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory/alerts")
@Tag(name = "库存预警", description = "提供库存告警查询、统计、处理与扫描接口。")
public class InvAlertController {

  private final InvAlertQueryService queryService;
  private final InvAlertCommandService commandService;

  /**
   * 分页查询告警
   */
  @Operation(summary = "分页查询告警", description = "按 SKU、仓库、告警类型和解决状态分页查询库存告警。")
  @PostMapping("/query")
  public ApiResponse<Page<InvAlertResponse>> query(
      @RequestBody @Valid InvAlertQueryRequest request) {
    Page<InvAlert> page = queryService.query(request);
    Page<InvAlertResponse> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
    responsePage.setRecords(page.getRecords().stream().map(InvAlertResponse::from).toList());
    return ApiResponse.success(responsePage);
  }

  /**
   * 查询告警统计
   */
  @Operation(summary = "查询告警统计", description = "统计告警总数、未解决数量以及库存不足与缺货告警数量。")
  @GetMapping("/stats")
  public ApiResponse<InvAlertStatsResponse> stats() {
    return ApiResponse.success(queryService.stats());
  }

  /**
   * 查询告警详情
   */
  @Operation(summary = "查询告警详情", description = "根据告警 ID 查询告警明细。")
  @GetMapping("/{id}")
  public ApiResponse<InvAlertResponse> get(
      @Parameter(description = "告警 ID") @PathVariable @NotBlank(message = "告警 ID 不能为空") String id) {
    InvAlert alert = queryService.getById(id);
    return alert == null
        ? ApiResponse.fail(ResultCode.NOT_FOUND.getCode(), "告警不存在")
        : ApiResponse.success(InvAlertResponse.from(alert));
  }

  /**
   * 处理告警
   */
  @Operation(summary = "处理告警", description = "将指定告警标记为已解决，并记录处理人与处理时间。")
  @PutMapping("/{id}/resolve")
  public ApiResponse<Boolean> resolve(
      @Parameter(description = "告警 ID") @PathVariable @NotBlank(message = "告警 ID 不能为空") String id,
      @Parameter(description = "处理人，缺省记为 SYSTEM")
      @RequestParam(required = false) String resolvedBy) {
    return ApiResponse.success(commandService.resolve(id, resolvedBy) > 0);
  }

  /**
   * 手动触发告警扫描
   */
  @Operation(summary = "触发告警扫描", description = "扫描当前租户全部库存，生成或刷新库存水位告警，并自动解除已恢复库存的告警。")
  @PostMapping("/scan")
  public ApiResponse<InvAlertScanResult> scan() {
    return ApiResponse.success(commandService.scanAndGenerate());
  }
}
