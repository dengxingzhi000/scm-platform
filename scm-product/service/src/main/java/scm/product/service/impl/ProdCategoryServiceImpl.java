package scm.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import scm.product.domain.entity.ProdCategory;
import scm.product.mapper.ProdCategoryMapper;
import scm.product.service.IProdCategoryService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProdCategoryServiceImpl extends ServiceImpl<ProdCategoryMapper, ProdCategory>
        implements IProdCategoryService {

    @Override
    public List<ProdCategory> listByParentId(String parentId) {
        log.debug("查询子分类: parentId={}", parentId);
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
        log.debug("搜索分类: name={}", name);
        return lambdaQuery()
                .like(StringUtils.hasText(name), ProdCategory::getCategoryName, name)
                .eq(ProdCategory::getDeleted, false)
                .orderByAsc(ProdCategory::getSortOrder)
                .list();
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
