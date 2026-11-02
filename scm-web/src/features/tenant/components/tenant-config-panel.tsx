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
import { Textarea } from "@/components/ui/textarea";
import type { TenantConfigDTO } from "@/lib/api/contracts/tenant";

import { useDeleteConfig, useSaveConfig, useTenantConfigs } from "../hooks";

interface ConfigForm {
  configCategory: string;
  configKey: string;
  configValue: string;
  valueType: string;
  description: string;
}

const EMPTY_FORM: ConfigForm = {
  configCategory: "general",
  configKey: "",
  configValue: "",
  valueType: "string",
  description: "",
};

export function TenantConfigPanel({ tenantId }: { tenantId: string }) {
  const { data, isLoading } = useTenantConfigs(tenantId);
  const save = useSaveConfig();
  const remove = useDeleteConfig();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<TenantConfigDTO | null>(null);
  const [form, setForm] = useState<ConfigForm>(EMPTY_FORM);

  const rows = data?.records ?? [];

  const openEditor = (record: TenantConfigDTO | null) => {
    setEditing(record);
    setForm(
      record
        ? {
            configCategory: record.configCategory ?? "general",
            configKey: record.configKey ?? "",
            configValue: record.configValue ?? "",
            valueType: record.valueType ?? "string",
            description: record.description ?? "",
          }
        : EMPTY_FORM,
    );
    setOpen(true);
  };

  const submit = async () => {
    await save.mutateAsync({
      ...editing,
      tenantId,
      configCategory: form.configCategory.trim() || "general",
      configKey: form.configKey.trim(),
      configValue: form.configValue,
      valueType: form.valueType,
      description: form.description.trim() || null,
    });
    setOpen(false);
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-muted-foreground text-[11px]">
          租户级配置覆盖平台默认值，键在租户内唯一
        </span>
        <Button size="sm" onClick={() => openEditor(null)}>
          新增配置
        </Button>
      </div>

      {isLoading ? (
        <TableLoading />
      ) : rows.length === 0 ? (
        <TableEmpty>暂无租户配置</TableEmpty>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>分类</TableHead>
              <TableHead>配置键</TableHead>
              <TableHead className="min-w-[200px]">配置值</TableHead>
              <TableHead>类型</TableHead>
              <TableHead>说明</TableHead>
              <TableHead className="text-right">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((item) => (
              <TableRow key={item.id}>
                <TableCell>{item.configCategory ?? "-"}</TableCell>
                <TableCell className="font-mono text-[11px]">
                  {item.configKey}
                </TableCell>
                <TableCell className="max-w-[280px] truncate">
                  {item.configValue ?? "-"}
                </TableCell>
                <TableCell>{item.valueType ?? "-"}</TableCell>
                <TableCell className="text-muted-foreground">
                  {item.description ?? "-"}
                </TableCell>
                <TableCell className="text-right">
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
            <DialogTitle>{editing ? "编辑配置" : "新增配置"}</DialogTitle>
            <DialogDescription>
              `valueType` 决定后端如何解析配置值（string / number / boolean / json）。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">分类</Label>
              <Input
                value={form.configCategory}
                onChange={(event) =>
                  setForm({ ...form, configCategory: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">配置键</Label>
              <Input
                value={form.configKey}
                onChange={(event) =>
                  setForm({ ...form, configKey: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">值类型</Label>
              <Select
                value={form.valueType}
                onValueChange={(value) => setForm({ ...form, valueType: value })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="string">string</SelectItem>
                  <SelectItem value="number">number</SelectItem>
                  <SelectItem value="boolean">boolean</SelectItem>
                  <SelectItem value="json">json</SelectItem>
                </SelectContent>
              </Select>
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">配置值</Label>
              <Textarea
                rows={3}
                value={form.configValue}
                onChange={(event) =>
                  setForm({ ...form, configValue: event.target.value })
                }
              />
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">说明</Label>
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
              disabled={save.isPending || !form.configKey.trim()}
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
