package com.scmcloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.scmcloud.order.domain.entity.OrdOrder;
import java.util.UUID;

@Mapper
public interface OrdOrderMapper extends BaseMapper<OrdOrder> {
}
