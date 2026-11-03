package com.scmcloud.system.service.command;

import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.security.PermissionChecker;
import com.scmcloud.common.tenant.TenantValidationUtil;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysUser;
import com.scmcloud.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Cross-cutting template for user command operations.
 *
 * <p>Encapsulates the repeated 8-step boilerplate shared by most user write methods:</p>
 * <ol>
 *   <li>Validate tenant context</li>
 *   <li>Resolve current operator UUID</li>
 *   <li>Check operator permission</li>
 *   <li>Load the target user (404 if absent)</li>
 *   <li>Validate tenant ownership</li>
 *   <li>Check operator's data scope against target</li>
 *   <li>Run caller-provided mutation body</li>
 *   <li>Write tenant operation audit log</li>
 * </ol>
 *
 * <p>Callers are still responsible for: business-rule validation, persistence
 * (save/update/delete), event publishing, and the final info log line.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCommandTemplate {

    private final SysUserMapper userMapper;
    private final PermissionChecker permissionChecker;

    /**
     * Execute a user command that mutates an existing user.
     *
     * @param operation         audit log operation name (e.g. "UPDATE", "DELETE")
     * @param operatorPermission permission code required from current operator
     * @param userId            target user id
     * @param body              mutation body; receives the loaded user and operator id
     * @return the loaded SysUser (so the caller can read fields like username for logging)
     */
    public SysUser execute(String operation,
                           String operatorPermission,
                           UUID userId,
                           BiConsumer<SysUser, UUID> body) {
        TenantValidationUtil.getRequiredTenantId();
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, operatorPermission);

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
                    "无权操作该用户数据");
        }

        body.accept(user, operatorId);

        TenantValidationUtil.logTenantOperation(operation, "USER", userId);
        return user;
    }
}
