package com.scmcloud.logistics.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.logistics.domain.entity.TmsDeliveryArea;
import com.scmcloud.logistics.mapper.TmsDeliveryAreaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsDeliveryAreaQueryService {

    private final TmsDeliveryAreaMapper tmsDeliveryAreaMapper;

    @Slave
    public TmsDeliveryArea getById(String id) {
        return tmsDeliveryAreaMapper.selectById(id);
    }

    @Slave
    public List<TmsDeliveryArea> listAll() {
        return tmsDeliveryAreaMapper.selectList(null);
    }

    @Slave
    public Page<TmsDeliveryArea> pageQuery(Page<TmsDeliveryArea> page, Wrapper<TmsDeliveryArea> wrapper) {
        return tmsDeliveryAreaMapper.selectPage(page, wrapper);
    }

    @Slave
    public Page<TmsDeliveryArea> pageList(int page, int size, String carrierId, String province, String city) {
        log.debug("查询配送区域列表: page={}, size={}, carrierId={}, province={}, city={}", page, size, carrierId, province, city);
        LambdaQueryWrapper<TmsDeliveryArea> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(carrierId)) {
            wrapper.eq(TmsDeliveryArea::getCarrierId, carrierId);
        }
        if (StringUtils.hasText(province)) {
            wrapper.eq(TmsDeliveryArea::getProvince, province);
        }
        if (StringUtils.hasText(city)) {
            wrapper.eq(TmsDeliveryArea::getCity, city);
        }
        wrapper.eq(TmsDeliveryArea::getDeleted, false);
        wrapper.orderByDesc(TmsDeliveryArea::getCreateTime);
        return tmsDeliveryAreaMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<TmsDeliveryArea> listByCarrier(String carrierId) {
        log.debug("根据物流商查询配送区域: carrierId={}", carrierId);
        LambdaQueryWrapper<TmsDeliveryArea> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsDeliveryArea::getCarrierId, carrierId);
        wrapper.eq(TmsDeliveryArea::getDeleted, false);
        return tmsDeliveryAreaMapper.selectList(wrapper);
    }

    @Slave
    public boolean checkCoverage(String carrierId, String province, String city, String district) {
        log.debug("检查区域覆盖: carrierId={}, province={}, city={}, district={}", carrierId, province, city, district);
        LambdaQueryWrapper<TmsDeliveryArea> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsDeliveryArea::getCarrierId, carrierId);
        wrapper.eq(TmsDeliveryArea::getEnabled, true);
        wrapper.eq(TmsDeliveryArea::getDeleted, false);
        wrapper.eq(TmsDeliveryArea::getProvince, province);
        wrapper.eq(TmsDeliveryArea::getCity, city);
        if (StringUtils.hasText(district)) {
            wrapper.and(w -> w.like(TmsDeliveryArea::getDistricts, district)
                    .or().isNull(TmsDeliveryArea::getDistricts)
                    .or().eq(TmsDeliveryArea::getDistricts, ""));
        }
        return tmsDeliveryAreaMapper.selectCount(wrapper) > 0;
    }
}
