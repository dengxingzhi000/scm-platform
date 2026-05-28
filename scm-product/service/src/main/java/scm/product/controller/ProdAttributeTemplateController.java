package scm.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.domain.PageResult;
import com.frog.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import scm.product.domain.entity.ProdAttributeTemplate;
import scm.product.service.IProdAttributeTemplateService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/prod-attribute-template")
@RequiredArgsConstructor
@Tag(name = "商品属性模板", description = "商品属性模板管理接口")
public class ProdAttributeTemplateController {

    private final IProdAttributeTemplateService attributeTemplateService;

    @GetMapping
    @Operation(summary = "分页查询属性模板")
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
    @Operation(summary = "查询属性模板详情")
    public ApiResponse<ProdAttributeTemplate> getById(@PathVariable String id) {
        ProdAttributeTemplate template = attributeTemplateService.getById(id);
        return ApiResponse.success(template);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "查询分类下的属性模板")
    public ApiResponse<List<ProdAttributeTemplate>> listByCategoryId(@PathVariable String categoryId) {
        List<ProdAttributeTemplate> result = attributeTemplateService.listByCategoryId(categoryId);
        return ApiResponse.success(result);
    }

    @PostMapping
    @Operation(summary = "创建属性模板")
    public ApiResponse<Void> create(@RequestBody ProdAttributeTemplate template) {
        template.setId(UUID.randomUUID().toString());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(false);
        attributeTemplateService.save(template);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新属性模板")
    public ApiResponse<Void> update(@PathVariable String id, @RequestBody ProdAttributeTemplate template) {
        template.setId(id);
        template.setUpdateTime(LocalDateTime.now());
        attributeTemplateService.updateById(template);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除属性模板")
    public ApiResponse<Void> delete(@PathVariable String id) {
        attributeTemplateService.lambdaUpdate()
                .eq(ProdAttributeTemplate::getId, id)
                .set(ProdAttributeTemplate::getDeleted, true)
                .set(ProdAttributeTemplate::getUpdateTime, LocalDateTime.now())
                .update();
        return ApiResponse.success();
    }
}
