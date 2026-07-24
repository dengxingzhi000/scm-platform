package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysRole;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * <p>
 * 瑙掕壊锟組apper 鎺ュ彛
 * </p>
 *
 * @author author
 * @since 2025-10-15
 */
@Mapper
@DS("permission")
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 妫€鏌ヨ鑹茬紪鐮佹槸鍚﹀瓨鍦紙涓嶈€冭檻绉熸埛锟?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_role
            WHERE role_code = #{roleCode} AND NOT deleted
            """)
    boolean existsByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 妫€鏌ヨ鑹茬紪鐮佸湪鎸囧畾绉熸埛涓嬫槸鍚﹀瓨锟?
     * 鐢ㄤ簬澶氱鎴风幆澧冧笅鐨勫敮涓€鎬ф牎锟?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_role
            WHERE role_code = #{roleCode}
              AND (tenant_id = #{tenantId} OR (tenant_id IS NULL AND #{tenantId} IS NULL))
              AND NOT deleted
            """)
    boolean existsByRoleCodeAndTenantId(@Param("roleCode") String roleCode, @Param("tenantId") UUID tenantId);

    /**
     * 鏍规嵁瑙掕壊缂栫爜鏌ヨ瑙掕壊 ID
     */
    @Select("""
            SELECT id FROM sys_role
            WHERE role_code = #{roleCode} AND status = 1 AND NOT deleted
            """)
    UUID findIdByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 鏍规嵁瑙掕壊缂栫爜鏌ヨ瑙掕壊
     */
    @Select("""
            SELECT * FROM sys_role
            WHERE role_code = #{roleCode} AND NOT deleted
            """)
    SysRole findByRoleCode(@Param("roleCode") String roleCode);

    /**
     * 鑾峰彇瑙掕壊绛夌骇
     * <p>
     * role_level 瀛楁鍊艰秺灏忥紝鏉冮檺瓒婇珮
     *
     * @param roleId 瑙掕壊 ID
     * @return 瑙掕壊绛夌骇锛坮ole_level锛夛紝濡傛灉瑙掕壊涓嶅瓨鍦ㄥ垯杩斿洖 null
     */
    @Select("""
            SELECT role_level FROM sys_role
            WHERE id = #{roleId} AND NOT deleted
            """)
    Integer getRoleLevel(@Param("roleId") UUID roleId);

    /**
     * 鑾峰彇瑙掕壊鎵€灞炵殑绉熸埛 ID
     * <p>
     * 鐢ㄤ簬楠岃瘉瑙掕壊褰掑睘锛圢ULL 琛ㄧず骞冲彴瑙掕壊锟?
     *
     * @param roleId 瑙掕壊 ID
     * @return 绉熸埛 ID锛圢ULL 琛ㄧず骞冲彴瑙掕壊锟?
     */
    @Select("""
            SELECT tenant_id FROM sys_role
            WHERE id = #{roleId} AND NOT deleted
            """)
    UUID getRoleTenantId(@Param("roleId") UUID roleId);

    /**
     * 批量查询角色等级
     *
     * @param roleIds 角色 ID 列表
     * @return 角色 ID -> 等级的映射
     */
    @Select("""
            SELECT id, role_level FROM sys_role
            WHERE id IN
            <foreach collection="roleIds" item="roleId" open="(" separator="," close=")">
                #{roleId}
            </foreach>
            AND NOT deleted
            """)
    @MapKey("id")
    Map<UUID, Integer> getRoleLevelsByRoleIds(@Param("roleIds") List<UUID> roleIds);
}
