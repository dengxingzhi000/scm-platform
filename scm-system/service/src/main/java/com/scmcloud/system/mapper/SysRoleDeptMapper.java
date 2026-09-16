package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysRoleDept;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * 瑙掕壊閮ㄩ棬鍏宠仈 Mapper 鎺ュ彛
 *
 * @author Deng
 * @since 2025-12-15
 */
@Mapper
@DS("permission")
public interface SysRoleDeptMapper extends BaseMapper<SysRoleDept> {

    /**
     * 鏍规嵁瑙掕壊 ID鏌ヨ閮ㄩ棬ID鍒楄〃
     */
    @Select("""
            SELECT dept_id FROM sys_role_dept
            WHERE role_id = #{roleId}
            """)
    List<UUID> findDeptIdsByRoleId(@Param("roleId") UUID roleId);

    /**
     * 鏍规嵁瑙掕壊ID鏌ヨ鍏宠仈淇℃伅锛堝寘鍚瓙閮ㄩ棬鏍囪锟?
     */
    @Select("""
            SELECT * FROM sys_role_dept
            WHERE role_id = #{roleId}
            """)
    List<SysRoleDept> findByRoleId(@Param("roleId") UUID roleId);

    // 娉ㄦ剰锛歠indAccessibleDeptIds 鏂规硶宸茬Щ锟絊ervice 灞傚疄锟?
    // 璇ユ柟娉曟秹鍙婅法搴撴煡璇紙permission + org锛夛紝闇€瑕侀€氳繃 Service 灞傝仛鍚堬細
    // 1. 鍏堥€氳繃 findByRoleId 鏌ヨ瑙掕壊閮ㄩ棬鍏宠仈锛堝寘锟絠nclude_children 鏍囪锟?
    // 2. 瀵逛簬 include_children=true 鐨勯儴闂紝閫氳繃 SysDeptMapper.selectDeptAndChildren 閫掑綊鏌ヨ
    // 3. 鍚堝苟鎵€鏈夐儴锟絀D

    /**
     * 鏌ヨ闇€瑕侀€掑綊瀛愰儴闂ㄧ殑閮ㄩ棬 ID 鍒楄〃
     */
    @Select("""
            SELECT dept_id FROM sys_role_dept
            WHERE role_id = #{roleId} AND include_children = true
            """)
    List<UUID> findDeptIdsWithChildren(@Param("roleId") UUID roleId);

    /**
     * 鏌ヨ涓嶉渶瑕侀€掑綊瀛愰儴闂ㄧ殑閮ㄩ棬 ID 鍒楄〃
     */
    @Select("""
            SELECT dept_id FROM sys_role_dept
            WHERE role_id = #{roleId} AND include_children = false
            """)
    List<UUID> findDeptIdsWithoutChildren(@Param("roleId") UUID roleId);

    /**
     * 鍒犻櫎瑙掕壊鐨勬墍鏈夐儴闂ㄥ叧锟?
     */
    @Delete("""
            DELETE FROM sys_role_dept
            WHERE role_id = #{roleId}
            """)
    int deleteByRoleId(@Param("roleId") UUID roleId);

    /**
     * 鍒犻櫎閮ㄩ棬鐨勬墍鏈夎鑹插叧锟?
     * <p>
     * 鐢ㄤ簬閮ㄩ棬鍒犻櫎鏃舵竻鐞嗗叧鑱旀暟锟?
     */
    @Delete("""
            DELETE FROM sys_role_dept
            WHERE dept_id = #{deptId}
            """)
    int deleteByDeptId(@Param("deptId") UUID deptId);

    /**
     * 鎵归噺鎻掑叆瑙掕壊閮ㄩ棬鍏宠仈
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_dept (id, role_id, dept_id, include_children, create_by, create_time) VALUES
            <foreach collection='deptIds' item='deptId' separator=','>
            (gen_random_uuid(), #{roleId}, #{deptId}, #{includeChildren}, #{createBy}, NOW())
            </foreach>
            </script>
            """)
    int batchInsert(@Param("roleId") UUID roleId,
                    @Param("deptIds") List<UUID> deptIds,
                    @Param("includeChildren") boolean includeChildren,
                    @Param("createBy") UUID createBy);

    /**
     * 鍒犻櫎瑙掕壊閮ㄩ棬鍏宠仈
     * <p>
     * 鐢ㄤ簬鍒犻櫎瑙掕壊鏃舵竻鐞嗚嚜瀹氫箟鏁版嵁鏉冮檺鍏宠仈
     */
    @Delete("""
            DELETE FROM sys_role_dept
            WHERE role_id = #{roleId}
            """)
    int deleteRoleDepts(@Param("roleId") UUID roleId);
}
