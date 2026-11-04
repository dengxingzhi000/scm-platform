package com.cognora.platform.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.common.dto.permission.PermissionDTO;
import com.cognora.platform.system.domain.entity.SysPermission;
import org.apache.ibatis.annotations.*;

import java.io.Serializable;
import java.security.Permission;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * 鏉冮檺锟組apper 鎺ュ彛
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Mapper
@DS("permission")
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /**
     * 鏌ヨ鐢ㄦ埛鏉冮檺
     */
    @Select("""
            SELECT DISTINCT p.permission_code FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            AND p.status = 1 AND NOT p.deleted
            """)
    Set<String> findAllPermissionsByUserId(@Param("userId") UUID userId);

    /**
     * 鏌ヨ鐢ㄦ埛瑙掕壊
     */
    @Select("""
            SELECT DISTINCT r.role_code FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            AND r.status = 1 AND NOT r.deleted
            """)
    Set<String> findRolesByUserId(@Param("userId") UUID userId);

    /**
     * 鏌ヨ瑙掕壊鏉冮檺锟?
     */
    @Select("""
            SELECT * FROM sys_permission WHERE status = 1 AND NOT deleted
            ORDER BY sort_order ASC
            """)
    List<SysPermission> findPermissionTree();

    /**
     * 鏍规嵁瑙掕壊 ID鏌ヨ鏉冮檺
     */
    @Select("""
            SELECT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            WHERE rp.role_id = #{roleId} AND p.status = 1 AND NOT p.deleted
            """)
    List<Permission> findPermissionsByRoleId(@Param("roleId") UUID roleId);

    /**
     * 妫€鏌ヨ祫婧愭潈锟?
     */
    @Select("""
            <script>
            SELECT COUNT(*) > 0 FROM sys_user_role ur
            INNER JOIN sys_role_permission rp ON ur.role_id = rp.role_id
            INNER JOIN sys_permission p ON rp.permission_id = p.id
            WHERE ur.user_id = #{userId}
            AND p.permission_code = #{permission}
            AND p.status = 1
            </script>
            """)
    boolean checkResourcePermission(@Param("userId") UUID userId,
                                    @Param("resourceType") String resourceType,
                                    @Param("resourceId") Serializable resourceId,
                                    @Param("permission") String permission);

    /**
     * 鏌ヨ鐢ㄦ埛鑿滃崟锟?
     */
    @Select("""
            SELECT DISTINCT p.* FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            AND p.permission_type IN (1, 2)
            AND p.visible = true AND p.status = 1 AND NOT p.deleted
            ORDER BY p.sort_order ASC
            """)
    @Results({
            @Result(property = "id", column = "id"),
            @Result(property = "parentId", column = "parent_id"),
            @Result(property = "children", column = "id",
                    many = @Many(select = "findChildrenPermissions"))
    })
    List<PermissionDTO> findMenuTreeByUserId(@Param("userId") UUID userId);

    /**
     * 鏌ヨ瀛愭潈锟?
     */
    @Select("""
            SELECT * FROM sys_permission
            WHERE parent_id = #{parentId}
            AND permission_type IN (1, 2)
            AND visible = true AND status = 1 AND NOT deleted
            ORDER BY sort_order ASC
            """)
    List<PermissionDTO> findChildrenPermissions(@Param("parentId") UUID parentId);

    /**
     * 查询 URL 权限候选行：字面量精确命中，或带 {@code {var}} / Ant 通配符的模板行。
     *
     * <p>模板的最终匹配在 {@code ApiPermissionMatcher} 里完成（SQL 只做候选预筛，
     * 避免把模板匹配逻辑写进 SQL）。</p>
     */
    @Select("""
            SELECT permission_code, api_path, http_method FROM sys_permission
            WHERE status = 1 AND NOT deleted
            AND api_path IS NOT NULL
            AND (http_method = #{method} OR http_method = '*')
            AND (api_path = #{url}
                 OR api_path LIKE '%{%'
                 OR api_path LIKE '%*%')
            """)
    List<SysPermission> findApiPermissionCandidates(@Param("url") String url,
                                                    @Param("method") String method);

    /**
     * 妫€鏌ユ潈闄愮紪鐮佹槸鍚﹀瓨锟?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_permission
            WHERE permission_code = #{permissionCode} AND NOT deleted
            """)
    boolean existsByPermissionCode(@Param("permissionCode") String permissionCode);
}
