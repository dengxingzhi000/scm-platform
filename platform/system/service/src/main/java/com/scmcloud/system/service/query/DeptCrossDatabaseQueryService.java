package com.scmcloud.system.service.query;

import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.common.dto.dept.DeptDTO;
import com.scmcloud.system.domain.entity.SysDept;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysDeptMapper;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 閮ㄩ棬璺ㄥ簱鏌ヨ鏈嶅姟
 * <p>
 * 澶勭悊涓庨儴闂ㄧ浉鍏崇殑璺ㄥ簱鏌ヨ鎿嶄綔锛坉b_org 锟絛b_user 锟絛b_permission锟?
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptCrossDatabaseQueryService {
    private final SysDeptMapper deptMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    /**
     * 鏌ヨ閮ㄩ棬鏍戯紙鍖呭惈璐熻矗浜轰俊鎭級
     * <p>
     * 鏇夸唬锟絊ysDeptMapper.selectDeptTree
     *
     * @return 閮ㄩ棬 DTO 鍒楄〃锛堝寘鍚礋璐d汉濮撳悕锟?
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "selectDeptTree"})
    public List<DeptDTO> selectDeptTree() {
        // 1. 锟給rg 搴撴煡璇㈡墍鏈夐儴锟?
        List<SysDept> depts = deptMapper.selectDeptList();
        if (depts == null || depts.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 鏀堕泦鎵€鏈夎礋璐d汉 ID
        Set<UUID> leaderIds = depts.stream()
                .map(SysDept::getLeaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 3. 锟絬ser 搴撴壒閲忔煡璇㈣礋璐d汉淇℃伅
        Map<UUID, String> leaderNameMap = new HashMap<>();
        if (!leaderIds.isEmpty()) {
            List<SysUser> leaders = userMapper.selectBasicInfoByIds(new ArrayList<>(leaderIds));
            leaderNameMap = leaders.stream()
                    .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        }

        // 4. 缁勮 DTO
        Map<UUID, String> finalLeaderNameMap = leaderNameMap;
        return depts.stream()
                .map(dept -> DeptDTO.builder()
                        .id(dept.getId())
                        .parentId(dept.getParentId())
                        .deptCode(dept.getDeptCode())
                        .deptName(dept.getDeptName())
                        .deptType(dept.getDeptType())
                        .leaderId(dept.getLeaderId())
                        .leaderName(dept.getLeaderId() != null ? finalLeaderNameMap.get(dept.getLeaderId()) : null)
                        .phone(dept.getPhone())
                        .email(dept.getEmail())
                        .isolationLevel(dept.getIsolationLevel())
                        .sortOrder(dept.getSortOrder())
                        .status(dept.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 鑾峰彇閮ㄩ棬璐熻矗锟絀D
     * <p>
     * 鏇夸唬 SysDeptMapper.getLeaderId
     * 璺ㄥ簱鏌ヨ锛歞b_org
     *
     * @param deptId 閮ㄩ棬 ID
     * @return 璐熻矗锟絀D
     */
    @Slave
    public UUID getDeptLeaderId(UUID deptId) {
        if (deptId == null) {
            return null;
        }
        return deptMapper.getLeaderId(deptId);
    }

    /**
     * 鏌ヨ鐢ㄦ埛鐨勯儴闂ㄥ強鍏舵墍鏈夊瓙閮ㄩ棬 ID
     * <p>
     * 鏇夸唬锟絊ysUserMapper.findUserDeptAndChildren
     *
     * @param userId 鐢ㄦ埛 ID
     * @return 閮ㄩ棬鍙婂瓙閮ㄩ棬 ID 鍒楄〃
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "findUserDeptAndChildren"})
    public List<UUID> findUserDeptAndChildren(UUID userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        // 1. 锟絬ser 搴撹幏鍙栫敤鎴风殑閮ㄩ棬 ID
        UUID deptId = userMapper.getUserDeptId(userId);
        if (deptId == null) {
            return Collections.emptyList();
        }

        // 2. 锟給rg 搴撻€掑綊鏌ヨ閮ㄩ棬鍙婂瓙閮ㄩ棬
        return deptMapper.selectDeptAndChildren(deptId);
    }

    /**
     * 妫€鏌ョ敤鎴锋槸鍚︽湁鏉冭闂寚瀹氶儴锟?
     * <p>
     * 鏇夸唬锟絊ysUserMapper.hasAccessToDept
     *
     * @param userId 鐢ㄦ埛 ID
     * @param deptId 鐩爣閮ㄩ棬 ID
     * @return 鏄惁鏈夎闂潈锟?
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "hasAccessToDept"})
    public boolean hasAccessToDept(UUID userId, UUID deptId) {
        if (userId == null || deptId == null) {
            return false;
        }

        // 1. 鑾峰彇鐢ㄦ埛鐨勬暟鎹潈闄愯寖锟?
        Integer dataScope = userRoleMapper.getUserDataScope(userId);
        if (dataScope == null) {
            return false;
        }

        // 鏁版嵁鏉冮檺锟?鍏ㄩ儴鏁版嵁
        if (dataScope == 1) {
            return true;
        }

        // 2. 鑾峰彇鐢ㄦ埛鐨勯儴锟絀D
        UUID userDeptId = userMapper.getUserDeptId(userId);
        if (userDeptId == null) {
            return false;
        }

        // 鏁版嵁鏉冮檺锟?鏈儴锟?
        if (dataScope == 3) {
            return userDeptId.equals(deptId);
        }

        // 鏁版嵁鏉冮檺锟?鏈儴闂ㄥ強瀛愰儴锟?
        if (dataScope == 4) {
            List<UUID> accessibleDepts = deptMapper.selectDeptAndChildren(userDeptId);
            return accessibleDepts.contains(deptId);
        }

        // 鏁版嵁鏉冮檺锟?浠呮湰浜猴紙涓嶈兘璁块棶鍏朵粬閮ㄩ棬锟?
        return false;
    }

    /**
     * 缁熻鍗曚釜閮ㄩ棬鐢ㄦ埛锟?
     * <p>
     * 鏇夸唬 SysUserMapper.countUsersByDeptId
     * 璺ㄥ簱鏌ヨ锛歞b_user
     *
     * @param deptId 閮ㄩ棬 ID
     * @return 鐢ㄦ埛锟?
     */
    @Slave
    public int countUsersByDeptId(UUID deptId) {
        if (deptId == null) {
            return 0;
        }
        return userMapper.countUsersByDeptId(deptId);
    }

    /**
     * 鎵归噺缁熻閮ㄩ棬鐢ㄦ埛锟?
     * <p>
     * 鏇夸唬 SysUserMapper.countUsersByDeptIds
     * 璺ㄥ簱鏌ヨ锛歞b_user
     *
     * @param deptIds 閮ㄩ棬 ID 鍒楄〃
     * @return 閮ㄩ棬 ID 锟界敤鎴凤拷鏄犲皠
     */
    @Slave
    @Timed(value = "cross_db_query", extraTags = {"method", "countUsersByDeptIds"})
    public Map<UUID, Integer> countUsersByDeptIds(List<UUID> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, Map<String, Object>> countResult = userMapper.countUsersByDeptIds(deptIds);
        if (countResult == null) {
            return Collections.emptyMap();
        }

        Map<UUID, Integer> result = new HashMap<>();
        countResult.forEach((deptId, row) -> {
            Object count = row.get("user_count");
            result.put(deptId, count != null ? ((Number) count).intValue() : 0);
        });

        return result;
    }
}