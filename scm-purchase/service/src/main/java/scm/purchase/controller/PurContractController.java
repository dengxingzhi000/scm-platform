package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurContract;
import scm.purchase.service.IPurContractService;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-contract")
public class PurContractController {

    private final IPurContractService purContractService;

    @GetMapping("/{id}")
    public PurContract getById(@PathVariable String id) {
        return purContractService.getById(id);
    }

    @GetMapping("/no/{contractNo}")
    public PurContract getByContractNo(@PathVariable String contractNo) {
        return purContractService.getByContractNo(contractNo);
    }

    @GetMapping("/page")
    public Page<PurContract> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer contractType,
            @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String keyword) {
        return purContractService.pageQuery(page, size, status, contractType, supplierId, keyword);
    }

    @GetMapping("/list")
    public List<PurContract> listByStatus(@RequestParam Integer status) {
        return purContractService.listByStatus(status);
    }

    @GetMapping("/supplier/{supplierId}")
    public List<PurContract> listBySupplierId(@PathVariable String supplierId) {
        return purContractService.listBySupplierId(supplierId);
    }

    @PostMapping
    public boolean save(@RequestBody PurContract purContract) {
        return purContractService.save(purContract);
    }

    @PutMapping
    public boolean update(@RequestBody PurContract purContract) {
        return purContractService.updateById(purContract);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable String id) {
        return purContractService.removeById(id);
    }

    @PostMapping("/{id}/sign")
    public boolean sign(
            @PathVariable String id,
            @RequestParam String signedBy,
            @RequestParam String signedByName) {
        return purContractService.sign(id, signedBy, signedByName);
    }

    @PostMapping("/{id}/terminate")
    public boolean terminate(@PathVariable String id) {
        return purContractService.terminate(id);
    }
}
