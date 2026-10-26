package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsWavePicking;
import com.scmcloud.warehouse.service.command.WmsWavePickingCommandService;
import com.scmcloud.warehouse.service.query.WmsWavePickingQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-wave-picking")
@Tag(name = "波次拣货", description = "波次拣货单的创建、查询、拣货执行与取消管理")
public class WmsWavePickingController {

    private final WmsWavePickingCommandService wavePickingCommandService;
    private final WmsWavePickingQueryService wavePickingQueryService;

    @Operation(summary = "创建波次拣货单", description = "按仓库与订单数量新建波次拣货单并生成波次号。")
    @PostMapping
    public ApiResponse<WmsWavePicking> create(@RequestBody WmsWavePicking wave) {
        log.info("[API] 创建波次拣货单 warehouseId={}, orderCount={}", wave.getWarehouseId(), wave.getOrderCount());
        WmsWavePicking created = wavePickingCommandService.create(wave);
        log.info("[API] 波次拣货单创建成功 id={}, waveNo={}", created.getId(), created.getWaveNo());
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新波次拣货单", description = "按主键更新波次拣货单信息，仅未开始拣货的波次允许修改。")
    @PutMapping("/{id}")
    public ApiResponse<WmsWavePicking> update(@Parameter(description = "波次拣货单主键 ID") @PathVariable String id, @RequestBody WmsWavePicking wave) {
        log.info("[API] 更新波次拣货单 id={}", id);
        wave.setId(id);
        try {
            boolean success = wavePickingCommandService.update(wave);
            if (!success) {
                return ApiResponse.fail(404, "波次拣货单不存在");
            }
            return ApiResponse.success(wavePickingQueryService.getById(id));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "删除波次拣货单", description = "按主键删除波次拣货单，删除后不再参与拣货作业。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "波次拣货单主键 ID") @PathVariable String id) {
        log.info("[API] 删除波次拣货单 id={}", id);
        boolean success = wavePickingCommandService.removeById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "波次拣货单不存在");
    }

    @Operation(summary = "查询波次拣货单详情", description = "按主键查询波次拣货单，单据不存在时返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<WmsWavePicking> getById(@Parameter(description = "波次拣货单主键 ID") @PathVariable String id) {
        WmsWavePicking wave = wavePickingQueryService.getById(id);
        if (wave == null) {
            return ApiResponse.fail(404, "波次拣货单不存在");
        }
        return ApiResponse.success(wave);
    }

    @Operation(summary = "分页查询波次拣货单", description = "按仓库与波次状态分页查询波次拣货单列表。")
    @GetMapping("/page")
    public ApiResponse<Page<WmsWavePicking>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "仓库 ID，用于按仓库过滤") @RequestParam(required = false) String warehouseId,
            @Parameter(description = "波次状态编码") @RequestParam(required = false) Integer status) {
        return ApiResponse.success(wavePickingQueryService.pageList(page, size, warehouseId, status));
    }

    @Operation(summary = "开始拣货", description = "指派拣货员并开始执行指定波次拣货单。")
    @PutMapping("/{id}/start")
    public ApiResponse<Void> start(
            @Parameter(description = "波次拣货单主键 ID") @PathVariable String id,
            @Parameter(description = "拣货员用户 ID") @RequestParam String pickerId,
            @Parameter(description = "拣货员姓名") @RequestParam String pickerName) {
        log.info("[API] 开始拣货 id={}, picker={}", id, pickerName);
        try {
            boolean success = wavePickingCommandService.start(id, pickerId, pickerName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "开始拣货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "完成拣货", description = "将指定波次拣货单标记为拣货完成并记录操作人。")
    @PutMapping("/{id}/complete")
    public ApiResponse<Void> complete(
            @Parameter(description = "波次拣货单主键 ID") @PathVariable String id,
            @Parameter(description = "操作人用户 ID") @RequestParam String operatorId) {
        log.info("[API] 完成拣货: id={}", id);
        try {
            boolean success = wavePickingCommandService.complete(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "完成拣货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "取消波次拣货单", description = "取消指定波次拣货单并记录操作人，已完成拣货的波次不允许取消。")
    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @Parameter(description = "波次拣货单主键 ID") @PathVariable String id,
            @Parameter(description = "操作人用户 ID") @RequestParam String operatorId) {
        log.info("[API] 取消波次拣货单 id={}", id);
        try {
            boolean success = wavePickingCommandService.cancel(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
