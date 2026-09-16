package com.scmcloud.system.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 鐢ㄦ埛璺ㄥ簱鏌ヨ鏈嶅姟
 * <p>
 * 澶勭悊涓庣敤鎴风浉鍏崇殑璺ㄥ簱鏌ヨ鎿嶄綔锛坉b_user 锟絛b_permission 锟絛b_org锟?
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCrossDatabaseQueryService {
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 鑾峰彇鐢ㄦ埛鍩烘湰淇℃伅
     * <p>
     * 鏇夸唬 SysUserMapper.selectById
     * 璺ㄥ簱鏌ヨ锛歞b_user
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鐢ㄦ埛瀹炰綋
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getUserBasicInfo"})
    public SysUser getUserBasicInfo(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(userId);
    }

    /**
     * 鎵归噺鑾峰彇鐢ㄦ埛鍩烘湰淇℃伅
     * <p>
     * 鏇夸唬 SysUserMapper.selectBasicInfoByIds
     * 璺ㄥ簱鏌ヨ锛歞b_user
     *
     * @param userIds 鐢ㄦ埛 ID 鍒楄〃
     * @return 鐢ㄦ埛瀹炰綋鍒楄〃
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getUserBasicInfoBatch"})
    public List<SysUser> getUserBasicInfoBatch(List<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return userMapper.selectBasicInfoByIds(userIds);
    }

    /**
     * 鎵归噺鑾峰彇鐢ㄦ埛鍩烘湰淇℃伅锛圡ap 褰㈠紡锟?
     *
     * @param userIds 鐢ㄦ埛 ID 鍒楄〃
     * @return 鐢ㄦ埛 ID 锟界敤鎴峰疄浣?鏄犲皠
     */
    @Slave
    public Map<UUID, SysUser> getUserBasicInfoMap(List<UUID> userIds) {
        List<SysUser> users = getUserBasicInfoBatch(userIds);
        return users.stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
    }

    /**
     * 鏌ヨ鐢ㄦ埛瑙掕壊锛堝甫瑙掕壊鍚嶇О锟?
     * <p>
     * 鏇夸唬 SysUserRoleMapper.findUserRolesWithNames
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 瑙掕壊鍒楄〃锛堝寘锟絠d, name 瀛楁锟?
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findUserRolesWithNames"})
    @Cacheable(value = "userRoles", key = "#userId", unless = "#result.isEmpty()")
    public List<Map<String, Object>> findUserRolesWithNames(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userRoleMapper.findUserRolesWithNames(userId);
    }

    /**
     * 鏌ヨ鐢ㄦ埛瑙掕壊缂栫爜闆嗗悎
     * <p>
     * 鏇夸唬 SysUserRoleMapper.findRoleCodesByUserId
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 瑙掕壊缂栫爜闆嗗悎
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findRoleCodesByUserId"})
    @Cacheable(value = "userRoleCodes", key = "#userId", unless = "#result.isEmpty()")
    public Set<String> findRoleCodesByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return userRoleMapper.findRoleCodesByUserId(userId);
    }

    /**
     * 鑾峰彇鐢ㄦ埛鐨勬渶澶ц鑹茬瓑锟?
     * <p>
     * 鐢ㄤ簬瑙掕壊鎺堟潈鏃剁殑鏉冮檺妫€鏌ワ紙鍙兘鍒嗛厤涓嶉珮浜庤嚜宸辩殑瑙掕壊锟?
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏈€澶ц鑹茬瓑绾э紙role_level 鏈€灏忓€硷紝鍥犱负绛夌骇瓒婂皬鏉冮檺瓒婇珮锛夛紝濡傛灉鐢ㄦ埛娌℃湁瑙掕壊鍒欒繑锟絥ull
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getUserMaxRoleLevel"})
    public Integer getUserMaxRoleLevel(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRoleMapper.getUserMaxRoleLevel(userId);
    }

    /**
     * 缁熻鐢ㄦ埛鏈夋晥瑙掕壊锟?
     * <p>
     * 鏇夸唬 SysUserRoleMapper.countUserRoles
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏈夋晥瑙掕壊锟?
     */
    @Slave
    public Integer countUserRoles(UUID userId) {
        return getCountOrDefault(userId, userRoleMapper::countUserRoles);
    }

    /**
     * 鏌ヨ鐢ㄦ埛鏉冮檺缂栫爜闆嗗悎
     * <p>
     * 鏇夸唬 SysUserRoleMapper.findPermissionCodesByUserId
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏉冮檺缂栫爜闆嗗悎
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findPermissionCodesByUserId"})
    @Cacheable(value = "userPermissionCodes", key = "#userId", unless = "#result.isEmpty()")
    public Set<String> findPermissionCodesByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        return userRoleMapper.findPermissionCodesByUserId(userId);
    }

    /**
     * 鑾峰彇鐢ㄦ埛鏁版嵁鏉冮檺鑼冨洿
     * <p>
     * 鏇夸唬 SysUserRoleMapper.getUserDataScope
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏁版嵁鏉冮檺鑼冨洿锟?鍏ㄩ儴, 2-鑷畾锟?3-鏈儴锟?4-鏈儴闂ㄥ強瀛愰儴锟?5-浠呮湰浜猴級
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getUserDataScope"})
    @Cacheable(value = "userDataScope", key = "#userId")
    public Integer getUserDataScope(UUID userId) {
        if (userId == null) {
            return 5; // 榛樿浠呮湰锟?
        }
        Integer dataScope = userRoleMapper.getUserDataScope(userId);
        return dataScope != null ? dataScope : 5;
    }

    /**
     * 鑾峰彇鐢ㄦ埛鏈€澶у鎵归噾锟?
     * <p>
     * 鏇夸唬 SysUserRoleMapper.getMaxApprovalAmount
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 鏈€澶у鎵归噾锟?
     */
    @Slave
    public BigDecimal getMaxApprovalAmount(UUID userId) {
        if (userId == null) {
            return BigDecimal.ZERO;
        }
        return userRoleMapper.getMaxApprovalAmount(userId);
    }

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽嫢鏈夋寚瀹氱殑涓存椂瑙掕壊
     * <p>
     * 鏇夸唬 SysUserRoleMapper.hasTemporaryRole
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @param roleId 瑙掕壊 ID
     * @return 鏄惁鎷ユ湁璇ヤ复鏃惰锟?
     */
    @Slave
    public boolean hasTemporaryRole(UUID userId, UUID roleId) {
        if (userId == null || roleId == null) {
            return false;
        }
        return userRoleMapper.hasTemporaryRole(userId, roleId);
    }

    /**
     * 鏌ヨ鐢ㄦ埛鐨勪复鏃惰鑹插垪锟?
     * <p>
     * 鏇夸唬 SysUserRoleMapper.findTemporaryRolesByUserId
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 涓存椂瑙掕壊鍒楄〃
     */
    @Slave
    @Cacheable(value = "userTemporaryRoles", key = "#userId", unless = "#result.isEmpty()")
    public List<Map<String, Object>> findTemporaryRolesByUserId(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        return userRoleMapper.findTemporaryRolesByUserId(userId);
    }

    /**
     * 缁熻鐢ㄦ埛涓存椂瑙掕壊锟?
     * <p>
     * 鏇夸唬 SysUserRoleMapper.countTemporaryRoles
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 涓存椂瑙掕壊锟?
     */
    @Slave
    public Integer countTemporaryRoles(UUID userId) {
        return getCountOrDefault(userId, userRoleMapper::countTemporaryRoles);
    }

    /**
     * 缁熻鐢ㄦ埛鍗冲皢杩囨湡鐨勮鑹叉暟
     * <p>
     * 鏇夸唬 SysUserRoleMapper.countExpiringRoles
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param userId 鐢ㄦ埛 ID
     * @param days   澶╂暟
     * @return 鍗冲皢杩囨湡鐨勮鑹叉暟
     */
    @Slave
    public Integer countExpiringRoles(UUID userId, Integer days) {
        return getCountOrDefault(userId, uid -> userRoleMapper.countExpiringRoles(uid, days));
    }

    /**
     * 缁熻鎸囧畾閮ㄩ棬鐨勭敤鎴锋暟
     *
     * @param deptId 閮ㄩ棬 ID
     * @return 鐢ㄦ埛鏁?
     */
    @Slave
    public int countUsersByDeptId(UUID deptId) {
        if (deptId == null) {
            return 0;
        }
        Integer count = userMapper.countUsersByDeptId(deptId);
        return count != null ? count : 0;
    }

    /**
     * 鎵归噺缁熻鎸囧畾閮ㄩ棬鐨勭敤鎴锋暟
     *
     * @param deptIds 閮ㄩ棬 ID 鍒楄〃
     * @return 閮ㄩ棬 ID -> 鐢ㄦ埛鏁?鏄犲皠
     */
    @Slave
    public Map<UUID, Integer> countUsersByDeptIds(List<UUID> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, Map<String, Object>> raw = userMapper.countUsersByDeptIds(deptIds);
        if (raw == null) {
            return Collections.emptyMap();
        }
        Map<UUID, Integer> result = new HashMap<>();
        for (Map.Entry<UUID, Map<String, Object>> entry : raw.entrySet()) {
            Object count = entry.getValue().get("user_count");
            result.put(entry.getKey(), count instanceof Number ? ((Number) count).intValue() : 0);
        }
        return result;
    }

    // ==================== 绉佹湁杈呭姪鏂规硶 ====================

    /**
     * 閫氱敤鐨勮鏁版煡璇㈡柟娉曪紝澶勭悊null妫€鏌ュ拰榛樿锟?
     *
     * @param userId 鐢ㄦ埛ID
     * @param countFunction 璁℃暟鍑芥暟
     * @return 璁℃暟缁撴灉锛宯ull鏃惰繑锟?
     */
    private Integer getCountOrDefault(UUID userId, java.util.function.Function<UUID, Integer> countFunction) {
        if (userId == null) {
            return 0;
        }
        Integer count = countFunction.apply(userId);
        return count != null ? count : 0;
    }

}
