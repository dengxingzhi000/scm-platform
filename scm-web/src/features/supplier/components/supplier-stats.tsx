"use client";

import { useRouter } from "next/navigation";

import { Card, CardContent } from "@/components/ui/card";
import { useSupplierStats } from "../hooks";
import type { SupplierSearchParams } from "../types";

interface SupplierStatsProps {
  /** 点击数字卡时把要应用的筛选条件交给列表页。 */
  onFilter: (patch: Partial<SupplierSearchParams>) => void;
}

/** 参与统计的五个数值字段 —— 与 `SupplierStats` 的数字字段一一对应。 */
type StatKey =
  | "total"
  | "enabled"
  | "cooperating"
  | "pendingAudit"
  | "blacklisted";

interface StatCard {
  key: StatKey;
  label: string;
  /** 点击后应用的筛选条件；空对象表示清空全部筛选。 */
  filter: Partial<SupplierSearchParams>;
}

const CARDS: StatCard[] = [
  { key: "total", label: "供应商总数", filter: {} },
  { key: "enabled", label: "已启用", filter: { status: "active" } },
  { key: "cooperating", label: "合作中", filter: { cooperationStatus: 1 } },
  { key: "pendingAudit", label: "待审核", filter: { auditStatus: 1 } },
  { key: "blacklisted", label: "黑名单", filter: { blacklisted: true } },
];

export default function SupplierStats({ onFilter }: SupplierStatsProps) {
  const router = useRouter();
  const { data } = useSupplierStats();

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6">
      {CARDS.map((card) => (
        <Card key={card.key} className="gap-0 py-0">
          <CardContent className="px-4 py-3">
            <button
              type="button"
              className="w-full text-left"
              onClick={() => onFilter(card.filter)}
            >
              <span className="text-muted-foreground block text-[10px]">
                {card.label}
              </span>
              <span className="mt-1 block text-[23px] font-semibold tabular-nums">
                {data?.[card.key] ?? 0}
              </span>
            </button>
          </CardContent>
        </Card>
      ))}

      {/* 证照到期预警在另一个页面，这里只做跳转，不改筛选。 */}
      <Card className="gap-0 py-0">
        <CardContent className="px-4 py-3">
          <button
            type="button"
            className="w-full text-left"
            onClick={() => router.push("/supplier/audit")}
          >
            <span className="text-muted-foreground block text-[10px]">
              30 天内到期证照
            </span>
            <span className="mt-1 block text-[23px] font-semibold tabular-nums">
              {data?.expiringCertificates ?? 0}
            </span>
          </button>
        </CardContent>
      </Card>
    </div>
  );
}
