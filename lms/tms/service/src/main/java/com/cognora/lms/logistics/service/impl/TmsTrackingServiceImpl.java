package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.lms.logistics.domain.WaybillStatus;
import com.cognora.lms.logistics.domain.entity.TmsTracking;
import com.cognora.lms.logistics.domain.entity.TmsWaybill;
import com.cognora.lms.logistics.mapper.TmsTrackingMapper;
import com.cognora.lms.logistics.mapper.TmsWaybillMapper;
import com.cognora.lms.logistics.service.ITmsTrackingService;
import com.cognora.lms.logistics.support.TenantGuard;
import com.cognora.platform.common.log.util.LogUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TmsTrackingServiceImpl extends ServiceImpl<TmsTrackingMapper, TmsTracking> implements ITmsTrackingService {

    private static final int EVENT_ID_MAX_LENGTH = 128;

    private final TmsWaybillMapper waybillMapper;

    @Override
    public List<TmsTracking> listByWaybillId(String waybillId) {
        log.debug("Query tracking by waybill ID: waybillId={}", (waybillId + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(TmsTracking::getWaybillId, waybillId)
                .orderByDesc(TmsTracking::getTrackTime)
                .list();
    }

    @Override
    public List<TmsTracking> listByWaybillNo(String waybillNo) {
        log.debug("Query tracking by waybill number: waybillNo={}", (waybillNo + "").replace("\r", "_").replace("\n", "_"));
        return lambdaQuery()
                .eq(TmsTracking::getWaybillNo, waybillNo)
                .orderByDesc(TmsTracking::getTrackTime)
                .list();
    }

    @Override
    public Page<TmsTracking> pageList(int page, int size, String waybillNo, String trackStatus) {
        log.debug("Page query tracking: page={}, size={}, waybillNo={}, trackStatus={}", (page + "").replace("\r", "_").replace("\n", "_"), (size + "").replace("\r", "_").replace("\n", "_"), (waybillNo + "").replace("\r", "_").replace("\n", "_"), (trackStatus + "").replace("\r", "_").replace("\n", "_"));

        LambdaQueryWrapper<TmsTracking> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(waybillNo)) {
            wrapper.eq(TmsTracking::getWaybillNo, waybillNo);
        }
        if (StringUtils.hasText(trackStatus)) {
            wrapper.eq(TmsTracking::getTrackStatus, trackStatus);
        }
        wrapper.orderByDesc(TmsTracking::getTrackTime);

        return page(new Page<>(page, size), wrapper);
    }

    /**
     * 写入一条轨迹，重复推送（同一租户、同一 eventId / 同一内容）只落库一次。
     *
     * <p>幂等键优先取调用方传入的 eventId；未传时按运单号 + 轨迹状态 + 轨迹时间 +
     * 位置 + 描述生成 SHA-256 指纹。唯一约束由
     * {@code uk_tms_tracking_event(tenant_id, event_id)} 兜底，覆盖并发重复回调。</p>
     */
    @Override
    public TmsTracking addTracking(TmsTracking tracking) {
        TenantGuard.requireTenant();

        String payloadHash = fingerprint(tracking);
        String eventId = resolveEventId(tracking, payloadHash);
        tracking.setEventId(eventId);
        tracking.setPayloadHash(payloadHash);

        TmsTracking existing = findByEventId(eventId);
        if (existing != null) {
            log.info("Duplicate tracking callback ignored: waybillNo={}, eventId={}, trackingId={}",
                    LogUtils.sanitizeForLog(tracking.getWaybillNo()),
                    LogUtils.sanitizeForLog(eventId), existing.getId());
            return existing;
        }

        TmsWaybill waybill = requireAccessibleWaybill(tracking);
        tracking.setWaybillId(waybill.getId());
        tracking.setWaybillNo(waybill.getWaybillNo());

        if (tracking.getId() == null) {
            tracking.setId(UUID.randomUUID().toString());
        }
        if (tracking.getTrackTime() == null) {
            tracking.setTrackTime(LocalDateTime.now());
        }
        if (tracking.getCreateTime() == null) {
            tracking.setCreateTime(LocalDateTime.now());
        }

        try {
            if (!save(tracking)) {
                throw new IllegalStateException("添加物流轨迹失败");
            }
        } catch (DuplicateKeyException duplicate) {
            // 并发下两个回调同时通过前置查询，由唯一索引拦下后回读已落库记录。
            existing = findByEventId(eventId);
            if (existing != null) {
                log.info("Concurrent duplicate tracking callback merged: waybillNo={}, eventId={}, trackingId={}",
                        LogUtils.sanitizeForLog(tracking.getWaybillNo()),
                        LogUtils.sanitizeForLog(eventId), existing.getId());
                return existing;
            }
            throw duplicate;
        }

        applyWaybillStatus(waybill, tracking);
        applyWaybillTrackingNo(waybill, tracking);

        log.info("物流轨迹添加入库: id={}, waybillNo={}, eventId={}", tracking.getId(),
                LogUtils.sanitizeForLog(tracking.getWaybillNo()), LogUtils.sanitizeForLog(eventId));
        return tracking;
    }

    /**
     * 轨迹驱动运单状态（批次 2）：只向前推进，允许承运商合并推送跳过中间态。
     * 已签收/已退回为终态，不因后续重复或乱序事件回退；签收时间只在首次签收时写入。
     */
    private void applyWaybillStatus(TmsWaybill waybill, TmsTracking tracking) {
        int target = WaybillStatus.fromTrackingEvent(tracking.getTrackStatus());
        if (target < 0) {
            return;
        }
        int current = waybill.getStatus() == null ? WaybillStatus.CREATED : waybill.getStatus();
        if (current == target) {
            return;
        }
        if (!WaybillStatus.isTrackingAdvanceAllowed(current, target)) {
            // 乱序/回退事件不改变运单状态，保留轨迹原文供人工核查。
            log.warn("Tracking event ignored due to inconsistent waybill state: waybillNo={}, current={}, target={}",
                    waybill.getWaybillNo(), current, target);
            return;
        }

        LocalDateTime signedAt = tracking.getTrackTime() != null ? tracking.getTrackTime() : LocalDateTime.now();
        TmsWaybill patch = new TmsWaybill();
        patch.setId(waybill.getId());
        patch.setStatus(target);
        patch.setUpdateTime(LocalDateTime.now());
        if (target == WaybillStatus.DELIVERED) {
            if (waybill.getSignTime() == null) {
                patch.setSignTime(signedAt);
            }
            if (waybill.getActualDelivery() == null) {
                patch.setActualDelivery(signedAt);
            }
        }

        if (waybillMapper.updateById(patch) > 0) {
            waybill.setStatus(target);
            if (target == WaybillStatus.DELIVERED) {
                if (waybill.getSignTime() == null) {
                    waybill.setSignTime(patch.getSignTime());
                }
                if (waybill.getActualDelivery() == null) {
                    waybill.setActualDelivery(patch.getActualDelivery());
                }
            }
            log.info("Waybill status advanced by tracking: waybillNo={}, {} -> {}",
                    waybill.getWaybillNo(), current, target);
        }
    }

    /**
     * 承运商回传真实跟踪号时写入运单；首次回传后不再覆盖，避免后续不同格式的回传抖动。
     */
    private void applyWaybillTrackingNo(TmsWaybill waybill, TmsTracking tracking) {
        if (!StringUtils.hasText(tracking.getTrackingNo()) || StringUtils.hasText(waybill.getTrackingNo())) {
            return;
        }
        TmsWaybill patch = new TmsWaybill();
        patch.setId(waybill.getId());
        patch.setTrackingNo(tracking.getTrackingNo().trim());
        patch.setUpdateTime(LocalDateTime.now());
        if (waybillMapper.updateById(patch) > 0) {
            waybill.setTrackingNo(patch.getTrackingNo());
            log.info("Waybill tracking number recorded: waybillNo={}, trackingNo={}",
                    waybill.getWaybillNo(), patch.getTrackingNo());
        }
    }

    private TmsTracking findByEventId(String eventId) {
        return baseMapper.selectOne(new QueryWrapper<TmsTracking>().eq("event_id", eventId));
    }

    private TmsWaybill requireAccessibleWaybill(TmsTracking tracking) {
        TmsWaybill waybill = null;
        if (StringUtils.hasText(tracking.getWaybillId())) {
            waybill = waybillMapper.selectById(tracking.getWaybillId());
        }
        if (waybill == null && StringUtils.hasText(tracking.getWaybillNo())) {
            waybill = waybillMapper.selectOne(new QueryWrapper<TmsWaybill>()
                    .eq("waybill_no", tracking.getWaybillNo())
                    .eq("deleted", false));
        }
        if (waybill == null) {
            // 不存在与跨租户不可见都按 403 处理，避免向调用方泄露其他租户的运单是否存在。
            throw new AccessDeniedException("Waybill is not accessible in the current tenant");
        }
        return waybill;
    }

    private String resolveEventId(TmsTracking tracking, String payloadHash) {
        if (StringUtils.hasText(tracking.getEventId())) {
            String eventId = tracking.getEventId().trim();
            if (eventId.length() > EVENT_ID_MAX_LENGTH) {
                throw new IllegalArgumentException("eventId exceeds " + EVENT_ID_MAX_LENGTH + " characters");
            }
            return eventId;
        }
        return payloadHash;
    }

    private String fingerprint(TmsTracking tracking) {
        String canonical = String.join("|",
                nullSafe(tracking.getWaybillNo()),
                nullSafe(tracking.getTrackStatus()),
                nullSafe(tracking.getTrackTime()),
                nullSafe(tracking.getLocation()),
                nullSafe(tracking.getDescription()));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }
}
