package com.scmcloud.purchase.service.command;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurPlanItem;
import com.scmcloud.purchase.mapper.PurPlanItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurPlanItemCommandService {

    private final PurPlanItemMapper purPlanItemMapper;

    @Master(reason = "保存采购计划明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurPlanItem entity) {
        return purPlanItemMapper.insert(entity) > 0;
    }

    @Master(reason = "更新采购计划明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurPlanItem entity) {
        return purPlanItemMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除采购计划明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purPlanItemMapper.deleteById(id) > 0;
    }

    @Master(reason = "根据计划ID删除明细")
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByPlanId(String planId) {
        LambdaUpdateWrapper<PurPlanItem> wrapper = Wrappers.lambdaUpdate();
        wrapper.eq(PurPlanItem::getPlanId, planId);
        return purPlanItemMapper.delete(wrapper) > 0;
    }
}
