package com.scmcloud.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.scmcloud.product.domain.entity.ProdCategory;
import com.scmcloud.product.mapper.ProdCategoryMapper;
import com.scmcloud.product.service.IProdCategoryService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProdCategoryServiceImpl extends ServiceImpl<ProdCategoryMapper, ProdCategory>
        implements IProdCategoryService {

    @Override
    public List<ProdCategory> listByParentId(String parentId) {
        log.debug("查询子分类 parentId={}", (parentId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(ProdCategory::getParentId, parentId)
                .eq(ProdCategory::getDeleted, false)
                .eq(ProdCategory::getEnabled, true)
                .orderByAsc(ProdCategory::getSortOrder)
                .list();
    }

    @Override
    public List<ProdCategory> getCategoryTree() {
        log.debug("获取分类树");
        List<ProdCategory> allCategories = lambdaQuery()
                .eq(ProdCategory::getDeleted, false)
                .eq(ProdCategory::getEnabled, true)
                .orderByAsc(ProdCategory::getSortOrder)
                .list();

        return buildTree(allCategories, "0");
    }

    @Override
    public List<ProdCategory> searchByName(String name) {
        log.debug("搜索分类: name={}", (name + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .like(StringUtils.hasText(name), ProdCategory::getCategoryName, name)
                .eq(ProdCategory::getDeleted, false)
                .orderByAsc(ProdCategory::getSortOrder)
                .list();
    }

    @Override
    public Page<ProdCategory> pageByName(String name, Integer page, Integer size) {
        return lambdaQuery()
                .like(name != null, ProdCategory::getCategoryName, name)
                .eq(ProdCategory::getDeleted, false)
                .orderByAsc(ProdCategory::getSortOrder)
                .page(new Page<>(page, size));
    }

    @Override
    public ProdCategory createCategory(ProdCategory category) {
        category.setId(UUID.randomUUID().toString());
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setDeleted(false);
        save(category);
        return category;
    }

    @Override
    public void updateCategory(String id, ProdCategory category) {
        category.setId(id);
        category.setUpdateTime(LocalDateTime.now());
        updateById(category);
    }

    @Override
    public void deleteCategory(String id) {
        lambdaUpdate()
                .eq(ProdCategory::getId, id)
                .set(ProdCategory::getDeleted, true)
                .set(ProdCategory::getUpdateTime, LocalDateTime.now())
                .update();
    }

    private List<ProdCategory> buildTree(List<ProdCategory> allCategories, String parentId) {
        Map<String, List<ProdCategory>> parentMap = allCategories.stream()
                .collect(Collectors.groupingBy(ProdCategory::getParentId));

        return buildTreeNode(parentMap, parentId);
    }

    private List<ProdCategory> buildTreeNode(Map<String, List<ProdCategory>> parentMap, String parentId) {
        List<ProdCategory> children = parentMap.getOrDefault(parentId, new ArrayList<>());
        for (ProdCategory child : children) {
            List<ProdCategory> subChildren = buildTreeNode(parentMap, child.getId());
            child.setIsLeaf(subChildren.isEmpty());
        }
        return children;
    }
}
