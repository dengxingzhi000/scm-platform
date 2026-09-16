package com.scmcloud.product.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.scmcloud.product.domain.entity.ProdAttributeTemplate;

import java.util.List;

public interface IProdAttributeTemplateService extends IService<ProdAttributeTemplate> {

    List<ProdAttributeTemplate> listByCategoryId(String categoryId);
}
