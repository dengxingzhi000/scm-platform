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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "分析元数据", description = "指标、维度、数据集及数据集字段等分析元数据的维护接口。")
public class AnalyticsMetaController {

    private final MetricService metricService;
    private final DimensionService dimensionService;
    private final DatasetService datasetService;
    private final DatasetFieldService datasetFieldService;

    // ──────────────────────── Metrics ────────────────────────────────

    @Operation(summary = "创建指标", description = "新增一个指标定义，返回创建后的指标信息。")
    @PostMapping("/metrics")
    public ApiResponse<MetricDefinition> createMetric(@RequestBody MetricDTO dto) {
        log.info("[API] create metric: code={}, type={}", (dto.getMetricCode() + "").replace("\r", "_").replace("\n", "_"), (dto.getMetricType() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(metricService.create(dto));
    }

    @Operation(summary = "更新指标", description = "根据指标 ID 更新指标定义的编码、类型与计算口径等信息。")
    @PutMapping("/metrics/{id}")
    public ApiResponse<MetricDefinition> updateMetric(@Parameter(description = "指标 ID") @PathVariable UUID id,
                                                      @RequestBody MetricDTO dto) {
        log.info("[API] update metric: id={}, code={}", (id + "").replace("\r", "_").replace("\n", "_"), (dto.getMetricCode() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(metricService.update(id, dto));
    }

    @Operation(summary = "删除指标", description = "根据指标 ID 删除指定的指标定义。")
    @DeleteMapping("/metrics/{id}")
    public ApiResponse<Void> deleteMetric(@Parameter(description = "指标 ID") @PathVariable UUID id) {
        log.info("[API] delete metric: id={}", id);
        metricService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "查询指标详情", description = "根据指标 ID 查询指标定义的详细信息。")
    @GetMapping("/metrics/{id}")
    public ApiResponse<MetricDefinition> getMetric(@Parameter(description = "指标 ID") @PathVariable UUID id) {
        return ApiResponse.success(metricService.getById(id));
    }

    @Operation(summary = "按编码查询指标", description = "根据指标编码查询对应的指标定义。")
    @GetMapping("/metrics/code/{code}")
    public ApiResponse<MetricDefinition> getMetricByCode(@Parameter(description = "指标编码") @PathVariable String code) {
        return ApiResponse.success(metricService.getByCode(code));
    }

    @Operation(summary = "分页查询指标", description = "按关键字分页查询当前租户下的指标定义列表。")
    @GetMapping("/metrics")
    public ApiResponse<Page<MetricDefinition>> listMetrics(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键字，按指标编码或名称模糊匹配") @RequestParam(required = false) String keyword) {
        return ApiResponse.success(metricService.list(page, size, keyword));
    }

    // ──────────────────────── Dimensions ─────────────────────────────

    @Operation(summary = "创建维度", description = "新增一个维度定义，返回创建后的维度信息。")
    @PostMapping("/dimensions")
    public ApiResponse<DimensionDefinition> createDimension(@RequestBody DimensionDTO dto) {
        log.info("[API] create dimension: code={}, type={}", (dto.getDimCode() + "").replace("\r", "_").replace("\n", "_"), (dto.getDimType() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(dimensionService.create(dto));
    }

    @Operation(summary = "更新维度", description = "根据维度 ID 更新维度定义的类型与取值来源等信息。")
    @PutMapping("/dimensions/{id}")
    public ApiResponse<DimensionDefinition> updateDimension(@Parameter(description = "维度 ID") @PathVariable UUID id,
                                                            @RequestBody DimensionDTO dto) {
        log.info("[API] update dimension: id={}, code={}", (id + "").replace("\r", "_").replace("\n", "_"), (dto.getDimCode() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(dimensionService.update(id, dto));
    }

    @Operation(summary = "删除维度", description = "根据维度 ID 删除指定的维度定义。")
    @DeleteMapping("/dimensions/{id}")
    public ApiResponse<Void> deleteDimension(@Parameter(description = "维度 ID") @PathVariable UUID id) {
        log.info("[API] delete dimension: id={}", id);
        dimensionService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "查询维度详情", description = "根据维度 ID 查询维度定义的详细信息。")
    @GetMapping("/dimensions/{id}")
    public ApiResponse<DimensionDefinition> getDimension(@Parameter(description = "维度 ID") @PathVariable UUID id) {
        return ApiResponse.success(dimensionService.getById(id));
    }

    @Operation(summary = "按编码查询维度", description = "根据维度编码查询对应的维度定义。")
    @GetMapping("/dimensions/code/{code}")
    public ApiResponse<DimensionDefinition> getDimensionByCode(@Parameter(description = "维度编码") @PathVariable String code) {
        return ApiResponse.success(dimensionService.getByCode(code));
    }

    @Operation(summary = "分页查询维度", description = "按关键字分页查询当前租户下的维度定义列表。")
    @GetMapping("/dimensions")
    public ApiResponse<Page<DimensionDefinition>> listDimensions(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键字，按维度编码或名称模糊匹配") @RequestParam(required = false) String keyword) {
        return ApiResponse.success(dimensionService.list(page, size, keyword));
    }

    // ──────────────────────── Datasets ───────────────────────────────

    @Operation(summary = "创建数据集", description = "新增一个数据集定义，返回创建后的数据集信息。")
    @PostMapping("/datasets")
    public ApiResponse<DatasetDefinition> createDataset(@RequestBody DatasetDTO dto) {
        log.info("[API] create dataset: code={}, type={}", (dto.getDatasetCode() + "").replace("\r", "_").replace("\n", "_"), (dto.getDatasetType() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(datasetService.create(dto));
    }

    @Operation(summary = "更新数据集", description = "根据数据集 ID 更新数据集定义的类型与来源配置。")
    @PutMapping("/datasets/{id}")
    public ApiResponse<DatasetDefinition> updateDataset(@Parameter(description = "数据集 ID") @PathVariable UUID id,
                                                        @RequestBody DatasetDTO dto) {
        log.info("[API] update dataset: id={}, code={}", (id + "").replace("\r", "_").replace("\n", "_"), (dto.getDatasetCode() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(datasetService.update(id, dto));
    }

    @Operation(summary = "删除数据集", description = "根据数据集 ID 删除指定的数据集定义。")
    @DeleteMapping("/datasets/{id}")
    public ApiResponse<Void> deleteDataset(@Parameter(description = "数据集 ID") @PathVariable UUID id) {
        log.info("[API] delete dataset: id={}", id);
        datasetService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "查询数据集详情", description = "根据数据集 ID 查询数据集定义的详细信息。")
    @GetMapping("/datasets/{id}")
    public ApiResponse<DatasetDefinition> getDataset(@Parameter(description = "数据集 ID") @PathVariable UUID id) {
        return ApiResponse.success(datasetService.getById(id));
    }

    @Operation(summary = "按编码查询数据集", description = "根据数据集编码查询对应的数据集定义。")
    @GetMapping("/datasets/code/{code}")
    public ApiResponse<DatasetDefinition> getDatasetByCode(@Parameter(description = "数据集编码") @PathVariable String code) {
        return ApiResponse.success(datasetService.getByCode(code));
    }

    @Operation(summary = "分页查询数据集", description = "按关键字分页查询当前租户下的数据集定义列表。")
    @GetMapping("/datasets")
    public ApiResponse<Page<DatasetDefinition>> listDatasets(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页记录数") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "关键字，按数据集编码或名称模糊匹配") @RequestParam(required = false) String keyword) {
        return ApiResponse.success(datasetService.list(page, size, keyword));
    }

    // ──────────────────────── Dataset Fields ─────────────────────────

    @Operation(summary = "创建数据集字段", description = "为指定数据集新增一个字段定义，返回创建后的字段信息。")
    @PostMapping("/datasets/{datasetId}/fields")
    public ApiResponse<DatasetFieldDefinition> createField(@Parameter(description = "数据集 ID") @PathVariable UUID datasetId,
                                                            @RequestBody DatasetFieldDTO dto) {
        log.info("[API] create dataset field: dataset={}, code={}", (datasetId + "").replace("\r", "_").replace("\n", "_"), (dto.getFieldCode() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(datasetFieldService.create(dto));
    }

    @Operation(summary = "更新数据集字段", description = "根据字段 ID 更新指定数据集下的字段定义。")
    @PutMapping("/datasets/{datasetId}/fields/{id}")
    public ApiResponse<DatasetFieldDefinition> updateField(@Parameter(description = "字段 ID") @PathVariable UUID id,
                                                            @RequestBody DatasetFieldDTO dto) {
        log.info("[API] update dataset field: id={}, code={}", id, dto.getFieldCode());
        return ApiResponse.success(datasetFieldService.update(id, dto));
    }

    @Operation(summary = "删除数据集字段", description = "根据字段 ID 删除指定数据集下的字段定义。")
    @DeleteMapping("/datasets/{datasetId}/fields/{id}")
    public ApiResponse<Void> deleteField(@Parameter(description = "字段 ID") @PathVariable UUID id) {
        log.info("[API] delete dataset field: id={}", id);
        datasetFieldService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "查询数据集字段详情", description = "根据字段 ID 查询指定数据集下字段定义的详细信息。")
    @GetMapping("/datasets/{datasetId}/fields/{id}")
    public ApiResponse<DatasetFieldDefinition> getField(@Parameter(description = "字段 ID") @PathVariable UUID id) {
        return ApiResponse.success(datasetFieldService.getById(id));
    }

    @Operation(summary = "查询数据集字段列表", description = "查询指定数据集下登记的全部字段定义。")
    @GetMapping("/datasets/{datasetId}/fields")
    public ApiResponse<List<DatasetFieldDefinition>> listFields(@Parameter(description = "数据集 ID") @PathVariable UUID datasetId) {
        return ApiResponse.success(datasetFieldService.listByDataset(datasetId));
    }
}