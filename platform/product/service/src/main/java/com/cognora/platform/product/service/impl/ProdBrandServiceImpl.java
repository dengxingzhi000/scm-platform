package com.cognora.platform.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.cognora.platform.product.domain.entity.ProdBrand;
import com.cognora.platform.product.mapper.ProdBrandMapper;
import com.cognora.platform.product.service.IProdBrandService;
import com.cognora.platform.product.support.ProductTenantResolver;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdBrandServiceImpl extends ServiceImpl<ProdBrandMapper, ProdBrand> implements IProdBrandService {

    private final ProductTenantResolver tenantResolver;

    @Override
    public List<ProdBrand> searchByName(String name) {
        log.debug("搜索品牌: name={}", (name + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .like(StringUtils.hasText(name), ProdBrand::getBrandName, name)
                .eq(ProdBrand::getDeleted, false)
                .eq(ProdBrand::getEnabled, true)
                .orderByAsc(ProdBrand::getSortOrder)
                .list();
    }

    @Override
    public List<ProdBrand> listFeatured() {
        log.debug("查询推荐品牌");
        return lambdaQuery()
                .eq(ProdBrand::getFeatured, true)
                .eq(ProdBrand::getDeleted, false)
                .eq(ProdBrand::getEnabled, true)
                .orderByAsc(ProdBrand::getSortOrder)
                .list();
    }

    @Override
    public Page<ProdBrand> pageByName(String name, Integer page, Integer size) {
        return lambdaQuery()
                .like(name != null, ProdBrand::getBrandName, name)
                .eq(ProdBrand::getDeleted, false)
                .orderByAsc(ProdBrand::getSortOrder)
                .page(new Page<>(page, size));
    }

    @Override
    public ProdBrand getBrand(String id) {
        return requireOwnedBrand(id, tenantResolver.resolveTenantIdString());
    }

    @Override
    public ProdBrand createBrand(ProdBrand brand) {
        String tenantId = tenantResolver.resolveTenantIdString();
        rejectForeignTenant(brand.getTenantId(), tenantId);

        brand.setTenantId(tenantId);
        brand.setId(UUID.randomUUID().toString());
        brand.setCreateTime(LocalDateTime.now());
        brand.setUpdateTime(LocalDateTime.now());
        brand.setDeleted(false);
        save(brand);
        return brand;
    }

    @Override
    public void updateBrand(String id, ProdBrand brand) {
        String tenantId = tenantResolver.resolveTenantIdString();
        rejectForeignTenant(brand.getTenantId(), tenantId);
        requireOwnedBrand(id, tenantId);

        brand.setId(id);
        // 归属不可通过更新改写：置空后交给持久层忽略，避免把本租户数据迁到其他租户
        brand.setTenantId(null);
        brand.setUpdateTime(LocalDateTime.now());
        updateById(brand);
    }

    @Override
    public void deleteBrand(String id) {
        String tenantId = tenantResolver.resolveTenantIdString();
        requireOwnedBrand(id, tenantId);

        lambdaUpdate()
                .eq(ProdBrand::getId, id)
                .eq(ProdBrand::getTenantId, tenantId)
                .set(ProdBrand::getDeleted, true)
                .set(ProdBrand::getUpdateTime, LocalDateTime.now())
                .update();
    }

    private ProdBrand requireOwnedBrand(String id, String tenantId) {
        ProdBrand existing = getById(id);
        if (existing == null || !tenantId.equals(existing.getTenantId())) {
            throw new AccessDeniedException("Brand does not belong to the current tenant");
        }
        return existing;
    }

    private void rejectForeignTenant(String requestTenantId, String currentTenantId) {
        if (StringUtils.hasText(requestTenantId) && !currentTenantId.equals(requestTenantId)) {
            throw new AccessDeniedException("Brand tenant does not match the authenticated tenant");
        }
    }
}
