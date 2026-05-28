package scm.finance.controller;

import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.PlatformServiceFee;
import scm.finance.service.IPlatformServiceFeeService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/platform-service-fee")
@Tag(name = "平台服务费", description = "平台服务费管理接口")
public class PlatformServiceFeeController {

    @Autowired
    private IPlatformServiceFeeService platformServiceFeeService;

    @GetMapping("/{id}")
    @Operation(summary = "查询平台服务费详情")
    public ApiResponse<PlatformServiceFee> getById(@PathVariable String id) {
        PlatformServiceFee fee = platformServiceFeeService.getById(id);
        return ApiResponse.success(fee);
    }

    @PostMapping
    @Operation(summary = "创建平台服务费")
    public ApiResponse<PlatformServiceFee> create(@RequestBody PlatformServiceFee fee) {
        fee.setId(UUIDv7Util.generateString());
        fee.setStatus(0);
        fee.setCreateTime(LocalDateTime.now());
        fee.setUpdateTime(LocalDateTime.now());
        platformServiceFeeService.save(fee);
        log.info("平台服务费创建成功: id={}, feeType={}", fee.getId(), fee.getFeeType());
        return ApiResponse.success(fee);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新平台服务费")
    public ApiResponse<PlatformServiceFee> update(@PathVariable String id, @RequestBody PlatformServiceFee fee) {
        fee.setId(id);
        fee.setUpdateTime(LocalDateTime.now());
        platformServiceFeeService.updateById(fee);
        log.info("平台服务费更新成功: id={}", id);
        return ApiResponse.success(fee);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除平台服务费")
    public ApiResponse<Void> delete(@PathVariable String id) {
        platformServiceFeeService.removeById(id);
        log.info("平台服务费删除成功: id={}", id);
        return ApiResponse.success();
    }

    @GetMapping("/pending")
    @Operation(summary = "查询待付款平台服务费")
    public ApiResponse<List<PlatformServiceFee>> listPendingFees() {
        List<PlatformServiceFee> fees = platformServiceFeeService.listPendingFees();
        return ApiResponse.success(fees);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "记录平台服务费付款")
    public ApiResponse<PlatformServiceFee> pay(
            @PathVariable String id,
            @Parameter(description = "付款金额", required = true) @RequestParam BigDecimal paidAmount) {
        PlatformServiceFee fee = platformServiceFeeService.recordPayment(id, paidAmount);
        return ApiResponse.success(fee);
    }
}
