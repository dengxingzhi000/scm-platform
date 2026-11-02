package com.scmcloud.supplier.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.supplier.domain.entity.SupSupplierContact;
import com.scmcloud.supplier.service.ISupSupplierContactService;
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
@RequestMapping("/api/v1/suppliers/{supplierId}/contacts")
@Tag(name = "供应商联系人", description = "维护供应商多联系人及主联系人。")
public class SupSupplierContactController {

    private final ISupSupplierContactService contactService;

    @Operation(summary = "查询联系人列表", description = "查询指定供应商的全部联系人，主联系人置顶。")
    @GetMapping
    public ApiResponse<List<SupSupplierContact>> list(
            @Parameter(description = "供应商ID") @PathVariable String supplierId) {
        return ApiResponse.success(contactService.listBySupplierId(supplierId));
    }

    @Operation(summary = "新增联系人", description = "新增供应商联系人；首个联系人自动成为主联系人。")
    @PostMapping
    public ApiResponse<SupSupplierContact> create(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @RequestBody SupSupplierContact contact) {
        log.info("[API] 新增供应商联系人 supplierId={}", supplierId);
        return ApiResponse.success(contactService.create(supplierId, contact));
    }

    @Operation(summary = "更新联系人", description = "更新联系人信息；设为主联系人时会自动取消其他主联系人。")
    @PutMapping("/{id}")
    public ApiResponse<SupSupplierContact> update(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @Parameter(description = "联系人ID") @PathVariable String id,
            @RequestBody SupSupplierContact contact) {
        log.info("[API] 更新供应商联系人 id={}", id);
        return ApiResponse.success(contactService.update(id, contact));
    }

    @Operation(summary = "删除联系人", description = "删除联系人；若为主联系人则自动提升下一位联系人。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @Parameter(description = "联系人ID") @PathVariable String id) {
        log.info("[API] 删除供应商联系人 id={}", id);
        boolean success = contactService.delete(id);
        return success ? ApiResponse.success() : ApiResponse.fail(404, "联系人不存在");
    }

    @Operation(summary = "设置主联系人", description = "将指定联系人设为主联系人，同步供应商档案联系方式。")
    @PutMapping("/{id}/primary")
    public ApiResponse<Void> setPrimary(
            @Parameter(description = "供应商ID") @PathVariable String supplierId,
            @Parameter(description = "联系人ID") @PathVariable String id) {
        contactService.setPrimary(id);
        return ApiResponse.success();
    }
}
