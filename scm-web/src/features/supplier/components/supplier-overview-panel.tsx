"use client";

import type { ReactNode } from "react";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";
import { SUPPLIER_LEVELS, SUPPLIER_TYPES, labelOf } from "../lib/constants";
import { formatDate } from "../lib/mappers";
import type { SupplierPerformance, SupplierProfile } from "../types";

interface SupplierOverviewPanelProps {
  profile: SupplierProfile;
  performance?: SupplierPerformance | null;
}

/** 缺失值的统一占位 —— 本模块的既有约定。 */
const MISSING = "-";

function MasterField({
  label,
  children,
  mono = false,
}: {
  label: string;
  children: ReactNode;
  /** 数字与金额用等宽字体。 */
  mono?: boolean;
}) {
  return (
    <div className="min-w-0">
      <div className="text-muted-foreground text-[10px] font-medium tracking-[0.12em] uppercase">
        {label}
      </div>
      <div
        className={cn(
          "mt-1.5 text-[13px] leading-5 break-words",
          mono && "font-mono",
        )}
      >
        {children}
      </div>
    </div>
  );
}

/** 分数 → 进度条宽度（百分比）；缺失按 0 处理。 */
const percentOf = (score?: number | null): number =>
  typeof score === "number" ? Math.min(100, Math.max(0, score)) : 0;

/** 注册资本 —— 千分位 + 元。 */
const capitalOf = (capital?: number): string =>
  typeof capital === "number" ? `¥ ${capital.toLocaleString("zh-CN")}` : MISSING;

export default function SupplierOverviewPanel({
  profile,
  performance,
}: SupplierOverviewPanelProps) {
  const supplier = profile.supplier;

  /**
   * 评分卡的格子标题带权重后缀，进度条标签不带 —— 两处文字必须不同，
   * 否则同一维度会命中两个「质量」。
   */
  const scoreCells = [
    { label: "综合得分", value: performance?.overallScore ?? MISSING },
    { label: "质量 (30%)", value: performance?.qualityScore ?? MISSING },
    { label: "交付 (30%)", value: performance?.deliveryScore ?? MISSING },
    { label: "服务 (20%)", value: performance?.serviceScore ?? MISSING },
    { label: "价格 (20%)", value: performance?.priceScore ?? MISSING },
  ];

  const scoreBars = [
    { label: "质量", score: performance?.qualityScore },
    { label: "交付", score: performance?.deliveryScore },
    { label: "服务", score: performance?.serviceScore },
    { label: "价格", score: performance?.priceScore },
  ];

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>主数据</CardTitle>
          <CardDescription>供应商工商与商务主档信息</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-x-8 gap-y-5 md:grid-cols-3">
            <MasterField label="供应商编码" mono>
              {supplier.code || MISSING}
            </MasterField>
            <MasterField label="英文名称">{supplier.nameEn ?? MISSING}</MasterField>
            <MasterField label="供应商类型">
              {labelOf(SUPPLIER_TYPES, supplier.supplierType)}
            </MasterField>
            <MasterField label="业务类型">
              {supplier.businessType ?? MISSING}
            </MasterField>
            <MasterField label="法人">{supplier.legalPerson ?? MISSING}</MasterField>
            <MasterField label="注册资本" mono>
              {capitalOf(supplier.registeredCapital)}
            </MasterField>
            <MasterField label="成立日期" mono>
              {formatDate(supplier.establishmentDate)}
            </MasterField>
            <MasterField label="统一社会信用代码" mono>
              {supplier.registrationNo ?? MISSING}
            </MasterField>
            <MasterField label="税号" mono>
              {supplier.taxNo ?? MISSING}
            </MasterField>
            <MasterField label="信用等级">
              {supplier.creditRating ?? MISSING}
            </MasterField>
            <MasterField label="付款条件">
              {supplier.paymentTerms ?? MISSING}
            </MasterField>
            <MasterField label="付款方式">
              {supplier.paymentMethod ?? MISSING}
            </MasterField>
            <MasterField label="合作起始日期" mono>
              {formatDate(supplier.cooperationStartDate)}
            </MasterField>
            <MasterField label="官网">{supplier.website ?? MISSING}</MasterField>
            <MasterField label="排序" mono>
              {supplier.sortOrder ?? MISSING}
            </MasterField>
            <MasterField label="备注">{supplier.remark ?? MISSING}</MasterField>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>绩效评分卡</CardTitle>
          <CardDescription>
            后端按质量 30%、交付 30%、服务 20%、价格 20% 的权重计算
          </CardDescription>
        </CardHeader>
        <CardContent>
          {!performance ? (
            <p className="text-muted-foreground text-xs">暂无绩效数据。</p>
          ) : (
            <>
              <div className="grid gap-4 sm:grid-cols-3 lg:grid-cols-5">
                {scoreCells.map((cell) => (
                  <div key={cell.label} className="min-w-0">
                    <div className="text-muted-foreground text-[11px]">
                      {cell.label}
                    </div>
                    <div className="mt-1 font-mono text-[22px] leading-tight font-semibold tabular-nums">
                      {cell.value}
                    </div>
                  </div>
                ))}
              </div>

              <div className="mt-6 space-y-2.5">
                {scoreBars.map((bar) => (
                  <div key={bar.label} className="flex items-center gap-3">
                    <span className="text-muted-foreground w-8 shrink-0 text-[11px]">
                      {bar.label}
                    </span>
                    <div className="h-[5px] flex-1 overflow-hidden rounded-full bg-[var(--color-primary-soft)]">
                      <div
                        className="h-[5px] rounded-full bg-[var(--color-primary)]"
                        style={{ width: `${percentOf(bar.score)}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>

              <p className="text-muted-foreground mt-5 text-[11px] leading-5">
                {`共 ${performance.evaluationCount} 次评价 · 最近周期 ${performance.latestPeriod ?? MISSING} · 最近得分 ${performance.latestTotalScore ?? MISSING} · 建议分级 ${labelOf(SUPPLIER_LEVELS, performance.suggestedLevel)}`}
              </p>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
