package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurReceipt;
import scm.purchase.service.IPurReceiptService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-receipt")
@Tag(name = "采购入库管理", description = "采购入库单CRUD及工作流接口")
public class PurReceiptController {

    @Autowired
    private IPurReceiptService purReceiptService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询入库单")
    public PurReceipt getById(@PathVariable String id) {
        return purReceiptService.getById(id);
    }

    @GetMapping("/no/{receiptNo}")
    @Operation(summary = "根据入库单号查询")
    public PurReceipt getByReceiptNo(@PathVariable String receiptNo) {
        return purReceiptService.getByReceiptNo(receiptNo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询入库单")
    public Page<PurReceipt> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer receiptType,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String keyword) {
        return purReceiptService.pageQuery(page, size, status, receiptType, supplierId, keyword);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "根据订单ID查询入库单列表")
    public List<PurReceipt> listByOrderId(@PathVariable String orderId) {
        return purReceiptService.listByOrderId(orderId);
    }

    @PostMapping
    @Operation(summary = "创建入库单")
    public boolean save(@RequestBody PurReceipt purReceipt) {
        return purReceiptService.save(purReceipt);
    }

    @PutMapping
    @Operation(summary = "更新入库单")
    public boolean update(@RequestBody PurReceipt purReceipt) {
        return purReceiptService.updateById(purReceipt);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除入库单")
    public boolean delete(@PathVariable String id) {
        return purReceiptService.removeById(id);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "收货确认")
    public boolean receive(
            @PathVariable String id,
            @RequestParam String receiverId,
            @RequestParam String receiverName) {
        return purReceiptService.receive(id, receiverId, receiverName);
    }

    @PostMapping("/{id}/quality-inspect")
    @Operation(summary = "质检")
    public boolean qualityInspect(
            @PathVariable String id,
            @RequestParam String inspectorId,
            @RequestParam String inspectorName,
            @RequestParam Integer result,
            @RequestParam(required = false) String remark) {
        return purReceiptService.qualityInspect(id, inspectorId, inspectorName, result, remark);
    }

    @PostMapping("/{id}/shelve")
    @Operation(summary = "上架入库")
    public boolean shelve(
            @PathVariable String id,
            @RequestParam String shelvedBy,
            @RequestParam String shelvedByName) {
        return purReceiptService.shelve(id, shelvedBy, shelvedByName);
    }
}
