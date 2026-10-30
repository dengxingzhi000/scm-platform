package com.scmcloud.supplier.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.supplier.domain.entity.SupSupplierBankAccount;
import com.scmcloud.supplier.service.ISupSupplierBankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/bank-accounts")
@Tag(name = "供应商银行账户", description = "维护供应商多银行账户及默认结算账户。")
public class SupSupplierBankAccountController {

    private final ISupSupplierBankAccountService bankAccountService;

    @Operation(summary = "查询银行账户列表", description = "查询指定供应商的全部银行账户，默认账户置顶。")
    @GetMapping
    public ApiResponse<List<SupSupplierBankAccount>> list(
            @Parameter(description = "供应商ID") @PathVariable String supplierId) {
        return ApiResponse.success(bankAccountService.listBySupplierId(supplierId));
    }

    @Operation(summary = "新增银行账户", description = "新增供应商银行账户；首个账户自动成为默认账户。")
    @PostMapping
    public ApiResponse<SupSupplierBankAccount> create(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @RequestBody SupSupplierBankAccount account) {
        log.info("[API] 新增供应商银行账户 supplierId={}", supplierId);
        return ApiResponse.success(bankAccountService.create(supplierId, account));
    }

    @Operation(summary = "更新银行账户", description = "更新银行账户；设为默认时会自动取消其他默认账户。")
    @PutMapping("/{id}")
    public ApiResponse<SupSupplierBankAccount> update(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @Parameter(description = "银行账户ID") @PathVariable String id,
            @RequestBody SupSupplierBankAccount account) {
        return ApiResponse.success(bankAccountService.update(id, account));
    }

    @Operation(summary = "删除银行账户", description = "删除银行账户；若为默认账户则自动提升下一位账户。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @Parameter(description = "银行账户ID") @PathVariable String id) {
        boolean success = bankAccountService.delete(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "银行账户不存在");
    }

    @Operation(summary = "设置默认账户", description = "将指定账户设为默认结算账户，同步供应商档案银行信息。")
    @PutMapping("/{id}/default")
    public ApiResponse<Void> setDefault(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @Parameter(description = "银行账户ID") @PathVariable String id) {
        bankAccountService.setDefault(id);
        return ApiResponse.success();
    }
}
