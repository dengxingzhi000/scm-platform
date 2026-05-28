package scm.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.product.domain.entity.ProdAttributeTemplate;

import java.util.List;

public interface IProdAttributeTemplateService extends IService<ProdAttributeTemplate> {

    List<ProdAttributeTemplate> listByCategoryId(String categoryId);
}
