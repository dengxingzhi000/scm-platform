package com.cognora.platform.tenant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.response.ApiResponse;
import com.cognora.platform.tenant.domain.entity.TenantPlatformFee;
import com.cognora.platform.tenant.service.IPlatformFeeService;
import com.cognora.platform.tenant.service.query.PlatformFeeQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

/**
 * 平台服务费控制器
 *
 * <p>提供月度账单查询与手动触发计算接口。
 *
 * @author SCM Platform Team
 * @since 2026-10-29
 */
@RequiredArgsConstructor
@Slf4j
@Validated
@RestController
@RequestMapping("/tenant-platform-fee")
@Tag(name = "平台服务费", description = "查询租户月度平台服务费账单并支持手动触发计算。")
public class TenantPlatformFeeController {

  private final PlatformFeeQueryService queryService;
  private final IPlatformFeeService platformFeeService;

  @Operation(summary = "分页查询服务费账单", description = "按租户、账期和状态分页查询平台服务费账单。")
  @GetMapping("/page")
  public ApiResponse<Page<TenantPlatformFee>> page(
      @Parameter(description = "页码，默认 1")
      @RequestParam(required = false) @Min(value = 1, message = "页码不能小于1") Integer page,
      @Parameter(description = "每页数量，默认 20")
      @RequestParam(required = false) @Min(value = 1, message = "每页数量不能小于1")
      @Max(value = 200, message = "每页数量不能超过200") Integer size,
      @Parameter(description = "租户 ID") @RequestParam(required = false) String tenantId,
      @Parameter(description = "账期，格式 yyyy-MM") @RequestParam(required = false) String billingPeriod,
      @Parameter(description = "状态：0-待付款，1-已付款，2-已作废") @RequestParam(required = false) Integer status) {
    return ApiResponse.success(
        queryService.page(page == null ? 1 : page, size == null ? 20 : size, tenantId, billingPeriod, status));
  }

  @Operation(summary = "查询租户服务费账单", description = "查询指定租户的全部平台服务费账单，按账期倒序。")
  @GetMapping("/tenant/{tenantId}")
  public ApiResponse<List<TenantPlatformFee>> listByTenant(
      @Parameter(description = "租户 ID") @PathVariable String tenantId) {
    return ApiResponse.success(queryService.listByTenant(tenantId));
  }

  @Operation(summary = "计算平台服务费", description = "按账期计算租户平台服务费，支持指定单个租户；同一账期重复执行会更新未支付账单。")
  @PostMapping("/calculate")
  public ApiResponse<Integer> calculate(
      @Parameter(description = "账期，格式 yyyy-MM，默认上月")
      @RequestParam(required = false) String billingPeriod,
      @Parameter(description = "租户 ID，不传时计算全部租户")
      @RequestParam(required = false) UUID tenantId) {
    YearMonth targetMonth = parseBillingPeriod(billingPeriod);
    return ApiResponse.success(platformFeeService.calculateMonthlyFees(targetMonth, tenantId));
  }

  private YearMonth parseBillingPeriod(String billingPeriod) {
    if (billingPeriod == null || billingPeriod.isBlank()) {
      return YearMonth.now().minusMonths(1);
    }
    try {
      return YearMonth.parse(billingPeriod, DateTimeFormatter.ofPattern("yyyy-MM"));
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("账期格式必须为 yyyy-MM: " + billingPeriod);
    }
  }
}
