package com.scmcloud.warehouse.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.service.command.WmsOutboundItemCommandService;
import com.scmcloud.warehouse.service.query.WmsOutboundItemQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-outbound-item")
@Tag(name = "出库明细", description = "出库单据下商品明细行的增删改查管理")
public class WmsOutboundItemController {

    private final WmsOutboundItemCommandService outboundItemCommandService;
    private final WmsOutboundItemQueryService outboundItemQueryService;

    @Operation(summary = "创建出库明细", description = "为指定出库单新增一条商品明细行。")
    @PostMapping
    public ApiResponse<WmsOutboundItem> create(@RequestBody WmsOutboundItem item) {
        log.info("[API] 创建出库明细: outboundId={}, skuId={}", (item.getOutboundId() + "").replace("\r", "_").replace("\n", "_"), (item.getSkuId() + "").replace("\r", "_").replace("\n", "_"));
        WmsOutboundItem created = outboundItemCommandService.create(item);
        log.info("[API] 出库明细创建成功: id={}", (created.getId() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新出库明细", description = "按主键更新出库明细的数量、库位等信息。")
    @PutMapping("/{id}")
    public ApiResponse<WmsOutboundItem> update(@Parameter(description = "出库明细主键 ID") @PathVariable String id, @RequestBody WmsOutboundItem item) {
        log.info("[API] 更新出库明细: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        item.setId(id);
        boolean success = outboundItemCommandService.update(item);
        if (!success) {
            return ApiResponse.fail(404, "出库明细不存在");
        }
        return ApiResponse.success(outboundItemQueryService.getById(id));
    }

    @Operation(summary = "删除出库明细", description = "按主键逻辑删除出库明细行。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "出库明细主键 ID") @PathVariable String id) {
        log.info("[API] 删除出库明细: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        boolean success = outboundItemCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "出库明细不存在");
    }

    @Operation(summary = "查询出库明细详情", description = "按主键查询出库明细，明细不存在或已删除时返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<WmsOutboundItem> getById(@Parameter(description = "出库明细主键 ID") @PathVariable String id) {
        WmsOutboundItem item = outboundItemQueryService.getById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return ApiResponse.fail(404, "出库明细不存在");
        }
        return ApiResponse.success(item);
    }

    @Operation(summary = "查询出库单明细列表", description = "根据出库单 ID 查询该出库单下的全部明细行。")
    @GetMapping("/list")
    public ApiResponse<List<WmsOutboundItem>> listByOutboundId(
            @Parameter(description = "出库单主键 ID") @RequestParam String outboundId) {
        return ApiResponse.success(outboundItemQueryService.listByOutboundId(outboundId));
    }
}
