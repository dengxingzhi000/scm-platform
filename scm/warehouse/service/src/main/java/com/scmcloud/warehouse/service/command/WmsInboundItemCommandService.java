package com.scmcloud.warehouse.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsInboundItemCommandService {

    private final WmsInboundItemMapper inboundItemMapper;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsInboundItem create(WmsInboundItem item) {
        item.setId(UUIDv7Util.generateString());
        item.setActualQuantity(0);
        item.setQualityStatus(1);
        item.setDeleted(false);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        inboundItemMapper.insert(item);
        log.info("入库明细创建成功: id={}", (item.getId() + "").replace("\r", "_").replace("\n", "_"));
        return item;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsInboundItem item) {
        WmsInboundItem existing = inboundItemMapper.selectById(item.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        item.setUpdateTime(LocalDateTime.now());
        return inboundItemMapper.updateById(item) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsInboundItem item = inboundItemMapper.selectById(id);
        if (item == null || Boolean.TRUE.equals(item.getDeleted())) {
            return false;
        }
        item.setDeleted(true);
        item.setUpdateTime(LocalDateTime.now());
        return inboundItemMapper.updateById(item) > 0;
    }
}
