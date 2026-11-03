package com.scmcloud.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.purchase.domain.entity.PurOrder;
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
