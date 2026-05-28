package scm.finance.controller;

import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.FreightRule;
import scm.finance.service.IFreightRuleService;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/freight-rule")
@Tag(name = "运费规则", description = "运费规则管理接口")
public class FreightRuleController {

    @Autowired
    private IFreightRuleService freightRuleService;

    @GetMapping("/{id}")
    @Operation(summary = "查询运费规则详情")
    public ApiResponse<FreightRule> getById(@PathVariable String id) {
        FreightRule rule = freightRuleService.getById(id);
        return ApiResponse.success(rule);
    }

    @PostMapping
    @Operation(summary = "创建运费规则")
    public ApiResponse<FreightRule> create(@RequestBody FreightRule rule) {
        rule.setDeleted(false);
        rule.setCreateTime(java.time.LocalDateTime.now());
        rule.setUpdateTime(java.time.LocalDateTime.now());
        freightRuleService.save(rule);
        log.info("运费规则创建成功: id={}", rule.getId());
        return ApiResponse.success(rule);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新运费规则")
    public ApiResponse<FreightRule> update(@PathVariable String id, @RequestBody FreightRule rule) {
        rule.setId(id);
        rule.setUpdateTime(java.time.LocalDateTime.now());
        freightRuleService.updateById(rule);
        log.info("运费规则更新成功: id={}", id);
        return ApiResponse.success(rule);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除运费规则")
    public ApiResponse<Void> delete(@PathVariable String id) {
        FreightRule rule = freightRuleService.getById(id);
        if (rule != null) {
            rule.setDeleted(true);
            rule.setUpdateTime(java.time.LocalDateTime.now());
            freightRuleService.updateById(rule);
            log.info("运费规则删除成功: id={}", id);
        }
        return ApiResponse.success();
    }

    @GetMapping("/active")
    @Operation(summary = "查询生效中的运费规则")
    public ApiResponse<List<FreightRule>> listActiveRules() {
        List<FreightRule> rules = freightRuleService.listActiveRules();
        return ApiResponse.success(rules);
    }

    @GetMapping("/calculate")
    @Operation(summary = "计算运费")
    public ApiResponse<BigDecimal> calculateFreight(
            @Parameter(description = "规则ID", required = true) @RequestParam String ruleId,
            @Parameter(description = "重量") @RequestParam(required = false) BigDecimal weight,
            @Parameter(description = "件数") @RequestParam(required = false) Integer quantity,
            @Parameter(description = "体积") @RequestParam(required = false) BigDecimal volume,
            @Parameter(description = "订单金额") @RequestParam(required = false) BigDecimal orderAmount) {
        BigDecimal freight = freightRuleService.calculateFreight(ruleId, weight, quantity, volume, orderAmount);
        return ApiResponse.success(freight);
    }
}
