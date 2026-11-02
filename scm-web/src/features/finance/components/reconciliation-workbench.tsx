"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
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
import { TableEmpty, TableLoading, TablePagination, TablePanel, TableToolbar, TableToolbarEnd } from "@/components/ui/table-shell";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import type { ReconciliationRecordDTO } from "@/lib/api/contracts/finance";
import { useAuthStore } from "@/stores/useAuthStore";

import {
  useCreateReconciliation,
  useDeleteReconciliation,
  useReconcileAction,
  useReconciliation,
  useReconciliationPage,
  useUpdateReconciliation,
} from "../hooks";

const PAGE_SIZE = 20;
const ALL = "__all__";

const STATUS: Record<
  number,
  { label: string; variant: "warning" | "info" | "success" | "destructive" }
> = {
  0: { label: "待对账", variant: "warning" },
  1: { label: "已对账", variant: "info" },
  2: { label: "已确认", variant: "success" },
  3: { label: "有差异", variant: "destructive" },
};

const PARTY_TYPE: Record<string, string> = {
  SUPPLIER: "供应商",
  CUSTOMER: "客户",
  PLATFORM: "平台",
  OTHER: "其他",
};

const currency = (value?: number | null) =>
  value === null || value === undefined ? "-" : `¥${value.toLocaleString()}`;

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

interface ReconForm {
  reconciliationNo: string;
  partyType: string;
  partyId: string;
  partyName: string;
  reconciliationPeriod: string;
  startDate: string;
  endDate: string;
  ourTotalAmount: string;
  ourPaidAmount: string;
  theirTotalAmount: string;
  theirPaidAmount: string;
  remark: string;
}

const emptyForm = (no: string): ReconForm => ({
  reconciliationNo: no,
  partyType: "SUPPLIER",
  partyId: "",
  partyName: "",
  reconciliationPeriod: "",
  startDate: "",
  endDate: "",
  ourTotalAmount: "",
  ourPaidAmount: "",
  theirTotalAmount: "",
  theirPaidAmount: "",
  remark: "",
});

function toForm(record: ReconciliationRecordDTO): ReconForm {
  const num = (value?: number | null) =>
    value === null || value === undefined ? "" : String(value);
  return {
    reconciliationNo: record.reconciliationNo ?? "",
    partyType: record.partyType ?? "SUPPLIER",
    partyId: record.partyId ?? "",
    partyName: record.partyName ?? "",
    reconciliationPeriod: record.reconciliationPeriod ?? "",
    startDate: record.startDate ?? "",
    endDate: record.endDate ?? "",
    ourTotalAmount: num(record.ourTotalAmount),
    ourPaidAmount: num(record.ourPaidAmount),
    theirTotalAmount: num(record.theirTotalAmount),
    theirPaidAmount: num(record.theirPaidAmount),
    remark: record.remark ?? "",
  };
}

