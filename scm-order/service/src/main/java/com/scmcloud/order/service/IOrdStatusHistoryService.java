package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdStatusHistory;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 璁㈠崟鐘舵€佹祦杞巻锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdStatusHistoryService extends IService<OrdStatusHistory> {

    List<OrdStatusHistory> listByOrderId(UUID orderId);
}
