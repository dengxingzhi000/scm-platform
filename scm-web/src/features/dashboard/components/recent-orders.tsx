"use client";

import { useLocale } from "next-intl";
import { useRouter } from "next/navigation";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardAction,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { RecentOrder } from "../types";

interface RecentOrdersProps {
  orders: RecentOrder[];
  loading?: boolean;
}

const statusVariants: Record<
  string,
  "default" | "secondary" | "destructive" | "outline" | "success" | "warning"
> = {
  PENDING: "warning",
  PAID: "default",
  SHIPPED: "secondary",
  COMPLETED: "success",
  CANCELLED: "destructive",
};

export default function RecentOrders({ orders, loading }: RecentOrdersProps) {
  const router = useRouter();
  const locale = useLocale();

  return (
    <Card className="gap-3 overflow-hidden">
      <CardHeader>
        <CardTitle>最近订单</CardTitle>
        <CardAction>
          <button
            type="button"
            className="text-primary text-sm hover:underline"
            onClick={() => router.push(`/${locale}/order`)}
          >
            查看全部
          </button>
        </CardAction>
      </CardHeader>
      <CardContent className="px-0">
        {loading ? (
          <div className="px-6">
            <Skeleton className="h-64 w-full" />
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>订单号</TableHead>
                <TableHead>客户</TableHead>
                <TableHead numeric>金额</TableHead>
                <TableHead className="text-center">状态</TableHead>
                <TableHead>时间</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {orders.map((order) => (
                <TableRow key={order.id}>
                  <TableCell className="text-primary font-medium">
                    {order.orderNo}
                  </TableCell>
                  <TableCell>{order.customerName}</TableCell>
                  <TableCell numeric>
                    ¥{order.amount.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-center">
                    <Badge variant={statusVariants[order.status] ?? "outline"}>
                      {order.statusLabel}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-muted-foreground">
                    {order.createdAt}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </CardContent>
    </Card>
  );
}
