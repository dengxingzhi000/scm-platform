package com.scmcloud.approval.service.command;

import com.scmcloud.common.data.rw.annotation.Master;
import com.scmcloud.common.util.UUIDv7Util;
import com.scmcloud.approval.domain.entity.SysPermissionApproval;
import com.scmcloud.approval.mapper.SysPermissionApprovalMapper;
import com.scmcloud.approval.service.notification.ApprovalNotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysPermissionApprovalCommandService {
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_WITHDRAWN = 4;

    private final SysPermissionApprovalMapper sysPermissionApprovalMapper;
    private final ApprovalNotificationPublisher notificationPublisher;

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval submitApproval(SysPermissionApproval approval) {
        log.info("提交审批申请: applicantId={}, type={}", approval.getApplicantId(), approval.getApprovalType());

        approval.setId(UUIDv7Util.generateString());
        approval.setApprovalStatus(STATUS_PENDING);
        approval.setCreateTime(LocalDateTime.now());
        approval.setUpdateTime(LocalDateTime.now());

        int rows = sysPermissionApprovalMapper.insert(approval);
        if (rows <= 0) {
            throw new RuntimeException("提交审批申请失败");
        }

        log.info("审批申请提交成功: id={}", approval.getId());
        return approval;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval approve(String approvalId, String approverId, String approverName) {
        log.info("审批通过: approvalId={}, approverId={}", approvalId, approverId);

        SysPermissionApproval approval = sysPermissionApprovalMapper.selectById(approvalId);
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

        int rows = sysPermissionApprovalMapper.updateById(approval);
        if (rows <= 0) {
            throw new RuntimeException("审批操作失败");
        }

        log.info("审批通过成功: id={}", approvalId);
        notifyAfterCommit(approval, true);
        return approval;
    }

    @Master(reason = "写操作必须走主库")
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionApproval reject(String approvalId, String approverId, String approverName, String rejectReason) {
        log.info("审批拒绝: approvalId={}, approverId={}", approvalId, approverId);

        SysPermissionApproval approval = sysPermissionApprovalMapper.selectById(approvalId);
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

        int rows = sysPermissionApprovalMapper.updateById(approval);
        if (rows <= 0) {
            throw new RuntimeException("审批拒绝操作失败");
        }

        log.info("审批拒绝成功: id={}", approvalId);
        notifyAfterCommit(approval, false);
        return approval;
    }

    /** Notifying before the commit would tell the applicant about a decision that never landed. */
    private void notifyAfterCommit(SysPermissionApproval approval, boolean approved) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationPublisher.publish(approval, approved);
                }
            });
        } else {
            notificationPublisher.publish(approval, approved);
        }
    }
}
