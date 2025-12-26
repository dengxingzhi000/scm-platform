package scm.order.service.impl;

-order/service.service.impl;

import scm-order/service.domain.entity.OrdRefund;
import scm-order/service.mapper.OrdRefundMapper;
import scm-order/service.service.IOrdRefundService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 退款/退货表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class OrdRefundServiceImpl extends ServiceImpl<OrdRefundMapper, OrdRefund> implements scm.IOrdRefundService {

}
