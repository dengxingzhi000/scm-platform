package com.scmcloud.purchase.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.scmcloud.purchase.domain.entity.PurPlanItem;
import com.scmcloud.purchase.mapper.PurPlanItemMapper;
import com.scmcloud.purchase.service.IPurPlanItemService;

import java.util.List;

@Slf4j
@Service
public class PurPlanItemServiceImpl extends ServiceImpl<PurPlanItemMapper, PurPlanItem> implements IPurPlanItemService {

    @Override
    public List<PurPlanItem> listByPlanId(String planId) {
        return lambdaQuery()
                .eq(PurPlanItem::getPlanId, planId)
                .orderByAsc(PurPlanItem::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPlanId(String planId) {
        return lambdaUpdate()
                .eq(PurPlanItem::getPlanId, planId)
                .remove();
    }
}
