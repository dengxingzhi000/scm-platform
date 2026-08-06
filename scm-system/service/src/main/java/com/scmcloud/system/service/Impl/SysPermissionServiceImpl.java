package com.scmcloud.system.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.response.ResultCode;
import com.scmcloud.common.security.PermissionChecker;
import com.scmcloud.common.tenant.TenantValidationUtil;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.common.dto.permission.ApiPermissionDTO;
import com.scmcloud.common.dto.permission.PermissionDTO;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.system.domain.entity.SysPermission;
import com.scmcloud.system.mapper.SysPermissionMapper;
import com.scmcloud.system.mapper.SysRolePermissionMapper;
import com.scmcloud.system.mapper.SysTempPermissionMapper;
import com.scmcloud.system.service.ISysPermissionService;
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
 * 权限服务实现
 *
 * @author author
 * @since 2025-10-14
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SysPermissionServiceImpl extends ServiceImpl<SysPermissionMapper, SysPermission>
        implements ISysPermissionService {
    private final SysTempPermissionMapper tempPermissionMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final PermissionChecker permissionChecker;

    private static final int PERMISSION_TYPE_API = 4;

    @Override
    @Cacheable(value = "userPermissions", key = "#userId + ':' + #permissionCode")
    public boolean hasPermission(UUID userId, String permissionCode) {
        Set<String> permissions = getBaseMapper().findAllPermissionsByUserId(userId);
        return permissions.contains(permissionCode);
    }

    @Override
    public boolean hasResourcePermission(UUID userId, String resourceType,
                                         Serializable resourceId, String permission) {
        return getBaseMapper().checkResourcePermission(userId, resourceType, resourceId, permission);
    }

    @Override
    @Cacheable(value = "userRoles", key = "#userId")
    public Set<String> getUserRoles(UUID userId) {
        return getBaseMapper().findRolesByUserId(userId);
    }

    @Override
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> getUserPermissions(UUID userId) {
        return getBaseMapper().findAllPermissionsByUserId(userId);
    }

    @Override
    public List<String> findPermissionsByUrl(String url, String method) {
        return getBaseMapper().findPermissionsByUrl(url, method);
    }

    /**
     * 查询所有 API 类型的权限
     * 用于动态权限加载（DynamicPermissionLoader）
     */
    @Override
    @Cacheable(value = "apiPermissions", key = "'all'")
    public List<ApiPermissionDTO> findApiPermissions() {
        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getPermissionType, PERMISSION_TYPE_API)
               .eq(SysPermission::getStatus, 1)
               .isNotNull(SysPermission::getApiPath)
               .isNotNull(SysPermission::getPermissionCode);

        List<SysPermission> permissions = getBaseMapper().selectList(wrapper);

        return permissions.stream()
                .map(permission -> ApiPermissionDTO.builder()
                        .apiPath(permission.getApiPath())
                        .httpMethod(permission.getHttpMethod())
                        .permissionCode(permission.getPermissionCode())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "permissionTree", key = "'all'")
    public List<PermissionDTO> getPermissionTree() {
        List<SysPermission> permissions = getBaseMapper().findPermissionTree();

        List<PermissionDTO> permissionDTOs = permissions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return buildTree(permissionDTOs);
    }

    @Override
    @Cacheable(value = "permission", key = "#id")
    public PermissionDTO getPermissionById(UUID id) {
        SysPermission permission = getBaseMapper().selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }
        return convertToDTO(permission);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"permission", "permissionTree", "userPermissions", "userInfo"}, allEntries = true)
    public void addPermission(PermissionDTO permissionDTO) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "permission:add");

        UUID tenantId = resolveTenantIdForPermission(permissionDTO.getPermissionScope());

        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getPermissionCode, permissionDTO.getPermissionCode());
        if (getBaseMapper().selectCount(wrapper) > 0) {
            throw new BusinessException("权限编码已存在");
        }

        SysPermission permission = new SysPermission();
        BeanUtils.copyProperties(permissionDTO, permission);
        permission.setId(UUIDv7Util.generate());
        permission.setTenantId(tenantId);

        getBaseMapper().insert(permission);

        logTenantOperation("CREATE", permission.getId(), tenantId);

        log.info("权限创建成功: {} ({}), 操作 {}", permission.getPermissionCode(),
                permission.getPermissionScope(), SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"permission", "permissionTree", "userPermissions", "userInfo"}, allEntries = true)
    public void updatePermission(PermissionDTO permissionDTO) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "permission:update");

        SysPermission existPermission = getBaseMapper().selectById(permissionDTO.getId());
        if (existPermission == null) {
            throw new BusinessException("权限不存在");
        }

        if (permissionDTO.getParentId() != null && permissionDTO.getParentId().equals(permissionDTO.getId())) {
            throw new BusinessException("父节点不能是自己");
        }

        requirePermissionOwnership(existPermission);

        SysPermission permission = new SysPermission();
        BeanUtils.copyProperties(permissionDTO, permission);
        permission.setTenantId(existPermission.getTenantId());
        permission.setPermissionScope(existPermission.getPermissionScope());

        getBaseMapper().updateById(permission);

        logTenantOperation("UPDATE", permissionDTO.getId(), existPermission.getTenantId());

        log.info("权限更新成功: {} ({}), 操作 {}", permission.getPermissionCode(),
                permission.getPermissionScope(), SecurityUtils.getCurrentUsername());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"permission", "permissionTree", "userPermissions", "userInfo"}, allEntries = true)
    public void deletePermission(UUID id) {
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "permission:delete");

        SysPermission permission = getBaseMapper().selectById(id);
        if (permission == null) {
            throw new BusinessException("权限不存在");
        }

        requirePermissionOwnership(permission);

        LambdaQueryWrapper<SysPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPermission::getParentId, id);
        Long childCount = getBaseMapper().selectCount(wrapper);
        if (childCount > 0) {
            throw new BusinessException("该权限下还有子权限，不能删除");
        }

        Integer roleCount = rolePermissionMapper.countRolesByPermissionId(id);
        if (roleCount > 0) {
            throw new BusinessException("该权限已被 " + roleCount + " 个角色使用，不能删除");
        }

        Integer tempPermCount = tempPermissionMapper.countActiveByPermissionId(id);
        if (tempPermCount != null && tempPermCount > 0) {
            throw new BusinessException("该权限正被 " + tempPermCount + " 个用户作为临时权限使用，不能删除");
        }

        getBaseMapper().deleteById(id);

        logTenantOperation("DELETE", id, permission.getTenantId());

        log.info("权限删除成功: {} ({}), 操作 {}", permission.getPermissionCode(),
                permission.getPermissionScope(), SecurityUtils.getCurrentUsername());
    }

    private void requirePermissionOwnership(SysPermission permission) {
        if ("PLATFORM".equals(permission.getPermissionScope())) {
            if (TenantValidationUtil.isTenantUser()) {
                throw new BusinessException(
                        ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getCode(),
                        "只有平台管理员可以操作平台权限"
                );
            }
        } else {
            TenantValidationUtil.validateDataOwnership(permission.getTenantId());
        }
    }

    private UUID resolveTenantIdForPermission(String permissionScope) {
        if ("PLATFORM".equals(permissionScope)) {
            if (TenantValidationUtil.isTenantUser()) {
                throw new BusinessException(
                        ResultCode.PLATFORM_RESOURCE_ACCESS_DENIED.getCode(),
                        "只有平台管理员可以创建平台权限"
                );
            }
            return null;
        }
        return TenantValidationUtil.getRequiredTenantId();
    }

    private void logTenantOperation(String action, UUID entityId, UUID tenantId) {
        if (tenantId != null) {
            TenantValidationUtil.logTenantOperation(action, "PERMISSION", entityId);
        }
    }

    private PermissionDTO convertToDTO(SysPermission permission) {
        PermissionDTO permissionDTO = new PermissionDTO();
        BeanUtils.copyProperties(permission, permissionDTO);
        return permissionDTO;
    }

    private List<PermissionDTO> buildTree(List<PermissionDTO> permissions) {
        Map<UUID, PermissionDTO> permissionMap = new HashMap<>();
        for (PermissionDTO permission : permissions) {
            permissionMap.put(permission.getId(), permission);
        }

        List<PermissionDTO> tree = new ArrayList<>();
        for (PermissionDTO permission : permissions) {
            if (permission.getParentId() == null) {
                buildTreeChildren(permission, permissionMap);
                tree.add(permission);
            }
        }

        return tree;
    }

    private void buildTreeChildren(PermissionDTO parent, Map<UUID, PermissionDTO> permissionMap) {
        List<PermissionDTO> children = new ArrayList<>();
        for (PermissionDTO permission : permissionMap.values()) {
            if (permission.getParentId() != null && permission.getParentId().equals(parent.getId())) {
                buildTreeChildren(permission, permissionMap);
                children.add(permission);
            }
        }
        parent.setChildren(children);
    }
}
