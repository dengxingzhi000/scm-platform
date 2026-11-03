package com.scmcloud.logistics.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.logistics.domain.entity.TmsTracking;
import com.scmcloud.logistics.mapper.TmsTrackingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsTrackingQueryService {

    private final TmsTrackingMapper tmsTrackingMapper;

    @Slave
    public TmsTracking getById(String id) {
        return tmsTrackingMapper.selectById(id);
    }

    @Slave
    public List<TmsTracking> listAll() {
        return tmsTrackingMapper.selectList(null);
    }

    @Slave
    public Page<TmsTracking> pageQuery(Page<TmsTracking> page, Wrapper<TmsTracking> wrapper) {
        return tmsTrackingMapper.selectPage(page, wrapper);
    }

    @Slave
    public List<TmsTracking> listByWaybillId(String waybillId) {
        log.debug("根据运单ID查询物流轨迹: waybillId={}", waybillId);
        LambdaQueryWrapper<TmsTracking> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsTracking::getWaybillId, waybillId);
        wrapper.orderByDesc(TmsTracking::getTrackTime);
        return tmsTrackingMapper.selectList(wrapper);
    }

    @Slave
    public List<TmsTracking> listByWaybillNo(String waybillNo) {
        log.debug("根据运单号查询物流轨迹: waybillNo={}", waybillNo);
        LambdaQueryWrapper<TmsTracking> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsTracking::getWaybillNo, waybillNo);
        wrapper.orderByDesc(TmsTracking::getTrackTime);
        return tmsTrackingMapper.selectList(wrapper);
    }

    @Slave
    public Page<TmsTracking> pageList(int page, int size, String waybillNo, String trackStatus) {
        log.debug("分页查询物流轨迹: page={}, size={}, waybillNo={}, trackStatus={}", page, size, waybillNo, trackStatus);
        LambdaQueryWrapper<TmsTracking> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(waybillNo)) {
            wrapper.eq(TmsTracking::getWaybillNo, waybillNo);
        }
        if (StringUtils.hasText(trackStatus)) {
            wrapper.eq(TmsTracking::getTrackStatus, trackStatus);
        }
        wrapper.orderByDesc(TmsTracking::getTrackTime);
        return tmsTrackingMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
