package com.scmcloud.purchase.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scmcloud.purchase.domain.entity.PurRfqItem;
import com.scmcloud.purchase.mapper.PurRfqItemMapper;
import com.scmcloud.purchase.service.IPurRfqItemService;

import java.util.List;

@Slf4j
@Service
public class PurRfqItemServiceImpl extends ServiceImpl<PurRfqItemMapper, PurRfqItem> implements IPurRfqItemService {

    @Override
    public List<PurRfqItem> listByRfqId(String rfqId) {
        return lambdaQuery()
                .eq(PurRfqItem::getRfqId, rfqId)
                .orderByAsc(PurRfqItem::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByRfqId(String rfqId) {
        return lambdaUpdate()
                .eq(PurRfqItem::getRfqId, rfqId)
                .remove();
    }
}
