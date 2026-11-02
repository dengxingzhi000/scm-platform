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
import type { PurRfqDTO } from "@/lib/api/contracts/purchase";
import { useAuthStore } from "@/stores/useAuthStore";

import {
  useCreateRfqItem,
  useDeleteRfqItem,
  useRfqAction,
  useRfqItems,
  useRfqPage,
  useSaveRfq,
} from "../hooks";

const PAGE_SIZE = 20;
const ALL = "__all__";

const STATUS: Record<
  number,
  { label: string; variant: "secondary" | "info" | "warning" | "success" | "destructive" }
> = {
  0: { label: "草稿", variant: "secondary" },
  1: { label: "已发布", variant: "info" },
  2: { label: "报价中", variant: "info" },
  3: { label: "已截止", variant: "warning" },
  4: { label: "已关闭", variant: "secondary" },
};

const RFQ_TYPE: Record<number, string> = {
  1: "公开询价",
  2: "邀请询价",
  3: "竞价采购",
};

const INQUIRY_METHOD: Record<number, string> = {
  1: "电话询价",
  2: "邮件询价",
  3: "平台询价",
};

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

const toDateTimeParam = (value: string) => (value ? `${value}:00` : null);

interface RfqForm {
  rfqNo: string;
  rfqTitle: string;
  rfqType: string;
  inquiryMethod: string;
  deadline: string;
  quotationRequirement: string;
  paymentTerms: string;
  deliveryTerms: string;
  remark: string;
}

const emptyForm = (rfqNo: string): RfqForm => ({
  rfqNo,
  rfqTitle: "",
  rfqType: "1",
  inquiryMethod: "3",
  deadline: "",
  quotationRequirement: "",
  paymentTerms: "",
  deliveryTerms: "",
  remark: "",
});

