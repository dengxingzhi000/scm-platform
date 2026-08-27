package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdOrderItem;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 璁㈠崟鏄庣粏锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdOrderItemService extends IService<OrdOrderItem> {

    List<OrdOrderItem> listByOrderId(UUID orderId);
}
