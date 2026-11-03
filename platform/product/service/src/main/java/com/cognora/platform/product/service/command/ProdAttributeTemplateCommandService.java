package com.cognora.platform.product.service.command;

import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.cognora.platform.common.data.rw.annotation.Master;
import com.cognora.platform.product.domain.entity.ProdAttributeTemplate;
import com.cognora.platform.product.mapper.ProdAttributeTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdAttributeTemplateCommandService {

    private final ProdAttributeTemplateMapper prodAttributeTemplateMapper;

    @Master(reason = "创建属性模板")
    @Transactional(rollbackFor = Exception.class)
    public int save(ProdAttributeTemplate template) {
        return prodAttributeTemplateMapper.insert(template);
    }

    @Master(reason = "更新属性模板")
    @Transactional(rollbackFor = Exception.class)
    public int updateById(ProdAttributeTemplate template) {
        return prodAttributeTemplateMapper.updateById(template);
    }

    @Master(reason = "更新属性模板")
    @Transactional(rollbackFor = Exception.class)
    public LambdaUpdateChainWrapper<ProdAttributeTemplate> lambdaUpdate() {
        return new LambdaUpdateChainWrapper<>(prodAttributeTemplateMapper);
    }
}
