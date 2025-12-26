package scm.audit.service.impl;

import scm.audit.domain.entity.SysSensitiveOperationLog;
import scm.audit.mapper.SysSensitiveOperationLogMapper;
import scm.audit.service.ISysSensitiveOperationLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 敏感操作日志表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class SysSensitiveOperationLogServiceImpl extends ServiceImpl<SysSensitiveOperationLogMapper, SysSensitiveOperationLog> implements ISysSensitiveOperationLogService {

}
