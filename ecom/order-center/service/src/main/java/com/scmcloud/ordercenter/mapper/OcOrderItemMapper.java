package com.scmcloud.ordercenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.ordercenter.domain.entity.OcOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OcOrderItemMapper extends BaseMapper<OcOrderItem> {
}
