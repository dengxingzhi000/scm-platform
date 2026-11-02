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
import type { PurQuotationDTO } from "@/lib/api/contracts/purchase";
import { useAuthStore } from "@/stores/useAuthStore";

import {
  useCreateQuotation,
  useQuotationAction,
  useQuotationItems,
  useQuotationPage,
} from "../hooks";

const PAGE_SIZE = 20;
const ALL = "__all__";

const STATUS: Record<
  number,
  { label: string; variant: "secondary" | "info" | "success" | "warning" | "destructive" }
> = {
  0: { label: "草稿", variant: "secondary" },
  1: { label: "已提交", variant: "info" },
  2: { label: "已评审", variant: "success" },
  3: { label: "已过期", variant: "warning" },
};

const formatDateTime = (value?: string | null) =>
  value ? value.replace("T", " ").slice(0, 16) : "-";

const currency = (value?: number | null) =>
  value === null || value === undefined ? "-" : `¥${value.toLocaleString()}`;

interface QuotationForm {
  quotationNo: string;
  rfqId: string;
  rfqNo: string;
  supplierId: string;
  supplierName: string;
  supplierContact: string;
  supplierPhone: string;
  totalAmount: string;
  taxRate: string;
  validFrom: string;
  validUntil: string;
  paymentTerms: string;
  deliveryTerms: string;
  remark: string;
}

const emptyForm = (quotationNo: string): QuotationForm => ({
  quotationNo,
  rfqId: "",
  rfqNo: "",
  supplierId: "",
  supplierName: "",
  supplierContact: "",
  supplierPhone: "",
  totalAmount: "",
  taxRate: "",
  validFrom: "",
  validUntil: "",
  paymentTerms: "",
  deliveryTerms: "",
  remark: "",
});

