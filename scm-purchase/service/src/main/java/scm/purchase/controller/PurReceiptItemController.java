package scm.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurReceiptItem;
import scm.purchase.service.IPurReceiptItemService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-receipt-item")
public class PurReceiptItemController {

    private final IPurReceiptItemService purReceiptItemService;

    @GetMapping("/{id}")
    public PurReceiptItem getById(@PathVariable String id) {
        return purReceiptItemService.getById(id);
    }

    @GetMapping("/list/{receiptId}")
    public List<PurReceiptItem> listByReceiptId(@PathVariable String receiptId) {
        return purReceiptItemService.listByReceiptId(receiptId);
    }

    @PostMapping
    public boolean save(@RequestBody PurReceiptItem purReceiptItem) {
        return purReceiptItemService.save(purReceiptItem);
    }

    @PutMapping
    public boolean update(@RequestBody PurReceiptItem purReceiptItem) {
        return purReceiptItemService.updateById(purReceiptItem);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purReceiptItemService.removeById(id);
    }

    @DeleteMapping("/receipt/{receiptId}")
    public boolean deleteByReceiptId(@PathVariable String receiptId) {
        return purReceiptItemService.deleteByReceiptId(receiptId);
    }
}
