package com.scmcloud.supplier.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.common.exception.ServiceException;
import com.scmcloud.supplier.domain.entity.SupSupplierCategory;
import com.scmcloud.supplier.domain.entity.SupSupplierCategoryRel;
import com.scmcloud.supplier.mapper.SupSupplierCategoryMapper;
import com.scmcloud.supplier.mapper.SupSupplierCategoryRelMapper;
import com.scmcloud.supplier.service.ISupSupplierCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierCategoryServiceImpl
        extends ServiceImpl<SupSupplierCategoryMapper, SupSupplierCategory>
        implements ISupSupplierCategoryService {

    private final SupSupplierCategoryRelMapper categoryRelMapper;

    @Override
    public List<SupSupplierCategory> listCategories(String keyword, Boolean enabled) {
        return lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SupSupplierCategory::getCategoryCode, keyword)
                        .or()
                        .like(SupSupplierCategory::getCategoryName, keyword))
                .eq(enabled != null, SupSupplierCategory::getEnabled, enabled)
                .orderByAsc(SupSupplierCategory::getLevel)
                .orderByAsc(SupSupplierCategory::getSortOrder)
                .orderByAsc(SupSupplierCategory::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierCategory createCategory(SupSupplierCategory category) {
        validateCategory(category);
        ensureCodeUnique(category.getCategoryCode(), null);

        category.setId(UUID.randomUUID().toString());
        category.setTenantId(null);
        category.setCreateBy(null);
        category.setUpdateBy(null);
        category.setLevel(resolveLevel(category.getParentId()));
        if (category.getEnabled() == null) {
            category.setEnabled(true);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        category.setDeleted(false);
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        save(category);
        log.info("创建供应商分类 code={}, name={}", category.getCategoryCode(), category.getCategoryName());
        return category;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SupSupplierCategory updateCategory(String id, SupSupplierCategory category) {
        SupSupplierCategory existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "供应商分类不存在");
        }
        validateCategory(category);
        if (StringUtils.hasText(category.getCategoryCode())) {
            ensureCodeUnique(category.getCategoryCode(), id);
        }
        if (id.equals(category.getParentId())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "上级分类不能是自身");
        }
        category.setId(id);
        category.setTenantId(null);
        category.setCreateBy(null);
        category.setCreateTime(null);
        category.setUpdateBy(null);
        category.setDeleted(null);
        if (category.getParentId() != null) {
            category.setLevel(resolveLevel(category.getParentId()));
        }
        category.setUpdateTime(LocalDateTime.now());
        updateById(category);
        return getById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(String id) {
        SupSupplierCategory existing = getById(id);
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            throw new ServiceException(404, HttpStatus.NOT_FOUND, "供应商分类不存在");
        }
        long referenced = categoryRelMapper.selectCount(
                Wrappers.<SupSupplierCategoryRel>lambdaQuery()
                        .eq(SupSupplierCategoryRel::getCategoryId, id));
        if (referenced > 0) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST,
                    "该分类已被供应商引用，无法删除");
        }
        removeById(id);
        log.info("删除供应商分类 id={}", id);
    }

    private void validateCategory(SupSupplierCategory category) {
        if (!StringUtils.hasText(category.getCategoryName())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "分类名称不能为空");
        }
        if (!StringUtils.hasText(category.getCategoryCode())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "分类编码不能为空");
        }
        if (category.getCategoryCode().length() > 64) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "分类编码长度不能超过 64");
        }
    }

    private void ensureCodeUnique(String categoryCode, String excludeId) {
        long count = lambdaQuery()
                .eq(SupSupplierCategory::getCategoryCode, categoryCode)
                .ne(StringUtils.hasText(excludeId), SupSupplierCategory::getId, excludeId)
                .count();
        if (count > 0) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "分类编码已存在");
        }
    }

    private int resolveLevel(String parentId) {
        if (!StringUtils.hasText(parentId)) {
            return 1;
        }
        SupSupplierCategory parent = getById(parentId);
        if (parent == null || Boolean.TRUE.equals(parent.getDeleted())) {
            throw new ServiceException(400, HttpStatus.BAD_REQUEST, "上级分类不存在");
        }
        return (parent.getLevel() == null ? 1 : parent.getLevel()) + 1;
    }
}
