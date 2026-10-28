"use client";

import type { ReactNode } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { PageHeader } from "@/components/ui/page-header";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import { useSupplierDetail } from "../hooks";
import RatingStars from "./rating-stars";

interface SupplierDetailProps {
  id: string;
}

function DetailField({
  label,
  children,
  className,
}: {
  label: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("min-w-0", className)}>
      <div className="text-muted-foreground text-[10px] font-medium tracking-[0.12em] uppercase">
        {label}
      </div>
      <div className="mt-1.5 text-[13px] leading-5 break-words">
        {children}
      </div>
    </div>
  );
}

export default function SupplierDetail({ id }: SupplierDetailProps) {
  const router = useRouter();
  const { data: supplier, isLoading } = useSupplierDetail(id);

  if (isLoading && !supplier) {
    return (
      <div className="space-y-4">
        <div className="space-y-2">
          <Skeleton className="h-3 w-24" />
          <Skeleton className="h-7 w-56" />
          <Skeleton className="h-3 w-80" />
        </div>
        <Skeleton className="h-24 w-full rounded-lg" />
        <div className="grid gap-4 lg:grid-cols-3">
          <Skeleton className="h-64 rounded-lg lg:col-span-2" />
          <Skeleton className="h-64 rounded-lg" />
        </div>
      </div>
    );
  }

  if (!supplier) {
    return (
      <div className="space-y-4">
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
      </div>
    );
  }

  const active = supplier.status === "active";

  const metrics = [
    {
      label: "准入状态",
      value: (
        <Badge variant={active ? "success" : "destructive"}>
          {active ? "启用" : "禁用"}
        </Badge>
      ),
    },
    {
      label: "综合评级",
      value: <RatingStars value={supplier.rating} showValue />,
    },
    {
      label: "联系人",
      value: supplier.contactPerson || "-",
    },
    {
      label: "合作起始",
      value: supplier.createdAt,
    },
  ];

  return (
    <div className="space-y-4">
      <PageHeader
        eyebrow="供应商档案"
        title={supplier.name}
        description={`供应商编码 ${supplier.code} · 创建于 ${supplier.createdAt}`}
        actions={
          <Button variant="outline" onClick={() => router.push("/supplier")}>
            <ArrowLeft />
            返回列表
          </Button>
        }
      />

      <Card className="gap-0 overflow-hidden py-0">
        <div className="grid grid-cols-2 md:grid-cols-4">
          {metrics.map((metric, index) => (
            <div
              key={metric.label}
              className={cn(
                "border-[var(--color-border-secondary)] px-5 py-4",
                index % 2 === 1 && "border-l",
                index < 2 && "border-b md:border-b-0",
                index > 0 && "md:border-l",
              )}
            >
              <div className="text-muted-foreground text-[10px] font-medium tracking-[0.12em] uppercase">
                {metric.label}
              </div>
              <div className="mt-2 text-[13px] font-medium">
                {metric.value}
              </div>
            </div>
          ))}
        </div>
      </Card>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>联系信息</CardTitle>
            <CardDescription>主要联系人与履约联络方式</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid gap-x-8 gap-y-6 sm:grid-cols-2">
              <DetailField label="联系人">
                {supplier.contactPerson || "-"}
              </DetailField>
              <DetailField label="联系电话">
                <span className="font-mono">{supplier.contactPhone || "-"}</span>
              </DetailField>
              <DetailField label="电子邮箱">
                {supplier.email || "-"}
              </DetailField>
              <DetailField label="联系地址" className="sm:col-span-2">
                {supplier.address || "-"}
              </DetailField>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>档案信息</CardTitle>
            <CardDescription>编码、创建时间与备注</CardDescription>
          </CardHeader>
          <CardContent className="space-y-5">
            <DetailField label="供应商编码">
              <span className="font-mono">{supplier.code}</span>
            </DetailField>
            <DetailField label="创建时间">
              {supplier.createdAt}
            </DetailField>
            <DetailField label="备注">
              {supplier.remark ? (
                supplier.remark
              ) : (
                <span className="text-muted-foreground">暂无备注信息</span>
              )}
            </DetailField>

            <Separator />

            <p className="text-muted-foreground text-[11px] leading-5">
              档案信息以供应商主数据为准，状态与评级变更需走准入审核流程。
            </p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
