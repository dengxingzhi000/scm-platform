package com.scmcloud.product.service.command;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.product.domain.entity.ProdSku;
import com.scmcloud.product.mapper.ProdSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdSkuCommandService {

    private final ProdSkuMapper prodSkuMapper;

    @Master(reason = "创建SKU")
    @Transactional(rollbackFor = Exception.class)
    public int save(ProdSku sku) {
        return prodSkuMapper.insert(sku);
    }

    @Master(reason = "更新SKU")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(ProdSku sku) {
        return prodSkuMapper.updateById(sku);
    }

    @Master(reason = "更新SKU")
    @Transactional(rollbackFor = Exception.class)
    public LambdaUpdateChainWrapper<ProdSku> lambdaUpdate() {
        return new LambdaUpdateChainWrapper<>(prodSkuMapper);
    }
}
