package com.cognora.scm.finance.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.scm.finance.domain.entity.ReconciliationRecord;
import com.cognora.scm.finance.mapper.ReconciliationRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationRecordQueryService {
    private final ReconciliationRecordMapper reconciliationRecordMapper;

    @Slave
    public ReconciliationRecord getById(String id) {
        return reconciliationRecordMapper.selectById(id);
    }

    /**
     * 分页查询对账记录，支持按单号/往来单位关键字、账期、状态和差异标记过滤。
     */
    @Slave
    public Page<ReconciliationRecord> pageQuery(int page, int size, String keyword,
                                                String reconciliationPeriod, Integer status, Boolean hasDiff) {
        LambdaQueryWrapper<ReconciliationRecord> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(ReconciliationRecord::getReconciliationNo, keyword)
                    .or().like(ReconciliationRecord::getPartyName, keyword));
        }
        wrapper.eq(StringUtils.hasText(reconciliationPeriod),
                        ReconciliationRecord::getReconciliationPeriod, reconciliationPeriod)
                .eq(status != null, ReconciliationRecord::getStatus, status)
                .eq(hasDiff != null, ReconciliationRecord::getHasDiff, hasDiff)
                .orderByDesc(ReconciliationRecord::getCreateTime);
        return reconciliationRecordMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
