package com.scmcloud.system.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.decision.rule.DecisionRule;
import com.scmcloud.decision.rule.RuleConflictDetector;
import com.scmcloud.system.service.DecisionRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/decision/rules")
public class DecisionRuleController {

    private final DecisionRuleService ruleService;

    @GetMapping("/{engineType}")
    public ApiResponse<List<DecisionRule>> list(@PathVariable String engineType) {
        return ApiResponse.success(ruleService.findByEngineType(engineType));
    }

    @PostMapping
    public ApiResponse<DecisionRule> create(@RequestBody DecisionRule rule) {
        return ApiResponse.success(ruleService.create(rule));
    }

    @PutMapping("/{id}")
    public ApiResponse<DecisionRule> update(@PathVariable String id, @RequestBody DecisionRule rule) {
        return ApiResponse.success(ruleService.update(id, rule));
    }

    @PutMapping("/{id}/toggle")
    public ApiResponse<Void> toggle(@PathVariable String id, @RequestParam boolean enabled) {
        ruleService.toggle(id, enabled);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        ruleService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/conflicts/{engineType}")
    public ApiResponse<List<RuleConflictDetector.Conflict>> detectConflicts(@PathVariable String engineType) {
        return ApiResponse.success(ruleService.detectConflicts(engineType));
    }
}
