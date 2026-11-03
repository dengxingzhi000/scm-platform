package com.scmcloud.product.service.command;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.product.domain.entity.ProdCategory;
import com.scmcloud.product.mapper.ProdCategoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdCategoryCommandService {

    private final ProdCategoryMapper prodCategoryMapper;

    @Master(reason = "创建分类")
    @Transactional(rollbackFor = Exception.class)
    public int save(ProdCategory category) {
        return prodCategoryMapper.insert(category);
    }

    @Master(reason = "更新分类")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(ProdCategory category) {
        return prodCategoryMapper.updateById(category);
    }

    @Master(reason = "更新分类")
    @Transactional(rollbackFor = Exception.class)
    public LambdaUpdateChainWrapper<ProdCategory> lambdaUpdate() {
        return new LambdaUpdateChainWrapper<>(prodCategoryMapper);
    }
}
