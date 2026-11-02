"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Pencil } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { PageHeader } from "@/components/ui/page-header";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { useSupplierPerformance, useSupplierProfile } from "../hooks";
import {
  AUDIT_STATUSES,
  COOPERATION_STATUSES,
  RISK_LEVELS,
  SUPPLIER_LEVELS,
  labelOf,
  variantOf,
} from "../lib/constants";
import type { BadgeVariant } from "../lib/constants";
import SupplierOverviewPanel from "./supplier-overview-panel";

interface SupplierDetailProps {
  id: string;
}

/** 左栏分区 —— 顺序即页面上从上到下的顺序。 */
type SectionKey =
  | "overview"
  | "categories"
  | "contacts"
  | "bankAccounts"
  | "certificates"
  | "performance"
  | "changeLogs";

export default function SupplierDetail({ id }: SupplierDetailProps) {
  const router = useRouter();
  const [section, setSection] = useState<SectionKey>("overview");

  const { data: profile, isLoading } = useSupplierProfile(id);
  const { data: performance } = useSupplierPerformance(id);

  if (isLoading && !profile) {
    return (
      <div className="space-y-4">
        <div className="space-y-2">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-7 w-56" />
          <Skeleton className="h-3 w-80" />
        </div>
        <div className="flex gap-4">
          <Skeleton className="h-64 w-[122px] shrink-0" />
          <Skeleton className="h-64 flex-1" />
        </div>
      </div>
    );
  }

  if (!profile) {
    return (
      <PageHeader
        eyebrow="供应商档案"
        title="供应商不存在"
        description="该供应商可能已被移除，或链接已失效"
        actions={
          <Button variant="outline" onClick={() => router.push("/supplier")}>
            <ArrowLeft />
            返回列表
          </Button>
        }
      />
    );
  }

  const supplier = profile.supplier;

  const badges: { label: string; value: string; variant: BadgeVariant }[] = [
    {
      label: "分级",
      value: labelOf(SUPPLIER_LEVELS, supplier.supplierLevel),
      variant: variantOf(SUPPLIER_LEVELS, supplier.supplierLevel),
    },
    {
      label: "准入状态",
      value: labelOf(AUDIT_STATUSES, supplier.auditStatus),
      variant: variantOf(AUDIT_STATUSES, supplier.auditStatus),
    },
    {
      label: "风险等级",
      value: labelOf(RISK_LEVELS, supplier.riskLevel),
      variant: variantOf(RISK_LEVELS, supplier.riskLevel),
    },
    {
      label: "黑名单",
      value: supplier.blacklisted ? "已列入" : "未列入",
      variant: supplier.blacklisted ? "destructive" : "outline",
    },
    {
      label: "合作状态",
      value: labelOf(COOPERATION_STATUSES, supplier.cooperationStatus),
      variant: variantOf(COOPERATION_STATUSES, supplier.cooperationStatus),
    },
    {
      label: "启用状态",
      value: supplier.status === "active" ? "启用" : "禁用",
      variant: supplier.status === "active" ? "success" : "destructive",
    },
  ];

  const sections: { key: SectionKey; label: string; count?: number }[] = [
    { key: "overview", label: "概览" },
    { key: "categories", label: "分类", count: profile.categories.length },
    { key: "contacts", label: "联系人", count: profile.contacts.length },
    { key: "bankAccounts", label: "银行账户", count: profile.bankAccounts.length },
    { key: "certificates", label: "资质证照", count: profile.certificates.length },
    { key: "performance", label: "绩效评价", count: performance?.evaluationCount },
    { key: "changeLogs", label: "变更留痕", count: profile.changeLogs.length },
  ];

  const activeSection = sections.find((item) => item.key === section);

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="供应商档案"
        title={supplier.name}
        description={`供应商编码 ${supplier.code} · 创建于 ${supplier.createdAt}`}
        actions={
          <>
            <Button variant="outline" onClick={() => router.push("/supplier")}>
              <ArrowLeft />
              返回列表
            </Button>
            <Button onClick={() => router.push(`/supplier/${id}/edit`)}>
              <Pencil />
              编辑
            </Button>
          </>
        }
      />

      <div className="flex flex-wrap items-center gap-x-4 gap-y-2">
        {badges.map((badge) => (
          <div key={badge.label} className="flex items-center gap-1.5">
            <span className="text-muted-foreground text-[11px]">
              {badge.label}
            </span>
            <Badge variant={badge.variant}>{badge.value}</Badge>
          </div>
        ))}
      </div>

      <div className="flex gap-4">
        <nav className="w-[122px] shrink-0 border-r border-[var(--color-border-secondary)] pr-2">
          {sections.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => setSection(item.key)}
              className={cn(
                "flex w-full items-center justify-between gap-1 rounded-md px-2 py-1.5 text-left text-[12px] transition-colors",
                section === item.key
                  ? "bg-[var(--color-primary-soft)] text-[var(--color-primary-text)] font-semibold"
                  : "text-muted-foreground hover:bg-[var(--color-primary-soft)] hover:text-[var(--color-primary-text)]",
              )}
            >
              <span>{item.label}</span>
              {item.count !== undefined && (
                <span className="text-[10px] tabular-nums">{item.count}</span>
              )}
            </button>
          ))}
        </nav>

        <div className="min-w-0 flex-1">
          {section === "overview" ? (
            <SupplierOverviewPanel profile={profile} performance={performance} />
          ) : (
            <p className="text-muted-foreground text-xs">
              {`${activeSection?.label ?? ""}分区正在建设中。`}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
