package scm.audit.mapper;

import scm.audit.domain.entity.SysAuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 操作审计日志表(按月分区) Mapper 接口
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

}
