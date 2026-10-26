package com.scmcloud.system.controller;

import com.scmcloud.common.dto.dept.DeptDTO;
import com.scmcloud.common.log.annotation.AuditLog;
import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.system.service.ISysDeptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 部门表前端控制器
 * </p>
 *
 * @author author
 * @since 2025-11-07
 */
@RestController
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
@Tag(name = "部门管理", description = "组织架构部门的树形查询、新增、修改与删除。")
public class SysDeptController {
    private final ISysDeptService deptService;

    @Operation(summary = "查询部门树", description = "查询完整的部门树形结构，用于组织架构展示与部门选择。")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public ApiResponse<List<DeptDTO>> tree() {
        List<DeptDTO> tree = deptService.getDeptTree();

        return ApiResponse.success(tree);
    }

    @Operation(summary = "新增部门", description = "新增部门，可设置上级部门、部门编码、名称、负责人与联系方式等信息。")
    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    @AuditLog(
            operation = "新增部门",
            businessType = "DEPT",
            riskLevel = 3
    )
    public ApiResponse<Void> add(@Validated @RequestBody DeptDTO deptDTO) {
        deptService.addDept(deptDTO);

        return ApiResponse.success();
    }

    @Operation(summary = "修改部门", description = "根据部门 ID 更新部门编码、名称、上级部门等信息。")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:edit')")
    @AuditLog(
            operation = "修改部门",
            businessType = "DEPT",
            riskLevel = 3
    )
    public ApiResponse<Void> update(@Parameter(description = "部门 ID") @PathVariable UUID id, @Validated @RequestBody DeptDTO deptDTO) {
        deptDTO.setId(id);
        deptService.updateDept(deptDTO);

        return ApiResponse.success();
    }

    @Operation(summary = "删除部门", description = "根据部门 ID 删除指定部门。")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    @AuditLog(
            operation = "删除部门",
            businessType = "DEPT",
            riskLevel = 4
    )
    public ApiResponse<Void> delete(@Parameter(description = "部门 ID") @PathVariable UUID id) {
        deptService.deleteDept(id);

        return ApiResponse.success();
    }
}
