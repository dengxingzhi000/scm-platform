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
  TablePagination,
  TablePanel,
  TableToolbar,
  TableToolbarEnd,
} from "@/components/ui/table-shell";
import type { TenantPackageDTO } from "@/lib/api/contracts/tenant";

import { useDeletePackage, usePackageList, useSavePackage } from "../hooks";
import { formatMoney, PACKAGE_LEVEL } from "../lib/format";

const PAGE_SIZE = 20;
const NUMBER_FIELDS = [
  { key: "priceMonthly", label: "月付价格" },
  { key: "priceYearly", label: "年付价格" },
  { key: "maxUsers", label: "用户数上限" },
  { key: "maxWarehouses", label: "仓库数上限" },
  { key: "maxSkus", label: "SKU 上限" },
  { key: "maxOrdersPerDay", label: "每日订单上限" },
  { key: "maxStorageGb", label: "存储上限 (GB)" },
  { key: "maxApiCallsPerDay", label: "每日 API 调用" },
  { key: "trialDays", label: "试用天数" },
] as const;

type NumberKey = (typeof NUMBER_FIELDS)[number]["key"];

interface PackageForm {
  packageCode: string;
  packageName: string;
  packageLevel: string;
  isTrial: string;
  enabled: string;
  description: string;
  numbers: Record<NumberKey, string>;
}

const EMPTY_FORM: PackageForm = {
  packageCode: "",
  packageName: "",
  packageLevel: "1",
  isTrial: "false",
  enabled: "true",
  description: "",
  numbers: Object.fromEntries(NUMBER_FIELDS.map((f) => [f.key, ""])) as Record<
    NumberKey,
    string
  >,
};

