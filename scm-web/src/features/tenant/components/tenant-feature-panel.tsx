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
import { TableEmpty, TableLoading } from "@/components/ui/table-shell";
import type { TenantFeatureDTO } from "@/lib/api/contracts/tenant";

import {
  useDeleteFeature,
  useSaveFeature,
  useTenantFeatures,
} from "../hooks";
import { formatDateTime } from "../lib/format";

interface FeatureForm {
  featureCode: string;
  featureName: string;
  enabled: string;
  isBeta: string;
  usageLimit: string;
  expireAt: string;
}

const EMPTY_FORM: FeatureForm = {
  featureCode: "",
  featureName: "",
  enabled: "true",
  isBeta: "false",
  usageLimit: "",
  expireAt: "",
};

export function TenantFeaturePanel({ tenantId }: { tenantId: string }) {
  const { data, isLoading } = useTenantFeatures(tenantId);
  const save = useSaveFeature();
  const remove = useDeleteFeature();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<TenantFeatureDTO | null>(null);
  const [form, setForm] = useState<FeatureForm>(EMPTY_FORM);

  const rows = data ?? [];

  const openEditor = (record: TenantFeatureDTO | null) => {
    setEditing(record);
    setForm(
      record
        ? {
            featureCode: record.featureCode ?? "",
            featureName: record.featureName ?? "",
            enabled: String(record.enabled ?? true),
            isBeta: String(record.isBeta ?? false),
            usageLimit:
              record.usageLimit === null || record.usageLimit === undefined
                ? ""
                : String(record.usageLimit),
            expireAt: record.expireAt ? record.expireAt.slice(0, 16) : "",
          }
        : EMPTY_FORM,
    );
    setOpen(true);
  };

  const submit = async () => {
    await save.mutateAsync({
      ...editing,
      tenantId,
      featureCode: form.featureCode.trim(),
      featureName: form.featureName.trim() || null,
      enabled: form.enabled === "true",
      isBeta: form.isBeta === "true",
      usageLimit: form.usageLimit === "" ? null : Number(form.usageLimit),
      expireAt: form.expireAt ? `${form.expireAt}:00` : null,
    });
    setOpen(false);
  };

  const toggle = (record: TenantFeatureDTO) =>
    save.mutate({ ...record, enabled: !record.enabled });

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-muted-foreground text-[11px]">
          未配置的功能开关按套餐默认值生效
        </span>
        <Button size="sm" onClick={() => openEditor(null)}>
          新增功能开关
        </Button>
      </div>

      {isLoading ? (
        <TableLoading />
      ) : rows.length === 0 ? (
        <TableEmpty>暂无功能开关</TableEmpty>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>功能编码</TableHead>
              <TableHead>名称</TableHead>
              <TableHead className="text-center">状态</TableHead>
              <TableHead numeric>用量</TableHead>
              <TableHead>到期时间</TableHead>
              <TableHead className="text-right">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((item) => (
              <TableRow key={item.id}>
                <TableCell className="font-mono text-[11px]">
                  {item.featureCode}
                  {item.isBeta && (
                    <Badge variant="info" className="ml-1.5">
                      Beta
                    </Badge>
                  )}
                </TableCell>
                <TableCell>{item.featureName ?? "-"}</TableCell>
                <TableCell className="text-center">
                  <Badge variant={item.enabled ? "success" : "secondary"}>
                    {item.enabled ? "启用" : "停用"}
                  </Badge>
                </TableCell>
                <TableCell numeric>
                  {item.currentUsage ?? 0} /{" "}
                  {item.usageLimit === null || item.usageLimit === undefined
                    ? "不限"
                    : item.usageLimit}
                </TableCell>
                <TableCell>{formatDateTime(item.expireAt)}</TableCell>
                <TableCell className="text-right">
                  <Button variant="ghost" size="sm" onClick={() => toggle(item)}>
                    {item.enabled ? "停用" : "启用"}
                  </Button>
                  <Button variant="ghost" size="sm" onClick={() => openEditor(item)}>
                    编辑
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="text-destructive"
                    disabled={remove.isPending}
                    onClick={() => remove.mutate(item.id)}
                  >
                    删除
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>{editing ? "编辑功能开关" : "新增功能开关"}</DialogTitle>
            <DialogDescription>
              功能编码是后端判断功能是否开放的键，例如 `advanced_report`。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">功能编码</Label>
              <Input
                value={form.featureCode}
                onChange={(event) =>
                  setForm({ ...form, featureCode: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">功能名称</Label>
              <Input
                value={form.featureName}
                onChange={(event) =>
                  setForm({ ...form, featureName: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">启用状态</Label>
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
            <div>
              <Label className="mb-1.5 text-[12px]">Beta 功能</Label>
              <Select
                value={form.isBeta}
                onValueChange={(value) => setForm({ ...form, isBeta: value })}
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
              <Label className="mb-1.5 text-[12px]">用量上限</Label>
              <Input
                type="number"
                min={0}
                value={form.usageLimit}
                placeholder="留空不限"
                onChange={(event) =>
                  setForm({ ...form, usageLimit: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">到期时间</Label>
              <Input
                type="datetime-local"
                value={form.expireAt}
                onChange={(event) =>
                  setForm({ ...form, expireAt: event.target.value })
                }
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button
              disabled={save.isPending || !form.featureCode.trim()}
              onClick={submit}
            >
              {save.isPending ? "保存中…" : "保存"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
