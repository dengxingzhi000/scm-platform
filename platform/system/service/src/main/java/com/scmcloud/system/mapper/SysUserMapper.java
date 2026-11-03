package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 鐢ㄦ埛锟組apper 鎺ュ彛
 * <p>
 * 娉ㄦ剰锛氭 Mapper 鍙锟絛b_user 搴撲腑锟絪ys_user 锟?
 * 娑夊強瑙掕壊銆佹潈闄愮殑鏌ヨ璇蜂娇鐢ㄥ搴旂殑 Mapper 锟絊ervice 灞傝仛锟?
 *
 * @author Deng
 * @since 2025-11-03
 */
@Mapper
@DS("user")
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 鏍规嵁鐢ㄦ埛鍚嶆煡璇㈢敤锟?
     */
    @Select("""
            SELECT * FROM sys_user
            WHERE username = #{username} AND NOT deleted
            """)
    SysUser findByUsername(@Param("username") String username);

    /**
     * 妫€鏌ョ敤鎴峰悕鏄惁瀛樺湪
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_user
            WHERE username = #{username} AND NOT deleted
            """)
    boolean existsByUsername(@Param("username") String username);

    /**
     * 鏇存柊鏈€鍚庣櫥褰曚俊锟?
     */
    @Update("""
            UPDATE sys_user SET
                last_login_time = #{loginTime},
                last_login_ip = #{ipAddress},
                login_attempts = 0
            WHERE id = #{userId}
            """)
    void updateLastLogin(@Param("userId") UUID userId,
                         @Param("ipAddress") String ipAddress,
                         @Param("loginTime") LocalDateTime loginTime);

    /**
     * 澧炲姞鐧诲綍灏濊瘯娆℃暟
     */
    @Update("""
            UPDATE sys_user SET login_attempts = login_attempts + 1
            WHERE username = #{username}
            """)
    void incrementLoginAttempts(@Param("username") String username);

    /**
     * 閿佸畾璐︽埛
     */
    @Update("""
            UPDATE sys_user
            SET status = 2,
                locked_until = #{lockedUntil}
            WHERE username = #{username}
            """)
    void lockAccount(@Param("username") String username,
                     @Param("lockedUntil") LocalDateTime lockedUntil);

    /**
     * 鏍规嵁鐢ㄦ埛 ID鍒楄〃鎵归噺鏌ヨ鐢ㄦ埛鍩烘湰淇℃伅
     */
    @Select("""
            <script>
            SELECT id, username, real_name, email, dept_id FROM sys_user
            WHERE id IN
            <foreach collection='userIds' item='id' open='(' close=')' separator=','>
                #{id}
            </foreach>
            AND NOT deleted
            </script>
            """)
    List<SysUser> selectBasicInfoByIds(@Param("userIds") List<UUID> userIds);

    /**
     * 鏍规嵁閮ㄩ棬 ID鏌ヨ鐢ㄦ埛ID鍒楄〃
     */
    @Select("""
            SELECT id FROM sys_user
            WHERE dept_id = #{deptId} AND NOT deleted
            """)
    List<UUID> findUserIdsByDeptId(@Param("deptId") UUID deptId);

    /**
     * 鑾峰彇鐢ㄦ埛鐨勯儴锟絀D
     */
    @Select("""
            SELECT dept_id FROM sys_user
            WHERE id = #{userId} AND NOT deleted
            """)
    UUID getUserDeptId(@Param("userId") UUID userId);

    /**
     * 鎵归噺缁熻澶氫釜閮ㄩ棬鐨勭敤鎴锋暟锟?
     * <p>
     * 鐢ㄤ簬浼樺寲 getDeptTree 绛夐渶瑕佺粺璁″涓儴闂ㄧ敤鎴锋暟鐨勫満锟?
     */
    @Select("""
            <script>
            SELECT dept_id, COUNT(*) as user_count FROM sys_user
            WHERE dept_id IN
            <foreach collection='deptIds' item='deptId' open='(' close=')' separator=','>
                #{deptId}
            </foreach>
            AND NOT deleted
            GROUP BY dept_id
            </script>
            """)
    @MapKey("dept_id")
    Map<UUID, Map<String, Object>> countUsersByDeptIds(@Param("deptIds") List<UUID> deptIds);

    /**
     * 缁熻鍗曚釜閮ㄩ棬鐨勭敤鎴锋暟
     */
    @Select("""
            SELECT COUNT(*) FROM sys_user
            WHERE dept_id = #{deptId} AND NOT deleted
            """)
    int countUsersByDeptId(@Param("deptId") UUID deptId);
}
