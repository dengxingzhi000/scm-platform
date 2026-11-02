"use client";

import { useState } from "react";
import { MoreHorizontal } from "lucide-react";

import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuSub,
  DropdownMenuSubContent,
  DropdownMenuSubTrigger,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  useAuditSupplier,
  useBlacklistSupplier,
  useChangeCooperationStatus,
  useChangeSupplierRiskLevel,
  useDeleteSupplier,
  useDisableSupplier,
  useEnableSupplier,
  useSubmitSupplierAudit,
  useUnblacklistSupplier,
} from "../hooks";
import { errorMessage } from "../lib/api-error";
import { COOPERATION_STATUSES, RISK_LEVELS } from "../lib/constants";
import type { Supplier } from "../types";

interface SupplierActionsProps {
  supplier: Supplier;
  /** 动作成功后回调 —— 列表页据此刷新数据。 */
  onDone: () => void;
}

/** 行内错误条 —— 与 product-form 的 submitError 同一个样式。 */
const ERROR_CLASS =
  "border-destructive/30 bg-destructive/10 text-destructive rounded-md border px-3 py-2 text-sm";

interface Failure {
  title: string;
  message: string;
}

interface GuardedMenuItemProps {
  label: string;
  /** 置灰原因 —— 置灰时作为 tooltip 文案展示。 */
  reason?: string;
  disabled?: boolean;
  destructive?: boolean;
  onSelect: () => void;
}

/**
 * 菜单项。置灰时把原因挂在 tooltip 上 —— Radix 的 disabled 项不派发指针事件，
 * 所以 tooltip 的触发点必须是包在外面的 span。
 */
function GuardedMenuItem({
  label,
  reason,
  disabled,
  destructive,
  onSelect,
}: GuardedMenuItemProps) {
  const item = (
    <DropdownMenuItem
      disabled={disabled}
      variant={destructive ? "destructive" : "default"}
      onSelect={onSelect}
    >
      {label}
    </DropdownMenuItem>
  );

  if (!disabled || !reason) return item;

  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <span className="block">{item}</span>
      </TooltipTrigger>
      <TooltipContent>{reason}</TooltipContent>
    </Tooltip>
  );
}

