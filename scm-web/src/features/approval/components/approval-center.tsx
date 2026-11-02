"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { PageHeader } from "@/components/ui/page-header";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  TableEmpty,
  TableLoading,
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import { Textarea } from "@/components/ui/textarea";
import { cn } from "@/lib/utils";
import {
  APPROVAL_STATUS,
  APPROVAL_TYPE,
  type SysPermissionApprovalDTO,
  type TaskDTO,
} from "@/lib/api/contracts/approval";
import { useAuthStore } from "@/stores/useAuthStore";

import {
  useApplicantApprovals,
  useApprovePermission,
  useApproveTask,
  usePendingApprovals,
  useRejectPermission,
  useRejectTask,
  useStartProcess,
  useSubmitApproval,
  useTodoTasks,
} from "../hooks";
import { formatDateTime } from "../lib/format";

const TABS = [
  { key: "todo", label: "待办任务" },
  { key: "permission", label: "权限审批" },
] as const;

type TabKey = (typeof TABS)[number]["key"];

type Decision = {
  kind: "task" | "permission";
  action: "approve" | "reject";
  id: string;
  title: string;
};

export function ApprovalCenter() {
  const storeUser = useAuthStore((state) => state.user);
  const [actorId, setActorId] = useState("");
  const [actorName, setActorName] = useState("");
  const [tab, setTab] = useState<TabKey>("todo");
  const [scope, setScope] = useState<"pending" | "applicant">("pending");

  const effectiveId = actorId || storeUser?.id || "";
  const effectiveName =
    actorName || storeUser?.displayName || storeUser?.username || "";

  const pending = usePendingApprovals(
    scope === "pending" ? effectiveId : "",
  );
  const applicant = useApplicantApprovals(
    scope === "applicant" ? effectiveId : "",
  );
  const todo = useTodoTasks(effectiveId);

  const approvePermission = useApprovePermission();
  const rejectPermission = useRejectPermission();
  const approveTask = useApproveTask();
  const rejectTask = useRejectTask();
  const startProcess = useStartProcess();
  const submitApproval = useSubmitApproval();

  const [decision, setDecision] = useState<Decision | null>(null);
  const [comment, setComment] = useState("");
  const [startOpen, setStartOpen] = useState(false);
  const [applyOpen, setApplyOpen] = useState(false);

  const [startForm, setStartForm] = useState({
    processKey: "",
    businessKey: "",
    businessType: "",
    variables: "{}",
  });
  const [applyForm, setApplyForm] = useState({
    approvalType: "2",
    targetUserId: "",
    roleIds: "",
    permissionIds: "",
    applyReason: "",
    businessJustification: "",
    effectiveTime: "",
    expireTime: "",
  });

  const openDecision = (next: Decision) => {
    setComment("");
    setDecision(next);
  };

  const submitDecision = async () => {
    if (!decision) return;
    const needComment = decision.action === "reject";
    if (needComment && !comment.trim()) return;

    if (decision.kind === "task") {
      if (decision.action === "approve") {
        await approveTask.mutateAsync({
          taskId: decision.id,
          approverId: effectiveId,
          comment: comment.trim() || undefined,
        });
      } else {
        await rejectTask.mutateAsync({
          taskId: decision.id,
          approverId: effectiveId,
          comment: comment.trim(),
        });
      }
    } else if (decision.action === "approve") {
      await approvePermission.mutateAsync({
        id: decision.id,
        approverId: effectiveId,
        approverName: effectiveName,
      });
    } else {
      await rejectPermission.mutateAsync({
        id: decision.id,
        approverId: effectiveId,
        approverName: effectiveName,
        rejectReason: comment.trim(),
      });
    }
    setDecision(null);
  };

  const submitProcess = async () => {
    let variables: Record<string, unknown> = {};
    if (startForm.variables.trim()) {
      try {
        variables = JSON.parse(startForm.variables) as Record<string, unknown>;
      } catch {
        return;
      }
    }
    await startProcess.mutateAsync({
      processKey: startForm.processKey.trim(),
      businessKey: startForm.businessKey.trim(),
      businessType: startForm.businessType.trim() || undefined,
      applicantId: effectiveId,
      variables,
    });
    setStartOpen(false);
  };

  const submitApplication = async () => {
    await submitApproval.mutateAsync({
      applicantId: effectiveId,
      approvalType: Number(applyForm.approvalType),
      targetUserId: applyForm.targetUserId.trim() || null,
      roleIds: applyForm.roleIds.trim() || null,
      permissionIds: applyForm.permissionIds.trim() || null,
      applyReason: applyForm.applyReason.trim(),
      businessJustification: applyForm.businessJustification.trim() || null,
      effectiveTime: applyForm.effectiveTime || null,
      expireTime: applyForm.expireTime || null,
      approvalStatus: 0,
    });
    setApplyOpen(false);
  };

  const permissionRows: SysPermissionApprovalDTO[] =
    (scope === "pending" ? pending.data : applicant.data) ?? [];
  const permissionLoading =
    scope === "pending" ? pending.isLoading : applicant.isLoading;
  const todoRows: TaskDTO[] = todo.data ?? [];
  const canAct = Boolean(effectiveId && effectiveName);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="审批中心"
        title="审批工作台"
        description="处理权限变更申请与流程待办；操作人取自当前登录用户，也可手动指定"
        actions={
          <>
            <Button variant="outline" onClick={() => setApplyOpen(true)}>
              提交权限申请
            </Button>
            <Button onClick={() => setStartOpen(true)}>发起审批流</Button>
          </>
        }
      />

      <div className="flex flex-wrap items-end gap-2 rounded-lg border border-[var(--color-border-secondary)] bg-card px-3 py-2">
        <div>
          <Label className="mb-1 block text-[10px] text-muted-foreground">
            操作人 ID
          </Label>
          <Input
            className="h-7 w-64 text-xs"
            value={actorId}
            placeholder={storeUser?.id ?? "未登录或未加载用户"}
            onChange={(event) => setActorId(event.target.value)}
          />
        </div>
        <div>
          <Label className="mb-1 block text-[10px] text-muted-foreground">
            操作人姓名
          </Label>
          <Input
            className="h-7 w-40 text-xs"
            value={actorName}
            placeholder={storeUser?.displayName ?? "审批人姓名"}
            onChange={(event) => setActorName(event.target.value)}
          />
        </div>
        {!canAct && (
          <span className="text-[11px] text-[var(--color-warning-text)]">
            请填写操作人 ID 与姓名后再审批
          </span>
        )}
      </div>

      <TablePanel>
        <TableToolbar>
          <div className="flex gap-1">
            {TABS.map((item) => (
              <button
                key={item.key}
                type="button"
                onClick={() => setTab(item.key)}
                className={cn(
                  "rounded-md px-2.5 py-1 text-[12px] transition-colors",
                  tab === item.key
                    ? "bg-[var(--color-primary-soft)] font-medium text-[var(--color-primary-text)]"
                    : "text-muted-foreground hover:bg-sunken",
                )}
              >
                {item.label}
                {item.key === "todo" && todoRows.length > 0 && ` (${todoRows.length})`}
              </button>
            ))}
          </div>
          <TableToolbarEnd>
            {tab === "permission" && (
              <Select
                value={scope}
                onValueChange={(value) =>
                  setScope(value as "pending" | "applicant")
                }
              >
                <SelectTrigger size="sm" className="w-32 text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="pending">待我审批</SelectItem>
                  <SelectItem value="applicant">我提交的</SelectItem>
                </SelectContent>
              </Select>
            )}
            <span className="text-muted-foreground text-[11px]">
              {tab === "todo"
                ? `共 ${todoRows.length} 条待办`
                : `共 ${permissionRows.length} 条申请`}
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {tab === "todo" ? (
          todo.isLoading ? (
            <TableLoading />
          ) : todoRows.length === 0 ? (
            <TableEmpty>
              {effectiveId ? "暂无待办任务" : "填写操作人 ID 后查询待办任务"}
            </TableEmpty>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="min-w-[180px]">任务</TableHead>
                  <TableHead>流程实例</TableHead>
                  <TableHead>节点</TableHead>
                  <TableHead>负责人</TableHead>
                  <TableHead>创建时间</TableHead>
                  <TableHead>截止时间</TableHead>
                  <TableHead className="text-right">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {todoRows.map((task) => (
                  <TableRow key={task.taskId}>
                    <TableCell className="font-medium">{task.taskName}</TableCell>
                    <TableCell className="font-mono text-[10px]">
                      {task.processInstanceId ?? "-"}
                    </TableCell>
                    <TableCell>{task.taskDefinitionKey ?? "-"}</TableCell>
                    <TableCell>{task.assignee ?? "-"}</TableCell>
                    <TableCell>{formatDateTime(task.createTime)}</TableCell>
                    <TableCell>{formatDateTime(task.dueDate)}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled={!canAct}
                        onClick={() =>
                          openDecision({
                            kind: "task",
                            action: "approve",
                            id: task.taskId,
                            title: task.taskName ?? "审批任务",
                          })
                        }
                      >
                        通过
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        disabled={!canAct}
                        onClick={() =>
                          openDecision({
                            kind: "task",
                            action: "reject",
                            id: task.taskId,
                            title: task.taskName ?? "审批任务",
                          })
                        }
                      >
                        驳回
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )
        ) : permissionLoading ? (
          <TableLoading />
        ) : permissionRows.length === 0 ? (
          <TableEmpty>
            {effectiveId ? "暂无权限审批单" : "填写操作人 ID 后查询"}
          </TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[160px]">申请人</TableHead>
                <TableHead>类型</TableHead>
                <TableHead className="min-w-[200px]">申请内容</TableHead>
                <TableHead>理由</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>申请时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {permissionRows.map((item) => {
                const status = APPROVAL_STATUS[item.approvalStatus ?? 0];
                const pendingStatus =
                  (item.approvalStatus ?? 0) === 0 ||
                  (item.approvalStatus ?? 0) === 1;
                return (
                  <TableRow key={item.id}>
                    <TableCell>
                      <div className="font-medium">
                        {item.applicantName ?? item.applicantId ?? "-"}
                      </div>
                      <div className="text-muted-foreground text-[10px]">
                        {item.applicantDeptName ?? "-"}
                      </div>
                    </TableCell>
                    <TableCell>
                      {APPROVAL_TYPE[item.approvalType ?? 1] ?? "-"}
                    </TableCell>
                    <TableCell>
                      <div>{item.targetUserName ?? item.roleNames ?? "-"}</div>
                      <div className="text-muted-foreground text-[10px]">
                        {item.permissionNames ?? ""}
                      </div>
                    </TableCell>
                    <TableCell className="max-w-[240px] truncate">
                      {item.applyReason ?? "-"}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={status?.variant ?? "secondary"}>
                        {status?.label ?? "-"}
                      </Badge>
                    </TableCell>
                    <TableCell>{formatDateTime(item.createTime)}</TableCell>
                    <TableCell className="text-right">
                      {pendingStatus ? (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={!canAct}
                            onClick={() =>
                              openDecision({
                                kind: "permission",
                                action: "approve",
                                id: item.id,
                                title: "通过权限审批",
                              })
                            }
                          >
                            通过
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-destructive"
                            disabled={!canAct}
                            onClick={() =>
                              openDecision({
                                kind: "permission",
                                action: "reject",
                                id: item.id,
                                title: "驳回权限审批",
                              })
                            }
                          >
                            驳回
                          </Button>
                        </>
                      ) : (
                        <span className="text-muted-foreground text-[11px]">
                          {item.rejectReason || "已处理"}
                        </span>
                      )}
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}
      </TablePanel>

      <Dialog
        open={Boolean(decision)}
        onOpenChange={(open) => !open && setDecision(null)}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              {decision?.action === "approve" ? "通过审批" : "驳回审批"}
            </DialogTitle>
            <DialogDescription>
              {decision?.title}
              {decision?.action === "reject" ? "：请填写驳回原因。" : "：可填写审批意见。"}
            </DialogDescription>
          </DialogHeader>
          <Textarea
            rows={3}
            value={comment}
            placeholder={
              decision?.action === "reject" ? "驳回原因（必填）" : "审批意见（可选）"
            }
            onChange={(event) => setComment(event.target.value)}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setDecision(null)}>
              取消
            </Button>
            <Button
              variant={decision?.action === "reject" ? "destructive" : "default"}
              disabled={
                (decision?.action === "reject" && !comment.trim()) ||
                approveTask.isPending ||
                rejectTask.isPending ||
                approvePermission.isPending ||
                rejectPermission.isPending
              }
              onClick={submitDecision}
            >
              确认
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={startOpen} onOpenChange={setStartOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>发起审批流</DialogTitle>
            <DialogDescription>
              以当前操作人身份针对业务单据发起流程实例。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3">
            <div>
              <Label className="mb-1.5 text-[12px]">流程 Key</Label>
              <Input
                value={startForm.processKey}
                placeholder="例如 purchase_approval"
                onChange={(event) =>
                  setStartForm({ ...startForm, processKey: event.target.value })
                }
              />
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
              <div>
                <Label className="mb-1.5 text-[12px]">业务单号</Label>
                <Input
                  value={startForm.businessKey}
                  onChange={(event) =>
                    setStartForm({ ...startForm, businessKey: event.target.value })
                  }
                />
              </div>
              <div>
                <Label className="mb-1.5 text-[12px]">业务类型</Label>
                <Input
                  value={startForm.businessType}
                  placeholder="例如 PURCHASE_ORDER"
                  onChange={(event) =>
                    setStartForm({ ...startForm, businessType: event.target.value })
                  }
                />
              </div>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">流程变量 (JSON)</Label>
              <Textarea
                rows={4}
                className="font-mono text-[11px]"
                value={startForm.variables}
                onChange={(event) =>
                  setStartForm({ ...startForm, variables: event.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setStartOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                startProcess.isPending ||
                !effectiveId ||
                !startForm.processKey.trim() ||
                !startForm.businessKey.trim()
              }
              onClick={submitProcess}
            >
              {startProcess.isPending ? "发起中…" : "发起"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={applyOpen} onOpenChange={setApplyOpen}>
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>提交权限申请</DialogTitle>
            <DialogDescription>
              提交后进入待审批状态，由审批人处理。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">申请类型</Label>
              <Select
                value={applyForm.approvalType}
                onValueChange={(value) =>
                  setApplyForm({ ...applyForm, approvalType: value })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(APPROVAL_TYPE).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">目标用户 ID</Label>
              <Input
                value={applyForm.targetUserId}
                onChange={(event) =>
                  setApplyForm({ ...applyForm, targetUserId: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">角色 ID</Label>
              <Input
                value={applyForm.roleIds}
                placeholder="多个用逗号分隔"
                onChange={(event) =>
                  setApplyForm({ ...applyForm, roleIds: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">权限 ID</Label>
              <Input
                value={applyForm.permissionIds}
                placeholder="多个用逗号分隔"
                onChange={(event) =>
                  setApplyForm({
                    ...applyForm,
                    permissionIds: event.target.value,
                  })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">生效时间</Label>
              <Input
                type="datetime-local"
                value={applyForm.effectiveTime}
                onChange={(event) =>
                  setApplyForm({ ...applyForm, effectiveTime: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">失效时间</Label>
              <Input
                type="datetime-local"
                value={applyForm.expireTime}
                onChange={(event) =>
                  setApplyForm({ ...applyForm, expireTime: event.target.value })
                }
              />
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">申请理由</Label>
              <Textarea
                rows={2}
                value={applyForm.applyReason}
                onChange={(event) =>
                  setApplyForm({ ...applyForm, applyReason: event.target.value })
                }
              />
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">业务说明</Label>
              <Textarea
                rows={2}
                value={applyForm.businessJustification}
                onChange={(event) =>
                  setApplyForm({
                    ...applyForm,
                    businessJustification: event.target.value,
                  })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setApplyOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                submitApproval.isPending ||
                !effectiveId ||
                !applyForm.applyReason.trim()
              }
              onClick={submitApplication}
            >
              {submitApproval.isPending ? "提交中…" : "提交申请"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
