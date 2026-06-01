package scm.finance.controller;

import com.frog.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.finance.domain.entity.FreightRule;
import scm.finance.service.IFreightRuleService;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/freight-rule")
public class FreightRuleController {

    private final IFreightRuleService freightRuleService;

    @GetMapping("/{id}")
    public ApiResponse<FreightRule> getById(@PathVariable String id) {
        FreightRule rule = freightRuleService.getById(id);
        return ApiResponse.success(rule);
    }

    @PostMapping
    public ApiResponse<FreightRule> create(@RequestBody FreightRule rule) {
        rule.setDeleted(false);
        rule.setCreateTime(java.time.LocalDateTime.now());
        rule.setUpdateTime(java.time.LocalDateTime.now());
        freightRuleService.save(rule);
        log.info("运费规则创建成功: id={}", rule.getId());
        return ApiResponse.success(rule);
    }

    @PutMapping("/{id}")
    public ApiResponse<FreightRule> update(@PathVariable String id, @RequestBody FreightRule rule) {
        rule.setId(id);
        rule.setUpdateTime(java.time.LocalDateTime.now());
        freightRuleService.updateById(rule);
        log.info("运费规则更新成功: id={}", id);
        return ApiResponse.success(rule);
    }

    @DeleteMapping("/{id}")
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
    public ApiResponse<List<FreightRule>> listActiveRules() {
        List<FreightRule> rules = freightRuleService.listActiveRules();
        return ApiResponse.success(rules);
    }

    @GetMapping("/calculate")
    public ApiResponse<BigDecimal> calculateFreight(
            @RequestParam String ruleId,
            @RequestParam(required = false) BigDecimal weight,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) BigDecimal volume,
            @RequestParam(required = false) BigDecimal orderAmount) {
        BigDecimal freight = freightRuleService.calculateFreight(ruleId, weight, quantity, volume, orderAmount);
        return ApiResponse.success(freight);
    }
}
