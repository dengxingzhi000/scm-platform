package com.scmcloud.purchase.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scmcloud.purchase.domain.entity.PurReceiptItem;
import com.scmcloud.purchase.mapper.PurReceiptItemMapper;
import com.scmcloud.purchase.service.IPurReceiptItemService;

import java.util.List;

@Slf4j
@Service
public class PurReceiptItemServiceImpl extends ServiceImpl<PurReceiptItemMapper, PurReceiptItem> implements IPurReceiptItemService {

    @Override
    public List<PurReceiptItem> listByReceiptId(String receiptId) {
        return lambdaQuery()
                .eq(PurReceiptItem::getReceiptId, receiptId)
                .orderByAsc(PurReceiptItem::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByReceiptId(String receiptId) {
        return lambdaUpdate()
                .eq(PurReceiptItem::getReceiptId, receiptId)
                .remove();
    }
}
