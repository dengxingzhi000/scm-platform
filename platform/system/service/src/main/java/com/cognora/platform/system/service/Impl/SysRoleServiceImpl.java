package com.cognora.platform.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.constant.RoleConstants;
import com.cognora.platform.common.exception.BusinessException;
import com.cognora.platform.common.exception.ErrorCode;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.platform.common.dto.role.RoleDTO;
import com.cognora.platform.common.response.ResultCode;
import com.cognora.platform.common.tenant.TenantValidationUtil;
import com.cognora.platform.common.web.util.SecurityUtils;
import com.cognora.platform.system.cache.UserAuthorizationCacheEvictor;
import com.cognora.platform.system.domain.entity.SysRole;
import com.cognora.platform.system.event.DataSyncEventPublisher;
import com.cognora.platform.system.mapper.*;
import com.cognora.platform.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 角色服务实现
 *
 * @author author
 * @since 2025-10-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
    private final SysUserRoleMapper userRoleMapper;
    private final DataSyncEventPublisher dataSyncEventPublisher;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleDataRuleMapper roleDataRuleMapper;
    private final com.cognora.platform.common.security.PermissionChecker permissionChecker;
    private final UserAuthorizationCacheEvictor cacheEvictor;

    private static final String SUPER_ADMIN_ROLE_CODE = "ROLE_SUPER_ADMIN";

    /**
     * 分页查询角色列表
     * <p>
     * 多租户过滤规则：
     * - 平台管理员：查看所有平台角色 + 所有租户角色
     * - 租户管理用户：查看所有平台角色 + 当前租户角色
     */
    @Override
    public Page<RoleDTO> listRoles(Integer pageNum, Integer pageSize, String roleName) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(roleName != null && !roleName.isEmpty(), SysRole::getRoleName, roleName);

        applyTenantFilter(wrapper);

        wrapper.orderByAsc(SysRole::getSortOrder)
                .orderByDesc(SysRole::getCreateTime);

        Page<SysRole> rolePage = getBaseMapper().selectPage(page, wrapper);

        Page<RoleDTO> roleDTOPage = new Page<>(pageNum, pageSize, rolePage.getTotal());
        List<RoleDTO> roleDTOs = rolePage.getRecords().stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toList());
        roleDTOPage.setRecords(roleDTOs);

        return roleDTOPage;
    }

    /**
     * 查询所有角色（不分页）
     * <p>
     * 多租户过滤规则：
     * - 平台管理员：查看所有平台角色 + 所有租户角色
     * - 租户管理用户：查看所有平台角色 + 当前租户角色
     */
    @Override
    @Cacheable(
            value = "roles",
            key = "'all:' + #root.method.name + ':' + (T(com.cognora.platform.common.tenant.TenantContextHolder).getTenantId() " +
                    "!= null ? T(com.cognora.platform.common.tenant.TenantContextHolder).getTenantId().toString() : 'platform')"
    )
    public List<RoleDTO> listAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1);

        applyTenantFilter(wrapper);

        wrapper.orderByAsc(SysRole::getSortOrder);

        List<SysRole> roles = getBaseMapper().selectList(wrapper);
        return roles.stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID 查询角色
     */
    @Override
    @Cacheable(value = "role", key = "#id")
    public RoleDTO getRoleById(UUID id) {
        SysRole role = getBaseMapper().selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        requireRoleOwnership(role);

        RoleDTO roleDTO = convertToRoleDTO(role);

        List<UUID> permissionIds = rolePermissionMapper.findPermissionIdsByRoleId(id);
        roleDTO.setPermissionIds(permissionIds);

        Integer userCount = userRoleMapper.countUsersByRoleId(id);
        roleDTO.setUserCount(userCount);

        return roleDTO;
    }

    /**
     * 新增角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRole(RoleDTO roleDTO) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "role:add");

        UUID tenantId = resolveTenantIdForRole(roleDTO.getRoleType());

        if (getBaseMapper().existsByRoleCodeAndTenantId(roleDTO.getRoleCode(), tenantId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "角色编码在当前租户下已存在");
        }

        SysRole role = new SysRole();
        BeanUtils.copyProperties(roleDTO, role);
        role.setTenantId(tenantId);

        getBaseMapper().insert(role);

        if (roleDTO.getPermissionIds() != null && !roleDTO.getPermissionIds().isEmpty()) {
            rolePermissionMapper.batchInsertRolePermissions(role.getId(), roleDTO.getPermissionIds(),
                    operatorId, tenantId);
        }

        cacheEvictor.evictRoleList();

        dataSyncEventPublisher.publishRoleCreated(role);
        logTenantOperation("CREATE", role.getId(), tenantId);

        log.info("角色创建成功: {} ({}), 操作 {}", role.getRoleCode(),
                role.getRoleType(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 修改角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(UUID id, RoleDTO roleDTO) {
        roleDTO.setId(id);
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "role:update");

        SysRole existRole = getBaseMapper().selectById(roleDTO.getId());
        if (existRole == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        requireNotSuperAdminRole(existRole);
        requireRoleOwnership(existRole);

        SysRole role = new SysRole();
        BeanUtils.copyProperties(roleDTO, role);
        role.setTenantId(existRole.getTenantId());
        role.setRoleType(existRole.getRoleType());

        getBaseMapper().updateById(role);

        cacheEvictor.evictRoleDefinition(id);
        cacheEvictor.evictAllUserAuthorization();

        SysRole updatedRole = getBaseMapper().selectById(role.getId());
        dataSyncEventPublisher.publishRoleUpdated(updatedRole);
        logTenantOperation("UPDATE", roleDTO.getId(), existRole.getTenantId());

        log.info("角色更新成功: {} ({}), 操作 {}", role.getRoleCode(),
                role.getRoleType(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 删除角色
     * <p>
     * 删除角色时会同时清理以下关联数据：
     * <ul>
     *   <li>sys_role_permission - 角色权限关联</li>
     *   <li>sys_role_dept - 角色部门关联（自定义数据权限）</li>
     *   <li>sys_role_data_rule - 角色数据权限规则关联</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(UUID id) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "role:delete");

        SysRole role = getBaseMapper().selectById(id);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        requireNotSuperAdminRole(role);
        requireRoleOwnership(role);

        Integer userCount = userRoleMapper.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该角色下还有 " + userCount + " 个用户，不能删除");
        }

        rolePermissionMapper.deleteRolePermissions(id);
        roleDeptMapper.deleteRoleDepts(id);
        roleDataRuleMapper.deleteRoleDataRules(id);

        getBaseMapper().deleteById(id);

        cacheEvictor.evictRoleDefinition(id);

        dataSyncEventPublisher.publishRoleDeleted(id);
        logTenantOperation("DELETE", id, role.getTenantId());

        log.info("角色删除成功: {} ({}), 操作 {}", role.getRoleCode(),
                role.getRoleType(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 授权权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantPermissions(UUID roleId, List<UUID> permissionIds) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "role:grant-permission");

        SysRole role = getBaseMapper().selectById(roleId);
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在");
        }

        requireRoleOwnership(role);

        rolePermissionMapper.deleteRolePermissions(roleId);

        if (permissionIds != null && !permissionIds.isEmpty()) {
            rolePermissionMapper.batchInsertRolePermissions(roleId, permissionIds,
                    operatorId, TenantValidationUtil.getRequiredTenantId());
        }

        cacheEvictor.evictRolePermissionGrant(roleId);

        logTenantOperation("GRANT_PERMISSIONS", roleId, role.getTenantId());

        log.info("权限授予成功: role={} ({}), 权限 {}, 操作 {}",
                role.getRoleCode(), role.getRoleType(),
                permissionIds != null ? permissionIds.size() : 0,
                SecurityUtils.getCurrentUsername());
    }

    /**
     * 查询角色权限 ID 列表
     */
    @Override
    @Cacheable(value = "rolePermissions", key = "#roleId")
    public List<UUID> getRolePermissionIds(UUID roleId) {
        return rolePermissionMapper.findPermissionIdsByRoleId(roleId);
    }

    private void applyTenantFilter(LambdaQueryWrapper<SysRole> wrapper) {
        if (TenantValidationUtil.isTenantUser()) {
            UUID tenantId = TenantValidationUtil.getRequiredTenantId();
            wrapper.and(w -> w.isNull(SysRole::getTenantId)
                    .or()
                    .eq(SysRole::getTenantId, tenantId));
        }
    }

    private void requireRoleOwnership(SysRole role) {
        if (RoleConstants.ROLE_TYPE_PLATFORM.equals(role.getRoleType())) {
            if (TenantValidationUtil.isTenantUser()) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED.getCode(), "只有平台管理员可以操作平台角色");
            }
        } else {
            if (TenantValidationUtil.isTenantUser()) {
                TenantValidationUtil.validateDataOwnership(role.getTenantId());
            }
        }
    }

    private void requireNotSuperAdminRole(SysRole role) {
        if (SUPER_ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "不能操作超级管理员角色");
        }
    }

    private UUID resolveTenantIdForRole(String roleType) {
        if (RoleConstants.ROLE_TYPE_PLATFORM.equals(roleType)) {
            if (TenantValidationUtil.isTenantUser()) {
                throw new BusinessException(ResultCode.PERMISSION_DENIED.getCode(), "只有平台管理员可以创建平台角色");
            }
            return null;
        }

        return TenantValidationUtil.getRequiredTenantId();
    }

    private void logTenantOperation(String action, UUID entityId, UUID tenantId) {
        if (tenantId != null) {
            TenantValidationUtil.logTenantOperation(action, "ROLE", entityId);
        }
    }

    private RoleDTO convertToRoleDTO(SysRole role) {
        RoleDTO roleDTO = new RoleDTO();
        BeanUtils.copyProperties(role, roleDTO);
        return roleDTO;
    }
}