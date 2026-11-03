package com.scmcloud.purchase.service.query;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.scmcloud.common.data.rw.annotation.Slave;
import com.scmcloud.purchase.domain.entity.PurPlan;
import com.scmcloud.purchase.mapper.PurPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurPlanQueryService {

    private final PurPlanMapper purPlanMapper;

    @Slave
    public PurPlan getById(String id) {
        return purPlanMapper.selectById(id);
    }

    @Slave
    public List<PurPlan> listAll() {
        return purPlanMapper.selectList(null);
    }

    @Slave
    public Page<PurPlan> pageQuery(Page<PurPlan> page, Wrapper<PurPlan> wrapper) {
        return purPlanMapper.selectPage(page, wrapper);
    }

    @Slave
    public PurPlan getByPlanNo(String planNo) {
        LambdaQueryWrapper<PurPlan> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurPlan::getPlanNo, planNo);
        wrapper.eq(PurPlan::getDeleted, false);
        return purPlanMapper.selectOne(wrapper);
    }

    @Slave
    public Page<PurPlan> pageQuery(int page, int size, Integer status, Integer planType, String keyword) {
        LambdaQueryWrapper<PurPlan> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(PurPlan::getStatus, status);
        }
        if (planType != null) {
            wrapper.eq(PurPlan::getPlanType, planType);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(PurPlan::getPlanNo, keyword)
                    .or()
                    .like(PurPlan::getPlanName, keyword));
        }
        wrapper.eq(PurPlan::getDeleted, false);
        wrapper.orderByDesc(PurPlan::getCreateTime);
        return purPlanMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Slave
    public List<PurPlan> listByStatus(Integer status) {
        LambdaQueryWrapper<PurPlan> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(PurPlan::getStatus, status);
        wrapper.eq(PurPlan::getDeleted, false);
        wrapper.orderByDesc(PurPlan::getCreateTime);
        return purPlanMapper.selectList(wrapper);
    }
}
