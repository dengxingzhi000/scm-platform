package com.scmcloud.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurRequestItem;
import com.scmcloud.purchase.service.IPurRequestItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-request-item")
@Tag(name = "采购申请明细", description = "采购申请单明细行的查询与维护接口")
public class PurRequestItemController {

    private final IPurRequestItemService purRequestItemService;

    @Operation(summary = "根据 ID 查询申请明细", description = "按明细主键查询单条采购申请明细记录。")
    @GetMapping("/{id}")
    public PurRequestItem getById(@Parameter(description = "申请明细主键 ID") @PathVariable String id) {
        return purRequestItemService.getById(id);
    }

    @Operation(summary = "查询申请单下的明细列表", description = "根据采购申请单 ID 查询该申请单包含的全部明细行。")
    @GetMapping("/list/{requestId}")
    public List<PurRequestItem> listByRequestId(@Parameter(description = "采购申请单 ID") @PathVariable String requestId) {
        return purRequestItemService.listByRequestId(requestId);
    }

    @Operation(summary = "新增申请明细", description = "创建一条新的采购申请明细记录。")
    @PostMapping
    public boolean save(@RequestBody PurRequestItem purRequestItem) {
        return purRequestItemService.save(purRequestItem);
    }

    @Operation(summary = "修改申请明细", description = "按主键更新已有的采购申请明细记录。")
    @PutMapping
    public boolean update(@RequestBody PurRequestItem purRequestItem) {
        return purRequestItemService.updateById(purRequestItem);
    }

    @Operation(summary = "删除申请明细", description = "按明细主键删除单条采购申请明细记录。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "申请明细主键 ID") @PathVariable String id) {
        return purRequestItemService.removeById(id);
    }

    @Operation(summary = "按申请单删除明细", description = "删除指定采购申请单下的全部明细行。")
    @DeleteMapping("/request/{requestId}")
    public boolean deleteByRequestId(@Parameter(description = "采购申请单 ID") @PathVariable String requestId) {
        return purRequestItemService.deleteByRequestId(requestId);
    }
}
