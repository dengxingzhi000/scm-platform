package com.cognora.platform.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.inventory.domain.entity.InvSnapshot;
import com.cognora.platform.inventory.dto.InvSnapshotGenerationResult;
import com.cognora.platform.inventory.dto.InvSnapshotQueryRequest;
import com.cognora.platform.inventory.service.command.InvSnapshotCommandService;
import com.cognora.platform.inventory.service.query.InvSnapshotQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 库存快照控制器
 *
 * <p>提供每日库存快照的查询、趋势与手动生成接口。
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory/snapshots")
@Tag(name = "库存快照", description = "提供库存每日快照的查询、趋势与生成接口。")
public class InvSnapshotController {

  private final InvSnapshotQueryService queryService;
  private final InvSnapshotCommandService commandService;

  /**
   * 分页查询快照
   */
  @Operation(summary = "分页查询快照", description = "按日期、SKU 和仓库分页查询库存每日快照。")
  @PostMapping("/query")
  public ApiResponse<Page<InvSnapshot>> query(
      @RequestBody @Valid InvSnapshotQueryRequest request) {
    return ApiResponse.success(queryService.query(request));
  }

  /**
   * 查询库存水位趋势
   */
  @Operation(summary = "查询库存趋势", description = "查询指定 SKU/仓库最近若干天的库存快照，用于绘制水位趋势。")
  @GetMapping("/trend")
  public ApiResponse<List<InvSnapshot>> trend(
      @Parameter(description = "SKU ID") @RequestParam(required = false) String skuId,
      @Parameter(description = "仓库 ID") @RequestParam(required = false) String warehouseId,
      @Parameter(description = "统计天数，默认 30 天")
      @RequestParam(required = false) @Min(value = 1, message = "天数不能小于1")
      @Max(value = 365, message = "天数不能超过365") Integer days) {
    return ApiResponse.success(
        queryService.trend(skuId, warehouseId, days == null ? 30 : days));
  }

  /**
   * 查询快照详情
   */
  @Operation(summary = "查询快照详情", description = "根据快照 ID 查询库存快照明细。")
  @GetMapping("/{id}")
  public ApiResponse<InvSnapshot> get(
      @Parameter(description = "快照 ID") @PathVariable @NotBlank(message = "快照 ID 不能为空") String id) {
    InvSnapshot snapshot = queryService.getById(id);
    return snapshot == null
        ? ApiResponse.fail(ResultCode.NOT_FOUND.getCode(), "快照不存在")
        : ApiResponse.success(snapshot);
  }

  /**
   * 手动生成快照
   */
  @Operation(summary = "生成库存快照", description = "为当前租户生成指定日期的库存快照，默认使用当天；同日重复执行会跳过已生成的记录。")
  @PostMapping("/generate")
  public ApiResponse<InvSnapshotGenerationResult> generate(
      @Parameter(description = "快照日期，格式 yyyy-MM-dd，默认当天")
      @RequestParam(required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ApiResponse.success(
        commandService.generateDailySnapshots(date == null ? LocalDate.now() : date));
  }
}
