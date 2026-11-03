package com.scmcloud.system.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scmcloud.system.domain.entity.NotificationAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知审计日志 Mapper 接口
 */
@Mapper
@DS("notify")
public interface NotificationAuditLogMapper extends BaseMapper<NotificationAuditLog> {
}

