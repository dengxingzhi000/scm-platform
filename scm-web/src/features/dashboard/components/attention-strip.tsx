"use client";

import Link from "next/link";
import { useLocale } from "next-intl";
import {
  AlertTriangle,
  CheckCircle2,
  Clock3,
  PackageCheck,
  RotateCcw,
  Zap,
} from "lucide-react";

import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import type { DashboardAttention } from "../types";

interface AttentionItem {
  key: keyof DashboardAttention;
  label: string;
  sub: string;
  href: string;
  tone: "warning" | "info" | "error";
  icon: typeof Clock3;
}

const ITEMS: AttentionItem[] = [
  {
    key: "pendingPayment",
    label: "待付款订单",
    sub: "超时未支付需跟进",
    href: "/order?status=PENDING_PAYMENT",
    tone: "warning",
    icon: Clock3,
  },
  {
    key: "pendingShip",
    label: "待发货订单",
    sub: "已付款待出库",
    href: "/order?status=PENDING_SHIP",
    tone: "info",
    icon: PackageCheck,
  },
  {
    key: "refunding",
    label: "退款处理中",
    sub: "需财务与客服确认",
    href: "/order/refund",
    tone: "error",
    icon: RotateCcw,
  },
  {
    key: "lowStock",
    label: "库存风险 SKU",
    sub: "偏低 + 缺货合计",
    href: "/inventory/alerts",
    tone: "error",
    icon: AlertTriangle,
  },
];

const TONE = {
  warning: {
    icon: "bg-[var(--color-warning-soft)] text-[var(--color-warning-text)]",
    value: "text-[var(--color-warning-text)]",
  },
  info: {
    icon: "bg-[var(--color-info-soft)] text-[var(--color-info-text)]",
    value: "text-[var(--color-info-text)]",
  },
  error: {
    icon: "bg-[var(--color-error-soft)] text-[var(--color-error-text)]",
    value: "text-[var(--color-error-text)]",
  },
} as const;

interface AttentionStripProps {
  attention?: DashboardAttention;
  loading?: boolean;
  /** Failed sources, so an unavailable count is never rendered as a real zero. */
  issues?: string[];
}

/** Exception-first strip: the numbers an operator should act on today. */
export default function AttentionStrip({
  attention,
  loading,
  issues = [],
}: AttentionStripProps) {
  const locale = useLocale();

  if (loading || !attention) {
    return <Skeleton className="h-[76px] w-full rounded-lg" />;
  }

  const ordersDown = issues.includes("订单聚合");
  const inventoryDown = issues.some((issue) => issue.startsWith("库存"));
  const isUnavailable = (key: keyof DashboardAttention) =>
    key === "lowStock" || key === "outOfStock" ? inventoryDown : ordersDown;

  const counts = {
    ...attention,
    lowStock: attention.lowStock + attention.outOfStock,
  };
  const availableItems = ITEMS.filter((item) => !isUnavailable(item.key));
  const total = availableItems.reduce((sum, item) => sum + counts[item.key], 0);
  const anyUnavailable = availableItems.length < ITEMS.length;

  return (
    <section
      aria-label="待处理事项"
      className="flex flex-col overflow-hidden rounded-lg border border-[var(--color-border-secondary)] bg-card lg:flex-row"
    >
      <div className="flex items-center justify-between gap-2 border-b border-[var(--color-border-secondary)] px-4 py-2.5 lg:w-36 lg:flex-col lg:items-start lg:justify-center lg:border-r lg:border-b-0">
        <span className="flex items-center gap-2 text-[12px] font-semibold">
          <Zap className="size-4 text-[var(--color-primary)]" />
          待处理
        </span>
        {anyUnavailable ? (
          <span className="text-[10px] text-[var(--color-warning-text)]">
            部分数据不可用
          </span>
        ) : total === 0 ? (
          <span className="flex items-center gap-1 text-[10px] text-[var(--color-success-text)]">
            <CheckCircle2 className="size-3" />
            全部处理完毕
          </span>
        ) : (
          <span className="text-muted-foreground text-[10px] tabular-nums">
            共 {total} 项
          </span>
        )}
      </div>

      <div className="grid flex-1 grid-cols-2 gap-px bg-[var(--color-border-secondary)] lg:grid-cols-4">
        {ITEMS.map((item) => {
          const Icon = item.icon;
          const unavailable = isUnavailable(item.key);
          const value = counts[item.key];
          const tone = TONE[item.tone];
          return (
            <Link
              key={item.key}
              href={`/${locale}${item.href}`}
              className="group bg-card flex items-center gap-3 px-4 py-2.5 transition-colors hover:bg-[var(--color-bg-hover)]"
            >
              <span
                className={cn(
                  "flex size-8 shrink-0 items-center justify-center rounded-lg",
                  !unavailable && value > 0
                    ? tone.icon
                    : "bg-sunken text-muted-foreground",
                )}
              >
                <Icon className="size-4" />
              </span>
              <span className="min-w-0">
                <span className="text-muted-foreground block text-[10px]">
                  {item.label}
                </span>
                <span
                  className={cn(
                    "block text-[18px] leading-tight font-semibold tabular-nums",
                    !unavailable && value > 0
                      ? tone.value
                      : "text-muted-foreground",
                  )}
                >
                  {unavailable ? "—" : value}
                </span>
                <span className="text-muted-foreground block truncate text-[10px]">
                  {unavailable ? "数据不可用" : item.sub}
                </span>
              </span>
            </Link>
          );
        })}
      </div>
    </section>
  );
}
