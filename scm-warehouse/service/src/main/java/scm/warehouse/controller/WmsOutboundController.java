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
import scm.warehouse.domain.entity.WmsOutbound;
import scm.warehouse.service.IWmsOutboundService;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/wms-outbound")
@Tag(name = "出库管理", description = "出库单增删改查及出库接口")
public class WmsOutboundController {

    @Autowired
    private IWmsOutboundService outboundService;

    @PostMapping
    @Operation(summary = "创建出库单")
    public ApiResponse<WmsOutbound> create(@RequestBody WmsOutbound outbound) {
        log.info("[API] 创建出库单: warehouseId={}, type={}", outbound.getWarehouseId(), outbound.getOutboundType());

        outbound.setId(UUIDv7Util.generateString());
        outbound.setOutboundNo("OUT" + System.currentTimeMillis());
        outbound.setStatus(0); // 0-待拣货
        outbound.setPickedQuantity(0);
        outbound.setDeleted(false);
        outbound.setCreateTime(LocalDateTime.now());
        outbound.setUpdateTime(LocalDateTime.now());

        outboundService.save(outbound);
        log.info("[API] 出库单创建成功: id={}, outboundNo={}", outbound.getId(), outbound.getOutboundNo());
        return ApiResponse.success(outbound);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新出库单")
    public ApiResponse<WmsOutbound> update(@PathVariable String id, @RequestBody WmsOutbound outbound) {
        log.info("[API] 更新出库单: id={}", id);

        WmsOutbound existing = outboundService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "出库单不存在");
        }
        if (existing.getStatus() != 0) {
            return ApiResponse.fail(400, "只有待拣货状态的出库单才能修改");
        }

        outbound.setId(id);
        outbound.setUpdateTime(LocalDateTime.now());
        outboundService.updateById(outbound);
        return ApiResponse.success(outboundService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除出库单（逻辑删除）")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除出库单: id={}", id);

        WmsOutbound existing = outboundService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "出库单不存在");
        }

        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        outboundService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询出库单详情")
    public ApiResponse<WmsOutbound> getById(@PathVariable String id) {
        WmsOutbound outbound = outboundService.getById(id);
        if (outbound == null || Boolean.TRUE.equals(outbound.getDeleted())) {
            return ApiResponse.fail(404, "出库单不存在");
        }
        return ApiResponse.success(outbound);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询出库单")
    public ApiResponse<Page<WmsOutbound>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer outboundType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(outboundService.pageList(page, size, warehouseId, outboundType, status));
    }

    @PutMapping("/{id}/ship")
    @Operation(summary = "出库确认")
    public ApiResponse<Void> ship(
            @PathVariable String id,
            @Parameter(description = "操作人ID", required = true)
            @RequestParam String operatorId,
            @Parameter(description = "操作人姓名", required = true)
            @RequestParam String operatorName) {
        log.info("[API] 出库确认: id={}, operator={}", id, operatorName);

        try {
            boolean success = outboundService.ship(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "出库失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消出库单")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @Parameter(description = "操作人ID", required = true)
            @RequestParam String operatorId,
            @Parameter(description = "操作人姓名", required = true)
            @RequestParam String operatorName) {
        log.info("[API] 取消出库单: id={}, operator={}", id, operatorName);

        try {
            boolean success = outboundService.cancel(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
