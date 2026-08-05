package com.scmcloud.system.service.command;

import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import com.scmcloud.system.service.query.UserCrossDatabaseQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * User-role lifecycle facade. Behaviour matches the original
 * {@code SysUserServiceImpl} methods; no permission or tenant checks are added
 * (pre-existing gaps are explicitly out of scope).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRoleCommandServiceImpl implements IUserRoleCommandService {

    private static final String KEY_GEN = "tenantAwareCacheKeyGenerator";

    private final SysUserMapper userMapper;
    private final UserCrossDatabaseQueryService userQueryService;
    private final UserRoleCrossDatabaseCommandService userRoleCommandService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void grantTemporaryRoles(UUID userId, List<UUID> roleIds,
                                    LocalDateTime effectiveTime, LocalDateTime expireTime) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }

        if (expireTime != null && expireTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("过期时间不能早于当前时间");
        }
        if (effectiveTime != null && expireTime != null && effectiveTime.isAfter(expireTime)) {
            throw new BusinessException("生效时间不能晚于过期时间");
        }

        if (roleIds != null && !roleIds.isEmpty()) {
            UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
            int inserted = userRoleCommandService.batchInsertTemporaryUserRoles(
                    userId,
                    roleIds,
                    effectiveTime != null ? effectiveTime : LocalDateTime.now(),
                    expireTime,
                    operatorId);
            log.debug("为用户 {} 授予 {} 个临时角色", user.getUsername(), inserted);
        }

        log.info("Temporary roles granted to user: {}, roles: {}, expireTime: {}, by: {}",
                user.getUsername(), roleIds, expireTime, SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void extendTemporaryRole(UUID userId, UUID roleId, LocalDateTime newExpireTime) {
        if (!userQueryService.hasTemporaryRole(userId, roleId)) {
            throw new BusinessException("用户不存在该临时角色或已过期");
        }
        if (newExpireTime.isBefore(LocalDateTime.now())) {
            throw new BusinessException("新的过期时间不能早于当前时间");
        }

        int updated = userRoleCommandService.extendTemporaryRole(userId, roleId, newExpireTime);
        if (updated == 0) {
            throw new BusinessException("延长临时角色失败");
        }

        log.info("Temporary role extended: userId={}, roleId={}, newExpireTime={}, by={}",
                userId, roleId, newExpireTime, SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"user", "userDetails", "userInfo"}, keyGenerator = KEY_GEN)
    public void terminateTemporaryRole(UUID userId, UUID roleId) {
        int updated = userRoleCommandService.terminateTemporaryRole(userId, roleId);
        if (updated == 0) {
            throw new BusinessException("终止临时角色失败，可能该角色不存在或已过期");
        }

        log.info("Temporary role terminated: userId={}, roleId={}, by={}",
                userId, roleId, SecurityUtils.getCurrentUsername());
    }
}