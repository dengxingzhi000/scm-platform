"use client";

import { useState } from "react";

import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { cn } from "@/lib/utils";
import type { TenantDTO } from "@/lib/api/contracts/tenant";

import { TenantConfigPanel } from "./tenant-config-panel";
import { TenantFeaturePanel } from "./tenant-feature-panel";
import { TenantLogPanel } from "./tenant-log-panel";
import { TenantQuotaPanel } from "./tenant-quota-panel";
import { TenantSubscriptionPanel } from "./tenant-subscription-panel";
import {
  COMPANY_SIZE,
  formatDate,
  formatDateTime,
  TENANT_TYPE,
  tenantStatus,
} from "../lib/format";

const TABS = [
  { key: "profile", label: "档案" },
  { key: "subscription", label: "订阅" },
  { key: "quota", label: "资源配额" },
  { key: "feature", label: "功能开关" },
  { key: "config", label: "配置" },
  { key: "log", label: "操作日志" },
] as const;

type TabKey = (typeof TABS)[number]["key"];

function Info({ label, value }: { label: string; value?: string | null }) {
  return (
    <div>
      <dt className="text-muted-foreground text-[11px]">{label}</dt>
      <dd className="mt-0.5 text-[12px]">{value || "-"}</dd>
    </div>
  );
}

interface TenantDetailDialogProps {
  open: boolean;
  tenant: TenantDTO | null;
  onOpenChange: (open: boolean) => void;
}

export function TenantDetailDialog({
  open,
  tenant,
  onOpenChange,
}: TenantDetailDialogProps) {
  const [tab, setTab] = useState<TabKey>("profile");
  if (!tenant) return null;

  const status = tenantStatus(tenant.status);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-4xl">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            {tenant.tenantName}
            <Badge variant={status.variant}>{status.label}</Badge>
          </DialogTitle>
          <DialogDescription>
            {tenant.tenantCode} · {TENANT_TYPE[tenant.tenantType ?? 1] ?? "未知类型"}
          </DialogDescription>
        </DialogHeader>

        <div className="flex gap-1 overflow-x-auto border-b border-[var(--color-border-secondary)] pb-1">
          {TABS.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => setTab(item.key)}
              className={cn(
                "rounded-md px-2.5 py-1 text-[12px] whitespace-nowrap transition-colors",
                tab === item.key
                  ? "bg-[var(--color-primary-soft)] text-[var(--color-primary-text)] font-medium"
                  : "text-muted-foreground hover:bg-sunken",
              )}
            >
              {item.label}
            </button>
          ))}
        </div>

        <div className="max-h-[60vh] min-h-[260px] overflow-y-auto pr-1">
          {tab === "profile" && (
            <dl className="grid gap-3 sm:grid-cols-3">
              <Info label="公司名称" value={tenant.companyName} />
              <Info label="所属行业" value={tenant.industry} />
              <Info
                label="企业规模"
                value={
                  tenant.companySize ? COMPANY_SIZE[tenant.companySize] : null
                }
              />
              <Info label="联系人" value={tenant.contactName} />
              <Info label="联系电话" value={tenant.contactPhone} />
              <Info label="联系邮箱" value={tenant.contactEmail} />
              <Info label="管理员账号" value={tenant.adminUsername} />
              <Info label="管理员邮箱" value={tenant.adminEmail} />
              <Info label="自定义域名" value={tenant.domain} />
              <Info label="试用期" value={`${formatDate(tenant.trialStartDate)} ~ ${formatDate(tenant.trialEndDate)}`} />
              <Info
                label="合同期"
                value={`${formatDate(tenant.contractStartDate)} ~ ${formatDate(tenant.contractEndDate)}`}
              />
              <Info label="开通时间" value={formatDateTime(tenant.activatedAt)} />
              <Info label="暂停时间" value={formatDateTime(tenant.suspendedAt)} />
              <Info label="创建时间" value={formatDateTime(tenant.createTime)} />
              <Info label="备注" value={tenant.remark} />
            </dl>
          )}
          {tab === "subscription" && <TenantSubscriptionPanel tenantId={tenant.id} />}
          {tab === "quota" && <TenantQuotaPanel tenantId={tenant.id} />}
          {tab === "feature" && <TenantFeaturePanel tenantId={tenant.id} />}
          {tab === "config" && <TenantConfigPanel tenantId={tenant.id} />}
          {tab === "log" && <TenantLogPanel tenantId={tenant.id} />}
        </div>
      </DialogContent>
    </Dialog>
  );
}
