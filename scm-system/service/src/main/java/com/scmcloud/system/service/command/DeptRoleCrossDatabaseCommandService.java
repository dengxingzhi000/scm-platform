package com.scmcloud.system.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.system.mapper.SysRoleDeptMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 閮ㄩ棬瑙掕壊璺ㄥ簱鍛戒护鏈嶅姟
 * <p>
 * 澶勭悊閮ㄩ棬瑙掕壊鍏宠仈鐨勫啓鎿嶄綔锛坉b_permission锟?
 *
 * @author Deng
 * @since 2025-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeptRoleCrossDatabaseCommandService {
    private final SysRoleDeptMapper roleDeptMapper;

    /**
     * 鍒犻櫎閮ㄩ棬鐨勮鑹插叧锟?
     * <p>
     * 鐢ㄤ簬鍒犻櫎閮ㄩ棬鏃舵竻锟絛b_permission.sys_role_dept 涓殑鍏宠仈鏁版嵁
     * 璺ㄥ簱鎿嶄綔锛歞b_org 锟絛b_permission
     *
     * @param deptId 閮ㄩ棬 ID
     * @return 鍒犻櫎琛屾暟
     */
    @Master(reason = "Write operation must use master database")
    @Transactional(rollbackFor = Exception.class)
    public int deleteRoleDeptsByDeptId(UUID deptId) {
        if (deptId == null) {
            return 0;
        }
        log.debug("Deleting role-dept associations for deptId={}", deptId);
        return roleDeptMapper.deleteByDeptId(deptId);
    }
}