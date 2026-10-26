package com.scmcloud.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurReceiptItem;
import com.scmcloud.purchase.service.IPurReceiptItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-receipt-item")
@Tag(name = "收货明细", description = "采购收货单明细行的查询与维护接口")
public class PurReceiptItemController {

    private final IPurReceiptItemService purReceiptItemService;

    @Operation(summary = "根据 ID 查询收货明细", description = "按明细主键查询单条采购收货明细记录。")
    @GetMapping("/{id}")
    public PurReceiptItem getById(@Parameter(description = "收货明细主键 ID") @PathVariable String id) {
        return purReceiptItemService.getById(id);
    }

    @Operation(summary = "查询收货单下的明细列表", description = "根据收货单 ID 查询该收货单包含的全部明细行。")
    @GetMapping("/list/{receiptId}")
    public List<PurReceiptItem> listByReceiptId(@Parameter(description = "收货单 ID") @PathVariable String receiptId) {
        return purReceiptItemService.listByReceiptId(receiptId);
    }

    @Operation(summary = "新增收货明细", description = "创建一条新的采购收货明细记录。")
    @PostMapping
    public boolean save(@RequestBody PurReceiptItem purReceiptItem) {
        return purReceiptItemService.save(purReceiptItem);
    }

    @Operation(summary = "修改收货明细", description = "按主键更新已有的收货明细记录。")
    @PutMapping
    public boolean update(@RequestBody PurReceiptItem purReceiptItem) {
        return purReceiptItemService.updateById(purReceiptItem);
    }

    @Operation(summary = "删除收货明细", description = "按明细主键删除单条收货明细记录。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "收货明细主键 ID") @PathVariable String id) {
        return purReceiptItemService.removeById(id);
    }

    @Operation(summary = "按收货单删除明细", description = "删除指定收货单下的全部明细行。")
    @DeleteMapping("/receipt/{receiptId}")
    public boolean deleteByReceiptId(@Parameter(description = "收货单 ID") @PathVariable String receiptId) {
        return purReceiptItemService.deleteByReceiptId(receiptId);
    }
}
