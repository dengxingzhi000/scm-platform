package com.scmcloud.warehouse.service.command;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.status.StatusValidator;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.warehouse.domain.entity.InboundStatus;
import com.scmcloud.warehouse.domain.entity.WmsInbound;
import com.scmcloud.warehouse.domain.entity.WmsInboundItem;
import com.scmcloud.warehouse.mapper.WmsInboundItemMapper;
import com.scmcloud.warehouse.mapper.WmsInboundMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WmsInboundCommandService {

    private final WmsInboundMapper inboundMapper;
    private final WmsInboundItemMapper inboundItemMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public WmsInbound create(WmsInbound inbound) {
        inbound.setId(UUIDv7Util.generateString());
        inbound.setInboundNo("IN" + System.currentTimeMillis());
        inbound.setStatus(InboundStatus.WAITING.getCode());
        inbound.setReceivedQuantity(0);
        inbound.setDeleted(false);
        inbound.setCreateTime(LocalDateTime.now());
        inbound.setUpdateTime(LocalDateTime.now());
        inboundMapper.insert(inbound);
        log.info("入库单创建成功 id={}, inboundNo={}", (inbound.getId() + "").replace("\r", "_").replace("\n", "_"), (inbound.getInboundNo() + "").replace("\r", "_").replace("\n", "_"));
        return inbound;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean update(WmsInbound inbound) {
        WmsInbound existing = inboundMapper.selectById(inbound.getId());
        if (existing == null || Boolean.TRUE.equals(existing.getDeleted())) {
            return false;
        }
        if (existing.getStatus() != InboundStatus.WAITING.getCode()) {
            throw new IllegalStateException("只有待入库状态的入库单才能修改");
        }
        inbound.setUpdateTime(LocalDateTime.now());
        return inboundMapper.updateById(inbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean softDeleteById(String id) {
        WmsInbound inbound = inboundMapper.selectById(id);
        if (inbound == null || Boolean.TRUE.equals(inbound.getDeleted())) {
            return false;
        }
        inbound.setDeleted(true);
        inbound.setUpdateTime(LocalDateTime.now());
        return inboundMapper.updateById(inbound) > 0;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean receive(String inboundId, String operatorId, String operatorName) {
        WmsInbound inbound = inboundMapper.selectById(inboundId);
        if (inbound == null) {
            log.warn("[receive] 入库单不存在: id={}", (inboundId + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }

        List<WmsInboundItem> items = inboundItemMapper.selectList(
                Wrappers.<WmsInboundItem>lambdaQuery()
                        .eq(WmsInboundItem::getInboundId, inboundId)
                        .eq(WmsInboundItem::getDeleted, false));

        int totalReceived = items.stream()
                .mapToInt(item -> item.getActualQuantity() != null ? item.getActualQuantity() : 0)
                .sum();

        if (totalReceived == 0) {
            throw new IllegalStateException("入库明细实际收货数量不能全部为0");
        }

        boolean allReceived = items.stream()
                .allMatch(item -> item.getActualQuantity() != null
                        && item.getActualQuantity().equals(item.getPlanQuantity()));

        InboundStatus currentStatus = InboundStatus.fromCode(inbound.getStatus());
        statusValidator.validateTransition("INBOUND",
                currentStatus.name(), allReceived ? "COMPLETED" : "PARTIAL");

        inbound.setReceivedQuantity(totalReceived);
        inbound.setStatus(allReceived ? InboundStatus.COMPLETED.getCode() : InboundStatus.PARTIAL.getCode());
        inbound.setOperatorId(operatorId);
        inbound.setOperatorName(operatorName);
        inbound.setUpdateTime(LocalDateTime.now());
        inbound.setUpdateBy(operatorId);

        if (allReceived) {
            inbound.setCompletedAt(LocalDateTime.now());
        }

        boolean success = inboundMapper.updateById(inbound) > 0;
        if (success) {
            log.info("入库单收货完成: id={}, inboundNo={}, status={}, receivedQty={}",
                    (inboundId + "").replace("\r", "_").replace("\n", "_"), (inbound.getInboundNo() + "").replace("\r", "_").replace("\n", "_"), (inbound.getStatus() + "").replace("\r", "_").replace("\n", "_"), (totalReceived + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(String inboundId, String operatorId, String operatorName) {
        WmsInbound inbound = inboundMapper.selectById(inboundId);
        if (inbound == null) {
            log.warn("[cancel] 入库单不存在: id={}", (inboundId + "").replace("\r", "_").replace("\n", "_"));
            return false;
        }

        InboundStatus currentStatus = InboundStatus.fromCode(inbound.getStatus());
        statusValidator.validateTransition("INBOUND", currentStatus.name(), "CANCELLED");

        inbound.setStatus(InboundStatus.CANCELLED.getCode());
        inbound.setOperatorId(operatorId);
        inbound.setOperatorName(operatorName);
        inbound.setUpdateTime(LocalDateTime.now());
        inbound.setUpdateBy(operatorId);

        boolean success = inboundMapper.updateById(inbound) > 0;
        if (success) {
            log.info("入库单已取消: id={}, inboundNo={}", (inboundId + "").replace("\r", "_").replace("\n", "_"), (inbound.getInboundNo() + "").replace("\r", "_").replace("\n", "_"));
        }
        return success;
    }
}
