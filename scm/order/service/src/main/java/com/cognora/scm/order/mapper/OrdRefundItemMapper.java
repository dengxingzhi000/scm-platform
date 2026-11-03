package com.cognora.scm.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.scm.order.domain.entity.OrdRefundItem;
import org.apache.ibatis.annotations.Mapper;
import java.util.UUID;

@Mapper
public interface OrdRefundItemMapper extends BaseMapper<OrdRefundItem> {
}
