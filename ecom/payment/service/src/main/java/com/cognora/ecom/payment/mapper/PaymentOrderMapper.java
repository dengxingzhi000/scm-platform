package com.cognora.ecom.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.ecom.payment.domain.entity.PaymentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
}
