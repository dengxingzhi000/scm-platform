package scm.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.product.domain.entity.ProdBrand;

import java.util.List;

public interface IProdBrandService extends IService<ProdBrand> {

    List<ProdBrand> searchByName(String name);

    List<ProdBrand> listFeatured();
}
