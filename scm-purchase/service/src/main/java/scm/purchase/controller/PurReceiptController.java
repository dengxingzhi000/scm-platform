package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurReceipt;
import scm.purchase.service.IPurReceiptService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-receipt")
public class PurReceiptController {

    private final IPurReceiptService purReceiptService;

    @GetMapping("/{id}")
    public PurReceipt getById(@PathVariable String id) {
        return purReceiptService.getById(id);
    }

    @GetMapping("/no/{receiptNo}")
    public PurReceipt getByReceiptNo(@PathVariable String receiptNo) {
        return purReceiptService.getByReceiptNo(receiptNo);
    }

    @GetMapping("/page")
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
    public List<PurReceipt> listByOrderId(@PathVariable String orderId) {
        return purReceiptService.listByOrderId(orderId);
    }

    @PostMapping
    public boolean save(@RequestBody PurReceipt purReceipt) {
        return purReceiptService.save(purReceipt);
    }

    @PutMapping
    public boolean update(@RequestBody PurReceipt purReceipt) {
        return purReceiptService.updateById(purReceipt);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purReceiptService.removeById(id);
    }

    @PostMapping("/{id}/receive")
    public boolean receive(
            @PathVariable String id,
            @RequestParam String receiverId,
            @RequestParam String receiverName) {
        return purReceiptService.receive(id, receiverId, receiverName);
    }

    @PostMapping("/{id}/quality-inspect")
    public boolean qualityInspect(
            @PathVariable String id,
            @RequestParam String inspectorId,
            @RequestParam String inspectorName,
            @RequestParam Integer result,
            @RequestParam(required = false) String remark) {
        return purReceiptService.qualityInspect(id, inspectorId, inspectorName, result, remark);
    }

    @PostMapping("/{id}/shelve")
    public boolean shelve(
            @PathVariable String id,
            @RequestParam String shelvedBy,
            @RequestParam String shelvedByName) {
        return purReceiptService.shelve(id, shelvedBy, shelvedByName);
    }
}
