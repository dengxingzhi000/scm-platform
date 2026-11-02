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
import { Textarea } from "@/components/ui/textarea";
import {
  CARRIER_TYPE,
  type TmsCarrierDTO,
} from "@/lib/api/contracts/logistics";

import {
  useCarrierPage,
  useDeleteCarrier,
  useSaveCarrier,
  useToggleCarrier,
} from "../hooks";

const PAGE_SIZE = 20;
const ALL = "__all__";

interface CarrierForm {
  carrierCode: string;
  carrierName: string;
  carrierType: string;
  contactName: string;
  contactPhone: string;
  contactEmail: string;
  serviceArea: string;
  baseRate: string;
  sortOrder: string;
  remark: string;
}

const EMPTY_FORM: CarrierForm = {
  carrierCode: "",
  carrierName: "",
  carrierType: "1",
  contactName: "",
  contactPhone: "",
  contactEmail: "",
  serviceArea: "",
  baseRate: "",
  sortOrder: "",
  remark: "",
};

export function CarrierList() {
  const [page, setPage] = useState(1);
  const [nameDraft, setNameDraft] = useState("");
  const [name, setName] = useState("");
  const [type, setType] = useState(ALL);
  const [enabled, setEnabled] = useState(ALL);

  const { data, isLoading } = useCarrierPage({
    page,
    size: PAGE_SIZE,
    carrierName: name || undefined,
    carrierType: type === ALL ? undefined : Number(type),
    enabled: enabled === ALL ? undefined : enabled === "true",
  });

  const save = useSaveCarrier();
  const remove = useDeleteCarrier();
  const toggle = useToggleCarrier();

  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<TmsCarrierDTO | null>(null);
  const [form, setForm] = useState<CarrierForm>(EMPTY_FORM);
  const [deleting, setDeleting] = useState<TmsCarrierDTO | null>(null);

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const openEditor = (record: TmsCarrierDTO | null) => {
    setEditing(record);
    setForm(
      record
        ? {
            carrierCode: record.carrierCode ?? "",
            carrierName: record.carrierName ?? "",
            carrierType: String(record.carrierType ?? 1),
            contactName: record.contactName ?? "",
            contactPhone: record.contactPhone ?? "",
            contactEmail: record.contactEmail ?? "",
            serviceArea: record.serviceArea ?? "",
            baseRate:
              record.baseRate === null || record.baseRate === undefined
                ? ""
                : String(record.baseRate),
            sortOrder:
              record.sortOrder === null || record.sortOrder === undefined
                ? ""
                : String(record.sortOrder),
            remark: record.remark ?? "",
          }
        : EMPTY_FORM,
    );
    setOpen(true);
  };

  const submit = async () => {
    const payload = {
      carrierCode: form.carrierCode.trim(),
      carrierName: form.carrierName.trim(),
      carrierType: Number(form.carrierType),
      contactName: form.contactName.trim() || null,
      contactPhone: form.contactPhone.trim() || null,
      contactEmail: form.contactEmail.trim() || null,
      serviceArea: form.serviceArea.trim() || null,
      baseRate: form.baseRate ? Number(form.baseRate) : null,
      sortOrder: form.sortOrder ? Number(form.sortOrder) : null,
      remark: form.remark.trim() || null,
      enabled: editing?.enabled ?? true,
    };
    if (editing) {
      await save.mutateAsync({ ...editing, ...payload });
    } else {
      await save.mutateAsync(payload);
    }
    setOpen(false);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="物流管理"
        title="承运商管理"
        description="维护承运商档案、服务区域与基础费率，启停影响运单分配"
        actions={<Button onClick={() => openEditor(null)}>新增承运商</Button>}
      />

      <TablePanel>
        <TableToolbar>
          <Input
            className="h-7 w-48 text-xs"
            placeholder="承运商名称"
            value={nameDraft}
            onChange={(event) => setNameDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                setName(nameDraft.trim());
                setPage(1);
              }
            }}
          />
          <Select
            value={type}
            onValueChange={(value) => {
              setType(value);
              setPage(1);
            }}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="类型" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部类型</SelectItem>
              {Object.entries(CARRIER_TYPE).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={enabled}
            onValueChange={(value) => {
              setEnabled(value);
              setPage(1);
            }}
          >
            <SelectTrigger size="sm" className="w-28 text-xs">
              <SelectValue placeholder="状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部状态</SelectItem>
              <SelectItem value="true">启用</SelectItem>
              <SelectItem value="false">停用</SelectItem>
            </SelectContent>
          </Select>
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setName(nameDraft.trim());
              setPage(1);
            }}
          >
            查询
          </Button>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 家承运商
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无承运商</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[180px]">承运商</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>联系人</TableHead>
                <TableHead>服务区域</TableHead>
                <TableHead numeric>基础费率</TableHead>
                <TableHead numeric>排序</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={row.id}>
                  <TableCell>
                    <div className="font-medium">{row.carrierName}</div>
                    <div className="text-muted-foreground font-mono text-[10px]">
                      {row.carrierCode}
                    </div>
                  </TableCell>
                  <TableCell>
                    {CARRIER_TYPE[row.carrierType ?? 1] ?? "-"}
                  </TableCell>
                  <TableCell>
                    {row.contactName ?? "-"}
                    {row.contactPhone && (
                      <div className="text-muted-foreground text-[10px]">
                        {row.contactPhone}
                      </div>
                    )}
                  </TableCell>
                  <TableCell className="max-w-[200px] truncate">
                    {row.serviceArea ?? "-"}
                  </TableCell>
                  <TableCell numeric>
                    {row.baseRate === null || row.baseRate === undefined
                      ? "-"
                      : `¥${row.baseRate}`}
                  </TableCell>
                  <TableCell numeric>{row.sortOrder ?? "-"}</TableCell>
                  <TableCell className="text-center">
                    <Badge variant={row.enabled ? "success" : "secondary"}>
                      {row.enabled ? "启用" : "停用"}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => openEditor(row)}
                    >
                      编辑
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={toggle.isPending}
                      onClick={() =>
                        toggle.mutate({ id: row.id, enabled: !row.enabled })
                      }
                    >
                      {row.enabled ? "停用" : "启用"}
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      className="text-destructive"
                      onClick={() => setDeleting(row)}
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
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{editing ? "编辑承运商" : "新增承运商"}</DialogTitle>
            <DialogDescription>
              承运商编码在租户内唯一；API 地址与密钥用于对接承运商轨迹回传。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">承运商编码</Label>
              <Input
                value={form.carrierCode}
                onChange={(event) =>
                  setForm({ ...form, carrierCode: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">承运商名称</Label>
              <Input
                value={form.carrierName}
                onChange={(event) =>
                  setForm({ ...form, carrierName: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">类型</Label>
              <Select
                value={form.carrierType}
                onValueChange={(value) =>
                  setForm({ ...form, carrierType: value })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(CARRIER_TYPE).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">联系人</Label>
              <Input
                value={form.contactName}
                onChange={(event) =>
                  setForm({ ...form, contactName: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">联系电话</Label>
              <Input
                value={form.contactPhone}
                onChange={(event) =>
                  setForm({ ...form, contactPhone: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">联系邮箱</Label>
              <Input
                value={form.contactEmail}
                onChange={(event) =>
                  setForm({ ...form, contactEmail: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">服务区域</Label>
              <Input
                value={form.serviceArea}
                placeholder="例如 华东、华南"
                onChange={(event) =>
                  setForm({ ...form, serviceArea: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">基础费率</Label>
              <Input
                type="number"
                min={0}
                value={form.baseRate}
                onChange={(event) =>
                  setForm({ ...form, baseRate: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">排序</Label>
              <Input
                type="number"
                value={form.sortOrder}
                onChange={(event) =>
                  setForm({ ...form, sortOrder: event.target.value })
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
            <Button variant="outline" onClick={() => setOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                save.isPending ||
                !form.carrierCode.trim() ||
                !form.carrierName.trim()
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
        title="删除承运商"
        description={
          deleting ? `确定删除承运商「${deleting.carrierName}」吗？` : undefined
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
