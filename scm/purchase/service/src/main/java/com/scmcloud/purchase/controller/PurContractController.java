package com.scmcloud.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.purchase.domain.entity.PurContract;
import com.scmcloud.purchase.service.IPurContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/pur-contract")
@Tag(name = "采购合同", description = "采购合同的查询、维护、签订与终止管理。")
public class PurContractController {

    private final IPurContractService purContractService;

    @Operation(summary = "根据 ID 查询合同", description = "按主键查询采购合同的详细信息。")
    @GetMapping("/{id}")
    public PurContract getById(@Parameter(description = "采购合同 ID") @PathVariable String id) {
        return purContractService.getById(id);
    }

    @Operation(summary = "根据合同编号查询", description = "通过合同编号查询采购合同详情。")
    @GetMapping("/no/{contractNo}")
    public PurContract getByContractNo(@Parameter(description = "采购合同编号") @PathVariable String contractNo) {
        return purContractService.getByContractNo(contractNo);
    }

    @Operation(summary = "分页查询合同", description = "按状态、合同类型、供应商和关键字分页查询采购合同。")
    @GetMapping("/page")
    public Page<PurContract> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @Parameter(description = "合同类型") @RequestParam(required = false) Integer contractType,
            @Parameter(description = "供应商 ID") @RequestParam(required = false) String supplierId,
            @RequestParam(required = false) String keyword) {
        return purContractService.pageQuery(page, size, status, contractType, supplierId, keyword);
    }

    @Operation(summary = "按状态查询合同列表", description = "查询指定状态下的全部采购合同。")
    @GetMapping("/list")
    public List<PurContract> listByStatus(@Parameter(description = "合同状态") @RequestParam Integer status) {
        return purContractService.listByStatus(status);
    }

    @Operation(summary = "按供应商查询合同列表", description = "查询指定供应商名下的全部采购合同。")
    @GetMapping("/supplier/{supplierId}")
    public List<PurContract> listBySupplierId(@Parameter(description = "供应商 ID") @PathVariable String supplierId) {
        return purContractService.listBySupplierId(supplierId);
    }

    @Operation(summary = "新增合同", description = "创建一份新的采购合同记录。")
    @PostMapping
    public boolean save(@RequestBody PurContract purContract) {
        return purContractService.save(purContract);
    }

    @Operation(summary = "更新合同", description = "按主键更新采购合同信息。")
    @PutMapping
    public boolean update(@RequestBody PurContract purContract) {
        return purContractService.updateById(purContract);
    }

    @Operation(summary = "删除合同", description = "按主键删除采购合同。")
    @DeleteMapping("/{id}")
    public boolean delete(@Parameter(description = "采购合同 ID") @PathVariable String id) {
        return purContractService.removeById(id);
    }

    @Operation(summary = "签订合同", description = "记录合同签订人与签订时间并推进合同状态。")
    @PostMapping("/{id}/sign")
    public boolean sign(
            @Parameter(description = "采购合同 ID") @PathVariable String id,
            @Parameter(description = "签订人 ID") @RequestParam String signedBy,
            @Parameter(description = "签订人姓名") @RequestParam String signedByName) {
        return purContractService.sign(id, signedBy, signedByName);
    }

    @Operation(summary = "终止合同", description = "终止指定采购合同并更新其状态。")
    @PostMapping("/{id}/terminate")
    public boolean terminate(@Parameter(description = "采购合同 ID") @PathVariable String id) {
        return purContractService.terminate(id);
    }
}
