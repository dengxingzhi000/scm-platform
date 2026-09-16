package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.scmcloud.system.domain.entity.SysDept;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 閮ㄩ棬锟組apper 鎺ュ彛
 * <p>
 * 娉ㄦ剰锛氭 Mapper 鍙锟絛b_org 搴撲腑锟絪ys_dept 锟?
 * 闇€瑕佽幏鍙栭儴闂ㄨ礋璐d汉淇℃伅鏃讹紝璇峰湪 Service 灞傝仛鍚堟煡锟?
 *
 * @author author
 * @since 2025-11-07
 */
@Mapper
@DS("org")
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 鏌ヨ鎵€鏈夐儴闂ㄥ垪琛紙涓嶅寘鍚礋璐d汉淇℃伅锟?
     * 璐熻矗浜轰俊鎭渶瑕佸湪 Service 灞傞€氳繃 SysUserMapper 鑱氬悎
     */
    @Select("""
            SELECT * FROM sys_dept
            WHERE NOT deleted
            ORDER BY sort_order, create_time
            """)
    List<SysDept> selectDeptList();

    /**
     * 閫掑綊鏌ヨ閮ㄩ棬鍙婂叾鎵€鏈夊瓙閮ㄩ棬 ID
     */
    @Select("""
            WITH RECURSIVE dept_tree AS (
                SELECT id FROM sys_dept
                WHERE id = #{deptId} AND NOT deleted
                UNION ALL
                SELECT d.id FROM sys_dept d
                INNER JOIN dept_tree dt ON d.parent_id = dt.id
                WHERE NOT d.deleted
            )
            SELECT id FROM dept_tree
            """)
    List<UUID> selectDeptAndChildren(@Param("deptId") UUID deptId);

    /**
     * 閫掑綊鏌ヨ澶氫釜閮ㄩ棬鍙婂叾鎵€鏈夊瓙閮ㄩ棬 ID
     */
    @Select("""
            <script>
            WITH RECURSIVE dept_tree AS (
                SELECT id FROM sys_dept
                WHERE id IN
                <foreach collection='deptIds' item='deptId' open='(' close=')' separator=','>
                    #{deptId}
                </foreach>
                AND NOT deleted
                UNION ALL
                SELECT d.id FROM sys_dept d
                INNER JOIN dept_tree dt ON d.parent_id = dt.id
                WHERE NOT d.deleted
            )
            SELECT DISTINCT id FROM dept_tree
            </script>
            """)
    List<UUID> selectDeptsAndChildren(@Param("deptIds") List<UUID> deptIds);

    /**
     * 缁熻瀛愰儴闂ㄦ暟
     */
    @Select("""
            SELECT COUNT(*) FROM sys_dept
            WHERE parent_id = #{deptId} AND NOT deleted
            """)
    Integer countChildren(@Param("deptId") UUID deptId);

    /**
     * 鎵归噺缁熻澶氫釜閮ㄩ棬鐨勫瓙閮ㄩ棬鏁伴噺
     * <p>
     * 鐢ㄤ簬浼樺寲 getDeptTree 绛夐渶瑕佺粺璁″涓儴闂ㄥ瓙閮ㄩ棬鏁扮殑鍦烘櫙锛岄伩锟絅+1 鏌ヨ
     */
    @Select("""
            <script>
            SELECT parent_id, COUNT(*) as child_count FROM sys_dept
            WHERE parent_id IN
            <foreach collection='deptIds' item='deptId' open='(' close=')' separator=','>
                #{deptId}
            </foreach>
            AND NOT deleted
            GROUP BY parent_id
            </script>
            """)
    @MapKey("parent_id")
    Map<UUID, Map<String, Object>> countChildrenByDeptIds(@Param("deptIds") List<UUID> deptIds);

    /**
     * 妫€鏌ラ儴闂ㄧ紪鐮佹槸鍚﹀瓨锟?
     */
    @Select("""
            <script>
            SELECT COUNT(*) > 0 FROM sys_dept
            WHERE dept_code = #{deptCode}
            AND NOT deleted
            <if test='excludeId != null'>
                AND id != #{excludeId}
            </if>
            </script>
            """)
    boolean existsByDeptCode(@Param("deptCode") String deptCode,
                             @Param("excludeId") UUID excludeId);

    default boolean existsByDeptCode(String deptCode) {
        return existsByDeptCode(deptCode, null);
    }

    /**
     * 鏌ヨ閮ㄩ棬璐熻矗锟絀D
     */
    @Select("""
            SELECT leader_id FROM sys_dept
            WHERE id = #{deptId} AND NOT deleted
            """)
    UUID getLeaderId(@Param("deptId") UUID deptId);

    /**
     * 鎵归噺鏌ヨ閮ㄩ棬鍚嶇О
     */
    @Select("""
            <script>
            SELECT id, dept_name FROM sys_dept
            WHERE id IN
            <foreach collection='deptIds' item='id' open='(' close=')' separator=','>
                #{id}
            </foreach>
            AND NOT deleted
            </script>
            """)
    List<Map<String, Object>> selectDeptNames(@Param("deptIds") List<UUID> deptIds);

    /**
     * 鏍规嵁閮ㄩ棬 ID鏌ヨ閮ㄩ棬淇℃伅
     */
    @Select("""
            SELECT * FROM sys_dept
            WHERE id = #{deptId} AND NOT deleted
            """)
    SysDept selectByDeptId(@Param("deptId") UUID deptId);

    /**
     * 鏌ヨ椤剁骇閮ㄩ棬鍒楄〃
     */
    @Select("""
            SELECT * FROM sys_dept
            WHERE parent_id IS NULL AND NOT deleted
            ORDER BY sort_order, create_time
            """)
    List<SysDept> selectTopDepts();

    /**
     * 鏌ヨ鎸囧畾閮ㄩ棬鐨勭洿鎺ュ瓙閮ㄩ棬
     */
    @Select("""
            SELECT * FROM sys_dept
            WHERE parent_id = #{parentId} AND NOT deleted
            ORDER BY sort_order, create_time
            """)
    List<SysDept> selectChildDepts(@Param("parentId") UUID parentId);

    /**
     * 鎵归噺鏌ヨ閮ㄩ棬璐熻矗锟絀D 鏄犲皠
     */
    @Select("""
            <script>
            SELECT id as dept_id, leader_id FROM sys_dept
            WHERE id IN
            <foreach collection='deptIds' item='id' open='(' close=')' separator=','>
                #{id}
            </foreach>
            AND NOT deleted AND leader_id IS NOT NULL
            </script>
            """)
    List<Map<String, Object>> selectLeaderIds(@Param("deptIds") List<UUID> deptIds);

    // ==================== 鍐椾綑瀛楁鍚屾锛堟暟鎹竴鑷存€э級 ====================

    /**
     * 鏇存柊璐熻矗浜哄啑浣欎俊锟?
     * 锟絛b_user.sys_user 鍙樻洿鏃惰皟鐢紙鏇存柊璇ョ敤鎴蜂綔涓鸿礋璐d汉鐨勬墍鏈夐儴闂級
     */
    @Update("""
            UPDATE sys_dept
            SET leader_name = #{leaderName},
                leader_phone = #{leaderPhone}
            WHERE leader_id = #{leaderId}
            """)
    int updateLeaderRedundancy(@Param("leaderId") UUID leaderId,
                               @Param("leaderName") String leaderName,
                               @Param("leaderPhone") String leaderPhone);

    /**
     * 鏌ヨ閮ㄩ棬鏍戯紙鍖呭惈鍐椾綑鐨勮礋璐d汉淇℃伅锛屾棤闇€璺ㄥ簱锟?
     */
    @Select("""
            SELECT id, parent_id, dept_code, dept_name, dept_type,
                   leader_id, leader_name, leader_phone,
                   phone, email, isolation_level, sort_order, status
            FROM sys_dept
            WHERE NOT deleted
            ORDER BY sort_order, create_time
            """)
    List<SysDept> selectDeptTreeWithLeader();

    /**
     * 鏌ヨ鎵€鏈夋湁璐熻矗浜虹殑閮ㄩ棬锛堢敤浜庡垵濮嬪寲鍚屾锟?
     */
    @Select("""
            SELECT id as dept_id, leader_id FROM sys_dept
            WHERE NOT deleted AND leader_id IS NOT NULL
            """)
    List<Map<String, Object>> selectAllLeaderIds();
}
