package com.scmcloud.common.mybatisPlus.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 榛樿鏁版嵁鏉冮檺鏈嶅姟瀹炵幇
 * 浠庢暟鎹簱鏌ヨ鐢ㄦ埛鐨勮嚜瀹氫箟鏁版嵁鏉冮檺瑙勫垯
 *
 * @author Deng
 * @since 2025-12-15
 */
@Service
@Slf4j
@ConditionalOnMissingBean(DataPermissionService.class)
@RequiredArgsConstructor
public class DefaultDataPermissionService implements DataPermissionService {
    private final JdbcTemplate jdbcTemplate;

    /**
     * 鏌ヨ鐢ㄦ埛鐨勮嚜瀹氫箟鏁版嵁鏉冮檺閮ㄩ棬鍒楄〃
     * SQL: 閫氳繃鐢ㄦ埛ID -> 鐢ㄦ埛瑙掕壊 -> 瑙掕壊閮ㄩ棬鍏宠仈 鑾峰彇鍙闂儴锟?
     */
    @Override
    public List<UUID> findCustomDeptPermissions(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        try {
            String sql = """
                SELECT DISTINCT rd.dept_id
                FROM sys_role_dept rd
                INNER JOIN sys_user_role ur ON rd.role_id = ur.role_id
                WHERE ur.user_id = ?
                  AND ur.approval_status = 2
                  AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
                """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Object obj = rs.getObject("dept_id");
                if (obj instanceof UUID) {
                    return (UUID) obj;
                }
                return UUID.fromString(rs.getString("dept_id"));
            }, userId);

        } catch (Exception e) {
            log.warn("Failed to query custom data permissions for user {}: {}",
                    userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽湁鑷畾涔夋暟鎹潈闄愰厤锟?
     */
    @Override
    public boolean hasCustomDataPermission(UUID userId) {
        if (userId == null) {
            return false;
        }

        try {
            String sql = """
                SELECT COUNT(*) > 0
                FROM sys_role_dept rd
                INNER JOIN sys_user_role ur ON rd.role_id = ur.role_id
                WHERE ur.user_id = ?
                  AND ur.approval_status = 2
                  AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
                """;

            Boolean result = jdbcTemplate.queryForObject(sql, Boolean.class, userId);
            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            log.warn("Failed to check custom data permission for user {}: {}",
                    userId, e.getMessage());
            return false;
        }
    }
}
