package com.scmcloud.system.service.dubbo;

import com.scmcloud.common.dto.user.UserDTO;
import com.scmcloud.common.dto.user.UserInfo;
import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.system.service.ISysPermissionService;
import com.scmcloud.system.service.command.ISysUserCommandService;
import com.scmcloud.system.service.query.ISysUserQueryService;
import com.scmcloud.system.api.UserDubboService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 鐢ㄦ埛Dubbo鏈嶅姟瀹炵幇
 * 涓虹敤鎴风浉鍏虫搷浣滄彁渚涢珮鎬ц兘RPC鏈嶅姟
 *
 * @author Deng
 * @version 2.0
 */
@RequiredArgsConstructor
@DubboService
@Component
public class UserDubboServiceImpl implements UserDubboService {
    private final ISysUserQueryService sysUserQueryService;
    private final ISysUserCommandService sysUserCommandService;
    private final ISysPermissionService sysPermissionService;

    @Override
    public SecurityUser getUserByUsername(String username) {
        return sysUserQueryService.getUserByUsername(username);
    }

    @Override
    public Set<String> getUserRoles(UUID userId) {
        return sysPermissionService.getUserRoles(userId);
    }

    @Override
    public Set<String> getUserPermissions(UUID userId) {
        return sysPermissionService.getUserPermissions(userId);
    }

    @Override
    public UserInfo getUserInfo(UUID userId) {
        return sysUserQueryService.getUserInfo(userId);
    }

    @Override
    public void updateLastLogin(UUID userId, String ipAddress, LocalDateTime loginTime) {
        // 璋冪敤鐜版湁鏂规硶锛涘鏋滀笉闇€瑕侊紝鍒欏拷锟絣oginTime 鍙傛暟锟?
        sysUserCommandService.updateLastLogin(userId, ipAddress);
    }

    @Override
    public UserDTO getUserById(UUID userId) {
        return sysUserQueryService.getUserById(userId);
    }

    @Override
    public Set<String> findRolesByUserId(UUID userId) {
        return sysPermissionService.getUserRoles(userId);
    }

    @Override
    public Set<String> findPermissionsByUserId(UUID userId) {
        return sysPermissionService.getUserPermissions(userId);
    }
}