export function ReconciliationWorkbench() {
  const storeUser = useAuthStore((state) => state.user);
  const [page, setPage] = useState(1);
  const [draft, setDraft] = useState({ keyword: "", status: ALL, hasDiff: ALL });
  const [applied, setApplied] = useState(draft);
  const [recordId, setRecordId] = useState("");
  const listQuery = useReconciliationPage({
    page,
    size: PAGE_SIZE,
    keyword: applied.keyword || undefined,
    status: applied.status === ALL ? undefined : Number(applied.status),
    hasDiff: applied.hasDiff === ALL ? undefined : applied.hasDiff === "true",
  });
  const listRows = listQuery.data?.records ?? [];
  const listTotal = listQuery.data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(listTotal / PAGE_SIZE));
  const { data: record, isLoading } = useReconciliation(recordId);

  const create = useCreateReconciliation();
  const update = useUpdateReconciliation();
  const remove = useDeleteReconciliation();
  const action = useReconcileAction();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<ReconciliationRecordDTO | null>(null);
  const [form, setForm] = useState<ReconForm>(emptyForm(""));
  const [diffOpen, setDiffOpen] = useState(false);
  const [diffReason, setDiffReason] = useState("");
  const [deleting, setDeleting] = useState(false);

  const actorId = storeUser?.id ?? "";
  const actorName = storeUser?.displayName ?? storeUser?.username ?? "";

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm(`REC-${Date.now()}`));
    setFormOpen(true);
  };

  const openEdit = () => {
    if (!record) return;
    setEditing(record);
    setForm(toForm(record));
    setFormOpen(true);
  };

  const submitForm = async () => {
    const payload = {
      reconciliationNo: form.reconciliationNo.trim(),
      partyType: form.partyType,
      partyId: form.partyId.trim() || null,
      partyName: form.partyName.trim() || null,
      reconciliationPeriod: form.reconciliationPeriod.trim() || null,
      startDate: form.startDate || null,
      endDate: form.endDate || null,
      ourTotalAmount: form.ourTotalAmount ? Number(form.ourTotalAmount) : null,
      ourPaidAmount: form.ourPaidAmount ? Number(form.ourPaidAmount) : null,
      theirTotalAmount: form.theirTotalAmount
        ? Number(form.theirTotalAmount)
        : null,
      theirPaidAmount: form.theirPaidAmount
        ? Number(form.theirPaidAmount)
        : null,
      remark: form.remark.trim() || null,
    };
    if (editing) {
      await update.mutateAsync({ id: editing.id, entity: payload });
      setFormOpen(false);
      return;
    }
    const created = await create.mutateAsync(payload);
    setFormOpen(false);
    if (created?.id) {
      setRecordId(created.id);
    }
  };

  const statusInfo = STATUS[record?.status ?? 0] ?? STATUS[0];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="财务管理"
        title="对账管理"
        description="按账期与往来方检索对账记录，比较双方金额并完成对账确认"
        actions={<Button onClick={openCreate}>新建对账记录</Button>}
      />

      <TablePanel>
        <TableToolbar>
          <Input
            className="h-7 w-56 text-xs"
            placeholder="对账单号 / 往来单位"
            value={draft.keyword}
            onChange={(event) => setDraft({ ...draft, keyword: event.target.value })}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                setApplied(draft);
                setPage(1);
              }
            }}
          />
          <Select
            value={draft.status}
            onValueChange={(value) => setDraft({ ...draft, status: value })}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部状态</SelectItem>
              {Object.entries(STATUS).map(([value, meta]) => (
                <SelectItem key={value} value={value}>
                  {meta.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={draft.hasDiff}
            onValueChange={(value) => setDraft({ ...draft, hasDiff: value })}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="差异" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部</SelectItem>
              <SelectItem value="true">有差异</SelectItem>
              <SelectItem value="false">无差异</SelectItem>
            </SelectContent>
          </Select>
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setApplied(draft);
              setPage(1);
            }}
          >
            查询
          </Button>
          <Button
            size="sm"
            variant="ghost"
            onClick={() => {
              const empty = { keyword: "", status: ALL, hasDiff: ALL };
              setDraft(empty);
              setApplied(empty);
              setPage(1);
            }}
          >
            重置
          </Button>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {listTotal} 条对账记录
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {listQuery.isLoading ? (
          <TableLoading />
        ) : listRows.length === 0 ? (
          <TableEmpty>暂无对账记录</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">对账单号</TableHead>
                <TableHead>往来单位</TableHead>
                <TableHead>账期</TableHead>
                <TableHead numeric>我方应结</TableHead>
                <TableHead numeric>对方应结</TableHead>
                <TableHead numeric>差异</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {listRows.map((row) => {
                const status = STATUS[row.status ?? 0] ?? STATUS[0];
                return (
                  <TableRow key={row.id}>
                    <TableCell className="font-mono text-[11px] font-semibold">
                      {row.reconciliationNo ?? "-"}
                    </TableCell>
                    <TableCell>
                      <div>{row.partyName ?? "-"}</div>
                      <div className="text-muted-foreground text-[10px]">
                        {PARTY_TYPE[row.partyType ?? ""] ?? row.partyType ?? ""}
                      </div>
                    </TableCell>
                    <TableCell>{row.reconciliationPeriod ?? "-"}</TableCell>
                    <TableCell numeric>{currency(row.ourTotalAmount)}</TableCell>
                    <TableCell numeric>{currency(row.theirTotalAmount)}</TableCell>
                    <TableCell
                      numeric
                      className={
                        row.hasDiff ? "text-[var(--color-error-text)]" : undefined
                      }
                    >
                      {currency(row.diffAmount)}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={status.variant}>{status.label}</Badge>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setRecordId(row.id)}
                      >
                        查看
                      </Button>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={page}
          pageSize={PAGE_SIZE}
          total={listTotal}
          pageCount={pageCount}
          onPageChange={setPage}
        />
      </TablePanel>

      {recordId && (isLoading ? (
        <TableLoading />
      ) : !record ? (
        <TableEmpty>未找到该对账记录</TableEmpty>
      ) : (
        <div className="space-y-3 rounded-lg border border-[var(--color-border-secondary)] bg-card p-4">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <div className="flex items-center gap-2">
                <span className="font-mono text-[14px] font-semibold">
                  {record.reconciliationNo}
                </span>
                <Badge variant={statusInfo.variant}>{statusInfo.label}</Badge>
                {record.hasDiff && <Badge variant="destructive">存在差异</Badge>}
              </div>
              <div className="text-muted-foreground mt-1 text-[12px]">
                {PARTY_TYPE[record.partyType ?? ""] ?? record.partyType} ·{" "}
                {record.partyName ?? record.partyId ?? "-"} · 期间{" "}
                {record.reconciliationPeriod ?? "-"}（{record.startDate ?? "-"} ~{" "}
                {record.endDate ?? "-"}）
              </div>
            </div>
            <div className="flex flex-wrap gap-2">
              <Button variant="outline" size="sm" onClick={openEdit}>
                编辑
              </Button>
              <Button
                size="sm"
                variant="outline"
                disabled={action.isPending || !actorId || !actorName}
                onClick={() =>
                  action.mutate({
                    id: record.id,
                    action: "reconcile",
                    actorId,
                    actorName,
                  })
                }
              >
                执行对账
              </Button>
              <Button
                size="sm"
                variant="outline"
                disabled={action.isPending}
                onClick={() => {
                  setDiffReason(record.diffReason ?? "");
                  setDiffOpen(true);
                }}
              >
                标记差异
              </Button>
              <Button
                size="sm"
                disabled={action.isPending || !actorId || !actorName}
                onClick={() =>
                  action.mutate({
                    id: record.id,
                    action: "confirm",
                    actorId,
                    actorName,
                  })
                }
              >
                确认对账
              </Button>
              <Button
                size="sm"
                variant="destructive"
                onClick={() => setDeleting(true)}
              >
                删除
              </Button>
            </div>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-[12px]">
              <thead>
                <tr className="text-muted-foreground border-b border-[var(--color-border-secondary)] text-left">
                  <th className="py-1.5 pr-4 font-medium">金额项</th>
                  <th className="py-1.5 pr-4 text-right font-medium">我方</th>
                  <th className="py-1.5 pr-4 text-right font-medium">对方</th>
                  <th className="py-1.5 text-right font-medium">差异</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-[var(--color-border-secondary)]">
                  <td className="py-1.5 pr-4">应结总额</td>
                  <td className="py-1.5 pr-4 text-right tabular-nums">
                    {currency(record.ourTotalAmount)}
                  </td>
                  <td className="py-1.5 pr-4 text-right tabular-nums">
                    {currency(record.theirTotalAmount)}
                  </td>
                  <td className="py-1.5 text-right tabular-nums">
                    {currency(record.diffAmount)}
                  </td>
                </tr>
                <tr className="border-b border-[var(--color-border-secondary)]">
                  <td className="py-1.5 pr-4">已付金额</td>
                  <td className="py-1.5 pr-4 text-right tabular-nums">
                    {currency(record.ourPaidAmount)}
                  </td>
                  <td className="py-1.5 pr-4 text-right tabular-nums">
                    {currency(record.theirPaidAmount)}
                  </td>
                  <td className="py-1.5 text-right tabular-nums">-</td>
                </tr>
                <tr>
                  <td className="py-1.5 pr-4">未付金额</td>
                  <td className="py-1.5 pr-4 text-right tabular-nums">
                    {currency(record.ourUnpaidAmount)}
                  </td>
                  <td className="py-1.5 pr-4 text-right tabular-nums">
                    {currency(record.theirUnpaidAmount)}
                  </td>
                  <td className="py-1.5 text-right tabular-nums">-</td>
                </tr>
              </tbody>
            </table>
          </div>

          <div className="text-muted-foreground grid gap-1 text-[11px] sm:grid-cols-2">
            <span>对账人：{record.reconcilerName ?? "-"}（{formatDateTime(record.reconciledAt)}）</span>
            <span>确认人：{record.confirmerName ?? "-"}（{formatDateTime(record.confirmedAt)}）</span>
            <span className="sm:col-span-2">
              差异原因：{record.diffReason ?? "-"}
            </span>
            <span className="sm:col-span-2">备注：{record.remark ?? "-"}</span>
          </div>
        </div>
      ))}

      <Dialog open={formOpen} onOpenChange={setFormOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>
              {editing ? "编辑对账记录" : "新建对账记录"}
            </DialogTitle>
            <DialogDescription>
              差异金额由后端按双方应结总额计算；标记差异时再补充原因。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">对账单号</Label>
              <Input
                value={form.reconciliationNo}
                onChange={(event) =>
                  setForm({ ...form, reconciliationNo: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">往来方类型</Label>
              <Select
                value={form.partyType}
                onValueChange={(value) => setForm({ ...form, partyType: value })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(PARTY_TYPE).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">往来方 ID</Label>
              <Input
                value={form.partyId}
                onChange={(event) =>
                  setForm({ ...form, partyId: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">往来方名称</Label>
              <Input
                value={form.partyName}
                onChange={(event) =>
                  setForm({ ...form, partyName: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">对账期间</Label>
              <Input
                value={form.reconciliationPeriod}
                placeholder="例如 2026-09"
                onChange={(event) =>
                  setForm({ ...form, reconciliationPeriod: event.target.value })
                }
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label className="mb-1.5 text-[12px]">开始日期</Label>
                <Input
                  type="date"
                  value={form.startDate}
                  onChange={(event) =>
                    setForm({ ...form, startDate: event.target.value })
                  }
                />
              </div>
              <div>
                <Label className="mb-1.5 text-[12px]">结束日期</Label>
                <Input
                  type="date"
                  value={form.endDate}
                  onChange={(event) =>
                    setForm({ ...form, endDate: event.target.value })
                  }
                />
              </div>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">我方应结总额</Label>
              <Input
                type="number"
                min={0}
                value={form.ourTotalAmount}
                onChange={(event) =>
                  setForm({ ...form, ourTotalAmount: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">我方已付</Label>
              <Input
                type="number"
                min={0}
                value={form.ourPaidAmount}
                onChange={(event) =>
                  setForm({ ...form, ourPaidAmount: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">对方应结总额</Label>
              <Input
                type="number"
                min={0}
                value={form.theirTotalAmount}
                onChange={(event) =>
                  setForm({ ...form, theirTotalAmount: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">对方已付</Label>
              <Input
                type="number"
                min={0}
                value={form.theirPaidAmount}
                onChange={(event) =>
                  setForm({ ...form, theirPaidAmount: event.target.value })
                }
              />
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">备注</Label>
              <Textarea
                rows={2}
                value={form.remark}
                onChange={(event) =>
                  setForm({ ...form, remark: event.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setFormOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                (editing ? update.isPending : create.isPending) ||
                !form.reconciliationNo.trim()
              }
              onClick={submitForm}
            >
              保存
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={diffOpen} onOpenChange={setDiffOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>标记对账差异</DialogTitle>
            <DialogDescription>
              记录差异原因，并把该对账记录置为「有差异」。
            </DialogDescription>
          </DialogHeader>
          <Textarea
            rows={3}
            value={diffReason}
            placeholder="差异原因（必填）"
            onChange={(event) => setDiffReason(event.target.value)}
          />
          <DialogFooter>
            <Button variant="outline" onClick={() => setDiffOpen(false)}>
              取消
            </Button>
            <Button
              variant="destructive"
              disabled={action.isPending || !diffReason.trim()}
              onClick={async () => {
                if (!record) return;
                await action.mutateAsync({
                  id: record.id,
                  action: "mark-diff",
                  diffReason: diffReason.trim(),
                });
                setDiffOpen(false);
              }}
            >
              标记差异
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={deleting}
        title="删除对账记录"
        description={
          record
            ? `确定删除对账记录「${record.reconciliationNo}」吗？`
            : undefined
        }
        confirmText="删除"
        destructive
        pending={remove.isPending}
        onOpenChange={setDeleting}
        onConfirm={async () => {
          if (!record) return;
          await remove.mutateAsync(record.id);
          setDeleting(false);
          setRecordId("");
        }}
      />
    </div>
  );
}
