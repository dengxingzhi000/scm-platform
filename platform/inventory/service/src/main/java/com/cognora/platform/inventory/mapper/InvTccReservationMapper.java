package com.cognora.platform.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.inventory.domain.entity.InvTccReservation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 库存 TCC 预留记录 Mapper
 *
 * <p>状态流转使用带 {@code status = 'TRYING'} 条件的单条 UPDATE：
 * 只有 Try 记录能被推向 CONFIRMED / CANCELLED，重复调用幂等，
 * 非法流转（CONFIRMED→CANCELLED、CANCELLED→CONFIRMED）返回 0 行。
 *
 * <p>幂等键为 (tenant_id, business_key)，查询与更新都显式带 tenant_id。
 *
 * @author SCM Platform Team
 * @since 2025-12-26
 */
@Mapper
public interface InvTccReservationMapper extends BaseMapper<InvTccReservation> {

    @Select("SELECT * FROM inv_tcc_reservation "
            + "WHERE tenant_id = CAST(#{tenantId} AS UUID) AND business_key = #{businessKey} LIMIT 1")
    InvTccReservation selectByBusinessKey(@Param("tenantId") String tenantId,
                                          @Param("businessKey") String businessKey);

    @Update("UPDATE inv_tcc_reservation SET status = 'CONFIRMED', confirm_time = NOW(), "
            + "update_time = NOW() WHERE id = #{id} "
            + "AND tenant_id = CAST(#{tenantId} AS UUID) AND status = 'TRYING'")
    int markConfirmed(@Param("id") Long id, @Param("tenantId") String tenantId);

    @Update("UPDATE inv_tcc_reservation SET status = 'CANCELLED', cancel_time = NOW(), "
            + "update_time = NOW() WHERE id = #{id} "
            + "AND tenant_id = CAST(#{tenantId} AS UUID) AND status = 'TRYING'")
    int markCancelled(@Param("id") Long id, @Param("tenantId") String tenantId);
}
