package com.scmcloud.supplier.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.mapper.SupSupplierMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierQueryService {

    private final SupSupplierMapper supSupplierMapper;

    @Slave
    public SupSupplier getById(String id) {
        return supSupplierMapper.selectById(id);
    }

    @Slave
    public List<SupSupplier> listAll() {
        return supSupplierMapper.selectList(null);
    }

    @Slave
    public Page<SupSupplier> pageQuery(Page<SupSupplier> page, Wrapper<SupSupplier> wrapper) {
        return supSupplierMapper.selectPage(page, wrapper);
    }

    @Slave
    public Page<SupSupplier> pageList(int page, int size, String keyword, Integer supplierType, Integer cooperationStatus, Boolean enabled) {
        log.debug("分页查询供应商: page={}, size={}, keyword={}", page, size, keyword);
        LambdaQueryWrapper<SupSupplier> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(SupSupplier::getSupplierName, keyword)
                    .or().like(SupSupplier::getSupplierCode, keyword)
                    .or().like(SupSupplier::getContactName, keyword));
        }
        if (supplierType != null) {
            wrapper.eq(SupSupplier::getSupplierType, supplierType);
        }
        if (cooperationStatus != null) {
            wrapper.eq(SupSupplier::getCooperationStatus, cooperationStatus);
        }
        if (enabled != null) {
            wrapper.eq(SupSupplier::getEnabled, enabled);
        }
        wrapper.eq(SupSupplier::getDeleted, false);
        wrapper.orderByAsc(SupSupplier::getSortOrder);
        wrapper.orderByDesc(SupSupplier::getCreateTime);
        return supSupplierMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<SupSupplier> listActive() {
        log.debug("查询所有启用且合作中的供应商");
        LambdaQueryWrapper<SupSupplier> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SupSupplier::getEnabled, true);
        wrapper.eq(SupSupplier::getCooperationStatus, 1);
        wrapper.eq(SupSupplier::getDeleted, false);
        wrapper.orderByAsc(SupSupplier::getSortOrder);
        return supSupplierMapper.selectList(wrapper);
    }

    @Slave
    public List<SupSupplier> searchByName(String name) {
        if (!StringUtils.hasText(name)) {
            return List.of();
        }
        log.debug("按名称搜索供应商: name={}", name);
        LambdaQueryWrapper<SupSupplier> wrapper = Wrappers.lambdaQuery();
        wrapper.like(SupSupplier::getSupplierName, name);
        wrapper.eq(SupSupplier::getDeleted, false);
        wrapper.last("LIMIT 20");
        return supSupplierMapper.selectList(wrapper);
    }
}
