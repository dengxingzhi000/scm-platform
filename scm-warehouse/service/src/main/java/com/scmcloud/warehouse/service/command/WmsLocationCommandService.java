package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.WmsLocation;
import com.scmcloud.warehouse.mapper.WmsLocationMapper;
import com.scmcloud.warehouse.service.query.WmsLocationQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsLocationCommandService {

    private final WmsLocationMapper locationMapper;
    private final WmsLocationQueryService locationQueryService;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsLocation create(WmsLocation location) {
        if (locationQueryService.existsByWarehouseIdAndCode(location.getWarehouseId(), location.getLocationCode())) {
            throw new IllegalStateException("同一仓库下库位编码已存在: " + location.getLocationCode());
        }
        location.setId(UUIDv7Util.generateString());
        location.setCurrentCapacity(0);
        location.setStatus(1);
        location.setEnabled(true);
        location.setDeleted(false);
        location.setCreateTime(LocalDateTime.now());
        location.setUpdateTime(LocalDateTime.now());
        locationMapper.insert(location);
        log.info("库位创建成功: id={}, code={}", (location.getId() + "").replace("\r", "_").replace("\n", "_"), (location.getLocationCode() + "").replace("\r", "_").replace("\n", "_"));
        return location;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsLocation location) {
        WmsLocation existing = locationMapper.selectById(location.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        location.setUpdateTime(LocalDateTime.now());
        return locationMapper.updateById(location) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsLocation location = locationMapper.selectById(id);
        if (location == null || Boolean.TRUE.equals(location.getDeleted())) {
            return false;
        }
        location.setDeleted(true);
        location.setUpdateTime(LocalDateTime.now());
        return locationMapper.updateById(location) > 0;
    }
}
