package scm.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import scm.purchase.domain.entity.PurPlan;
import scm.purchase.mapper.PurPlanMapper;
import scm.purchase.service.IPurPlanService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class PurPlanServiceImpl extends ServiceImpl<PurPlanMapper, PurPlan> implements IPurPlanService {

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
            throw new IllegalArgumentException("采购计划不存在: " + id);
        }
        if (plan.getStatus() != 0) {
            throw new IllegalStateException("只有编制中的计划才能提交");
        }
        plan.setStatus(1);
        plan.setUpdateTime(LocalDateTime.now());
        return updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(String id, String approverId, String approverName) {
        PurPlan plan = getById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("采购计划不存在: " + id);
        }
        if (plan.getStatus() != 1) {
            throw new IllegalStateException("只有待审批的计划才能审批");
        }
        plan.setStatus(2);
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
            throw new IllegalArgumentException("采购计划不存在: " + id);
        }
        if (plan.getStatus() != 2) {
            throw new IllegalStateException("只有已审批的计划才能开始执行");
        }
        plan.setUpdateTime(LocalDateTime.now());
        return updateById(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean complete(String id) {
        PurPlan plan = getById(id);
        if (plan == null || plan.getDeleted()) {
            throw new IllegalArgumentException("采购计划不存在: " + id);
        }
        if (plan.getStatus() != 2) {
            throw new IllegalStateException("只有执行中的计划才能完成");
        }
        plan.setStatus(3);
        plan.setExecutionRate(java.math.BigDecimal.valueOf(100));
        plan.setUpdateTime(LocalDateTime.now());
        return updateById(plan);
    }
}
