package scm.purchase.service;

import com.baomidou.mybatisplus.extension.service.IService;
import scm.purchase.domain.entity.PurOrderItem;

import java.util.List;

public interface IPurOrderItemService extends IService<PurOrderItem> {

    List<PurOrderItem> listByOrderId(String orderId);

    boolean deleteByOrderId(String orderId);
}
