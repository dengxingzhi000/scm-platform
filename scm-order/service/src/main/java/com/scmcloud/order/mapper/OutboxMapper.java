package com.scmcloud.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.order.domain.entity.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * MyBatis mapper for {@link OutboxEvent}.
 * <p>Provides polling for unpublished events and mark-published support.
 * Used in same-TX writes via {@code OrdOrderCommandService}.</p>
 */
@Mapper
public interface OutboxMapper extends BaseMapper<OutboxEvent> {

    @Select("SELECT * FROM outbox_event WHERE published = false ORDER BY created_at ASC LIMIT #{limit} FOR UPDATE SKIP LOCKED")
    java.util.List<OutboxEvent> findUnpublished(@Param("limit") int limit);

    @Update("UPDATE outbox_event SET published = true, published_at = now() WHERE id = #{id}")
    int markPublished(@Param("id") String id);

    @Select("SELECT COUNT(*) FROM outbox_event WHERE aggregate_type = #{aggregateType} AND aggregate_id = #{aggregateId}")
    long countByAggregate(@Param("aggregateType") String aggregateType,
                          @Param("aggregateId") String aggregateId);
}
