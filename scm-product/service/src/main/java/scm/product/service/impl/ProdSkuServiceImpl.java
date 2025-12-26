package scm.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.product.domain.entity.ProdSku;
import scm.product.mapper.ProdSkuMapper;
import scm.product.service.IProdSkuService;

/**
 * <p>
 * SKU库存单位表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Service
public class ProdSkuServiceImpl extends ServiceImpl<ProdSkuMapper, ProdSku> implements IProdSkuService {

}
