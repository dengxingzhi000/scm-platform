package com.cognora.platform.system.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cognora.platform.common.dto.user.BatchOperationResult;
import com.cognora.platform.common.dto.user.BatchUserRequest;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.status.StatusValidator;
import com.cognora.platform.common.web.util.SecurityUtils;
import com.cognora.platform.system.domain.entity.SysUser;
import com.cognora.platform.system.domain.enums.UserStatus;
import com.cognora.platform.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * 批量用户操作。
 *
 * <p>逐行调用独立事务方法，不套外层大事务：单行失败只记录该行结果，其他行继续执行。
 * 每一行都复用 {@link UserCommandTemplate} 的租户隔离与数据范围校验。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchUserCommandService {

    private final SysUserMapper userMapper;
    private final UserCommandTemplate commandTemplate;
    private final StatusValidator statusValidator;
    private final ISysUserCommandService userCommandService;

    public BatchOperationResult enable(BatchUserRequest request) {
        return executeRows(request.getIds(), id -> changeStatus(id, true, null));
    }

    public BatchOperationResult disable(BatchUserRequest request) {
        return executeRows(request.getIds(), id -> changeStatus(id, false, request.getReason()));
    }

    public BatchOperationResult transferDept(BatchUserRequest request) {
        if (request.getDeptId() == null) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "deptId 不能为空");
        }
        return executeRows(request.getIds(), id -> commandTemplate.execute(
                "BATCH_TRANSFER_DEPT", "user:update", id, (loaded, op) -> {
                    SysUser update = new SysUser();
                    update.setId(loaded.getId());
                    update.setVersion(loaded.getVersion());
                    update.setDeptId(request.getDeptId());
                    update.setStatusChangedAt(LocalDateTime.now());
                    update.setStatusChangedBy(op);
                    if (userMapper.updateById(update) == 0) {
                        throw new BusinessException(ResultCode.CONFLICT.getCode(),
                                "用户数据已被其他操作修改，请刷新后重试");
                    }
                }));
    }

    public BatchOperationResult delete(BatchUserRequest request) {
        return executeRows(request.getIds(), userCommandService::deleteUser);
    }

    public BatchOperationResult grantRoles(BatchUserRequest request) {
        if (request.getRoleIds() == null || request.getRoleIds().isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "roleIds 不能为空");
        }
        return executeRows(request.getIds(), id -> userCommandService.grantRoles(id, request.getRoleIds()));
    }

    private void changeStatus(UUID userId, boolean enable, String reason) {
        commandTemplate.execute(enable ? "BATCH_ENABLE" : "BATCH_DISABLE", "user:update", userId,
                (loaded, op) -> {
                    UserStatus from = UserStatus.fromCode(loaded.getStatus());
                    String target = enable ? "ACTIVE" : "INACTIVE";
                    statusValidator.validateTransition("USER", from.name(), target);

                    LambdaUpdateWrapper<SysUser> update = new LambdaUpdateWrapper<>();
                    update.eq(SysUser::getId, loaded.getId())
                            .set(SysUser::getStatus,
                                    enable ? UserStatus.ACTIVE.getCode() : UserStatus.INACTIVE.getCode())
                            .set(SysUser::getLockedUntil, null)
                            .set(SysUser::getLockReason, enable ? null : reason)
                            .set(SysUser::getLoginAttempts, enable ? 0 : loaded.getLoginAttempts())
                            .set(SysUser::getStatusChangedAt, LocalDateTime.now())
                            .set(SysUser::getStatusChangedBy, op)
                            .setSql("version = version + 1");
                    if (loaded.getVersion() != null) {
                        update.eq(SysUser::getVersion, loaded.getVersion());
                    }
                    if (userMapper.update(null, update) == 0) {
                        throw new BusinessException(ResultCode.CONFLICT.getCode(),
                                "用户数据已被其他操作修改，请刷新后重试");
                    }
                });
    }

    private BatchOperationResult executeRows(List<UUID> ids, Consumer<UUID> action) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResultCode.VALIDATION_FAILED.getCode(), "用户 ID 列表不能为空");
        }
        List<BatchOperationResult.BatchUserResult> results = new ArrayList<>(ids.size());
        int succeeded = 0;
        for (UUID id : ids) {
            try {
                action.accept(id);
                succeeded++;
                results.add(BatchOperationResult.BatchUserResult.builder()
                        .userId(id).success(true).build());
            } catch (BusinessException ex) {
                results.add(BatchOperationResult.BatchUserResult.builder()
                        .userId(id).success(false).code(ex.getCode()).message(ex.getMessage()).build());
            } catch (Exception ex) {
                log.warn("批量用户操作单行失败: userId={}, operator={}, error={}",
                        id, SecurityUtils.getCurrentUsername(), ex.getMessage());
                results.add(BatchOperationResult.BatchUserResult.builder()
                        .userId(id).success(false).code(ResultCode.SERVER_ERROR.getCode())
                        .message("操作失败").build());
            }
        }
        return BatchOperationResult.builder()
                .total(ids.size())
                .succeeded(succeeded)
                .failed(ids.size() - succeeded)
                .results(results)
                .build();
    }
}
