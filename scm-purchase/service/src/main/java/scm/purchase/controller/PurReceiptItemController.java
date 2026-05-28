package scm.purchase.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurReceiptItem;
import scm.purchase.service.IPurReceiptItemService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-receipt-item")
@Tag(name = "入库明细管理", description = "采购入库明细CRUD接口")
public class PurReceiptItemController {

    @Autowired
    private IPurReceiptItemService purReceiptItemService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询明细")
    public PurReceiptItem getById(@PathVariable String id) {
        return purReceiptItemService.getById(id);
    }

    @GetMapping("/list/{receiptId}")
    @Operation(summary = "根据入库单ID查询明细列表")
    public List<PurReceiptItem> listByReceiptId(@PathVariable String receiptId) {
        return purReceiptItemService.listByReceiptId(receiptId);
    }

    @PostMapping
    @Operation(summary = "创建明细")
    public boolean save(@RequestBody PurReceiptItem purReceiptItem) {
        return purReceiptItemService.save(purReceiptItem);
    }

    @PutMapping
    @Operation(summary = "更新明细")
    public boolean update(@RequestBody PurReceiptItem purReceiptItem) {
        return purReceiptItemService.updateById(purReceiptItem);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除明细")
    public boolean delete(@PathVariable String id) {
        return purReceiptItemService.removeById(id);
    }

    @DeleteMapping("/receipt/{receiptId}")
    @Operation(summary = "根据入库单ID删除所有明细")
    public boolean deleteByReceiptId(@PathVariable String receiptId) {
        return purReceiptItemService.deleteByReceiptId(receiptId);
    }
}
