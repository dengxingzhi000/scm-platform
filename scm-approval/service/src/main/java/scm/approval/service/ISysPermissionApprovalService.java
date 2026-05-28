package scm.approval.service;

import scm.approval.domain.entity.SysPermissionApproval;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 * 权限申请审批表 服务类
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
