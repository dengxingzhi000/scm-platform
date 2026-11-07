package com.cognora.platform.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.system.domain.entity.SysUserPasswordHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.UUID;

@Mapper
@DS("user")
public interface SysUserPasswordHistoryMapper extends BaseMapper<SysUserPasswordHistory> {

    /**
     * 最近 N 条密码哈希，按时间倒序。租户隔离由 MyBatis 租户插件补齐。
     */
    @Select("""
            SELECT password_hash FROM sys_user_password_history
            WHERE user_id = #{userId}
            ORDER BY create_time DESC
            LIMIT #{limit}
            """)
    List<String> selectRecentHashes(@Param("userId") UUID userId, @Param("limit") int limit);
}