export default function SupplierActions({
  supplier,
  onDone,
}: SupplierActionsProps) {
  const submitAudit = useSubmitSupplierAudit();
  const auditSupplier = useAuditSupplier();
  const enableSupplier = useEnableSupplier();
  const disableSupplier = useDisableSupplier();
  const changeRiskLevel = useChangeSupplierRiskLevel();
  const changeCooperationStatus = useChangeCooperationStatus();
  const blacklistSupplier = useBlacklistSupplier();
  const unblacklistSupplier = useUnblacklistSupplier();
  const deleteSupplier = useDeleteSupplier();

  const [auditOpen, setAuditOpen] = useState(false);
  const [riskOpen, setRiskOpen] = useState(false);
  const [blacklistOpen, setBlacklistOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [failure, setFailure] = useState<Failure | null>(null);

  const [auditAction, setAuditAction] = useState<"APPROVE" | "REJECT">("APPROVE");
  const [auditRemark, setAuditRemark] = useState("");
  const [auditError, setAuditError] = useState<string | null>(null);

  const [riskLevel, setRiskLevel] = useState(supplier.riskLevel ?? 1);
  const [riskReason, setRiskReason] = useState("");
  const [riskError, setRiskError] = useState<string | null>(null);

  const [blacklistReason, setBlacklistReason] = useState("");
  const [blacklistError, setBlacklistError] = useState<string | null>(null);

  // 置灰只看当前状态的布尔判断，流转规则由后端裁决。
  const enableReason = supplier.blacklisted
    ? "黑名单供应商不可启用"
    : undefined;
  const disableReason =
    supplier.status === "disabled" ? "供应商已停用" : undefined;
  const submitAuditReason = supplier.taxNo
    ? undefined
    : "税号未补全，无法提交审核";
  const establishReason = supplier.blacklisted
    ? "黑名单供应商不可建立合作"
    : supplier.auditStatus !== 2
      ? "未通过准入审核的供应商不可建立合作"
      : undefined;
  const deleteReason =
    supplier.cooperationStatus === 1
      ? "合作中的供应商不能删除，请先终止合作"
      : undefined;
  const unblacklistReason = supplier.blacklisted
    ? undefined
    : "供应商不在黑名单中";

  const fail = (title: string, error: unknown) =>
    setFailure({ title, message: errorMessage(error) });

  const runAction = async (
    title: string,
    action: () => Promise<unknown>,
  ) => {
    try {
      await action();
      onDone();
    } catch (error) {
      fail(title, error);
    }
  };

  const submitAuditDecision = async () => {
    setAuditError(null);
    try {
      await auditSupplier.mutateAsync({
        id: supplier.id,
        action: auditAction,
        remark: auditRemark.trim() || undefined,
      });
      setAuditOpen(false);
      setAuditRemark("");
      onDone();
    } catch (error) {
      setAuditError(errorMessage(error));
    }
  };

  const submitRiskLevel = async () => {
    setRiskError(null);
    try {
      await changeRiskLevel.mutateAsync({
        id: supplier.id,
        level: riskLevel,
        reason: riskReason.trim() || undefined,
      });
      setRiskOpen(false);
      setRiskReason("");
      onDone();
    } catch (error) {
      setRiskError(errorMessage(error));
    }
  };

  const submitBlacklist = async () => {
    setBlacklistError(null);
    try {
      await blacklistSupplier.mutateAsync({
        id: supplier.id,
        reason: blacklistReason.trim(),
      });
      setBlacklistOpen(false);
      setBlacklistReason("");
      onDone();
    } catch (error) {
      setBlacklistError(errorMessage(error));
    }
  };

  const confirmDelete = async () => {
    try {
      await deleteSupplier.mutateAsync({ id: supplier.id });
      setDeleteOpen(false);
      onDone();
    } catch (error) {
      setDeleteOpen(false);
      fail("删除供应商失败", error);
    }
  };

  return (
    <>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" aria-label="更多操作">
            <MoreHorizontal />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <GuardedMenuItem
            label="提交审核"
            reason={submitAuditReason}
            disabled={Boolean(submitAuditReason)}
            onSelect={() =>
              runAction("提交审核失败", () =>
                submitAudit.mutateAsync({ id: supplier.id }),
              )
            }
          />
          <DropdownMenuItem
            onSelect={() => {
              setAuditAction("APPROVE");
              setAuditRemark("");
              setAuditError(null);
              setAuditOpen(true);
            }}
          >
            审核准入
          </DropdownMenuItem>
          <GuardedMenuItem
            label="启用"
            reason={enableReason}
            disabled={Boolean(enableReason)}
            onSelect={() =>
              runAction("启用失败", () =>
                enableSupplier.mutateAsync({ id: supplier.id }),
              )
            }
          />
          <GuardedMenuItem
            label="停用"
            reason={disableReason}
            disabled={Boolean(disableReason)}
            onSelect={() =>
              runAction("停用失败", () =>
                disableSupplier.mutateAsync({ id: supplier.id }),
              )
            }
          />
          <DropdownMenuItem
            onSelect={() => {
              setRiskLevel(supplier.riskLevel ?? 1);
              setRiskReason("");
              setRiskError(null);
              setRiskOpen(true);
            }}
          >
            调整风险等级
          </DropdownMenuItem>

          {/* 合作状态的目标值全部列出，非法流转由后端拒绝。 */}
          <DropdownMenuSub>
            <DropdownMenuSubTrigger>变更合作状态</DropdownMenuSubTrigger>
            <DropdownMenuSubContent>
              {COOPERATION_STATUSES.map((item) => {
                const reason =
                  item.value === 1 ? establishReason : undefined;
                return (
                  <GuardedMenuItem
                    key={item.value}
                    label={item.label}
                    reason={reason}
                    disabled={Boolean(reason)}
                    onSelect={() =>
                      runAction("变更合作状态失败", () =>
                        changeCooperationStatus.mutateAsync({
                          id: supplier.id,
                          status: item.value,
                        }),
                      )
                    }
                  />
                );
              })}
            </DropdownMenuSubContent>
          </DropdownMenuSub>

          <DropdownMenuSeparator />

          <DropdownMenuItem
            onSelect={() => {
              setBlacklistReason("");
              setBlacklistError(null);
              setBlacklistOpen(true);
            }}
          >
            列入黑名单
          </DropdownMenuItem>
          <GuardedMenuItem
            label="移出黑名单"
            reason={unblacklistReason}
            disabled={Boolean(unblacklistReason)}
            onSelect={() =>
              runAction("移出黑名单失败", () =>
                unblacklistSupplier.mutateAsync({ id: supplier.id }),
              )
            }
          />
          <GuardedMenuItem
            label="删除"
            reason={deleteReason}
            disabled={Boolean(deleteReason)}
            destructive
            onSelect={() => setDeleteOpen(true)}
          />
        </DropdownMenuContent>
      </DropdownMenu>

      {/* 审核准入 */}
      <Dialog open={auditOpen} onOpenChange={setAuditOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>审核准入</DialogTitle>
            <DialogDescription>
              {supplier.name} 的准入结论，驳回时必须填写审核意见。
            </DialogDescription>
          </DialogHeader>

          <div role="radiogroup" aria-label="审核结论" className="flex gap-6">
            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="supplier-audit-action"
                className="accent-[var(--color-primary)]"
                checked={auditAction === "APPROVE"}
                onChange={() => setAuditAction("APPROVE")}
              />
              通过
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input
                type="radio"
                name="supplier-audit-action"
                className="accent-[var(--color-primary)]"
                checked={auditAction === "REJECT"}
                onChange={() => setAuditAction("REJECT")}
              />
              驳回
            </label>
          </div>

          <Label htmlFor="supplier-audit-remark">
            审核意见{auditAction === "REJECT" ? "（必填）" : "（可选）"}
          </Label>
          <Textarea
            id="supplier-audit-remark"
            rows={3}
            value={auditRemark}
            placeholder={auditAction === "REJECT" ? "请填写驳回原因" : "可填写审核意见"}
            onChange={(event) => setAuditRemark(event.target.value)}
          />

          {auditError && <p className={ERROR_CLASS}>{auditError}</p>}

          <DialogFooter>
            <Button variant="outline" onClick={() => setAuditOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                auditSupplier.isPending ||
                (auditAction === "REJECT" && !auditRemark.trim())
              }
              onClick={submitAuditDecision}
            >
              提交
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 调整风险等级 */}
      <Dialog open={riskOpen} onOpenChange={setRiskOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>调整风险等级</DialogTitle>
            <DialogDescription>{supplier.name} 的风险等级变更原因。</DialogDescription>
          </DialogHeader>

          <Label htmlFor="supplier-risk-level">风险等级</Label>
          <Select
            value={String(riskLevel)}
            onValueChange={(next) => setRiskLevel(Number(next))}
          >
            <SelectTrigger id="supplier-risk-level" size="sm" className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {RISK_LEVELS.map((item) => (
                <SelectItem key={item.value} value={String(item.value)}>
                  {item.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>

          <Label htmlFor="supplier-risk-reason">调整原因（可选）</Label>
          <Textarea
            id="supplier-risk-reason"
            rows={3}
            value={riskReason}
            placeholder="可填写调整原因"
            onChange={(event) => setRiskReason(event.target.value)}
          />

          {riskError && <p className={ERROR_CLASS}>{riskError}</p>}

          <DialogFooter>
            <Button variant="outline" onClick={() => setRiskOpen(false)}>
              取消
            </Button>
            <Button
              disabled={changeRiskLevel.isPending}
              onClick={submitRiskLevel}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 列入黑名单 */}
      <Dialog open={blacklistOpen} onOpenChange={setBlacklistOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>列入黑名单</DialogTitle>
            <DialogDescription>
              {supplier.name} 的列入原因将记入变更留痕。
            </DialogDescription>
          </DialogHeader>

          <Label htmlFor="supplier-blacklist-reason">列入原因（必填）</Label>
          <Textarea
            id="supplier-blacklist-reason"
            rows={3}
            value={blacklistReason}
            placeholder="请填写列入黑名单的原因"
            onChange={(event) => setBlacklistReason(event.target.value)}
          />

          {blacklistError && <p className={ERROR_CLASS}>{blacklistError}</p>}

          <DialogFooter>
            <Button variant="outline" onClick={() => setBlacklistOpen(false)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={blacklistSupplier.isPending || !blacklistReason.trim()}
              onClick={submitBlacklist}
            >
              列入黑名单
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 删除 */}
      <ConfirmDialog
        open={deleteOpen}
        title="删除供应商"
        description={`确认删除 ${supplier.name}？该操作不可撤销。`}
        confirmText="删除"
        destructive
        pending={deleteSupplier.isPending}
        onConfirm={confirmDelete}
        onOpenChange={setDeleteOpen}
      />

      {/* 直接触发的动作失败时，后端的业务原因在这里展示。 */}
      <Dialog
        open={Boolean(failure)}
        onOpenChange={(open) => !open && setFailure(null)}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{failure?.title ?? "操作失败"}</DialogTitle>
            <DialogDescription>
              后端拒绝了本次操作，请根据提示调整后重试。
            </DialogDescription>
          </DialogHeader>

          {failure && <p className={ERROR_CLASS}>{failure.message}</p>}

          <DialogFooter>
            <Button variant="outline" onClick={() => setFailure(null)}>
              关闭
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
