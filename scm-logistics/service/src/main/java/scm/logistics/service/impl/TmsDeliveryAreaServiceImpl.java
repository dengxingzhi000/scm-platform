package scm.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import scm.logistics.domain.entity.TmsDeliveryArea;
import scm.logistics.mapper.TmsDeliveryAreaMapper;
import scm.logistics.service.ITmsDeliveryAreaService;

import java.util.List;

@Slf4j
@Service
public class TmsDeliveryAreaServiceImpl extends ServiceImpl<TmsDeliveryAreaMapper, TmsDeliveryArea>
        implements ITmsDeliveryAreaService {

    @Override
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

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<TmsDeliveryArea> listByCarrier(String carrierId) {
        log.debug("根据物流商查询配送区域: carrierId={}", carrierId);
        return lambdaQuery()
                .eq(TmsDeliveryArea::getCarrierId, carrierId)
                .eq(TmsDeliveryArea::getDeleted, false)
                .list();
    }

    @Override
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

        return count(wrapper) > 0;
    }
}
