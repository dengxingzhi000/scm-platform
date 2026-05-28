package scm.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import scm.product.domain.entity.ProdSpu;

import java.util.List;

public interface IProdSpuService extends IService<ProdSpu> {

    List<ProdSpu> listByCategoryId(String categoryId);

    List<ProdSpu> listByBrandId(String brandId);

    Page<ProdSpu> search(String keyword, String categoryId, String brandId, Integer status,
                         Integer page, Integer size);
}
