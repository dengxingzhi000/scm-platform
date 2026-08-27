package com.scmcloud.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.scmcloud.order.domain.entity.OrdPayment;
import java.util.UUID;

/**
 * <p>
 * 鏀粯璁板綍锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-25
 */
public interface IOrdPaymentService extends IService<OrdPayment> {

    OrdPayment createPayment(OrdPayment payment);

    boolean updatePaymentStatus(UUID paymentId, Integer status);
}
