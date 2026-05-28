package scm.approval.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.frog.common.util.UUIDv7Util;
import scm.approval.domain.entity.SysPermissionApproval;
import scm.approval.mapper.SysPermissionApprovalMapper;
import scm.approval.service.ISysPermissionApprovalService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SysPermissionApprovalServiceImpl extends ServiceImpl<SysPermissionApprovalMapper, SysPermissionApproval>
        implements ISysPermissionApprovalService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_WITHDRAWN = 4;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval submitApproval(SysPermissionApproval approval) {
        log.info("提交审批申请: applicantId={}, type={}", approval.getApplicantId(), approval.getApprovalType());

        approval.setId(UUIDv7Util.generateString());
        approval.setApprovalStatus(STATUS_PENDING);
        approval.setCreateTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());

        boolean success = save(approval);
        if (!success) {
            throw new RuntimeException("提交审批申请失败");
        }

        log.info("审批申请提交成功: id={}", approval.getId());
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval approve(String approvalId, String approverId, String approverName) {
        log.info("审批通过: approvalId={}, approverId={}", approvalId, approverId);

        SysPermissionApproval approval = getById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("审批记录不存在: " + approvalId);
        }

        if (approval.getApprovalStatus() != STATUS_PENDING
                && approval.getApprovalStatus() != STATUS_APPROVING) {
            throw new IllegalStateException("当前状态不允许审批: status=" + approval.getApprovalStatus());
        }

        approval.setApprovalStatus(STATUS_APPROVED);
        approval.setApprovedBy(approverId);
        approval.setApproverName(approverName);
        approval.setApprovedTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(approval);
        if (!success) {
            throw new RuntimeException("审批操作失败");
        }

        log.info("审批通过成功: id={}", approvalId);
        return approval;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval reject(String approvalId, String approverId, String approverName, String rejectReason) {
        log.info("审批拒绝: approvalId={}, approverId={}", approvalId, approverId);

        SysPermissionApproval approval = getById(approvalId);
        if (approval == null) {
            throw new IllegalArgumentException("审批记录不存在: " + approvalId);
        }

        if (approval.getApprovalStatus() != STATUS_PENDING
                && approval.getApprovalStatus() != STATUS_APPROVING) {
            throw new IllegalStateException("当前状态不允许审批: status=" + approval.getApprovalStatus());
        }

        approval.setApprovalStatus(STATUS_REJECTED);
        approval.setApprovedBy(approverId);
        approval.setApproverName(approverName);
        approval.setApprovedTime(LocalDateTime.now());
        approval.setRejectReason(rejectReason);
        approval.setUpdateTime(LocalDateTime.now());

        boolean success = updateById(approval);
        if (!success) {
            throw new RuntimeException("审批拒绝操作失败");
        }

        log.info("审批拒绝成功: id={}", approvalId);
        return approval;
    }

    @Override
    public List<SysPermissionApproval> listByApplicant(String applicantId) {
        log.debug("查询申请人审批列表: applicantId={}", applicantId);

        LambdaQueryWrapper<SysPermissionApproval> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SysPermissionApproval::getApplicantId, applicantId)
               .orderByDesc(SysPermissionApproval::getCreateTime);

        return list(wrapper);
    }

    @Override
    public List<SysPermissionApproval> listPending(String approverId) {
        log.debug("查询待审批列表: approverId={}", approverId);

        LambdaQueryWrapper<SysPermissionApproval> wrapper = Wrappers.lambdaQuery();
        wrapper.and(w -> w.eq(SysPermissionApproval::getApprovalStatus, STATUS_PENDING)
                          .or()
                          .eq(SysPermissionApproval::getApprovalStatus, STATUS_APPROVING));
        wrapper.orderByDesc(SysPermissionApproval::getCreateTime);

        return list(wrapper);
    }
}
