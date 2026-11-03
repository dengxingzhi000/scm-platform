package com.scmcloud.system.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.decision.rule.DecisionRule;
import com.scmcloud.decision.rule.RuleConflictDetector;
import com.scmcloud.system.service.DecisionRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/decision/rules")
@Tag(name = "决策规则", description = "决策引擎规则的查询、新增、修改、启停与规则冲突检测。")
public class DecisionRuleController {

    private final DecisionRuleService ruleService;

    @Operation(summary = "查询规则列表", description = "按决策引擎类型查询该引擎下的全部决策规则。")
    @GetMapping("/{engineType}")
    public ApiResponse<List<DecisionRule>> list(@Parameter(description = "决策引擎类型标识") @PathVariable String engineType) {
        return ApiResponse.success(ruleService.findByEngineType(engineType));
    }

    @Operation(summary = "新增决策规则", description = "创建一条决策规则并返回创建后的规则信息。")
    @PostMapping
    public ApiResponse<DecisionRule> create(@RequestBody DecisionRule rule) {
        return ApiResponse.success(ruleService.create(rule));
    }

    @Operation(summary = "修改决策规则", description = "根据规则 ID 更新规则内容并返回更新后的规则。")
    @PutMapping("/{id}")
    public ApiResponse<DecisionRule> update(@Parameter(description = "决策规则 ID") @PathVariable String id,
                                            @RequestBody DecisionRule rule) {
        return ApiResponse.success(ruleService.update(id, rule));
    }

    @Operation(summary = "启停决策规则", description = "根据规则 ID 开启或关闭该条决策规则。")
    @PutMapping("/{id}/toggle")
    public ApiResponse<Void> toggle(@Parameter(description = "决策规则 ID") @PathVariable String id,
                                    @RequestParam boolean enabled) {
        ruleService.toggle(id, enabled);
        return ApiResponse.success();
    }

    @Operation(summary = "删除决策规则", description = "根据规则 ID 删除指定的决策规则。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "决策规则 ID") @PathVariable String id) {
        ruleService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "检测规则冲突", description = "检测指定引擎类型下全部决策规则之间是否存在条件冲突。")
    @PostMapping("/conflicts/{engineType}")
    public ApiResponse<List<RuleConflictDetector.Conflict>> detectConflicts(
            @Parameter(description = "决策引擎类型标识") @PathVariable String engineType) {
        return ApiResponse.success(ruleService.detectConflicts(engineType));
    }
}
