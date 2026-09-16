package com.scmcloud.approval.service;

import com.scmcloud.approval.domain.entity.SysPermissionApproval;
import com.baomidou.mybatisplus.spring.service.IService;

import java.util.List;

/**
 * <p>
 * 鏉冮檺鐢宠瀹℃壒锟芥湇鍔★拷
 * </p>
 *
 * @author deng
 * @since 2025-12-26
 */
public interface ISysPermissionApprovalService extends IService<SysPermissionApproval> {

    SysPermissionApproval submitApproval(SysPermissionApproval approval);

    SysPermissionApproval approve(String approvalId, String approverId, String approverName);

    SysPermissionApproval reject(String approvalId, String approverId, String approverName, String rejectReason);

    List<SysPermissionApproval> listByApplicant(String applicantId);

    List<SysPermissionApproval> listPending(String approverId);
}
