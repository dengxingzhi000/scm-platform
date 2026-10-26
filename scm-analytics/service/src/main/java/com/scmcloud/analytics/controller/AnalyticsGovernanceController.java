package com.scmcloud.analytics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.domain.entity.DataQualityResult;
import com.scmcloud.analytics.domain.entity.DataQualityRule;
import com.scmcloud.analytics.domain.entity.LineageEdge;
import com.scmcloud.analytics.service.DataQualityService;
import com.scmcloud.analytics.service.LineageService;
import com.scmcloud.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Lineage and data-quality management endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "分析治理", description = "数据血缘关系与数据质量规则的登记、查询、执行等治理接口。")
public class AnalyticsGovernanceController {

    private final LineageService lineageService;
    private final DataQualityService dataQualityService;

    @Operation(summary = "记录血缘", description = "登记一条数据血缘边，用于建立上游节点与下游节点之间的依赖关系。")
    @PostMapping("/lineage")
    public ApiResponse<LineageEdge> recordLineage(@RequestBody LineageEdge edge) {
        return ApiResponse.success(lineageService.record(edge));
    }

    @Operation(summary = "查询血缘列表", description = "查询当前租户下已登记的全部数据血缘关系。")
    @GetMapping("/lineage")
    public ApiResponse<List<LineageEdge>> listLineage() {
        return ApiResponse.success(lineageService.list());
    }

    @Operation(summary = "查询上游血缘", description = "根据节点标识查询该节点的上游依赖血缘关系。")
    @GetMapping("/lineage/upstream/{node}")
    public ApiResponse<List<LineageEdge>> upstream(@Parameter(description = "血缘节点标识") @PathVariable String node) {
        return ApiResponse.success(lineageService.upstream(node));
    }

    @Operation(summary = "查询下游血缘", description = "根据节点标识查询该节点的下游影响血缘关系。")
    @GetMapping("/lineage/downstream/{node}")
    public ApiResponse<List<LineageEdge>> downstream(@Parameter(description = "血缘节点标识") @PathVariable String node) {
        return ApiResponse.success(lineageService.downstream(node));
    }

    @Operation(summary = "删除血缘关系", description = "根据血缘记录 ID 删除指定的数据血缘关系。")
    @DeleteMapping("/lineage/{id}")
    public ApiResponse<Void> deleteLineage(@Parameter(description = "血缘记录 ID") @PathVariable UUID id) {
        lineageService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "创建质量规则", description = "新增一条数据质量校验规则并返回创建结果。")
    @PostMapping("/quality/rules")
    public ApiResponse<DataQualityRule> createRule(@RequestBody DataQualityRule rule) {
        log.info("[API] create quality rule: code={}", rule.getRuleCode());
        return ApiResponse.success(dataQualityService.create(rule));
    }

    @Operation(summary = "更新质量规则", description = "根据规则 ID 更新数据质量校验规则的配置内容。")
    @PutMapping("/quality/rules/{id}")
    public ApiResponse<DataQualityRule> updateRule(@Parameter(description = "质量规则 ID") @PathVariable UUID id,
                                                   @RequestBody DataQualityRule rule) {
        log.info("[API] update quality rule: id={}", id);
        return ApiResponse.success(dataQualityService.update(id, rule));
    }

    @Operation(summary = "删除质量规则", description = "根据规则 ID 删除指定的数据质量校验规则。")
    @DeleteMapping("/quality/rules/{id}")
    public ApiResponse<Void> deleteRule(@Parameter(description = "质量规则 ID") @PathVariable UUID id) {
        dataQualityService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "查询质量规则详情", description = "根据规则 ID 查询数据质量校验规则的详细信息。")
    @GetMapping("/quality/rules/{id}")
    public ApiResponse<DataQualityRule> getRule(@Parameter(description = "质量规则 ID") @PathVariable UUID id) {
        return ApiResponse.success(dataQualityService.getById(id));
    }

    @Operation(summary = "分页查询质量规则", description = "按规则编码等关键字分页查询数据质量规则列表。")
    @GetMapping("/quality/rules")
    public ApiResponse<Page<DataQualityRule>> listRules(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键字，按规则编码或名称模糊匹配") @RequestParam(required = false) String keyword) {
        return ApiResponse.success(dataQualityService.list(page, size, keyword));
    }

    @Operation(summary = "执行质量规则", description = "立即执行指定的数据质量规则并返回本次校验结果。")
    @PostMapping("/quality/rules/{id}/run")
    public ApiResponse<DataQualityResult> runRule(@Parameter(description = "质量规则 ID") @PathVariable UUID id) {
        log.info("[API] run quality rule: id={}", id);
        return ApiResponse.success(dataQualityService.runRule(id));
    }

    @Operation(summary = "查询规则执行结果", description = "查询指定质量规则的历史执行结果，可按执行日期过滤。")
    @GetMapping("/quality/rules/{id}/results")
    public ApiResponse<List<DataQualityResult>> listResults(
            @Parameter(description = "质量规则 ID") @PathVariable UUID id,
            @Parameter(description = "执行日期，格式 yyyy-MM-dd，不传则返回全部") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(dataQualityService.listResults(id, date));
    }
}
