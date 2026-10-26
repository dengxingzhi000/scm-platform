package com.scmcloud.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurReceipt;
import com.scmcloud.purchase.service.IPurReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-receipt")
@Tag(name = "采购收货", description = "采购收货单的查询、收货、质检与上架接口")
public class PurReceiptController {

    private final IPurReceiptService purReceiptService;

    @Operation(summary = "根据 ID 查询收货单", description = "按主键查询单张采购收货单的完整信息。")
    @GetMapping("/{id}")
    public PurReceipt getById(@Parameter(description = "收货单主键 ID") @PathVariable String id) {
        return purReceiptService.getById(id);
    }

    @Operation(summary = "根据收货单号查询", description = "按业务单号查询采购收货单，用于外部单据核对。")
    @GetMapping("/no/{receiptNo}")
    public PurReceipt getByReceiptNo(@Parameter(description = "收货单业务单号") @PathVariable String receiptNo) {
        return purReceiptService.getByReceiptNo(receiptNo);
    }

    @Operation(summary = "分页查询收货单", description = "按状态、收货类型、供应商和关键字分页查询采购收货单。")
    @GetMapping("/page")
    public Page<PurReceipt> page(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "收货单状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "收货类型") @RequestParam(required = false) Integer receiptType,
            @Parameter(description = "供应商 ID") @RequestParam(required = false) String supplierId,
            @Parameter(description = "关键字，用于模糊匹配单号等字段") @RequestParam(required = false) String keyword) {
        return purReceiptService.pageQuery(page, size, status, receiptType, supplierId, keyword);
    }

    @Operation(summary = "查询采购订单的收货单", description = "根据采购订单 ID 查询该订单关联的全部收货单。")
    @GetMapping("/order/{orderId}")
    public List<PurReceipt> listByOrderId(@Parameter(description = "采购订单 ID") @PathVariable String orderId) {
        return purReceiptService.listByOrderId(orderId);
    }

    @Operation(summary = "新增收货单", description = "创建一张新的采购收货单。")
    @PostMapping
    public boolean save(@RequestBody PurReceipt purReceipt) {
        return purReceiptService.save(purReceipt);
    }

    @Operation(summary = "修改收货单", description = "按主键更新已有的采购收货单。")
    @PutMapping
    public boolean update(@RequestBody PurReceipt purReceipt) {
        return purReceiptService.updateById(purReceipt);
    }

    @Operation(summary = "删除收货单", description = "按主键删除采购收货单。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "收货单主键 ID") @PathVariable String id) {
        return purReceiptService.removeById(id);
    }

    @Operation(summary = "确认收货", description = "登记收货人和收货人名称，将收货单推进至已收货状态。")
    @PostMapping("/{id}/receive")
    public boolean receive(
            @Parameter(description = "收货单主键 ID") @PathVariable String id,
            @Parameter(description = "收货人用户 ID") @RequestParam String receiverId,
            @Parameter(description = "收货人姓名") @RequestParam String receiverName) {
        return purReceiptService.receive(id, receiverId, receiverName);
    }

    @Operation(summary = "收货质检", description = "记录质检人、质检结果与备注，完成收货单的质量检验。")
    @PostMapping("/{id}/quality-inspect")
    public boolean qualityInspect(
            @Parameter(description = "收货单主键 ID") @PathVariable String id,
            @Parameter(description = "质检人用户 ID") @RequestParam String inspectorId,
            @Parameter(description = "质检人姓名") @RequestParam String inspectorName,
            @Parameter(description = "质检结果编码，例如合格或不合格") @RequestParam Integer result,
            @Parameter(description = "质检备注") @RequestParam(required = false) String remark) {
        return purReceiptService.qualityInspect(id, inspectorId, inspectorName, result, remark);
    }

    @Operation(summary = "收货上架", description = "记录上架操作人，将质检合格的货物上架入库。")
    @PostMapping("/{id}/shelve")
    public boolean shelve(
            @Parameter(description = "收货单主键 ID") @PathVariable String id,
            @Parameter(description = "上架操作人用户 ID") @RequestParam String shelvedBy,
            @Parameter(description = "上架操作人姓名") @RequestParam String shelvedByName) {
        return purReceiptService.shelve(id, shelvedBy, shelvedByName);
    }
}
