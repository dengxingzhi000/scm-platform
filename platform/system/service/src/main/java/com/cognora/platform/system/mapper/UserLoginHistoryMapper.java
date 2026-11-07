package com.cognora.platform.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.cognora.platform.common.dto.user.LoginHistoryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 登录历史查询（db_audit.sys_audit_log）。
 *
 * <p>强制带 create_time 范围以触发分区裁剪；tenant_id 由 MyBatis 租户插件补齐，
 * 这里仍显式传入，避免跨租户读取。</p>
 */
@Mapper
@DS("audit")
public interface UserLoginHistoryMapper {

    @Select("""
            <script>
            SELECT id, user_id, username, operation_type, status,
                   ip_address::text AS ip_address, error_msg,
                   (create_time AT TIME ZONE 'Asia/Shanghai') AS create_time
            FROM sys_audit_log
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
              AND operation_type IN ('LOGIN', 'LOGIN_FAILURE', 'LOGOUT')
              AND create_time &gt;= (#{startTime} AT TIME ZONE 'Asia/Shanghai')
              AND create_time &lt; (#{endTime} AT TIME ZONE 'Asia/Shanghai')
            <if test="operationType != null and operationType != ''">
              AND operation_type = #{operationType}
            </if>
            ORDER BY create_time DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<LoginHistoryDTO> selectPage(@Param("tenantId") UUID tenantId,
                                     @Param("userId") UUID userId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("operationType") String operationType,
                                     @Param("offset") long offset,
                                     @Param("limit") int limit);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_audit_log
            WHERE tenant_id = #{tenantId}
              AND user_id = #{userId}
              AND operation_type IN ('LOGIN', 'LOGIN_FAILURE', 'LOGOUT')
              AND create_time &gt;= (#{startTime} AT TIME ZONE 'Asia/Shanghai')
              AND create_time &lt; (#{endTime} AT TIME ZONE 'Asia/Shanghai')
            <if test="operationType != null and operationType != ''">
              AND operation_type = #{operationType}
            </if>
            </script>
            """)
    long count(@Param("tenantId") UUID tenantId,
               @Param("userId") UUID userId,
               @Param("startTime") LocalDateTime startTime,
               @Param("endTime") LocalDateTime endTime,
               @Param("operationType") String operationType);
}
