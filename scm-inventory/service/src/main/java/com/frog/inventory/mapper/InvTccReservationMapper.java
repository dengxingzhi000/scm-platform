package com.frog.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frog.inventory.domain.entity.InvTccReservation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 库存 TCC 预留记录 Mapper
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Mapper
public interface InvTccReservationMapper extends BaseMapper<InvTccReservation> {
}