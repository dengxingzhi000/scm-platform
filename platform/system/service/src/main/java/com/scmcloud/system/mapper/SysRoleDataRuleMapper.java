package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysRoleDataRule;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.UUID;

/**
 * 瑙掕壊鏁版嵁鏉冮檺瑙勫垯鍏宠仈 Mapper 鎺ュ彛
 *
 * @author Deng
 * @since 2025-12-15
 */
@Mapper
@DS("permission")
public interface SysRoleDataRuleMapper extends BaseMapper<SysRoleDataRule> {

    /**
     * 鏍规嵁瑙掕壊 ID鏌ヨ瑙勫垯ID鍒楄〃
     */
    @Select("""
            SELECT rule_id FROM sys_role_data_rule
            WHERE role_id = #{roleId}
            """)
    List<UUID> findRuleIdsByRoleId(@Param("roleId") UUID roleId);

    /**
     * 鍒犻櫎瑙掕壊鐨勬墍鏈夎鍒欏叧锟?
     */
    @Delete("""
            DELETE FROM sys_role_data_rule
            WHERE role_id = #{roleId}
            """)
    int deleteByRoleId(@Param("roleId") UUID roleId);

    /**
     * 鍒犻櫎瑙勫垯鐨勬墍鏈夎鑹插叧锟?
     */
    @Delete("""
            DELETE FROM sys_role_data_rule
            WHERE rule_id = #{ruleId}
            """)
    int deleteByRuleId(@Param("ruleId") UUID ruleId);

    /**
     * 鎵归噺鎻掑叆瑙掕壊瑙勫垯鍏宠仈
     */
    @Insert("""
            <script>
            INSERT INTO sys_role_data_rule (id, role_id, rule_id, create_by, create_time) VALUES
            <foreach collection='ruleIds' item='ruleId' separator=','>
            (gen_random_uuid(), #{roleId}, #{ruleId}, #{createBy}, NOW())
            </foreach>
            </script>
            """)
    int batchInsert(@Param("roleId") UUID roleId,
                    @Param("ruleIds") List<UUID> ruleIds,
                    @Param("createBy") UUID createBy);

    /**
     * 妫€鏌ヨ鑹叉槸鍚﹀凡鍏宠仈瑙勫垯
     */
    @Select("""
            SELECT COUNT(*) > 0 FROM sys_role_data_rule
            WHERE role_id = #{roleId} AND rule_id = #{ruleId}
            """)
    boolean exists(@Param("roleId") UUID roleId, @Param("ruleId") UUID ruleId);

    /**
     * 鍒犻櫎瑙掕壊鏁版嵁鏉冮檺瑙勫垯鍏宠仈
     * <p>
     * 鐢ㄤ簬鍒犻櫎瑙掕壊鏃舵竻鐞嗘暟鎹潈闄愯鍒欏叧锟?
     */
    @Delete("""
            DELETE FROM sys_role_data_rule
            WHERE role_id = #{roleId}
            """)
    int deleteRoleDataRules(@Param("roleId") UUID roleId);
}
