"use client";

import { useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { message } from "antd";
import { Plus, Search } from "lucide-react";

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
import { TablePanel, TableToolbar } from "@/components/ui/table-shell";
import type { Seller } from "@/lib/api/contracts/ecommerce";
import { toApiError } from "@/lib/api/envelope";
import { mallApi } from "@/lib/api/services/ecommerce";
import { formatDateTime, toNumberOrUndefined } from "../lib/status";

interface SellerFormState {
  sellerName: string;
  sellerType: string;
  contactName: string;
  contactPhone: string;
  licenseNo: string;
  status: string;
}

const emptyForm: SellerFormState = {
  sellerName: "",
  sellerType: "",
  contactName: "",
  contactPhone: "",
  licenseNo: "",
  status: "1",
};

export default function SellerManage() {
  const [sellerIdDraft, setSellerIdDraft] = useState("");
  const [lookupId, setLookupId] = useState<number | null>(null);
  const [registered, setRegistered] = useState<Seller | null>(null);

  const [registerOpen, setRegisterOpen] = useState(false);
  const [form, setForm] = useState<SellerFormState>(emptyForm);

  const lookupQuery = useQuery({
    queryKey: ["ecommerce", "sellers", lookupId],
    queryFn: () => mallApi.sellers.get(lookupId!),
    enabled: lookupId !== null,
  });

  const registerSeller = useMutation({
    mutationFn: () =>
      mallApi.sellers.register({
        sellerName: form.sellerName.trim(),
        sellerType: form.sellerType.trim() || undefined,
        contactName: form.contactName.trim() || undefined,
        contactPhone: form.contactPhone.trim() || undefined,
        licenseNo: form.licenseNo.trim() || undefined,
        status: Number(form.status),
      }),
    onSuccess: (created) => {
      message.success("卖家入驻成功");
      setRegistered(created);
      setLookupId(null);
      setRegisterOpen(false);
      setForm(emptyForm);
    },
    onError: (error) => message.error(toApiError(error).message),
  });

  const lookup = () => {
    const value = toNumberOrUndefined(sellerIdDraft);
    if (value === undefined || value <= 0) {
      message.error("请填写有效的卖家 ID");
      return;
    }
    setRegistered(null);
    setLookupId(value);
  };

  const seller = registered ?? lookupQuery.data ?? null;

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="电商中心"
        title="卖家管理"
        description="按卖家 ID 查询档案，登记新的入驻卖家"
        actions={
          <Button
            onClick={() => {
              setForm(emptyForm);
              setRegisterOpen(true);
            }}
          >
            <Plus />
            卖家入驻
          </Button>
        }
      />

      <TablePanel>
        <TableToolbar>
          <form
            className="flex items-center gap-2"
            onSubmit={(event) => {
              event.preventDefault();
              lookup();
            }}
          >
            <Input
              type="number"
              min={1}
              value={sellerIdDraft}
              onChange={(event) => setSellerIdDraft(event.target.value)}
              placeholder="卖家 ID"
              className="h-8 w-40 text-xs"
            />
            <Button type="submit" variant="outline" size="sm">
              <Search />
              查询
            </Button>
          </form>
          {seller && (
            <span className="text-muted-foreground text-[11px]">
              当前卖家：{seller.sellerName}
            </span>
          )}
        </TableToolbar>

        <div className="p-4">
          {seller ? (
            <div className="space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="text-muted-foreground text-[10px] tracking-[0.08em] uppercase">
                    卖家档案
                  </p>
                  <h2 className="text-[15px] font-semibold">
                    {seller.sellerName}
                  </h2>
                </div>
                <Badge
                  variant={seller.status === 1 ? "success" : "secondary"}
                >
                  {seller.status === 1
                    ? "正常"
                    : `状态 ${seller.status ?? "—"}`}
                </Badge>
              </div>

              <dl className="grid gap-3 sm:grid-cols-3">
                <Info label="卖家 ID" value={String(seller.id)} />
                <Info label="卖家类型" value={seller.sellerType || "—"} />
                <Info label="联系人" value={seller.contactName || "—"} />
                <Info label="联系电话" value={seller.contactPhone || "—"} />
                <Info label="营业执照号" value={seller.licenseNo || "—"} />
                <Info
                  label="创建时间"
                  value={formatDateTime(seller.createdAt)}
                />
                <Info
                  label="更新时间"
                  value={formatDateTime(seller.updatedAt)}
                />
              </dl>
            </div>
          ) : lookupQuery.isLoading ? (
            <p className="text-muted-foreground py-12 text-center text-xs">
              查询中…
            </p>
          ) : lookupQuery.isError ? (
            <p className="text-muted-foreground py-12 text-center text-xs">
              查询失败：{toApiError(lookupQuery.error).message}
            </p>
          ) : (
            <p className="text-muted-foreground py-12 text-center text-xs">
              输入卖家 ID 后点击查询，或通过右上角「卖家入驻」登记新卖家
            </p>
          )}
        </div>
      </TablePanel>

      <Dialog open={registerOpen} onOpenChange={setRegisterOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>卖家入驻</DialogTitle>
            <DialogDescription>
              登记新卖家后，档案会展示在下方详情区。
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="space-y-1">
              <Label className="text-xs">卖家名称</Label>
              <Input
                value={form.sellerName}
                onChange={(event) =>
                  setForm((prev) => ({
                    ...prev,
                    sellerName: event.target.value,
                  }))
                }
                placeholder="必填"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">卖家类型</Label>
              <Input
                value={form.sellerType}
                onChange={(event) =>
                  setForm((prev) => ({
                    ...prev,
                    sellerType: event.target.value,
                  }))
                }
                placeholder="如：企业 / 个人"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">联系人</Label>
              <Input
                value={form.contactName}
                onChange={(event) =>
                  setForm((prev) => ({
                    ...prev,
                    contactName: event.target.value,
                  }))
                }
                placeholder="可选"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">联系电话</Label>
              <Input
                value={form.contactPhone}
                onChange={(event) =>
                  setForm((prev) => ({
                    ...prev,
                    contactPhone: event.target.value,
                  }))
                }
                placeholder="可选"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">营业执照号</Label>
              <Input
                value={form.licenseNo}
                onChange={(event) =>
                  setForm((prev) => ({
                    ...prev,
                    licenseNo: event.target.value,
                  }))
                }
                placeholder="可选"
              />
            </div>
            <div className="space-y-1">
              <Label className="text-xs">状态</Label>
              <Select
                value={form.status}
                onValueChange={(value) =>
                  setForm((prev) => ({ ...prev, status: value }))
                }
              >
                <SelectTrigger size="sm" className="w-full text-xs">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="1">正常</SelectItem>
                  <SelectItem value="0">停用</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setRegisterOpen(false)}>
              取消
            </Button>
            <Button
              disabled={registerSeller.isPending || !form.sellerName.trim()}
              onClick={() => registerSeller.mutate()}
            >
              提交入驻
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-[var(--color-border-secondary)] px-3 py-2">
      <dt className="text-muted-foreground text-[10px] tracking-[0.08em] uppercase">
        {label}
      </dt>
      <dd className="mt-0.5 truncate text-[12px] font-medium">{value}</dd>
    </div>
  );
}
