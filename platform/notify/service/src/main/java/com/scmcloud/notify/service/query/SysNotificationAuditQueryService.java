package com.scmcloud.notify.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.notify.domain.entity.SysNotificationAudit;
import com.scmcloud.notify.mapper.SysNotificationAuditMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysNotificationAuditQueryService {

    private final SysNotificationAuditMapper sysNotificationAuditMapper;

    @Slave
    public SysNotificationAudit getById(String id) {
        LambdaQueryWrapper<SysNotificationAudit> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysNotificationAudit::getId, id);
        return sysNotificationAuditMapper.selectOne(wrapper);
    }

    @Slave
    public List<SysNotificationAudit> listByUserId(String userId) {
        LambdaQueryWrapper<SysNotificationAudit> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysNotificationAudit::getUserId, userId)
                .orderByDesc(SysNotificationAudit::getCreatedAt);
        return sysNotificationAuditMapper.selectList(wrapper);
    }

    @Slave
    public List<SysNotificationAudit> listByStatus(String status) {
        LambdaQueryWrapper<SysNotificationAudit> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysNotificationAudit::getStatus, status)
                .orderByDesc(SysNotificationAudit::getCreatedAt);
        return sysNotificationAuditMapper.selectList(wrapper);
    }

    @Slave
    public Page<SysNotificationAudit> pageQuery(int page, int size, String userId,
                                                  String channel, String status) {
        LambdaQueryWrapper<SysNotificationAudit> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(userId)) {
            wrapper.eq(SysNotificationAudit::getUserId, userId);
        }
        if (StringUtils.hasText(channel)) {
            wrapper.eq(SysNotificationAudit::getChannel, channel);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(SysNotificationAudit::getStatus, status);
        }
        wrapper.orderByDesc(SysNotificationAudit::getCreatedAt);

        return sysNotificationAuditMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
