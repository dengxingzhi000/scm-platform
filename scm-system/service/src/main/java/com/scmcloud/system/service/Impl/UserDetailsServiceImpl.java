package com.scmcloud.system.service.Impl;

import com.scmcloud.common.web.domain.SecurityUser;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.mapper.SysUserRoleMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * UserDetailsService 瀹炵幇
 *
 * @author Deng
 * createData 2025/10/14 14:54
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    @Cacheable(
            value = "userDetails",
            key = "#username",
            unless = "#result == null"
    )
    @NonNull
    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        // 1. 锟絛b_user 搴撴煡璇㈢敤鎴峰熀鏈俊锟?
        var user = sysUserMapper.findByUsername(username);
        if (user == null || user.getDeleted()) {
            log.warn("User not found: {}", username);
            throw new UsernameNotFoundException("User not found or deleted: " + username);
        }

        // 2. 锟絛b_permission 搴撴煡璇㈢敤鎴疯鑹诧紙璺ㄥ簱鏌ヨ锟?
        Set<String> roles = sysUserRoleMapper.findRoleCodesByUserId(user.getId());

        // 3. 锟絛b_permission 搴撴煡璇㈢敤鎴锋潈闄愶紙璺ㄥ簱鏌ヨ锟?
        Set<String> permissions = sysUserRoleMapper.findPermissionCodesByUserId(user.getId());

        SecurityUser securityUser = SecurityUser.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .realName(user.getRealName())
                .deptId(user.getDeptId())
                .status(user.getStatus())
                .accountType(user.getAccountType())
                .userLevel(user.getUserLevel())
                .roles(roles)
                .permissions(permissions)
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .passwordExpireTime(user.getPasswordExpireTime())
                .forceChangePassword(user.getForceChangePassword())
                .build();

        log.info("User loaded: {}, Roles: {}, Permissions count: {}",
                username, roles, permissions.size());

        return securityUser;
    }
}
