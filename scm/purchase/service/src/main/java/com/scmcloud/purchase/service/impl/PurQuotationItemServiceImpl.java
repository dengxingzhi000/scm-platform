package com.scmcloud.purchase.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scmcloud.purchase.domain.entity.PurQuotationItem;
import com.scmcloud.purchase.mapper.PurQuotationItemMapper;
import com.scmcloud.purchase.service.IPurQuotationItemService;

import java.util.List;

@Slf4j
@Service
public class PurQuotationItemServiceImpl extends ServiceImpl<PurQuotationItemMapper, PurQuotationItem> implements IPurQuotationItemService {

    @Override
    public List<PurQuotationItem> listByQuotationId(String quotationId) {
        return lambdaQuery()
                .eq(PurQuotationItem::getQuotationId, quotationId)
                .orderByAsc(PurQuotationItem::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByQuotationId(String quotationId) {
        return lambdaUpdate()
                .eq(PurQuotationItem::getQuotationId, quotationId)
                .remove();
    }
}
