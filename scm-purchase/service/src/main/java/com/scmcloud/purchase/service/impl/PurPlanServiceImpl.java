package com.scmcloud.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.scmcloud.common.status.StatusValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.scmcloud.purchase.domain.entity.PurPlan;
import com.scmcloud.purchase.mapper.PurPlanMapper;
import com.scmcloud.purchase.service.IPurPlanService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurPlanServiceImpl extends ServiceImpl<PurPlanMapper, PurPlan> implements IPurPlanService {

    @Autowired
    private StatusValidator statusValidator;

    @Override
    public PurPlan getByPlanNo(String planNo) {
        return lambdaQuery()
                .eq(PurPlan::getPlanNo, planNo)
                .eq(PurPlan::getDeleted, false)
                .one();
    }

    @Override
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
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public List<PurPlan> listByStatus(Integer status) {
        return lambdaQuery()
                .eq(PurPlan::getStatus, status)
                .eq(PurPlan::getDeleted, false)
                .orderByDesc(PurPlan::getCreateTime)
                .list();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(String id) {
        PurPlan plan = getById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("Purchase plan not found: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "DRAFT", "PENDING_APPROVAL");
        plan.setStatus(1); // PENDING_APPROVAL
        plan.setUpdateTime(LocalDateTime.now());
        return updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(String id, String approverId, String approverName) {
        PurPlan plan = getById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("Purchase plan not found: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "PENDING_APPROVAL", "APPROVED");
        plan.setStatus(2); // APPROVED
        plan.setApprovedBy(approverId);
        plan.setApprovedByName(approverName);
        plan.setApprovedAt(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        return updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startExecution(String id) {
        PurPlan plan = getById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("Purchase plan not found: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "APPROVED", "APPROVED");
        plan.setUpdateTime(LocalDateTime.now());
        return updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String id) {
        PurPlan plan = getById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("Purchase plan not found: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "APPROVED", "REJECTED");
        plan.setStatus(3); // REJECTED
        plan.setExecutionRate(java.math.BigDecimal.valueOf(100));
        plan.setUpdateTime(LocalDateTime.now());
        return updateById(plan);
    }
}
