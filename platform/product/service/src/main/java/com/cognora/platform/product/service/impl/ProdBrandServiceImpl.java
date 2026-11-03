package com.cognora.platform.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.cognora.platform.product.domain.entity.ProdBrand;
import com.cognora.platform.product.mapper.ProdBrandMapper;
import com.cognora.platform.product.service.IProdBrandService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProdBrandServiceImpl extends ServiceImpl<ProdBrandMapper, ProdBrand> implements IProdBrandService {

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
    public ProdBrand createBrand(ProdBrand brand) {
        brand.setId(UUID.randomUUID().toString());
        brand.setCreateTime(LocalDateTime.now());
        brand.setUpdateTime(LocalDateTime.now());
        brand.setDeleted(false);
        save(brand);
        return brand;
    }

    @Override
    public void updateBrand(String id, ProdBrand brand) {
        brand.setId(id);
        brand.setUpdateTime(LocalDateTime.now());
        updateById(brand);
    }

    @Override
    public void deleteBrand(String id) {
        lambdaUpdate()
                .eq(ProdBrand::getId, id)
                .set(ProdBrand::getDeleted, true)
                .set(ProdBrand::getUpdateTime, LocalDateTime.now())
                .update();
    }
}
