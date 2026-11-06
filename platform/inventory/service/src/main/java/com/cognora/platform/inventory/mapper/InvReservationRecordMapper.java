package com.cognora.platform.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.inventory.domain.entity.InvReservationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 预占记录 Mapper。
 *
 * <p>状态流转全部使用带 {@code status = 'RESERVED'} 条件的单条 UPDATE，
 * 由数据库保证“只有一个线程能把 RESERVED 推向终态”，因此重复调用
 * confirm/release/expire 不会重复扣减或重复归还。
 *
 * <p>租户隔离由 {@code TenantInterceptor} 在 SQL 层追加 tenant_id 条件；
 * 这里再显式带上 tenant_id 参数，作为双重保险。
 *
 * @author SCM Platform Team
 * @since 2026-11-04
 */
@Mapper
public interface InvReservationRecordMapper extends BaseMapper<InvReservationRecord> {

    @Select("SELECT * FROM inv_reservation_record "
            + "WHERE tenant_id = #{tenantId} AND business_key = #{businessKey} LIMIT 1")
    InvReservationRecord selectByBusinessKey(@Param("tenantId") String tenantId,
                                             @Param("businessKey") String businessKey);

    @Update("UPDATE inv_reservation_record SET status = 'CONFIRMED', confirmed_at = NOW(), "
            + "update_time = NOW() WHERE id = #{id} AND tenant_id = #{tenantId} "
            + "AND status = 'RESERVED'")
    int markConfirmed(@Param("id") String id, @Param("tenantId") String tenantId);

    @Update("UPDATE inv_reservation_record SET status = 'RELEASED', released_at = NOW(), "
            + "update_time = NOW() WHERE id = #{id} AND tenant_id = #{tenantId} "
            + "AND status = 'RESERVED'")
    int markReleased(@Param("id") String id, @Param("tenantId") String tenantId);

    @Update("UPDATE inv_reservation_record SET status = 'EXPIRED', released_at = NOW(), "
            + "update_time = NOW() WHERE id = #{id} AND tenant_id = #{tenantId} "
            + "AND status = 'RESERVED' AND expire_at <= NOW()")
    int markExpired(@Param("id") String id, @Param("tenantId") String tenantId);

    @Select("SELECT * FROM inv_reservation_record WHERE status = 'RESERVED' "
            + "AND expire_at <= NOW() ORDER BY expire_at LIMIT #{limit}")
    List<InvReservationRecord> selectExpired(@Param("limit") int limit);
}
