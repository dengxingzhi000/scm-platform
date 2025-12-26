package com.frog.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frog.common.exception.BusinessException;
import com.frog.common.util.UUIDv7Util;
import com.frog.common.dto.permission.ApiPermissionDTO;
import com.frog.common.dto.permission.PermissionDTO;
import com.frog.common.web.util.SecurityUtils;
import com.frog.system.domain.entity.SysPermission;
import com.frog.system.mapper.SysPermissionMapper;
import com.frog.system.mapper.SysRolePermissionMapper;
import com.frog.system.mapper.SysTempPermissionMapper;
import com.frog.system.service.ISysPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 权限表 服务实现类
 * </p>
 *
 * @author author
 * @since 2025-10-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements ISysPermissionService {
    private final SysPermissionMapper sysPermissionMapper;
    private final SysTempPermissionMapper tempPermissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final com.frog.common.security.PermissionChecker permissionChecker;

    /**
     * 检查用户是否有指定权限
     */
    @Cacheable(
            value = "userPermissions",
            key = "#userId + ':' + #permissionCode"
    )
    public boolean hasPermission(UUID userId, String permissionCode) {
        Set<String> permissions = sysPermissionMapper.findAllPermissionsByUserId(userId);
        return permissions.contains(permissionCode);
    }

    /**
     * 检查用户是否有资源权限
     */
    public boolean hasResourcePermission(UUID userId, String resourceType,
                                         Serializable resourceId, String permission) {
        // 实现基于资源的权限控制
        // 例如：检查用户是否可以访问特定部门的数据
        return sysPermissionMapper.checkResourcePermission(userId, resourceType, resourceId, permission);
    }

    /**
     * 获取用户角色
     */
    @Cacheable(
            value = "userRoles",
            key = "#userId"
    )
    public Set<String> getUserRoles(UUID userId) {
        return sysPermissionMapper.findRolesByUserId(userId);
    }

    /**
     * 获取用户权限
     */
    @Cacheable(
            value = "userPermissions",
            key = "#userId"
    )
    public Set<String> getUserPermissions(UUID userId) {
        return sysPermissionMapper.findAllPermissionsByUserId(userId);
    }

    /**
     * 根据 URL和方法查询需要的权限
     */
    public List<String> findPermissionsByUrl(String url, String method) {
        return sysPermissionMapper.findPermissionsByUrl(url, method);
    }

    /**
     * 查询所有 API 类型的权限
     * 用于动态权限加载（DynamicPermissionLoader）
     */
    @Override
    @Cacheable(
            value = "apiPermissions",
            key = "'all'"
    )
    public List<ApiPermissionDTO> findApiPermissions() {
        // 查询所有 API 类型的权限（permissionType = 4）
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getPermissionType, 4) // 4 = API 类型
               .eq(SysPermission::getStatus, 1)         // 只查询启用的权限
               .isNotNull(SysPermission::getApiPath)    // 必须有 API 路径
               .isNotNull(SysPermission::getPermissionCode); // 必须有权限编码

        List<SysPermission> permissions = sysPermissionMapper.selectList(wrapper);

        // 转换为 ApiPermissionDTO
        return permissions.stream()
                .map(permission -> ApiPermissionDTO.builder()
                        .apiPath(permission.getApiPath())
                        .httpMethod(permission.getHttpMethod())
                        .permissionCode(permission.getPermissionCode())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 获取权限树
     */
    @Cacheable(
            value = "permissionTree",
            key = "'all'"
    )
    public List<PermissionDTO> getPermissionTree() {
        List<SysPermission> permissions = sysPermissionMapper.findPermissionTree();

        // 转换为 DTO
        List<PermissionDTO> permissionDTOs = permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 构建树形结构
        return buildTree(permissionDTOs);
    }

    /**
     * 根据 ID查询权限
     */
    @Cacheable(
            value = "permission",
            key = "#id"
    )
    public PermissionDTO getPermissionById(UUID id) {
        SysPermission permission = sysPermissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }
        return convertToDTO(permission);
    }

    /**
     * 新增权限
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"permission", "permissionTree", "userPermissions", "userInfo"},
            allEntries = true
    )
    public void addPermission(PermissionDTO permissionDTO) {
        // 1. 检查操作权限
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "permission:add");

        // 2. 区分平台权限和租户权限的创建
        UUID tenantId = null;
        String permissionScope = permissionDTO.getPermissionScope();

        if ("PLATFORM".equals(permissionScope)) {
            // 创建平台权限 - 只有平台管理员可以创建
            if (!com.frog.common.tenant.TenantValidationUtil.isPlatformAdmin()) {
                throw new BusinessException("PERMISSION_DENIED", "只有平台管理员可以创建平台权限");
            }
            // 平台权限的 tenant_id 为 NULL
            tenantId = null;
        } else {
            // 创建租户权限 - 验证租户上下文
            tenantId = com.frog.common.tenant.TenantValidationUtil.getRequiredTenantId();
            // 自动设置为租户权限
            permissionDTO.setPermissionScope("TENANT");
        }

        // 3. 检查权限编码是否存在
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getPermissionCode, permissionDTO.getPermissionCode());
        if (sysPermissionMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("权限编码已存在");
        }

        // 4. 准备实体
        SysPermission permission = new SysPermission();
        copyPropertiesFromDTO(permissionDTO, permission);
        permission.setId(UUIDv7Util.generate());
        permission.setTenantId(tenantId); // 平台权限为 NULL，租户权限为当前租户ID

        // 5. 数据库操作
        sysPermissionMapper.insert(permission);

        // 6. 记录租户操作日志
        if (tenantId != null) {
            com.frog.common.tenant.TenantValidationUtil.logTenantOperation("CREATE", "PERMISSION", permission.getId());
        }

        log.info("权限创建成功: {} ({}), 操作人: {}", permission.getPermissionCode(),
                permission.getPermissionScope(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 修改权限
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"permission", "permissionTree", "userPermissions", "userInfo"},
            allEntries = true
    )
    public void updatePermission(PermissionDTO permissionDTO) {
        // 1. 检查操作权限
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "permission:update");

        // 2. 查询数据
        SysPermission existPermission = sysPermissionMapper.selectById(permissionDTO.getId());
        if (existPermission == null) {
            throw new BusinessException("权限不存在");
        }

        // 3. 业务校验
        if (permissionDTO.getParentId() != null && permissionDTO.getParentId().equals(permissionDTO.getId())) {
            throw new BusinessException("父节点不能是自己");
        }

        // 4. 验证数据归属（区分平台权限和租户权限）
        if ("PLATFORM".equals(existPermission.getPermissionScope())) {
            // 修改平台权限 - 只有平台管理员可以修改
            if (!com.frog.common.tenant.TenantValidationUtil.isPlatformAdmin()) {
                throw new BusinessException("PERMISSION_DENIED", "只有平台管理员可以修改平台权限");
            }
        } else {
            // 修改租户权限 - 验证租户上下文和数据归属
            UUID tenantId = com.frog.common.tenant.TenantValidationUtil.getRequiredTenantId();
            com.frog.common.tenant.TenantValidationUtil.validateDataOwnership(existPermission.getTenantId());
        }

        // 5. 执行业务逻辑
        SysPermission permission = new SysPermission();
        copyPropertiesFromDTO(permissionDTO, permission);
        permission.setTenantId(existPermission.getTenantId()); // 保持 tenant_id 不变
        permission.setPermissionScope(existPermission.getPermissionScope()); // 保持 permission_scope 不变

        sysPermissionMapper.updateById(permission);

        // 6. 记录日志
        if (existPermission.getTenantId() != null) {
            com.frog.common.tenant.TenantValidationUtil.logTenantOperation("UPDATE", "PERMISSION", permissionDTO.getId());
        }

        log.info("权限更新成功: {} ({}), 操作人: {}", permission.getPermissionCode(),
                permission.getPermissionScope(), SecurityUtils.getCurrentUsername());
    }

    /**
     * 删除权限
     * <p>
     * 删除前检查：
     * <ul>
     *   <li>是否有子权限</li>
     *   <li>是否有角色使用该权限 (sys_role_permission)</li>
     *   <li>是否有用户拥有该临时权限 (sys_temp_permission)</li>
     * </ul>
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(
            value = {"permission", "permissionTree", "userPermissions", "userInfo"},
            allEntries = true
    )
    public void deletePermission(UUID id) {
        // 1. 检查操作权限
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "permission:delete");

        // 2. 查询数据
        SysPermission permission = sysPermissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }

        // 3. 验证数据归属（区分平台权限和租户权限）
        if ("PLATFORM".equals(permission.getPermissionScope())) {
            // 删除平台权限 - 只有平台管理员可以删除
            if (!com.frog.common.tenant.TenantValidationUtil.isPlatformAdmin()) {
                throw new BusinessException("PERMISSION_DENIED", "只有平台管理员可以删除平台权限");
            }
        } else {
            // 删除租户权限 - 验证租户上下文和数据归属
            UUID tenantId = com.frog.common.tenant.TenantValidationUtil.getRequiredTenantId();
            com.frog.common.tenant.TenantValidationUtil.validateDataOwnership(permission.getTenantId());
        }

        // 4. 检查是否有子权限
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getParentId, id);
        Long childCount = sysPermissionMapper.selectCount(wrapper);
        if (childCount > 0) {
            throw new BusinessException("该权限下还有子权限，不能删除");
        }

        // 5. 检查是否有角色使用该权限
        Integer roleCount = rolePermissionMapper.countRolesByPermissionId(id);
        if (roleCount > 0) {
            throw new BusinessException("该权限已被 " + roleCount + " 个角色使用，不能删除");
        }

        // 6. 检查是否有用户拥有该临时权限（有效的临时授权）
        Integer tempPermCount = tempPermissionMapper.countActiveByPermissionId(id);
        if (tempPermCount != null && tempPermCount > 0) {
            throw new BusinessException("该权限正被 " + tempPermCount + " 个用户作为临时权限使用，不能删除");
        }

        // 7. 执行删除
        sysPermissionMapper.deleteById(id);

        // 8. 记录日志
        if (permission.getTenantId() != null) {
            com.frog.common.tenant.TenantValidationUtil.logTenantOperation("DELETE", "PERMISSION", id);
        }

        log.info("权限删除成功: {} ({}), 操作人: {}", permission.getPermissionCode(),
                permission.getPermissionScope(), SecurityUtils.getCurrentUsername());
    }

    private void copyPropertiesFromDTO(PermissionDTO permissionDTO, SysPermission permission) {
        BeanUtils.copyProperties(permissionDTO, permission);
    }

    private PermissionDTO convertToDTO(SysPermission permission) {
        PermissionDTO permissionDTO = new PermissionDTO();
        BeanUtils.copyProperties(permission, permissionDTO);

        return permissionDTO;
    }

    /**
     * 构建树形结构
     */
    private List<PermissionDTO> buildTree(List<PermissionDTO> permissions) {
        // 创建一个Map来存储所有节点，key为id，value为PermissionDTO
        Map<UUID, PermissionDTO> permissionMap = new HashMap<>();
        for (PermissionDTO permission : permissions) {
            permissionMap.put(permission.getId(), permission);
        }

        // 构建树形结构
        List<PermissionDTO> tree = new ArrayList<>();
        for (PermissionDTO permission : permissions) {
            if (permission.getParentId() == null) {
                // 根节点
                buildTreeChildren(permission, permissionMap);
                tree.add(permission);
            }
        }

        return tree;
    }

    /**
     * 递归构建子节点
     */
    private void buildTreeChildren(PermissionDTO parent, Map<UUID, PermissionDTO> permissionMap) {
        List<PermissionDTO> children = new ArrayList<>();
        for (PermissionDTO permission : permissionMap.values()) {
            if (permission.getParentId() != null && permission.getParentId().equals(parent.getId())) {
                buildTreeChildren(permission, permissionMap); // 递归构建子节点的子节点
                children.add(permission);
            }
        }
        parent.setChildren(children);
    }
}
