package com.scmcloud.warehouse.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsInbound;
import com.scmcloud.warehouse.service.command.WmsInboundCommandService;
import com.scmcloud.warehouse.service.query.WmsInboundQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-inbound")
@Tag(name = "入库管理", description = "仓库入库单的创建、查询、收货与取消管理")
public class WmsInboundController {

    private final WmsInboundCommandService inboundCommandService;
    private final WmsInboundQueryService inboundQueryService;

    @Operation(summary = "创建入库单", description = "根据仓库与入库类型新建一张入库单并生成入库单号。")
    @PostMapping
    public ApiResponse<WmsInbound> create(@RequestBody WmsInbound inbound) {
        log.info("[API] 创建入库单 warehouseId={}, type={}", inbound.getWarehouseId(), inbound.getInboundType());
        WmsInbound created = inboundCommandService.create(inbound);
        log.info("[API] 入库单创建成功 id={}, inboundNo={}", created.getId(), created.getInboundNo());
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新入库单", description = "按主键更新入库单信息，仅允许在未收货状态下修改。")
    @PutMapping("/{id}")
    public ApiResponse<WmsInbound> update(@Parameter(description = "入库单主键 ID") @PathVariable String id, @RequestBody WmsInbound inbound) {
        log.info("[API] 更新入库单 id={}", id);
        inbound.setId(id);
        try {
            boolean success = inboundCommandService.update(inbound);
            if (!success) {
                return ApiResponse.fail(404, "入库单不存在");
            }
            return ApiResponse.success(inboundQueryService.getById(id));
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "删除入库单", description = "按主键逻辑删除入库单，已删除的单据不再对外可见。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "入库单主键 ID") @PathVariable String id) {
        log.info("[API] 删除入库单 id={}", id);
        boolean success = inboundCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "入库单不存在");
    }

    @Operation(summary = "查询入库单详情", description = "按主键查询入库单，单据不存在或已删除时返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<WmsInbound> getById(@Parameter(description = "入库单主键 ID") @PathVariable String id) {
        WmsInbound inbound = inboundQueryService.getById(id);
        if (inbound == null || Boolean.TRUE.equals(inbound.getDeleted())) {
            return ApiResponse.fail(404, "入库单不存在");
        }
        return ApiResponse.success(inbound);
    }

    @Operation(summary = "分页查询入库单", description = "按仓库、入库类型与单据状态分页查询入库单列表。")
    @GetMapping("/page")
    public ApiResponse<Page<WmsInbound>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "仓库 ID，用于按仓库过滤") @RequestParam(required = false) String warehouseId,
            @Parameter(description = "入库类型编码，用于按业务类型过滤") @RequestParam(required = false) Integer inboundType,
            @Parameter(description = "入库单状态编码") @RequestParam(required = false) Integer status) {
        return ApiResponse.success(inboundQueryService.pageList(page, size, warehouseId, inboundType, status));
    }

    @Operation(summary = "入库收货", description = "对指定入库单执行收货确认，并记录操作人信息。")
    @PutMapping("/{id}/receive")
    public ApiResponse<Void> receive(
            @Parameter(description = "入库单主键 ID") @PathVariable String id,
            @Parameter(description = "操作人用户 ID") @RequestParam String operatorId,
            @Parameter(description = "操作人姓名") @RequestParam String operatorName) {
        log.info("[API] 入库收货: id={}, operator={}", id, operatorName);
        try {
            boolean success = inboundCommandService.receive(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "收货失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }

    @Operation(summary = "取消入库单", description = "取消指定入库单并记录操作人，仅未完成的单据允许取消。")
    @PutMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @Parameter(description = "入库单主键 ID") @PathVariable String id,
            @Parameter(description = "操作人用户 ID") @RequestParam String operatorId,
            @Parameter(description = "操作人姓名") @RequestParam String operatorName) {
        log.info("[API] 取消入库单 id={}, operator={}", id, operatorName);
        try {
            boolean success = inboundCommandService.cancel(id, operatorId, operatorName);
            return success ? ApiResponse.success() : ApiResponse.fail(400, "取消失败");
        } catch (IllegalStateException e) {
            return ApiResponse.fail(400, e.getMessage());
        }
    }
}
