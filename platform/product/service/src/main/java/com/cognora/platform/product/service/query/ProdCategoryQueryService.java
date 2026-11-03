package com.cognora.platform.product.service.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.product.domain.entity.ProdCategory;
import com.cognora.platform.product.mapper.ProdCategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdCategoryQueryService {

    private final ProdCategoryMapper prodCategoryMapper;

    @Slave
    public ProdCategory getById(String id) {
        return prodCategoryMapper.selectById(id);
    }

    @Slave
    public List<ProdCategory> listByParentId(String parentId) {
        return prodCategoryMapper.selectList(
                Wrappers.<ProdCategory>lambdaQuery()
                        .eq(ProdCategory::getParentId, parentId)
                        .eq(ProdCategory::getDeleted, false)
                        .eq(ProdCategory::getEnabled, true)
                        .orderByAsc(ProdCategory::getSortOrder)
        );
    }

    @Slave
    public List<ProdCategory> getCategoryTree() {
        List<ProdCategory> allCategories = prodCategoryMapper.selectList(
                Wrappers.<ProdCategory>lambdaQuery()
                        .eq(ProdCategory::getDeleted, false)
                        .eq(ProdCategory::getEnabled, true)
                        .orderByAsc(ProdCategory::getSortOrder)
        );

        return buildTree(allCategories, "0");
    }

    @Slave
    public List<ProdCategory> searchByName(String name) {
        return prodCategoryMapper.selectList(
                Wrappers.<ProdCategory>lambdaQuery()
                        .like(StringUtils.hasText(name), ProdCategory::getCategoryName, name)
                        .eq(ProdCategory::getDeleted, false)
                        .orderByAsc(ProdCategory::getSortOrder)
        );
    }

    @Slave
    public LambdaQueryChainWrapper<ProdCategory> lambdaQuery() {
        return new LambdaQueryChainWrapper<>(prodCategoryMapper);
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
