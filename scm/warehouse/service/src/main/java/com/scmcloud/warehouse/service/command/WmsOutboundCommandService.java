package com.scmcloud.warehouse.service.command;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.OutboundStatus;
import com.scmcloud.warehouse.domain.entity.WmsOutbound;
import com.scmcloud.warehouse.domain.entity.WmsOutboundItem;
import com.scmcloud.warehouse.mapper.WmsOutboundItemMapper;
import com.scmcloud.warehouse.mapper.WmsOutboundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsOutboundCommandService {

    private final WmsOutboundMapper outboundMapper;
    private final WmsOutboundItemMapper outboundItemMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsOutbound create(WmsOutbound outbound) {
        outbound.setId(UUIDv7Util.generateString());
        outbound.setOutboundNo("OUT" + System.currentTimeMillis());
        outbound.setStatus(OutboundStatus.WAITING.getCode());
        outbound.setPickedQuantity(0);
        outbound.setDeleted(false);
        outbound.setCreateTime(LocalDateTime.now());
        outbound.setUpdateTime(LocalDateTime.now());
        outboundMapper.insert(outbound);
        log.info("出库单创建成功 id={}, outboundNo={}", (outbound.getId() + "").replace("\r", "_").replace("\n", "_"), (outbound.getOutboundNo() + "").replace("\r", "_").replace("\n", "_"));
        return outbound;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsOutbound outbound) {
        WmsOutbound existing = outboundMapper.selectById(outbound.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        if (existing.getStatus() != OutboundStatus.WAITING.getCode()) {
            throw new IllegalStateException("只有待拣货状态的出库单才能修改");
        }
        outbound.setUpdateTime(LocalDateTime.now());
        return outboundMapper.updateById(outbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsOutbound outbound = outboundMapper.selectById(id);
        if (outbound == null || Boolean.TRUE.equals(outbound.getDeleted())) {
            return false;
        }
        outbound.setDeleted(true);
        outbound.setUpdateTime(LocalDateTime.now());
        return outboundMapper.updateById(outbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean ship(String outboundId, String operatorId, String operatorName) {
        WmsOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            log.warn("[ship] 出库单不存在: id={}", (outboundId + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }

        List<WmsOutboundItem> items = outboundItemMapper.selectList(
                Wrappers.<WmsOutboundItem>lambdaQuery()
                        .eq(WmsOutboundItem::getOutboundId, outboundId)
                        .eq(WmsOutboundItem::getDeleted, false));

        int totalPicked = items.stream()
                .mapToInt(item -> item.getActualQuantity() != null ? item.getActualQuantity() : 0)
                .sum();

        if (totalPicked == 0) {
            throw new IllegalStateException("出库明细实际拣货数量不能全部为0");
        }

        OutboundStatus currentStatus = OutboundStatus.fromCode(outbound.getStatus());
        statusValidator.validateTransition("OUTBOUND", currentStatus.name(), "SHIPPED");

        outbound.setPickedQuantity(totalPicked);
        outbound.setStatus(OutboundStatus.SHIPPED.getCode());
        outbound.setPickerId(operatorId);
        outbound.setPickerName(operatorName);
        outbound.setUpdateTime(LocalDateTime.now());
        outbound.setUpdateBy(operatorId);
        outbound.setCompletedAt(LocalDateTime.now());

        boolean success = outboundMapper.updateById(outbound) > 0;
        if (success) {
            log.info("出库单已出库: id={}, outboundNo={}, pickedQty={}", (outboundId + "").replace("\r", "_").replace("\n", "_"), (outbound.getOutboundNo() + "").replace("\r", "_").replace("\n", "_"), (totalPicked + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String outboundId, String operatorId, String operatorName) {
        WmsOutbound outbound = outboundMapper.selectById(outboundId);
        if (outbound == null) {
            log.warn("[cancel] 出库单不存在: id={}", (outboundId + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }

        OutboundStatus currentStatus = OutboundStatus.fromCode(outbound.getStatus());
        statusValidator.validateTransition("OUTBOUND", currentStatus.name(), "CANCELLED");

        outbound.setStatus(OutboundStatus.CANCELLED.getCode());
        outbound.setPickerId(operatorId);
        outbound.setPickerName(operatorName);
        outbound.setUpdateTime(LocalDateTime.now());
        outbound.setUpdateBy(operatorId);

        boolean success = outboundMapper.updateById(outbound) > 0;
        if (success) {
            log.info("出库单已取消: id={}, outboundNo={}", (outboundId + "").replace("\r", "_").replace("\n", "_"), (outbound.getOutboundNo() + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }
}
