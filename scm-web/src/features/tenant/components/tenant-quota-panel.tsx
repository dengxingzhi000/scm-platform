"use client";

import { useState } from "react";

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
import { TableEmpty, TableLoading } from "@/components/ui/table-shell";
import { cn } from "@/lib/utils";
import type { TenantResourceQuotaDTO } from "@/lib/api/contracts/tenant";

import { useSaveQuota, useTenantQuotas } from "../hooks";

const METRICS: Array<{
  key: keyof TenantResourceQuotaDTO;
  currentKey: keyof TenantResourceQuotaDTO;
  label: string;
}> = [
  { key: "maxUsers", currentKey: "currentUsers", label: "用户数" },
  { key: "maxWarehouses", currentKey: "currentWarehouses", label: "仓库数" },
  { key: "maxSkus", currentKey: "currentSkus", label: "SKU 数" },
  { key: "maxOrdersPerDay", currentKey: "currentOrdersToday", label: "今日订单" },
  { key: "maxStorageGb", currentKey: "currentStorageGb", label: "存储 (GB)" },
  {
    key: "maxApiCallsPerDay",
    currentKey: "currentApiCallsToday",
    label: "今日 API 调用",
  },
];

const FORM_FIELDS: Array<{ key: keyof TenantResourceQuotaDTO; label: string }> = [
  { key: "maxUsers", label: "用户数上限" },
  { key: "maxWarehouses", label: "仓库数上限" },
  { key: "maxSkus", label: "SKU 数上限" },
  { key: "maxOrdersPerDay", label: "每日订单上限" },
  { key: "maxStorageGb", label: "存储上限 (GB)" },
  { key: "maxApiCallsPerDay", label: "每日 API 调用上限" },
];

interface QuotaEditorProps {
  open: boolean;
  editing: TenantResourceQuotaDTO | null;
  form: Record<string, string>;
  pending: boolean;
  onFormChange: (form: Record<string, string>) => void;
  onOpenChange: (open: boolean) => void;
  onSubmit: () => void;
}

function QuotaEditor({
  open,
  editing,
  form,
  pending,
  onFormChange,
  onOpenChange,
  onSubmit,
}: QuotaEditorProps) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{editing ? "编辑资源配额" : "初始化资源配额"}</DialogTitle>
          <DialogDescription>
            留空表示不限制该资源；当前用量由后端统计，不在本表单中修改。
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-3 sm:grid-cols-2">
          {FORM_FIELDS.map((field) => (
            <div key={field.key}>
              <Label className="mb-1.5 text-[12px]">{field.label}</Label>
              <Input
                type="number"
                min={0}
                value={form[field.key] ?? ""}
                onChange={(event) =>
                  onFormChange({ ...form, [field.key]: event.target.value })
                }
              />
            </div>
          ))}
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            取消
          </Button>
          <Button disabled={pending} onClick={onSubmit}>
            {pending ? "保存中…" : "保存"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

export function TenantQuotaPanel({ tenantId }: { tenantId: string }) {
  const { data, isLoading } = useTenantQuotas(tenantId);
  const save = useSaveQuota();
  const [editing, setEditing] = useState<TenantResourceQuotaDTO | null>(null);
  const [open, setOpen] = useState(false);
  const [form, setForm] = useState<Record<string, string>>({});

  const quota = data?.[0];

  const openEditor = (record: TenantResourceQuotaDTO | null) => {
    setEditing(record);
    setForm(
      Object.fromEntries(
        FORM_FIELDS.map((field) => [
          field.key,
          record?.[field.key] === null || record?.[field.key] === undefined
            ? ""
            : String(record[field.key]),
        ]),
      ),
    );
    setOpen(true);
  };

  const submit = async () => {
    const values = Object.fromEntries(
      FORM_FIELDS.map((field) => [
        field.key,
        form[field.key] === "" ? null : Number(form[field.key]),
      ]),
    ) as Partial<TenantResourceQuotaDTO>;
    await save.mutateAsync({ ...editing, tenantId, ...values });
    setOpen(false);
  };

  const editor = (
    <QuotaEditor
      open={open}
      editing={editing}
      form={form}
      pending={save.isPending}
      onFormChange={setForm}
      onOpenChange={setOpen}
      onSubmit={submit}
    />
  );

  if (isLoading) return <TableLoading />;

  if (!quota) {
    return (
      <div className="space-y-3">
        <TableEmpty>该租户还没有资源配额记录</TableEmpty>
        <div className="flex justify-center">
          <Button size="sm" onClick={() => openEditor(null)}>
            初始化配额
          </Button>
        </div>
        {editor}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-muted-foreground text-[11px]">
          配额随套餐下发，可按租户单独调整
        </span>
        <Button variant="outline" size="sm" onClick={() => openEditor(quota)}>
          编辑配额
        </Button>
      </div>
      <div className="grid gap-2 sm:grid-cols-2">
        {METRICS.map((metric) => {
          const max = Number(quota[metric.key] ?? 0);
          const current = Number(quota[metric.currentKey] ?? 0);
          const pct = max > 0 ? Math.min(100, Math.round((current / max) * 100)) : 0;
          return (
            <div
              key={metric.key}
              className="rounded-lg border border-[var(--color-border-secondary)] px-3 py-2"
            >
              <div className="flex items-baseline justify-between text-[12px]">
                <span className="text-muted-foreground">{metric.label}</span>
                <span className="font-medium tabular-nums">
                  {current} / {max > 0 ? max : "不限"}
                </span>
              </div>
              <div className="bg-sunken mt-1.5 h-1.5 overflow-hidden rounded-full">
                <div
                  className={cn(
                    "h-full rounded-full",
                    pct >= 90
                      ? "bg-[var(--color-error)]"
                      : pct >= 70
                        ? "bg-[var(--color-warning)]"
                        : "bg-[var(--color-primary)]",
                  )}
                  style={{ width: `${pct}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
      {editor}
    </div>
  );
}
