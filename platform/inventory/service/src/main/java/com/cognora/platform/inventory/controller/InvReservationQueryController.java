package com.cognora.platform.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.inventory.dto.InvReservationQueryRequest;
import com.cognora.platform.inventory.dto.InvReservationResponse;
import com.cognora.platform.inventory.service.query.InvReservationPageQueryService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预占查询控制器（面向前端）。
 *
 * <p>路径使用复数 {@code /api/v1/inventory/reservations}，与内部服务间接口
 * {@code /api/v1/inventory/reservation/**}（HMAC 签名保护）明确隔离，
 * 避免把查询能力暴露到内部通道上。
 *
 * @author SCM Platform Team
 * @since 2026-11-06
 */
@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/inventory/reservations")
@Tag(name = "库存预占", description = "提供预占分页查询与按业务键查询。")
public class InvReservationQueryController {

  private final InvReservationPageQueryService queryService;

  @Operation(summary = "分页查询预占",
      description = "按业务键、SKU、仓库、状态与创建时间范围分页查询预占记录。")
  @PostMapping("/query")
  public ApiResponse<Page<InvReservationResponse>> query(
      @RequestBody @Valid InvReservationQueryRequest request) {
    return ApiResponse.success(queryService.query(request));
  }

  @Operation(summary = "按业务键查询预占",
      description = "返回该业务键下唯一的预占记录及状态时间线。")
  @GetMapping("/{businessKey}")
  public ApiResponse<InvReservationResponse> get(
      @Parameter(description = "业务键（订单号等）")
      @PathVariable @NotBlank(message = "业务键不能为空") String businessKey) {
    InvReservationResponse response = queryService.getByBusinessKey(businessKey);
    return response == null
        ? ApiResponse.fail(ResultCode.NOT_FOUND.getCode(), "预占记录不存在")
        : ApiResponse.success(response);
  }
}
