import apiClient from "../client";
import { unwrap } from "../envelope";
import type {
  ApproveRequest,
  RejectRequest,
  StartProcessRequest,
  SysPermissionApprovalDTO,
  TaskDTO,
} from "../contracts/approval";

const APPROVAL_BASE = "/scm-approval";

const get = <T>(url: string, params?: object) =>
  unwrap<T>(apiClient.get(url, { params }));

const post = <T>(url: string, body?: unknown, params?: object) =>
  unwrap<T>(apiClient.post(url, body, { params }));

export const approvalApiService = {
  listPending: (approverId: string) =>
    get<SysPermissionApprovalDTO[]>(
      `${APPROVAL_BASE}/sys-permission-approval/pending`,
      { approverId },
    ),
  listByApplicant: (applicantId: string) =>
    get<SysPermissionApprovalDTO[]>(
      `${APPROVAL_BASE}/sys-permission-approval/applicant/${applicantId}`,
    ),
  submit: (entity: Partial<SysPermissionApprovalDTO>) =>
    post<SysPermissionApprovalDTO>(
      `${APPROVAL_BASE}/sys-permission-approval/submit`,
      entity,
    ),
  approvePermission: (id: string, approverId: string, approverName: string) =>
    post<SysPermissionApprovalDTO>(
      `${APPROVAL_BASE}/sys-permission-approval/${id}/approve`,
      undefined,
      { approverId, approverName },
    ),
  rejectPermission: (
    id: string,
    approverId: string,
    approverName: string,
    rejectReason: string,
  ) =>
    post<SysPermissionApprovalDTO>(
      `${APPROVAL_BASE}/sys-permission-approval/${id}/reject`,
      undefined,
      { approverId, approverName, rejectReason },
    ),

  listTodo: (assigneeId: string) =>
    get<TaskDTO[]>(`${APPROVAL_BASE}/approval/flow/todo`, { assigneeId }),
  startProcess: (request: StartProcessRequest) =>
    post<string>(`${APPROVAL_BASE}/approval/flow/start`, request),
  approveTask: (request: ApproveRequest) =>
    post<void>(`${APPROVAL_BASE}/approval/flow/approve`, request),
  rejectTask: (request: RejectRequest) =>
    post<void>(`${APPROVAL_BASE}/approval/flow/reject`, request),
  processStatus: (businessKey: string) =>
    get<string>(`${APPROVAL_BASE}/approval/flow/status/${businessKey}`),
};
