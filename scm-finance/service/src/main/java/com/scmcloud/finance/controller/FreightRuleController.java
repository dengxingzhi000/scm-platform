package com.scmcloud.finance.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.finance.domain.entity.FreightRule;
import com.scmcloud.finance.service.IFreightRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/freight-rule")
@Tag(name = "运费规则", description = "管理运费规则的配置、查询与运费试算。")
public class FreightRuleController {
    private final IFreightRuleService freightRuleService;

    @Operation(summary = "查询运费规则详情", description = "根据规则 ID 查询运费规则的完整配置信息。")
    @GetMapping("/{id}")
    public ApiResponse<FreightRule> getById(@Parameter(description = "运费规则 ID") @PathVariable String id) {
        FreightRule rule = freightRuleService.getById(id);
        return ApiResponse.success(rule);
    }

    @Operation(summary = "新增运费规则", description = "创建一条运费规则，记录创建时间与更新时间后保存。")
    @PostMapping
    public ApiResponse<FreightRule> create(@RequestBody FreightRule rule) {
        rule.setDeleted(false);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        freightRuleService.save(rule);
        log.info("运费规则创建成功: id={}", rule.getId());
        return ApiResponse.success(rule);
    }

    @Operation(summary = "更新运费规则", description = "按规则 ID 覆盖更新运费规则的配置内容并刷新更新时间。")
    @PutMapping("/{id}")
    public ApiResponse<FreightRule> update(@Parameter(description = "运费规则 ID") @PathVariable String id, @RequestBody FreightRule rule) {
        rule.setId(id);
        rule.setUpdateTime(LocalDateTime.now());
        freightRuleService.updateById(rule);
        log.info("运费规则更新成功: id={}", id);
        return ApiResponse.success(rule);
    }

    @Operation(summary = "删除运费规则", description = "按规则 ID 对运费规则执行逻辑删除。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "运费规则 ID") @PathVariable String id) {
        FreightRule rule = freightRuleService.getById(id);
        if (rule != null) {
            rule.setDeleted(true);
            rule.setUpdateTime(LocalDateTime.now());
            freightRuleService.updateById(rule);
            log.info("运费规则删除成功: id={}", id);
        }
        return ApiResponse.success();
    }

    @Operation(summary = "查询启用的运费规则", description = "获取当前所有处于启用状态的运费规则列表。")
    @GetMapping("/active")
    public ApiResponse<List<FreightRule>> listActiveRules() {
        List<FreightRule> rules = freightRuleService.listActiveRules();
        return ApiResponse.success(rules);
    }

    @Operation(summary = "试算运费", description = "按指定运费规则与重量、件数、体积、订单金额试算应付运费。")
    @GetMapping("/calculate")
    public ApiResponse<BigDecimal> calculateFreight(
            @Parameter(description = "运费规则 ID") @RequestParam String ruleId,
            @Parameter(description = "货物重量，用于按重量计费") @RequestParam(required = false) BigDecimal weight,
            @Parameter(description = "货物件数，用于按件数计费") @RequestParam(required = false) Integer quantity,
            @Parameter(description = "货物体积，用于按体积计费") @RequestParam(required = false) BigDecimal volume,
            @Parameter(description = "订单金额，用于判断是否达到包邮门槛") @RequestParam(required = false) BigDecimal orderAmount) {
        BigDecimal freight = freightRuleService.calculateFreight(ruleId, weight, quantity, volume, orderAmount);
        return ApiResponse.success(freight);
    }
}
