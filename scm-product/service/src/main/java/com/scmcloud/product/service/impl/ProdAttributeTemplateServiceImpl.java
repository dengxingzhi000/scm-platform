package com.scmcloud.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.scmcloud.product.domain.entity.ProdAttributeTemplate;
import com.scmcloud.product.mapper.ProdAttributeTemplateMapper;
import com.scmcloud.product.service.IProdAttributeTemplateService;

import java.util.List;

@Slf4j
@Service
public class ProdAttributeTemplateServiceImpl extends ServiceImpl<ProdAttributeTemplateMapper, ProdAttributeTemplate>
        implements IProdAttributeTemplateService {

    @Override
    public List<ProdAttributeTemplate> listByCategoryId(String categoryId) {
        log.debug("查询分类属性模板 categoryId={}", categoryId);
        return lambdaQuery()
                .eq(ProdAttributeTemplate::getCategoryId, categoryId)
                .eq(ProdAttributeTemplate::getDeleted, false)
                .list();
    }
}
