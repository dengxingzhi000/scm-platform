package com.scmcloud.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.product.domain.entity.ProdAttributeTemplate;

import java.util.List;

public interface IProdAttributeTemplateService extends IService<ProdAttributeTemplate> {

    List<ProdAttributeTemplate> listByCategoryId(String categoryId);

    Page<ProdAttributeTemplate> pageByCategoryId(String categoryId, Integer page, Integer size);

    ProdAttributeTemplate createTemplate(ProdAttributeTemplate template);

    void updateTemplate(String id, ProdAttributeTemplate template);

    void deleteTemplate(String id);
}
