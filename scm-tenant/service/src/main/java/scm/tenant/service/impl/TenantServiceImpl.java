package scm.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scm.tenant.domain.entity.Tenant;
import scm.tenant.mapper.TenantMapper;
import scm.tenant.service.ITenantService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements ITenantService {

    public Tenant createTenant(Tenant entity) {
        log.info("创建租户: tenantCode={}, tenantName={}", entity.getTenantCode(), entity.getTenantName());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        if (entity.getStatus() == null) {
            entity.setStatus(0);
        }
        save(entity);
        log.info("租户创建成功: id={}", entity.getId());
        return entity;
    }

    public Tenant getById(String id) {
        return lambdaQuery()
                .eq(Tenant::getId, id)
                .eq(Tenant::getDeleted, false)
                .one();
    }

    public Tenant updateTenant(Tenant entity) {
        log.info("更新租户: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    public boolean deleteById(String id) {
        log.info("删除租户: id={}", id);
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setDeleted(true);
        tenant.setUpdateTime(LocalDateTime.now());
        return updateById(tenant);
    }

    public boolean enableTenant(String id) {
        log.info("启用租户: id={}", id);
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setStatus(1);
        tenant.setActivatedAt(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        return updateById(tenant);
    }

    public boolean disableTenant(String id) {
        log.info("禁用租户: id={}", id);
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setStatus(2);
        tenant.setSuspendedAt(LocalDateTime.now());
        tenant.setUpdateTime(LocalDateTime.now());
        return updateById(tenant);
    }

    public List<Tenant> listActive() {
        return lambdaQuery()
                .eq(Tenant::getDeleted, false)
                .in(Tenant::getStatus, 0, 1)
                .orderByDesc(Tenant::getCreateTime)
                .list();
    }

    public Page<Tenant> pageQuery(int page, int size, String tenantName, Integer tenantType, Integer status) {
        log.debug("分页查询租户: page={}, size={}, tenantName={}, tenantType={}, status={}",
                page, size, tenantName, tenantType, status);

        LambdaQueryWrapper<Tenant> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(tenantName)) {
            wrapper.like(Tenant::getTenantName, tenantName);
        }
        if (tenantType != null) {
            wrapper.eq(Tenant::getTenantType, tenantType);
        }
        if (status != null) {
            wrapper.eq(Tenant::getStatus, status);
        }
        wrapper.eq(Tenant::getDeleted, false);
        wrapper.orderByDesc(Tenant::getCreateTime);

        Page<Tenant> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
