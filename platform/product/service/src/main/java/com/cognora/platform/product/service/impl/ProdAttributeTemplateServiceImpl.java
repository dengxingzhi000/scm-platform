package com.cognora.platform.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.cognora.platform.product.domain.entity.ProdAttributeTemplate;
import com.cognora.platform.product.mapper.ProdAttributeTemplateMapper;
import com.cognora.platform.product.service.IProdAttributeTemplateService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ProdAttributeTemplateServiceImpl extends ServiceImpl<ProdAttributeTemplateMapper, ProdAttributeTemplate>
        implements IProdAttributeTemplateService {

    @Override
    public List<ProdAttributeTemplate> listByCategoryId(String categoryId) {
        log.debug("查询分类属性模板 categoryId={}", (categoryId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(ProdAttributeTemplate::getCategoryId, categoryId)
                .eq(ProdAttributeTemplate::getDeleted, false)
                .list();
    }

    @Override
    public Page<ProdAttributeTemplate> pageByCategoryId(String categoryId, Integer page, Integer size) {
        return lambdaQuery()
                .eq(categoryId != null, ProdAttributeTemplate::getCategoryId, categoryId)
                .eq(ProdAttributeTemplate::getDeleted, false)
                .orderByDesc(ProdAttributeTemplate::getCreateTime)
                .page(new Page<>(page, size));
    }

    @Override
    public ProdAttributeTemplate createTemplate(ProdAttributeTemplate template) {
        template.setId(UUID.randomUUID().toString());
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        template.setDeleted(false);
        save(template);
        return template;
    }

    @Override
    public void updateTemplate(String id, ProdAttributeTemplate template) {
        template.setId(id);
        template.setUpdateTime(LocalDateTime.now());
        updateById(template);
    }

    @Override
    public void deleteTemplate(String id) {
        lambdaUpdate()
                .eq(ProdAttributeTemplate::getId, id)
                .set(ProdAttributeTemplate::getDeleted, true)
                .set(ProdAttributeTemplate::getUpdateTime, LocalDateTime.now())
                .update();
    }
}
