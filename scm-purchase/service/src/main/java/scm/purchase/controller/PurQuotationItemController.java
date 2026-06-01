package scm.purchase.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurQuotationItem;
import scm.purchase.service.IPurQuotationItemService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-quotation-item")
public class PurQuotationItemController {

    private final IPurQuotationItemService purQuotationItemService;

    @GetMapping("/{id}")
    public PurQuotationItem getById(@PathVariable String id) {
        return purQuotationItemService.getById(id);
    }

    @GetMapping("/list/{quotationId}")
    public List<PurQuotationItem> listByQuotationId(@PathVariable String quotationId) {
        return purQuotationItemService.listByQuotationId(quotationId);
    }

    @PostMapping
    public boolean save(@RequestBody PurQuotationItem purQuotationItem) {
        return purQuotationItemService.save(purQuotationItem);
    }

    @PutMapping
    public boolean update(@RequestBody PurQuotationItem purQuotationItem) {
        return purQuotationItemService.updateById(purQuotationItem);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purQuotationItemService.removeById(id);
    }

    @DeleteMapping("/quotation/{quotationId}")
    public boolean deleteByQuotationId(@PathVariable String quotationId) {
        return purQuotationItemService.deleteByQuotationId(quotationId);
    }
}
