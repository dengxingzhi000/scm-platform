package scm.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import scm.logistics.domain.entity.TmsRoute;
import scm.logistics.mapper.TmsRouteMapper;
import scm.logistics.service.ITmsRouteService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class TmsRouteServiceImpl extends ServiceImpl<TmsRouteMapper, TmsRoute> implements ITmsRouteService {

    @Override
    public Page<TmsRoute> pageList(int page, int size, String courierId, Integer status, LocalDate deliveryDate) {
        log.debug("查询配送路线列表: page={}, size={}, courierId={}, status={}, deliveryDate={}", page, size, courierId, status, deliveryDate);

        LambdaQueryWrapper<TmsRoute> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(courierId)) {
            wrapper.eq(TmsRoute::getCourierId, courierId);
        }
        if (status != null) {
            wrapper.eq(TmsRoute::getStatus, status);
        }
        if (deliveryDate != null) {
            wrapper.eq(TmsRoute::getDeliveryDate, deliveryDate);
        }
        wrapper.orderByDesc(TmsRoute::getCreateTime);

        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<TmsRoute> listByCourierId(String courierId) {
        log.debug("根据配送员查询路线: courierId={}", courierId);
        return lambdaQuery()
                .eq(TmsRoute::getCourierId, courierId)
                .orderByDesc(TmsRoute::getDeliveryDate)
                .list();
    }

    @Override
    public TmsRoute getByRouteNo(String routeNo) {
        log.debug("根据路线编号查询: routeNo={}", routeNo);
        return lambdaQuery()
                .eq(TmsRoute::getRouteNo, routeNo)
                .one();
    }
}
