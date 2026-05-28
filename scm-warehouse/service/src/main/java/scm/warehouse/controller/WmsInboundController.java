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
import scm.warehouse.domain.entity.WmsInbound;
import scm.warehouse.service.IWmsInboundService;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/wms-inbound")
@Tag(name = "入库管理", description = "入库单增删改查及收货接口")
public class WmsInboundController {

    @Autowired
    private IWmsInboundService inboundService;

    @PostMapping
    @Operation(summary = "创建入库单")
    public ApiResponse<WmsInbound> create(@RequestBody WmsInbound inbound) {
        log.info("[API] 创建入库单: warehouseId={}, type={}", inbound.getWarehouseId(), inbound.getInboundType());

        inbound.setId(UUIDv7Util.generateString());
        inbound.setInboundNo("IN" + System.currentTimeMillis());
        inbound.setStatus(0); // 0-待入库
        inbound.setReceivedQuantity(0);
        inbound.setDeleted(false);
        inbound.setCreateTime(LocalDateTime.now());
        inbound.setUpdateTime(LocalDateTime.now());

        inboundService.save(inbound);
        log.info("[API] 入库单创建成功: id={}, inboundNo={}", inbound.getId(), inbound.getInboundNo());
        return ApiResponse.success(inbound);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新入库单")
    public ApiResponse<WmsInbound> update(@PathVariable String id, @RequestBody WmsInbound inbound) {
        log.info("[API] 更新入库单: id={}", id);

        WmsInbound existing = inboundService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "入库单不存在");
        }
        if (existing.getStatus() != 0) {
            return ApiResponse.fail(400, "只有待入库状态的入库单才能修改");
        }

        inbound.setId(id);
        inbound.setUpdateTime(LocalDateTime.now());
        inboundService.updateById(inbound);
        return ApiResponse.success(inboundService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除入库单（逻辑删除）")
    public ApiResponse<Void> delete(@PathVariable String id) {
        log.info("[API] 删除入库单: id={}", id);

        WmsInbound existing = inboundService.getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return ApiResponse.fail(404, "入库单不存在");
        }

        existing.setDeleted(true);
        existing.setUpdateTime(LocalDateTime.now());
        inboundService.updateById(existing);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询入库单详情")
    public ApiResponse<WmsInbound> getById(@PathVariable String id) {
        WmsInbound inbound = inboundService.getById(id);
        if (inbound == null || Boolean.TRUE.equals(inbound.getDeleted())) {
            return ApiResponse.fail(404, "入库单不存在");
        }
        return ApiResponse.success(inbound);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询入库单")
    public ApiResponse<Page<WmsInbound>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) Integer inboundType,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(inboundService.pageList(page, size, warehouseId, inboundType, status));
    }

    @PutMapping("/{id}/receive")
    @Operation(summary = "入库收货")
    public ApiResponse<Void> receive(
            @PathVariable String id,
            @Parameter(description = "操作人ID", required = true)
            @RequestParam String operatorId,
            @Parameter(description = "操作人姓名", required = true)
            @RequestParam String operatorName) {
        log.info("[API] 入库收货: id={}, operator={}", id, operatorName);

        try {
            boolean success = inboundService.receive(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "收货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "取消入库单")
    public ApiResponse<Void> cancel(
            @PathVariable String id,
            @Parameter(description = "操作人ID", required = true)
            @RequestParam String operatorId,
            @Parameter(description = "操作人姓名", required = true)
            @RequestParam String operatorName) {
        log.info("[API] 取消入库单: id={}, operator={}", id, operatorName);

        try {
            boolean success = inboundService.cancel(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
