package com.cognora.platform.product.service.query;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.platform.product.domain.entity.ProdAttributeTemplate;
import com.cognora.platform.product.mapper.ProdAttributeTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProdAttributeTemplateQueryService {

    private final ProdAttributeTemplateMapper prodAttributeTemplateMapper;

    @Slave
    public ProdAttributeTemplate getById(String id) {
        return prodAttributeTemplateMapper.selectById(id);
    }

    @Slave
    public List<ProdAttributeTemplate> listByCategoryId(String categoryId) {
        return prodAttributeTemplateMapper.selectList(
                Wrappers.<ProdAttributeTemplate>lambdaQuery()
                        .eq(ProdAttributeTemplate::getCategoryId, categoryId)
                        .eq(ProdAttributeTemplate::getDeleted, false)
        );
    }

    @Slave
    public LambdaQueryChainWrapper<ProdAttributeTemplate> lambdaQuery() {
        return new LambdaQueryChainWrapper<>(prodAttributeTemplateMapper);
    }
}
