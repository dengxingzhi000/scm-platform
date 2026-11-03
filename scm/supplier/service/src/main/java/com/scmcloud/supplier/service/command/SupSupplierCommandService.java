package com.scmcloud.supplier.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.supplier.domain.entity.SupSupplier;
import com.scmcloud.supplier.mapper.SupSupplierMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SupSupplierCommandService {

    private final SupSupplierMapper supSupplierMapper;

    @Master(reason = "保存供应商")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(SupSupplier entity) {
        return supSupplierMapper.insert(entity) > 0;
    }

    @Master(reason = "更新供应商")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(SupSupplier entity) {
        return supSupplierMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除供应商")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return supSupplierMapper.deleteById(id) > 0;
    }

    @Master(reason = "启用供应商")
    @Transactional(rollbackFor = Exception.class)
    public boolean enableSupplier(String id) {
        log.info("启用供应商: id={}", id);
        SupSupplier supplier = supSupplierMapper.selectById(id);
        if (supplier == null) {
            log.warn("供应商不存在: id={}", id);
            return false;
        }
        supplier.setEnabled(true);
        supplier.setUpdateTime(LocalDateTime.now());
        return supSupplierMapper.updateById(supplier) > 0;
    }

    @Master(reason = "停用供应商")
    @Transactional(rollbackFor = Exception.class)
    public boolean disableSupplier(String id) {
        log.info("停用供应商: id={}", id);
        SupSupplier supplier = supSupplierMapper.selectById(id);
        if (supplier == null) {
            log.warn("供应商不存在: id={}", id);
            return false;
        }
        supplier.setEnabled(false);
        supplier.setUpdateTime(LocalDateTime.now());
        return supSupplierMapper.updateById(supplier) > 0;
    }
}
