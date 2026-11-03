package com.scmcloud.product.service.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.product.domain.entity.ProdSku;
import com.scmcloud.product.mapper.ProdSkuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdSkuQueryService {

    private final ProdSkuMapper prodSkuMapper;

    @Slave
    public ProdSku getById(String id) {
        return prodSkuMapper.selectById(id);
    }

    @Slave
    public List<ProdSku> listBySpuId(String spuId) {
        return prodSkuMapper.selectList(
                Wrappers.<ProdSku>lambdaQuery()
                        .eq(ProdSku::getSpuId, spuId)
                        .eq(ProdSku::getDeleted, false)
                        .ne(ProdSku::getStatus, 3)
                        .orderByAsc(ProdSku::getCreateTime)
        );
    }

    @Slave
    public List<ProdSku> listBySpuIds(List<String> spuIds) {
        return prodSkuMapper.selectList(
                Wrappers.<ProdSku>lambdaQuery()
                        .in(ProdSku::getSpuId, spuIds)
                        .eq(ProdSku::getDeleted, false)
                        .ne(ProdSku::getStatus, 3)
                        .orderByAsc(ProdSku::getCreateTime)
        );
    }

    @Slave
    public LambdaQueryChainWrapper<ProdSku> lambdaQuery() {
        return new LambdaQueryChainWrapper<>(prodSkuMapper);
    }
}
