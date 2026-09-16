package com.scmcloud.system.service.Impl;

import com.scmcloud.common.dto.dept.DeptDTO;
import com.scmcloud.common.exception.BusinessException;
import com.scmcloud.common.tenant.TenantValidationUtil;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.common.web.util.SecurityUtils;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.system.domain.entity.SysDept;
import com.scmcloud.system.event.DataSyncEventPublisher;
import com.scmcloud.system.mapper.SysDeptMapper;
import com.scmcloud.system.service.ISysDeptService;
import com.scmcloud.system.service.command.DeptRoleCrossDatabaseCommandService;
import com.scmcloud.system.service.query.DeptCrossDatabaseQueryService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
/**
 * <p>
 * 部门服务实现
 * </p>
 *
 * @author author
 * @since 2025-11-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {
    private final SysDeptMapper deptMapper;
    private final DeptCrossDatabaseQueryService deptQueryService;
    private final DeptRoleCrossDatabaseCommandService deptRoleCommandService;
    private final DataSyncEventPublisher dataSyncEventPublisher;
    private final com.scmcloud.common.security.PermissionChecker permissionChecker;

    /**
     * 查询部门
     * <p>
     * 注意：此方法使用冗余字段获取负责人信息（无需跨库
     * 用户数统计通过 CrossDatabaseQueryService 批量跨库查询 db_user 实现（性能优化
     * <p>
     * 查询走从
     * <p>
     * 多租户隔离：通过 MyBatis-Plus TenantLineHandler 自动过滤 tenant_id
     */
    @Override
    @Slave
    @Cacheable(value = "deptTree", key = "#root.target.getTenantCacheKey()")
    public List<DeptDTO> getDeptTree() {
        // 验证租户上下文（查询会自动应tenant_id 过滤
        TenantValidationUtil.getRequiredTenantId();

        // 1. org 库查询当前租户的所有部门（使用冗余字段包含负责人信息）
        // MyBatis-Plus 拦截器会自动添加 WHERE tenant_id = #{tenantId} 条件
        List<SysDept> depts = deptMapper.selectDeptTreeWithLeader();

        // 2. 收集所有部ID，用于批量统计用户数
        List<UUID> deptIds = depts.stream().map(SysDept::getId).toList();

        // 3. 通过 CrossDatabaseQueryService user 库批量统计每个部门的用户数（单次跨库查询，性能优化
        Map<UUID, Integer> userCountMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            userCountMap = deptQueryService.countUsersByDeptIds(deptIds);
        }

        // 4. 批量统计每个部门的子部门数（避免 N+1 查询
        Map<UUID, Integer> childCountMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            Map<UUID, Map<String, Object>> childCountResult = deptMapper.countChildrenByDeptIds(deptIds);
            if (childCountResult != null) {
                childCountResult.forEach((parentId, row) -> {
                    Object count = row.get("child_count");
                    childCountMap.put(parentId, count != null ? ((Number) count).intValue() : 0);
                });
            }
        }

        // 5. 转换DTO 并填充统计信息
        Map<UUID, Integer> finalUserCountMap = userCountMap;
        List<DeptDTO> allDepts = depts.stream().map(dept -> {
            DeptDTO dto = new DeptDTO();
            BeanUtils.copyProperties(dept, dto);
            dto.setLeaderName(dept.getLeaderName());  // 使用冗余字段
            dto.setUserCount(finalUserCountMap.getOrDefault(dept.getId(), 0));
            dto.setChildCount(childCountMap.getOrDefault(dept.getId(), 0));
            return dto;
        }).toList();

        // 6. 构建树形结构
        return buildTree(new ArrayList<>(allDepts));
    }

    /**
     * 查询子部门（包含自身 递归查询
     * <p>
     * 查询走从
     * <p>
     * 多租户隔离：通过 MyBatis-Plus TenantLineHandler 自动过滤 tenant_id
     */
    @Override
    @Slave
    @Cacheable(value = "deptChildren", key = "#deptId + ':' + #root.target.getTenantCacheKey()")
    public List<UUID> getDeptAndChildren(UUID deptId) {
        // 验证租户上下文（查询会自动应tenant_id 过滤
        TenantValidationUtil.getRequiredTenantId();

        // MyBatis-Plus 拦截器会自动添加 WHERE tenant_id = #{tenantId} 条件
        return deptMapper.selectDeptAndChildren(deptId);
    }

    /**
     * 新增部门
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"deptTree", "deptChildren"}, allEntries = true)
    public void addDept(DeptDTO deptDTO) {
        // 1. 验证租户上下
        UUID tenantId = com.scmcloud.common.tenant.TenantValidationUtil.getRequiredTenantId();

        // 2. 检查操作权
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "dept:add");

        // 3. 业务校验
        // 3.1 校验部门编码唯一性（租户内唯一
        if (deptMapper.existsByDeptCode(deptDTO.getDeptCode())) {
            throw new BusinessException("部门编码已存在");
        }

        // 3.2 校验父部门是否存在并属于当前租户
        if (deptDTO.getParentId() != null && !deptDTO.getParentId().equals(
                UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
            SysDept parent = deptMapper.selectById(deptDTO.getParentId());
            if (parent == null) {
                throw new BusinessException("父部门不存在");
            }
            // 验证父部门归
            TenantValidationUtil.validateDataOwnership(parent.getTenantId());
        }

        // 4. 准备实体并设置租户ID
        SysDept dept = new SysDept();
        BeanUtils.copyProperties(deptDTO, dept);
        dept.setId(UUIDv7Util.generate());
        dept.setTenantId(tenantId);  // 自动设置租户 ID

        // 5. 执行业务逻辑
        deptMapper.insert(dept);

        // 6. 发布同步事件
        dataSyncEventPublisher.publishDeptCreated(dept);

        // 7. 记录租户操作日志
        TenantValidationUtil.logTenantOperation("CREATE", "DEPT", dept.getId());

        log.info("部门创建成功: {}, 租户: {}, 操作 {}", dept.getDeptName(),
                tenantId, SecurityUtils.getCurrentUsername());
    }

    /**
     * 修改部门
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"deptTree", "deptChildren"}, allEntries = true)
    public void updateDept(DeptDTO deptDTO) {
        // 1. 验证租户上下
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();

        // 2. 检查操作权
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "dept:update");

        // 3. 查询数据
        SysDept existDept = deptMapper.selectById(deptDTO.getId());
        if (existDept == null) {
            throw new BusinessException("部门不存在");
        }

        // 4. 验证数据归属（tenant_id
        TenantValidationUtil.validateDataOwnership(existDept.getTenantId());

        // 5. 业务校验
        // 5.1 不能将父部门设置为自己或自己的子部门
        if (deptDTO.getParentId() != null) {
            if (deptDTO.getParentId().equals(deptDTO.getId())) {
                throw new BusinessException("父部门不能是自己");
            }

            // 检查是否是子部门
            List<UUID> children = deptMapper.selectDeptAndChildren(deptDTO.getId());
            if (children.contains(deptDTO.getParentId())) {
                throw new BusinessException("父部门不能是自己的子部门");
            }

            // 5.2 验证新的父部门也属于当前租户
            if (!deptDTO.getParentId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
                SysDept newParent = deptMapper.selectById(deptDTO.getParentId());
                if (newParent == null) {
                    throw new BusinessException("父部门不存在");
                }
                TenantValidationUtil.validateDataOwnership(newParent.getTenantId());
            }
        }

        // 6. 执行业务逻辑
        SysDept dept = new SysDept();
        BeanUtils.copyProperties(deptDTO, dept);
        dept.setTenantId(existDept.getTenantId()); // 保持 tenant_id 不变

        deptMapper.updateById(dept);

        // 7. 发布同步事件
        SysDept updatedDept = deptMapper.selectById(dept.getId());
        dataSyncEventPublisher.publishDeptUpdated(updatedDept);

        // 8. 记录租户操作日志
        TenantValidationUtil.logTenantOperation("UPDATE", "DEPT", deptDTO.getId());

        log.info("部门修改成功: {}, 租户: {}, 操作 {}", dept.getDeptName(),
                tenantId, SecurityUtils.getCurrentUsername());
    }

    /**
     * 删除部门
     * <p>
     * 删除部门时会同时清理以下关联数据
     * <ul>
     *   <li>sys_role_dept (db_permission) - 角色与部门的数据权限关联</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"deptTree", "deptChildren"}, allEntries = true)
    public void deleteDept(UUID id) {
        // 1. 验证租户上下
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();

        // 2. 检查操作权
        UUID operatorId = SecurityUtils.getCurrentUserUuid().orElse(null);
        permissionChecker.requirePermission(operatorId, "dept:delete");

        // 3. 查询数据
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException("部门不存在");
        }

        // 4. 验证数据归属（tenant_id
        TenantValidationUtil.validateDataOwnership(dept.getTenantId());

        // 5. 业务校验
        // 5.1 检查是否有子部门
        if (hasChildren(id)) {
            throw new BusinessException("该部门下还有子部门，不能删除");
        }

        // 5.2 检查是否有用户（跨库查db_user
        int userCount = countUsersByDeptId(id);
        if (userCount > 0) {
            throw new BusinessException("该部门下还有 " + userCount + " 个用户，不能删除");
        }

        // 6. 执行业务逻辑
        // 6.1 清理角色部门关联数据 (sys_role_dept in db_permission) - 跨库操作
        int deletedRoleDeptCount = deptRoleCommandService.deleteRoleDeptsByDeptId(id);
        if (deletedRoleDeptCount > 0) {
            log.debug("已清理部门{} 的角色关联记录 {} 个", dept.getDeptName(), deletedRoleDeptCount);
        }

        // 6.2 删除部门记录
        deptMapper.deleteById(id);

        // 7. 发布同步事件
        dataSyncEventPublisher.publishDeptDeleted(id);

        // 8. 记录租户操作日志
        TenantValidationUtil.logTenantOperation("DELETE", "DEPT", id);

        log.info("部门删除成功: {}, 租户: {}, 操作 {}", dept.getDeptName(),
                tenantId, SecurityUtils.getCurrentUsername());
    }

    /**
     * 检查部门下是否有用
     * <p>
     * 跨库查询 db_user
     */
    @Override
    public boolean hasUsers(UUID deptId) {
        return countUsersByDeptId(deptId) > 0;
    }

    /**
     * 检查部门下是否有子部门
     */
    @Override
    public boolean hasChildren(UUID deptId) {
        Integer count = deptMapper.countChildren(deptId);
        return count != null && count > 0;
    }

    /**
     * 统计部门下的用户
     * <p>
     * 通过 CrossDatabaseQueryService 跨库查询 db_user
     */
    private int countUsersByDeptId(UUID deptId) {
        return deptQueryService.countUsersByDeptId(deptId);
    }

    // ========== 私有方法 ==========

    /**
     * 构建树形结构
     */
    private List<DeptDTO> buildTree(List<DeptDTO> depts) {
        Map<UUID, DeptDTO> deptMap = new HashMap<>();
        for (DeptDTO dept : depts) {
            deptMap.put(dept.getId(), dept);
        }

        List<DeptDTO> tree = new ArrayList<>();
        for (DeptDTO dept : depts) {
            // 根节点
            if (dept.getParentId() == null ||
                    dept.getParentId().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) {
                buildTreeChildren(dept, deptMap);
                tree.add(dept);
            }
        }

        return tree;
    }

    /**
     * 递归构建子节点
     */
    private void buildTreeChildren(DeptDTO parent, Map<UUID, DeptDTO> deptMap) {
        List<DeptDTO> children = new ArrayList<>();
        for (DeptDTO dept : deptMap.values()) {
            if (dept.getParentId() != null && dept.getParentId().equals(parent.getId())) {
                buildTreeChildren(dept, deptMap);
                children.add(dept);
            }
        }
        parent.setChildren(children);
    }

    /**
     * 获取租户缓存键（用于 @Cacheable key 表达式）
     * <p>
     * 使每个租户的缓存数据独立隔离
     *
     * @return 租户ID字符串，用于缓存key生成
     */
    public String getTenantCacheKey() {
        UUID tenantId = TenantValidationUtil.getRequiredTenantId();
        return tenantId.toString();
    }
}
