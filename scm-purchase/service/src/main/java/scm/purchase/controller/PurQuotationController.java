package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurQuotation;
import scm.purchase.service.IPurQuotationService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-quotation")
@Tag(name = "供应商报价管理", description = "报价单CRUD及工作流接口")
public class PurQuotationController {

    @Autowired
    private IPurQuotationService purQuotationService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询报价单")
    public PurQuotation getById(@PathVariable String id) {
        return purQuotationService.getById(id);
    }

    @GetMapping("/no/{quotationNo}")
    @Operation(summary = "根据报价编号查询")
    public PurQuotation getByQuotationNo(@PathVariable String quotationNo) {
        return purQuotationService.getByQuotationNo(quotationNo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询报价单")
    public Page<PurQuotation> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String rfqId) {
        return purQuotationService.pageQuery(page, size, status, supplierId, rfqId);
    }

    @GetMapping("/rfq/{rfqId}")
    @Operation(summary = "根据询价单ID查询报价列表")
    public List<PurQuotation> listByRfqId(@PathVariable String rfqId) {
        return purQuotationService.listByRfqId(rfqId);
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "根据供应商ID查询报价列表")
    public List<PurQuotation> listBySupplierId(@PathVariable String supplierId) {
        return purQuotationService.listBySupplierId(supplierId);
    }

    @PostMapping
    @Operation(summary = "创建报价单")
    public boolean save(@RequestBody PurQuotation purQuotation) {
        return purQuotationService.save(purQuotation);
    }

    @PutMapping
    @Operation(summary = "更新报价单")
    public boolean update(@RequestBody PurQuotation purQuotation) {
        return purQuotationService.updateById(purQuotation);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除报价单")
    public boolean delete(@PathVariable String id) {
        return purQuotationService.removeById(id);
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "提交报价单")
    public boolean submit(@PathVariable String id) {
        return purQuotationService.submit(id);
    }

    @PostMapping("/{id}/select")
    @Operation(summary = "选中报价单")
    public boolean select(
            @PathVariable String id,
            @RequestParam String selectedBy,
            @RequestParam String selectedByName) {
        return purQuotationService.select(id, selectedBy, selectedByName);
    }
}
