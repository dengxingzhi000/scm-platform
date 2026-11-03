package com.scmcloud.finance.service.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.finance.domain.entity.Invoice;
import com.scmcloud.finance.mapper.InvoiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceQueryService {
    private final InvoiceMapper invoiceMapper;

    @Slave
    public List<Invoice> listByPartyId(String partyId) {
        log.debug("按往来方查询发票: partyId={}", partyId);
        LambdaQueryWrapper<Invoice> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.hasText(partyId), Invoice::getPartyId, partyId)
                .eq(Invoice::getDeleted, false)
                .orderByDesc(Invoice::getInvoiceDate);
        return invoiceMapper.selectList(wrapper);
    }
}
