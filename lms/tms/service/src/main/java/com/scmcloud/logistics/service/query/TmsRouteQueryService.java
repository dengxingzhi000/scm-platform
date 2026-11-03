package com.scmcloud.logistics.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.logistics.domain.entity.TmsRoute;
import com.scmcloud.logistics.mapper.TmsRouteMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsRouteQueryService {

    private final TmsRouteMapper tmsRouteMapper;

    @Slave
    public TmsRoute getById(String id) {
        return tmsRouteMapper.selectById(id);
    }

    @Slave
    public List<TmsRoute> listAll() {
        return tmsRouteMapper.selectList(null);
    }

    @Slave
    public Page<TmsRoute> pageQuery(Page<TmsRoute> page, Wrapper<TmsRoute> wrapper) {
        return tmsRouteMapper.selectPage(page, wrapper);
    }

    @Slave
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
        return tmsRouteMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<TmsRoute> listByCourierId(String courierId) {
        log.debug("根据配送员查询路线: courierId={}", courierId);
        LambdaQueryWrapper<TmsRoute> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsRoute::getCourierId, courierId);
        wrapper.orderByDesc(TmsRoute::getDeliveryDate);
        return tmsRouteMapper.selectList(wrapper);
    }

    @Slave
    public TmsRoute getByRouteNo(String routeNo) {
        log.debug("根据路线编号查询: routeNo={}", routeNo);
        LambdaQueryWrapper<TmsRoute> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsRoute::getRouteNo, routeNo);
        return tmsRouteMapper.selectOne(wrapper);
    }
}
