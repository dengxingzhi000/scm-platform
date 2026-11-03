package com.scmcloud.product.service.command;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.product.domain.entity.ProdSpu;
import com.scmcloud.product.mapper.ProdSpuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdSpuCommandService {

    private final ProdSpuMapper prodSpuMapper;

    @Master(reason = "创建SPU")
    @Transactional(rollbackFor = Exception.class)
    public int save(ProdSpu spu) {
        return prodSpuMapper.insert(spu);
    }

    @Master(reason = "更新SPU")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(ProdSpu spu) {
        return prodSpuMapper.updateById(spu);
    }

    @Master(reason = "更新SPU")
    @Transactional(rollbackFor = Exception.class)
    public LambdaUpdateChainWrapper<ProdSpu> lambdaUpdate() {
        return new LambdaUpdateChainWrapper<>(prodSpuMapper);
    }
}
