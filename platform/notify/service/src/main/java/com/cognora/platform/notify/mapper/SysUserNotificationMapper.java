package com.cognora.platform.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cognora.platform.notify.domain.entity.SysUserNotification;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户站内通知 Mapper。
 */
@Mapper
public interface SysUserNotificationMapper extends BaseMapper<SysUserNotification> {
}
