"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import dayjs from "dayjs";
import { useRouter } from "next/navigation";

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
import { Skeleton } from "@/components/ui/skeleton";
import { Textarea } from "@/components/ui/textarea";
import type {
  PurRequestConvertPayload,
  PurRequestDetailDTO,
} from "@/lib/api/contracts/purchase";
import { supplierApiService } from "@/lib/api/services/supplier";
import { warehouseApiService } from "@/lib/api/services/warehouse";

import { useConvertRequest, useRequestDetail } from "../hooks";
import { formatMoney, toDateTimeParam } from "../lib/request-meta";

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  requestId: string | null;
}

export function PurchaseRequestConvertDialog({
  open,
  onOpenChange,
  requestId,
}: Props) {
  const { data, isError, refetch } = useRequestDetail(
    requestId ?? "",
    open && !!requestId,
  );

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[88vh] overflow-y-auto sm:max-w-3xl">
        {data ? (
          <ConvertForm
            key={data.request.id}
            detail={data}
            onClose={() => onOpenChange(false)}
          />
        ) : isError ? (
          <>
            <DialogHeader>
              <DialogTitle>转采购单</DialogTitle>
              <DialogDescription>采购申请加载失败</DialogDescription>
            </DialogHeader>
            <Button variant="outline" size="sm" onClick={() => refetch()}>
              重试
            </Button>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>转采购单</DialogTitle>
              <DialogDescription>正在加载申请明细...</DialogDescription>
            </DialogHeader>
            <Skeleton className="h-40 w-full rounded-lg" />
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}

function ConvertForm({
  detail,
  onClose,
}: {
  detail: PurRequestDetailDTO;
  onClose: () => void;
}) {
  const router = useRouter();
  const convert = useConvertRequest();
  const items = detail.items;

  const { data: suppliers = [] } = useQuery({
    queryKey: ["suppliers", "active"],
    queryFn: () => supplierApiService.active(),
  });
  const { data: warehouses = [] } = useQuery({
    queryKey: ["warehouses", "list"],
    queryFn: () => warehouseApiService.listWarehouses(),
  });

  const [supplierId, setSupplierId] = useState(
    () => items.find((item) => item.suggestedSupplierId)?.suggestedSupplierId ?? "",
  );
  const [warehouseId, setWarehouseId] = useState("");
  const [expectedDelivery, setExpectedDelivery] = useState(() =>
    detail.request.expectedDelivery
      ? dayjs(detail.request.expectedDelivery).format("YYYY-MM-DDTHH:mm")
      : dayjs().add(7, "day").format("YYYY-MM-DDTHH:mm"),
  );
  const [orderType, setOrderType] = useState(
    () => String(detail.request.requestType ?? 1),
  );
  const [taxRatePercent, setTaxRatePercent] = useState("13");
  const [freight, setFreight] = useState("0");
  const [discount, setDiscount] = useState("0");
  const [remark, setRemark] = useState("");
  const [prices, setPrices] = useState<Record<string, string>>(() =>
    Object.fromEntries(
      items.map((item) => [
        item.id,
        item.budgetPrice != null ? String(item.budgetPrice) : "",
      ]),
    ),
  );
  const [error, setError] = useState<string | null>(null);

  const supplier = suppliers.find((item) => item.id === supplierId);
  const warehouse = warehouses.find((item) => item.id === warehouseId);

  const subtotal = items.reduce((sum, item) => {
    const price = Number(prices[item.id] ?? 0);
    return sum + (item.quantity ?? 0) * (Number.isFinite(price) ? price : 0);
  }, 0);
  const taxRate = Number(taxRatePercent) / 100;
  const taxAmount = subtotal * (Number.isFinite(taxRate) ? taxRate : 0);
  const payable = subtotal + Number(freight || 0) - Number(discount || 0);

  const submit = async () => {
    if (!supplierId) {
      setError("请选择供应商");
      return;
    }
    if (!warehouseId) {
      setError("请选择收货仓库");
      return;
    }
    if (!expectedDelivery) {
      setError("请填写期望到货时间");
      return;
    }
    const itemPrices = items.map((item) => ({
      requestItemId: item.id,
      unitPrice: Number(prices[item.id]),
    }));
    if (
      itemPrices.some(
        (item) => !Number.isFinite(item.unitPrice) || item.unitPrice < 0,
      )
    ) {
      setError("请为每条明细填写不小于 0 的单价");
      return;
    }

    const payload: PurRequestConvertPayload = {
      supplierId,
      supplierName: supplier?.supplierName ?? undefined,
      supplierContact: supplier?.contactName ?? undefined,
      warehouseId,
      warehouseName: warehouse?.warehouseName ?? undefined,
      warehouseAddress: warehouse?.address ?? undefined,
      orderType: Number(orderType),
      expectedDelivery: toDateTimeParam(expectedDelivery),
      freightAmount: Number(freight || 0),
      discountAmount: Number(discount || 0),
      taxRate,
      remark: remark.trim() || undefined,
      itemPrices,
    };

    try {
      const order = await convert.mutateAsync({
        id: detail.request.id,
        payload,
      });
      onClose();
      if (order?.id) {
        router.push(`/purchase/${order.id}`);
      }
    } catch {
      // Mutation hook already surfaces the API error.
    }
  };

  return (
    <>
      <DialogHeader>
        <DialogTitle>转采购单</DialogTitle>
        <DialogDescription>
          申请单 {detail.request.requestNo ?? ""} 将生成一张已审批的采购订单，并回写转单状态。
        </DialogDescription>
      </DialogHeader>

      <div className="space-y-4">
        <div className="grid gap-3 md:grid-cols-2">
          <div>
            <Label>供应商</Label>
            <Select value={supplierId} onValueChange={setSupplierId}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="请选择供应商" />
              </SelectTrigger>
              <SelectContent>
                {suppliers.map((item) => (
                  <SelectItem key={item.id} value={item.id}>
                    {item.supplierName ?? item.id}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>收货仓库</Label>
            <Select value={warehouseId} onValueChange={setWarehouseId}>
              <SelectTrigger className="w-full">
                <SelectValue placeholder="请选择仓库" />
              </SelectTrigger>
              <SelectContent>
                {warehouses.map((item) => (
                  <SelectItem key={item.id} value={item.id}>
                    {item.warehouseName ?? item.id}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>订单类型</Label>
            <Select value={orderType} onValueChange={setOrderType}>
              <SelectTrigger className="w-full">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="1">标准采购</SelectItem>
                <SelectItem value="2">紧急采购</SelectItem>
                <SelectItem value="3">补货采购</SelectItem>
                <SelectItem value="4">工程采购</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div>
            <Label>期望到货时间</Label>
            <Input
              type="datetime-local"
              value={expectedDelivery}
              onChange={(event) => setExpectedDelivery(event.target.value)}
            />
          </div>
          <div>
            <Label>税率（%）</Label>
            <Input
              type="number"
              min="0"
              max="100"
              step="0.01"
              value={taxRatePercent}
              onChange={(event) => setTaxRatePercent(event.target.value)}
            />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <Label>运费</Label>
              <Input
                type="number"
                min="0"
                step="0.01"
                value={freight}
                onChange={(event) => setFreight(event.target.value)}
              />
            </div>
            <div>
              <Label>折扣</Label>
              <Input
                type="number"
                min="0"
                step="0.01"
                value={discount}
                onChange={(event) => setDiscount(event.target.value)}
              />
            </div>
          </div>
        </div>

        <div className="rounded-lg border border-[var(--color-border-secondary)]">
          <div className="text-muted-foreground grid grid-cols-12 gap-2 border-b border-[var(--color-border-secondary)] px-3 py-2 text-[11px] font-medium">
            <span className="col-span-5">物料</span>
            <span className="col-span-2 text-right">数量</span>
            <span className="col-span-3 text-right">单价</span>
            <span className="col-span-2 text-right">小计</span>
          </div>
          {items.map((item) => {
            const price = Number(prices[item.id] ?? 0);
            return (
              <div
                key={item.id}
                className="grid grid-cols-12 items-center gap-2 border-b border-[var(--color-border-secondary)] px-3 py-2 last:border-b-0"
              >
                <div className="col-span-5">
                  <div className="text-[13px]">{item.materialName}</div>
                  <div className="text-muted-foreground text-[11px]">
                    {item.materialSpec || item.materialCode || "-"}
                  </div>
                </div>
                <div className="col-span-2 text-right text-[13px]">
                  {item.quantity ?? 0} {item.unit ?? ""}
                </div>
                <div className="col-span-3">
                  <Input
                    type="number"
                    min="0"
                    step="0.01"
                    className="h-8 text-right text-xs"
                    value={prices[item.id] ?? ""}
                    onChange={(event) =>
                      setPrices((current) => ({
                        ...current,
                        [item.id]: event.target.value,
                      }))
                    }
                  />
                </div>
                <div className="col-span-2 text-right text-[13px] font-medium">
                  {formatMoney(
                    (item.quantity ?? 0) * (Number.isFinite(price) ? price : 0),
                  )}
                </div>
              </div>
            );
          })}
        </div>

        <div className="flex flex-wrap justify-end gap-x-6 gap-y-1 text-[13px]">
          <span className="text-muted-foreground">
            合计 {formatMoney(subtotal)}
          </span>
          <span className="text-muted-foreground">
            税额 {formatMoney(taxAmount)}
          </span>
          <span className="font-semibold">应付 {formatMoney(payable)}</span>
        </div>

        <div>
          <Label>备注</Label>
          <Textarea
            rows={2}
            placeholder="可留空，默认记录来源采购申请单号"
            value={remark}
            onChange={(event) => setRemark(event.target.value)}
          />
        </div>

        {error && <p className="text-destructive text-xs">{error}</p>}
      </div>

      <DialogFooter>
        <Button variant="outline" onClick={onClose}>
          取消
        </Button>
        <Button onClick={submit} disabled={convert.isPending}>
          {convert.isPending ? "生成中..." : "生成采购单"}
        </Button>
      </DialogFooter>
    </>
  );
}
