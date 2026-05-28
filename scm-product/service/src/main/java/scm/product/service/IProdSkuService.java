package scm.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.product.domain.entity.ProdSku;

import java.util.List;

public interface IProdSkuService extends IService<ProdSku> {

    List<ProdSku> listBySpuId(String spuId);

    List<ProdSku> listBySpuIds(List<String> spuIds);
}
