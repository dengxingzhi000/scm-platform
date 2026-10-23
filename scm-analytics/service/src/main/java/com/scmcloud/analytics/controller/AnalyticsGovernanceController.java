package com.scmcloud.analytics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.domain.entity.DataQualityResult;
import com.scmcloud.analytics.domain.entity.DataQualityRule;
import com.scmcloud.analytics.domain.entity.LineageEdge;
import com.scmcloud.analytics.service.DataQualityService;
import com.scmcloud.analytics.service.LineageService;
import com.scmcloud.common.response.ApiResponse;
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
public class AnalyticsGovernanceController {

    private final LineageService lineageService;
    private final DataQualityService dataQualityService;

    @PostMapping("/lineage")
    public ApiResponse<LineageEdge> recordLineage(@RequestBody LineageEdge edge) {
        return ApiResponse.success(lineageService.record(edge));
    }

    @GetMapping("/lineage")
    public ApiResponse<List<LineageEdge>> listLineage() {
        return ApiResponse.success(lineageService.list());
    }

    @GetMapping("/lineage/upstream/{node}")
    public ApiResponse<List<LineageEdge>> upstream(@PathVariable String node) {
        return ApiResponse.success(lineageService.upstream(node));
    }

    @GetMapping("/lineage/downstream/{node}")
    public ApiResponse<List<LineageEdge>> downstream(@PathVariable String node) {
        return ApiResponse.success(lineageService.downstream(node));
    }

    @DeleteMapping("/lineage/{id}")
    public ApiResponse<Void> deleteLineage(@PathVariable UUID id) {
        lineageService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/quality/rules")
    public ApiResponse<DataQualityRule> createRule(@RequestBody DataQualityRule rule) {
        log.info("[API] create quality rule: code={}", rule.getRuleCode());
        return ApiResponse.success(dataQualityService.create(rule));
    }

    @PutMapping("/quality/rules/{id}")
    public ApiResponse<DataQualityRule> updateRule(@PathVariable UUID id,
                                                   @RequestBody DataQualityRule rule) {
        log.info("[API] update quality rule: id={}", id);
        return ApiResponse.success(dataQualityService.update(id, rule));
    }

    @DeleteMapping("/quality/rules/{id}")
    public ApiResponse<Void> deleteRule(@PathVariable UUID id) {
        dataQualityService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/quality/rules/{id}")
    public ApiResponse<DataQualityRule> getRule(@PathVariable UUID id) {
        return ApiResponse.success(dataQualityService.getById(id));
    }

    @GetMapping("/quality/rules")
    public ApiResponse<Page<DataQualityRule>> listRules(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(dataQualityService.list(page, size, keyword));
    }

    @PostMapping("/quality/rules/{id}/run")
    public ApiResponse<DataQualityResult> runRule(@PathVariable UUID id) {
        log.info("[API] run quality rule: id={}", id);
        return ApiResponse.success(dataQualityService.runRule(id));
    }

    @GetMapping("/quality/rules/{id}/results")
    public ApiResponse<List<DataQualityResult>> listResults(
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.success(dataQualityService.listResults(id, date));
    }
}
