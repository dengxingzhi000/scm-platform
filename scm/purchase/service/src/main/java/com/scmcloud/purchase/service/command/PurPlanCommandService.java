package com.scmcloud.purchase.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.purchase.domain.entity.PurPlan;
import com.scmcloud.purchase.mapper.PurPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.scmcloud.common.status.StatusValidator;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurPlanCommandService {

    private final PurPlanMapper purPlanMapper;
    private final StatusValidator statusValidator;

    @Master(reason = "保存采购计划")
    @Transactional(rollbackFor = Exception.class)
    public boolean save(PurPlan entity) {
        return purPlanMapper.insert(entity) > 0;
    }

    @Master(reason = "更新采购计划")
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(PurPlan entity) {
        return purPlanMapper.updateById(entity) > 0;
    }

    @Master(reason = "删除采购计划")
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(String id) {
        return purPlanMapper.deleteById(id) > 0;
    }

    @Master(reason = "提交采购计划")
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(String id) {
        PurPlan plan = purPlanMapper.selectById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("采购计划不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "DRAFT", "PENDING_APPROVAL");
        plan.setStatus(1); // PENDING_APPROVAL
        plan.setUpdateTime(LocalDateTime.now());
        return purPlanMapper.updateById(plan) > 0;
    }

    @Master(reason = "审批采购计划")
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(String id, String approverId, String approverName) {
        PurPlan plan = purPlanMapper.selectById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("采购计划不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "PENDING_APPROVAL", "APPROVED");
        plan.setStatus(2); // APPROVED
        plan.setApprovedBy(approverId);
        plan.setApprovedByName(approverName);
        plan.setApprovedAt(LocalDateTime.now());
        plan.setUpdateTime(LocalDateTime.now());
        return purPlanMapper.updateById(plan) > 0;
    }

    @Master(reason = "开始执行采购计划")
    @Transactional(rollbackFor = Exception.class)
    public boolean startExecution(String id) {
        PurPlan plan = purPlanMapper.selectById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("采购计划不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "APPROVED", "APPROVED");
        plan.setUpdateTime(LocalDateTime.now());
        return purPlanMapper.updateById(plan) > 0;
    }

    @Master(reason = "完成采购计划")
    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String id) {
        PurPlan plan = purPlanMapper.selectById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("采购计划不存在: " + id);
        }
        statusValidator.validateTransition("PURCHASE", "APPROVED", "REJECTED");
        plan.setStatus(3); // REJECTED
        plan.setExecutionRate(java.math.BigDecimal.valueOf(100));
        plan.setUpdateTime(LocalDateTime.now());
        return purPlanMapper.updateById(plan) > 0;
    }
}
