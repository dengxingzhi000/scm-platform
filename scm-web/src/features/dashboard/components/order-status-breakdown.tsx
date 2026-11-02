"use client";

import Link from "next/link";
import { useLocale } from "next-intl";
import { ArrowRight } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardAction,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import type { OrderStatusData } from "../types";

/** Ten backend states collapse into six fulfilment stages operators reason about. */
const STAGES = [
  {
    key: "awaitingPayment",
    label: "待付款",
    statuses: ["PENDING_PAYMENT"],
    color: "var(--color-warning)",
    href: "/order?status=PENDING_PAYMENT",
  },
  {
    key: "awaitingShipment",
    label: "待发货",
    statuses: ["PAID", "PENDING_SHIP"],
    color: "var(--color-info)",
    href: "/order?status=PENDING_SHIP",
  },
  {
    key: "inFulfilment",
    label: "配送中",
    statuses: ["SHIPPED", "IN_TRANSIT", "DELIVERED"],
    color: "var(--color-primary)",
    href: "/order?status=SHIPPED",
  },
  {
    key: "completed",
    label: "已完成",
    statuses: ["COMPLETED"],
    color: "var(--color-success)",
    href: "/order?status=COMPLETED",
  },
  {
    key: "afterSales",
    label: "退款 / 售后",
    statuses: ["REFUNDING", "REFUNDED"],
    color: "var(--color-error)",
    href: "/order/refund",
  },
  {
    key: "cancelled",
    label: "已取消",
    statuses: ["CANCELLED"],
    color: "var(--color-text-tertiary)",
    href: "/order?status=CANCELLED",
  },
] as const;

const KNOWN_STATUSES = new Set<string>(STAGES.flatMap((stage) => stage.statuses));

interface StageRow {
  key: string;
  label: string;
  color: string;
  count: number;
  href?: string;
}

interface OrderStatusBreakdownProps {
  data: OrderStatusData[];
  loading?: boolean;
}

/**
 * Ranked horizontal bars instead of a donut: with up to ten order states a pie
 * chart is unreadable, while grouped bars keep the counts, shares and process
 * order legible at a glance.
 */
export default function OrderStatusBreakdown({
  data,
  loading,
}: OrderStatusBreakdownProps) {
  const locale = useLocale();
  const total = data.reduce((sum, item) => sum + item.count, 0);

  const rows: StageRow[] = STAGES.map((stage) => ({
    key: stage.key,
    label: stage.label,
    color: stage.color,
    href: stage.href,
    count: data
      .filter((item) => (stage.statuses as readonly string[]).includes(item.status))
      .reduce((sum, item) => sum + item.count, 0),
  })).filter((row) => row.count > 0);

  const other = data
    .filter((item) => !KNOWN_STATUSES.has(item.status))
    .reduce((sum, item) => sum + item.count, 0);
  if (other > 0) {
    rows.push({
      key: "other",
      label: "其他",
      color: "var(--color-text-tertiary)",
      count: other,
    });
  }

  const fulfilled = data
    .filter((item) => item.status === "DELIVERED" || item.status === "COMPLETED")
    .reduce((sum, item) => sum + item.count, 0);
  const fulfilmentRate =
    total > 0 ? Math.round((fulfilled / total) * 100) : 0;

  // Round the rows, then absorb the rounding drift into the last row so the
  // displayed shares always add up to 100%.
  const percents = rows.map((row) =>
    total > 0 ? Math.round((row.count / total) * 100) : 0
  );
  const drift = percents.reduce((sum, value) => sum + value, 0);
  if (percents.length > 0 && drift !== 100) {
    percents[percents.length - 1] += 100 - drift;
  }

  return (
    <Card className="gap-3">
      <CardHeader>
        <CardTitle>订单状态</CardTitle>
        <CardDescription>按履约阶段归并，点击阶段下钻订单</CardDescription>
        <CardAction>
          <Badge variant="secondary">近期 {total} 单</Badge>
        </CardAction>
      </CardHeader>

      <CardContent>
        {loading ? (
          <Skeleton className="h-[260px] w-full" />
        ) : rows.length === 0 ? (
          <div className="text-muted-foreground flex h-[260px] items-center justify-center text-[13px]">
            暂无订单数据
          </div>
        ) : (
          <>
            <div className="mb-4 flex items-baseline justify-between gap-2 border-b border-[var(--color-border-secondary)] pb-3">
              <span className="text-muted-foreground text-[11px]">
                履约完成率
              </span>
              <span className="text-[20px] leading-none font-semibold tabular-nums">
                {fulfilmentRate}
                <span className="text-muted-foreground ml-0.5 text-[12px] font-medium">
                  %
                </span>
              </span>
            </div>
            <ul aria-label="订单状态分布" className="space-y-3">
              {rows.map((item, index) => {
                const percent = percents[index] ?? 0;
                const content = (
                  <>
                    <div className="flex items-center justify-between gap-2 text-[12px]">
                      <span className="flex items-center gap-1.5">
                        <span
                          className="size-1.5 shrink-0 rounded-full"
                          style={{ backgroundColor: item.color }}
                        />
                        {item.label}
                      </span>
                      <span className="tabular-nums">
                        <span className="font-semibold">{item.count}</span>
                        <span className="text-muted-foreground ml-1 text-[10px]">
                          {percent}%
                        </span>
                      </span>
                    </div>
                    <div className="bg-sunken mt-1 h-1.5 overflow-hidden rounded-full">
                      <div
                        className="h-full rounded-full"
                        style={{
                          width: `${Math.max(percent, item.count > 0 ? 2 : 0)}%`,
                          backgroundColor: item.color,
                        }}
                      />
                    </div>
                  </>
                );
                return (
                  <li key={item.key}>
                    {item.href ? (
                      <Link
                        href={`/${locale}${item.href}`}
                        className="-mx-1 block rounded-md px-1 py-0.5 transition-colors hover:bg-[var(--color-bg-hover)]"
                      >
                        {content}
                      </Link>
                    ) : (
                      <div className="-mx-1 px-1 py-0.5">{content}</div>
                    )}
                  </li>
                );
              })}
            </ul>
          </>
        )}
      </CardContent>

      <CardFooter className="border-t border-[var(--color-border-secondary)] pt-3">
        <Link
          href={`/${locale}/order`}
          className="text-primary inline-flex items-center gap-1 text-[12px] hover:underline"
        >
          查看全部订单
          <ArrowRight className="size-3.5" />
        </Link>
      </CardFooter>
    </Card>
  );
}
