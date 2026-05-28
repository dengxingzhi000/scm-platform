package scm.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.response.ApiResponse;
import com.frog.common.util.UUIDv7Util;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.warehouse.domain.entity.WmsWavePicking;
import scm.warehouse.service.IWmsWavePickingService;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/wms-wave-picking")
@Tag(name = "波次拣货管理", description = "波次拣货单增删改查及拣货操作接口")
public class WmsWavePickingController {

    @Autowired
    private IWmsWavePickingService wavePickingService;

    @PostMapping
    @Operation(summary = "创建波次拣货单")
    public ApiResponse<WmsWavePicking> create(@RequestBody WmsWavePicking wave) {
        log.info("[API] 创建波次拣货单: warehouseId={}, orderCount={}", wave.getWarehouseId(), wave.getOrderCount());

        wave.setId(UUIDv7Util.generateString());
        wave.setWaveNo("WAVE" + System.currentTimeMillis());
        wave.setStatus(0); // 0-待拣货
        wave.setCreateTime(LocalDateTime.now());
        wave.setUpdateTime(LocalDateTime.now());

        wavePickingService.save(wave);
        log.info("[API] 波次拣货单创建成功: id={}, waveNo={}", wave.getId(), wave.getWaveNo());
        return ApiResponse.success(wave);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新波次拣货单")
    public ApiResponse<WmsWavePicking> update(@PathVariable String id, @RequestBody WmsWavePicking wave) {
        log.info("[API] 更新波次拣货单: id={}", id);

        WmsWavePicking existing = wavePickingService.getById(id);
        if (existing == null) {
            return ApiResponse.fail(404, "波次拣货单不存在");
        }
        if (existing.getStatus() != 0) {
            return ApiResponse.fail(400, "只有待拣货状态的波次拣货单才能修改");
        }

        wave.setId(id);
        wave.setUpdateTime(LocalDateTime.now());
        wavePickingService.updateById(wave);
        return ApiResponse.success(wavePickingService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除波次拣货单")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除波次拣货单: id={}", id);

        WmsWavePicking existing = wavePickingService.getById(id);
        if (existing == null) {
            return ApiResponse.fail(404, "波次拣货单不存在");
        }

        wavePickingService.removeById(id);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询波次拣货单详情")
    public ApiResponse<WmsWavePicking> getById(@PathVariable String id) {
        WmsWavePicking wave = wavePickingService.getById(id);
        if (wave == null) {
            return ApiResponse.fail(404, "波次拣货单不存在");
        }
        return ApiResponse.success(wave);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询波次拣货单")
    public ApiResponse<Page<WmsWavePicking>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(wavePickingService.pageList(page, size, warehouseId, status));
    }

    @PutMapping("/{id}/start")
    @Operation(summary = "开始拣货")
    public ApiResponse<Void> start(
            @PathVariable String id,
            @Parameter(description = "拣货人ID", required = true)
            @RequestParam String pickerId,
            @Parameter(description = "拣货人姓名", required = true)
            @RequestParam String pickerName) {
        log.info("[API] 开始拣货: id={}, picker={}", id, pickerName);

        try {
            boolean success = wavePickingService.start(id, pickerId, pickerName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "开始拣货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "完成拣货")
    public ApiResponse<Void> complete(
            @PathVariable String id,
            @Parameter(description = "操作人ID", required = true)
            @RequestParam String operatorId) {
        log.info("[API] 完成拣货: id={}", id);

        try {
            boolean success = wavePickingService.complete(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "完成拣货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消波次拣货单")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @Parameter(description = "操作人ID", required = true)
            @RequestParam String operatorId) {
        log.info("[API] 取消波次拣货单: id={}", id);

        try {
            boolean success = wavePickingService.cancel(id, operatorId);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
