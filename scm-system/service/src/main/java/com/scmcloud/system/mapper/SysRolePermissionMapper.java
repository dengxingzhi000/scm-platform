package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysRolePermission;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * 瑙掕壊鏉冮檺鍏宠仈锟組apper 鎺ュ彛
 *
 * @author Deng
 * @since 2025-12-17
 */
@Mapper
@DS("permission")
public interface SysRolePermissionMapper extends BaseMapper<SysRolePermission> {

    /**
     * 鏍规嵁瑙掕壊 ID 鏌ヨ鏉冮檺 ID 鍒楄〃
     */
    @Select("""
            SELECT permission_id FROM sys_role_permission
            WHERE role_id = #{roleId}
            """)
    List<UUID> findPermissionIdsByRoleId(@Param("roleId") UUID roleId);

    /**
     * 鏍规嵁鏉冮檺 ID 鏌ヨ瑙掕壊 ID 鍒楄〃
     */
    @Select("""
            SELECT role_id FROM sys_role_permission
            WHERE permission_id = #{permissionId}
            """)
    List<UUID> findRoleIdsByPermissionId(@Param("permissionId") UUID permissionId);

    /**
     * 鍒犻櫎瑙掕壊鐨勬墍鏈夋潈闄愬叧锟?
     */
    @Delete("""
            DELETE FROM sys_role_permission
            WHERE role_id = #{roleId}
            """)
    int deleteByRoleId(@Param("roleId") UUID roleId);

    /**
     * 鍒犻櫎鏉冮檺鐨勬墍鏈夎鑹插叧锟?
     */
    @Delete("""
            DELETE FROM sys_role_permission
            WHERE permission_id = #{permissionId}
            """)
    int deleteByPermissionId(@Param("permissionId") UUID permissionId);

    /**
     * 妫€鏌ヨ鑹叉槸鍚︽嫢鏈夋寚瀹氭潈锟?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_role_permission
            WHERE role_id = #{roleId} AND permission_id = #{permissionId}
            """)
    boolean existsByRoleIdAndPermissionId(@Param("roleId") UUID roleId,
                                          @Param("permissionId") UUID permissionId);

    /**
     * 鍒犻櫎瑙掕壊鏉冮檺鍏宠仈
     */
    @Delete("""
            DELETE FROM sys_role_permission
            WHERE role_id = #{roleId}
            """)
    void deleteRolePermissions(@Param("roleId") UUID roleId);

    /**
     * 鎵归噺鎻掑叆瑙掕壊鏉冮檺
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_permission (role_id, permission_id, create_by, create_time) VALUES
            <foreach collection='permissionIds' item='permissionId' separator=','>
            (#{roleId}, #{permissionId}, #{createBy}, NOW())
            </foreach>
            </script>
            """)
    void batchInsertRolePermissions(@Param("roleId") UUID roleId,
                                    @Param("permissionIds") List<UUID> permissionIds,
                                    @Param("createBy") UUID createBy);

    /**
     * 缁熻浣跨敤璇ユ潈闄愮殑瑙掕壊锟?
     */
    @Select("""
            SELECT COUNT(*) FROM sys_role_permission WHERE permission_id = #{permissionId}
            """)
    Integer countRolesByPermissionId(@Param("permissionId") UUID permissionId);
}