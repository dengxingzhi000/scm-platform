/** `com.scmcloud.approval.domain.entity.SysPermissionApproval`. */
export interface SysPermissionApprovalDTO {
  id: string;
  applicantId?: string | null;
  approvalType?: number | null;
  targetUserId?: string | null;
  roleIds?: string | null;
  permissionIds?: string | null;
  effectiveTime?: string | null;
  expireTime?: string | null;
  applyReason?: string | null;
  businessJustification?: string | null;
  approvalStatus?: number | null;
  currentApproverId?: string | null;
  approvalChain?: string | null;
  approvedBy?: string | null;
  approvedTime?: string | null;
  rejectReason?: string | null;
  createTime?: string | null;
  updateTime?: string | null;
  applicantName?: string | null;
  applicantDeptName?: string | null;
  targetUserName?: string | null;
  approverName?: string | null;
  roleNames?: string | null;
  permissionNames?: string | null;
}

/** `com.scmcloud.approval.api.dto.TaskDTO` (flow engine task). */
export interface TaskDTO {
  taskId: string;
  taskName?: string | null;
  assignee?: string | null;
  processInstanceId?: string | null;
  createTime?: string | null;
  dueDate?: string | null;
  taskDefinitionKey?: string | null;
}

export interface StartProcessRequest {
  processKey?: string | null;
  businessKey?: string | null;
  businessType?: string | null;
  applicantId?: string | null;
  variables?: Record<string, unknown> | null;
}

export interface ApproveRequest {
  taskId: string;
  approverId: string;
  comment?: string;
  variables?: Record<string, unknown> | null;
}

export interface RejectRequest {
  taskId: string;
  approverId: string;
  comment?: string;
}

export const APPROVAL_TYPE: Record<number, string> = {
  1: "角色申请",
  2: "权限申请",
  3: "临时授权",
};

export const APPROVAL_STATUS: Record<
  number,
  { label: string; variant: "warning" | "info" | "success" | "destructive" | "secondary" }
> = {
  0: { label: "待审批", variant: "warning" },
  1: { label: "审批中", variant: "info" },
  2: { label: "已批准", variant: "success" },
  3: { label: "已拒绝", variant: "destructive" },
  4: { label: "已撤回", variant: "secondary" },
};
