package com.scmcloud.product.service.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.product.domain.entity.ProdBrand;
import com.scmcloud.product.mapper.ProdBrandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdBrandQueryService {

    private final ProdBrandMapper prodBrandMapper;

    @Slave
    public ProdBrand getById(String id) {
        return prodBrandMapper.selectById(id);
    }

    @Slave
    public List<ProdBrand> searchByName(String name) {
        return prodBrandMapper.selectList(
                Wrappers.<ProdBrand>lambdaQuery()
                        .like(StringUtils.hasText(name), ProdBrand::getBrandName, name)
                        .eq(ProdBrand::getDeleted, false)
                        .eq(ProdBrand::getEnabled, true)
                        .orderByAsc(ProdBrand::getSortOrder)
        );
    }

    @Slave
    public List<ProdBrand> listFeatured() {
        return prodBrandMapper.selectList(
                Wrappers.<ProdBrand>lambdaQuery()
                        .eq(ProdBrand::getFeatured, true)
                        .eq(ProdBrand::getDeleted, false)
                        .eq(ProdBrand::getEnabled, true)
                        .orderByAsc(ProdBrand::getSortOrder)
        );
    }

    @Slave
    public LambdaQueryChainWrapper<ProdBrand> lambdaQuery() {
        return new LambdaQueryChainWrapper<>(prodBrandMapper);
    }
}
