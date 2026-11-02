"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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

import {
  usePackageList,
  useSubscribePackage,
  useTenantSubscriptions,
  useUnsubscribe,
} from "../hooks";
import {
  formatDate,
  formatMoney,
  PAYMENT_STATUS,
  SUBSCRIPTION_TYPE,
  subscriptionStatus,
} from "../lib/format";

export function TenantSubscriptionPanel({ tenantId }: { tenantId: string }) {
  const [packageId, setPackageId] = useState("");
  const { data, isLoading } = useTenantSubscriptions(tenantId);
  const { data: packagePage } = usePackageList({ page: 1, size: 100 });
  const subscribe = useSubscribePackage();
  const unsubscribe = useUnsubscribe();

  const rows = data ?? [];
  const packages = packagePage?.records ?? [];

  const packageName = (id?: string | null) =>
    packages.find((item) => item.id === id)?.packageName ?? "-";

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-2">
        <Select value={packageId} onValueChange={setPackageId}>
          <SelectTrigger size="sm" className="w-56 text-xs">
            <SelectValue placeholder="选择套餐" />
          </SelectTrigger>
          <SelectContent>
            {packages.map((item) => (
              <SelectItem key={item.id} value={item.id}>
                {item.packageName}（{item.packageCode}）
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <Button
          size="sm"
          disabled={!packageId || subscribe.isPending}
          onClick={() =>
            subscribe.mutate({ tenantId, packageId }, { onSuccess: () => setPackageId("") })
          }
        >
          订阅套餐
        </Button>
        <span className="text-muted-foreground text-[11px]">
          订阅会立即生效，原订阅需手工取消
        </span>
      </div>

      {isLoading ? (
        <TableLoading />
      ) : rows.length === 0 ? (
        <TableEmpty>暂无订阅记录</TableEmpty>
      ) : (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>套餐</TableHead>
              <TableHead>类型</TableHead>
              <TableHead className="text-center">状态</TableHead>
              <TableHead>有效期</TableHead>
              <TableHead numeric>实际价格</TableHead>
              <TableHead>支付</TableHead>
              <TableHead className="text-right">操作</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((item) => {
              const status = subscriptionStatus(item.status);
              return (
                <TableRow key={item.id}>
                  <TableCell className="font-medium">
                    {packageName(item.packageId)}
                  </TableCell>
                  <TableCell>
                    {SUBSCRIPTION_TYPE[item.subscriptionType ?? 1] ?? "-"}
                  </TableCell>
                  <TableCell className="text-center">
                    <Badge variant={status.variant}>{status.label}</Badge>
                  </TableCell>
                  <TableCell>
                    {formatDate(item.startDate)} ~ {formatDate(item.endDate)}
                  </TableCell>
                  <TableCell numeric>{formatMoney(item.actualPrice)}</TableCell>
                  <TableCell>
                    {PAYMENT_STATUS[item.paymentStatus ?? 0] ?? "-"}
                  </TableCell>
                  <TableCell className="text-right">
                    <Button
                      variant="ghost"
                      size="sm"
                      disabled={unsubscribe.isPending || item.status === 3}
                      onClick={() => unsubscribe.mutate(item.id)}
                    >
                      取消订阅
                    </Button>
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
