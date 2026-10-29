package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.service.command.WmsOutboundCommandService;
import com.scmcloud.warehouse.service.query.WmsOutboundQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-outbound")
@Tag(name = "出库管理", description = "仓库出库单的创建、查询、发货与取消管理")
public class WmsOutboundController {

    private final WmsOutboundCommandService outboundCommandService;
    private final WmsOutboundQueryService outboundQueryService;

    @Operation(summary = "创建出库单", description = "根据仓库与出库类型新建一张出库单并生成出库单号。")
    @PostMapping
    public ApiResponse<WmsOutbound> create(@RequestBody WmsOutbound outbound) {
        log.info("[API] 创建出库单 warehouseId={}, type={}", (outbound.getWarehouseId() + "").replace("\r", "_").replace("\n", "_"), (outbound.getOutboundType() + "").replace("\r", "_").replace("\n", "_"));
        WmsOutbound created = outboundCommandService.create(outbound);
        log.info("[API] 出库单创建成功 id={}, outboundNo={}", (created.getId() + "").replace("\r", "_").replace("\n", "_"), (created.getOutboundNo() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新出库单", description = "按主键更新出库单信息，仅允许在未发货状态下修改。")
    @PutMapping("/{id}")
    public ApiResponse<WmsOutbound> update(@Parameter(description = "出库单主键 ID") @PathVariable String id, @RequestBody WmsOutbound outbound) {
        log.info("[API] 更新出库单 id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        outbound.setId(id);
        try {
            boolean success = outboundCommandService.update(outbound);
            if (!success) {
                return ApiResponse.fail(404, "出库单不存在");
            }
            return ApiResponse.success(outboundQueryService.getById(id));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "删除出库单", description = "按主键逻辑删除出库单，已删除的单据不再对外可见。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "出库单主键 ID") @PathVariable String id) {
        log.info("[API] 删除出库单 id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        boolean success = outboundCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "出库单不存在");
    }

    @Operation(summary = "查询出库单详情", description = "按主键查询出库单，单据不存在或已删除时返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<WmsOutbound> getById(@Parameter(description = "出库单主键 ID") @PathVariable String id) {
        WmsOutbound outbound = outboundQueryService.getById(id);
        if (outbound == null || Boolean.TRUE.equals(outbound.getDeleted())) {
            return ApiResponse.fail(404, "出库单不存在");
        }
        return ApiResponse.success(outbound);
    }

    @Operation(summary = "分页查询出库单", description = "按仓库、出库类型与单据状态分页查询出库单列表。")
    @GetMapping("/page")
    public ApiResponse<Page<WmsOutbound>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "仓库 ID，用于按仓库过滤") @RequestParam(required = false) String warehouseId,
            @Parameter(description = "出库类型编码，用于按业务类型过滤") @RequestParam(required = false) Integer outboundType,
            @Parameter(description = "出库单状态编码") @RequestParam(required = false) Integer status) {
        return ApiResponse.success(outboundQueryService.pageList(page, size, warehouseId, outboundType, status));
    }

    @Operation(summary = "出库发货", description = "对指定出库单执行发货确认，并记录操作人信息。")
    @PutMapping("/{id}/ship")
    public ApiResponse<Void> ship(
            @Parameter(description = "出库单主键 ID") @PathVariable String id,
            @Parameter(description = "操作人用户 ID") @RequestParam String operatorId,
            @Parameter(description = "操作人姓名") @RequestParam String operatorName) {
        log.info("[API] 出库确认: id={}, operator={}", (id + "").replace("\r", "_").replace("\n", "_"), (operatorName + "").replace("\r", "_").replace("\n", "_"));
        try {
            boolean success = outboundCommandService.ship(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "出库失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "取消出库单", description = "取消指定出库单并记录操作人，仅未发货的单据允许取消。")
    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @Parameter(description = "出库单主键 ID") @PathVariable String id,
            @Parameter(description = "操作人用户 ID") @RequestParam String operatorId,
            @Parameter(description = "操作人姓名") @RequestParam String operatorName) {
        log.info("[API] 取消出库单 id={}, operator={}", (id + "").replace("\r", "_").replace("\n", "_"), (operatorName + "").replace("\r", "_").replace("\n", "_"));
        try {
            boolean success = outboundCommandService.cancel(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
