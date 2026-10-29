package com.scmcloud.warehouse.controller;

import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.service.command.WmsInboundItemCommandService;
import com.scmcloud.warehouse.service.query.WmsInboundItemQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/wms-inbound-item")
@Tag(name = "入库明细", description = "入库单据下商品明细行的增删改查管理")
public class WmsInboundItemController {

    private final WmsInboundItemCommandService inboundItemCommandService;
    private final WmsInboundItemQueryService inboundItemQueryService;

    @Operation(summary = "创建入库明细", description = "为指定入库单新增一条商品明细行。")
    @PostMapping
    public ApiResponse<WmsInboundItem> create(@RequestBody WmsInboundItem item) {
        log.info("[API] 创建入库明细: inboundId={}, skuId={}", (item.getInboundId() + "").replace("\r", "_").replace("\n", "_"), (item.getSkuId() + "").replace("\r", "_").replace("\n", "_"));
        WmsInboundItem created = inboundItemCommandService.create(item);
        log.info("[API] 入库明细创建成功: id={}", (created.getId() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(created);
    }

    @Operation(summary = "更新入库明细", description = "按主键更新入库明细的数量、库位等信息。")
    @PutMapping("/{id}")
    public ApiResponse<WmsInboundItem> update(@Parameter(description = "入库明细主键 ID") @PathVariable String id, @RequestBody WmsInboundItem item) {
        log.info("[API] 更新入库明细: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        item.setId(id);
        boolean success = inboundItemCommandService.update(item);
        if (!success) {
            return ApiResponse.fail(404, "入库明细不存在");
        }
        return ApiResponse.success(inboundItemQueryService.getById(id));
    }

    @Operation(summary = "删除入库明细", description = "按主键逻辑删除入库明细行。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "入库明细主键 ID") @PathVariable String id) {
        log.info("[API] 删除入库明细: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        boolean success = inboundItemCommandService.softDeleteById(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "入库明细不存在");
    }

    @Operation(summary = "查询入库明细详情", description = "按主键查询入库明细，明细不存在或已删除时返回 404。")
    @GetMapping("/{id}")
    public ApiResponse<WmsInboundItem> getById(@Parameter(description = "入库明细主键 ID") @PathVariable String id) {
        WmsInboundItem item = inboundItemQueryService.getById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return ApiResponse.fail(404, "入库明细不存在");
        }
        return ApiResponse.success(item);
    }

    @Operation(summary = "查询入库单明细列表", description = "根据入库单 ID 查询该入库单下的全部明细行。")
    @GetMapping("/list")
    public ApiResponse<List<WmsInboundItem>> listByInboundId(
            @Parameter(description = "入库单主键 ID") @RequestParam String inboundId) {
        return ApiResponse.success(inboundItemQueryService.listByInboundId(inboundId));
    }
}
