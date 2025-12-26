package scm.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.product.domain.entity.ProdAttributeTemplate;
import scm.product.mapper.ProdAttributeTemplateMapper;
import scm.product.service.IProdAttributeTemplateService;

/**
 * <p>
 * 商品属性模板表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Service
public class ProdAttributeTemplateServiceImpl extends ServiceImpl<ProdAttributeTemplateMapper, ProdAttributeTemplate>
        implements IProdAttributeTemplateService {

}
