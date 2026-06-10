package com.scmcloud.finance.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.finance.domain.entity.PlatformServiceFee;
import com.scmcloud.finance.service.IPlatformServiceFeeService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/platform-service-fee")
public class PlatformServiceFeeController {

    private final IPlatformServiceFeeService platformServiceFeeService;

    @GetMapping("/{id}")
    public ApiResponse<PlatformServiceFee> getById(@PathVariable String id) {
        PlatformServiceFee fee = platformServiceFeeService.getById(id);
        return ApiResponse.success(fee);
    }

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

    @PutMapping("/{id}")
    public ApiResponse<PlatformServiceFee> update(@PathVariable String id, @RequestBody PlatformServiceFee fee) {
        fee.setId(id);
        fee.setUpdateTime(LocalDateTime.now());
        platformServiceFeeService.updateById(fee);
        log.info("Platform service fee updated successfully: id={}", id);
        return ApiResponse.success(fee);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        platformServiceFeeService.removeById(id);
        log.info("Platform service fee deleted successfully: id={}", id);
        return ApiResponse.success();
    }

    @GetMapping("/pending")
    public ApiResponse<List<PlatformServiceFee>> listPendingFees() {
        List<PlatformServiceFee> fees = platformServiceFeeService.listPendingFees();
        return ApiResponse.success(fees);
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<PlatformServiceFee> pay(
            @PathVariable String id,
            @RequestParam BigDecimal paidAmount) {
        PlatformServiceFee fee = platformServiceFeeService.recordPayment(id, paidAmount);
        return ApiResponse.success(fee);
    }
}
