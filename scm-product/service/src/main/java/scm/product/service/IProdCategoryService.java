package scm.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.product.domain.entity.ProdCategory;

import java.util.List;

public interface IProdCategoryService extends IService<ProdCategory> {

    List<ProdCategory> listByParentId(String parentId);

    List<ProdCategory> getCategoryTree();

    List<ProdCategory> searchByName(String name);
}
