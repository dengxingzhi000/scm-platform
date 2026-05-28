package scm.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import scm.purchase.domain.entity.PurContract;
import scm.purchase.service.IPurContractService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/pur-contract")
@Tag(name = "采购合同管理", description = "采购合同CRUD及工作流接口")
public class PurContractController {

    @Autowired
    private IPurContractService purContractService;

    @GetMapping("/{id}")
    @Operation(summary = "根据ID查询合同")
    public PurContract getById(@PathVariable String id) {
        return purContractService.getById(id);
    }

    @GetMapping("/no/{contractNo}")
    @Operation(summary = "根据合同编号查询")
    public PurContract getByContractNo(@PathVariable String contractNo) {
        return purContractService.getByContractNo(contractNo);
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询合同")
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
    @Operation(summary = "根据状态查询合同列表")
    public List<PurContract> listByStatus(@RequestParam Integer status) {
        return purContractService.listByStatus(status);
    }

    @GetMapping("/supplier/{supplierId}")
    @Operation(summary = "根据供应商ID查询合同列表")
    public List<PurContract> listBySupplierId(@PathVariable String supplierId) {
        return purContractService.listBySupplierId(supplierId);
    }

    @PostMapping
    @Operation(summary = "创建合同")
    public boolean save(@RequestBody PurContract purContract) {
        return purContractService.save(purContract);
    }

    @PutMapping
    @Operation(summary = "更新合同")
    public boolean update(@RequestBody PurContract purContract) {
        return purContractService.updateById(purContract);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除合同")
    public boolean delete(@PathVariable String id) {
        return purContractService.removeById(id);
    }

    @PostMapping("/{id}/sign")
    @Operation(summary = "签署合同")
    public boolean sign(
            @PathVariable String id,
            @RequestParam String signedBy,
            @RequestParam String signedByName) {
        return purContractService.sign(id, signedBy, signedByName);
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "终止合同")
    public boolean terminate(@PathVariable String id) {
        return purContractService.terminate(id);
    }
}
