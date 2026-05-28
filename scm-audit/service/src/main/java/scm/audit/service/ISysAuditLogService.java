package scm.audit.service;

import scm.audit.domain.entity.SysAuditLog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 操作审计日志表(按月分区) 服务类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
public interface ISysAuditLogService extends IService<SysAuditLog> {

}
