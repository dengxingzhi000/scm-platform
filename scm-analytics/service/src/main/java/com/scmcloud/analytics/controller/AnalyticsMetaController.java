package com.scmcloud.analytics.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.analytics.api.dto.DatasetDTO;
import com.scmcloud.analytics.api.dto.DatasetFieldDTO;
import com.scmcloud.analytics.api.dto.DimensionDTO;
import com.scmcloud.analytics.api.dto.MetricDTO;
import com.scmcloud.analytics.domain.entity.DatasetDefinition;
import com.scmcloud.analytics.domain.entity.DatasetFieldDefinition;
import com.scmcloud.analytics.domain.entity.DimensionDefinition;
import com.scmcloud.analytics.domain.entity.MetricDefinition;
import com.scmcloud.analytics.service.DatasetFieldService;
import com.scmcloud.analytics.service.DatasetService;
import com.scmcloud.analytics.service.DimensionService;
import com.scmcloud.analytics.service.MetricService;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Analytics metadata REST controller.
 *
 * <p>Exposes CRUD endpoints for metric / dimension / dataset / dataset-field definitions.
 * All endpoints are tenant-scoped via {@code TenantContextHolder}; the {@code @Master}
 * / {@code @Slave} annotations on the underlying services route reads to replica and
 * writes to master.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsMetaController {

    private final MetricService metricService;
    private final DimensionService dimensionService;
    private final DatasetService datasetService;
    private final DatasetFieldService datasetFieldService;

    // ──────────────────────── Metrics ────────────────────────────────

    @PostMapping("/metrics")
    public ApiResponse<MetricDefinition> createMetric(@RequestBody MetricDTO dto) {
        log.info("[API] create metric: code={}, type={}", dto.getMetricCode(), dto.getMetricType());
        return ApiResponse.success(metricService.create(dto));
    }

    @PutMapping("/metrics/{id}")
    public ApiResponse<MetricDefinition> updateMetric(@PathVariable UUID id,
                                                      @RequestBody MetricDTO dto) {
        log.info("[API] update metric: id={}, code={}", id, dto.getMetricCode());
        return ApiResponse.success(metricService.update(id, dto));
    }

    @DeleteMapping("/metrics/{id}")
    public ApiResponse<Void> deleteMetric(@PathVariable UUID id) {
        log.info("[API] delete metric: id={}", id);
        metricService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/metrics/{id}")
    public ApiResponse<MetricDefinition> getMetric(@PathVariable UUID id) {
        return ApiResponse.success(metricService.getById(id));
    }

    @GetMapping("/metrics/code/{code}")
    public ApiResponse<MetricDefinition> getMetricByCode(@PathVariable String code) {
        return ApiResponse.success(metricService.getByCode(code));
    }

    @GetMapping("/metrics")
    public ApiResponse<Page<MetricDefinition>> listMetrics(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(metricService.list(page, size, keyword));
    }

    // ──────────────────────── Dimensions ─────────────────────────────

    @PostMapping("/dimensions")
    public ApiResponse<DimensionDefinition> createDimension(@RequestBody DimensionDTO dto) {
        log.info("[API] create dimension: code={}, type={}", dto.getDimCode(), dto.getDimType());
        return ApiResponse.success(dimensionService.create(dto));
    }

    @PutMapping("/dimensions/{id}")
    public ApiResponse<DimensionDefinition> updateDimension(@PathVariable UUID id,
                                                            @RequestBody DimensionDTO dto) {
        log.info("[API] update dimension: id={}, code={}", id, dto.getDimCode());
        return ApiResponse.success(dimensionService.update(id, dto));
    }

    @DeleteMapping("/dimensions/{id}")
    public ApiResponse<Void> deleteDimension(@PathVariable UUID id) {
        log.info("[API] delete dimension: id={}", id);
        dimensionService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/dimensions/{id}")
    public ApiResponse<DimensionDefinition> getDimension(@PathVariable UUID id) {
        return ApiResponse.success(dimensionService.getById(id));
    }

    @GetMapping("/dimensions/code/{code}")
    public ApiResponse<DimensionDefinition> getDimensionByCode(@PathVariable String code) {
        return ApiResponse.success(dimensionService.getByCode(code));
    }

    @GetMapping("/dimensions")
    public ApiResponse<Page<DimensionDefinition>> listDimensions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(dimensionService.list(page, size, keyword));
    }

    // ──────────────────────── Datasets ───────────────────────────────

    @PostMapping("/datasets")
    public ApiResponse<DatasetDefinition> createDataset(@RequestBody DatasetDTO dto) {
        log.info("[API] create dataset: code={}, type={}", dto.getDatasetCode(), dto.getDatasetType());
        return ApiResponse.success(datasetService.create(dto));
    }

    @PutMapping("/datasets/{id}")
    public ApiResponse<DatasetDefinition> updateDataset(@PathVariable UUID id,
                                                        @RequestBody DatasetDTO dto) {
        log.info("[API] update dataset: id={}, code={}", id, dto.getDatasetCode());
        return ApiResponse.success(datasetService.update(id, dto));
    }

    @DeleteMapping("/datasets/{id}")
    public ApiResponse<Void> deleteDataset(@PathVariable UUID id) {
        log.info("[API] delete dataset: id={}", id);
        datasetService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/datasets/{id}")
    public ApiResponse<DatasetDefinition> getDataset(@PathVariable UUID id) {
        return ApiResponse.success(datasetService.getById(id));
    }

    @GetMapping("/datasets/code/{code}")
    public ApiResponse<DatasetDefinition> getDatasetByCode(@PathVariable String code) {
        return ApiResponse.success(datasetService.getByCode(code));
    }

    @GetMapping("/datasets")
    public ApiResponse<Page<DatasetDefinition>> listDatasets(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(datasetService.list(page, size, keyword));
    }

    // ──────────────────────── Dataset Fields ─────────────────────────

    @PostMapping("/datasets/{datasetId}/fields")
    public ApiResponse<DatasetFieldDefinition> createField(@PathVariable UUID datasetId,
                                                            @RequestBody DatasetFieldDTO dto) {
        log.info("[API] create dataset field: dataset={}, code={}", datasetId, dto.getFieldCode());
        return ApiResponse.success(datasetFieldService.create(dto));
    }

    @PutMapping("/datasets/{datasetId}/fields/{id}")
    public ApiResponse<DatasetFieldDefinition> updateField(@PathVariable UUID datasetId,
                                                            @PathVariable UUID id,
                                                            @RequestBody DatasetFieldDTO dto) {
        log.info("[API] update dataset field: id={}, code={}", id, dto.getFieldCode());
        return ApiResponse.success(datasetFieldService.update(id, dto));
    }

    @DeleteMapping("/datasets/{datasetId}/fields/{id}")
    public ApiResponse<Void> deleteField(@PathVariable UUID datasetId,
                                          @PathVariable UUID id) {
        log.info("[API] delete dataset field: id={}", id);
        datasetFieldService.delete(id);
        return ApiResponse.success();
    }

    @GetMapping("/datasets/{datasetId}/fields/{id}")
    public ApiResponse<DatasetFieldDefinition> getField(@PathVariable UUID datasetId,
                                                         @PathVariable UUID id) {
        return ApiResponse.success(datasetFieldService.getById(id));
    }

    @GetMapping("/datasets/{datasetId}/fields")
    public ApiResponse<List<DatasetFieldDefinition>> listFields(@PathVariable UUID datasetId) {
        return ApiResponse.success(datasetFieldService.listByDataset(datasetId));
    }
}