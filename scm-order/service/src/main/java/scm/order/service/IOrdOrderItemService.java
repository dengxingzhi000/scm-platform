package scm.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.order.domain.entity.OrdOrderItem;

import java.util.List;

/**
 * <p>
 * 订单明细表 服务类
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdOrderItemService extends IService<OrdOrderItem> {

    List<OrdOrderItem> listByOrderId(Long orderId);
}
