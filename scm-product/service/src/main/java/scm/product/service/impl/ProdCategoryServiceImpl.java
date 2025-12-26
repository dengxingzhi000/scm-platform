package scm.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.product.domain.entity.ProdCategory;
import scm.product.mapper.ProdCategoryMapper;
import scm.product.service.IProdCategoryService;

/**
 * <p>
 * 商品分类表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Service
public class ProdCategoryServiceImpl extends ServiceImpl<ProdCategoryMapper, ProdCategory>
        implements IProdCategoryService {

}
