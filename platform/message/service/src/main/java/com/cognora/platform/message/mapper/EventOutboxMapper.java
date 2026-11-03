package com.cognora.platform.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.message.entity.EventOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.Date;
import java.util.List;

@Mapper
public interface EventOutboxMapper extends BaseMapper<EventOutbox> {
    
    @Select("SELECT * FROM sys_event_outbox WHERE status = 'PENDING' AND next_retry_at <= #{now} LIMIT #{limit}")
    List<EventOutbox> findPendingEvents(@Param("now") Date now, @Param("limit") int limit);
    
    @Select("SELECT * FROM sys_event_outbox WHERE status = 'RETRYING' AND next_retry_at <= #{now} LIMIT #{limit}")
    List<EventOutbox> findRetryingEvents(@Param("now") Date now, @Param("limit") int limit);
}
