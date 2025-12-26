package scm.finance.service.impl;

import scm.finance.domain.entity.ReconciliationRecord;
import scm.finance.mapper.ReconciliationRecordMapper;
import scm.finance.service.IReconciliationRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 对账记录表 服务实现类
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
@Service
public class ReconciliationRecordServiceImpl extends ServiceImpl<ReconciliationRecordMapper, ReconciliationRecord>
        implements IReconciliationRecordService {

}
