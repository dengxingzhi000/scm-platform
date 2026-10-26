package com.scmcloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.domain.entity.ProdAttributeTemplate;
import com.scmcloud.product.service.IProdAttributeTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-attribute-template")
@RequiredArgsConstructor
@Tag(name = "属性模板", description = "商品分类属性模板的查询、新增、修改与删除接口。")
public class ProdAttributeTemplateController {
    private final IProdAttributeTemplateService attributeTemplateService;

    @Operation(summary = "分页查询属性模板", description = "按分类筛选并分页查询未删除的属性模板列表。")
    @GetMapping
    public ApiResponse<PageResult<ProdAttributeTemplate>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "商品分类ID，可选，用于筛选该分类下的属性模板")
            @RequestParam(required = false) String categoryId) {

        Page<ProdAttributeTemplate> result = attributeTemplateService.lambdaQuery()
                .eq(categoryId != null, ProdAttributeTemplate::getCategoryId, categoryId)
                .eq(ProdAttributeTemplate::getDeleted, false)
                .orderByDesc(ProdAttributeTemplate::getCreateTime)
                .page(new Page<>(page, size));

        return ApiResponse.success(PageResult.of(result));
    }

    @Operation(summary = "查询属性模板详情", description = "根据模板ID查询属性模板的详细信息。")
    @GetMapping("/{id}")
    public ApiResponse<ProdAttributeTemplate> getById(@Parameter(description = "属性模板ID") @PathVariable String id) {
        ProdAttributeTemplate template = attributeTemplateService.getById(id);
        return ApiResponse.success(template);
    }

    @Operation(summary = "按分类查询属性模板", description = "查询指定商品分类下配置的全部属性模板。")
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<ProdAttributeTemplate>> listByCategoryId(@Parameter(description = "商品分类ID") @PathVariable String categoryId) {
        List<ProdAttributeTemplate> result = attributeTemplateService.listByCategoryId(categoryId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "创建属性模板", description = "新增属性模板，自动生成ID并初始化创建与更新时间。")
    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProdAttributeTemplate template) {
        template.setId(UUID.randomUUID().toString());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(false);
        attributeTemplateService.save(template);
        return ApiResponse.success();
    }

    @Operation(summary = "更新属性模板", description = "根据模板ID更新属性模板内容并刷新更新时间。")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@Parameter(description = "属性模板ID") @PathVariable String id, @RequestBody ProdAttributeTemplate template) {
        template.setId(id);
        template.setUpdateTime(LocalDateTime.now());
        attributeTemplateService.updateById(template);
        return ApiResponse.success();
    }

    @Operation(summary = "删除属性模板", description = "根据模板ID逻辑删除属性模板。")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@Parameter(description = "属性模板ID") @PathVariable String id) {
        attributeTemplateService.lambdaUpdate()
                .eq(ProdAttributeTemplate::getId, id)
                .set(ProdAttributeTemplate::getDeleted, true)
                .set(ProdAttributeTemplate::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
