package com.scmcloud.ordercenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.ordercenter.domain.entity.OcOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OcOrderMapper extends BaseMapper<OcOrder> {
}
