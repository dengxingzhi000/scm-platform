package scm.product.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import scm.product.domain.entity.ProdSpu;
import scm.product.mapper.ProdSpuMapper;
import scm.product.service.IProdSpuService;

/**
 * <p>
 * SPU标准产品单元表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-12-25
 */
@Service
public class ProdSpuServiceImpl extends ServiceImpl<ProdSpuMapper, ProdSpu> implements IProdSpuService {

}
