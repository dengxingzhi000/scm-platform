package com.scmcloud.system.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.*;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 瑙掕壊璺ㄥ簱鏌ヨ鏈嶅姟
 * <p>
 * 澶勭悊涓庤鑹茬浉鍏崇殑璺ㄥ簱鏌ヨ鎿嶄綔锛坉b_permission 锟絛b_user 锟絛b_org锟?
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleCrossDatabaseQueryService {
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /**
     * 鑾峰彇瑙掕壊鐨勭瓑锟?
     * <p>
     * 鐢ㄤ簬瑙掕壊鎺堟潈鏃剁殑鏉冮檺妫€锟?
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param roleId 瑙掕壊 ID
     * @return 瑙掕壊绛夌骇锛坮ole_level锟?
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getRoleLevel"})
    public Integer getRoleLevel(UUID roleId) {
        if (roleId == null) {
            return null;
        }
        return roleMapper.getRoleLevel(roleId);
    }

    /**
     * 鑾峰彇瑙掕壊鎵€灞炵殑绉熸埛 ID
     * <p>
     * 鐢ㄤ簬瑙掕壊鎺堟潈鏃堕獙璇佽鑹插綊灞烇紙鍙兘鍒嗛厤鏈鎴锋垨骞冲彴瑙掕壊锟?
     * 璺ㄥ簱鏌ヨ锛歞b_permission
     *
     * @param roleId 瑙掕壊 ID
     * @return 绉熸埛 ID锛圢ULL 琛ㄧず骞冲彴瑙掕壊锟?
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "getRoleTenantId"})
    public UUID getRoleTenantId(UUID roleId) {
        if (roleId == null) {
            return null;
        }
        return roleMapper.getRoleTenantId(roleId);
    }

    /**
     * 鏍规嵁瑙掕壊缂栫爜鏌ヨ绗竴涓湁鏁堢敤鎴风殑ID
     *
     * @param roleCode 瑙掕壊缂栫爜
     * @return 绗竴涓湁鏁堢敤鎴风殑 ID锛屽鏋滄病鏈夊垯杩斿洖 null
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findFirstUserIdByRoleCode"})
    public UUID findFirstUserIdByRoleCode(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) {
            return null;
        }

        // 1. 锟絧ermission 搴撴煡璇㈣锟絀D
        UUID roleId = roleMapper.findIdByRoleCode(roleCode);
        if (roleId == null) {
            return null;
        }

        // 2. 锟絧ermission 搴撴煡璇㈣瑙掕壊鐨勭敤锟絀D 鍒楄〃
        List<UUID> userIds = userRoleMapper.findUserIdsByRoleId(roleId);
        if (userIds == null || userIds.isEmpty()) {
            return null;
        }

        // 3. 锟絬ser 搴撴煡璇㈡湁鏁堢敤鎴凤紝鍙栫涓€锟?
        List<SysUser> users = userMapper.selectBasicInfoByIds(userIds);
        return users.stream()
                .filter(u -> u.getStatus() != null && u.getStatus() == 1)
                .min(Comparator.comparing(SysUser::getCreateTime))
                .map(SysUser::getId)
                .orElse(null);
    }

    /**
     * 鑾峰彇瑙掕壊鍙闂殑閮ㄩ棬 ID 鍒楄〃锛堥€掑綊鍖呭惈瀛愰儴闂級
     * <p>
     * 鏇夸唬锟絊ysRoleDeptMapper.findAccessibleDeptIds
     *
     * @param roleId 瑙掕壊 ID
     * @return 鍙闂殑閮ㄩ棬 ID 鍒楄〃
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findAccessibleDeptIds"})
    public List<UUID> findAccessibleDeptIds(UUID roleId) {
        if (roleId == null) {
            return Collections.emptyList();
        }

        Set<UUID> result = new HashSet<>();

        // 1. 鑾峰彇涓嶉渶瑕侀€掑綊鐨勯儴锟絀D
        List<UUID> directDeptIds = roleDeptMapper.findDeptIdsWithoutChildren(roleId);
        if (directDeptIds != null) {
            result.addAll(directDeptIds);
        }

        // 2. 鑾峰彇闇€瑕侀€掑綊瀛愰儴闂ㄧ殑閮ㄩ棬 ID
        List<UUID> deptIdsWithChildren = roleDeptMapper.findDeptIdsWithChildren(roleId);
        if (deptIdsWithChildren != null && !deptIdsWithChildren.isEmpty()) {
            // 3. 锟給rg 搴撻€掑綊鏌ヨ瀛愰儴锟?
            List<UUID> allChildDepts = deptMapper.selectDeptsAndChildren(deptIdsWithChildren);
            if (allChildDepts != null) {
                result.addAll(allChildDepts);
            }
        }

        return new ArrayList<>(result);
    }

    /**
     * 鏌ヨ鍗冲皢杩囨湡鐨勮鑹诧紙鍖呭惈鐢ㄦ埛淇℃伅锟?
     * <p>
     * 鏇夸唬锟絊ysUserMapper.findExpiringRoles
     *
     * @param days 澶╂暟
     * @return 鍗冲皢杩囨湡鐨勮鑹蹭俊鎭垪锟?
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findExpiringRolesWithUserInfo"})
    public List<Map<String, Object>> findExpiringRolesWithUserInfo(Integer days) {
        if (days == null || days <= 0) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> expiringRoles = userRoleMapper.findExpiringRolesForNotification(days);
        return enrichRolesWithUserInfo(expiringRoles, true);
    }

    /**
     * 鏌ヨ宸茶繃鏈熺殑瑙掕壊锛堝寘鍚敤鎴蜂俊鎭級
     * <p>
     * 鏇夸唬锟絊ysUserMapper.findExpiredRoles
     *
     * @return 宸茶繃鏈熺殑瑙掕壊淇℃伅鍒楄〃
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findExpiredRolesWithUserInfo"})
    public List<Map<String, Object>> findExpiredRolesWithUserInfo() {
        List<Map<String, Object>> expiredRoles = userRoleMapper.findExpiredRolesForCleanup();
        return enrichRolesWithUserInfo(expiredRoles, false);
    }

    /**
     * 涓鸿鑹插垪琛ㄨˉ鍏呯敤鎴蜂俊锟?
     *
     * @param roles        瑙掕壊鍒楄〃
     * @param includeEmail 鏄惁鍖呭惈閭瀛楁
     * @return 鍖呭惈鐢ㄦ埛淇℃伅鐨勮鑹插垪锟?
     */
    private List<Map<String, Object>> enrichRolesWithUserInfo(List<Map<String, Object>> roles, boolean includeEmail) {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 鏀堕泦鐢ㄦ埛 ID
        Set<UUID> userIds = roles.stream()
                .map(m -> (UUID) m.get("user_id"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 锟絬ser 搴撴壒閲忔煡璇㈢敤鎴蜂俊锟?
        Map<UUID, SysUser> userMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            List<SysUser> users = userMapper.selectBasicInfoByIds(new ArrayList<>(userIds));
            userMap = users.stream()
                    .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        }

        // 3. 缁勮缁撴灉
        Map<UUID, SysUser> finalUserMap = userMap;
        return roles.stream()
                .map(m -> {
                    Map<String, Object> result = new HashMap<>(m);
                    UUID userId = (UUID) m.get("user_id");
                    SysUser user = finalUserMap.get(userId);
                    if (user != null) {
                        result.put("username", user.getUsername());
                        if (includeEmail) {
                            result.put("email", user.getEmail());
                        }
                    }
                    return result;
                })
                .collect(Collectors.toList());
    }
}