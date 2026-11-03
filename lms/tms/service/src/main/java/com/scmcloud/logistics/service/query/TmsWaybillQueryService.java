package com.scmcloud.logistics.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.logistics.domain.entity.TmsWaybill;
import com.scmcloud.logistics.mapper.TmsWaybillMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsWaybillQueryService {

    private final TmsWaybillMapper tmsWaybillMapper;

    @Slave
    public TmsWaybill getById(String id) {
        return tmsWaybillMapper.selectById(id);
    }

    @Slave
    public List<TmsWaybill> listAll() {
        return tmsWaybillMapper.selectList(null);
    }

    @Slave
    public Page<TmsWaybill> pageQuery(Page<TmsWaybill> page, Wrapper<TmsWaybill> wrapper) {
        return tmsWaybillMapper.selectPage(page, wrapper);
    }

    @Slave
    public Page<TmsWaybill> pageList(int page, int size, String waybillNo, Integer status, String carrierId) {
        log.debug("查询运单列表: page={}, size={}, waybillNo={}, status={}, carrierId={}", page, size, waybillNo, status, carrierId);
        LambdaQueryWrapper<TmsWaybill> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(waybillNo)) {
            wrapper.like(TmsWaybill::getWaybillNo, waybillNo);
        }
        if (status != null) {
            wrapper.eq(TmsWaybill::getStatus, status);
        }
        if (StringUtils.hasText(carrierId)) {
            wrapper.eq(TmsWaybill::getCarrierId, carrierId);
        }
        wrapper.eq(TmsWaybill::getDeleted, false);
        wrapper.orderByDesc(TmsWaybill::getCreateTime);
        return tmsWaybillMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public TmsWaybill getByWaybillNo(String waybillNo) {
        log.debug("根据运单号查询: waybillNo={}", waybillNo);
        LambdaQueryWrapper<TmsWaybill> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsWaybill::getWaybillNo, waybillNo);
        wrapper.eq(TmsWaybill::getDeleted, false);
        return tmsWaybillMapper.selectOne(wrapper);
    }

    @Slave
    public List<TmsWaybill> listByOrderId(String orderId) {
        log.debug("根据订单ID查询运单: orderId={}", orderId);
        LambdaQueryWrapper<TmsWaybill> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsWaybill::getOrderId, orderId);
        wrapper.eq(TmsWaybill::getDeleted, false);
        wrapper.orderByDesc(TmsWaybill::getCreateTime);
        return tmsWaybillMapper.selectList(wrapper);
    }

    @Slave
    public List<TmsWaybill> listByOrderNo(String orderNo) {
        log.debug("根据订单号查询运单: orderNo={}", orderNo);
        LambdaQueryWrapper<TmsWaybill> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsWaybill::getOrderNo, orderNo);
        wrapper.eq(TmsWaybill::getDeleted, false);
        wrapper.orderByDesc(TmsWaybill::getCreateTime);
        return tmsWaybillMapper.selectList(wrapper);
    }
}
