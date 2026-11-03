package com.cognora.scm.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.scm.purchase.domain.entity.PurOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 采购订单 Mapper。
 *
 * @author deng
 * @since 2025-12-25
 */
@Mapper
public interface PurOrderMapper extends BaseMapper<PurOrder> {
}
