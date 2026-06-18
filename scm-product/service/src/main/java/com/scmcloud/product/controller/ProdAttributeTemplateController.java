package com.scmcloud.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.domain.PageResult;
import com.scmcloud.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.scmcloud.product.domain.entity.ProdAttributeTemplate;
import com.scmcloud.product.service.IProdAttributeTemplateService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-attribute-template")
@RequiredArgsConstructor
public class ProdAttributeTemplateController {
    private final IProdAttributeTemplateService attributeTemplateService;

    @GetMapping
    public ApiResponse<PageResult<ProdAttributeTemplate>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String categoryId) {

        Page<ProdAttributeTemplate> result = attributeTemplateService.lambdaQuery()
                .eq(categoryId != null, ProdAttributeTemplate::getCategoryId, categoryId)
                .eq(ProdAttributeTemplate::getDeleted, false)
                .orderByDesc(ProdAttributeTemplate::getCreateTime)
                .page(new Page<>(page, size));

        return ApiResponse.success(PageResult.of(result));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProdAttributeTemplate> getById(@PathVariable String id) {
        ProdAttributeTemplate template = attributeTemplateService.getById(id);
        return ApiResponse.success(template);
    }

    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<ProdAttributeTemplate>> listByCategoryId(@PathVariable String categoryId) {
        List<ProdAttributeTemplate> result = attributeTemplateService.listByCategoryId(categoryId);
        return ApiResponse.success(result);
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody ProdAttributeTemplate template) {
        template.setId(UUID.randomUUID().toString());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(false);
        attributeTemplateService.save(template);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody ProdAttributeTemplate template) {
        template.setId(id);
        template.setUpdateTime(LocalDateTime.now());
        attributeTemplateService.updateById(template);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        attributeTemplateService.lambdaUpdate()
                .eq(ProdAttributeTemplate::getId, id)
                .set(ProdAttributeTemplate::getDeleted, true)
                .set(ProdAttributeTemplate::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
