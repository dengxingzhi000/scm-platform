package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.SysTempPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.UUID;

/**
 * 涓存椂鏉冮檺锟組apper 鎺ュ彛
 *
 * @author Deng
 * @since 2025-12-17
 */
@Mapper
@DS("permission")
public interface SysTempPermissionMapper extends BaseMapper<SysTempPermission> {

    /**
     * 鏌ヨ鐢ㄦ埛鏈夋晥鐨勪复鏃舵潈锟?
     */
    @Select("""
            SELECT * FROM sys_temp_permission
            WHERE user_id = #{userId}
              AND status = 1
              AND effective_time <= NOW()
              AND expire_time > NOW()
            """)
    List<SysTempPermission> findEffectiveByUserId(@Param("userId") UUID userId);

    /**
     * 鏌ヨ鐢ㄦ埛鏈夋晥鐨勪复鏃舵潈锟絀D 鍒楄〃
     */
    @Select("""
            SELECT permission_id FROM sys_temp_permission
            WHERE user_id = #{userId}
              AND status = 1
              AND effective_time <= NOW()
              AND expire_time > NOW()
            """)
    List<UUID> findEffectivePermissionIdsByUserId(@Param("userId") UUID userId);

    /**
     * 鏌ヨ鍗冲皢杩囨湡鐨勪复鏃舵潈闄愶紙鐢ㄤ簬娓呯悊浠诲姟锟?
     */
    @Select("""
            SELECT * FROM sys_temp_permission
            WHERE status = 1
              AND expire_time <= NOW()
            """)
    List<SysTempPermission> findExpired();

    /**
     * 绂佺敤杩囨湡鐨勪复鏃舵潈锟?
     */
    @Update("""
            UPDATE sys_temp_permission
            SET status = 0
            WHERE status = 1
              AND expire_time <= NOW()
            """)
    int disableExpired();

    /**
     * 鏍规嵁瀹℃壒 ID 鏌ヨ涓存椂鏉冮檺
     */
    @Select("""
            SELECT * FROM sys_temp_permission
            WHERE approval_id = #{approvalId}
            """)
    List<SysTempPermission> findByApprovalId(@Param("approvalId") UUID approvalId);

    /**
     * 缁熻姝e湪浣跨敤鎸囧畾鏉冮檺鐨勪复鏃舵巿鏉冩暟锟?
     * <p>
     * 鐢ㄤ簬鏉冮檺鍒犻櫎鍓嶆鏌ワ紝闃叉鎰忓鍒犻櫎姝e湪琚娇鐢ㄧ殑鏉冮檺
     */
    @Select("""
            SELECT COUNT(*) FROM sys_temp_permission
            WHERE permission_id = #{permissionId}
              AND status = 1
              AND expire_time > NOW()
            """)
    Integer countActiveByPermissionId(@Param("permissionId") UUID permissionId);
}