package scm.order.service.impl;

-order/service.service.impl;

import scm-order/service.domain.entity.OrdOrderItem;
import scm-order/service.mapper.OrdOrderItemMapper;
import scm-order/service.service.IOrdOrderItemService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单明细表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class OrdOrderItemServiceImpl extends ServiceImpl<OrdOrderItemMapper, OrdOrderItem> implements scm.IOrdOrderItemService {

}
