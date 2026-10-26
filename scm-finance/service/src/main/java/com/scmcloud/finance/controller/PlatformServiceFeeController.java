package com.scmcloud.finance.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.finance.domain.entity.PlatformServiceFee;
import com.scmcloud.finance.service.IPlatformServiceFeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/platform-service-fee")
@Tag(name = "平台服务费", description = "管理 SaaS 平台服务费账单的生成、查询与付款登记。")
public class PlatformServiceFeeController {
    private final IPlatformServiceFeeService platformServiceFeeService;

    @Operation(summary = "查询服务费详情", description = "根据账单 ID 查询平台服务费的完整计费信息。")
    @GetMapping("/{id}")
    public ApiResponse<PlatformServiceFee> getById(@Parameter(description = "平台服务费账单 ID") @PathVariable String id) {
        PlatformServiceFee fee = platformServiceFeeService.getById(id);
        return ApiResponse.success(fee);
    }

    @Operation(summary = "创建服务费账单", description = "新增平台服务费账单，生成主键并将状态置为待支付。")
    @PostMapping
    public ApiResponse<PlatformServiceFee> create(@RequestBody PlatformServiceFee fee) {
        fee.setId(UUIDv7Util.generateString());
        fee.setStatus(0);
        fee.setCreateTime(LocalDateTime.now());
        fee.setUpdateTime(LocalDateTime.now());
        platformServiceFeeService.save(fee);
        log.info("Platform service fee created successfully: id={}, feeType={}", fee.getId(), fee.getFeeType());
        return ApiResponse.success(fee);
    }

    @Operation(summary = "更新服务费账单", description = "按账单 ID 更新平台服务费账单信息并刷新更新时间。")
    @PutMapping("/{id}")
    public ApiResponse<PlatformServiceFee> update(@Parameter(description = "平台服务费账单 ID") @PathVariable String id, @RequestBody PlatformServiceFee fee) {
        fee.setId(id);
        fee.setUpdateTime(LocalDateTime.now());
        platformServiceFeeService.updateById(fee);
        log.info("Platform service fee updated successfully: id={}", id);
        return ApiResponse.success(fee);
    }

    @Operation(summary = "删除服务费账单", description = "按账单 ID 删除平台服务费账单。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "平台服务费账单 ID") @PathVariable String id) {
        platformServiceFeeService.removeById(id);
        log.info("Platform service fee deleted successfully: id={}", id);
        return ApiResponse.success();
    }

    @Operation(summary = "查询待支付服务费", description = "获取所有尚未结清的平台服务费账单列表。")
    @GetMapping("/pending")
    public ApiResponse<List<PlatformServiceFee>> listPendingFees() {
        List<PlatformServiceFee> fees = platformServiceFeeService.listPendingFees();
        return ApiResponse.success(fees);
    }

    @Operation(summary = "登记服务费付款", description = "按账单 ID 登记本次实付金额并更新账单的已付金额。")
    @PostMapping("/{id}/pay")
    public ApiResponse<PlatformServiceFee> pay(
            @Parameter(description = "平台服务费账单 ID") @PathVariable String id,
            @Parameter(description = "本次实付金额") @RequestParam BigDecimal paidAmount) {
        PlatformServiceFee fee = platformServiceFeeService.recordPayment(id, paidAmount);
        return ApiResponse.success(fee);
    }
}
