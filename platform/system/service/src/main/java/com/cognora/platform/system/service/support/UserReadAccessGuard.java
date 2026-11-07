package com.cognora.platform.system.service.support;

import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.security.PermissionChecker;
import com.cognora.platform.common.tenant.TenantValidationUtil;
import com.cognora.platform.common.web.util.SecurityUtils;
import com.cognora.platform.system.domain.entity.SysUser;
import com.cognora.platform.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 用户详情类读取的数据范围校验。
 *
 * <p>必须在缓存查询之前调用，且不能把结果缓存起来：缓存键只有用户 id，若把按操作者
 * 计算的数据范围结论写入缓存，会把“当前操作者可读”错误地复用给其他操作者。</p>
 */
@Component
@RequiredArgsConstructor
public class UserReadAccessGuard {

    private final SysUserMapper userMapper;
    private final PermissionChecker permissionChecker;

    /**
     * 校验当前登录用户可读取目标用户，并返回目标用户实体（未经过缓存）。
     */
    public SysUser requireReadable(UUID userId) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.UNAUTHORIZED.getCode(), ResultCode.UNAUTHORIZED.getMessage()));

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND.getCode(),
                    ResultCode.USER_NOT_FOUND.getMessage());
        }

        TenantValidationUtil.validateDataOwnership(user.getTenantId());

        String dataScope = permissionChecker.getUserDataScope(operatorId);
        if (permissionChecker.cannotOperateResource(operatorId, user.getCreateBy(),
                user.getDeptId(), dataScope)) {
            throw new BusinessException(ResultCode.DATA_ACCESS_DENIED.getCode(),
                    "无权访问该用户数据");
        }
        return user;
    }
}
