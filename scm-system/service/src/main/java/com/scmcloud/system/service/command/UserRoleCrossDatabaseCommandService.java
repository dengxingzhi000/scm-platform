package com.scmcloud.system.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 鐢ㄦ埛瑙掕壊璺ㄥ簱鍛戒护鏈嶅姟
 * <p>
 * 澶勭悊鐢ㄦ埛瑙掕壊鍏宠仈鐨勫啓鎿嶄綔锛坉b_permission锟?
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleCrossDatabaseCommandService {
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 鎵归噺鎻掑叆鐢ㄦ埛瑙掕壊鍏宠仈锛堟案涔呮巿鏉冿級
     * <p>
     * 鏇夸唬 SysUserRoleMapper.batchInsert
     * 璺ㄥ簱鎿嶄綔锛歞b_permission
     *
     * @param userId   鐢ㄦ埛 ID
     * @param roleIds  瑙掕壊 ID 鍒楄〃
     * @param createBy 鍒涘缓锟絀D
     * @return 鎻掑叆琛屾暟
     */
    @Master(reason = "Write operation must use master database")
    @Transactional(rollbackFor = Exception.class)
    public int batchInsertUserRoles(UUID userId, List<UUID> roleIds, UUID createBy) {
        if (userId == null || roleIds == null || roleIds.isEmpty()) {
            return 0;
        }
        log.debug("Batch inserting user roles: userId={}, roleCount={}", userId, roleIds.size());
        return userRoleMapper.batchInsert(userId, roleIds, createBy);
    }

    /**
     * 鎵归噺鎻掑叆涓存椂鐢ㄦ埛瑙掕壊鍏宠仈
     * <p>
     * 鏇夸唬 SysUserRoleMapper.batchInsertTemporary
     * 璺ㄥ簱鎿嶄綔锛歞b_permission
     *
     * @param userId        鐢ㄦ埛 ID
     * @param roleIds       瑙掕壊 ID 鍒楄〃
     * @param effectiveTime 鐢熸晥鏃堕棿
     * @param expireTime    杩囨湡鏃堕棿
     * @param createBy      鍒涘缓锟絀D
     * @return 鎻掑叆琛屾暟
     */
    @Master(reason = "Write operation must use master database")
    @Transactional(rollbackFor = Exception.class)
    public int batchInsertTemporaryUserRoles(UUID userId, List<UUID> roleIds,
                                              LocalDateTime effectiveTime,
                                              LocalDateTime expireTime,
                                              UUID createBy) {
        if (userId == null || roleIds == null || roleIds.isEmpty()) {
            return 0;
        }
        log.debug("Batch inserting temporary user roles: userId={}, roleCount={}, expireTime={}",
                userId, roleIds.size(), expireTime);
        return userRoleMapper.batchInsertTemporary(userId, roleIds, effectiveTime, expireTime, createBy);
    }

    /**
     * 鍒犻櫎鐢ㄦ埛鐨勬墍鏈夎鑹插叧锟?
     * <p>
     * 鏇夸唬 SysUserRoleMapper.deleteByUserId
     * 璺ㄥ簱鎿嶄綔锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鍒犻櫎琛屾暟
     */
    @Master(reason = "Write operation must use master database")
    @Transactional(rollbackFor = Exception.class)
    public int deleteUserRoles(UUID userId) {
        if (userId == null) {
            return 0;
        }
        log.debug("Deleting user roles: userId={}", userId);
        return userRoleMapper.deleteByUserId(userId);
    }

    /**
     * 寤堕暱涓存椂瑙掕壊鏈夋晥锟?
     * <p>
     * 鏇夸唬 SysUserRoleMapper.extendTemporaryRole
     * 璺ㄥ簱鎿嶄綔锛歞b_permission
     *
     * @param userId        鐢ㄦ埛 ID
     * @param roleId        瑙掕壊 ID
     * @param newExpireTime 鏂扮殑杩囨湡鏃堕棿
     * @return 鏇存柊琛屾暟
     */
    @Master(reason = "Write operation must use master database")
    @Transactional(rollbackFor = Exception.class)
    public int extendTemporaryRole(UUID userId, UUID roleId, LocalDateTime newExpireTime) {
        if (userId == null || roleId == null || newExpireTime == null) {
            return 0;
        }
        log.debug("Extending temporary role: userId={}, roleId={}, newExpireTime={}",
                userId, roleId, newExpireTime);
        return userRoleMapper.extendTemporaryRole(userId, roleId, newExpireTime);
    }

    /**
     * 缁堟涓存椂瑙掕壊
     * <p>
     * 鏇夸唬 SysUserRoleMapper.terminateTemporaryRole
     * 璺ㄥ簱鎿嶄綔锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @param roleId 瑙掕壊 ID
     * @return 鏇存柊琛屾暟
     */
    @Master(reason = "Write operation must use master database")
    @Transactional(rollbackFor = Exception.class)
    public int terminateTemporaryRole(UUID userId, UUID roleId) {
        if (userId == null || roleId == null) {
            return 0;
        }
        log.debug("Terminating temporary role: userId={}, roleId={}", userId, roleId);
        return userRoleMapper.terminateTemporaryRole(userId, roleId);
    }
}