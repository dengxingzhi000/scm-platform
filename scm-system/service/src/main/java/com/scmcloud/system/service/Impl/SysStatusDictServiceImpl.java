package com.scmcloud.system.service.Impl;

import com.scmcloud.system.cache.StatusDictCacheManager;
import com.scmcloud.system.cache.StatusDictEventPublisher;
import com.scmcloud.system.domain.entity.SysStatusDict;
import com.scmcloud.system.domain.entity.SysStatusTransition;
import com.scmcloud.system.mapper.SysStatusDictMapper;
import com.scmcloud.system.mapper.SysStatusTransitionMapper;
import com.scmcloud.system.service.ISysStatusDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SysStatusDictServiceImpl implements ISysStatusDictService {
    private final SysStatusDictMapper statusDictMapper;
    private final SysStatusTransitionMapper transitionMapper;
    private final StatusDictCacheManager cacheManager;
    private final StatusDictEventPublisher eventPublisher;

    // ─── 状态字典查询 (走缓存) ─────────────────────────────────

    @Override
    public List<SysStatusDict> listStatusDict(String bizType) {
        return cacheManager.getStatusDict(bizType);
    }

    @Override
    public SysStatusDict getStatusByCode(String bizType, String statusCode) {
        return cacheManager.getStatusByCode(bizType, statusCode);
    }

    // ─── 状态字典变更 (写 DB + 发事件刷新缓存) ─────────────────

    @Override
    public void createStatusDict(SysStatusDict entity) {
        statusDictMapper.insert(entity);
        eventPublisher.publishChange(resolveTenantId(entity.getTenantId()), entity.getBizType());
    }

    @Override
    public void updateStatusDict(SysStatusDict entity) {
        statusDictMapper.updateById(entity);
        eventPublisher.publishChange(resolveTenantId(entity.getTenantId()), entity.getBizType());
    }

    @Override
    public void deleteStatusDict(String id) {
        SysStatusDict existing = statusDictMapper.selectById(id);
        statusDictMapper.deleteById(id);
        if (existing != null) {
            eventPublisher.publishChange(resolveTenantId(existing.getTenantId()), existing.getBizType());
        }
    }

    // ─── 流转规则查询 (走缓存) ─────────────────────────────────

    @Override
    public List<SysStatusTransition> listTransitions(String bizType) {
        return cacheManager.getTransitions(bizType);
    }

    @Override
    public List<SysStatusTransition> listTransitionsFrom(String bizType, String fromStatus) {
        return cacheManager.getTransitionsFrom(bizType, fromStatus);
    }

    @Override
    public boolean canTransition(String bizType, String fromStatus, String toStatus) {
        return cacheManager.getTransitionsFrom(bizType, fromStatus).stream()
                .anyMatch(t -> t.getToStatus().equals(toStatus) && t.getEnabled());
    }

    @Override
    public SysStatusTransition findTransition(String bizType, String fromStatus, String toStatus, String actionCode) {
        return cacheManager.getTransitionsFrom(bizType, fromStatus).stream()
                .filter(t -> t.getToStatus().equals(toStatus)
                        && t.getActionCode().equals(actionCode)
                        && t.getEnabled())
                .findFirst()
                .orElse(null);
    }

    // ─── 流转规则变更 (写 DB + 发事件刷新缓存) ─────────────────

    @Override
    public void createTransition(SysStatusTransition entity) {
        transitionMapper.insert(entity);
        eventPublisher.publishChange(resolveTenantId(entity.getTenantId()), entity.getBizType());
    }

    @Override
    public void updateTransition(SysStatusTransition entity) {
        transitionMapper.updateById(entity);
        eventPublisher.publishChange(resolveTenantId(entity.getTenantId()), entity.getBizType());
    }

    @Override
    public void deleteTransition(String id) {
        SysStatusTransition existing = transitionMapper.selectById(id);
        transitionMapper.deleteById(id);
        if (existing != null) {
            eventPublisher.publishChange(resolveTenantId(existing.getTenantId()), existing.getBizType());
        }
    }

    private String resolveTenantId(UUID tenantId) {
        return tenantId != null ? tenantId.toString() : "global";
    }
}
