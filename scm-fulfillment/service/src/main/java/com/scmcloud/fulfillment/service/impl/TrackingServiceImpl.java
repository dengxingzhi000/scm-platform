package com.scmcloud.fulfillment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.scmcloud.fulfillment.domain.entity.Tracking;
import com.scmcloud.fulfillment.mapper.TrackingMapper;
import com.scmcloud.fulfillment.service.ITrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class TrackingServiceImpl extends ServiceImpl<TrackingMapper, Tracking> implements ITrackingService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Tracking recordEvent(Tracking tracking) {
        if (tracking == null || tracking.getFulfillmentNo() == null || tracking.getFulfillmentNo().isBlank()) {
            throw new IllegalArgumentException("fulfillmentNo is required");
        }
        LocalDateTime now = LocalDateTime.now();
        if (tracking.getEventTime() == null) {
            tracking.setEventTime(now);
        }
        tracking.setCreatedAt(now);
        save(tracking);
        log.info("Tracking event recorded: fulfillmentNo={}, status={}, location={}",
                tracking.getFulfillmentNo(), tracking.getStatus(), tracking.getLocation());
        return tracking;
    }

    @Override
    public List<Tracking> getByTrackingNo(String trackingNo) {
        return list(new LambdaQueryWrapper<Tracking>()
                .eq(Tracking::getTrackingNo, trackingNo)
                .orderByDesc(Tracking::getEventTime));
    }

    @Override
    public List<Tracking> getByFulfillmentNo(String fulfillmentNo) {
        return list(new LambdaQueryWrapper<Tracking>()
                .eq(Tracking::getFulfillmentNo, fulfillmentNo)
                .orderByDesc(Tracking::getEventTime));
    }

    @Override
    public Tracking getLatest(String fulfillmentNo) {
        return getOne(new LambdaQueryWrapper<Tracking>()
                .eq(Tracking::getFulfillmentNo, fulfillmentNo)
                .orderByDesc(Tracking::getEventTime)
                .last("LIMIT 1"));
    }
}
