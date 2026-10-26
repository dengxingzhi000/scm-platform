package com.scmcloud.system.controller;

import com.scmcloud.common.response.ApiResponse;
import com.scmcloud.system.domain.entity.SysDictItem;
import com.scmcloud.system.domain.entity.SysDictType;
import com.scmcloud.system.service.ISysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/system/dicts")
@RequiredArgsConstructor
@Tag(name = "数据字典", description = "字典类型与字典项的查询、新增、修改与删除，供各业务模块统一取值。")
public class SysDictController {

    private final ISysDictService dictService;

    @Operation(summary = "查询字典类型列表", description = "按字典编码和状态筛选查询全部字典类型。")
    @GetMapping("/types")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public ApiResponse<List<SysDictType>> listTypes(
            @Parameter(description = "字典编码，用于按编码筛选") @RequestParam(required = false) String dictCode,
            @Parameter(description = "字典类型状态，用于按启用状态筛选") @RequestParam(required = false) Integer status) {
        return ApiResponse.success(dictService.listDictTypes(dictCode, status));
    }

    @Operation(summary = "按编码查询字典类型", description = "根据字典编码查询对应的字典类型详情。")
    @GetMapping("/types/code/{dictCode}")
    public ApiResponse<SysDictType> getTypeByCode(@Parameter(description = "字典编码") @PathVariable String dictCode) {
        return ApiResponse.success(dictService.getDictTypeByCode(dictCode));
    }

    @Operation(summary = "新增字典类型", description = "新增一个字典类型，主键由系统自动生成。")
    @PostMapping("/types")
    @PreAuthorize("hasAuthority('system:dict:add')")
    public ApiResponse<Void> createType(@RequestBody SysDictType entity) {
        entity.setId(UUID.randomUUID().toString());
        dictService.createDictType(entity);
        return ApiResponse.success(null);
    }

    @Operation(summary = "修改字典类型", description = "更新已有字典类型的编码、名称、描述与状态等信息。")
    @PutMapping("/types")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public ApiResponse<Void> updateType(@RequestBody SysDictType entity) {
        dictService.updateDictType(entity);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除字典类型", description = "根据字典类型 ID 删除该字典类型。")
    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public ApiResponse<Void> deleteType(@Parameter(description = "字典类型 ID") @PathVariable String id) {
        dictService.deleteDictType(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "查询字典项列表", description = "根据字典编码查询该字典下的全部字典项。")
    @GetMapping("/items/{dictCode}")
    public ApiResponse<List<SysDictItem>> listItems(@Parameter(description = "字典编码") @PathVariable String dictCode) {
        return ApiResponse.success(dictService.listDictItems(dictCode));
    }

    @Operation(summary = "新增字典项", description = "为指定字典类型新增一个字典项，主键由系统自动生成。")
    @PostMapping("/items")
    @PreAuthorize("hasAuthority('system:dict:add')")
    public ApiResponse<Void> createItem(@RequestBody SysDictItem entity) {
        entity.setId(UUID.randomUUID().toString());
        dictService.createDictItem(entity);
        return ApiResponse.success(null);
    }

    @Operation(summary = "修改字典项", description = "更新已有字典项的编码、名称、取值、排序与状态等信息。")
    @PutMapping("/items")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public ApiResponse<Void> updateItem(@RequestBody SysDictItem entity) {
        dictService.updateDictItem(entity);
        return ApiResponse.success(null);
    }

    @Operation(summary = "删除字典项", description = "根据字典项 ID 删除该字典项。")
    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public ApiResponse<Void> deleteItem(@Parameter(description = "字典项 ID") @PathVariable String id) {
        dictService.deleteDictItem(id);
        return ApiResponse.success(null);
    }
}
