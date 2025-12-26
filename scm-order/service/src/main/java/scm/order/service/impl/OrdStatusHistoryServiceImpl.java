package scm.order.service.impl;

-order/service.service.impl;

import scm-order/service.domain.entity.OrdStatusHistory;
import scm-order/service.mapper.OrdStatusHistoryMapper;
import scm-order/service.service.IOrdStatusHistoryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 订单状态流转历史 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class OrdStatusHistoryServiceImpl extends ServiceImpl<OrdStatusHistoryMapper, OrdStatusHistory> implements scm.IOrdStatusHistoryService {

}
