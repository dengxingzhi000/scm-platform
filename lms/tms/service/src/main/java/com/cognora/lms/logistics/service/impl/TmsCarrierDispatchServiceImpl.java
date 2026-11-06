package com.cognora.lms.logistics.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.cognora.lms.logistics.domain.entity.TmsCarrierDispatch;
import com.cognora.lms.logistics.mapper.TmsCarrierDispatchMapper;
import com.cognora.lms.logistics.service.ITmsCarrierDispatchService;
import com.cognora.lms.logistics.support.TenantGuard;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class TmsCarrierDispatchServiceImpl extends ServiceImpl<TmsCarrierDispatchMapper, TmsCarrierDispatch>
        implements ITmsCarrierDispatchService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TmsCarrierDispatch beginDispatch(String idempotencyKey, String waybillNo, String primaryCarrierId) {
        TenantGuard.requireTenant();
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new IllegalArgumentException("幂等键不能为空");
        }
        TmsCarrierDispatch existing = findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            log.info("承运商派发幂等命中: key={}, result={}", idempotencyKey, existing.getResult());
            return existing;
        }

        TmsCarrierDispatch dispatch = new TmsCarrierDispatch();
        dispatch.setId(UUID.randomUUID().toString());
        dispatch.setIdempotencyKey(idempotencyKey.trim());
        dispatch.setWaybillNo(waybillNo);
        dispatch.setPrimaryCarrierId(primaryCarrierId);
        dispatch.setResult(TmsCarrierDispatch.RESULT_RUNNING);
        dispatch.setAttemptCount(0);
        dispatch.setCreateTime(LocalDateTime.now());
        dispatch.setUpdateTime(LocalDateTime.now());
        try {
            save(dispatch);
        } catch (DuplicateKeyException duplicate) {
            // 并发重复下单由唯一索引兜底，回读既有记录保证幂等。
            TmsCarrierDispatch concurrent = findByIdempotencyKey(idempotencyKey);
            if (concurrent != null) {
                return concurrent;
            }
            throw duplicate;
        }
        return dispatch;
    }

    @Override
    public TmsCarrierDispatch completeDispatch(String id, String actualCarrierId, int attemptCount,
                                               String result, String errorMessage) {
        TenantGuard.requireTenant();
        TmsCarrierDispatch existing = getById(id);
        if (existing == null) {
            throw new AccessDeniedException("Carrier dispatch is not accessible in the current tenant");
        }
        TmsCarrierDispatch patch = new TmsCarrierDispatch();
        patch.setId(id);
        patch.setActualCarrierId(actualCarrierId);
        patch.setAttemptCount(attemptCount);
        patch.setResult(result);
        patch.setErrorMessage(errorMessage);
        patch.setUpdateTime(LocalDateTime.now());
        updateById(patch);
        existing.setActualCarrierId(actualCarrierId);
        existing.setAttemptCount(attemptCount);
        existing.setResult(result);
        return existing;
    }

    @Override
    public TmsCarrierDispatch findByIdempotencyKey(String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return null;
        }
        LambdaQueryWrapper<TmsCarrierDispatch> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TmsCarrierDispatch::getIdempotencyKey, idempotencyKey.trim());
        List<TmsCarrierDispatch> list = list(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }
}
