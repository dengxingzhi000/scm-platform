package com.scmcloud.fulfillment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.fulfillment.domain.entity.FulfillmentOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FulfillmentOrderMapper extends BaseMapper<FulfillmentOrder> {
}
