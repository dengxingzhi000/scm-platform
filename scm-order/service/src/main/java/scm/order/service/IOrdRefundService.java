package scm.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.order.domain.entity.OrdRefund;

import java.util.List;

/**
 * <p>
 * 退款/退货表 服务类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdRefundService extends IService<OrdRefund> {

    OrdRefund createRefund(OrdRefund refund);

    List<OrdRefund> listByOrderId(Long orderId);
}
