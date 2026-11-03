package com.scmcloud.product.service.command;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.product.domain.entity.ProdBrand;
import com.scmcloud.product.mapper.ProdBrandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdBrandCommandService {

    private final ProdBrandMapper prodBrandMapper;

    @Master(reason = "创建品牌")
    @Transactional(rollbackFor = Exception.class)
    public int save(ProdBrand brand) {
        return prodBrandMapper.insert(brand);
    }

    @Master(reason = "更新品牌")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(ProdBrand brand) {
        return prodBrandMapper.updateById(brand);
    }

    @Master(reason = "更新品牌")
    @Transactional(rollbackFor = Exception.class)
    public LambdaUpdateChainWrapper<ProdBrand> lambdaUpdate() {
        return new LambdaUpdateChainWrapper<>(prodBrandMapper);
    }
}
