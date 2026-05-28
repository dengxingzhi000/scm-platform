package scm.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.order.domain.entity.OrdPayment;

/**
 * <p>
 * 支付记录表 服务类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdPaymentService extends IService<OrdPayment> {

    OrdPayment createPayment(OrdPayment payment);

    boolean updatePaymentStatus(Long paymentId, Integer status);
}