export function QuotationList() {
  const storeUser = useAuthStore((state) => state.user);
  const [page, setPage] = useState(1);
  const [status, setStatus] = useState(ALL);
  const [supplierId, setSupplierId] = useState("");
  const [rfqId, setRfqId] = useState("");

  const { data, isLoading } = useQuotationPage({
    page,
    size: PAGE_SIZE,
    status: status === ALL ? undefined : Number(status),
    supplierId: supplierId.trim() || undefined,
    rfqId: rfqId.trim() || undefined,
  });

  const create = useCreateQuotation();
  const action = useQuotationAction();

  const [formOpen, setFormOpen] = useState(false);
  const [form, setForm] = useState<QuotationForm>(emptyForm(""));
  const [detail, setDetail] = useState<PurQuotationDTO | null>(null);
  const [confirm, setConfirm] = useState<PurQuotationDTO | null>(null);

  const { data: items } = useQuotationItems(detail?.id ?? "", Boolean(detail));

  const rows = data?.records ?? [];
  const total = data?.total ?? 0;
  const pageCount = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const canSelect = Boolean(storeUser?.id);

  const submitForm = async () => {
    await create.mutateAsync({
      quotationNo: form.quotationNo.trim(),
      rfqId: form.rfqId.trim() || null,
      rfqNo: form.rfqNo.trim() || null,
      supplierId: form.supplierId.trim() || null,
      supplierName: form.supplierName.trim() || null,
      supplierContact: form.supplierContact.trim() || null,
      supplierPhone: form.supplierPhone.trim() || null,
      totalAmount: form.totalAmount ? Number(form.totalAmount) : null,
      taxRate: form.taxRate ? Number(form.taxRate) : null,
      validFrom: form.validFrom ? `${form.validFrom}:00` : null,
      validUntil: form.validUntil ? `${form.validUntil}:00` : null,
      paymentTerms: form.paymentTerms.trim() || null,
      deliveryTerms: form.deliveryTerms.trim() || null,
      remark: form.remark.trim() || null,
      status: 0,
      isSelected: false,
    });
    setFormOpen(false);
  };

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="采购管理"
        title="报价管理"
        description="查看供应商报价、提交评审并选用中标报价"
        actions={
          <Button
            onClick={() => {
              setForm(emptyForm(`QT-${Date.now()}`));
              setFormOpen(true);
            }}
          >
            新建报价单
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <Input
            className="h-7 w-56 text-xs"
            placeholder="供应商 ID"
            value={supplierId}
            onChange={(event) => setSupplierId(event.target.value)}
            onKeyDown={(event) => event.key === "Enter" && setPage(1)}
          />
          <Input
            className="h-7 w-56 text-xs"
            placeholder="询价单 ID"
            value={rfqId}
            onChange={(event) => setRfqId(event.target.value)}
            onKeyDown={(event) => event.key === "Enter" && setPage(1)}
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
          <Button size="sm" variant="outline" onClick={() => setPage(1)}>
            查询
          </Button>
          <TableToolbarEnd>
            <span className="text-muted-foreground text-[11px]">
              共 {total} 份报价
            </span>
          </TableToolbarEnd>
        </TableToolbar>

        {isLoading ? (
          <TableLoading />
        ) : rows.length === 0 ? (
          <TableEmpty>暂无报价单</TableEmpty>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="min-w-[190px]">报价单</TableHead>
                <TableHead>供应商</TableHead>
                <TableHead>关联询价</TableHead>
                <TableHead numeric>总金额</TableHead>
                <TableHead>有效期</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead className="text-center">选用</TableHead>
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
                        {row.quotationNo}
                      </div>
                      <div className="text-muted-foreground text-[10px]">
                        {row.supplierContact ?? "-"}
                      </div>
                    </TableCell>
                    <TableCell>{row.supplierName ?? "-"}</TableCell>
                    <TableCell className="font-mono text-[10px]">
                      {row.rfqNo ?? row.rfqId ?? "-"}
                    </TableCell>
                    <TableCell numeric className="font-semibold">
                      {currency(row.totalAmount)}
                    </TableCell>
                    <TableCell>
                      {formatDateTime(row.validFrom)} ~{" "}
                      {formatDateTime(row.validUntil)}
                    </TableCell>
                    <TableCell className="text-center">
                      <Badge variant={info.variant}>{info.label}</Badge>
                    </TableCell>
                    <TableCell className="text-center">
                      {row.isSelected ? (
                        <Badge variant="success">已选用</Badge>
                      ) : (
                        <span className="text-muted-foreground">-</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => setDetail(row)}
                      >
                        明细
                      </Button>
                      {row.status === 0 && (
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={action.isPending}
                          onClick={() =>
                            action.mutate({ id: row.id, action: "submit" })
                          }
                        >
                          提交
                        </Button>
                      )}
                      {!row.isSelected && (row.status === 1 || row.status === 2) && (
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={action.isPending || !canSelect}
                          onClick={() =>
                            action.mutate({
                              id: row.id,
                              action: "select",
                              selectedBy: storeUser?.id,
                              selectedByName:
                                storeUser?.displayName ?? storeUser?.username,
                            })
                          }
                        >
                          选用
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive"
                        onClick={() => setConfirm(row)}
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
            <DialogTitle>新建报价单</DialogTitle>
            <DialogDescription>
              通常由供应商侧创建；运营可在导入历史报价时手工补录。
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-3 sm:grid-cols-2">
            <div>
              <Label className="mb-1.5 text-[12px]">报价单号</Label>
              <Input
                value={form.quotationNo}
                onChange={(event) =>
                  setForm({ ...form, quotationNo: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">供应商名称</Label>
              <Input
                value={form.supplierName}
                onChange={(event) =>
                  setForm({ ...form, supplierName: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">供应商 ID</Label>
              <Input
                value={form.supplierId}
                onChange={(event) =>
                  setForm({ ...form, supplierId: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">询价单 ID</Label>
              <Input
                value={form.rfqId}
                onChange={(event) =>
                  setForm({ ...form, rfqId: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">询价单号</Label>
              <Input
                value={form.rfqNo}
                onChange={(event) =>
                  setForm({ ...form, rfqNo: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">总金额</Label>
              <Input
                type="number"
                min={0}
                value={form.totalAmount}
                onChange={(event) =>
                  setForm({ ...form, totalAmount: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">税率 (%)</Label>
              <Input
                type="number"
                min={0}
                value={form.taxRate}
                onChange={(event) =>
                  setForm({ ...form, taxRate: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">联系人 / 电话</Label>
              <div className="flex gap-2">
                <Input
                  value={form.supplierContact}
                  onChange={(event) =>
                    setForm({ ...form, supplierContact: event.target.value })
                  }
                />
                <Input
                  value={form.supplierPhone}
                  onChange={(event) =>
                    setForm({ ...form, supplierPhone: event.target.value })
                  }
                />
              </div>
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">有效期起</Label>
              <Input
                type="datetime-local"
                value={form.validFrom}
                onChange={(event) =>
                  setForm({ ...form, validFrom: event.target.value })
                }
              />
            </div>
            <div>
              <Label className="mb-1.5 text-[12px]">有效期止</Label>
              <Input
                type="datetime-local"
                value={form.validUntil}
                onChange={(event) =>
                  setForm({ ...form, validUntil: event.target.value })
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
                create.isPending ||
                !form.quotationNo.trim() ||
                !form.supplierName.trim()
              }
              onClick={submitForm}
            >
              {create.isPending ? "保存中…" : "保存"}
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
              报价单 {detail?.quotationNo}
              <Badge
                variant={STATUS[detail?.status ?? 0]?.variant ?? "secondary"}
                className="ml-2"
              >
                {STATUS[detail?.status ?? 0]?.label ?? "-"}
              </Badge>
            </DialogTitle>
            <DialogDescription>
              {detail?.supplierName ?? "-"} · 询价单 {detail?.rfqNo ?? "-"}
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-2 text-[12px] sm:grid-cols-3">
            <div>
              <span className="text-muted-foreground">总金额：</span>
              {currency(detail?.totalAmount)}
            </div>
            <div>
              <span className="text-muted-foreground">税率：</span>
              {detail?.taxRate === null || detail?.taxRate === undefined
                ? "-"
                : `${detail.taxRate}%`}
            </div>
            <div>
              <span className="text-muted-foreground">付款条件：</span>
              {detail?.paymentTerms ?? "-"}
            </div>
            <div>
              <span className="text-muted-foreground">有效期：</span>
              {formatDateTime(detail?.validFrom)} ~{" "}
              {formatDateTime(detail?.validUntil)}
            </div>
            <div>
              <span className="text-muted-foreground">选用人：</span>
              {detail?.selectedByName ?? "-"}
            </div>
            <div>
              <span className="text-muted-foreground">选用时间：</span>
              {formatDateTime(detail?.selectedAt)}
            </div>
          </div>

          <div className="space-y-2">
            <div className="text-muted-foreground text-[11px] font-medium">
              报价明细
            </div>
            {(items ?? []).length === 0 ? (
              <TableEmpty className="py-6">暂无报价明细</TableEmpty>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>物料</TableHead>
                    <TableHead>规格</TableHead>
                    <TableHead numeric>数量</TableHead>
                    <TableHead numeric>单价</TableHead>
                    <TableHead numeric>小计</TableHead>
                    <TableHead numeric>交期</TableHead>
                    <TableHead>品牌</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(items ?? []).map((item) => (
                    <TableRow key={item.id}>
                      <TableCell>
                        <div className="font-mono text-[10px]">
                          {item.materialCode ?? "-"}
                        </div>
                        <div>{item.materialName ?? "-"}</div>
                      </TableCell>
                      <TableCell>{item.materialSpec ?? "-"}</TableCell>
                      <TableCell numeric>
                        {item.quantity ?? "-"} {item.unit ?? ""}
                      </TableCell>
                      <TableCell numeric>{currency(item.unitPrice)}</TableCell>
                      <TableCell numeric>{currency(item.subtotal)}</TableCell>
                      <TableCell numeric>
                        {item.deliveryDays === null ||
                        item.deliveryDays === undefined
                          ? "-"
                          : `${item.deliveryDays} 天`}
                      </TableCell>
                      <TableCell>{item.brand ?? "-"}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </div>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={Boolean(confirm)}
        title="删除报价单"
        description={
          confirm ? `确定删除报价单「${confirm.quotationNo}」吗？` : undefined
        }
        confirmText="删除"
        destructive
        pending={action.isPending}
        onOpenChange={(open) => !open && setConfirm(null)}
        onConfirm={async () => {
          if (!confirm) return;
          await action.mutateAsync({ id: confirm.id, action: "delete" });
          setConfirm(null);
        }}
      />
    </div>
  );
}
