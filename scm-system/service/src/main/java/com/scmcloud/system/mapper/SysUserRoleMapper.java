package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysUserRole;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 鐢ㄦ埛瑙掕壊鍏宠仈 Mapper 鎺ュ彛
 * <p>
 * 澶勭悊 db_permission 搴撲腑锟絪ys_user_role銆乻ys_role銆乻ys_permission 绛夎〃
 *
 * @author Deng
 * @since 2025-12-15
 */
@Mapper
@DS("permission")
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 鏌ヨ鐢ㄦ埛鐨勬湁鏁堣锟絀D 鍒楄〃
     */
    @Select("""
            SELECT role_id FROM sys_user_role
            WHERE user_id = #{userId}
              AND approval_status = 2
              AND (expire_time IS NULL OR expire_time > NOW())
            """)
    List<UUID> findEffectiveRoleIds(@Param("userId") UUID userId);

    /**
     * 鏌ヨ鐢ㄦ埛鐨勬湁鏁堣鑹茬紪鐮佸垪锟?
     */
    @Select("""
            SELECT r.role_code FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.status = 1 AND NOT r.deleted
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    Set<String> findRoleCodesByUserId(@Param("userId") UUID userId);

    /**
     * 鏌ヨ鐢ㄦ埛鐨勬湁鏁堣鑹诧紙鍖呭惈 ID 鍜屽悕绉帮級
     */
    @Select("""
            SELECT ur.role_id as id, r.role_code as code, r.role_name as name
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            AND NOT r.deleted
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    List<Map<String, Object>> findUserRolesWithNames(@Param("userId") UUID userId);

    /**
     * 鏌ヨ鐢ㄦ埛鐨勬墍鏈夎鑹插叧鑱旓紙鍖呮嫭杩囨湡鍜屽緟瀹℃壒鐨勶級
     */
    @Select("""
            SELECT * FROM sys_user_role
            WHERE user_id = #{userId}
            """)
    List<SysUserRole> findByUserId(@Param("userId") UUID userId);

    /**
     * 鏌ヨ鎷ユ湁鎸囧畾瑙掕壊鐨勭敤锟絀D 鍒楄〃
     */
    @Select("""
            SELECT user_id FROM sys_user_role
            WHERE role_id = #{roleId}
              AND approval_status = 2
              AND (expire_time IS NULL OR expire_time > NOW())
            """)
    List<UUID> findUserIdsByRoleId(@Param("roleId") UUID roleId);

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽嫢鏈夋寚瀹氳鑹诧紙鏈夋晥鐨勶級
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_user_role
            WHERE user_id = #{userId}
              AND role_id = #{roleId}
              AND approval_status = 2
              AND (expire_time IS NULL OR expire_time > NOW())
            """)
    boolean hasRole(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /**
     * 鏌ヨ鐢ㄦ埛鐨勬湁鏁堟潈闄愮紪鐮佸垪锟?
     */
    @Select("""
            SELECT DISTINCT p.permission_code FROM sys_permission p
            INNER JOIN sys_role_permission rp ON p.id = rp.permission_id
            INNER JOIN sys_user_role ur ON rp.role_id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND p.status = 1 AND NOT p.deleted
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    Set<String> findPermissionCodesByUserId(@Param("userId") UUID userId);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬暟鎹潈闄愯寖鍥达紙鍙栨渶灏忓€硷紝鍗虫渶澶ф潈闄愶級
     */
    @Select("""
            SELECT MIN(r.data_scope) FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.status = 1 AND NOT r.deleted
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    Integer getUserDataScope(@Param("userId") UUID userId);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬渶澶у鎵归噾锟?
     */
    @Select("""
            SELECT MAX(r.max_approval_amount) FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.status = 1 AND NOT r.deleted
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    BigDecimal getMaxApprovalAmount(@Param("userId") UUID userId);

    /**
     * 鏌ヨ鐢ㄦ埛鍗冲皢杩囨湡鐨勮鑹诧紙7澶╁唴锟?
     */
    @Select("""
            SELECT * FROM sys_user_role
            WHERE user_id = #{userId}
              AND approval_status = 2
              AND expire_time IS NOT NULL
              AND expire_time BETWEEN NOW() AND NOW() + INTERVAL '7 days'
            """)
    List<SysUserRole> findExpiringRolesByUserId(@Param("userId") UUID userId);

    /**
     * 鏌ヨ鎵€鏈夊凡杩囨湡鐨勮鑹插叧锟?
     */
    @Select("""
            SELECT * FROM sys_user_role
            WHERE approval_status = 2
              AND expire_time IS NOT NULL
              AND expire_time < NOW()
            """)
    List<SysUserRole> findAllExpiredRoles();

    /**
     * 鏌ヨ鐢ㄦ埛鐨勪复鏃舵巿鏉冨垪琛紙鍖呭惈瑙掕壊鍚嶇О锟?
     */
    @Select("""
            SELECT ur.id, ur.role_id, r.role_name,
                   ur.effective_time, ur.expire_time,
                   ur.approval_status, ur.create_time
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            AND ur.expire_time IS NOT NULL
            ORDER BY ur.create_time DESC
            """)
    List<Map<String, Object>> findTemporaryRolesByUserId(@Param("userId") UUID userId);

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽湁鐗瑰畾鐨勪复鏃惰锟?
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_user_role
            WHERE user_id = #{userId}
            AND role_id = #{roleId}
            AND expire_time IS NOT NULL
            AND expire_time > NOW()
            AND approval_status = 2
            """)
    boolean hasTemporaryRole(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /**
     * 寤堕暱涓存椂瑙掕壊鐨勮繃鏈熸椂锟?
     */
    @Update("""
            UPDATE sys_user_role
            SET expire_time = #{newExpireTime}
            WHERE user_id = #{userId}
            AND role_id = #{roleId}
            AND expire_time IS NOT NULL
            AND expire_time > NOW()
            """)
    int extendTemporaryRole(@Param("userId") UUID userId,
                            @Param("roleId") UUID roleId,
                            @Param("newExpireTime") LocalDateTime newExpireTime);

    /**
     * 鎻愬墠缁堟涓存椂鎺堟潈
     */
    @Update("""
            UPDATE sys_user_role
            SET approval_status = 3,
                expire_time = NOW()
            WHERE user_id = #{userId}
            AND role_id = #{roleId}
            AND expire_time IS NOT NULL
            AND expire_time > NOW()
            """)
    int terminateTemporaryRole(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /**
     * 鏌ヨ鍗冲皢杩囨湡鐨勮鑹诧紙鐢ㄤ簬鎻愰啋锛岃繑鍥炵敤鎴峰拰瑙掕壊淇℃伅锟?
     */
    @Select("""
            SELECT DISTINCT ur.user_id, ur.username, ur.role_id, r.role_name, ur.expire_time
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.expire_time IS NOT NULL
            AND ur.expire_time BETWEEN NOW() AND NOW() + make_interval(days => #{days})
            AND ur.approval_status = 2
            """)
    List<Map<String, Object>> findExpiringRolesForNotification(@Param("days") Integer days);

    /**
     * 鏌ヨ宸茶繃鏈熺殑瑙掕壊锛堢敤浜庢竻鐞嗭紝鍖呭惈鐢ㄦ埛淇℃伅锟?
     */
    @Select("""
            SELECT DISTINCT ur.user_id, ur.username, ur.role_id, r.role_name, ur.expire_time
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.expire_time < NOW()
            AND ur.approval_status = 2
            """)
    List<Map<String, Object>> findExpiredRolesForCleanup();

    /**
     * 鏌ヨ鐢ㄦ埛寰呭鎵圭殑瑙掕壊鐢宠
     */
    @Select("""
            SELECT ur.id, ur.role_id, r.role_name,
                   ur.effective_time, ur.expire_time,
                   ur.create_time
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            AND ur.approval_status = 0
            ORDER BY ur.create_time DESC
            """)
    List<Map<String, Object>> findPendingRoleApprovals(@Param("userId") UUID userId);

    /**
     * 鏇存柊瀹℃壒鐘讹拷
     */
    @Update("""
            UPDATE sys_user_role
            SET approval_status = #{status},
                approved_by = #{approvedBy},
                approved_time = NOW()
            WHERE id = #{id}
            """)
    int updateApprovalStatus(@Param("id") UUID id,
                             @Param("status") int status,
                             @Param("approvedBy") UUID approvedBy);

    /**
     * 鍒犻櫎鐢ㄦ埛鐨勬墍鏈夎鑹插叧锟?
     */
    @Delete("""
            DELETE FROM sys_user_role
            WHERE user_id = #{userId}
            """)
    int deleteByUserId(@Param("userId") UUID userId);

    /**
     * 鍒犻櫎瑙掕壊鐨勬墍鏈夌敤鎴峰叧锟?
     */
    @Delete("""
            DELETE FROM sys_user_role
            WHERE role_id = #{roleId}
            """)
    int deleteByRoleId(@Param("roleId") UUID roleId);

    /**
     * 鍒犻櫎鎸囧畾鐨勭敤鎴疯鑹插叧锟?
     */
    @Delete("""
            DELETE FROM sys_user_role
            WHERE user_id = #{userId} AND role_id = #{roleId}
            """)
    int deleteByUserIdAndRoleId(@Param("userId") UUID userId, @Param("roleId") UUID roleId);

    /**
     * 鎵归噺鎻掑叆鐢ㄦ埛瑙掕壊鍏宠仈锛堟案涔呮巿鏉冿級
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role (id, user_id, role_id, approval_status, create_by, create_time) VALUES
            <foreach collection='roleIds' item='roleId' separator=','>
            (gen_random_uuid(), #{userId}, #{roleId}, 2, #{createBy}, NOW())
            </foreach>
            </script>
            """)
    int batchInsert(@Param("userId") UUID userId,
                    @Param("roleIds") List<UUID> roleIds,
                    @Param("createBy") UUID createBy);

    /**
     * 鎵归噺鎻掑叆涓存椂鐢ㄦ埛瑙掕壊鍏宠仈
     */
    @Insert("""
            <script>
            INSERT INTO sys_user_role
            (id, user_id, role_id, approval_status, effective_time, expire_time, create_by, create_time)
            VALUES
            <foreach collection='roleIds' item='roleId' separator=','>
            (gen_random_uuid(), #{userId}, #{roleId}, 2, #{effectiveTime}, #{expireTime}, #{createBy}, NOW())
            </foreach>
            </script>
            """)
    int batchInsertTemporary(@Param("userId") UUID userId,
                             @Param("roleIds") List<UUID> roleIds,
                             @Param("effectiveTime") LocalDateTime effectiveTime,
                             @Param("expireTime") LocalDateTime expireTime,
                             @Param("createBy") UUID createBy);

    /**
     * 鎻掑叆涓存椂瑙掕壊鎺堟潈
     */
    @Insert("""
            INSERT INTO sys_user_role (id, user_id, role_id, effective_time, expire_time, approval_status, approved_by, approved_time, create_by, create_time)
            VALUES (gen_random_uuid(), #{userId}, #{roleId}, #{effectiveTime}, #{expireTime}, #{approvalStatus}, #{approvedBy}, #{approvedTime}, #{createBy}, NOW())
            """)
    int insertTemporary(SysUserRole userRole);

    // ==================== 杩囨湡娓呯悊 ====================

    /**
     * 鍒犻櫎宸茶繃鏈熺殑瑙掕壊鍏宠仈
     */
    @Delete("""
            DELETE FROM sys_user_role
            WHERE expire_time < NOW()
            AND approval_status = 2
            """)
    int deleteExpiredRoles();

    /**
     * 鏇存柊杩囨湡瑙掕壊鐘舵€佷负宸叉嫆锟?
     */
    @Update("""
            UPDATE sys_user_role
            SET approval_status = 3
            WHERE expire_time < NOW()
            AND approval_status = 2
            """)
    int updateExpiredRolesStatus();

    // ==================== 缁熻鏌ヨ ====================

    /**
     * 缁熻鐢ㄦ埛鐨勬湁鏁堣鑹叉暟锟?
     */
    @Select("""
            SELECT COUNT(*) FROM sys_user_role
            WHERE user_id = #{userId}
            AND approval_status = 2
            AND (expire_time IS NULL OR expire_time > NOW())
            """)
    Integer countUserRoles(@Param("userId") UUID userId);

    /**
     * 缁熻鐢ㄦ埛鐨勪复鏃惰鑹叉暟锟?
     */
    @Select("""
            SELECT COUNT(*) FROM sys_user_role
            WHERE user_id = #{userId}
            AND approval_status = 2
            AND expire_time IS NOT NULL
            AND expire_time > NOW()
            """)
    Integer countTemporaryRoles(@Param("userId") UUID userId);

    /**
     * 缁熻鍗冲皢杩囨湡鐨勪复鏃惰鑹叉暟锟?
     */
    @Select("""
            SELECT COUNT(*) FROM sys_user_role
            WHERE user_id = #{userId}
            AND approval_status = 2
            AND expire_time IS NOT NULL
            AND expire_time BETWEEN NOW() AND NOW() + make_interval(days => #{days})
            """)
    Integer countExpiringRoles(@Param("userId") UUID userId, @Param("days") Integer days);

    // ==================== 鍐椾綑瀛楁鍚屾锛堟暟鎹竴鑷存€э級 ====================

    /**
     * 鏇存柊鐢ㄦ埛鍐椾綑淇℃伅
     * 锟絛b_user.sys_user 鍙樻洿鏃惰皟锟?
     */
    @Update("""
            UPDATE sys_user_role
            SET username = #{username},
                real_name = #{realName},
                user_status = #{status}
            WHERE user_id = #{userId}
            """)
    int updateUserRedundancy(@Param("userId") UUID userId,
                             @Param("username") String username,
                             @Param("realName") String realName,
                             @Param("status") Integer status);

    /**
     * 鏇存柊鐢ㄦ埛鐘舵€佸啑浣欏瓧锟?
     */
    @Update("""
            UPDATE sys_user_role
            SET user_status = #{status}
            WHERE user_id = #{userId}
            """)
    int updateUserStatus(@Param("userId") UUID userId, @Param("status") Integer status);

    /**
     * 鑾峰彇鎵€鏈変笉閲嶅鐨勭敤鎴稩D锛堢敤浜庡垵濮嬪寲鍚屾锟?
     */
    @Select("""
            SELECT DISTINCT user_id FROM sys_user_role
            """)
    List<UUID> findAllDistinctUserIds();

    /**
     * 鏍规嵁鐢ㄦ埛鍚嶆煡璇㈢敤鎴疯鑹诧紙鍒╃敤鍐椾綑瀛楁锛屾棤闇€璺ㄥ簱锟?
     */
    @Select("""
            SELECT ur.*, r.role_code, r.role_name
            FROM sys_user_role ur
            INNER JOIN sys_role r ON ur.role_id = r.id
            WHERE ur.username = #{username}
            AND ur.user_status = 1
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            AND NOT r.deleted
            """)
    List<Map<String, Object>> findRolesByUsername(@Param("username") String username);

    /**
     * 缁熻鎷ユ湁璇ヨ鑹茬殑鐢ㄦ埛锟?
     */
    @Select("""
            SELECT COUNT(*) FROM sys_user_role
            WHERE role_id = #{roleId}
            """)
    Integer countUsersByRoleId(@Param("roleId") UUID roleId);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬渶澶ц鑹茬瓑锟?
     * <p>
     * role_level 瀛楁鍊艰秺灏忥紝鏉冮檺瓒婇珮
     * 杩斿洖鐢ㄦ埛鎵€鏈夋湁鏁堣鑹蹭腑 role_level 鐨勬渶灏忥拷
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏈€澶ц鑹茬瓑绾э紙鏈€灏忕殑 role_level 鍊硷級锛屽鏋滅敤鎴锋病鏈夎鑹插垯杩斿洖 null
     */
    @Select("""
            SELECT MIN(r.role_level) FROM sys_role r
            INNER JOIN sys_user_role ur ON r.id = ur.role_id
            WHERE ur.user_id = #{userId}
            AND r.status = 1 AND NOT r.deleted
            AND ur.approval_status = 2
            AND (ur.expire_time IS NULL OR ur.expire_time > NOW())
            """)
    Integer getUserMaxRoleLevel(@Param("userId") UUID userId);
}
