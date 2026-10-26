package com.scmcloud.system.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.decision.config.WeightProfile;
import com.scmcloud.system.service.DecisionWeightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/decision/weights")
@Tag(name = "决策权重", description = "决策引擎各维度权重方案的查询、创建、启用与删除。")
public class DecisionWeightController {

    private final DecisionWeightService weightService;

    @Operation(summary = "查询生效权重方案", description = "按引擎类型与业务场景查询当前生效的权重方案。")
    @GetMapping("/{engineType}")
    public ApiResponse<List<WeightProfile>> getActive(
            @Parameter(description = "决策引擎类型标识") @PathVariable String engineType,
            @Parameter(description = "业务场景编码，不传时按未指定场景匹配") @RequestParam(required = false) String scene) {
        return ApiResponse.success(weightService.findActive(engineType, scene));
    }

    @Operation(summary = "查询权重历史版本", description = "查询指定引擎类型下全部历史版本的权重方案。")
    @GetMapping("/{engineType}/versions")
    public ApiResponse<List<WeightProfile>> getVersions(@Parameter(description = "决策引擎类型标识") @PathVariable String engineType) {
        return ApiResponse.success(weightService.findAllVersions(engineType));
    }

    @Operation(summary = "新增权重方案", description = "创建新的权重方案，各维度权重之和须为 1。")
    @PostMapping
    public ApiResponse<WeightProfile> create(@RequestBody WeightProfile profile) {
        return ApiResponse.success(weightService.create(profile));
    }

    @Operation(summary = "启用权重方案", description = "将指定的权重方案设为该引擎当前生效的版本。")
    @PutMapping("/{id}/activate")
    public ApiResponse<Void> activate(@Parameter(description = "权重方案 ID") @PathVariable String id) {
        weightService.activate(id);
        return ApiResponse.success();
    }

    @Operation(summary = "删除权重方案", description = "根据方案 ID 删除指定的权重方案。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "权重方案 ID") @PathVariable String id) {
        weightService.delete(id);
        return ApiResponse.success();
    }
}
