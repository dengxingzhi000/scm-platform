package scm.tenant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import scm.tenant.domain.entity.TenantPackage;
import scm.tenant.mapper.TenantPackageMapper;
import scm.tenant.service.ITenantPackageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TenantPackageServiceImpl extends ServiceImpl<TenantPackageMapper, TenantPackage> implements ITenantPackageService {

    public TenantPackage createPackage(TenantPackage entity) {
        log.info("创建租户套餐: packageCode={}, packageName={}", entity.getPackageCode(), entity.getPackageName());
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        entity.setDeleted(false);
        if (entity.getEnabled() == null) {
            entity.setEnabled(true);
        }
        save(entity);
        log.info("租户套餐创建成功: id={}", entity.getId());
        return entity;
    }

    public TenantPackage getById(String id) {
        return lambdaQuery()
                .eq(TenantPackage::getId, id)
                .eq(TenantPackage::getDeleted, false)
                .one();
    }

    public TenantPackage updatePackage(TenantPackage entity) {
        log.info("更新租户套餐: id={}", entity.getId());
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
        return entity;
    }

    public boolean deleteById(String id) {
        log.info("删除租户套餐: id={}", id);
        TenantPackage pkg = new TenantPackage();
        pkg.setId(id);
        pkg.setDeleted(true);
        pkg.setUpdateTime(LocalDateTime.now());
        return updateById(pkg);
    }

    public List<TenantPackage> listActive() {
        return lambdaQuery()
                .eq(TenantPackage::getDeleted, false)
                .eq(TenantPackage::getEnabled, true)
                .orderByAsc(TenantPackage::getSortOrder)
                .list();
    }

    public Page<TenantPackage> pageQuery(int page, int size, String packageName, Integer packageLevel, Boolean enabled) {
        log.debug("分页查询租户套餐: page={}, size={}, packageName={}, packageLevel={}, enabled={}",
                page, size, packageName, packageLevel, enabled);

        LambdaQueryWrapper<TenantPackage> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(packageName)) {
            wrapper.like(TenantPackage::getPackageName, packageName);
        }
        if (packageLevel != null) {
            wrapper.eq(TenantPackage::getPackageLevel, packageLevel);
        }
        if (enabled != null) {
            wrapper.eq(TenantPackage::getEnabled, enabled);
        }
        wrapper.eq(TenantPackage::getDeleted, false);
        wrapper.orderByAsc(TenantPackage::getSortOrder);

        Page<TenantPackage> pageParam = new Page<>(page, size);
        return page(pageParam, wrapper);
    }
}
