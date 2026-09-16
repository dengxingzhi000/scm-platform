package com.scmcloud.notify.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.notify.domain.entity.SysNotificationAudit;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

/**
 * <p>
 * 閫氱煡鍙戦€佸璁¤〃 鏈嶅姟锟?
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
public interface ISysNotificationAuditService extends IService<SysNotificationAudit> {

    SysNotificationAudit createAudit(SysNotificationAudit entity);

    SysNotificationAudit getById(String id);

    boolean deleteById(String id);

    List<SysNotificationAudit> listByUserId(String userId);

    List<SysNotificationAudit> listByStatus(String status);

    boolean sendNotification(String id);

    Page<SysNotificationAudit> pageQuery(int page, int size, String userId,
                                          String channel, String status);
}
