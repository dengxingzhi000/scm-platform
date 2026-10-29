package com.scmcloud.finance.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.common.util.UUIDv7Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.finance.domain.entity.SettlementItem;
import com.scmcloud.finance.service.ISettlementItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/settlement-item")
@Tag(name = "结算明细", description = "管理结算单下结算明细的增删改查。")
public class SettlementItemController {
    private final ISettlementItemService settlementItemService;

    @Operation(summary = "查询结算明细详情", description = "根据明细 ID 查询结算明细的完整信息。")
    @GetMapping("/{id}")
    public ApiResponse<SettlementItem> getById(@Parameter(description = "结算明细 ID") @PathVariable String id) {
        SettlementItem item = settlementItemService.getById(id);
        return ApiResponse.success(item);
    }

    @Operation(summary = "创建结算明细", description = "新增一条结算明细并生成主键与创建时间。")
    @PostMapping
    public ApiResponse<SettlementItem> create(@RequestBody SettlementItem item) {
        item.setId(UUIDv7Util.generateString());
        item.setCreateTime(LocalDateTime.now());
        settlementItemService.save(item);
        log.info("结算明细创建成功: id={}, settlementId={}", (item.getId() + "").replace("\r", "_").replace("\n", "_"), (item.getSettlementId() + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(item);
    }

    @Operation(summary = "更新结算明细", description = "按明细 ID 更新结算明细信息。")
    @PutMapping("/{id}")
    public ApiResponse<SettlementItem> update(@Parameter(description = "结算明细 ID") @PathVariable String id, @RequestBody SettlementItem item) {
        item.setId(id);
        settlementItemService.updateById(item);
        log.info("结算明细更新成功: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success(item);
    }

    @Operation(summary = "删除结算明细", description = "按明细 ID 删除结算明细。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "结算明细 ID") @PathVariable String id) {
        settlementItemService.removeById(id);
        log.info("结算明细删除成功: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        return ApiResponse.success();
    }

    @Operation(summary = "按结算单查询明细", description = "查询指定结算单下的全部结算明细。")
    @GetMapping("/by-settlement/{settlementId}")
    public ApiResponse<List<SettlementItem>> listBySettlementId(@Parameter(description = "结算单 ID") @PathVariable String settlementId) {
        List<SettlementItem> items = settlementItemService.listBySettlementId(settlementId);
        return ApiResponse.success(items);
    }
}
