package com.scmcloud.logistics.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.logistics.domain.entity.TmsTracking;
import com.scmcloud.logistics.mapper.TmsTrackingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsTrackingCommandService {

    private final TmsTrackingMapper tmsTrackingMapper;

    @Master(reason = "Save tracking record")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TmsTracking entity) {
        return tmsTrackingMapper.insert(entity) > 0;
    }

    @Master(reason = "Update tracking record")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(TmsTracking entity) {
        return tmsTrackingMapper.updateById(entity) > 0;
    }

    @Master(reason = "Delete tracking record")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return tmsTrackingMapper.deleteById(id) > 0;
    }

    @Master(reason = "Add tracking record")
    @Transactional(rollbackFor = Exception.class)
    public TmsTracking addTracking(TmsTracking tracking) {
        log.info("Add tracking: waybillNo={}, location={}, status={}", tracking.getWaybillNo(), tracking.getLocation(), tracking.getTrackStatus());
        if (tracking.getId() == null) {
            tracking.setId(java.util.UUID.randomUUID().toString());
        }
        if (tracking.getTrackTime() == null) {
            tracking.setTrackTime(LocalDateTime.now());
        }
        if (tracking.getCreateTime() == null) {
            tracking.setCreateTime(LocalDateTime.now());
        }
        boolean success = tmsTrackingMapper.insert(tracking) > 0;
        if (!success) {
            throw new RuntimeException("Failed to add tracking record");
        }
        log.info("Tracking record added: id={}, waybillNo={}", tracking.getId(), tracking.getWaybillNo());
        return tracking;
    }
}
