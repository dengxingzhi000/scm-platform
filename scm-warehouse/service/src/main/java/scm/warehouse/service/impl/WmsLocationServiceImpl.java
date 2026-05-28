package scm.warehouse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import scm.warehouse.domain.entity.WmsLocation;
import scm.warehouse.mapper.WmsLocationMapper;
import scm.warehouse.service.IWmsLocationService;

import java.util.List;

@Slf4j
@Service
public class WmsLocationServiceImpl extends ServiceImpl<WmsLocationMapper, WmsLocation>
        implements IWmsLocationService {

    @Override
    public List<WmsLocation> listByWarehouseId(String warehouseId) {
        return lambdaQuery()
                .eq(WmsLocation::getWarehouseId, warehouseId)
                .eq(WmsLocation::getDeleted, false)
                .orderByAsc(WmsLocation::getZone)
                .orderByAsc(WmsLocation::getShelf)
                .orderByAsc(WmsLocation::getLayer)
                .orderByAsc(WmsLocation::getPosition)
                .list();
    }

    @Override
    public Page<WmsLocation> pageList(int page, int size, String warehouseId, Integer locationType, Integer status) {
        LambdaQueryWrapper<WmsLocation> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(WmsLocation::getDeleted, false);

        if (StringUtils.hasText(warehouseId)) {
            wrapper.eq(WmsLocation::getWarehouseId, warehouseId);
        }
        if (locationType != null) {
            wrapper.eq(WmsLocation::getLocationType, locationType);
        }
        if (status != null) {
            wrapper.eq(WmsLocation::getStatus, status);
        }
        wrapper.orderByAsc(WmsLocation::getZone)
                .orderByAsc(WmsLocation::getShelf)
                .orderByAsc(WmsLocation::getLayer)
                .orderByAsc(WmsLocation::getPosition);

        return page(new Page<>(page, size), wrapper);
    }
}
