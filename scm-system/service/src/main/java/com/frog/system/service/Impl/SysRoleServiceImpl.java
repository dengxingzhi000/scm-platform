package com.frog.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frog.common.exception.BusinessException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frog.common.dto.role.RoleDTO;
import com.frog.common.web.util.SecurityUtils;
import com.frog.system.domain.entity.SysRole;
import com.frog.system.event.DataSyncEventPublisher;
import com.frog.system.mapper.*;
import com.frog.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * <p>
 * 角色表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-10-15
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final DataSyncEventPublisher dataSyncEventPublisher;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleDataRuleMapper roleDataRuleMapper;
    private final com.frog.common.security.PermissionChecker permissionChecker;

    /**
     * 分页查询角色列表
     * <p>
     * 多租户过滤规则：
     * - 平台管理员：查看所有平台角色 + 所有租户角色
     * - 租户管理员/用户：查看所有平台角色 + 当前租户角色
     */
    public Page<RoleDTO> listRoles(Integer pageNum, Integer pageSize, String roleName) {
        Page<SysRole> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(roleName != null && !roleName.isEmpty(), SysRole::getRoleName, roleName);

        // 1. 租户过滤：平台角色 + 当前租户角色
        if (!com.frog.common.tenant.TenantValidationUtil.isPlatformAdmin()) {
            // 租户用户：只能看到平台角色和本租户角色
            UUID tenantId = com.frog.common.tenant.TenantValidationUtil.getRequiredTenantId();
            wrapper.and(w -> w.isNull(SysRole::getTenantId) // 平台角色
                    .or()
                    .eq(SysRole::getTenantId, tenantId)); // 当前租户角色
        }
        // 平台管理员不需要额外过滤，可以看到所有角色

        wrapper.orderByAsc(SysRole::getSortOrder)
                .orderByDesc(SysRole::getCreateTime);

        Page<SysRole> rolePage = roleMapper.selectPage(page, wrapper);

        // 2. 转换为 DTO
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
     * - 租户管理员/用户：查看所有平台角色 + 当前租户角色
     */
    @Cacheable(
            value = "roles",
            key = "'all:' + (#root.method.name) + ':' + T(com.frog.common.tenant.TenantContextHolder).getTenantId().orElse('platform')"
    )
    public List<RoleDTO> listAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1);

        // 1. 租户过滤：平台角色 + 当前租户角色
        if (!com.frog.common.tenant.TenantValidationUtil.isPlatformAdmin()) {
            // 租户用户：只能看到平台角色和本租户角色
            UUID tenantId = com.frog.common.tenant.TenantValidationUtil.getRequiredTenantId();
            wrapper.and(w -> w.isNull(SysRole::getTenantId) // 平台角色
                    .or()
                    .eq(SysRole::getTenantId, tenantId)); // 当前租户角色
        }
        // 平台管理员不需要额外过滤，可以看到所有角色

        wrapper.orderByAsc(SysRole::getSortOrder);

        List<SysRole> roles = roleMapper.selectList(wrapper);
        return roles.stream()
                .map(this::convertToRoleDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据 ID查询角色
     */
    @Cacheable(
            value = "role",
            key = "#id"
    )
    public RoleDTO getRoleById(UUID id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        RoleDTO roleDTO = convertToRoleDTO(role);

        // 查询角色权限
        List<UUID> permissionIds = rolePermissionMapper.findPermissionIdsByRoleId(id);
        roleDTO.setPermissionIds(permissionIds);

        // 查询拥有该角色的用户数
        Integer userCount = userRoleMapper.countUsersByRoleId(id);
        roleDTO.setUserCount(userCount);

        return roleDTO;
    }

    /**
     * 新增角色
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"role", "roles", "userRoles"},
            allEntries = true
    )
    public void addRole(RoleDTO roleDTO) {
        // 1. 检查操作权限
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "role:add");

        // 2. 区分平台角色和租户角色的创建
        UUID tenantId = null;
        if ("PLATFORM_ROLE".equals(roleDTO.getRoleType())) {
            // 创建平台角色 - 只有平台管理员可以创建
            if (!com.frog.common.tenant.TenantValidationUtil.isPlatformAdmin()) {
                throw new BusinessException("PERMISSION_DENIED", "只有平台管理员可以创建平台角色");
            }
            // 平台角色的 tenant_id 为 NULL
            tenantId = null;
        } else {
            // 创建租户角色 - 验证租户上下文
            tenantId = com.frog.common.tenant.TenantValidationUtil.getRequiredTenantId();
            // 自动设置为租户角色
            roleDTO.setRoleType("TENANT_ROLE");
        }

        // 3. 检查角色编码是否存在
        if (roleMapper.existsByRoleCode(roleDTO.getRoleCode())) {
            throw new BusinessException("角色编码已存在");
        }

        // 4. 准备实体
        SysRole role = new SysRole();
        BeanUtils.copyProperties(roleDTO, role);
        role.setTenantId(tenantId); // 平台角色为 NULL，租户角色为当前租户ID

        // 5. 数据库操作
        roleMapper.insert(role);

        // 6. 分配权限
        if (roleDTO.getPermissionIds() != null && !roleDTO.getPermissionIds().isEmpty()) {
            rolePermissionMapper.batchInsertRolePermissions(role.getId(), roleDTO.getPermissionIds(),
                    SecurityUtils.getCurrentUserUuid().orElse(null));
        }

        // 7. 发布同步事件
        dataSyncEventPublisher.publishRoleCreated(role);

        // 8. 记录租户操作日志
        if (tenantId != null) {
            com.frog.common.tenant.TenantValidationUtil.logTenantOperation("CREATE", "ROLE", role.getId());
        }

        log.info("角色创建成功: {} ({}), 操作人: {}", role.getRoleCode(),
                role.getRoleType(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 修改角色
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"role", "roles", "userRoles", "userPermissions"},
            allEntries = true
    )
    public void updateRole(RoleDTO roleDTO) {
        // 1. 检查操作权限
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "role:update");

        // 2. 查询数据
        SysRole existRole = roleMapper.selectById(roleDTO.getId());
        if (existRole == null) {
            throw new BusinessException("角色不存在");
        }

        // 3. 业务校验
        if (existRole.getId().equals(UUID.fromString("019a0aee-3b74-7bfc-b34f-48b5428d4875"))) {
            throw new BusinessException("不能修改超级管理员角色");
        }

        // 4. 验证数据归属（区分平台角色和租户角色）
        if ("PLATFORM_ROLE".equals(existRole.getRoleType())) {
            // 修改平台角色 - 只有平台管理员可以修改
            if (!com.frog.common.tenant.TenantValidationUtil.isPlatformAdmin()) {
                throw new BusinessException("PERMISSION_DENIED", "只有平台管理员可以修改平台角色");
            }
        } else {
            // 修改租户角色 - 验证租户上下文和数据归属
            UUID tenantId = com.frog.common.tenant.TenantValidationUtil.getRequiredTenantId();
            com.frog.common.tenant.TenantValidationUtil.validateDataOwnership(existRole.getTenantId());
        }

        // 5. 执行业务逻辑
        SysRole role = new SysRole();
        BeanUtils.copyProperties(roleDTO, role);
        role.setTenantId(existRole.getTenantId()); // 保持 tenant_id 不变
        role.setRoleType(existRole.getRoleType()); // 保持 role_type 不变

        roleMapper.updateById(role);

        // 6. 发布同步事件
        SysRole updatedRole = roleMapper.selectById(role.getId());
        dataSyncEventPublisher.publishRoleUpdated(updatedRole);

        // 7. 记录日志
        if (existRole.getTenantId() != null) {
            com.frog.common.tenant.TenantValidationUtil.logTenantOperation("UPDATE", "ROLE", roleDTO.getId());
        }

        log.info("角色更新成功: {} ({}), 操作人: {}", role.getRoleCode(),
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
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"role", "roles", "userRoles", "userPermissions"},
            allEntries = true
    )
    public void deleteRole(UUID id) {
        // 1. 检查操作权限
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "role:delete");

        // 2. 查询数据
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 3. 业务校验
        if (role.getId().equals(UUID.fromString("019a0aee-3b74-7bfc-b34f-48b5428d4875"))) {
            throw new BusinessException("不能删除超级管理员角色");
        }

        // 4. 验证数据归属（区分平台角色和租户角色）
        if ("PLATFORM_ROLE".equals(role.getRoleType())) {
            // 删除平台角色 - 只有平台管理员可以删除
            if (!com.frog.common.tenant.TenantValidationUtil.isPlatformAdmin()) {
                throw new BusinessException("PERMISSION_DENIED", "只有平台管理员可以删除平台角色");
            }
        } else {
            // 删除租户角色 - 验证租户上下文和数据归属
            UUID tenantId = com.frog.common.tenant.TenantValidationUtil.getRequiredTenantId();
            com.frog.common.tenant.TenantValidationUtil.validateDataOwnership(role.getTenantId());
        }

        // 5. 检查是否有用户使用该角色
        Integer userCount = userRoleMapper.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException("该角色下还有 " + userCount + " 个用户，不能删除");
        }

        // 6. 删除角色权限关联 (sys_role_permission)
        rolePermissionMapper.deleteRolePermissions(id);

        // 7. 删除角色部门关联 (sys_role_dept) - 自定义数据权限范围
        roleDeptMapper.deleteRoleDepts(id);

        // 8. 删除角色数据权限规则关联 (sys_role_data_rule)
        roleDataRuleMapper.deleteRoleDataRules(id);

        // 9. 删除角色记录
        roleMapper.deleteById(id);

        // 10. 发布同步事件用于冗余数据更新
        dataSyncEventPublisher.publishRoleDeleted(id);

        // 11. 记录日志
        if (role.getTenantId() != null) {
            com.frog.common.tenant.TenantValidationUtil.logTenantOperation("DELETE", "ROLE", id);
        }

        log.info("角色删除成功: {} ({}), 操作人: {}", role.getRoleCode(),
                role.getRoleType(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 授权权限
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"role", "userRoles", "userPermissions", "userInfo"},
            allEntries = true
    )
    public void grantPermissions(UUID roleId, List<UUID> permissionIds) {
        // 1. 检查操作权限
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "role:grant-permission");

        // 2. 查询数据
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        // 3. 验证数据归属（区分平台角色和租户角色）
        if ("PLATFORM_ROLE".equals(role.getRoleType())) {
            // 为平台角色授权 - 只有平台管理员可以操作
            if (!com.frog.common.tenant.TenantValidationUtil.isPlatformAdmin()) {
                throw new BusinessException("PERMISSION_DENIED", "只有平台管理员可以为平台角色授权");
            }
        } else {
            // 为租户角色授权 - 验证租户上下文和数据归属
            UUID tenantId = com.frog.common.tenant.TenantValidationUtil.getRequiredTenantId();
            com.frog.common.tenant.TenantValidationUtil.validateDataOwnership(role.getTenantId());
        }

        // 4. 删除原有权限
        rolePermissionMapper.deleteRolePermissions(roleId);

        // 5. 分配新权限
        if (permissionIds != null && !permissionIds.isEmpty()) {
            rolePermissionMapper.batchInsertRolePermissions(roleId, permissionIds,
                    SecurityUtils.getCurrentUserUuid().orElse(null));
        }

        // 6. 记录日志
        if (role.getTenantId() != null) {
            com.frog.common.tenant.TenantValidationUtil.logTenantOperation("GRANT_PERMISSIONS", "ROLE", roleId);
        }

        log.info("权限授予成功: role={} ({}), 权限数: {}, 操作人: {}",
                role.getRoleCode(), role.getRoleType(),
                permissionIds != null ? permissionIds.size() : 0,
                SecurityUtils.getCurrentUsername());
    }

    /**
     * 查询角色权限 ID列表
     */
    @Cacheable(
            value = "rolePermissions",
            key = "#roleId"
    )
    public List<UUID> getRolePermissionIds(UUID roleId) {
        return rolePermissionMapper.findPermissionIdsByRoleId(roleId);
    }

    private RoleDTO convertToRoleDTO(SysRole role) {
        RoleDTO roleDTO = new RoleDTO();
        BeanUtils.copyProperties(role, roleDTO);
        return roleDTO;
    }
}
