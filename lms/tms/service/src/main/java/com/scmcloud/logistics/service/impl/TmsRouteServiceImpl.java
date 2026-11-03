package com.scmcloud.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import com.scmcloud.logistics.domain.entity.TmsRoute;
import com.scmcloud.logistics.mapper.TmsRouteMapper;
import com.scmcloud.logistics.service.ITmsRouteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TmsRouteServiceImpl extends ServiceImpl<TmsRouteMapper, TmsRoute> implements ITmsRouteService {

    @Override
    public Page<TmsRoute> pageList(int page, int size, String courierId, Integer status, LocalDate deliveryDate) {
        log.debug("Query delivery route list: page={}, size={}, courierId={}, status={}, deliveryDate={}", (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (courierId + "").replace("\r", "_").replace("\n", "_"), (status + "").replace("\r", "_").replace("\n", "_"), (deliveryDate + "").replace("\r", "_").replace("\n", "_"));

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
        log.debug("Query route by courier: courierId={}", (courierId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(TmsRoute::getCourierId, courierId)
                .orderByDesc(TmsRoute::getDeliveryDate)
                .list();
    }

    @Override
    public TmsRoute getByRouteNo(String routeNo) {
        log.debug("Query route by number: routeNo={}", (routeNo + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(TmsRoute::getRouteNo, routeNo)
                .one();
    }

    @Override
    public TmsRoute createRoute(TmsRoute route) {
        log.info("Create route: routeNo={}, courierId={}", (route.getRouteNo() + "").replace("\r", "_").replace("\n", "_"), (route.getCourierId() + "").replace("\r", "_").replace("\n", "_"));
        route.setId(UUID.randomUUID().toString());
        route.setCreateTime(LocalDateTime.now());
        route.setUpdateTime(LocalDateTime.now());
        save(route);
        return route;
    }

    @Override
    public TmsRoute updateRoute(String id, TmsRoute route) {
        log.info("Update route: id={}", (id + "").replace("\r", "_").replace("\n", "_"));
        route.setId(id);
        route.setUpdateTime(LocalDateTime.now());
        updateById(route);
        return route;
    }
}
