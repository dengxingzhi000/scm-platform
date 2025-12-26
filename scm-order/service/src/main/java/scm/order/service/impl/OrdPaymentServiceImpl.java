package scm.order.service.impl;

-order/service.service.impl;

import scm-order/service.domain.entity.OrdPayment;
import scm-order/service.mapper.OrdPaymentMapper;
import scm-order/service.service.IOrdPaymentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 支付记录表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
@Service
public class OrdPaymentServiceImpl extends ServiceImpl<OrdPaymentMapper, OrdPayment> implements scm.IOrdPaymentService {

}