export function PackageList() {
  const [page, setPage] = useState(1);
  const { data, isLoading } = usePackageList({ page, size: PAGE_SIZE });
  const save = useSavePackage();
  const remove = useDeletePackage();

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<TenantPackageDTO | null>(null);
  const [form, setForm] = useState<PackageForm>(EMPTY_FORM);
  const [deleting, setDeleting] = useState<TenantPackageDTO | null>(null);

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const openEditor = (record: TenantPackageDTO | null) => {
    setEditing(record);
    setForm(
      record
        ? {
            packageCode: record.packageCode ?? "",
            packageName: record.packageName ?? "",
            packageLevel: String(record.packageLevel ?? 1),
            isTrial: String(record.isTrial ?? false),
            enabled: String(record.enabled ?? true),
            description: record.description ?? "",
            numbers: Object.fromEntries(
              NUMBER_FIELDS.map((f) => [
                f.key,
                record[f.key] === null || record[f.key] === undefined
                  ? ""
                  : String(record[f.key]),
              ]),
            ) as Record<NumberKey, string>,
          }
        : EMPTY_FORM,
    );
    setOpen(true);
  };

  const submit = async () => {
    const numbers = Object.fromEntries(
      NUMBER_FIELDS.map((f) => [
        f.key,
        form.numbers[f.key] === "" ? null : Number(form.numbers[f.key]),
      ]),
    );
    await save.mutateAsync({
      ...editing,
      packageCode: form.packageCode.trim(),
      packageName: form.packageName.trim(),
      packageLevel: Number(form.packageLevel),
      isTrial: form.isTrial === "true",
      enabled: form.enabled === "true",
      description: form.description.trim() || null,
      ...numbers,
    });
    setOpen(false);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="租户管理"
        title="套餐管理"
        description="维护租户套餐的价格与资源上限；开通租户前必须有一个启用的试用版或基础版套餐"
        actions={<Button onClick={() => openEditor(null)}>新建套餐</Button>}
      />

      <TablePanel>
        <TableToolbar>
          <span className="text-muted-foreground text-[11px]">
            套餐上限会在开通租户时复制为资源配额
          </span>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 个套餐
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无套餐，先新建一个试用版或基础版套餐</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[180px]">套餐</TableHead>
                <TableHead>等级</TableHead>
                <TableHead numeric>月付</TableHead>
                <TableHead numeric>年付</TableHead>
                <TableHead numeric>用户 / 仓库 / SKU</TableHead>
                <TableHead className="text-center">试用</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((item) => (
                <TableRow key={item.id}>
                  <TableCell>
                    <div className="font-medium">{item.packageName}</div>
                    <div className="text-muted-foreground font-mono text-[10px]">
                      {item.packageCode}
                    </div>
                  </TableCell>
                  <TableCell>
                    {PACKAGE_LEVEL[item.packageLevel ?? 1] ?? "-"}
                  </TableCell>
                  <TableCell numeric>{formatMoney(item.priceMonthly)}</TableCell>
                  <TableCell numeric>{formatMoney(item.priceYearly)}</TableCell>
                  <TableCell numeric>
                    {item.maxUsers ?? "不限"} / {item.maxWarehouses ?? "不限"} /{" "}
                    {item.maxSkus ?? "不限"}
                  </TableCell>
                  <TableCell className="text-center">
                    {item.isTrial ? (
                      <Badge variant="info">{item.trialDays ?? 30} 天</Badge>
                    ) : (
                      <span className="text-muted-foreground">-</span>
                    )}
                  </TableCell>
                  <TableCell className="text-center">
                    <Badge variant={item.enabled ? "success" : "secondary"}>
                      {item.enabled ? "启用" : "停用"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button variant="ghost" size="sm" onClick={() => openEditor(item)}>
                      编辑
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-destructive"
                      onClick={() => setDeleting(item)}
                    >
                      删除
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}

        <TablePagination
          page={page}
          pageSize={PAGE_SIZE}
          total={total}
          pageCount={pageCount}
          onPageChange={setPage}
        />
      </TablePanel>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editing ? "编辑套餐" : "新建套餐"}</DialogTitle>
            <DialogDescription>
              标记为试用的套餐会用于开通租户时自动创建的试用订阅。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">套餐编码</Label>
              <Input
                value={form.packageCode}
                onChange={(event) =>
                  setForm({ ...form, packageCode: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">套餐名称</Label>
              <Input
                value={form.packageName}
                onChange={(event) =>
                  setForm({ ...form, packageName: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">套餐等级</Label>
              <Select
                value={form.packageLevel}
                onValueChange={(value) => setForm({ ...form, packageLevel: value })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(PACKAGE_LEVEL).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <Label className="mb-1.5 text-[12px]">试用套餐</Label>
                <Select
                  value={form.isTrial}
                  onValueChange={(value) => setForm({ ...form, isTrial: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="false">否</SelectItem>
                    <SelectItem value="true">是</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label className="mb-1.5 text-[12px]">启用</Label>
                <Select
                  value={form.enabled}
                  onValueChange={(value) => setForm({ ...form, enabled: value })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="true">启用</SelectItem>
                    <SelectItem value="false">停用</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
            {NUMBER_FIELDS.map((field) => (
              <div key={field.key}>
                <Label className="mb-1.5 text-[12px]">{field.label}</Label>
                <Input
                  type="number"
                  min={0}
                  value={form.numbers[field.key]}
                  placeholder="留空不限"
                  onChange={(event) =>
                    setForm({
                      ...form,
                      numbers: {
                        ...form.numbers,
                        [field.key]: event.target.value,
                      },
                    })
                  }
                />
              </div>
            ))}
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">描述</Label>
              <Input
                value={form.description}
                onChange={(event) =>
                  setForm({ ...form, description: event.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                save.isPending ||
                !form.packageCode.trim() ||
                !form.packageName.trim()
              }
              onClick={submit}
            >
              {save.isPending ? "保存中…" : "保存"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={Boolean(deleting)}
        title="删除套餐"
        description={
          deleting ? `确定删除套餐「${deleting.packageName}」吗？` : undefined
        }
        confirmText="删除"
        destructive
        pending={remove.isPending}
        onOpenChange={(open) => !open && setDeleting(null)}
        onConfirm={async () => {
          if (!deleting) return;
          await remove.mutateAsync(deleting.id);
          setDeleting(null);
        }}
      />
    </div>
  );
}
