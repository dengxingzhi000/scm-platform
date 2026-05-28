package scm.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import scm.warehouse.domain.entity.WmsWarehouse;
import scm.warehouse.mapper.WmsWarehouseMapper;
import scm.warehouse.service.IWmsWarehouseService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class WmsWarehouseServiceImpl extends ServiceImpl<WmsWarehouseMapper, WmsWarehouse>
        implements IWmsWarehouseService {

    @Override
    public List<WmsWarehouse> listEnabled() {
        return lambdaQuery()
                .eq(WmsWarehouse::getEnabled, true)
                .eq(WmsWarehouse::getDeleted, false)
                .orderByAsc(WmsWarehouse::getSortOrder)
                .list();
    }

    @Override
    public Page<WmsWarehouse> pageList(int page, int size, String warehouseName, Integer warehouseType, Boolean enabled) {
        LambdaQueryWrapper<WmsWarehouse> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsWarehouse::getDeleted, false);

        if (StringUtils.hasText(warehouseName)) {
            wrapper.like(WmsWarehouse::getWarehouseName, warehouseName);
        }
        if (warehouseType != null) {
            wrapper.eq(WmsWarehouse::getWarehouseType, warehouseType);
        }
        if (enabled != null) {
            wrapper.eq(WmsWarehouse::getEnabled, enabled);
        }
        wrapper.orderByAsc(WmsWarehouse::getSortOrder);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enable(String id) {
        WmsWarehouse warehouse = getById(id);
        if (warehouse == null) {
            log.warn("仓库不存在: id={}", id);
            return false;
        }
        warehouse.setEnabled(true);
        warehouse.setUpdateTime(LocalDateTime.now());
        boolean success = updateById(warehouse);
        if (success) {
            log.info("仓库已启用: id={}, name={}", id, warehouse.getWarehouseName());
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disable(String id) {
        WmsWarehouse warehouse = getById(id);
        if (warehouse == null) {
            log.warn("仓库不存在: id={}", id);
            return false;
        }
        warehouse.setEnabled(false);
        warehouse.setUpdateTime(LocalDateTime.now());
        boolean success = updateById(warehouse);
        if (success) {
            log.info("仓库已停用: id={}, name={}", id, warehouse.getWarehouseName());
        }
        return success;
    }
}