export function RfqList() {
  const storeUser = useAuthStore((state) => state.user);
  const [page, setPage] = useState(1);
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState(ALL);
  const [rfqType, setRfqType] = useState(ALL);

  const { data, isLoading } = useRfqPage({
    page,
    size: PAGE_SIZE,
    keyword: keyword || undefined,
    status: status === ALL ? undefined : Number(status),
    rfqType: rfqType === ALL ? undefined : Number(rfqType),
  });

  const save = useSaveRfq();
  const action = useRfqAction();
  const createItem = useCreateRfqItem();
  const deleteItem = useDeleteRfqItem();

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<PurRfqDTO | null>(null);
  const [form, setForm] = useState<RfqForm>(emptyForm(""));
  const [detail, setDetail] = useState<PurRfqDTO | null>(null);
  const [confirm, setConfirm] = useState<{
    action: "close" | "delete";
    row: PurRfqDTO;
  } | null>(null);
  const [itemForm, setItemForm] = useState({
    materialCode: "",
    materialName: "",
    materialSpec: "",
    quantity: "",
    unit: "",
  });

  const { data: items } = useRfqItems(detail?.id ?? "", Boolean(detail));

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm(`RFQ-${Date.now()}`));
    setFormOpen(true);
  };

  const openEdit = (row: PurRfqDTO) => {
    setEditing(row);
    setForm({
      rfqNo: row.rfqNo ?? "",
      rfqTitle: row.rfqTitle ?? "",
      rfqType: String(row.rfqType ?? 1),
      inquiryMethod: String(row.inquiryMethod ?? 3),
      deadline: row.deadline ? row.deadline.slice(0, 16) : "",
      quotationRequirement: row.quotationRequirement ?? "",
      paymentTerms: row.paymentTerms ?? "",
      deliveryTerms: row.deliveryTerms ?? "",
      remark: row.remark ?? "",
    });
    setFormOpen(true);
  };

  const submitForm = async () => {
    await save.mutateAsync({
      ...editing,
      rfqNo: form.rfqNo.trim(),
      rfqTitle: form.rfqTitle.trim(),
      rfqType: Number(form.rfqType),
      inquiryMethod: Number(form.inquiryMethod),
      deadline: toDateTimeParam(form.deadline),
      quotationRequirement: form.quotationRequirement.trim() || null,
      paymentTerms: form.paymentTerms.trim() || null,
      deliveryTerms: form.deliveryTerms.trim() || null,
      remark: form.remark.trim() || null,
      status: editing?.status ?? 0,
      initiatorId: editing?.initiatorId ?? storeUser?.id ?? null,
      initiatorName:
        editing?.initiatorName ??
        storeUser?.displayName ??
        storeUser?.username ??
        null,
    });
    setFormOpen(false);
  };

  const submitItem = async () => {
    if (!detail) return;
    await createItem.mutateAsync({
      rfqId: detail.id,
      rfqNo: detail.rfqNo,
      materialCode: itemForm.materialCode.trim(),
      materialName: itemForm.materialName.trim(),
      materialSpec: itemForm.materialSpec.trim() || null,
      quantity: itemForm.quantity ? Number(itemForm.quantity) : null,
      unit: itemForm.unit.trim() || null,
    });
    setItemForm({
      materialCode: "",
      materialName: "",
      materialSpec: "",
      quantity: "",
      unit: "",
    });
  };

  const confirmAction = async () => {
    if (!confirm) return;
    await action.mutateAsync({ id: confirm.row.id, action: confirm.action });
    setConfirm(null);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="采购管理"
        title="询价管理"
        description="创建询价单、维护询价明细并发布给供应商报价"
        actions={<Button onClick={openCreate}>新建询价单</Button>}
      />

      <TablePanel>
        <TableToolbar>
          <Input
            className="h-7 w-48 text-xs"
            placeholder="单号或标题关键字"
            value={keywordDraft}
            onChange={(event) => setKeywordDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") {
                setKeyword(keywordDraft.trim());
                setPage(1);
              }
            }}
          />
          <Select
            value={status}
            onValueChange={(value) => {
              setStatus(value);
              setPage(1);
            }}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="状态" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部状态</SelectItem>
              {Object.entries(STATUS).map(([value, info]) => (
                <SelectItem key={value} value={value}>
                  {info.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Select
            value={rfqType}
            onValueChange={(value) => {
              setRfqType(value);
              setPage(1);
            }}
          >
            <SelectTrigger size="sm" className="w-32 text-xs">
              <SelectValue placeholder="类型" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL}>全部类型</SelectItem>
              {Object.entries(RFQ_TYPE).map(([value, label]) => (
                <SelectItem key={value} value={value}>
                  {label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button
            size="sm"
            variant="outline"
            onClick={() => {
              setKeyword(keywordDraft.trim());
              setPage(1);
            }}
          >
            查询
          </Button>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 张询价单
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无询价单</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[200px]">询价单</TableHead>
                <TableHead>类型</TableHead>
                <TableHead>方式</TableHead>
                <TableHead>截止时间</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>发起人</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => {
                const info = STATUS[row.status ?? 0] ?? STATUS[0];
                return (
                  <TableRow key={row.id}>
                    <TableCell>
                      <div className="font-mono text-[11px] font-semibold">
                        {row.rfqNo}
                      </div>
                      <div className="text-muted-foreground text-[10px]">
                        {row.rfqTitle ?? "-"}
                      </div>
                    </TableCell>
                    <TableCell>{RFQ_TYPE[row.rfqType ?? 1] ?? "-"}</TableCell>
                    <TableCell>
                      {INQUIRY_METHOD[row.inquiryMethod ?? 3] ?? "-"}
                    </TableCell>
                    <TableCell>{formatDateTime(row.deadline)}</TableCell>
                    <TableCell className="text-center">
                      <Badge variant={info.variant}>{info.label}</Badge>
                    </TableCell>
                    <TableCell>{row.initiatorName ?? "-"}</TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setDetail(row)}
                      >
                        明细
                      </Button>
                      {row.status === 0 && (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openEdit(row)}
                          >
                            编辑
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            disabled={action.isPending}
                            onClick={() =>
                              action.mutate({ id: row.id, action: "publish" })
                            }
                          >
                            发布
                          </Button>
                        </>
                      )}
                      {(row.status === 1 || row.status === 2) && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() =>
                            setConfirm({ action: "close", row })
                          }
                        >
                          关闭
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        onClick={() => setConfirm({ action: "delete", row })}
                      >
                        删除
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
          total={total}
          pageCount={pageCount}
          onPageChange={setPage}
        />
      </TablePanel>

      <Dialog open={formOpen} onOpenChange={setFormOpen}>
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>{editing ? "编辑询价单" : "新建询价单"}</DialogTitle>
            <DialogDescription>
              单号由前端生成占位，后端按提交值保存；发布后进入供应商报价阶段。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">询价单号</Label>
              <Input
                value={form.rfqNo}
                onChange={(event) => setForm({ ...form, rfqNo: event.target.value })}
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">标题</Label>
              <Input
                value={form.rfqTitle}
                onChange={(event) =>
                  setForm({ ...form, rfqTitle: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">询价类型</Label>
              <Select
                value={form.rfqType}
                onValueChange={(value) => setForm({ ...form, rfqType: value })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(RFQ_TYPE).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">询价方式</Label>
              <Select
                value={form.inquiryMethod}
                onValueChange={(value) =>
                  setForm({ ...form, inquiryMethod: value })
                }
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {Object.entries(INQUIRY_METHOD).map(([value, label]) => (
                    <SelectItem key={value} value={value}>
                      {label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">截止时间</Label>
              <Input
                type="datetime-local"
                value={form.deadline}
                onChange={(event) =>
                  setForm({ ...form, deadline: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">付款条件</Label>
              <Input
                value={form.paymentTerms}
                onChange={(event) =>
                  setForm({ ...form, paymentTerms: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">交货条件</Label>
              <Input
                value={form.deliveryTerms}
                onChange={(event) =>
                  setForm({ ...form, deliveryTerms: event.target.value })
                }
              />
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">报价要求</Label>
              <Textarea
                rows={2}
                value={form.quotationRequirement}
                onChange={(event) =>
                  setForm({ ...form, quotationRequirement: event.target.value })
                }
              />
            </div>
            <div className="sm:col-span-2">
              <Label className="mb-1.5 text-[12px]">备注</Label>
              <Textarea
                rows={2}
                value={form.remark}
                onChange={(event) => setForm({ ...form, remark: event.target.value })}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setFormOpen(false)}>
              取消
            </Button>
            <Button
              disabled={
                save.isPending || !form.rfqNo.trim() || !form.rfqTitle.trim()
              }
              onClick={submitForm}
            >
              {save.isPending ? "保存中…" : "保存"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog
        open={Boolean(detail)}
        onOpenChange={(open) => !open && setDetail(null)}
      >
        <DialogContent className="sm:max-w-3xl">
          <DialogHeader>
            <DialogTitle>
              询价单 {detail?.rfqNo}
              <Badge
                variant={STATUS[detail?.status ?? 0]?.variant ?? "secondary"}
                className="ml-2"
              >
                {STATUS[detail?.status ?? 0]?.label ?? "-"}
              </Badge>
            </DialogTitle>
            <DialogDescription>{detail?.rfqTitle ?? "-"}</DialogDescription>
          </DialogHeader>

          <div className="grid gap-2 text-[12px] sm:grid-cols-3">
            <div>
              <span className="text-muted-foreground">类型：</span>
              {RFQ_TYPE[detail?.rfqType ?? 1] ?? "-"}
            </div>
            <div>
              <span className="text-muted-foreground">方式：</span>
              {INQUIRY_METHOD[detail?.inquiryMethod ?? 3] ?? "-"}
            </div>
            <div>
              <span className="text-muted-foreground">截止：</span>
              {formatDateTime(detail?.deadline)}
            </div>
            <div className="sm:col-span-3">
              <span className="text-muted-foreground">报价要求：</span>
              {detail?.quotationRequirement ?? "-"}
            </div>
          </div>

          <div className="space-y-2">
            <div className="text-muted-foreground text-[11px] font-medium">
              询价明细
            </div>
            {(items ?? []).length === 0 ? (
              <TableEmpty className="py-6">暂无明细</TableEmpty>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>物料编码</TableHead>
                    <TableHead>名称</TableHead>
                    <TableHead>规格</TableHead>
                    <TableHead numeric>数量</TableHead>
                    <TableHead>单位</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(items ?? []).map((item) => (
                    <TableRow key={item.id}>
                      <TableCell className="font-mono text-[10px]">
                        {item.materialCode ?? "-"}
                      </TableCell>
                      <TableCell>{item.materialName ?? "-"}</TableCell>
                      <TableCell>{item.materialSpec ?? "-"}</TableCell>
                      <TableCell numeric>{item.quantity ?? "-"}</TableCell>
                      <TableCell>{item.unit ?? "-"}</TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-destructive"
                          disabled={deleteItem.isPending}
                          onClick={() => deleteItem.mutate(item.id)}
                        >
                          删除
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>

          {detail && detail.status === 0 && (
            <div className="grid gap-2 rounded-lg border border-[var(--color-border-secondary)] p-2 sm:grid-cols-6">
              <Input
                className="h-7 text-xs"
                placeholder="物料编码"
                value={itemForm.materialCode}
                onChange={(event) =>
                  setItemForm({ ...itemForm, materialCode: event.target.value })
                }
              />
              <Input
                className="h-7 text-xs"
                placeholder="名称"
                value={itemForm.materialName}
                onChange={(event) =>
                  setItemForm({ ...itemForm, materialName: event.target.value })
                }
              />
              <Input
                className="h-7 text-xs"
                placeholder="规格"
                value={itemForm.materialSpec}
                onChange={(event) =>
                  setItemForm({ ...itemForm, materialSpec: event.target.value })
                }
              />
              <Input
                className="h-7 text-xs"
                type="number"
                placeholder="数量"
                value={itemForm.quantity}
                onChange={(event) =>
                  setItemForm({ ...itemForm, quantity: event.target.value })
                }
              />
              <Input
                className="h-7 text-xs"
                placeholder="单位"
                value={itemForm.unit}
                onChange={(event) =>
                  setItemForm({ ...itemForm, unit: event.target.value })
                }
              />
              <Button
                size="sm"
                disabled={
                  createItem.isPending ||
                  !itemForm.materialCode.trim() ||
                  !itemForm.materialName.trim()
                }
                onClick={submitItem}
              >
                添加明细
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={Boolean(confirm)}
        title={confirm?.action === "close" ? "关闭询价单" : "删除询价单"}
        description={
          confirm
            ? `确定${confirm.action === "close" ? "关闭" : "删除"}询价单「${confirm.row.rfqNo}」吗？`
            : undefined
        }
        confirmText={confirm?.action === "close" ? "关闭" : "删除"}
        destructive
        pending={action.isPending}
        onOpenChange={(open) => !open && setConfirm(null)}
        onConfirm={confirmAction}
      />
    </div>
  );
}
