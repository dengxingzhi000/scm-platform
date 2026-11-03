package com.cognora.scm.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.cognora.scm.order.domain.entity.OrdOrderEvent;
import java.util.UUID;

@Mapper
public interface OrdOrderEventMapper extends BaseMapper<OrdOrderEvent> {
}
