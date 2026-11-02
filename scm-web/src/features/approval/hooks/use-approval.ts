"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { message } from "antd";

import { toApiError } from "@/lib/api/envelope";
import { approvalApiService } from "@/lib/api/services/approval";
import type {
  ApproveRequest,
  RejectRequest,
  StartProcessRequest,
  SysPermissionApprovalDTO,
} from "@/lib/api/contracts/approval";

const fail = (error: unknown) => message.error(toApiError(error).message);

export function usePendingApprovals(approverId: string) {
  return useQuery({
    queryKey: ["approval", "permission", "pending", approverId],
    queryFn: () => approvalApiService.listPending(approverId),
    enabled: !!approverId,
  });
}

export function useApplicantApprovals(applicantId: string) {
  return useQuery({
    queryKey: ["approval", "permission", "applicant", applicantId],
    queryFn: () => approvalApiService.listByApplicant(applicantId),
    enabled: !!applicantId,
  });
}

export function useApprovePermission() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      approverId,
      approverName,
    }: {
      id: string;
      approverId: string;
      approverName: string;
    }) => approvalApiService.approvePermission(id, approverId, approverName),
    onSuccess: () => {
      message.success("审批已通过");
      qc.invalidateQueries({ queryKey: ["approval"] });
    },
    onError: fail,
  });
}

export function useRejectPermission() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      approverId,
      approverName,
      rejectReason,
    }: {
      id: string;
      approverId: string;
      approverName: string;
      rejectReason: string;
    }) =>
      approvalApiService.rejectPermission(
        id,
        approverId,
        approverName,
        rejectReason,
      ),
    onSuccess: () => {
      message.success("审批已驳回");
      qc.invalidateQueries({ queryKey: ["approval"] });
    },
    onError: fail,
  });
}

export function useSubmitApproval() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (entity: Partial<SysPermissionApprovalDTO>) =>
      approvalApiService.submit(entity),
    onSuccess: () => {
      message.success("审批申请已提交");
      qc.invalidateQueries({ queryKey: ["approval"] });
    },
    onError: fail,
  });
}

export function useTodoTasks(assigneeId: string) {
  return useQuery({
    queryKey: ["approval", "flow", "todo", assigneeId],
    queryFn: () => approvalApiService.listTodo(assigneeId),
    enabled: !!assigneeId,
  });
}

export function useStartProcess() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: StartProcessRequest) =>
      approvalApiService.startProcess(request),
    onSuccess: () => {
      message.success("审批流程已发起");
      qc.invalidateQueries({ queryKey: ["approval"] });
    },
    onError: fail,
  });
}

export function useApproveTask() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: ApproveRequest) =>
      approvalApiService.approveTask(request),
    onSuccess: () => {
      message.success("待办任务已通过");
      qc.invalidateQueries({ queryKey: ["approval"] });
    },
    onError: fail,
  });
}

export function useRejectTask() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (request: RejectRequest) =>
      approvalApiService.rejectTask(request),
    onSuccess: () => {
      message.success("待办任务已驳回");
      qc.invalidateQueries({ queryKey: ["approval"] });
    },
    onError: fail,
  });
}
