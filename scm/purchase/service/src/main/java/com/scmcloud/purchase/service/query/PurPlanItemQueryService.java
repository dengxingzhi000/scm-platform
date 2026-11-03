package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurPlanItem;
import com.scmcloud.purchase.mapper.PurPlanItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurPlanItemQueryService {

    private final PurPlanItemMapper purPlanItemMapper;

    @Slave
    public PurPlanItem getById(String id) {
        return purPlanItemMapper.selectById(id);
    }

    @Slave
    public List<PurPlanItem> listAll() {
        return purPlanItemMapper.selectList(null);
    }

    @Slave
    public Page<PurPlanItem> pageQuery(Page<PurPlanItem> page, Wrapper<PurPlanItem> wrapper) {
        return purPlanItemMapper.selectPage(page, wrapper);
    }

    @Slave
    public List<PurPlanItem> listByPlanId(String planId) {
        LambdaQueryWrapper<PurPlanItem> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurPlanItem::getPlanId, planId);
        wrapper.orderByAsc(PurPlanItem::getCreateTime);
        return purPlanItemMapper.selectList(wrapper);
    }
}
