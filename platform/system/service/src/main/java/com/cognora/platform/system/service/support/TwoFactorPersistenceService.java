package com.cognora.platform.system.service.support;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.tenant.TenantContextHolder;
import com.cognora.platform.common.util.UUIDv7Util;
import com.cognora.platform.system.cache.UserAuthorizationCacheEvictor;
import com.cognora.platform.system.domain.entity.SysUser;
import com.cognora.platform.system.domain.entity.SysUserRecoveryCode;
import com.cognora.platform.system.mapper.SysUserMapper;
import com.cognora.platform.system.mapper.SysUserRecoveryCodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 2FA 秘钥与恢复码的持久化，供 auth 服务通过 Dubbo 调用。
 */
@Service
@RequiredArgsConstructor
public class TwoFactorPersistenceService {

    private final SysUserMapper userMapper;
    private final SysUserRecoveryCodeMapper recoveryCodeMapper;
    private final UserAuthorizationCacheEvictor cacheEvictor;

    @Transactional(rollbackFor = Exception.class)
    public void enableTwoFactor(UUID userId, String secret) {
        SysUser user = requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getTwoFactorEnabled, true)
                .set(SysUser::getTwoFactorSecret, secret));
        evictInTenant(user, () -> cacheEvictor.evictUserCredentials(user.getId(), user.getUsername()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void disableTwoFactor(UUID userId) {
        SysUser user = requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .set(SysUser::getTwoFactorEnabled, false)
                .set(SysUser::getTwoFactorSecret, null));
        recoveryCodeMapper.deleteByUserId(userId);
        evictInTenant(user, () -> cacheEvictor.evictUserCredentials(user.getId(), user.getUsername()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void replaceRecoveryCodes(UUID userId, List<String> codeHashes) {
        SysUser user = requireUser(userId);
        recoveryCodeMapper.deleteByUserId(userId);
        if (codeHashes == null) {
            return;
        }
        for (String hash : codeHashes) {
            SysUserRecoveryCode code = new SysUserRecoveryCode();
            code.setId(UUIDv7Util.generate());
            code.setTenantId(user.getTenantId());
            code.setUserId(userId);
            code.setCodeHash(hash);
            code.setUsed(false);
            code.setCreateTime(LocalDateTime.now());
            recoveryCodeMapper.insert(code);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean consumeRecoveryCode(UUID userId, String codeHash) {
        if (userId == null || codeHash == null) {
            return false;
        }
        return recoveryCodeMapper.consume(userId, codeHash) > 0;
    }

    private void evictInTenant(SysUser user, Runnable action) {
        TenantContextHolder.executeInTenantContext(user.getTenantId(), () -> {
            action.run();
            return null;
        });
    }

    private SysUser requireUser(UUID userId) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }
        return user;
    }
}
