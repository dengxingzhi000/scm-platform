package scm.notify.service.impl;

import scm.notify.domain.entity.SysNotificationAudit;
import scm.notify.mapper.SysNotificationAuditMapper;
import scm.notify.service.ISysNotificationAuditService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 通知发送审计表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class SysNotificationAuditServiceImpl extends ServiceImpl<SysNotificationAuditMapper, SysNotificationAudit> implements ISysNotificationAuditService {

}
