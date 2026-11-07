package com.cognora.scm.finance.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cognora.platform.common.data.rw.annotation.Slave;
import com.cognora.scm.finance.domain.entity.BadDebtWriteOff;
import com.cognora.scm.finance.mapper.BadDebtWriteOffMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadDebtWriteOffQueryService {

    private final BadDebtWriteOffMapper badDebtWriteOffMapper;

    /**
     * 分页查询核销单，支持按核销单号/往来单位关键字、账期和状态过滤。
     */
    @Slave
    public Page<BadDebtWriteOff> pageQuery(int page, int size, String keyword,
                                           String periodCode, Integer status) {
        LambdaQueryWrapper<BadDebtWriteOff> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(BadDebtWriteOff::getWriteOffNo, keyword)
                    .or().like(BadDebtWriteOff::getPartnerName, keyword));
        }
        wrapper.eq(StringUtils.hasText(periodCode), BadDebtWriteOff::getPeriodCode, periodCode)
                .eq(status != null, BadDebtWriteOff::getStatus, status)
                .orderByDesc(BadDebtWriteOff::getCreateTime);
        return badDebtWriteOffMapper.selectPage(new Page<>(page, size), wrapper);
    }
}
