package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurQuotation;
import scm.purchase.service.IPurQuotationService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-quotation")
public class PurQuotationController {

    private final IPurQuotationService purQuotationService;

    @GetMapping("/{id}")
    public PurQuotation getById(@PathVariable String id) {
        return purQuotationService.getById(id);
    }

    @GetMapping("/no/{quotationNo}")
    public PurQuotation getByQuotationNo(@PathVariable String quotationNo) {
        return purQuotationService.getByQuotationNo(quotationNo);
    }

    @GetMapping("/page")
    public Page<PurQuotation> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String rfqId) {
        return purQuotationService.pageQuery(page, size, status, supplierId, rfqId);
    }

    @GetMapping("/rfq/{rfqId}")
    public List<PurQuotation> listByRfqId(@PathVariable String rfqId) {
        return purQuotationService.listByRfqId(rfqId);
    }

    @GetMapping("/supplier/{supplierId}")
    public List<PurQuotation> listBySupplierId(@PathVariable String supplierId) {
        return purQuotationService.listBySupplierId(supplierId);
    }

    @PostMapping
    public boolean save(@RequestBody PurQuotation purQuotation) {
        return purQuotationService.save(purQuotation);
    }

    @PutMapping
    public boolean update(@RequestBody PurQuotation purQuotation) {
        return purQuotationService.updateById(purQuotation);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purQuotationService.removeById(id);
    }

    @PostMapping("/{id}/submit")
    public boolean submit(@PathVariable String id) {
        return purQuotationService.submit(id);
    }

    @PostMapping("/{id}/select")
    public boolean select(
            @PathVariable String id,
            @RequestParam String selectedBy,
            @RequestParam String selectedByName) {
        return purQuotationService.select(id, selectedBy, selectedByName);
    }
}
